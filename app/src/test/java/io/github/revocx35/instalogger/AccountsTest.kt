package io.github.revocx35.instalogger

import android.content.Context
import io.github.revocx35.instalogger.data.Account
import io.github.revocx35.instalogger.data.Prefs
import io.github.revocx35.instalogger.data.SecureCookieJar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.RuntimeEnvironment

/**
 * The 0.5.0 → 0.6.0 account migration and the account list rules. A single saved server has to become
 * the first account without signing the owner out (its secret file names stay the same).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class AccountsTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    /** Robolectric keeps one data directory for the class: every test starts from empty preferences. */
    @Before
    fun clearPrefs() {
        context.getSharedPreferences("wal_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `the single saved server becomes the first account`() {
        val sp = context.getSharedPreferences("wal_prefs", Context.MODE_PRIVATE)
        sp.edit()
            .putString("server_url", "https://ig.example.com/")
            .putBoolean("allow_http", true)
            .commit()
        val prefs = Prefs(context)

        val migrated = prefs.migrateLegacyAccount()

        assertEquals(Account.MAIN, migrated?.id)
        assertEquals("https://ig.example.com/", migrated?.serverUrl)
        assertTrue(migrated?.allowHttp == true)
        assertEquals(listOf(Account.MAIN), prefs.accounts.value.map { it.id })
        assertEquals(Account.MAIN, prefs.activeAccountId)
        // The legacy keys are gone, and the migration does not run twice.
        assertFalse(sp.contains("server_url"))
        assertFalse(sp.contains("allow_http"))
        assertNull(prefs.migrateLegacyAccount())
        assertEquals(1, prefs.accounts.value.size)
    }

    @Test
    fun `a fresh install has no accounts and never resurrects one`() {
        val prefs = Prefs(context)
        assertNull(prefs.migrateLegacyAccount())
        assertTrue(prefs.accounts.value.isEmpty())
        // The migration is one-shot: a "server_url" written after it ran is ignored (nothing writes it any more).
        context.getSharedPreferences("wal_prefs", Context.MODE_PRIVATE).edit().putString("server_url", "https://late.example.com/").commit()
        assertNull(Prefs(context).migrateLegacyAccount())
    }

    @Test
    fun `trusted certificates move to the migrated server's host`() {
        val cert = byteArrayOf(1, 2, 3, 4)
        context.getSharedPreferences("wal_prefs", Context.MODE_PRIVATE).edit()
            .putString("server_url", "https://ig.example.com/")
            .putStringSet("trusted_certs", setOf(android.util.Base64.encodeToString(cert, android.util.Base64.NO_WRAP)))
            .commit()
        val prefs = Prefs(context)
        prefs.migrateLegacyAccount()

        assertArrayEqualsBytes(cert, prefs.trustedCerts("ig.example.com").firstOrNull())
        assertTrue(prefs.trustedCerts("other.example.com").isEmpty())
    }

    @Test
    fun `accounts keep their order when one is updated`() {
        val prefs = Prefs(context)
        val a = Account("a", "https://a.example.com/", addedAt = 10)
        val b = Account("b", "https://b.example.com/", addedAt = 20)
        prefs.saveAccounts(listOf(a, b))
        prefs.upsertAccount(a.copy(username = "owner"))
        assertEquals(listOf("a", "b"), prefs.accounts.value.map { it.id })
        assertEquals("owner", prefs.accounts.value.first().username)
    }

    @Test
    fun `every account has its own secret file and the migrated one keeps the old name`() {
        assertEquals("session.bin", SecureCookieJar.fileName(Account.MAIN))
        assertEquals("session.bin", SecureCookieJar.fileName(null))
        assertEquals("session-abc-1.bin", SecureCookieJar.fileName("abc-1"))
        assertEquals("session-abc1.bin", SecureCookieJar.fileName("abc/1"))
    }

    @Test
    fun `the menu label is the user name, otherwise host and port`() {
        assertEquals("owner", Account("1", "https://ig.example.com/", username = "owner").label)
        assertEquals("ig.example.com", Account("1", "https://ig.example.com/").label)
        assertEquals("10.0.2.2:18091", Account("2", "http://10.0.2.2:18091/").label)
        assertEquals("ig.example.com:8443", Account("3", "https://ig.example.com:8443/").label)
    }

    private fun assertArrayEqualsBytes(expected: ByteArray, actual: ByteArray?) {
        assertTrue("expected $actual to equal $expected", actual != null && expected.contentEquals(actual))
    }
}
