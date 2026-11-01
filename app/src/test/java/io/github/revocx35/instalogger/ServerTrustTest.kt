package io.github.revocx35.instalogger

import android.content.Context
import io.github.revocx35.instalogger.data.Prefs
import io.github.revocx35.instalogger.data.ServerTrust
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Certificates are trusted per server host: a CA imported for a new server must not widen another account's trust. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ServerTrustTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private val pem = """
-----BEGIN CERTIFICATE-----
MIIBjTCCATOgAwIBAgIUBIkJaof0iM5o6TeuXpQvdY0OhogwCgYIKoZIzj0EAwIw
GzEZMBcGA1UEAwwQdGVzdC5leGFtcGxlLmNvbTAgFw0yNjExMDExMzEwNDBaGA8y
MTI2MTAwODEzMTA0MFowGzEZMBcGA1UEAwwQdGVzdC5leGFtcGxlLmNvbTBZMBMG
ByqGSM49AgEGCCqGSM49AwEHA0IABIr1ECKXotEu2QLuGgpJovP8Ry/zZAMcAtkg
P3eNcvT0tY5/dDtftmKl7UtfqeWHOiWpCaipZcBETTNTz1D8d0WjUzBRMB0GA1Ud
DgQWBBTtk1fyyVUf/zZfAnovUU2gbPDfCDAfBgNVHSMEGDAWgBTtk1fyyVUf/zZf
AnovUU2gbPDfCDAPBgNVHRMBAf8EBTADAQH/MAoGCCqGSM49BAMCA0gAMEUCIGeo
flno5qL4PpVNc8sAi8vV3Jg38MfCFaTaOxtBPfcWAiEAz3GJ/hRlykVywjuM4uHO
/h54ClI0qwOI+su55mpVpk0=
-----END CERTIFICATE-----
""".trimIndent()

    @Before
    fun clearPrefs() {
        context.getSharedPreferences("wal_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @Test
    fun `a certificate imported before connecting is trusted for the new host only`() {
        val prefs = Prefs(context)
        val trust = ServerTrust(prefs)
        val cert = ServerTrust.parseCertificate(pem.toByteArray())
        assertNotNull(cert)

        trust.serverHost = "old.example.com" // the account that is open while another server is being added
        trust.trustFor("New.Example.com", cert!!)

        assertEquals("new.example.com", trust.serverHost)
        assertEquals(1, trust.trusted().size)
        assertEquals(1, prefs.trustedCerts("new.example.com").size)
        assertTrue(prefs.trustedCerts("old.example.com").isEmpty())
    }

    @Test
    fun `plain add still needs a host`() {
        val prefs = Prefs(context)
        val trust = ServerTrust(prefs)
        trust.add(ServerTrust.parseCertificate(pem.toByteArray())!!)
        assertTrue(trust.trusted().isEmpty())
    }
}
