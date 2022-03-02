Calendar:
- Choose a month by clicking the '<' and '>' buttons
- Choose a day by clicking it
- The month name is displayed with the spent/total hours
- Days/Months are colored based on its status:
    * green = good spent hours
    * orange = wrong spent hours for today
    * red = wrong spent hours
    * grey = holiday
    * blue = in progress (some hours but not all spent)

Summary:
- Details of the selected day or other messages
- Colored the same as the day in calendar

Entries:
- The top-left input textbox allows to filter the displayed lists by issue content. Press the button to clear the filter.
- For the selected day, the following entries are displayed:
    * entries with spent hours
    * entries of the past n days, configurable (see configuration)
    * entries assigned to you
- For each entry the following information is displayed:
    - Issue information: project, label and assignation. Press it to display the issue description and/or open it on the external browser.
    - Issue estimated hours: editable. (click the number to edit raw)
    - Issue total spent hours: sum of all spent hours in all entries of the issue, and percentage total/estimated (unless invalid or not loaded). Colored red when more than 100%
    - '*' button: press to load total spent hours (if not automatic, see configuration). Hidden once loaded.
    - '>' button: set the realization hours to the total/estimated calculation. (hidden when not loaded)
    - Issue realization percentage: editable. Colored orange if less than total/estimated calculation.
    - Entry spent hours: editable. (click the number to edit raw)
    - Entry comment: editable.
- The right-most button allows to:
    * Copy the entry into today (hours and comment)

Insertion:
- Choose one of the already loaded issues to create a new entry.
  If the "Autoload assigned issues" setting is disabled, an entry to load them will be shown at the bottom.
- Enter the url (or its id) of one or multiple issues to load and add them.

Actions:
- Reload: press it to fetch data from Redmine
- Save: press it to upload modified data to Redmine. Asks to exit or reload afterwards.