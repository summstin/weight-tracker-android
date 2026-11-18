package com.example.weighttrackerapp

import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.weighttrackerapp.data.WeightEntry
import com.example.weighttrackerapp.domain.DateRangeIndex
import com.example.weighttrackerapp.domain.WeightAnalytics
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import kotlin.random.Random

@RunWith(AndroidJUnit4::class)
class RangeBenchmarkTest {
    private val sql = "SELECT COUNT(*), MIN(weight), MAX(weight), AVG(weight) " +
            "FROM weights WHERE user_id = ? AND date >= ? AND date <= ?"

    @Test fun compareApproaches() {
        val ctx = InstrumentationRegistry.getInstrumentation().targetContext
        val rnd = Random(42)
        val base = LocalDate.of(1900, 1, 1)
        for (n in listOf(1_000, 10_000, 100_000)) {
            val file = ctx.getDatabasePath("bench_$n.db")
            file.parentFile?.mkdirs(); file.delete()
            val db = SQLiteDatabase.openOrCreateDatabase(file, null)
            db.execSQL("CREATE TABLE weights (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "user_id INTEGER NOT NULL, date TEXT NOT NULL, weight REAL NOT NULL)")
            db.execSQL("CREATE INDEX idx_weights_user_date ON weights(user_id, date)")
            db.beginTransaction()
            for (i in 0 until n) db.execSQL(
                "INSERT INTO weights(user_id, date, weight) VALUES (1, ?, ?)",
                arrayOf<Any>(base.plusDays(i.toLong()).toString(), Math.round((150 + rnd.nextDouble() * 50) * 10) / 10.0))
            db.setTransactionSuccessful(); db.endTransaction()

            val q = 200
            val ranges = List(q) { val s = base.plusDays(rnd.nextInt(n - 30).toLong()); s to s.plusDays(29) }

            // approach 1: load once, clean and sort once, binary search per query
            val t0 = System.nanoTime()
            val entries = db.rawQuery(
                "SELECT id, date, weight FROM weights WHERE user_id = 1 ORDER BY date DESC, id DESC", null
            ).use { c ->
                val l = ArrayList<WeightEntry>()
                while (c.moveToNext()) l.add(WeightEntry(c.getLong(0), c.getString(1), c.getDouble(2)))
                l
            }
            val index = DateRangeIndex(WeightAnalytics.clean(entries).points)
            val setupMs = (System.nanoTime() - t0) / 1e6
            for ((a, b) in ranges.take(50)) index.summarize(a, b)               // warm-up
            var memCount = 0
            val t1 = System.nanoTime()
            for ((a, b) in ranges) memCount += index.summarize(a, b)?.count ?: 0
            val memUs = (System.nanoTime() - t1) / 1e3 / q

            // approach 2: ask SQLite for just the range
            fun sqlQuery(a: LocalDate, b: LocalDate): Int =
                db.rawQuery(sql, arrayOf("1", a.toString(), b.toString())).use { it.moveToFirst(); it.getInt(0) }
            for ((a, b) in ranges.take(50)) sqlQuery(a, b)                      // warm-up
            var sqlCount = 0
            val t2 = System.nanoTime()
            for ((a, b) in ranges) sqlCount += sqlQuery(a, b)
            val sqlUs = (System.nanoTime() - t2) / 1e3 / q

            assertEquals(memCount, sqlCount)
            Log.i("BENCH", "n=$n | memory setup=${"%.1f".format(setupMs)}ms, " +
                    "${"%.1f".format(memUs)}us/query | sqlite ${"%.1f".format(sqlUs)}us/query")
            db.close(); file.delete()
        }
    }
}