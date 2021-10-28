package com.hiberus.anaya.redmineeditor.model

/**
 * Events corresponding to 'changes in the model'
 * TODO: make events automatically depend on other events, to avoid registering or notifying multiple
 */
enum class ChangeEvents {

    /**
     * The app start event
     */
    Start,

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
    EntryList,

    /**
     * Content for any entry changed
     */
    EntryContent,

    /**
     * Content for any issue changed
     */
    IssueContent,

    /**
     * Hours from displayed day were changed
     */
    DayHours,

    /**
     * List of issues changed
     */
    IssueList,

}