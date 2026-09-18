---
title: Reading Log Database — Full Schema Reference
purpose: Complete property reference for the Reading Log (reading sessions) database
agent_relevant: true
key_topics: [reading-log, sessions, book, log-date, pages, database, schema]
related_docs: [databases/books.md]
---
# Reading Log

The **Reading Log** database stores all of your reading log entries. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

A log entry records the date, start and end page, and Book that you read.

Below you’ll find a reference guide for all database templates and properties in the Reading Log database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### @Today Reading Log

This simple template contains no page content, but it does auto-fill the Log Date property with the current date.

## Properties

### Book

**Type:** Relation

The book you read. If you read multiple books in a day, create a log entry for each one.

This Relation property connects to the Logs Relation property in the Books database.

### Details

**Type:** Rich Text

Record any details about your reading session here.

While I’d recommend taking actual book notes in the Book entry, you could do it here, too.

### End Page

**Type:** Number

The page on which you ended the reading session.

Used with Start Page to calculate Pages Read.

### Last End Page

**Type:** Formula

The end page of the most recent previous log entry for this book. This property essentially functions as a bookmark, and can help you fill out Start Page for the next entry.

CODE

`let( 	lastEntry, 	prop("Book").first().prop("Logs").filter(not empty(current.prop("End Page"))).sort(current.prop("Log Date")).reverse().first(), 	"Page " + lastEntry.prop("End Page") + ", " + (lastEntry.prop("Log Date").format()).link(lastEntry.id()) )`
Code language: JavaScript (javascript)

### Log Date

**Type:** Date

The date of your reading session. This date should be auto-filled with the current date when you create a new entry.

### Month Index

**Type:** Formula

Used to make the Last Month view in the Reading Log work properly. When a log entry is from last month, this index will be -1. The “Last Month” view filters entries so that only those with -1 will be shown. This is done because Notion lacks a “last calendar month” filter.

CODE

`dateBetween( 	dateSubtract(now(), date(now()), "days"), 	dateSubtract(prop("Log Date"), date(prop("Log Date")), "days"), 	"months" ) * -1`
Code language: JavaScript (javascript)

### Name

**Type:** Title

The name of your entry. Defaults to “\[Date\] Reading Log”.

You can change this by editing the database template in this Reading Log database.

### Pages Read

**Type:** Formula

The total number of pages read during this session. This is calculated by subtracting Start Page from End Page.

CODE

`if( 	not empty(prop("Start Page")) && not empty(prop("End Page")), 	prop("End Page") - prop("Start Page"), 	0 )`
Code language: JavaScript (javascript)

### Start Page

**Type:** Number

The page you started on during this reading session.

Used along with End Page to calculate Pages Read.
