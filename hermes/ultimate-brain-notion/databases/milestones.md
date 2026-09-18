---
title: Milestones Database — Full Schema Reference
purpose: Complete property reference for the Milestones database used to track goal progress checkpoints
agent_relevant: true
key_topics: [milestones, goals, progress, checkpoints, database, schema, target-deadline, date-completed]
related_docs: [databases/goals.md]
---
# Milestones

The **Milestones** database stores all of your milestones, which you can use to mark progress within Goals. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all properties in the Milestones database. Properties are in alphabetical order.

## Properties

### Date Completed

**Type:** Date

The date you passed the milestone.

### Goal

**Type:** Relation

The goal to which this milestone is related. This Relation property is connected to the Milestones Relation property in the Goals database.

### Goal Area

**Type:** Rollup

The Area from the Areas/Resources database to which this milestone’s goal is related (if there is one).

Rollup configuration:

-   **Relation:** Goal
-   **Property:** Tag
-   **Calculate:** Show Original

### Name

**Type:** Title

The name of the milestone. It is recommended to be descriptive and ensure the milestone is measurable.

### Target Deadline

**Type:** Date

The date by which you’d like to cross the milestone.
