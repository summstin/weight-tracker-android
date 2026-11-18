package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.data.WeightEntry
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class WeightAnalyticsTest {
    private fun e(id: Long, d: String, w: Double) = WeightEntry(id, d, w)
    private fun pts(vararg w: Double) = w.mapIndexed { i, x ->
        WeightPoint(i.toLong(), LocalDate.of(2026, 1, 1).plusDays(i.toLong()), x) }

    @Test fun clean_empty() {
        val c = WeightAnalytics.clean(emptyList())
        assertTrue(c.points.isEmpty()); assertTrue(c.skipped.isEmpty())
    }
    @Test fun clean_sortsAscendingByRealDate() {
        val c = WeightAnalytics.clean(listOf(e(1, "2026-03-05", 150.0), e(2, "2026-02-10", 151.0)))
        assertEquals(LocalDate.of(2026, 2, 10), c.points.first().date)
    }
    @Test fun clean_duplicateDate_keepsHighestId() {
        val c = WeightAnalytics.clean(listOf(e(5, "2026-01-01", 151.0), e(3, "2026-01-01", 150.0)))
        assertEquals(1, c.points.size); assertEquals(151.0, c.points[0].weight, 0.0)
    }
    @Test fun clean_skipsAndReportsInvalid() {
        val c = WeightAnalytics.clean(listOf(
            e(1, "3/5/2026", 150.0), e(2, "2026-01-02", Double.NaN),
            e(3, "2026-01-03", 0.0), e(4, "2026-01-04", 150.0)))
        assertEquals(1, c.points.size); assertEquals(3, c.skipped.size)
    }
    @Test fun rollingAverage_window3() {
        assertEquals(listOf(10.0, 15.0, 20.0, 30.0),
            WeightAnalytics.rollingAverage(listOf(10.0, 20.0, 30.0, 40.0), 3))
    }
    @Test(expected = IllegalArgumentException::class)
    fun rollingAverage_badWindow() { WeightAnalytics.rollingAverage(listOf(1.0), 0) }

    @Test fun crossing_skippedOverGoal() {
        val c = WeightAnalytics.findCrossings(pts(152.0, 148.0), 150.0)
        assertEquals(1, c.size); assertTrue(c[0].downward)
    }
    @Test fun crossing_goalBelowStart() =
        assertEquals(1, WeightAnalytics.findCrossings(pts(160.0, 155.0, 150.0, 145.0), 150.0).size)
    @Test fun crossing_goalAboveStart() {
        val c = WeightAnalytics.findCrossings(pts(140.0, 145.0, 150.0, 155.0), 150.0)
        assertEquals(1, c.size); assertFalse(c[0].downward)
    }
    @Test fun crossing_startsAtOrStaysAtGoal() =
        assertTrue(WeightAnalytics.findCrossings(pts(150.0, 150.0, 150.0), 150.0).isEmpty())
    @Test fun crossing_multiple() =
        assertEquals(2, WeightAnalytics.findCrossings(pts(160.0, 140.0, 160.0), 150.0).size)
    @Test fun crossing_emptyAndSingle() {
        assertTrue(WeightAnalytics.findCrossings(emptyList(), 150.0).isEmpty())
        assertTrue(WeightAnalytics.findCrossings(pts(150.0), 150.0).isEmpty())
    }
    @Test fun range_inclusiveEdgesAndEmpty() {
        val idx = DateRangeIndex(pts(1.0, 2.0, 3.0, 4.0, 5.0))
        val d = { n: Int -> LocalDate.of(2026, 1, n) }
        assertEquals(3, idx.range(d(2), d(4)).size)
        assertTrue(idx.range(d(20), d(25)).isEmpty())
        assertTrue(idx.range(d(4), d(2)).isEmpty())
        assertEquals(3.0, idx.summarize(d(1), d(5))!!.average, 1e-9)
    }
    @Test fun computeStats_rollingAndLast30() {
        val s = WeightAnalytics.computeStats(
            listOf(e(1, "2026-11-08", 180.0), e(2, "2026-11-07", 182.0), e(3, "2026-09-01", 190.0)),
            LocalDate.of(2026, 11, 8))
        assertEquals(184.0, s.rollingAverage!!, 1e-9)
        assertEquals(2, s.last30Days!!.count); assertEquals(181.0, s.last30Days!!.average, 1e-9)
    }
}