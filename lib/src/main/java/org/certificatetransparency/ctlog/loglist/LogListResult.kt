package org.certificatetransparency.ctlog.loglist

sealed class LogListResult {
    /**
     * Class representing log list loading successful
     */
    data class Valid(val servers: List<LogServer>) : LogListResult()

    /**
     * Abstract class representing log list loading failed
     */
    abstract class Invalid : LogListResult()
}
