---
title: Genres Database — Full Schema Reference
purpose: Complete property reference for the Genres database used to categorize books
agent_relevant: true
key_topics: [genres, books, categories, database, schema]
related_docs: [databases/books.md]
---
# Genres

The **Genres** database stores all of your genres. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Use genres to organize Books in your library.

Below you’ll find a reference guide for all database templates and properties in the Genres database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Genre Template

This template creates two gallery views of the [Books](https://thomasjfrank.com/docs/ultimate-brain/databases/books/) database:

-   By Title
-   By Author

Each of these views show book records that are related to this Genre.

## Properties

### Books

**Type:** Relation

Books within this Genre.

This Relation property connects to the Genres Relation property in the Books database.

### Name

**Type:** Title

The name of the Genre. Note that a Book can have multiple Genres.

### Stats

**Type:** Formula

Displays the number of books in the Genre, along with the number that you’ve read and how many you own.

Owned Books include any book that has an entry in the Owned Formats property (within the Books database).

CODE

`lets( 	booksCount, 	prop("Books").length(), 	countPlurality, 	booksCount != 1 ? "s" : "", 	booksRead, 	prop("Books").filter( 		current.prop("Status") == "Read" 	).length(), 	booksOwned, 	prop("Books").filter( 		current.prop("Owned Formats").length() >= 1 	).length(), 	(booksCount + " book" + countPlurality).style("c","b","blue","blue_background") + " " + 	(booksRead + " read").style("c","b","orange","orange_background") + " " + 	(booksOwned + " owned").style("c","b","green","green_background") )`
Code language: JavaScript (javascript)
