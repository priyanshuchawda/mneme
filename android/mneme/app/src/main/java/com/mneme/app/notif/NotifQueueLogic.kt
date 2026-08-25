package com.mneme.app.notif

/**
 * Pure queue ops over a JSON array string (SharedPreferences wrapper uses this).
 */
object NotifQueueLogic {
    fun offer(existingJson: String, text: String, max: Int = 12): String {
        val items = parse(existingJson).toMutableList()
        if (items.lastOrNull() == text) return encode(items)
        items.add(text)
        val trimmed = if (items.size > max) items.takeLast(max) else items
        return encode(trimmed)
    }

    fun drain(existingJson: String, limit: Int): Pair<List<String>, String> {
        val items = parse(existingJson)
        if (items.isEmpty()) return emptyList<String>() to "[]"
        val take = minOf(limit, items.size)
        val out = items.take(take)
        val rest = items.drop(take)
        return out to encode(rest)
    }

    fun size(existingJson: String): Int = parse(existingJson).size

    private fun parse(json: String): List<String> {
        val t = json.trim()
        if (t.isEmpty() || t == "[]") return emptyList()
        // Minimal JSON string-array parser (values are plain text we escaped)
        if (!t.startsWith("[") || !t.endsWith("]")) return emptyList()
        val inner = t.substring(1, t.length - 1).trim()
        if (inner.isEmpty()) return emptyList()
        val out = mutableListOf<String>()
        var i = 0
        while (i < inner.length) {
            while (i < inner.length && (inner[i] == ',' || inner[i].isWhitespace())) i++
            if (i >= inner.length) break
            if (inner[i] != '"') break
            i++
            val sb = StringBuilder()
            while (i < inner.length) {
                val c = inner[i]
                when {
                    c == '\\' && i + 1 < inner.length -> {
                        sb.append(inner[i + 1])
                        i += 2
                    }
                    c == '"' -> {
                        i++
                        break
                    }
                    else -> {
                        sb.append(c)
                        i++
                    }
                }
            }
            out.add(sb.toString())
        }
        return out
    }

    private fun encode(items: List<String>): String =
        items.joinToString(prefix = "[", postfix = "]", separator = ",") { "\"${escape(it)}\"" }

    private fun escape(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"")
}
