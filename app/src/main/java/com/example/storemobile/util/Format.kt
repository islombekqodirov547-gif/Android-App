package com.example.storemobile.util

import java.text.SimpleDateFormat
import java.util.Locale

object Format {

    /** 1234567.0 -> "1 234 567" (space-grouped, no decimals). */
    fun money(value: Double): String {
        val rounded = Math.round(value)
        val sb = StringBuilder(rounded.toString())
        val negative = sb.startsWith("-")
        if (negative) sb.deleteCharAt(0)
        var i = sb.length - 3
        while (i > 0) {
            sb.insert(i, ' ')
            i -= 3
        }
        if (negative) sb.insert(0, '-')
        return sb.toString()
    }

    fun sum(value: Double): String = money(value) + " so'm"

    /** Parses the ISO date the API returns and shows "dd.MM.yyyy HH:mm". */
    fun dateTime(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val input = iso.substringBefore('.').replace("Z", "")
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(input) ?: return iso
            SimpleDateFormat("dd.MM.yyyy  HH:mm", Locale.US).format(date)
        } catch (e: Exception) {
            iso
        }
    }

    fun timeOnly(iso: String): String {
        if (iso.isBlank()) return ""
        return try {
            val input = iso.substringBefore('.').replace("Z", "")
            val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
            val date = parser.parse(input) ?: return ""
            SimpleDateFormat("HH:mm", Locale.US).format(date)
        } catch (e: Exception) {
            ""
        }
    }
}
