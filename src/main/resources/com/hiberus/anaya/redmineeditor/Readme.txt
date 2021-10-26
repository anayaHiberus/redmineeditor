Calendar:
- Choose a month by clicking the '<' and '>' buttons
- Choose a day by clicking it
- The month name is displayed with the spent/total hours
- Days/Months are colored based on its status:
    * green = good spent hours
    * orange = wrong spent hours for today
    * red = wrong spent hours
    * grey = holiday

Summary:
- Details of the selected day or other messages
- Colored the same as the day in calendar

Entries:
- For the selected day, the following entries are displayed:
    * entries with spent hours
    * entries of the past n days, configurable (see configuration)
    * entries assigned to you
- For each entry the following information is displayed:
    - Issue information: project, label and assignation. Press it to display the issue description and/or open it on the external browser.
    - Issue estimated hours: editable.
    - Issue total spent hours: sum of all spent hours in all entries of the issue, and percentage total/estimated (unless invalid). You need to press the '*' button to load them unless automatic (see configuration)
    - Issue realization hours: editable. You can also press the '>' button to set it to total/estimated value.
    - Entry spent hours: editable.
    - Entry comment: editable.

Insertion:
- Choose one of the already loaded issues to create a new entry.
- Enter the url (or its id) of one or multiple issues to load and add them.

Actions:
- Reload: press it to fetch data from Redmine
- Save: press it to upload modified data to Redmine. Reloads afterwards.


Configuration:
- See 'settings.properties' file in 'conf' folder