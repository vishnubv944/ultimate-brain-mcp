---
title: Projects Database — Full Schema Reference
purpose: Complete property reference for the Projects database — all properties, status options, and formulas
agent_relevant: true
key_topics: [projects, database, schema, properties, status, progress, deadline, tag, goal, tasks, notes, time-tracked]
related_docs: [features/managing-tasks.md, databases/goals.md, databases/tags.md]
---
# Projects

The **Projects** database stores all of your projects. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Projects are useful ways to group tasks and notes that are all related to a common outcome, which should ideally be well-defined, measurable, and achievable.

Below you’ll find a reference guide for all database templates and properties in the Projects database. Properties are in alphabetical order.

## Database Templates

### Project

This template establishes a Project’s page as the “home base” for everything involved in that project. It displays note and tesks related to the project.

### Research Project

Research projects are identical to normal projects, but they also leveral the concept of **Pulls**. These are Notes and Tags that you “pull into” a project, without directly relating them to the project.

This concept is typically only used by a small subset of users, so don’t feel any pressure to use it. That said, some users enjoy the ability to bring materials they’ve captured in other contexts into a Project. To do this, you can leverage two properties in the Projects database:

-   Pulled Notes
-   Pulled Tags

Pulled Notes lets you pull individual notes into the Pulled database view within the Research Project. Pulled Tags will pull _every_ note from a Tag into the Pulled view, and will also show the Tag itself in the Pulled Tags section.

## Properties

### Archived

**Type:** Checkbox

If checked, this project is Archived. It will be removed from all main views in UB, and show up in the Archived Projects section of the Archive page.

This follows the PARA principle – archived items are removed from the main lists and safely tucked out of sight.

### Completed

**Type:** Date

The date the project was actually completed. Compare with your Target Deadline to see how well you were able to predict the project’s timeline.

### Created

**Type:** Created Time

The date and time this project page was created.

### Edited

**Type:** Last Edited Time

The date and time this project was last edited.

### Goal

**Type:** Relation

A goal this project is a part of. This Relation property connected to the Projects Relation property in the Goals database.

### Goal Tag

**Type:** Rollup

If this project is related to a goal, this Rollup property shows the Tag (if any) to which the goal is related.

### Latest Activity

**Type:** Formula

Returns the date and time of the latest activity in the project – including the project itself, along with any tasks or notes directly related to it.

Used for allowing projects to be sorted by Latest Activity in certain views.

CODE

`lets( 	taskActivity, 	prop("Tasks").map(current.prop("Edited")).sort().reverse(), 	noteActivity, 	prop("Notes").map(current.prop("Updated")).sort().reverse(), 	editedList, 	[prop("Edited")], 	concat(taskActivity, noteActivity, editedList).sort().reverse().first() )`
Code language: JavaScript (javascript)

### Localization Key

**Type:** Formula

This property allows you to define translated names for the Project database’s key Status options – Planned, On Hold, Doing, Ongoing, and Done.

These are important for filters in certain views within My Day and Task Manager.

CODE

`/* If you would like to translate your Status option names, translate them in the Status property AND in this formula as well. Below, you'll see variables for each of the important Status options in Ultimate Brain. For each, you can replace the terms in quotes ("") with translated terms. The final argument (the List defined by the brackets) is the return value of this formula. It is a multi-dimensional List (e.g. array), where the most important element is the 2nd (index 1) of the top List. This sub-List contains all of the Status options that are considered Active. Since Notion formulas don't let us access the Groups of Status properties (e.g. To Do, In Progress, Complete), this is the best way to get the Group of a translated (or added) Status option. If you have added extra options that are considered Active, add them as strings between the brackets underneath the additionalActiveStatusOptions variable. This can be helpful if you want to define multiple stages for Active projects. Example: ["Design", "Development", "QA"] */ lets( 	planned, 	/* Edit the term below this line to translated the Planned status. E.g. "Zaplanowany" */ 	"Planned", 	onHold, 	/* Edit the term below this line to translated the On Hold status. "Zastopowany" */ 	"On Hold", 	doing, 	/* Edit the term below this line to translated the Doing status. "W trakcie" */ 	"Doing", 	ongoing, 	/* Edit the term below this line to translated the Ongoing status. "Otwarty" */ 	"Ongoing", 	done, 	/* Edit the term below this line to translated the Done status. "Zakończony" */ 	"Done", 	additionalActiveStatusOptions, 	/* If you would like to add additional Active status options, add them as strings between these brackets. See the comment above for an example. */ 	[], 	/* Do not edit anything below this line */ 	[ 		[planned, onHold], 		[doing, ongoing, additionalActiveStatusOptions].flat(), 		[done] 	] )`
Code language: JavaScript (javascript)

### Meta

**Type:** Formula

Displays the number of active (e.g. unfinished) tasks related to the project, as well as the number of overdue tasks.

CODE

`lets( 	active, 	prop("Tasks") 		.filter( 			current.prop("Status") != "Done" and current.prop("Status") != current.prop("Localization Key").at(2).last() 		) 		.length(), 	overdue, 	prop("Tasks") 		.filter( 			current.prop("Status") != "Done" and current.prop("Status") != current.prop("Localization Key").at(2).last() 			and now().dateBetween(current.prop("Due"), "days") > 0 		) 		.length(), 	ifs( 		prop("Tasks").length() == 0, 		"No Tasks Created".style("blue"), 		active == 0, 		"All Tasks Done".style("green"), 		if( 			overdue == 0, 			(active + " Active").style("blue"), 			(active.style("b") + " Active - ").style("blue")  			+ (overdue.style("b") + " Overdue").style("red") 		) 	) )`
Code language: JavaScript (javascript)

### Name

**Type:** Title

The name of the project.

### Notes

**Type:** Relation

Notes that are directly related to this Project. This Relation property connects to the Project Relation property in the Notes database.

### People

**Type:** Relation

This property shows any people related to this project (e.g. a client or partner). This relation property connects to the Projects relation property in the People database.

### Progress

**Type:** Formula

Shows the progress of the project by calculating the percentage of completed tasks related to it.

CODE

`if( 	prop("Tasks").empty(), 	"".toNumber(), 	(prop("Tasks") 	.filter( 		current.prop("Status") == "Done" or current.prop("Status") == current.prop("Localization Key").at(2).last() 	) 	.length() / prop("Tasks") 	.length() 	* 1000) .round()  / 1000 )`
Code language: JavaScript (javascript)

### Pulled Notes

**Type:** Relation

This Relation property allows you to “pull” existing notes into the Pulled Notes view within this Project’s page body.

Pulled Notes are notes that aren’t directly related to the project (not created as part of the project), but that may provide useful context to the project.

### Pulled Tags

**Type:** Relation

This Relation property allows you to “pull” ALL of the Notes from a specific Tag page in the Tags database into the Pulled Notes view within this Project’s page body.

See the Pulled Notes property description/reference for more info on this property’s function.

### Quarter

**Type:** Formula

Returns the quarter of the year for the project’s Target Deadline. Used in the Plan page to filter projects into each quarter of the year.

CODE

`prop("Target Deadline").formatDate("Q")`
Code language: JavaScript (javascript)

### Review Notes

**Type:** Rich Text

Add any quick notes about this project here.

### Status

**Type:** Status

The status of the project. For “projects” that contain ongoing/maintenance items, use the Ongoing status.

| Name | Description |
| --- | --- |
| Group: To-do |
| Planned | Projects that you’ve planned to do, but haven’t started yet. |
| On Hold | Projects that have already been started, but are currently paused from active work. |
| Group: In progress |
| Ongoing | Projects that collect and organize tasks meant to maintain an ongoing standard (e.g. maintenance tasks in an Area within the PARA system). |
| Doing | Projects with a defined end goal that you’re currently working on. |
| Group: Complete |
| Done | Projects that are complete. |

### Tag

**Type:** Relation

The Tag with which this Project is associated. This Relation property connects to the Projects Relation property in the Tags database.

It’s typically best to associate Projects with Tags that have a type of Area (and have the Area database template applied).

### Target Deadline

**Type:** Date

The date on which you plan to complete this project. Useful for planning backwards – if you have a Target Deadline, you can more easily set due dates for any tasks related to the project.

### Tasks

**Type:** Relation

Tasks that are part of this Project. This Relation property connects to the Project Relation property in the Tasks database.

### This Quarter

**Type:** Formula

Returns true (checked) if this project’s Target Deadline falls within the current quarter of the year.

CODE

`if( 	empty(prop("Target Deadline")), 	false, 	prop("Target Deadline").formatDate("QY") == now().formatDate("QY") )`
Code language: JavaScript (javascript)

### This Year

**Type:** Formula

Returns true (checked) if this project’s Target Deadline falls within the current year. Used in the Plan page to filter projects into the correct timeframes.

CODE

`if( 	empty(prop("Target Deadline")), 	false, 	prop("Target Deadline").formatDate("Y") == now().formatDate("Y") )`
Code language: JavaScript (javascript)

### Time Tracked

**Type:** Formula

The total time tracked across all tasks related to this project, formatted in HH:MM:SS (Seconds will always be “00”).

Uses the Time Tracked (Mins) property, which contains the total number of tracked minutes as a numeric value.

CODE

`lets(   duration, prop("Time Tracked (Mins)").sum(),  hours, floor(duration / 60),  hoursLabel, hours < 10 ? "0" + hours : hours,  minutes, duration % 60,  minutesLabel, minutes < 10 ? "0" + minutes : minutes,  hoursLabel + ":" + minutesLabel + ":00" )`
Code language: JavaScript (javascript)

### Time Tracked (Mins)

**Type:** Formula

The total number of minutes tracked on tasks related to this project.

CODE

`prop("Tasks").map(   current.prop("Time Tracked (Mins)") ).sum()`
Code language: JavaScript (javascript)
