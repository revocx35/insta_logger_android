package io.github.revocx35.instalogger.core

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.text.BreakIterator

/*
 * Instagram message text → a node tree, ported from insta_logger's `web/src/lib/igText.tsx` (keep them in sync).
 * Instagram has no inline formatting: only http(s) links and @usernames become nodes; line breaks are kept (one
 * block per line). The UI turns nodes into styled text, never into markup. Indexing is by UTF-16 code unit, like
 * the JavaScript original.
 */

sealed interface TextNode {
    data class Text(val v: String) : TextNode
    data class Link(val href: String, val v: String) : TextNode
    /** An @username (without the @ in [name]). */
    data class MentionNode(val id: String, val name: String) : TextNode
}

sealed interface TextBlock {
    data class Line(val c: List<TextNode>) : TextBlock
}

object IgText {
    /** Beyond this length a message is shown as plain text (keeps rendering cheap). */
    const val MAX_FORMAT_CHARS = 20_000

    // JavaScript's \s (Unicode whitespace), used where the original regexes use \s.
    private const val JS_SPACE = "\\t\\n\\u000B\\f\\r \\u00a0\\u1680\\u2000-\\u200a\\u2028\\u2029\\u202f\\u205f\\u3000\\ufeff"

    // JS: /\b(?:https?:\/\/|www\.)[^\s<>"'`]+[^\s<>"'`.,;:!?)\]}]/gi  (\b is ASCII in non-unicode JS regexes)
    private val URL_RE = Regex(
        "(?<![A-Za-z0-9_])(?:https?://|www\\.)[^$JS_SPACE<>\"'`]+[^$JS_SPACE<>\"'`.,;:!?)\\]}]",
        RegexOption.IGNORE_CASE,
    )

    // JS: /(^|[^\w.])@([A-Za-z0-9._]{1,30})/g
    private val MENTION_RE = Regex("(^|[^A-Za-z0-9_.])@([A-Za-z0-9._]{1,30})")

    private fun isJsSpace(cp: Int): Boolean = when (cp) {
        0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x20, 0xA0, 0x1680, 0x2028, 0x2029, 0x202F, 0x205F, 0x3000, 0xFEFF -> true
        in 0x2000..0x200A -> true
        else -> false
    }

    /** Only http(s) links, normalized; anything else (javascript:, data:, …) is never linked. */
    fun safeHref(raw: String): String? {
        val href = if (raw.length >= 4 && raw.regionMatches(0, "www.", 0, 4, ignoreCase = true)) "https://$raw" else raw
        val url = href.toHttpUrlOrNull() ?: return null
        if (url.scheme != "https" && url.scheme != "http") return null
        return url.toString()
    }

    private fun mentions(text: String): List<TextNode> {
        val out = ArrayList<TextNode>()
        var last = 0
        for (m in MENTION_RE.findAll(text)) {
            val at = m.range.first + m.groupValues[1].length
            if (at > last) out += TextNode.Text(text.substring(last, at))
            val name = m.groupValues[2]
            out += TextNode.MentionNode(name, name)
            last = at + 1 + name.length
        }
        if (last < text.length) out += TextNode.Text(text.substring(last))
        return out
    }

    /** Splits one line into plain runs, links and @mentions (linear time). */
    fun parseInline(text: String): List<TextNode> {
        if (text.length > MAX_FORMAT_CHARS) return listOf(TextNode.Text(text))
        val out = ArrayList<TextNode>()
        var last = 0
        for (m in URL_RE.findAll(text)) {
            val href = safeHref(m.value) ?: continue
            if (m.range.first > last) out += mentions(text.substring(last, m.range.first))
            out += TextNode.Link(href, m.value)
            last = m.range.last + 1
        }
        if (last < text.length) out += mentions(text.substring(last))
        return out.filter { it !is TextNode.Text || it.v.isNotEmpty() }
    }

    /** One block per line. */
    fun parse(text: String): List<TextBlock> = text.split('\n').map { TextBlock.Line(parseInline(it)) }

    fun flatten(nodes: List<TextNode>): String = buildString {
        for (n in nodes) {
            when (n) {
                is TextNode.Text -> append(n.v)
                is TextNode.Link -> append(n.v)
                is TextNode.MentionNode -> append('@').append(n.name)
            }
        }
    }

    private val MULTI_SPACE = Regex("[$JS_SPACE]+")

    /** Plain one-line text (for previews and quotes). */
    fun strip(text: String): String = text.replace(MULTI_SPACE, " ").trim()

    /** 1–3 emoji and nothing else: shown large, like Instagram. */
    fun jumboEmoji(text: String): Boolean {
        if (text.isEmpty()) return false
        var i = 0
        while (i < text.length) {
            val cp = text.codePointAt(i)
            val ok = isExtendedPictographic(cp) || cp in 0x1F3FB..0x1F3FF || cp in 0x1F1E6..0x1F1FF ||
                cp == 0x200D || cp == 0xFE0F || isJsSpace(cp) || cp in 0xE0020..0xE007F
            if (!ok) return false
            i += Character.charCount(cp)
        }
        val trimmed = text.trim()
        val it = BreakIterator.getCharacterInstance()
        it.setText(trimmed)
        var count = 0
        while (it.next() != BreakIterator.DONE) {
            count++
            if (count > 3) return false
        }
        return count in 1..3
    }

    private val PICTOGRAPHIC_RANGES = intArrayOf(
        0x00A9, 0x00A9, 0x00AE, 0x00AE, 0x203C, 0x203C, 0x2049, 0x2049, 0x2122, 0x2122, 0x2139, 0x2139,
        0x2194, 0x2199, 0x21A9, 0x21AA, 0x231A, 0x231B, 0x2328, 0x2328, 0x2388, 0x2388, 0x23CF, 0x23CF,
        0x23E9, 0x23F3, 0x23F8, 0x23FA, 0x24C2, 0x24C2, 0x25AA, 0x25AB, 0x25B6, 0x25B6, 0x25C0, 0x25C0,
        0x25FB, 0x25FE, 0x2600, 0x2605, 0x2607, 0x2612, 0x2614, 0x2685, 0x2690, 0x2705, 0x2708, 0x2712,
        0x2714, 0x2714, 0x2716, 0x2716, 0x271D, 0x271D, 0x2721, 0x2721, 0x2728, 0x2728, 0x2733, 0x2734,
        0x2744, 0x2744, 0x2747, 0x2747, 0x274C, 0x274C, 0x274E, 0x274E, 0x2753, 0x2755, 0x2757, 0x2757,
        0x2763, 0x2767, 0x2795, 0x2797, 0x27A1, 0x27A1, 0x27B0, 0x27B0, 0x27BF, 0x27BF, 0x2934, 0x2935,
        0x2B05, 0x2B07, 0x2B1B, 0x2B1C, 0x2B50, 0x2B50, 0x2B55, 0x2B55, 0x3030, 0x3030, 0x303D, 0x303D,
        0x3297, 0x3297, 0x3299, 0x3299, 0x1F000, 0x1F0FF, 0x1F10D, 0x1F10F, 0x1F12F, 0x1F12F,
        0x1F16C, 0x1F171, 0x1F17E, 0x1F17F, 0x1F18E, 0x1F18E, 0x1F191, 0x1F19A, 0x1F1AD, 0x1F1E5,
        0x1F201, 0x1F20F, 0x1F21A, 0x1F21A, 0x1F22F, 0x1F22F, 0x1F232, 0x1F23A, 0x1F23C, 0x1F23F,
        0x1F249, 0x1F3FA, 0x1F400, 0x1F53D, 0x1F546, 0x1F64F, 0x1F680, 0x1F6FF, 0x1F774, 0x1F77F,
        0x1F7D5, 0x1F7FF, 0x1F80C, 0x1F80F, 0x1F848, 0x1F84F, 0x1F85A, 0x1F85F, 0x1F888, 0x1F88F,
        0x1F8AE, 0x1F8FF, 0x1F90C, 0x1F93A, 0x1F93C, 0x1F945, 0x1F947, 0x1FAFF, 0x1FC00, 0x1FFFD,
    )

    /** Unicode `Extended_Pictographic` (emoji-data.txt), without relying on the platform regex engine. */
    fun isExtendedPictographic(cp: Int): Boolean {
        if (cp < 0xA9) return false
        var k = 0
        while (k < PICTOGRAPHIC_RANGES.size) {
            if (cp < PICTOGRAPHIC_RANGES[k]) return false
            if (cp <= PICTOGRAPHIC_RANGES[k + 1]) return true
            k += 2
        }
        return false
    }
}
