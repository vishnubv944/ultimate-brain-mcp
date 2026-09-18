---
title: Tags Database — Full Schema Reference
purpose: Complete property reference for the Tags (Areas and Resources) database — PARA method implementation
agent_relevant: true
key_topics: [tags, areas, resources, entities, para, database, schema, properties, type, sub-tags, parent-tag, automations]
related_docs: [features/overview.md, databases/projects.md, databases/goals.md]
---
# Tags

The **Tags** database stores all of your tags. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Tags can have different types, with the primary types being **Area** and **Resource.** Tags of these types are used to enable [PARA Method](https://thomasjfrank.com/productivity/how-to-easily-organize-your-life-with-the-para-method/) organization in Ultimate Brain.

Below you’ll find a reference guide for all database templates and properties in the Tags database. Properties are in alphabetical order.

**Note:** In versions of Ultimate Brain released prior to November 2024, this database is called **Areas/Resources.**

## Automations

### Create Ongoing Project for Areas

-   **Default Status:** Disabled
-   **Description:** When you change the create a new Tag page, apply the Area type, and give it a name, a corresponding Ongoing Project will be created for it. E.g. if you create a “Work” Area page, you’ll automatically get a “Work Ongoing” project, which will be related to it.
-   **Released:** [2025-01-21: Time Tracking, Daily Planning Updates, and More!](https://thomasjfrank.com/docs/ultimate-brain/changelog/#time-tracking-daily-planning-updates-and-more)


Name formula:

CODE

`context("Trigger page").format() + " Ongoing"`
Code language: JavaScript (javascript)

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Area

This template is used to create Areas when practicing the [PARA method](https://thomasjfrank.com/docs/ultimate-brain/para-method/) for life organization. Areas display many things, including:

-   Projects & Tasks
-   Notes
-   Web Clips (notes with either a URL or the “Web Clip” Type)
-   People
-   Goals
-   Sub-Tags (found in the Sub-Tags relation, displayed minimally under that the page title)

### Resource

This template is used to create Resources when practicing the [PARA method](https://thomasjfrank.com/docs/ultimate-brain/para-method/) for life organization. Resources display:

-   Notes
-   Web Clips (notes with either a URL or the “Web Clip” Type)

### Entity

This template is used to create Entities – Tag pages that are useful for organizing information by its _type_ rather than _topic_ – e.g. “Apps”, “Essays”, “Manuals”, etc. Its page content is funcitonally equivalent to Resources, displaying:

-   Notes
-   Web Clips (notes with either a URL or the “Web Clip” Type)

## Properties

### Archived

**Type:** Checkbox

If checked, this Tag will be archived. It will disappear from all main dashboards in Ultimate Brain and show up in the Archived Tags view in the Archive.

### Favorite

**Type:** Checkbox

If checked, this Tag will display in the Favorites section within Tags views.

### Goals

**Type:** Relation

Any goals associated with this Tag.

This Relation property connects to the Tag Relation property in the Goals database.

This property is best used on Tag pages with the Area type (with the Area database template applied).

### Latest Activity

**Type:** Formula

Returns the date and time of the latest activity in the Tag – including edits to its directly as well as changes to notes, tasks, and projects.

Used for allowing Tags to be sorted by Latest Activity in certain views.

CODE

`lets( 	projectActivity, 	prop("Projects").map(current.prop("Latest Activity")).sort().reverse(), 	noteActivity, 	prop("Notes").map(current.prop("Updated")).sort().reverse(), 	editedList, 	[prop("Last Edited Time")], 	concat(projectActivity, noteActivity, editedList).sort().reverse().first() )`
Code language: JavaScript (javascript)

### Latest Note

**Type:** Formula

Fetches the note within this Tag that was most recently updated.

CODE

`prop("Notes") 	.filter(current.prop("Archived") == false) 	.sort(current.prop("Updated")) 	.reverse() 	.first()`
Code language: JavaScript (javascript)

### Name

**Type:** Title

The name of the Tag.

### Note Count

**Type:** Formula

Displays the number of non-archived notes associated with this Tag.

CODE

`lets( 	count, 	prop("Notes").filter(current.prop("Archived") == false).length(), 	plural, 	count == 1 ? " " : "s", 	count + " note" + plural ).style("c","b","blue","blue_background")`
Code language: JavaScript (javascript)

### Notes

**Type:** Relation

Any notes associated with this Tag. This Relation property connects to the Tag Relation property in the Notes database.

### Parent Tag

**Type:** Relation

This Tag’s parent. This Relation property connects to the Sub-Tags Relation property in this database.

### People

**Type:** Relation

Any People records associated with this Tag. Best used to associate People with Tags that have the Area type when practicing PARA organization.

This Relation property connects to the Tags Relation property in the People database.

### Projects

**Type:** Relation

Any projects associated with this Tag. This Relation property connects to the Tag Relation property in the Projects database.

It’s best to use this relation with Tag pages that have the Area type and Area database template applied.

### Pulls

**Type:** Relation

Projects that have pulled this Tag in order to display its notes within the project’s Pulled Notes section. This Relation property connects to the Pulled Tags Relation in the Projects database.

### Sub-Tags

**Type:** Relation

Sub-Tags underneath this Tag. This Relation property connects to the Parent Tag Relation property in this database.

Example use: Set Resource pages to be “within” an Area page.

### Tag Projects

**Type:** Formula

Displays a label with the number of active projects within this Tag, if there are any.

Best used with Tag pages that have the Area type.

CODE

`lets( 	count, 	prop("Projects").filter(current.prop("Status") == "Doing" or current.prop("Status") == "Ongoing").length(), 	ifs( 		count > 0, 		( 			count + " Active Project" + ifs(count != 1, "s") 		).style("b","c","green","green_background") 	) )`
Code language: JavaScript (javascript)

### Type

**Type:** Status

Allows you to give this page a specific tag type.

If you practice PARA, you’ll usually want to choose Area or Resource. You can also use the Entity option to build meta-collections, such as “Essays”.

You should also choose the corresponding page Template in the page body below.

| Name | Description |
| --- | --- |
| Group: To-do |
| Group: In progress |
| Entity | “Entity” is useful for building meta-collections in your second brain, such as “Essays” or “Apps”. It’s not part of PARA. It’s often useful to apply both a Resource and an Entity tag to a page – e.g. “Programming” as a topical Resource and “App” as an Entity. |
| Area | In the PARA Method, an Area is an “ongoing sphere of responsibility”. Areas represent the main categories of your life – Home, Work, School, Health, etc. They may have Notes, Projects, Goals, and even sub-Tags (such as a Resource). |
| Resource | In the PARA Method, a Resource is a “topic or interest that may be useful in the future”. Resource is the default Type within the Tags database. Use Resources to organize Notes about a specific topic or subject. |
| Group: Complete |
