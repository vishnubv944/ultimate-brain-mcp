---
title: Work Sessions Database — Full Schema Reference
purpose: Complete property reference for the Work Sessions time-tracking database
agent_relevant: true
key_topics: [work-sessions, time-tracking, start, end, duration, task, sessions, database, schema]
related_docs: [features/time-tracking.md, databases/tasks.md]
---
# Work Sessions

The **Work Sessions** database stores all of your time-tracking sessions. This database enables time-tracking in Ultimate Brain. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates and properties in the Work Sessions database. Properties are in alphabetical order.

This database is coming to Ultimate Brain in a future upgrade. Currently, this reference document is meant for use with our [time-tracking tutorial](https://thomasjfrank.com/docs/ultimate-brain/how-to-add-time-tracking-to-ultimate-brain/).

## Properties

### Duration

**Type:** Formula

This session’s duration, formatted in HH:MM:SS as a string value.

This formula relies on the Duration (Mins) property’s numeric value.

CODE

`lets(   duration, prop("Duration (Mins)"),  hours, floor(duration / 60),  hoursLabel, hours < 10 ? "0" + hours : hours,  minutes, duration % 60,  minutesLabel, minutes < 10 ? "0" + minutes : minutes,  hoursLabel + ":" + minutesLabel + ":00" )`
Code language: JavaScript (javascript)

### Duration (Mins)

**Type:** Formula

This session’s duration in minutes. Calculated by subtracting the Start date/time value from the End date/time value.

CODE

`let(   duration,  if(    empty(prop("End")),    dateBetween(now(), prop("Start"), "minutes"),    dateBetween(prop("End"), prop("Start"), "minutes")  ),  duration >= 0 ? duration : 0 )`
Code language: JavaScript (javascript)

### End

**Type:** Date

The date/time the session ended.

### End Session

**Type:** Button

Click this button to end this work session by setting the current date/time in the End property.

If there is already value there, clicking this button will overwrite it with the current date/time.


To configure the End Session button, create a single Edit Pages in Action.

Set the action to Edit this page, and change the End property to the Time Triggered dynamic value.

Click Save.

### Name

**Type:** Title

The name of this session. This is usually generated automatically when you click the Start button within a Task.

### Project

**Type:** Rollup

The project (if any) to which this session’s task is related.

### Start

**Type:** Date

The date/time the session started.

### Start/End

**Type:** Formula

A date range constructed from the Start and End date values.

Used as the Calendar By property in the Calendar view within the Work Sessions source database.

This allows you to view sessions in Notion Calendar.

CODE

`dateRange(   prop("Start"),  prop("End") )`
Code language: JavaScript (javascript)

### Task

**Type:** Relation

**Limit:** 1 Page

**Related Property:** Sessions (in the [Tasks](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/) database)

This session’s task.

This Relation property connects to the Sessions relation property in the Tasks database.

### Team Member

**Type:** People

**Limit:** 1

**Default:** None

The person who completed this work session. A “Work Session” is work done by a single person on a single task for a period of time.

While Ultimate Brain is meant for personal use, this property helps to future-proof the template in case you want to allow other people to use it with you.
