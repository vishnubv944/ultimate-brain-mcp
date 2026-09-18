---
title: Recipes Database — Full Schema Reference
purpose: Complete property reference for the Recipes database
agent_relevant: true
key_topics: [recipes, database, schema, properties, prep-time, cook-time, servings, recipe-tags, quick, inbox]
related_docs: [databases/recipe-tags.md, databases/meal-planner.md]
---
# Recipes

The **Recipes** database stores all of your recipes. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates and properties in the Recipes database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Recipe Template

This simple template creates sections for Ingredients, Instructions, and Notes about the recipe.

### Recipe w/ Shopping List

This template adds all the sections from the Recipe Template, and also adds a filtered view of the [Tasks](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/) database, showing only tasks with the “Shopping” Context.

**Note:** I added this view for folks who really want to create grocery lists in Notion, but I personally find it better to reference my recipes in Notion and add grocery items to the stock Reminders app on iOS.

## Properties

### Chef Name

**Type:** Rich Text

The name of the chef/author of this Recipe.

### Cook Time

**Type:** Number

The cook time for this recipe.

### Created

**Type:** Created Time

The date when this Recipe was added/created.

### Edited

**Type:** Last Edited Time

The date on which this Recipe was last edited.

### Favorite

**Type:** Checkbox

Mark this Recipe as a Favorite to have it show up in your Favorite Recipes views.

### Inbox

**Type:** Checkbox

When checked, this Recipe will only show in your Recipe Inbox.

### Is Quick

**Type:** Formula

Will be checked if the total time for this recipe is at or below the threshold value.

By default, the threshold for a Quick recipe is 45 minutes. You can unlock the Recipes database and edit this formula to change this value.

CODE

`prop("Total Time") <= 45`
Code language: JavaScript (javascript)

### Meal Time

**Type:** Multi Select

The meal time(s) for this recipe.

| Name | Description |
| --- | --- |
| Breakfast | N/A |
| Lunch | N/A |
| Dinner | N/A |
| Snack | N/A |

### Name

**Type:** Title

The name of the recipe.

### Prep Time

**Type:** Number

The prep time for this recipe.

### Servings

**Type:** Number

The number of servings this recipe makes.

### Tags

**Type:** Relation

The Tags attached to this Recipe.

This Relation property connects to the Recipes relation property in the Recipe Tags database.

### Time Card

**Type:** Formula

The total time for making this recipe (prep + cook time), formatted as a cute little card (string value).

CODE

`lets( 	totalTimeRaw, 	prop("Cook Time") + prop("Prep Time"), 	hours, 	floor(totalTimeRaw / 60), 	minutes, 	totalTimeRaw % 60, 	hourPlurality, 	if( 		hours == 1, 		"", 		"s" 	), 	minutePlurality, 	if( 		minutes == 1, 		"", 		"s" 	), 	time, 	ifs( 		totalTimeRaw > 60, 		hours + " hr" + hourPlurality + ", " + minutes + " minute" + minutePlurality, 		totalTimeRaw == 60, 		hours + " hr", 		minutes + " min" + minutePlurality 	), 	time.style( 		"c", 		"b", 		"green", 		"green_background" 	) )`
Code language: JavaScript (javascript)

### Total Time

**Type:** Formula

The total time for making this recipe (prep + cook time), formatted as a numeric value. Used for sorting in the Quick views.

CODE

`prop("Cook Time") + prop("Prep Time")`
Code language: JavaScript (javascript)

### URL

**Type:** Url

The URL of this recipe, if it was found online.

### URL Base

**Type:** Formula

The base domain of this note’s URL property.

CODE

`prop("URL") 	.replace("[^]*//", "") 	.split("/") 	.first()`
Code language: JavaScript (javascript)
