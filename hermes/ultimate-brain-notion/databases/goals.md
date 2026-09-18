---
title: Goals Database — Full Schema Reference
purpose: Complete property reference for the Goals database
agent_relevant: true
key_topics: [goals, database, schema, properties, status, milestones, projects, tag, deadline, achieved]
related_docs: [databases/milestones.md, databases/projects.md, databases/tags.md]
---
# Goals

The **Goals** database stores all of your goals. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Goals, in contrast to Projects, are aspirational and may not have a specific path toward achievement that you can perfectly map out right now. Notably, Projects can be part of Goals in Ultimate Brain.

Below you’ll find a reference guide for all database templates and properties in the Goals database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Goal

This template creates a “home base” for this goal. On the page, you can easily create and track [Milestones](https://thomasjfrank.com/docs/ultimate-brain/databases/milestones/), which are points of progress in your journey towards achieving the goal.

There’s also a [Projects](https://thomasjfrank.com/docs/ultimate-brain/databases/projects/) view that allows you to associate projects with the goal. Using goal-associated projects, you can create tasks that help you work towards the goal. _Tasks don’t related directly to Milestones or Goals._

## Properties

### Achieved

**Type:** Date

The date you achieved the goal and crossed off all milestones.

### Archived

**Type:** Checkbox

Check this checkbox if you’d like to archive this goal. Archived goals disappear from the Goals dashboard and show up in the Archived Goals section of the Archive page.

It is recommend only to archive goals you don’t intend on achieving and are no longer interested in.

### Goal Set

**Type:** Date

The date you set the goal.

### Latest Activity

**Type:** Formula

Returns the date and time of the latest activity in the Goal – including edits to it directly as well as changes to projects and milestones.

Used for allowing Goals to be sorted by Latest Activity in certain views.

CODE

`lets( 	projectActivity, 	prop("Projects").map(current.prop("Latest Activity")).sort().reverse(), 	milestoneActivity, 	prop("Milestones").map(current.prop("Last Edited Time")).sort().reverse(), 	editedList, 	[prop("Last Edited Time")], 	concat(projectActivity, milestoneActivity, editedList).sort().reverse().first() )`
Code language: JavaScript (javascript)

### Milestones

**Type:** Relation

Milestones that are related to this Goal. This Relation property connects to the Goal Relation property in the Projects database.

### Name

**Type:** Title

The name of the goal. It is recommend to be descriptive and choose a name with a measurable outcome.

### Progress

**Type:** Formula

Shows the percentage of milestones related to this goal that are complete, displayed as a progress bar.

CODE

`if( 	prop("Milestones").empty(), 	"".toNumber(), 	(prop("Milestones") 	.filter( 		current.prop("Date Completed") 	) 	.length() / prop("Milestones") 	.length() 	* 1000) .round() / 1000 )`
Code language: JavaScript (javascript)

### Projects

**Type:** Relation

Projects that are related to this Goal. This Relation property connects to the Goal Relation property in the Projects database.

### Status

**Type:** Status

That status of the goal.

| Name | Description |
| --- | --- |
| Group: To-do |
| Dream | You’ve set this goal, but it’s still just a dream. You’re not actively working towards it yet. |
| Group: In progress |
| Active | You’re now actively working towards this goal. You’ve set a Target Deadline, you have metrics and checkpoints in mind, and you’ve got Projects set up to help make and track progress. |
| Group: Complete |
| Achieved | You did it! |

### Tag

**Type:** Relation

The Tag that this Goal is a part of.

This Relation property is connected to the Goals Relation property in the Tags database.

It’s best to associate Goals with Tag pages that have the Area type (and that have the Area database template applied).

### Target Deadline

**Type:** Date

The date by which you hope to achieve the goal.

Note that goals differ from projects in that it’s not always clear if you’ll be able to meet the target deadline. What is important is that you work diligently toward it and make progress.

### This Quarter

**Type:** Formula

Returns true if this goal’s Target Deadline is in the current quarter number (e.g. 1 for Jan-Mar). In the Year Planner page, this property is used along with This Year to determine which goals have a Target Deadline in the current quarter.

CODE

`if( 	empty(prop("Target Deadline")), 	false, 	prop("Target Deadline").formatDate("QY") == now().formatDate("QY") )`
Code language: JavaScript (javascript)

### This Year

**Type:** Formula

Returns true if this goal’s Target Deadline is in the current year. In the Year Planner page, this property is used along with This Quarter to determine which goals have a Target Deadline in the current quarter.

CODE

`if( 	empty(prop("Target Deadline")), 	false, 	prop("Target Deadline").formatDate("Y") == now().formatDate("Y") )`
Code language: JavaScript (javascript)

### Updated

**Type:** Last Edited Time

The date and time this goal was last updated.
