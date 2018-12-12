package com.dexterous.flutterlocalnotifications.utils

object StringUtils {
    fun isNullOrEmpty(string: String?): Boolean {
        return string == null || string.isEmpty()
    }
}
