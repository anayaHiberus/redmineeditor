# Configuration of hours
## Format:
`year month day hours`
where: 
 - `year` = int (that year)
 - `month` = int (that month, 1=january)
 - `day` = int or string (if number, that day. If string, day of week [m,t,w,th,f,sa,su])
 - `hours` = float (number of hours, 0 if omitted)
Write '*' to mean 'all' (except on hours)

## Modifiers:
Write "<= year month day" to only apply the matching for dates before (and including) that one
Write ">= year month day" to only apply the matching for dates after (and including) that one
Note: >= must always be after <=

Days are matched from bottom to top, last line is the most important, first line the most generic
If a day doesn't match any line, is considered as 0h

## Examples:
- First of january of 1997, 7 hours: `1997 1 1 7`
- All fridays, 5 hours: `* * f 5`
- Third day of each month of 2020, 0 hours: `2020 * 3`
- All days, 3 hours, after 2021: `* * * 3 >= 2021 1 1`
- No hours before 2020: `* * * <= 2019 12 31`
- 1 hour all februaries on the 21 century: `* 2 * 1 >= 2001 1 1 <= 2100 12 31`

Empty lines are ignored, anything after a '#' is ignored too.