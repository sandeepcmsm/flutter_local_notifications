package com.dexterous.flutterlocalnotifications.models

class Time {

    var hour: Int? = 0
    var minute: Int? = 0
    var second: Int? = 0

    companion object {
        private val HOUR = "hour"
        private val MINUTE = "minute"
        private val SECOND = "second"

        fun from(arguments: Map<String, Any>): Time {
            val time = Time()
            time.hour = arguments[HOUR] as Int
            time.minute = arguments[MINUTE] as Int
            time.second = arguments[SECOND] as Int
            return time
        }
    }
}