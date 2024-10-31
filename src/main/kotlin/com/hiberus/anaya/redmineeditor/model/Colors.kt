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
    get() = ISSUE_COLOR_CACHE.getOrPut(project) {
        PROJECT_COLORS_FROM_FILE.firstOrNull { it.regex.matches(project) }?.color
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
    // future day, and something (not all) spent, IN PROGRESS
    spent > 0 -> Colors.GOOD.value.multiplyOpacity(0.25)
    // future day, NOTHING!
    else -> null // (null = no color)
}

/** Load colors from the configuration file */
fun LoadColors() = runCatching {
    // clear first
    PROJECT_COLORS_FROM_FILE.clear()
    APP_COLORS_FROM_FILE.clear()
    ISSUE_COLOR_CACHE.clear()

    // get file
    (COLORS_FILE ?: throw FileNotFoundException("conf/colors.properties"))
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
                            PROJECT_COLORS_FROM_FILE.add(ProjectColor(Regex(project), Color.web(v)))
                        } ?: run {
                            // basic color
                            debugln("Color found: $k = $v")
                            APP_COLORS_FROM_FILE[k] = Color.web(v)
                        }

                        null
                    }.getOrElse { exception ->
                        debugln("Error reading colors file '$COLORS_FILE' line '$line': $exception")
                        exception.debugPrintStackTrace()
                        "Error on line: '$line': ${exception.message}"
                    }
                }.toList()
        }.takeIf { it.isNotEmpty() }?.joinToString("\n")
}.getOrElse { exception ->
    debugln("Error reading colors file '$COLORS_FILE': $exception")
    exception.debugPrintStackTrace()
    "Generic error: ${exception.message}"
}

/** Opens the colors file in an external app */
fun OpenColorsFile() = (COLORS_FILE?.openInApp() ?: false)
    .ifNotOK { Alert(Alert.AlertType.ERROR, "Can't open colors file").showAndWait() }

/* ------------------------- enums ------------------------- */

/** App colors, returns the one from the preferences, or the file one. */
enum class Colors(val description: String) {
    GOOD("The required non-zero hours are the same as the imputed"),
    WARNING("There are missing hours for today"),
    SPEND_ERROR("There are more hours than required"),
    PAST_ERROR("There are missing hours for past days"),
    HOLIDAY("Holiday day"),
    INTENSIVE("Intensive day marker color"),
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
    val defaultValue get() = APP_COLORS_FROM_FILE.getValue(name.lowercase())

    private val prefKey = "COLOR_" + name.lowercase()
}

/** A project color: regex to identify the project and the color to apply. */
data class ProjectColor(val regex: Regex, val color: Color, val defaultId: String? = null)

/* ------------------------- private ------------------------- */

/** the colors file */
private val COLORS_FILE = getRelativeFile("conf/colors.properties")

/** List of loaded project colors from the colors file. */
public val PROJECT_COLORS_FROM_FILE = mutableListOf<ProjectColor>()

/** List of loaded app colors from the colors file. */
private val APP_COLORS_FROM_FILE = mutableMapOf<String, Color>().withDefault { Color.TRANSPARENT }

/** Cache for the Issue#color method. */
private val ISSUE_COLOR_CACHE = mutableMapOf<String, Color?>()

/** loaded settings preferences */
private val PREFS = Preferences.userNodeForPackage(Main::class.java)