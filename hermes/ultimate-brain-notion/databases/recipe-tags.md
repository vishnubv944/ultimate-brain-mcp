---
title: Recipe Tags Database — Full Schema Reference
purpose: Complete property reference for the Recipe Tags categorization database
agent_relevant: true
key_topics: [recipe-tags, categories, style, course, equipment, database, schema]
related_docs: [databases/recipes.md, databases/meal-planner.md]
---
# Recipe Tags

The **Recipe Tags** database stores tags that you can use to organize your Recipes. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates and properties in the Recipe Tags database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Tag

This template adds several useful views of the [Recipes](https://thomasjfrank.com/docs/ultimate-brain/databases/recipes/) database to the page, showing recipes that are related to this Recipe Tag.

## Properties

### Name

**Type:** Title

The name of the recipe tag.

### Recipe Count

**Type:** Formula

The number of Recipes that have this Recipe Tag applied.

CODE

`lets( 	recipeCount, 	prop("Recipes").length(), 	plurality, 	if( 		recipeCount == 1, 		"", 		"s" 	), 	( 		recipeCount.format() + 		" recipe" + 		plurality 	).style( 		"c", "b", "blue", "blue_background" 	) )`
Code language: JavaScript (javascript)

### Recipes

**Type:** Relation

Recipes that have this Recipe Tag applied.

This Relation property connects to the Tags property in the Recipes database.

### Type

**Type:** Status

This Recipe Tag’s type. Use this to create categories of tags, such as Style, Course, Equipment, etc.

To add or change options, unlock the Recipes database.

| Name | Description |
| --- | --- |
| Group: To-do |
| Group: In progress |
| Style | N/A |
| Course | N/A |
| Tag | N/A |
| Equipment | N/A |
| Group: Complete |
