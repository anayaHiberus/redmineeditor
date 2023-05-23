package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineapi.Issue
import javafx.scene.paint.Color
import java.io.FileNotFoundException
import java.time.LocalDate


/* ------------------------- static ------------------------- */

/**
 * average of a list of colors
 */
val List<Pair<Color, Double>>.average
    get() = reduceOrNull { (accColor, accWeight), (color, weight) -> accColor.interpolate(color, weight / (weight + accWeight)) to weight + accWeight }?.first

/**
 * replaces the opacity of a color
 */
fun Color.withOpacity(newOpacity: Double) = Color(red, green, blue, newOpacity)

/**
 * multiplies the opacity of a color
 */
fun Color.multiplyOpacity(opacityFactor: Double) = withOpacity(opacity * opacityFactor)


/* ------------------------- issues ------------------------- */

/**
 * the custom color of an issue, null if default
 * cached function
 */
val Issue.color
    get() = CACHE.getOrPut(project) {
        PROJECTS.firstOrNull { (regex, _) -> regex.matches(project) }?.second
    }

/**
 * Calculates the color based on the day, and hours
 *
 * @param expected expected hours that day, probably from [expectedHours]
 * @param spent    spent hours that day
 * @param day      the day
 * @return the color of that day (null for no color)
 */
fun getColor(expected: Double, spent: Double, day: LocalDate) = when {
    // something to spend, and correctly spent, GOOD!
    expected != 0.0 && expected == spent -> COLORS.getValue("good")
    // nothing to spend and nothing spent, HOLIDAY!
    expected == 0.0 && spent == 0.0 -> COLORS.getValue("holiday")
    // spent greater than expected, ERROR!
    spent > expected -> COLORS.getValue("spend_error")
    // today, but still not all, WARNING!
    day == LocalDate.now() -> COLORS.getValue("warning")
    // past day and not all, ERROR!
    day.isBefore(LocalDate.now()) -> COLORS.getValue("past_error")
    // future day, and something (not all) spent, IN PROGRESS
    spent > 0 -> COLORS.getValue("good").desaturate()
    // future day, NOTHING!
    else -> null // (null = no color)
}

/**
 * Load colors from the configuration file
 */
fun LoadColors() = runCatching {
    // clear first
    PROJECTS.clear()
    COLORS.clear()
    CACHE.clear()

    var valid = true

    // get file
    (getRelativeFile("conf/colors.properties") ?: throw FileNotFoundException("conf/colors.properties"))
        // parse lines
        .useLines { line ->
            // remove comments
            line.map { it.replace("#.*".toRegex(), "") }
                // extract key=value
                .map { it.split('=', limit = 2) }
                .filter { it.size == 2 }
                .map { it.map { it.trim() } }
                .forEach { (k, v) ->
                    runCatching {
                        Regex("project\\.\"(.*)\"").matchEntire(k)?.let {
                            // a project
                            val project = it.groupValues[1]
                            debugln("Color project found: '$project' = $v")
                            PROJECTS.add(Regex(project) to Color.web(v))
                        } ?: run {
                            // basic color
                            debugln("Color found: $k = $v")
                            COLORS[k] = Color.web(v)
                        }
                    }.onFailure { valid = false }
                }
        }

    valid
}.onFailure {
    debugln(it)
}.getOrDefault(false)


/* ------------------------- containers ------------------------- */

private val PROJECTS = mutableListOf<Pair<Regex, Color>>()
private val COLORS = mutableMapOf<String, Color>().withDefault { Color.GREY }
private val CACHE = mutableMapOf<String, Color?>()
