package ethereum.metrics

import kotlin.system.measureTimeMillis
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface Metrics {
    companion object {
        var enabled = false
    }
}

interface TimeMetrics {
    fun <R> measure(function: () -> R): R

    class Default : TimeMetrics {
        private var time: Duration? = null
        override fun <R> measure(function: () -> R): R {
            val result: R
            time = measureTimeMillis { result = function() }.milliseconds
            return result
        }
    }

    object NoOp : TimeMetrics {
        override fun <R> measure(function: () -> R): R = function()
    }

    companion object : () -> TimeMetrics {
        override fun invoke(): TimeMetrics {
            if (Metrics.enabled) return Default()
            return NoOp
        }
    }
}