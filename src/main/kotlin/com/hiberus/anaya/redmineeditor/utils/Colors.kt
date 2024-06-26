package com.hiberus.anaya.redmineeditor.utils

import com.hiberus.anaya.redmineapi.Issue
import javafx.scene.control.Alert
import javafx.scene.paint.Color
import java.io.FileNotFoundException
import java.security.InvalidParameterException
import java.time.LocalDate


/* ------------------------- static ------------------------- */

/** average of a list of colors */
val List<Pair<Color, Double>>.average
    get() = reduceOrNull { (accColor, accWeight), (color, weight) -> accColor.interpolate(color, weight / (weight + accWeight)) to weight + accWeight }?.first

/** replaces the opacity of a color */
fun Color.withOpacity(newOpacity: Double) = Color(red, green, blue, newOpacity)

/** multiplies the opacity of a color */
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
    expected != 0.0 && expected == spent -> Colors.GOOD.value
    // nothing to spend and nothing spent, HOLIDAY!
    expected == 0.0 && spent == 0.0 -> Colors.HOLIDAY.value
    // spent greater than expected, ERROR!
    spent > expected -> Colors.SPEND_ERROR.value
    // today, but still not all, WARNING!
    day == LocalDate.now() -> Colors.WARNING.value
    // past day and not all, ERROR!
    day.isBefore(LocalDate.now()) -> Colors.PAST_ERROR.value
    // Need to spent less than 8 hours and nothing spent, future intensive day!
    expected < 8.0 && spent == 0.0 -> Colors.INTENSIVE.value
    // future day, and something (not all) spent, IN PROGRESS
    spent > 0 -> Colors.GOOD.value.multiplyOpacity(0.25)
    // future day, NOTHING!
    else -> null // (null = no color)
}

/** Load colors from the configuration file */
fun LoadColors() = runCatching {
    // clear first
    PROJECTS.clear()
    COLORS.clear()
    CACHE.clear()

    // get file
    (colorsFile ?: throw FileNotFoundException("conf/colors.properties"))
        // parse lines
        .useLines { lines ->
            // remove comments
            lines.map { it.replace("#.*".toRegex(), "") }
                // skip empty
                .filter { it.isNotBlank() }
                .mapNotNull { line ->
                    runCatching {

                        // extract key=value
                        val parts = line.split('=', limit = 2)
                        if (parts.size != 2) throw InvalidParameterException("Lines must be in the format key=value.")
                        val (k, v) = parts.map { it.trim() }

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

                        null
                    }.getOrElse { exception ->
                        debugln("Error reading colors file '$colorsFile' line '$line': $exception")
                        exception.debugPrintStackTrace()
                        "Error on line: '$line': ${exception.message}"
                    }
                }.toList()
        }.takeIf { it.isNotEmpty() }?.joinToString("\n")
}.getOrElse { exception ->
    debugln("Error reading colors file '$colorsFile': $exception")
    exception.debugPrintStackTrace()
    "Generic error: ${exception.message}"
}

/** the colors file */
private val colorsFile = getRelativeFile("conf/colors.properties")

/** Opens the colors file in an external app */
fun OpenColorsFile() = (colorsFile?.openInApp() ?: false)
    .ifNotOK { Alert(Alert.AlertType.ERROR, "Can't open colors file").showAndWait() }

/* ------------------------- containers ------------------------- */

private val PROJECTS = mutableListOf<Pair<Regex, Color>>()
private val COLORS = mutableMapOf<String, Color>().withDefault { Color.TRANSPARENT }
private val CACHE = mutableMapOf<String, Color?>()

enum class Colors {
    GOOD,
    WARNING,
    INTENSIVE,
    PAST_ERROR,
    SPEND_ERROR,
    HOLIDAY,
    MARK_USED,
    MARK_UNUSED
    ;

    val value get() = COLORS.getValue(name.lowercase())
    val nonTransparentValue get() = value.takeUnless { it == Color.TRANSPARENT }
}