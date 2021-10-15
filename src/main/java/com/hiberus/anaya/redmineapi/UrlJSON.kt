package com.hiberus.anaya.redmineapi

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/* ------------------------- Utilities for urls with JSON ------------------------- */

/**
 * Reads JSON data from an url
 * From https://stackoverflow.com/a/4308662
 *
 * @param url url to load
 * @return the json data returned
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun get(url: String): JSONObject =
    URL(url).openStream().use { JSONObject(it.bufferedReader().readText()) }

/**
 * Performs a POST to an url with JSON data
 *
 * @param url    url to post to
 * @param data the json data to post
 * @return the response status code
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun post(url: String, data: JSONObject?): Int = send(url, "POST", data)

/**
 * Performs a PUT to an url with JSON data
 *
 * @param url    url to put to
 * @param data the json data to put
 * @return the response status code
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun put(url: String, data: JSONObject?): Int = send(url, "PUT", data)

/**
 * Performs a DELETE to an url
 *
 * @param url url to delete
 * @return the response status code
 * @throws IOException on network errors
 */
@Throws(IOException::class)
fun delete(url: String): Int = send(url, "DELETE", null)

/* ------------------------- private ------------------------- */

/**
 * performs a 'send' to an url
 */
@Throws(IOException::class)
private fun send(url: String, method: String, body: JSONObject?): Int =
    (URL(url).openConnection() as HttpURLConnection).run {
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