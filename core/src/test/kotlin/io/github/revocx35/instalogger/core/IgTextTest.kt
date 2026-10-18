package io.github.revocx35.instalogger.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IgTextTest {
    @Test
    fun linksAndMentions() {
        assertEquals(
            listOf(TextNode.Text("hi "), TextNode.MentionNode("ali.veli", "ali.veli"), TextNode.Text(" see "), TextNode.Link("https://example.com/a", "https://example.com/a"), TextNode.Text(".")),
            IgText.parseInline("hi @ali.veli see https://example.com/a."),
        )
        // An e-mail address is not a mention; www. links become https.
        assertEquals(listOf(TextNode.Text("mail a@b.c or "), TextNode.Link("https://www.x.org/", "www.x.org")), IgText.parseInline("mail a@b.c or www.x.org"))
    }

    @Test
    fun noFormattingAndNoUnsafeLinks() {
        assertEquals(listOf(TextNode.Text("*not bold* javascript:alert(1)")), IgText.parseInline("*not bold* javascript:alert(1)"))
        assertEquals(3, IgText.parse("a\nb\nc").size)
        assertEquals("a b", IgText.strip("  a \n b "))
    }

    @Test
    fun jumbo() {
        assertTrue(IgText.jumboEmoji("😂"))
        assertTrue(IgText.jumboEmoji("👍🏽❤️"))
        assertFalse(IgText.jumboEmoji("😂😂😂😂"))
        assertFalse(IgText.jumboEmoji("ok 😂"))
    }

    @Test
    fun hostileInputIsFast() {
        val t = "@" + "a.".repeat(50_000) + " https://" + "x".repeat(100_000)
        val start = System.nanoTime()
        IgText.parse(t)
        assertTrue((System.nanoTime() - start) / 1_000_000 < 2_000)
    }
}
