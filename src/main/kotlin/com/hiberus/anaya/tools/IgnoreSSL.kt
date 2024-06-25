package com.hiberus.anaya.tools

import com.hiberus.anaya.redmineeditor.commandline.Command
import javafx.application.Application
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/** Command to ignore SSL errors */
class IgnoreSSLErrorsCommand : Command {
    override val name = "Ignore SSL errors [WARNING! THIS IS DANGEROUS]"
    override val argument = "-ignoreSSLErrors"
    override val skipUI = false // don't skip

    override val help = listOf(
        "[WARNING! THIS IS DANGEROUS]",
        "This will ignore SSL errors, which means that invalid certificates will be ignored.",
        "You may be connecting to a man-in-the-middle server!",
        "But maybe that's what you want, since some business VPNs are...that",
    )

    override fun run(parameters: Application.Parameters) = IgnoreSSLErrors()
}

/** Create all-trusting host name verifier. All hosts will be valid. */
fun IgnoreSSLErrors() {
    runCatching {
        HttpsURLConnection.setDefaultSSLSocketFactory(SSLContext.getInstance("SSL").apply {
            init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                override fun checkClientTrusted(arg0: Array<X509Certificate?>?, arg1: String?) {}
                override fun checkServerTrusted(arg0: Array<X509Certificate?>?, arg1: String?) {}
            }
            ), SecureRandom())
        }.socketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier { _, _ -> true }
        println("WARNING! SSL errors will been ignored")
    }
}