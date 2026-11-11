package com.example.weighttrackerapp

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

// handle database operations
class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "WeightTracker.db"
        private const val DATABASE_VERSION = 1

        // user Table
        const val TABLE_USERS = "users"
        const val COL_USER_ID = "id"
        const val COL_USERNAME = "username"
        const val COL_PASSWORD = "password"

        // weight table
        const val TABLE_WEIGHTS = "weights"
        const val COL_WEIGHT_ID = "id"
        const val COL_DATE = "date"
        const val COL_WEIGHT = "weight"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COL_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_USERNAME TEXT UNIQUE,
                $COL_PASSWORD TEXT
            )
        """.trimIndent()

        val createWeightsTable = """
            CREATE TABLE $TABLE_WEIGHTS (
                $COL_WEIGHT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_DATE TEXT,
                $COL_WEIGHT TEXT
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createWeightsTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_WEIGHTS")
        onCreate(db)
    }



    // register new user
    fun registerUser(username: String, password: String): Boolean {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD, password)
        }

        val result = db.insert(TABLE_USERS, null, values)
        return result != -1L
    }

    // check login credentials
    fun checkUser(username: String, password: String): Boolean {
        val db = readableDatabase

        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COL_USERNAME=? AND $COL_PASSWORD=?",
            arrayOf(username, password)
        )

        val exists = cursor.count > 0
        cursor.close()

        return exists
    }

    // insert new weight
    fun insertWeight(date: String, weight: String): Boolean {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_DATE, date)
            put(COL_WEIGHT, weight)
        }

        return db.insert(TABLE_WEIGHTS, null, values) != -1L
    }

    // read weights
    fun getAllWeights(): Cursor {
        val db = readableDatabase
        return db.rawQuery("SELECT * FROM $TABLE_WEIGHTS", null)
    }

    // edit weights
    fun updateWeight(id: Int, date: String, weight: String): Boolean {
        val db = writableDatabase

        val values = ContentValues().apply {
            put(COL_DATE, date)
            put(COL_WEIGHT, weight)
        }

        val result = db.update(TABLE_WEIGHTS, values, "id=?", arrayOf(id.toString()))
        return result > 0
    }

    // delete weights
    fun deleteWeight(id: Int): Boolean {
        val db = writableDatabase
        val result = db.delete(TABLE_WEIGHTS, "id=?", arrayOf(id.toString()))
        return result > 0
    }
}