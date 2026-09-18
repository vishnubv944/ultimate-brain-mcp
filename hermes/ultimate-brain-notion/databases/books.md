---
title: Books Database — Full Schema Reference
purpose: Complete property reference for the Books library database
agent_relevant: true
key_topics: [books, library, database, schema, properties, status, rating, genres, reading-log, owned-formats, shelf]
related_docs: [databases/reading-log.md, databases/genres.md]
---
# Books

The **Books** database stores all of the books you’ve added to your library. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates and properties in the Books database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Book Template

This template pre-populates a Book page with sections for a summary, key insights, and book notes. It also create a view of the [Reading Log](https://thomasjfrank.com/docs/ultimate-brain/databases/reading-log/), showing log entries related to this Book.

## Properties

### Added

**Type:** Created Time

The creation time of this database record.

### Author

**Type:** Rich Text

The author(s) of the book.

### Author (L,F)

**Type:** Formula

The author’s name in last name, first name order. Used for sorting books in the By Author view in My Library.

In the case of multiple authors, the order is maintained from the Author property in order to keep the primary author in first position.

CODE

`prop("Author").split(", ").map( 	lets( 		suffixes, 		["Jr.", "Sr.", "II", "III", "IV", "V", "MD", "PhD", "DDS", "Esq.", "OBE", "QC"], 		parts, 		current.split(" "), 		suffixFlag, 		if( 			suffixes.includes(parts.last()), 			true, 			false 		), 		if( 			parts.length() == 1, 			current, 			if( 				suffixFlag, 				parts.at(parts.length() - 2) + ", " + parts.slice(0, parts.length() - 2).join(" ") + " " + parts.last(), 				parts.last() + ", " + parts.slice(0, parts.length() - 1).join(" ") 			) 		) 	) )`
Code language: JavaScript (javascript)

### Date Finished

**Type:** Date

The date you finished the book.

### Date Started

**Type:** Date

The date you started the book.

### Description

**Type:** Rich Text

Feel free to put the book’s official book jacket copy here, or write your own custom description.

### Genres

**Type:** Relation

The book’s Genres.

This Relation property connects to the Books Relation property in the Genres database.

### ISBN-13

**Type:** Number

The book’s ISBN-13 number. If you’re using this template’s book-scanning automation, this value will be set automatically.

### Image

**Type:** Files

The book’s cover image.

This template uses the actual Page Cover of your book pages to display cover images, rather than this property.

However, I’ve included this property as well, just in case future Notion updates let you better display images from database properties.

### Log Button

**Type:** Button

Creates a new Reading Log entry with today’s date set as the Log Date and this book set as the Book.

### Logs

**Type:** Relation

All log entries from the Reading Log with this book set as the Book relation value.

This Relation property connects to the Book Relation property in the Logs database.

### Owned Formats

**Type:** Multi Select

If you add any option in this property, the book is considered Owned and can be seen in the Owned view within My Library.

| Name | Description |
| --- | --- |
| Paperback | N/A |
| Hardback | N/A |
| Kindle | N/A |
| Audiobook | N/A |

### Pages

**Type:** Number

The number of pagers in the book.

This number can usually be auto-filled via this template’s book-scanning companion automation, which can get the page count from Google Books.

If you’re reading on Kindle, you can display page counts instead of locations!

### Progress

**Type:** Formula

Displays a progress bar if the Pages property has a page count and there is at least one entry in the Reading Log for this book with an End Page value.

Progress bar is visible in the Current view of the Books by Status section.

CODE

`let( 	furthestPage, 	prop("Logs").filter(not empty(current.prop("End Page"))).sort(current.prop("Log Date")).reverse().first().prop("End Page"), 	not empty(prop("Pages")) ? round(furthestPage / prop("Pages") * 100) / 100 : "".toNumber() )`
Code language: JavaScript (javascript)

### Publish Year

**Type:** Number

The book’s publish year.

This is a number property instead of a date property. You’re welcome to turn this into a date property if you want to record specific publish dates.

However, this can cause problems with API integrations that automatically add a publish time.

### Rating

**Type:** Select

Your rating of the book out of 5 stars (5 is best, 1 is worst).

| Name | Description |
| --- | --- |
| ⭐️⭐️⭐️⭐️⭐️ | N/A |
| ⭐️⭐️⭐️⭐️ | N/A |
| ⭐️⭐️⭐️ | N/A |
| ⭐️⭐️ | N/A |
| ⭐️ | N/A |

### Read Next

**Type:** Checkbox

Check this checkbox if you’d like to read this book soon.

Books marked as Read Next will be sorted ahead of other books in the To Read tab of Books by Status.

### Shelf

**Type:** Multi Select

Use this property to create custom “shelves” for book – e.g. Favorites, Books to Gift, Reread-Worthy, Holiday Reads, etc.

See the By Shelf tab in My Library to filter books by Shelf.

| Name | Description |
| --- | --- |
| Favorites | N/A |
| Reread-Worthy | N/A |
| Books to Gift | N/A |

### Status

**Type:** Status

Your reading status for the book.

| Name | Description |
| --- | --- |
| Group: To-do |
| Want to Read | N/A |
| Group: In progress |
| On Hold | N/A |
| Reading | N/A |
| Group: Complete |
| Read | N/A |

### Title

**Type:** Title

The title of the book.

Note that, by default, Notion databases name this primary property “Name”. It has been re-named to “Title” in this template.
