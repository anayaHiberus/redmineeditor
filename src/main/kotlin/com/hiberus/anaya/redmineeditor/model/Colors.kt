package com.hiberus.anaya.redmineeditor.model

import com.hiberus.anaya.redmineapi.Issue
import com.hiberus.anaya.redmineeditor.Main
import com.hiberus.anaya.redmineeditor.utils.*
import javafx.scene.control.Alert
import javafx.scene.paint.Color
import java.io.FileNotFoundException
import java.security.InvalidParameterException
import java.time.LocalDate
import java.util.prefs.Preferences


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
    FILE_COLORS.clear()
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
                            FILE_COLORS[k] = Color.web(v)
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
private val FILE_COLORS = mutableMapOf<String, Color>().withDefault { Color.TRANSPARENT }
private val CACHE = mutableMapOf<String, Color?>()


enum class Colors(val description: String) {
    GOOD("The required non-zero hours are the same as the imputed"),
    WARNING("There are missing hours for today"),
    SPEND_ERROR("There are more hours than required"),
    PAST_ERROR("There are missing hours for past days"),
    INTENSIVE("There are missing hours for a future intensive day"),
    HOLIDAY("Holiday day"),
    MARK_USED("In Mark used = Color, entries that have 0 hours"),
    MARK_UNUSED("In Mark used = Color, entries that don't have 0 hours"),
    ;

    var value
        get() = PREFS[prefKey, null]?.let { Color.valueOf(it) } ?: defaultValue
        set(value) {
            if (value != defaultValue) PREFS.put(prefKey, value.toString())
            else PREFS.remove(prefKey) // don't save default
        }
    val nonTransparentValue get() = value.takeUnless { it == Color.TRANSPARENT }
    val defaultValue get() = FILE_COLORS.getValue(name.lowercase())

    private val prefKey = PREF_PREFIX + name.lowercase()
}

/* ------------------------- private ------------------------- */

private const val PREF_PREFIX = "COLOR_"

/** loaded settings preferences */
private val PREFS = Preferences.userNodeForPackage(Main::class.java)