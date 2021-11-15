package com.hiberus.anaya.redmineapi

import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/* ------------------------- Utilities for urls with JSON ------------------------- */

/**
 * Reads JSON data from this url
 *
 * @return the json data returned
 * @throws IOException on network errors or invalid result (not 200-OK)
 */
@Throws(IOException::class) // TODO: replace all of this with async/await?
fun URL.getJSON() = (openConnection() as HttpURLConnection).run {
    if (responseCode != 200) throw IOException("Returned $responseCode: $responseMessage")
    else openStream().use {
        JSONObject(it.bufferedReader().readText())
    }
}

/**
 * POSTs this JSON data to that url
 *
 * @param url    url to post to
 * @return the response status code
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun JSONObject.postTo(url: URL) = url.send("POST", this)

/**
 * PUTs this JSON data to an url
 *
 * @param url    url to put to
 * @return the response status code
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun JSONObject.putTo(url: URL) = url.send("PUT", this)

/**
 * Performs a DELETE to this url
 *
 * @return the response status code
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun URL.delete(): Int = send("DELETE", null)

/* ------------------------- private ------------------------- */

/**
 * performs a 'send' to an url
 */
@Throws(IOException::class)
private fun URL.send(method: String, body: JSONObject?) =
    (openConnection() as HttpURLConnection).run {
        // prepare connection
        requestMethod = method
        body?.let {
            // append body
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            outputStream.write(it.toString().toByteArray(StandardCharsets.UTF_8))
        }

        // connect (call not needed, but easier to follow code flow)
        connect()

        // perform debug
        println("$responseCode: $responseMessage")

        // end
        disconnect()

        // return responseCode
        responseCode
    }

/* ------------------------- utils ------------------------- */

/**
 * Compare value with another one, if different execute function
 */
inline fun <T> T.ifNot(compareTo: T, execute: () -> Unit) {
    if (this != compareTo) execute()
}

/**
 * Map a JSONArray as a list of JSONObject
 */
internal fun JSONArray.mapAsObjects() = List(length()) { i -> getJSONObject(i) }