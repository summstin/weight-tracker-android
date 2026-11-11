package com.example.weighttrackerapp.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_DATE
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_GOAL_WEIGHT
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_ID
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_PASSWORD_HASH
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_PHONE
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_USERNAME
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_USER_ID
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.COL_WEIGHT
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.TABLE_GOALS
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.TABLE_USERS
import com.example.weighttrackerapp.data.DatabaseHelper.Companion.TABLE_WEIGHTS

// SQLite implementation of WeightRepository
class SqliteWeightRepository(private val helper: DatabaseHelper) : WeightRepository {

    override fun createUser(username: String, passwordHash: String): Long? {
        val values = ContentValues().apply {
            put(COL_USERNAME, username)
            put(COL_PASSWORD_HASH, passwordHash)
        }
        val id = helper.writableDatabase.insert(TABLE_USERS, null, values)
        return if (id == -1L) null else id
    }

    override fun findUser(username: String): UserRecord? =
        helper.readableDatabase.query(
            TABLE_USERS,
            arrayOf(COL_ID, COL_USERNAME, COL_PASSWORD_HASH),
            "$COL_USERNAME = ?",
            arrayOf(username),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                UserRecord(cursor.getLong(0), cursor.getString(1), cursor.getString(2))
            } else {
                null
            }
        }

    override fun updatePasswordHash(userId: Long, passwordHash: String) {
        val values = ContentValues().apply { put(COL_PASSWORD_HASH, passwordHash) }
        helper.writableDatabase.update(TABLE_USERS, values, "$COL_ID = ?", arrayOf(userId.toString()))
    }

    override fun addWeight(userId: Long, date: String, weight: Double): Boolean {
        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_DATE, date)
            put(COL_WEIGHT, weight)
        }
        return helper.writableDatabase.insert(TABLE_WEIGHTS, null, values) != -1L
    }

    override fun getWeights(userId: Long): List<WeightEntry> =
        helper.readableDatabase.query(
            TABLE_WEIGHTS,
            arrayOf(COL_ID, COL_DATE, COL_WEIGHT),
            "$COL_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null,
            "$COL_DATE DESC, $COL_ID DESC"
        ).use { cursor ->
            val entries = mutableListOf<WeightEntry>()
            while (cursor.moveToNext()) {
                entries.add(WeightEntry(cursor.getLong(0), cursor.getString(1), cursor.getDouble(2)))
            }
            entries
        }

    // The user id is part of every WHERE clause
    override fun updateWeight(userId: Long, entryId: Long, date: String, weight: Double): Boolean {
        val values = ContentValues().apply {
            put(COL_DATE, date)
            put(COL_WEIGHT, weight)
        }
        val rows = helper.writableDatabase.update(
            TABLE_WEIGHTS, values,
            "$COL_ID = ? AND $COL_USER_ID = ?",
            arrayOf(entryId.toString(), userId.toString())
        )
        return rows > 0
    }

    override fun deleteWeight(userId: Long, entryId: Long): Boolean {
        val rows = helper.writableDatabase.delete(
            TABLE_WEIGHTS,
            "$COL_ID = ? AND $COL_USER_ID = ?",
            arrayOf(entryId.toString(), userId.toString())
        )
        return rows > 0
    }

    override fun getGoal(userId: Long): Goal? =
        helper.readableDatabase.query(
            TABLE_GOALS,
            arrayOf(COL_GOAL_WEIGHT, COL_PHONE),
            "$COL_USER_ID = ?",
            arrayOf(userId.toString()),
            null, null, null
        ).use { cursor ->
            if (cursor.moveToFirst()) {
                Goal(cursor.getDouble(0), if (cursor.isNull(1)) null else cursor.getString(1))
            } else {
                null
            }
        }

    override fun saveGoal(userId: Long, goal: Goal) {
        val values = ContentValues().apply {
            put(COL_USER_ID, userId)
            put(COL_GOAL_WEIGHT, goal.weight)
            put(COL_PHONE, goal.phoneNumber)
        }
        helper.writableDatabase.insertWithOnConflict(TABLE_GOALS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}