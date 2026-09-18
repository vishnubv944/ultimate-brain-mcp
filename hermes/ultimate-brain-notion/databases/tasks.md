---
title: Tasks Database — Full Schema Reference
purpose: Complete property reference for the Tasks database — all properties, types, status options, automations, and formulas
agent_relevant: true
key_topics: [tasks, database, schema, properties, status, due, smart-list, priority, context, recurring, subtasks, my-day, automations, time-tracking, sessions]
related_docs: [features/managing-tasks.md, features/recurring-tasks.md, databases/work-sessions.md, features/daily-planning.md]
---
# Tasks

The **Tasks** database stores all of your tasks. It’s the primary database that drives task management in Ultimate Brain. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates and properties in the Tasks database. Properties are in alphabetical order.

**Note:** In versions of Ultimate Brain released prior to November 2024, this database is called **All Tasks.**

## Automations

This section is a reference for all automations that are included in the Tasks database. Each listed automation includes the following information:

-   Description of what the automation does
-   The automation’s default state (some automations are disabled by default)
-   When it was added to Ultimate Brain
-   A guide for building the automation, including screenshots and pastable formulas

Automations included in your template that are **disabled** by default can be easily **enabled**. Simply unlock the database that contains the automation, click the ⚡️ icon near the database’s `•••` menu, right-click the automation, and toggle it **Active.**

**Important:** Automations that are included in Notion templates can be _used_ and _activated/deactivated_ when your workspace is on Notion’s Free plan. However, _creating_ and _editing_ automations requires being on a paid Notion plan. [Learn more in Notion’s official docs](https://www.notion.com/help/database-automations). If you’d like to edit the included automations, or update your template with new ones I release, you’ll need to be on a paid plan. Alternatively, you can [migrate your data to a new copy of the template](https://thomasjfrank.com/docs/ultimate-brain/transfer-guide/) to get newly-released automations – though this can be complex, takes a long time, and isn’t something we can provide support for.

### Change Project → Remove Parent

-   **Default Status:** Disabled
-   **Description:** When you change the Project of a Sub-Task, its Parent task is removed, making it a top-level task in its new Project. This mimics the behavior of other task managers like Todoist, TickTick, and ClickUp.
-   **Released:** [2025-01-21: Time Tracking, Daily Planning Updates, and More!](https://thomasjfrank.com/docs/ultimate-brain/changelog/#time-tracking-daily-planning-updates-and-more)


Parent formula:

CODE

`context("Trigger page").prop("Parent Task")`
Code language: JavaScript (javascript)

### Clear Completion Dates

-   **Default Status:** Disabled
-   **Description:** When a task’s Status is set to To Do or Doing, its Completed value is cleared. This clears completion dates from tasks that are re-opened after being completed.
-   **Released:** [2024-11-28 – Ultimate Brain 3.0 Public Launch](https://thomasjfrank.com/docs/ultimate-brain/changelog/#ultimate-brain-public-launch)


### Recurring Tasks (Simple)

-   **Default Status:** Enabled
-   **Description:** When a recurring task is completed, this sets the Status back to To Do and changes the Due Date to the date displayed in the Next Due property. Learn more about this automation in our [recurring tasks guide](https://thomasjfrank.com/docs/ultimate-brain/recurring-tasks/).
-   **Released:** [2024-11-28 – Ultimate Brain 3.0 Public Launch](https://thomasjfrank.com/docs/ultimate-brain/changelog/#ultimate-brain-public-launch)
-   **Update:** [2026-02-07: Recurring Task Improvements, Notes ↔ Tasks Relation, and More](https://thomasjfrank.com/docs/ultimate-brain/changelog/#recurring-task-improvements-notes-tasks-relation-and-more)


Due formula:

CODE

`context("Trigger page").prop("Next Due")`
Code language: JavaScript (javascript)

### Recurring Tasks (Advanced)

-   **Default Status:** Disabled
-   **Description:** Functions like the Recurring Tasks (Simple) automation, but additionally creates a new task with a Status of Done. This new task acts as a historical record of this instance of the task, thereby enabling Task History. Learn more about this automation in our [recurring tasks guide](https://thomasjfrank.com/docs/ultimate-brain/recurring-tasks/#advanced-recurring-tasks-with-task-history).
-   **Released:** [2024-11-28 – Ultimate Brain 3.0 Public Launch](https://thomasjfrank.com/docs/ultimate-brain/changelog/#ultimate-brain-public-launch)
-   **Update:** [2026-02-07: Recurring Task Improvements, Notes ↔ Tasks Relation, and More](https://thomasjfrank.com/docs/ultimate-brain/changelog/#recurring-task-improvements-notes-tasks-relation-and-more)


Formulas for the historical record page (Add Task action):

Name:

CODE

`context("Trigger page").prop("Name")`
Code language: JavaScript (javascript)

Project:

CODE

`context("Trigger page").prop("Project")`
Code language: JavaScript (javascript)

Due:

CODE

`context("Trigger page").prop("Due")`
Code language: JavaScript (javascript)

Assignee:

CODE

`context("Trigger page").prop("Assignee")`
Code language: JavaScript (javascript)

Labels:

CODE

`context("Trigger page").prop("Labels")`
Code language: JavaScript (javascript)

People:

CODE

`context("Trigger page").prop("People")`
Code language: JavaScript (javascript)

Smart List:

CODE

`context("Trigger page").prop("Smart List")`
Code language: JavaScript (javascript)

Priority:

CODE

`context("Trigger page").prop("Priority")`
Code language: JavaScript (javascript)

My Day:

CODE

`context("Trigger page").prop("My Day")`
Code language: JavaScript (javascript)

Description:

CODE

`"Canonical: " + context("Trigger page")`
Code language: JavaScript (javascript)

Due formula for current page (Set Due to My Value action):

CODE

`context("Trigger page").prop("Next Due")`
Code language: JavaScript (javascript)

Additional formulas:

Time Tracking Sessions variable

CODE

`context("Trigger page").prop("Sessions")`
Code language: JavaScript (javascript)

“Name” property in the Edit Pages step (after Time Tracking Sessions variable step)

CODE

`context("Page added") + " (Session)"`
Code language: JavaScript (javascript)

### Set Completion Dates

-   **Default Status:** Disabled
-   **Description:** When a task is set to Done, sets the Completed date to Time Triggered.
-   **Released:** [2024-11-28 – Ultimate Brain 3.0 Public Launch](https://thomasjfrank.com/docs/ultimate-brain/changelog/#ultimate-brain-public-launch)
-   **Update:** [2026-02-07: Recurring Task Improvements, Notes ↔ Tasks Relation, and More](https://thomasjfrank.com/docs/ultimate-brain/changelog/#recurring-task-improvements-notes-tasks-relation-and-more)


Note: This automation is limited to all pages in the **Non-Recurring (Source)** view.

### Sync Parent/Sub-Item Projects & People

-   **Default Status:** Disabled
-   **Description:** When the Sub-Tasks, Project, or People properties are edited on a task, this automation syncs the task’s Project, People, and Content (UB+CC only) values to its sub-tasks. This automation’s main purpose to ensure sub-tasks have the same Project, People, and Content (UB+CC only) values as their parent task.
-   **Released:** [2025-01-21: Time Tracking, Daily Planning Updates, and More!](https://thomasjfrank.com/docs/ultimate-brain/changelog/#time-tracking-daily-planning-updates-and-more)

In Ultimate Brain + Creator’s Companion, this automation also syncs the related Content page, and triggers if the Content relation is edited.


Sub-Tasks formula:

CODE

`context("Trigger page").prop("Sub-Tasks")`
Code language: JavaScript (javascript)

Project formula:

CODE

`context("Trigger page").prop("Project")`
Code language: JavaScript (javascript)

People formula:

CODE

`context("Trigger page").prop("People")`
Code language: JavaScript (javascript)

### Task Done → Close All Open Sub-Tasks

-   **Default Status:** Disabled
-   **Description:** When a task is set to Done, this automation sets all of its open sub-tasks to Done, and sets their Completed dates to Time Triggered.
-   **Released:** [2025-01-21: Time Tracking, Daily Planning Updates, and More!](https://thomasjfrank.com/docs/ultimate-brain/changelog/#time-tracking-daily-planning-updates-and-more)


Open Sub-Tasks formula:

CODE

`context("Trigger page").prop("Sub-Tasks").filter( 	current.prop("Status") != "Done" and current.prop("Status") != current.prop("Localization Key").last().last() )`
Code language: JavaScript (javascript)

### Task Done → End Active Work Session

-   ****Default Status:**** Enabled
-   **Description:** When a task is set to Done, this automation ends any active Work Session related to it. This ensures time tracking sessions are ended when the task is completed.
-   **Released:** [2025-01-21: Time Tracking, Daily Planning Updates, and More!](https://thomasjfrank.com/docs/ultimate-brain/changelog/#time-tracking-daily-planning-updates-and-more)


Session formula:

CODE

`context("Trigger page").prop("Sessions").filter( 	current.prop("End").empty() )`
Code language: JavaScript (javascript)

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Task with Sub-Tasks

This template lets you easily add sub-tasks to a task. Note that while you can technically apply this template to sub-task pages themselves (effectively creating sub-sub-tasks), Ultimate Brain really only supports a single level of sub-tasks due to core Notion limitations.

### Recurring Task w/ History

This template works along with our [Advanced Recurring Tasks](https://thomasjfrank.com/docs/ultimate-brain/recurring-tasks/#advanced-recurring-tasks-with-task-history) automation to show you each time you’ve completed a recurring task in the past. This template essentially enables light habit-tracking in Ultimate Brain (though Notion Automations still has limitations preventing us from easily seeing _skipped_ instances of a habit).

### Time-Tracked Task

This template includes views of the [Work Sessions](https://thomasjfrank.com/docs/ultimate-brain/databases/work-sessions/) database, showing any time-tracking sessions associated with the task.

### Packing List

This simple template shows how you can use To Do blocks to create a re-usable checklist for travel purposes.

## Properties

### Assignee

**Type:** People

Use this property to set assignees for each task.

Note that Ultimate Brain is set up for individual use by default; to make this property useful, you’ll want to create filtered views that only show tasks assigned to specific people.

### Completed

**Type:** Date

The date on which this task was completed.

### Created

**Type:** Created Time

The date and time the task was created.

### Created By

**Type:** Created By

The user (or integration) who created the task.

### Current Session

**Type:** Formula

The current active Work Session related to this task (if it is currently active and being tracked).

CODE

`prop("Sessions").filter(   current.prop("End").empty() )`
Code language: JavaScript (javascript)

### Days

**Type:** Multi Select

Allows you to pick specific days of the week – e.g. M/W/F. Works in 2 scenarios:

Recur Unit = Day(s) and Recur Interval = 1. Allows for schedules like “M/W/F”

Recur Unit = Nth Weekday of Month and Days has ONE value. Allows for schedules like “Every 3rd Monday of the month”

**Note:** Prior to **January 20, 2025**, this property was called **Days (Only if Set to 1 Day(s)).**

| Name | Description |
| --- | --- |
| Monday | N/A |
| Tuesday | N/A |
| Wednesday | N/A |
| Thursday | N/A |
| Friday | N/A |
| Saturday | N/A |
| Sunday | N/A |

### Description

**Type:** Rich Text

A simple text property for adding a note about this task.

If you’ve added the Notion AI add-on to your workspace, you can turn AI Autofill on to generate a summary of the task’s details (provided there’s a full description in the task’s page body).

### Due

**Type:** Date

The due date for the task.

Note that most filters in Ultimate Brain filter against the End Date of Due. This doesn’t affect pages with a single date, but does come into play if you set a start and end date.

### Due Stamp (Parent)

**Type:** Formula

If a task is sub-task, this returns the Due Timestamp value of its parent task. Otherwise, it simply returns this task’s own Due Timestamp value.

This property is the highest-priority sorting criteria for Task views within Projects. It keeps sub-tasks beneath their parent tasks.

CODE

`!prop("Parent Task")   ? prop("Due Timestamp")  : prop("Parent Task").first().prop("Due Timestamp")`
Code language: JavaScript (javascript)

### Due Timestamp

**Type:** Formula

Used to aid in correctly sorting tasks and sub-tasks within Projects.

If the task doesn’t have a Due date set, this timestamp will be 100 years in the future. This simply keeps tasks/sub-tasks without a due date below tasks that have one.

CODE

`!prop("Due")   ? now().dateAdd(100, "years").timestamp()  : prop("Due").timestamp()`
Code language: JavaScript (javascript)

### Edited

**Type:** Last Edited Time

The date and time the task was last edited.

### Edited By

**Type:** Last Edited By

The user (or integration) who last edited the task.

### Edited Stamp (Parent)

**Type:** Formula

Used for sorting sub-tasks underneath their parent tasks in the Process → Task Intake view.

If the task is a sub-task, this returns the last edited time of its parent as a timestamp. Otherwise, it returns this task’s own last edited time as a timestamp.

CODE

`!prop("Parent Task")   ? prop("Edited").timestamp()  : prop("Parent Task").first().prop("Edited").timestamp()`
Code language: JavaScript (javascript)

### End

**Type:** Button

Ends the current Work Session related to this task, if one is active.


To configure the End button, first create a Define Variables action and name the variable Active Session. Type out the following formula **manually**:

CODE

`This Page.Sessions.find(   current.prop("End").empty() and current.prop("Team Member").includes(Whoever clicked) )`
Code language: JavaScript (javascript)

Next, create an **Edit Pages In** step. In the database picker, choose your **Active Session** variable.

This will allow your button to edit pages within the _list_ of pages returned by the formula, rather than needing a database and filter combo!

In this step, set **End** to **Time triggered.**

Click Save.

### Enforce Schedule

**Type:** Checkbox

Forces the Next Due date to be the next date in the recurring sequence, even if the current task is overdue.

Example: If a daily recurring task was due Monday, and it’s currently Thursday, Next Due date will be Friday. However, if Enforce Schedule is checked, Next Due will be Tuesday.

We added this feature in [February 2026](https://thomasjfrank.com/docs/ultimate-brain/changelog/#recurring-task-improvements-notes-tasks-relation-and-more) because users told us that some recurring tasks _must_ adhere to a schedule, even if one instance becomes overdue. In the case that a task becomes overdue, it may need to be completed multiple times in order to be “caught up”. This option enables this type of schedule.

### Label

**Type:** Multi Select

This property was previously called “Tag” in Ultimate Brain 3.0 prior to December 6, 2024. It was renamed on that date to prevent confusion with the [Tags](https://thomasjfrank.com/docs/ultimate-brain/databases/tags/) database, since Ultimate Brain doesn’t ship with a relation between this Tasks database and the Tags database by default.

Allows you to add labels to tasks, which will allow you to create label-based Board views in Project pages.

Useful if you want to organize tasks in a project by type – e.g. Dev, Marketing, etc. Unlock the Tasks db to add options.

This property has no options by default. Unlock the Tasks database in order to add options.

(Known as Kanban – Tag in previous versions of UB.)

### Localization Key

**Type:** Formula

This property allows you to localize your template. If you want to translate the option names in the Days, Recur Unit, and Status properties, you can do so and then change the arrays in this formula to the same values.

This will allow formulas to keep working.


Notion formulas are able to reference **property names** by their underlying ID. This means that if you have a working formula that references a property, you can change that property’s name later on and the formula will still work. If you look at the formula, you’ll see that any references to that property have updated to show the new name.

For example, if you translated the **Recur Unit** property’s name to **Unidad de Repetición**, you’d see this new name reflected in the **Next Due** property’s formula.

However, thie same cannot be **option names** in Select, Multi-Select, and Status properties – for example, options like “Day(s)” and “Week(s)” in the **Recur Unit** property. Even though these options technically do have IDs under the hood, the Notion formula language doesn’t provide us with a way to reference them.

This means we can only do plain old _string comparisons_ for these options. For example:

CODE

`if(   prop("Recur Unit") == "Days(s)", true, false )`
Code language: JavaScript (javascript)

The upshot here is that you can easily change the names of **properties** that are referenced in formulas (e.g. for localization purposes), but you can’t easily change the names of **options** in Select/Multi-Select/Status. If you do, string comparisons in formulas will always fail.

To solve this problem, we’ve included the **Localization Key** property in our templates. This formula returns a multi-dimensional array – essentially a list of lists. Each inner list contains a series of terms that match up to the option names in certain properties: Days, Recur Unit, and Status.

This allows us to do more robust comparisons in other formulas. For example, we can modify the above example to:

CODE

`if(   prop("Recur Unit") == "Days(s)" or prop("Recur Unit") == prop("Localization Key").at(1).at(0), true, false )`
Code language: JavaScript (javascript)

Arrays use _zero-based indexing,_ which means the first element in an array has an index of `0`. Therefore, the above “address” in the Localization key is “2nd element of the outer list, 1st element of that inner list”. If you look at Localization Key’s code below, you’ll see that element is “Day(s)”.

**This means you can localize your template without breaking formulas by updating Localization Key in addition to any property options.**

For example, if you changed “Day(s)” to “Día(s)” in **Recur Unit**, you can update Localization Key in the same way – changing “Day(s)” to “Día(s)” in the formula’s code.

When the **Next Due** formula checks the value of Recur Unit against the hard-coded string “Day(s)”, it’ll be false – but then when it checks against the _array position_ for “Day(s)” in Localization Key, it’ll return true, as expected.

**In summary, simply make sure you update the correct option in Localization Key to the same value you use when updating property option names in the Recur Interval, Days, and Status properties.**

CODE

`[ /* Rewrite these weekday and recur unit options in your own language, so your second brain can work even better with your first. Make sure to set up the same options in the "Recur Unit" and "Days" properties afterward, so you can select them. Feel free to remove the original names afterward! */ /* ["lunes", "3ª", "mercredi", "木曜日", "piątek", "lørdag", "Double Sunday"] */ ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"], /* ["Day(s)", "Week(s)", "Month(s)", "Year(s)", "Month(s) on the Last Day", "Month(s) on the First Weekday", "Month(s) on the Last Weekday", "Nth Weekday of Month"] */ ["Day(s)", "Week(s)", "Month(s)", "Year(s)", "Month(s) on the Last Day", "Month(s) on the First Weekday", "Month(s) on the Last Weekday", "Nth Weekday of Month"], /* This final list is for Status option names. */ ["To Do", "Doing", "Done"] ]`
Code language: JavaScript (javascript)

### Meta Labels

**Type:** Formula

Used in List views to show if a task is recurring and/or is currently being time-tracked.

CODE

`lets(   recurring,  if(    not empty(prop("Next Due")),    "🔁",    ""  ),  hasSubtask,  if(    prop("Sub-Tasks").filter(current.prop("Status") != "Done").length() > 0,    "",    ""  ),  trackingNow,  if(    prop("Sessions").filter(      current.prop("End").empty()    ).empty(),    "",    "⏱️"  ),  [trackingNow, recurring, hasSubtask].join(" ").replace("^\s","").replace("\s$","") )`
Code language: JavaScript (javascript)


If your copy of Ultimate Brain doesn’t have time tracking implemented, you’ll likely have the following version of Meta Labels:

CODE

`lets(   recurring,  if(    not empty(prop("Next Due")),    "🔁",    ""  ),  hasSubtask,  if(    prop("Sub-Tasks").filter(current.prop("Status") != "Done").length() > 0,    "",    ""  ),  [recurring, hasSubtask].join(" ").replace("^\s","").replace("\s$","") )`
Code language: JavaScript (javascript)

### My Day

**Type:** Checkbox

If checked, this task will show up in the Execute view of the My Day page.

Use this property to plan out the tasks you’re going to do today (regardless of their due date).

### My Day Label

**Type:** Formula

This simple property just displays the text “My Day”. It is used on the My Day page to label the My Day checkbox, making it visually distinct from the Status checkbox that is always shown to the left of task names.

CODE

`let(   showLabel,  true,  if(    showLabel,    "My Day",    ""  ) )`
Code language: JavaScript (javascript)

### Name

**Type:** Title

The name of the task.

### Next Due

**Type:** Formula

The “next due” date for the task if it is recurring. A date will show here once you have values set in the Due and Recur Interval properties.

The date is calculated the Due date, today’s date, and the values in Recur Interval, Recur Unit, and Days.

**Note:** Prior to the **February 7, 2026 update** ([Recurring Task Improvements, Notes ↔ Tasks Relation, and More](https://thomasjfrank.com/docs/ultimate-brain/changelog/#recurring-task-improvements-notes-tasks-relation-and-more)), Ultimate Brain did not have the **Enforce Schedule** (checkbox) property. The current version of the Next Due formula (version 2.2.2) which is shown below requires this property. If your template doesn’t have it, you can add it or use one of the previous versions shown below this current version of the formula.

CODE

`lets(   version, "2.2.2",     dueProp, prop("Due"),     recurIntervalProp, prop("Recur Interval"),     recurUnitProp, prop("Recur Unit"),     localizationKeyProp, prop("Localization Key"),     daysProp, prop("Days"),     emptyDate, parseDate(""),     if(!empty(recurIntervalProp) and !empty(dueProp),    if(recurIntervalProp > 0 and recurIntervalProp == ceil(recurIntervalProp),      lets(        recurUnit,          ifs(            or(recurUnitProp == at(at(localizationKeyProp, 1), 0), recurUnitProp == "Day(s)"), "days",            or(recurUnitProp == at(at(localizationKeyProp, 1), 1), recurUnitProp == "Week(s)"), "weeks",            or(recurUnitProp == at(at(localizationKeyProp, 1), 2), recurUnitProp == "Month(s)"), "months",            or(recurUnitProp == at(at(localizationKeyProp, 1), 3), recurUnitProp == "Year(s)"), "years",            or(recurUnitProp == at(at(localizationKeyProp, 1), 4), recurUnitProp == "Month(s) on the Last Day"), "monthsonthelastday",            or(recurUnitProp == at(at(localizationKeyProp, 1), 5), recurUnitProp == "Month(s) on the First Weekday"), "monthsonthefirstweekday",            or(recurUnitProp == at(at(localizationKeyProp, 1), 6), recurUnitProp == "Month(s) on the Last Weekday"), "monthsonthelastweekday",            or(and(!empty(at(at(localizationKeyProp, 1), 7)), recurUnitProp == at(at(localizationKeyProp, 1), 7)), recurUnitProp == "Nth Weekday of Month"), "nthweekdayofmonth",            "days"          ),                 weekdays,          match(            [              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 1 - 1)), includes(daysProp, "Monday")), 1, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 2 - 1)), includes(daysProp, "Tuesday")), 2, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 3 - 1)), includes(daysProp, "Wednesday")), 3, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 4 - 1)), includes(daysProp, "Thursday")), 4, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 5 - 1)), includes(daysProp, "Friday")), 5, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 6 - 1)), includes(daysProp, "Saturday")), 6, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 7 - 1)), includes(daysProp, "Sunday")), 7, false)            ],            "[1-7]"          ),                 dateDue, parseDate(formatDate(dueProp, "YYYY-MM-DD")),                 timeNow, if(          today() > dueProp and prop("Enforce Schedule"),          dueProp,          now()        ),                 dateNow, parseDate(formatDate(timeNow, "YYYY-MM-DD")),                 hasRange, dateEnd(dueProp) > dateStart(dueProp),                 recurUnitLapseLength,          if(            includes(["days", "weeks", "months", "years"], recurUnit),            dateBetween(dateNow, dateDue, recurUnit) / recurIntervalProp,            false          ),                 lastDayBaseDate,          if(            includes(["monthsonthelastday", "monthsonthefirstweekday", "monthsonthelastweekday"], recurUnit),            if(              year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue)) > 0,              dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, ceil((year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months"), date(dateAdd(dateDue, ceil((year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),              dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, recurIntervalProp, "months"), date(dateAdd(dateDue, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days")            ),            emptyDate          ),                 firstDayBaseDate,          if(            lastDayBaseDate != emptyDate,            dateSubtract(lastDayBaseDate, date(lastDayBaseDate) - 1, "days"),            emptyDate          ),                 firstWeekdayBaseDate,          if(            lastDayBaseDate != emptyDate,            if(              test(day(firstDayBaseDate), "6|7"),              dateAdd(firstDayBaseDate, 8 - day(firstDayBaseDate), "days"),              firstDayBaseDate            ),            emptyDate          ),                 lastWeekdayBaseDate,          if(            lastDayBaseDate != emptyDate,            if(              test(day(lastDayBaseDate), "6|7"),              dateSubtract(lastDayBaseDate, day(lastDayBaseDate) - 5, "days"),              lastDayBaseDate            ),            emptyDate          ),                 nextLastBaseDate,          if(            lastDayBaseDate != emptyDate,            dateSubtract(dateAdd(dateSubtract(dateAdd(lastDayBaseDate, recurIntervalProp, "months"), date(dateAdd(lastDayBaseDate, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),            emptyDate          ),                 nextFirstBaseDate,        if(          lastDayBaseDate != emptyDate,          dateSubtract(nextLastBaseDate, date(nextLastBaseDate) - 1, "days"),          emptyDate        ),                 nextFirstWeekday,          if(            lastDayBaseDate != emptyDate,            if(              test(day(nextFirstBaseDate), "6|7"),              dateAdd(nextFirstBaseDate, 8 - day(nextFirstBaseDate), "days"),              nextFirstBaseDate            ),            emptyDate          ),                 nextLastWeekday,          if(            lastDayBaseDate != emptyDate,            if(              test(day(nextLastBaseDate), "6|7"),              dateSubtract(nextLastBaseDate, day(nextLastBaseDate) - 5, "days"),              nextLastBaseDate            ),            emptyDate          ),                 firstWeekdayOfMonthBaseDate,          lets(            baseMonthDate,              if(                timeNow > dueProp,                timeNow,                dueProp              ),                           weekday, toNumber(at(weekdays, 0)),                         firstDayOfNextMonth, dateAdd(dateSubtract(baseMonthDate, date(baseMonthDate) - 1, "days"), 1, "months"),                         ifs(              day(firstDayOfNextMonth) < weekday, dateAdd(firstDayOfNextMonth, weekday - day(firstDayOfNextMonth), "days"),              day(firstDayOfNextMonth) > weekday, dateAdd(firstDayOfNextMonth, weekday - day(firstDayOfNextMonth) + 7, "days"),              firstDayOfNextMonth            )          ),                   nthWeekdayOfMonthBaseDate, dateAdd(firstWeekdayOfMonthBaseDate, recurIntervalProp - 1, "weeks"),                 nextDueStart,          ifs(            recurUnit == "days" and length(weekdays) > 0 and recurIntervalProp == 1,              if(                dateNow >= dateDue,                ifs(                  includes(weekdays, format(day(dateAdd(dateNow, 1, "days")))), dateAdd(dateNow, 1, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 2, "days")))), dateAdd(dateNow, 2, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 3, "days")))), dateAdd(dateNow, 3, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 4, "days")))), dateAdd(dateNow, 4, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 5, "days")))), dateAdd(dateNow, 5, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 6, "days")))), dateAdd(dateNow, 6, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 7, "days")))), dateAdd(dateNow, 7, "days"),                  emptyDate                ),                ifs(                  includes(weekdays, format(day(dateAdd(dateDue, 1, "days")))), dateAdd(dateDue, 1, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 2, "days")))), dateAdd(dateDue, 2, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 3, "days")))), dateAdd(dateDue, 3, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 4, "days")))), dateAdd(dateDue, 4, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 5, "days")))), dateAdd(dateDue, 5, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 6, "days")))), dateAdd(dateDue, 6, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 7, "days")))), dateAdd(dateDue, 7, "days"),                  emptyDate                )              ),             recurUnit == "nthweekdayofmonth" and length(weekdays) == 1 and recurIntervalProp >= 1 and recurIntervalProp <= 5,              if(                month(nthWeekdayOfMonthBaseDate) == month(firstWeekdayOfMonthBaseDate),                nthWeekdayOfMonthBaseDate,                dateSubtract(nthWeekdayOfMonthBaseDate, 1, "week")              ),             recurUnit == "monthsonthelastday",              if(                dateNow >= lastDayBaseDate,                nextLastBaseDate,                lastDayBaseDate              ),                         recurUnit == "monthsonthefirstweekday",              if(                dateNow >= firstWeekdayBaseDate,                nextFirstWeekday,                firstWeekdayBaseDate              ),                         recurUnit == "monthsonthelastweekday",              if(                dateNow >= lastWeekdayBaseDate,                nextLastWeekday,                lastWeekdayBaseDate              ),                         includes(["days", "weeks", "months", "years"], recurUnit),              if(                dateBetween(dateNow, dateDue, "days") >= 1,                if(                  recurUnitLapseLength == ceil(recurUnitLapseLength),                  dateAdd(dateDue, (recurUnitLapseLength + 1) * recurIntervalProp, recurUnit),                  dateAdd(dateDue, ceil(recurUnitLapseLength) * recurIntervalProp, recurUnit)                ),                dateAdd(dateDue, recurIntervalProp, recurUnit)              ),             emptyDate          ),                 recurRange, dateBetween(nextDueStart, dateDue, "days"),                 timeNextDueStart, dateAdd(dateStart(dueProp), recurRange, "days"),                 timeNextDueEnd, dateAdd(dateEnd(dueProp), recurRange, "days"),                 nextDue,          if(            hasRange,            dateRange(timeNextDueStart, timeNextDueEnd),            timeNextDueStart          ),                 nextDue      ),      dueProp    ),    emptyDate  ) )`
Code language: JavaScript (javascript)


The current version of the Next Due formula (2.2.0) shown above requires the following properties to be present in your template:

1.  Days (Prior to **January 20, 2025,** this property was called **Days (Only if Set to 1 Day(s)).**)
2.  Enforce Schedule (Checkbox-type property)

The **Enforce Schedule** property was added on **February 7, 2026**, and the latest version of Next Due (shown above) reflects its inclusion. If your template doesn’t include Enforce Schedule, it’s easy to add – it’s just a simple Checkbox property.

However, if you need the version of Next Due that was active from January 20, 2025 – February 6, 2026, here it is:


CODE

`lets(   version, "2.2.0",     dueProp, prop("Due"),     recurIntervalProp, prop("Recur Interval"),     recurUnitProp, prop("Recur Unit"),     localizationKeyProp, prop("Localization Key"),     daysProp, prop("Days"),     emptyDate, parseDate(""),     if(!empty(recurIntervalProp) and !empty(dueProp),    if(recurIntervalProp > 0 and recurIntervalProp == ceil(recurIntervalProp),      lets(        recurUnit,          ifs(            or(recurUnitProp == at(at(localizationKeyProp, 1), 0), recurUnitProp == "Day(s)"), "days",            or(recurUnitProp == at(at(localizationKeyProp, 1), 1), recurUnitProp == "Week(s)"), "weeks",            or(recurUnitProp == at(at(localizationKeyProp, 1), 2), recurUnitProp == "Month(s)"), "months",            or(recurUnitProp == at(at(localizationKeyProp, 1), 3), recurUnitProp == "Year(s)"), "years",            or(recurUnitProp == at(at(localizationKeyProp, 1), 4), recurUnitProp == "Month(s) on the Last Day"), "monthsonthelastday",            or(recurUnitProp == at(at(localizationKeyProp, 1), 5), recurUnitProp == "Month(s) on the First Weekday"), "monthsonthefirstweekday",            or(recurUnitProp == at(at(localizationKeyProp, 1), 6), recurUnitProp == "Month(s) on the Last Weekday"), "monthsonthelastweekday",            or(and(!empty(at(at(localizationKeyProp, 1), 7)), recurUnitProp == at(at(localizationKeyProp, 1), 7)), recurUnitProp == "Nth Weekday of Month"), "nthweekday",            "days"          ),                 weekdays,          match(            [              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 1 - 1)), includes(daysProp, "Monday")), 1, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 2 - 1)), includes(daysProp, "Tuesday")), 2, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 3 - 1)), includes(daysProp, "Wednesday")), 3, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 4 - 1)), includes(daysProp, "Thursday")), 4, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 5 - 1)), includes(daysProp, "Friday")), 5, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 6 - 1)), includes(daysProp, "Saturday")), 6, false),              if(or(includes(daysProp, at(at(localizationKeyProp, 0), 7 - 1)), includes(daysProp, "Sunday")), 7, false)            ],            "[1-7]"          ),                 dateDue, parseDate(formatDate(dueProp, "YYYY-MM-DD")),                 timeNow, now(),                 dateNow, parseDate(formatDate(timeNow, "YYYY-MM-DD")),                 hasRange, dateEnd(dueProp) > dateStart(dueProp),                 recurUnitLapseLength,          if(            includes(["days", "weeks", "months", "years"], recurUnit),            dateBetween(dateNow, dateDue, recurUnit) / recurIntervalProp,            false          ),                 lastDayBaseDate,          if(            includes(["monthsonthelastday", "monthsonthefirstweekday", "monthsonthelastweekday"], recurUnit),            if(              year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue)) > 0,              dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, ceil((year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months"), date(dateAdd(dateDue, ceil((year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),              dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, recurIntervalProp, "months"), date(dateAdd(dateDue, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days")            ),            emptyDate          ),                 firstDayBaseDate,          if(            lastDayBaseDate != emptyDate,            dateSubtract(lastDayBaseDate, date(lastDayBaseDate) - 1, "days"),            emptyDate          ),                 firstWeekdayBaseDate,          if(            lastDayBaseDate != emptyDate,            if(              test(day(firstDayBaseDate), "6|7"),              dateAdd(firstDayBaseDate, 8 - day(firstDayBaseDate), "days"),              firstDayBaseDate            ),            emptyDate          ),                 lastWeekdayBaseDate,          if(            lastDayBaseDate != emptyDate,            if(              test(day(lastDayBaseDate), "6|7"),              dateSubtract(lastDayBaseDate, day(lastDayBaseDate) - 5, "days"),              lastDayBaseDate            ),            emptyDate          ),                 nextLastBaseDate,          if(            lastDayBaseDate != emptyDate,            dateSubtract(dateAdd(dateSubtract(dateAdd(lastDayBaseDate, recurIntervalProp, "months"), date(dateAdd(lastDayBaseDate, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),            emptyDate          ),                 nextFirstBaseDate,        if(          lastDayBaseDate != emptyDate,          dateSubtract(nextLastBaseDate, date(nextLastBaseDate) - 1, "days"),          emptyDate        ),                 nextFirstWeekday,          if(            lastDayBaseDate != emptyDate,            if(              test(day(nextFirstBaseDate), "6|7"),              dateAdd(nextFirstBaseDate, 8 - day(nextFirstBaseDate), "days"),              nextFirstBaseDate            ),            emptyDate          ),                 nextLastWeekday,          if(            lastDayBaseDate != emptyDate,            if(              test(day(nextLastBaseDate), "6|7"),              dateSubtract(nextLastBaseDate, day(nextLastBaseDate) - 5, "days"),              nextLastBaseDate            ),            emptyDate          ),                 firstWeekSpecificDayBaseDate,          lets(            baseMonthDate,              if(                timeNow > dueProp,                timeNow,                dueProp              ),                           weekday, toNumber(at(weekdays, 0)),                         firstDayOfNextMonth, dateSubtract(dateAdd(baseMonthDate, 1, "months"), date(baseMonthDate) - 1, "days"),                         ifs(              day(firstDayOfNextMonth) < weekday, dateAdd(firstDayOfNextMonth, weekday - day(firstDayOfNextMonth), "days"),              day(firstDayOfNextMonth) > weekday, dateAdd(firstDayOfNextMonth, weekday - day(firstDayOfNextMonth) + 7, "days"),              firstDayOfNextMonth            )          ),                   nthWeekdayBaseDate, dateAdd(firstWeekSpecificDayBaseDate, recurIntervalProp - 1, "weeks"),                 nextDueStart,          ifs(            recurUnit == "days" and length(weekdays) > 0 and recurIntervalProp == 1,              if(                dateNow >= dateDue,                ifs(                  includes(weekdays, format(day(dateAdd(dateNow, 1, "days")))), dateAdd(dateNow, 1, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 2, "days")))), dateAdd(dateNow, 2, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 3, "days")))), dateAdd(dateNow, 3, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 4, "days")))), dateAdd(dateNow, 4, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 5, "days")))), dateAdd(dateNow, 5, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 6, "days")))), dateAdd(dateNow, 6, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 7, "days")))), dateAdd(dateNow, 7, "days"),                  emptyDate                ),                ifs(                  includes(weekdays, format(day(dateAdd(dateDue, 1, "days")))), dateAdd(dateDue, 1, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 2, "days")))), dateAdd(dateDue, 2, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 3, "days")))), dateAdd(dateDue, 3, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 4, "days")))), dateAdd(dateDue, 4, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 5, "days")))), dateAdd(dateDue, 5, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 6, "days")))), dateAdd(dateDue, 6, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 7, "days")))), dateAdd(dateDue, 7, "days"),                  emptyDate                )              ),             recurUnit == "nthweekday" and length(weekdays) == 1 and recurIntervalProp >= 1 and recurIntervalProp <= 5,              if(                month(nthWeekdayBaseDate) == month(firstWeekSpecificDayBaseDate),                nthWeekdayBaseDate,                dateSubtract(nthWeekdayBaseDate, 1, "week")              ),             recurUnit == "monthsonthelastday",              if(                dateNow >= lastDayBaseDate,                nextLastBaseDate,                lastDayBaseDate              ),                         recurUnit == "monthsonthefirstweekday",              if(                dateNow >= firstWeekdayBaseDate,                nextFirstWeekday,                firstWeekdayBaseDate              ),                         recurUnit == "monthsonthelastweekday",              if(                dateNow >= lastWeekdayBaseDate,                nextLastWeekday,                lastWeekdayBaseDate              ),                         includes(["days", "weeks", "months", "years"], recurUnit),              if(                dateBetween(dateNow, dateDue, "days") >= 1,                if(                  recurUnitLapseLength == ceil(recurUnitLapseLength),                  dateAdd(dateDue, (recurUnitLapseLength + 1) * recurIntervalProp, recurUnit),                  dateAdd(dateDue, ceil(recurUnitLapseLength) * recurIntervalProp, recurUnit)                ),                dateAdd(dateDue, recurIntervalProp, recurUnit)              ),             emptyDate          ),                 recurRange, dateBetween(nextDueStart, dateDue, "days"),                 timeNextDueStart, dateAdd(dateStart(dueProp), recurRange, "days"),                 timeNextDueEnd, dateAdd(dateEnd(dueProp), recurRange, "days"),                 nextDue,          if(            hasRange,            dateRange(timeNextDueStart, timeNextDueEnd),            timeNextDueStart          ),                 nextDue      ),      dueProp    ),    emptyDate  ) )`
Code language: JavaScript (javascript)

Here is the even older version of Next Due, which was current up until January 20, 2025:


CODE

`lets(   dueProp, prop("Due"),     recurIntervalProp, prop("Recur Interval"),     recurUnitProp, prop("Recur Unit"),     localizationKeyProp, prop("Localization Key"),     emptyDate, parseDate(""),     if(!empty(recurIntervalProp) and !empty(dueProp),    if(recurIntervalProp > 0 and recurIntervalProp == ceil(recurIntervalProp),      lets(        recurUnit,          ifs(            or(recurUnitProp == at(at(localizationKeyProp, 1), 0), recurUnitProp == "Day(s)"), "days",            or(recurUnitProp == at(at(localizationKeyProp, 1), 1), recurUnitProp == "Week(s)"), "weeks",            or(recurUnitProp == at(at(localizationKeyProp, 1), 2), recurUnitProp == "Month(s)"), "months",            or(recurUnitProp == at(at(localizationKeyProp, 1), 3), recurUnitProp == "Year(s)"), "years",            or(recurUnitProp == at(at(localizationKeyProp, 1), 4), recurUnitProp == "Month(s) on the Last Day"), "monthsonthelastday",            or(recurUnitProp == at(at(localizationKeyProp, 1), 5), recurUnitProp == "Month(s) on the First Weekday"), "monthsonthefirstweekday",            or(recurUnitProp == at(at(localizationKeyProp, 1), 6), recurUnitProp == "Month(s) on the Last Weekday"), "monthsonthelastweekday",            "days"          ),                 weekdays,          match(            [              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 1 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Monday")), 1, false),              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 2 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Tuesday")), 2, false),              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 3 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Wednesday")), 3, false),              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 4 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Thursday")), 4, false),              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 5 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Friday")), 5, false),              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 6 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Saturday")), 6, false),              if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 7 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Sunday")), 7, false)            ],            "[1-7]"          ),                 dateDue, parseDate(formatDate(dueProp, "YYYY-MM-DD")),                 timeNow, now(),                 dateNow, parseDate(formatDate(timeNow, "YYYY-MM-DD")),                 hasRange, dateEnd(dueProp) > dateStart(dueProp),                 recurUnitLapseLength,          if(            includes(["days", "weeks", "months", "years"], recurUnit),            dateBetween(dateNow, dateDue, recurUnit) / recurIntervalProp,            false          ),                 lastDayBaseDate,          if(            includes(["monthsonthelastday", "monthsonthefirstweekday", "monthsonthelastweekday"], recurUnit),            if(              year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue)) > 0,              dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, ceil((year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months"), date(dateAdd(dateDue, ceil((year(dateNow) * 12 + month(dateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),              dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, recurIntervalProp, "months"), date(dateAdd(dateDue, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days")            ),            emptyDate          ),                 firstDayBaseDate,          if(            lastDayBaseDate != emptyDate,            dateSubtract(lastDayBaseDate, date(lastDayBaseDate) - 1, "days"),            emptyDate          ),                 firstWeekdayBaseDate,          if(            lastDayBaseDate != emptyDate,            if(              test(day(firstDayBaseDate), "6|7"),              dateAdd(firstDayBaseDate, 8 - day(firstDayBaseDate), "days"),              firstDayBaseDate            ),            emptyDate          ),                 lastWeekdayBaseDate,          if(            lastDayBaseDate != emptyDate,            if(              test(day(lastDayBaseDate), "6|7"),              dateSubtract(lastDayBaseDate, day(lastDayBaseDate) - 5, "days"),              lastDayBaseDate            ),            emptyDate          ),                 nextLastBaseDate,          if(            lastDayBaseDate != emptyDate,            dateSubtract(dateAdd(dateSubtract(dateAdd(lastDayBaseDate, recurIntervalProp, "months"), date(dateAdd(lastDayBaseDate, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),            emptyDate          ),                 nextFirstBaseDate,        if(          lastDayBaseDate != emptyDate,          dateSubtract(nextLastBaseDate, date(nextLastBaseDate) - 1, "days"),          emptyDate        ),                 nextFirstWeekday,          if(            lastDayBaseDate != emptyDate,            if(              test(day(nextFirstBaseDate), "6|7"),              dateAdd(nextFirstBaseDate, 8 - day(nextFirstBaseDate), "days"),              nextFirstBaseDate            ),            emptyDate          ),                 nextLastWeekday,          if(            lastDayBaseDate != emptyDate,            if(              test(day(nextLastBaseDate), "6|7"),              dateSubtract(nextLastBaseDate, day(nextLastBaseDate) - 5, "days"),              nextLastBaseDate            ),            emptyDate          ),                 nextDueStart,          ifs(            recurUnit == "days" and length(weekdays) > 0 and recurIntervalProp == 1,              if(                dateNow >= dateDue,                ifs(                  includes(weekdays, format(day(dateAdd(dateNow, 1, "days")))), dateAdd(dateNow, 1, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 2, "days")))), dateAdd(dateNow, 2, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 3, "days")))), dateAdd(dateNow, 3, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 4, "days")))), dateAdd(dateNow, 4, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 5, "days")))), dateAdd(dateNow, 5, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 6, "days")))), dateAdd(dateNow, 6, "days"),                  includes(weekdays, format(day(dateAdd(dateNow, 7, "days")))), dateAdd(dateNow, 7, "days"),                  emptyDate                ),                ifs(                  includes(weekdays, format(day(dateAdd(dateDue, 1, "days")))), dateAdd(dateDue, 1, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 2, "days")))), dateAdd(dateDue, 2, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 3, "days")))), dateAdd(dateDue, 3, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 4, "days")))), dateAdd(dateDue, 4, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 5, "days")))), dateAdd(dateDue, 5, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 6, "days")))), dateAdd(dateDue, 6, "days"),                  includes(weekdays, format(day(dateAdd(dateDue, 7, "days")))), dateAdd(dateDue, 7, "days"),                  emptyDate                )              ),             recurUnit == "monthsonthelastday",              if(                dateNow >= lastDayBaseDate,                nextLastBaseDate,                lastDayBaseDate              ),                         recurUnit == "monthsonthefirstweekday",              if(                dateNow >= firstWeekdayBaseDate,                nextFirstWeekday,                firstWeekdayBaseDate              ),                         recurUnit == "monthsonthelastweekday",              if(                dateNow >= lastWeekdayBaseDate,                nextLastWeekday,                lastWeekdayBaseDate              ),                         includes(["days", "weeks", "months", "years"], recurUnit),              if(                dateBetween(dateNow, dateDue, "days") >= 1,                if(                  recurUnitLapseLength == ceil(recurUnitLapseLength),                  dateAdd(dateDue, (recurUnitLapseLength + 1) * recurIntervalProp, recurUnit),                  dateAdd(dateDue, ceil(recurUnitLapseLength) * recurIntervalProp, recurUnit)                ),                dateAdd(dateDue, recurIntervalProp, recurUnit)              ),             emptyDate          ),                 recurRange, dateBetween(nextDueStart, dateDue, "days"),                 timeNextDueStart, dateAdd(dateStart(dueProp), recurRange, "days"),                 timeNextDueEnd, dateAdd(dateEnd(dueProp), recurRange, "days"),                 nextDue,          if(            hasRange,            dateRange(timeNextDueStart, timeNextDueEnd),            timeNextDueStart          ),                 nextDue      ),      dueProp    ),    emptyDate  ) )`
Code language: JavaScript (javascript)

### Occurrences

**Type:** Relation

Past, finished occurrences of a recurring task.

This is part of Task History for recurring tasks. To enable it, unlock your Tasks database. Click the ⚡️ to access Automations, then de-activate Recurring Tasks (Simple), and activate Recurring Tasks (Advanced). Learn more in the [Recurring Tasks](https://thomasjfrank.com/docs/ultimate-brain/recurring-tasks/) guide.

### Parent Project

**Type:** Rollup

If this page is a sub-task, this Rollup property shows the Project of its parent task (if it has one).

### Parent Smart List

**Type:** Rollup

If this task is a sub-task, this Rollup property gets the calculated Smart List from its parent task.

### Parent Task

**Type:** Relation

If the current page is a sub-task, its parent task page will be shown here.

This property is a Relation which connects to another property inside this same All Tasks database.

### People

**Type:** Relation

This relation property connects to the People database. You can use it to associate tasks with other people.

For example, in the Delegated view of the Process dashboard, you can use this relation to note the person to whom you’ve delegated a task.

### Priority

**Type:** Status

Priority level for your task. By default, this value only matters in the special Priority View. Priority doesn’t affect task sorting or visibility in other views – but you can add Priority filters or grouping to them if you want.

| Name | Description |
| --- | --- |
| Group: To-do |
| Group: In progress |
| Low | For low-priority tasks. |
| Medium | For medium-priority tasks. |
| High | For high-priority tasks. |
| Group: Complete |

### Project

**Type:** Relation

This Relation connects to the Projects database, allowing you to associate a project with your tasks.

### Project Active

**Type:** Formula

This property shows whether this task’s project is active (either Doing or Ongoing). Used for views that only show tasks within projects that are active – helpful for filtering out tasks in projects that are planned, but not yet in progress.

This property’s value differs between the standalone Ultimate Brain template and the fully-integrated Ultimate Brain + Creator’s Companion template.


This version of the Project Active formula is found in the base version of Ultimate Brain.

CODE

`lets(   project,  if(    prop("Parent Task"),    prop("Parent Task").first().prop("Project").first(),    prop("Project").first()  ),  projectStatus,  project.prop("Status"),  projectLocalizationKey,  project.prop("Localization Key"),  if(    projectStatus == "Doing"    or projectStatus == "Ongoing"    or projectLocalizationKey.at(1).includes(projectStatus),    true,    false  ) )`
Code language: JavaScript (javascript)


This version of the Project Active formula is found in the fully-integrated version of Ultimate Brain + Creator’s Companion.

It checks both normal Projects, as well as content projects in the Content database, to determine in the task is part of an active project.

CODE

`lets(   project,  if(    prop("Parent Task"),    prop("Parent Task").first().prop("Project").first(),    prop("Project").first()  ),  projectStatus,  project.prop("Status"),  projectLocalizationKey,  project.prop("Localization Key"),  content,  if(    prop("Parent Task"),    prop("Parent Task").first().prop("Content").first(),    prop("Content").first()  ),  contentStatus,  content.prop("Status"),  contentLocalizationKey,  content.prop("Localization Key"),  projectActive,  if(    projectStatus == "Doing"    or projectStatus == "Ongoing"    or projectLocalizationKey.at(1).includes(projectStatus),    true,    false  ),  contentActive,  if(    contentStatus == "Research"    or contentStatus == "Writing"    or contentStatus == "Filming"    or contentStatus == "Review"    or contentStatus == "Recording"    or contentStatus == "Editing"    or contentStatus == "Ready to Publish"    or contentStatus == "Needs Update"    or contentLocalizationKey.at(1).includes(contentStatus),    true,    false  ),  if(    projectActive or contentActive,    true,    false  ) )`
Code language: JavaScript (javascript)

### Project Area

**Type:** Rollup

If this task is related to a Project, this Rollup property shows the Project’s Area (if it has one).

### Recur Interval

**Type:** Number

How often to make a recurring task recur. By default, this interval is a number of days – e.g an interval of 2 makes Next Due two days from Due (or today’s date). You can set a Recur Unit to adjust this.

### Recur Unit

**Type:** Select

Sets the unit of time that will be used by Recur Interval to set Next Due. E.g. if you set Week(s) here, then a Recur Interval of 2 will set a task to recur every 2 weeks.

| Name | Description |
| --- | --- |
| Day(s) | Recur by days.  
Example: If Recur Interval is 3, the task recurs every 3 days.  
If you want a task to recur on specific days of the week, set Recur Interval to 1, then choose specific days in the Days property. |
| Week(s) | Recur by weeks.  
Example: If Recur Interval is 3, the task recurs every 3 weeks.  
If you want a task to recur during a specific week of each month, use the “Nth Weekday of Month” option instead. |
| Month(s) | Recur by months.  
Example: If Recur Interval is 2, the task recurs every other month.  
Tip: Use a Recur Interval of 3 to create a “Quarterly” recur schedule. |
| Month(s) on the First Weekday | Recur by months, always on the first weekday of the month (Mon, Tues, Weds, Thurs, or Fri). |
| Month(s) on the Last Weekday | Recur by months, always on the last weekday of the month (Mon, Tues, Weds, Thurs, or Fri). |
| Month(s) on the Last Day | Recur by months, always on the last day of the month.  
Example: If Recur Interval is 2, and Due is December 31, Next Due will be February 28 (or 29 on a leap year). |
| Year(s) | Recur by years.  
Example: If Recur Interval is 2, the task recurs every 2 years. |
| Nth Weekday of Month | E.g. “3rd Thurs of the Month”. (Weekend days work too)  
This option only works if Days property is set, and only when Days has a SINGLE value.  
For the example above, Recur Interval = 3, Days = Thursday.  
Set Recur Interval to 1 for first week of month, 5 for last week of month. |

### Sessions

**Type:** Relation

**Limit:** None

**Related Property:** Task (in the [Work Sessions](https://thomasjfrank.com/docs/ultimate-brain/databases/work-sessions/) database)

Work Sessions related to this Task.

This Relation property connects to the Task Relation property in the Work Sessions database.

### Shopping List

**Type:** Checkbox

This task will show up in the Shopping list within the Recipe w/ Shopping List template in the Recipes database.

### Smart List

**Type:** Select

Allows you to send tasks to specific lists in the Process (GTD) page, which allows you to practice GTD (Getting Things Done) in Notion.

The Due and Snooze properties also factor into these lists; tasks with a Due date go to the Calendar, and tasks with a Snooze date go to Deferred.

| Name | Description |
| --- | --- |
| Do Next | “Next Actions” in GTD terms. |
| Delegated | Tasks that you’ve delegated to another person. |
| Someday | Tasks that you may do someday, but you’re not committed to right now. |

### Smart List (Formula)

**Type:** Formula

This formula determines which Smart List (aka GTD list) a task is currently on. Used in the Process page.

It takes into account the following properties: Due, Snooze, and Smart List.

Possible values (in priority order): Calendar, Do Next, Delegated, Snoozed, Someday, Inbox.

CODE

`ifs(   prop("Due"),  "Calendar",  prop("Smart List") == "Do Next" or prop("Smart List") == "Delegated",  prop("Smart List"),  prop("Snooze"),  "Snoozed",  prop("Smart List") == "Someday",  prop("Smart List"),  "Inbox" )`
Code language: JavaScript (javascript)

### Snooze

**Type:** Date

Used in the Process and Process → Deferred pages for practicing GTD in Notion.

Adding a Snooze date moves a task to the Deferred list/page.

Note that only tasks without a Due date will show there. Due (Calendar) has higher priority than Snooze (Deferred).

### Snooze (Parent)

**Type:** Formula

If this task is a sub-task of a parent task that is Snoozed, this returns that parent task’s Snooze date as a timestamp. Otherwise, it returns this task’s own Snooze date as a timestamp.

Aids in sorting sub-tasks beneath their parent tasks in Process → Snoozed.

CODE

`!prop("Parent Task").first().prop("Snooze")   ? prop("Snooze").timestamp()  : prop("Parent Task").first().prop("Snooze").timestamp()`
Code language: JavaScript (javascript)

### Start

**Type:** Button

Starts a new Work Session for this task. Also ends any currently-active Work Session that is being tracked for you.


To configure the Start button, first create an Edit Pages action, choosing the Work Sessions database as the target.

Create a filter where Team Member contains me and End is empty.

Set the End property to the Time Triggered dynamic value.

Next, create an **Add Page To** step, and choose your Work Sessions database. This step will create the new work session entry for this task. Set the following values:

-   **Name**: Custom formula
-   **Start:** Time triggered
-   **Task:** This page
-   **Team Member:** Whoever clicked

In the Name property’s custom formula editor, type out the following formula manually (copying and pasting doesn’t work in buttons and automations yet):

CODE

`let(   sessionNumber,  This Page.Sessions.length() + 1,  This Page + " (Session " + sessionNumber + ")" )`
Code language: JavaScript (javascript)

Click Save.

### Status

**Type:** Status

Tracks the status of the task. Most views in Ultimate Brain show this property as a checkbox.

Checking the checkbox will alternate Status between “To Do” and “Done”. ⌥/Alt + Click the checkbox in order to access all options, including “Doing”.

| Name | Description |
| --- | --- |
| Group: To-do |
| To Do | Tasks that you need to do. |
| Group: In progress |
| Doing | Tasks you’re currently doing. |
| Group: Complete |
| Done | Tasks that are done. |

### Sub-Task Sorter

**Type:** Formula

Used in Projects, alongside Due Stamp (Parent), to ensure that sub-tasks are sorted correctly beneath their parent tasks.

CODE

`lets(   subSeedName,  if(    !prop("Parent Task"),    prop("Name").lower(),    prop("Parent Task").first().format().lower()  ),  subSeed,  if(    !prop("Parent Task"),    1,    2  ),  taskStatus,  if(    prop("Status") == "Done" or prop("Status") == prop("Localization Key").at(2).last(),    2,    1  ),  taskStatus + " - " + subSeedName + " - " + subSeed )`
Code language: JavaScript (javascript)

### Sub-Tasks

**Type:** Relation

If the current task page is a Parent Task, all of its sub-tasks will be shown here.

This is a Relation property, which is connected to the Parent Task Relation property in this same All Tasks database.

### Time Tracked

**Type:** Formula

The total amount of time tracked on this task, formatted in HH:MM:SS as a string (seconds will always be zero).

Uses the Time Tracked (Mins) property for the underlying number of minutes tracked.

CODE

`lets(   duration, prop("Time Tracked (Mins)").sum(),  hours, floor(duration / 60),  hoursLabel, hours < 10 ? "0" + hours : hours,  minutes, duration % 60,  minutesLabel, minutes < 10 ? "0" + minutes : minutes,  hoursLabel + ":" + minutesLabel + ":00" )`
Code language: JavaScript (javascript)

### Time Tracked (Mins)

The total number of minutes tracked on this task, across all Work Sessions related to it.

**Type:** Formula

CODE

`prop("Sessions").map(   current.prop("Duration (Mins)") ).sum()`
Code language: JavaScript (javascript)

### Time Tracking Status

The current time-tracking status of this task. Can be “Active Now”, “Not Tracking”, or “Done”.

Used to group tasks in the Time view within the Execute section of My Day.

**Type:** Formula

CODE

`ifs(   prop("Status") == "Done",  "Done",  prop("Sessions").filter(    current.prop("End").empty()  ).length() > 0,  "Active Now",  "Not Tracking" )`
Code language: JavaScript (javascript)

### Wait (Parent)

**Type:** Formula

If this task is a sub-task of a parent task that has a Wait Date, this returns that parent task’s Wait Date as a timestamp. Otherwise, it returns this task’s own Wait Date as a timestamp.

Aids in sorting sub-tasks beneath their parent tasks in Process → Delegated.

CODE

`!prop("Parent Task").first().prop("Wait Date")   ? prop("Wait Date").timestamp()  : prop("Parent Task").first().prop("Wait Date").timestamp()`
Code language: JavaScript (javascript)

### Wait Date

**Type:** Date

Used to note the date you delegated a task to someone else. This is most useful in Process → Delegated, which functions as the Delegated list for practicing GTD in Notion.


The following properties were present in Ultimate Brain 3.0 at launch, but have since been removed.

### Contexts

**Type:** Multi Select

Allows you to apply GTD-style contexts to your tasks. In Process → Do Next, you’ll find a view where you can group your tasks by context.

| Name | Description |
| --- | --- |
| High-Energy | Tasks that require a lot of mental energy. |
| Low-Energy | Tasks that require only a small amount of mental energy. |
| Errand | Errands that have to be done outside the home. |
| Home | Tasks that need to be done in/around your home. |
| Office | Tasks that need to be done at the office. |
| Social | Tasks that involve talking or working with other people. |
| Shopping | Shopping-related tasks. This Context is used to power the Shopping page within Recipes. |

### Next Due API

**Type:** Formula

This formula takes the Date value from the Next Due property, separates the start and end dates (if an end date is present), and formats the output as a JSON string. Used exclusively by Pipedream for automated recurring tasks (you shouldn’t edit it).

CODE

`let(   nextDueAPI,    lets(      dueProp, prop("Due"),             recurIntervalProp, prop("Recur Interval"),             recurUnitProp, prop("Recur Unit"),             localizationKeyProp, prop("Localization Key"),             if(        !empty(recurIntervalProp) and !empty(dueProp),        if(          recurIntervalProp > 0 and recurIntervalProp == ceil(recurIntervalProp),          lets(            recurUnit, ifs(              or(recurUnitProp == at(at(localizationKeyProp, 1), 0), recurUnitProp == "Day(s)"), "days",              or(recurUnitProp == at(at(localizationKeyProp, 1), 1), recurUnitProp == "Week(s)"), "weeks",              or(recurUnitProp == at(at(localizationKeyProp, 1), 2), recurUnitProp == "Month(s)"), "months",              or(recurUnitProp == at(at(localizationKeyProp, 1), 3), recurUnitProp == "Year(s)"), "years",              or(recurUnitProp == at(at(localizationKeyProp, 1), 4), recurUnitProp == "Month(s) on the Last Day"), "monthsonthelastday",              or(recurUnitProp == at(at(localizationKeyProp, 1), 5), recurUnitProp == "Month(s) on the First Weekday"), "monthsonthefirstweekday",              or(recurUnitProp == at(at(localizationKeyProp, 1), 6), recurUnitProp == "Month(s) on the Last Weekday"), "monthsonthelastweekday",              "days"            ),                         weekdays,              match(                [                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 1 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Monday")), 1, false),                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 2 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Tuesday")), 2, false),                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 3 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Wednesday")), 3, false),                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 4 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Thursday")), 4, false),                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 5 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Friday")), 5, false),                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 6 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Saturday")), 6, false),                  if(or(includes(prop("Days (Only if Set to 1 Day(s))"), at(at(localizationKeyProp, 0), 7 - 1)), includes(prop("Days (Only if Set to 1 Day(s))"), "Sunday")), 7, false)                ],                "[1-7]"              ),                         dateDue, parseDate(formatDate(dueProp, "YYYY-MM-DD")),                         dateDueEnd, parseDate(formatDate(dateEnd(dueProp), "YYYY-MM-DD")),                         timeNow, now(),                         offsetTimeNow, dateAdd(timeNow, prop("UTC Offset"), "hours"),             inUTC,              if(                formatDate(now(), "ZZ") == "+0000",                true,                false              ),                         hasValidOffset,              if(                !empty(prop("UTC Offset")) and prop("UTC Offset") >= -12 and prop("UTC Offset") <= 14,                true,                false              ),                         hasRange, dateEnd(dateDueEnd) > dateStart(dateDue),                         dueRange, dateBetween(dateDueEnd, dateDue, "days"),                         conditionalTimeNow,              if(                inUTC and hasValidOffset,                offsetTimeNow,                timeNow              ),                         conditionalDateNow, parseDate(formatDate(conditionalTimeNow, "YYYY-MM-DD")),                         recurUnitLapseLength,              if(                includes(["days", "weeks", "months", "years"], recurUnit),                dateBetween(conditionalDateNow, dateDue, recurUnit) / recurIntervalProp,                false              ),                         lastDayBaseDate,              if(                includes(["monthsonthelastday", "monthsonthefirstweekday", "monthsonthelastweekday"], recurUnit),                if(                  year(conditionalDateNow) * 12 + month(conditionalDateNow) - (year(dateDue) * 12 + month(dateDue)) > 0,                  dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, ceil((year(conditionalDateNow) * 12 + month(conditionalDateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months"), date(dateAdd(dateDue, ceil((year(conditionalDateNow) * 12 + month(conditionalDateNow) - (year(dateDue) * 12 + month(dateDue))) / recurIntervalProp) * recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),                  dateSubtract(dateAdd(dateSubtract(dateAdd(dateDue, recurIntervalProp, "months"), date(dateAdd(dateDue, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days")                ),                false              ),                         firstDayBaseDate,              if(                lastDayBaseDate != false,                dateSubtract(lastDayBaseDate, date(lastDayBaseDate) - 1, "days"),                false              ),                         firstWeekdayBaseDate,              if(                lastDayBaseDate != false,                if(                  test(day(firstDayBaseDate), "6|7"),                  dateAdd(firstDayBaseDate, 8 - day(firstDayBaseDate), "days"),                  firstDayBaseDate                ),                false              ),                         lastWeekdayBaseDate,              if(                lastDayBaseDate != false,                if(                  test(day(lastDayBaseDate), "6|7"),                  dateSubtract(lastDayBaseDate, day(lastDayBaseDate) - 5, "days"),                  lastDayBaseDate                ),                false              ),                         nextLastBaseDate,              if(                lastDayBaseDate != false,                dateSubtract(dateAdd(dateSubtract(dateAdd(lastDayBaseDate, recurIntervalProp, "months"), date(dateAdd(lastDayBaseDate, recurIntervalProp, "months")) - 1, "days"), 1, "months"), 1, "days"),                false              ),                         nextFirstBaseDate,              if(                lastDayBaseDate != false,                dateSubtract(nextLastBaseDate, date(nextLastBaseDate) - 1, "days"),                false              ),                         nextFirstWeekday,              if(                lastDayBaseDate != false,                if(                  test(day(nextFirstBaseDate), "6|7"),                  dateAdd(nextFirstBaseDate, 8 - day(nextFirstBaseDate), "days"),                  nextFirstBaseDate                ),                false              ),                         nextLastWeekday,              if(                lastDayBaseDate != false,                if(                  test(day(nextLastBaseDate), "6|7"),                  dateSubtract(nextLastBaseDate, day(nextLastBaseDate) - 5, "days"),                  nextLastBaseDate                ),                false              ),                         nextDueStart,              ifs(                recurUnit == "days" and length(weekdays) > 0 and recurIntervalProp == 1,                  if(                    conditionalDateNow >= dateDue,                    ifs(                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 1, "days")))), dateAdd(conditionalDateNow, 1, "days"),                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 2, "days")))), dateAdd(conditionalDateNow, 2, "days"),                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 3, "days")))), dateAdd(conditionalDateNow, 3, "days"),                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 4, "days")))), dateAdd(conditionalDateNow, 4, "days"),                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 5, "days")))), dateAdd(conditionalDateNow, 5, "days"),                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 6, "days")))), dateAdd(conditionalDateNow, 6, "days"),                      includes(weekdays, format(day(dateAdd(conditionalDateNow, 7, "days")))), dateAdd(conditionalDateNow, 7, "days"),                      false                    ),                    ifs(                      includes(weekdays, format(day(dateAdd(dateDue, 1, "days")))), dateAdd(dateDue, 1, "days"),                      includes(weekdays, format(day(dateAdd(dateDue, 2, "days")))), dateAdd(dateDue, 2, "days"),                      includes(weekdays, format(day(dateAdd(dateDue, 3, "days")))), dateAdd(dateDue, 3, "days"),                      includes(weekdays, format(day(dateAdd(dateDue, 4, "days")))), dateAdd(dateDue, 4, "days"),                      includes(weekdays, format(day(dateAdd(dateDue, 5, "days")))), dateAdd(dateDue, 5, "days"),                      includes(weekdays, format(day(dateAdd(dateDue, 6, "days")))), dateAdd(dateDue, 6, "days"),                      includes(weekdays, format(day(dateAdd(dateDue, 7, "days")))), dateAdd(dateDue, 7, "days"),                      false                    )                  ),                                 recurUnit == "monthsonthelastday",                  if(                    conditionalDateNow >= lastDayBaseDate,                    nextLastBaseDate,                    lastDayBaseDate                  ),                                 recurUnit == "monthsonthefirstweekday",                  if(                    conditionalDateNow >= firstWeekdayBaseDate,                    nextFirstWeekday,                    firstWeekdayBaseDate                  ),                                 recurUnit == "monthsonthelastweekday",                  if(                    conditionalDateNow >= lastWeekdayBaseDate,                    nextLastWeekday,                    lastWeekdayBaseDate                  ),                                 includes(["days", "weeks", "months", "years"], recurUnit),                  if(                    dateBetween(conditionalDateNow, dateDue, "days") >= 1,                    if(                      recurUnitLapseLength == ceil(recurUnitLapseLength),                      dateAdd(dateDue, (recurUnitLapseLength + 1) * recurIntervalProp, recurUnit),                      dateAdd(dateDue, ceil(recurUnitLapseLength) * recurIntervalProp, recurUnit)                    ),                    dateAdd(dateDue, recurIntervalProp, recurUnit)                  ),                false              ),                         nextDueEnd,              if(                hasRange and nextDueStart != false,                dateAdd(nextDueStart, dueRange, "days"),                false              ),                         nextDue,              if(                hasRange and nextDueEnd != false,                dateRange(nextDueStart, nextDueEnd),                nextDueStart              ),                         nextDue          ),          false        ),        false      )    ),   if(    nextDueAPI == false,    "∅",    "{\"start\":\"" + formatDate(dateStart(nextDueAPI), "YYYY-MM-DD") + "\",\"end\":\"" + formatDate(dateEnd(nextDueAPI), "YYYY-MM-DD") + "\"}"  ) )`
Code language: JavaScript (javascript)

### Recurring Tasks Divider

**Type:** Formula

This formula property just helps to visually separate the recurring tasks helper properties below (which should not be edited) from the rest above.

CODE

`repeat("■", 3).style("red") + " Recurring Tasks helper properties".style("b")`
Code language: JavaScript (javascript)

### Sub-Task Arrow

**Type:** Formula

Indents sub-tasks in List views. This template does not use Notion’s native sub-items feature, as it can cause unexpected results and user confusion in this template’s Projects database template.

CODE

`ifs(   prop("Parent Task"),  "→" )`
Code language: JavaScript (javascript)

### UTC Offset

**Type:** Formula

Your UTC offset, based on your location (e.g. -6 for Denver during DST time, -7 at other times).

This property is set automatically by my [automated recurring tasks Pipedream workflow](https://thomasjfrank.com/notion-automated-recurring-tasks/), so you don’t need to edit it.

Ultimate Brain 3.0 also comes with native automated recurring tasks using [Notion database automations](https://thomasjfrank.com/notion-database-automations-the-complete-guide/), so this property likely won’t be used at all. It’s only used if you’re using the Pipedream workflow.

CODE

`0`
Code language: JavaScript (javascript)

## UB + CC Exclusive Properties

The fully-integrated version of Ultimate Brain + Creator’s Companion contains a few exclusive Tasks properties that are not found in the standalone version of Ultimate Brain.

In addition to these properties, the **Project Active** formula property (listed above) has a different formula in UB + CC compared to standalone UB.

### Content

**Type:** Relation

Tasks that are part of this content project. This Relation property connects to the Content Relation property in the Tasks database.

### Parent Content

**Type:** Rollup

If this page’s task is a sub-task, this Rollup property shows the Content project of the Parent Task (if one is set).

Primarily useful for making the Task With Sub-Tasks database template work as expected.
