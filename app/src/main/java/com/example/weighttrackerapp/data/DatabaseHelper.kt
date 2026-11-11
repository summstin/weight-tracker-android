package com.example.weighttrackerapp.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.weighttrackerapp.AppConstants
import com.example.weighttrackerapp.security.PasswordHasher

// Hashed passwords, weights owned by a user, numeric weights, goal stored per user.

class DatabaseHelper(
    private val appContext: Context,
    private val hasher: PasswordHasher = PasswordHasher()
) : SQLiteOpenHelper(appContext, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 2

        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD_HASH = "password_hash"

        const val TABLE_WEIGHTS = "weights"
        const val COL_USER_ID = "user_id"
        const val COL_DATE = "date"
        const val COL_WEIGHT = "weight"

        const val TABLE_GOALS = "goals"
        const val COL_GOAL_WEIGHT = "goal_weight"
        const val COL_PHONE = "phone_number"

        // Names that only exist in the version 1 schema, used by the migration.
        private const val LEGACY_USERS_TABLE = "users_v1"
        private const val LEGACY_WEIGHTS_TABLE = "weights_v1"
        private const val LEGACY_COL_PASSWORD = "password"
        private const val LEGACY_PREF_GOAL = "goal_weight"
    }

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) = createSchema(db)

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Upgrades run in order and keep user data (the original version dropped every table)
        if (oldVersion < 2) migrateV1ToV2(db)
    }

    private fun createSchema(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT NOT NULL UNIQUE COLLATE NOCASE,
                $COL_PASSWORD_HASH TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE $TABLE_WEIGHTS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USER_ID INTEGER NOT NULL REFERENCES $TABLE_USERS($COL_ID) ON DELETE CASCADE,
                $COL_DATE TEXT NOT NULL,
                $COL_WEIGHT REAL NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_weights_user_date ON $TABLE_WEIGHTS($COL_USER_ID, $COL_DATE)")
        db.execSQL(
            """
            CREATE TABLE $TABLE_GOALS (
                $COL_USER_ID INTEGER PRIMARY KEY REFERENCES $TABLE_USERS($COL_ID) ON DELETE CASCADE,
                $COL_GOAL_WEIGHT REAL NOT NULL,
                $COL_PHONE TEXT
            )
            """.trimIndent()
        )
    }

    // Moves version 1 data into the new schema:

    private fun migrateV1ToV2(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE $TABLE_USERS RENAME TO $LEGACY_USERS_TABLE")
        db.execSQL("ALTER TABLE $TABLE_WEIGHTS RENAME TO $LEGACY_WEIGHTS_TABLE")
        createSchema(db)

        var firstUserId: Long? = null
        db.rawQuery(
            "SELECT $COL_ID, $COL_USERNAME, $LEGACY_COL_PASSWORD FROM $LEGACY_USERS_TABLE ORDER BY $COL_ID",
            null
        ).use { cursor ->
            while (cursor.moveToNext()) {
                val legacyPassword = cursor.getString(2) ?: continue
                val values = ContentValues().apply {
                    put(COL_ID, cursor.getLong(0))
                    put(COL_USERNAME, cursor.getString(1))
                    put(COL_PASSWORD_HASH, hasher.hash(legacyPassword))
                }
                val rowId = db.insertWithOnConflict(TABLE_USERS, null, values, SQLiteDatabase.CONFLICT_IGNORE)
                if (rowId != -1L && firstUserId == null) firstUserId = rowId
            }
        }

        val ownerId = firstUserId
        if (ownerId != null) {
            db.execSQL(
                """
                INSERT INTO $TABLE_WEIGHTS ($COL_USER_ID, $COL_DATE, $COL_WEIGHT)
                SELECT ?, $COL_DATE, CAST($COL_WEIGHT AS REAL) FROM $LEGACY_WEIGHTS_TABLE
                WHERE $COL_DATE IS NOT NULL AND CAST($COL_WEIGHT AS REAL) > 0
                """.trimIndent(),
                arrayOf<Any>(ownerId)
            )

            val legacyGoal = appContext
                .getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
                .getString(LEGACY_PREF_GOAL, null)
                ?.trim()
                ?.toDoubleOrNull()
            if (legacyGoal != null && legacyGoal > 0) {
                db.execSQL(
                    "INSERT OR REPLACE INTO $TABLE_GOALS ($COL_USER_ID, $COL_GOAL_WEIGHT, $COL_PHONE) VALUES (?, ?, NULL)",
                    arrayOf<Any>(ownerId, legacyGoal)
                )
            }
        }

        db.execSQL("DROP TABLE $LEGACY_WEIGHTS_TABLE")
        db.execSQL("DROP TABLE $LEGACY_USERS_TABLE")
    }
}