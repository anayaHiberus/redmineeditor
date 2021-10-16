package com.hiberus.anaya.redmineeditor.model

/**
 * Events corresponding to changes in the model
 */
enum class ChangeEvents {

    /**
     * The loading state changed
     */
    Loading,

    /**
     * The displayed month changed
     */
    Month,

    /**
     * The displayed day changed
     */
    Day,

    /**
     * Entries for the displayed day changed (not its content)
     */
    Entries,

    /**
     * Hours from displayed day were changed
     */
    Hours,

    /**
     * List of issues changed
     */
    Issues

}