---
title: Notes Database — Full Schema Reference
purpose: Complete property reference for the Notes database — all properties, types, and note type options
agent_relevant: true
key_topics: [notes, database, schema, properties, type, tag, project, people, url, voice-note, journal, meeting, web-clip]
related_docs: [10-web-clips-highlights.md, 14-voice-notes.md, databases/tags.md]
---
# Notes

The **Notes** database stores all of your notes and web clips. It’s the primary database that drives note-taking and research in Ultimate Brain. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates properties in the Notes database. Properties are in alphabetical order.

**Note:** In versions of Ultimate Brain released prior to November 2024, this database is called **All Notes.**

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Journal: \[Date\]

This template is used for daily journal entries. It allows you to plan and review your day, manage tasks, and write a free-form journal entry.

### Week Journal: \[Date\]

This template is used for weekly planning. Used primarily in the [My Week](https://thomasjfrank.com/docs/ultimate-brain/pages/my-week/) page for [Weekly Planning](https://thomasjfrank.com/docs/ultimate-brain/weekly-planning/), it allows you to plan your work, review the previous week, and manage tasks.

### Meeting: \[Date\]

This template gives you a helpful starting point for recording meeting notes.

### Note with Tasks

This is a simple template that comes with a tasks view.

## Properties

### AI Cost

**Type:** Number

Used for voice notes captured and transcribed with Thomas Frank’s Voice Notes to Notion workflow.

Displays the total cost of the workflow run, including transcription and summarization costs from all involved AI models.

### Archived

**Type:** Checkbox

Check this to archive this note. Archived notes are removed from all main locations in Ultimate Brain and go to the Archived Notes section of the Archive page.

### Created

**Type:** Created Time

The date and time this note was created.

### Duration

**Type:** Formula

Uses the Duration (Seconds) number property to display the duration of a captured voice note in HH:MM:SS format.

CODE

`prop("Duration (Seconds)").empty() ? "" :  	lets( 		hour, 		floor(prop("Duration (Seconds)") / 3600), 		minute, 		floor((prop("Duration (Seconds)") % 3600) / 60), 		second, 		floor(prop("Duration (Seconds)") % 3600 % 60), 		[ 			hour < 10 ? "0" + hour : hour, 			minute < 10 ? "0" + minute : minute, 			second < 10 ? "0" + second: second 		] 	).join(":")`
Code language: JavaScript (javascript)

### Duration (Seconds)

**Type:** Number

The duration of a voice note captured and transcribed with Thomas Frank’s Voice Notes to Notion workflow.

This duration is expressed in seconds, and is later formatted in the Duration formula property.

### Favorite

**Type:** Checkbox

Check this to mark a note as a Favorite, and have it show in the Favorites section of Notes views.

### Image

**Type:** Files

An image file associated with this note.

### Name

**Type:** Title

The title of the note.

### Note Date

**Type:** Date

This property allows you to manually set a date for this note. Useful for Meeting notes and Journal entries, which may need a different date than the read-only Created date.

### People

**Type:** Relation

Any People records related to this note. Used most often with the Meeting Notes template, or when creating an interaction log with one or more people.

### Project

**Type:** Relation

The Project to which this note is directly related – i.e. its content is explicitly part of the project.

If you’d like to associate reference materials to a project, you may want to use the Pulls property instead.

### Project Archived

**Type:** Rollup

Displays whether or not the project related to this note (if there is one) is archived.

### Project People

**Type:** Rollup

Shows any People records associated with this note’s Project (if it has one). Used in People pages.

### Project Tag

**Type:** Rollup

If this note is related to a Project within the Project relation property, this will show that Project’s Tag (if it has one).

### Pulls

**Type:** Relation

This Relation property shows Projects that have “pulled” this note in as reference material.

Pulls are notes that aren’t directly related to a project, but that may provide helpful context and reference info.

### Review Date

**Type:** Date

Useful for planning a date in the future on which to review this note – e.g. mid-November for a holiday gift-planning note.

The Calendar view in the All Notes source database uses this property, allowing you to add it to Notion Calendar.

### Root Tag

**Type:** Rollup

If this note is related to a Tag that has a Parent Tag, this will show that Parent Tag.

### Tag

**Type:** Relation

The Tag(s) to which this note is related.

### Tag Pulls

**Type:** Rollup

If this note is part of a Tag that has been pulled into a Project, that project will be displayed here for reference purposes.

See the Pulls property description for more info.

### Type

**Type:** Select

Allows you to tag your notes with a Type. Useful in Tag pages, within the By Type view.

| Name | Description |
| --- | --- |
| Journal | N/A |
| Meeting | N/A |
| Web Clip | N/A |
| Lecture | N/A |
| Reference | N/A |
| Book | N/A |
| Idea | N/A |
| Plan | N/A |
| Recipe | N/A |
| Voice Note | N/A |
| Daily | N/A |

### URL

**Type:** Url

A URL associated with this note. Most commonly used for web clips/captures taken with Flylighter (our web clipper) or the Notion web clipper.

### URL Base

**Type:** Formula

The base domain of this note’s URL property.

CODE

`prop("URL") 	.replace("[^]*//", "") 	.split("/") 	.first()`
Code language: JavaScript (javascript)

### URL Icon

**Type:** Formula

Simply displays a small, clickable icon that links to this page’s URL, if it exists. If the page doesn’t have a URL, this will be empty.

Useful for making Web Clips clickable in condensed list views of Resources and Notes views (e.g. Side Peek, mobile).

CODE

`if( 	empty(prop("URL")), 	"", 	"🔗".style("c", "gray_background").link(prop("URL")) )`
Code language: JavaScript (javascript)

### Updated

**Type:** Last Edited Time

The base domain of this note’s URL property. Used in the Web Clips section of Resource pages, specifically in the By Site view.

### Updated (Short)

**Type:** Formula

A small, styled label displaying how many days have passed since this note was updated. Displayed in the Recents view of the Notes dashboard, among other places.

CODE

`(dateBetween(now(), prop("Updated"), "days") + "d").style("c","b","blue","blue_background")`
Code language: JavaScript (javascript)

## UB + CC Exclusive Properties

The fully-integrated version of Ultimate Brain + Creator’s Companion contains a few exclusive Notes properties that are not found in the standalone version of Ultimate Brain.

### Channel

**Type:** Relation

Channels to which this note is directly associated. Typically, this will gain relations when you create research notes directly from a Channel page.

### Content

**Type:** Relation

Content projects related to this note.

This Relation property connects to the Notes Relation property in the Content database.

### Content Keywords

**Type:** Rollup

This rollups shows any Focus Keywords set on content projects related to this piece of research.

This drives the Related Research view within keyword pages.

### Keywords

**Type:** Relation

Keywords that are associated with this piece of research. Typically, this property will gain relations when you add research items directly from a keyword’s page.

This Relation property connects to the Notes Relation property in the Keywords database.

### Sponsor

**Type:** Relation

Sponsors that are associated with this piece of research. Typically, this property will gain relations when you add research items directly from a sponsor’s page.
