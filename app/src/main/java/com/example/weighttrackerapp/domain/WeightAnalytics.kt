package com.example.weighttrackerapp.domain

import com.example.weighttrackerapp.data.WeightEntry
import java.time.LocalDate

data class WeightPoint(val id: Long, val date: LocalDate, val weight: Double)
data class Skipped(val id: Long, val reason: String)
data class CleanResult(val points: List<WeightPoint>, val skipped: List<Skipped>)
data class Crossing(val date: LocalDate, val downward: Boolean)
data class RangeSummary(val count: Int, val min: Double, val max: Double, val average: Double)
data class WeightStats(val rollingAverage: Double?, val last30Days: RangeSummary?, val skipped: Int)

object WeightAnalytics {

    // parse dates, skip and report bad rows, keep the highest id per date, sort ascending
    fun clean(entries: List<WeightEntry>): CleanResult {
        val skipped = ArrayList<Skipped>()
        val byDate = HashMap<LocalDate, WeightPoint>()
        for (e in entries) {
            val date = runCatching { LocalDate.parse(e.date.trim()) }.getOrNull()
            if (date == null) { skipped.add(Skipped(e.id, "bad date '${e.date}'")); continue }
            if (!e.weight.isFinite() || e.weight <= 0.0) {
                skipped.add(Skipped(e.id, "bad weight ${e.weight}")); continue
            }
            val existing = byDate[date]
            if (existing == null || e.id > existing.id) byDate[date] = WeightPoint(e.id, date, e.weight)
        }
        return CleanResult(byDate.values.sortedBy { it.date }, skipped)
    }

    // running sum: add the new value, subtract the one leaving the window
    fun rollingAverage(weights: List<Double>, window: Int): List<Double> {
        require(window > 0) { "window must be positive" }
        val out = ArrayList<Double>(weights.size)
        var sum = 0.0
        for (i in weights.indices) {
            sum += weights[i]
            if (i >= window) sum -= weights[i - window]
            out.add(sum / minOf(i + 1, window))
        }
        return out
    }

    // every consecutive pair that straddles the goal. Input must be date-sorted
    fun findCrossings(
        points: List<WeightPoint>, goal: Double, tolerance: Double = GoalTracker.TOLERANCE
    ): List<Crossing> {
        val out = ArrayList<Crossing>()
        for (i in 1 until points.size) {
            val prev = points[i - 1].weight
            val cur = points[i].weight
            val down = prev > goal + tolerance && cur <= goal + tolerance   // above to at-or-below
            val up = prev < goal - tolerance && cur >= goal - tolerance     // below to at-or-above
            if (down || up) out.add(Crossing(points[i].date, down))
        }
        return out
    }

    fun summarize(points: List<WeightPoint>): RangeSummary? {
        if (points.isEmpty()) return null
        var min = Double.MAX_VALUE; var max = -Double.MAX_VALUE; var sum = 0.0
        for (p in points) {
            if (p.weight < min) min = p.weight
            if (p.weight > max) max = p.weight
            sum += p.weight
        }
        return RangeSummary(points.size, min, max, sum / points.size)
    }

    fun computeStats(entries: List<WeightEntry>, today: LocalDate, window: Int = 7): WeightStats {
        val clean = clean(entries)
        val avg = rollingAverage(clean.points.map { it.weight }, window).lastOrNull()
        val last30 = DateRangeIndex(clean.points).summarize(today.minusDays(29), today)
        return WeightStats(avg, last30, clean.skipped.size)
    }
}

// approach 1: sorted list plus binary search. Input must come from clean()
class DateRangeIndex(private val sorted: List<WeightPoint>) {
    fun range(from: LocalDate, to: LocalDate): List<WeightPoint> {
        if (from.isAfter(to)) return emptyList()
        return sorted.subList(firstIndex { !it.isBefore(from) }, firstIndex { it.isAfter(to) })
    }
    fun summarize(from: LocalDate, to: LocalDate) = WeightAnalytics.summarize(range(from, to))

    // first index whose date satisfies test (false...false, true...true)
    private fun firstIndex(test: (LocalDate) -> Boolean): Int {
        var lo = 0; var hi = sorted.size
        while (lo < hi) {
            val mid = (lo + hi) ushr 1
            if (test(sorted[mid].date)) hi = mid else lo = mid + 1
        }
        return lo
    }
}