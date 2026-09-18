---
title: Meal Planner Database — Full Schema Reference
purpose: Complete property reference for the Meal Planner database
agent_relevant: true
key_topics: [meal-planner, meals, date, breakfast, lunch, dinner, snack, recipes, time-estimate, database, schema]
related_docs: [databases/recipes.md, databases/recipe-tags.md]
---
# Meal Planner

The **Meal Planner** database stores full meal plans. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Meal plans can reference multiple Recipes, creating a go-to plan for cooking an entire meal.

Below you’ll find a reference guide for all properties in the Meal Planner database. Properties are in alphabetical order.

## Properties

### Date

**Type:** Date

The date on which you plan to make this meal.

If you’d like to re-use a meal plan on the Calendar, simply move the date!

### Favorite

**Type:** Checkbox

Check this meal plan as a Favorite to make it show up in Favorite Meal views.

### Meal

**Type:** Status

The meal of the day (e.g. time of day) for this planned meal.

| Name | Description |
| --- | --- |
| Group: To-do |
| Group: In progress |
| Breakfast | N/A |
| Lunch | N/A |
| Dinner | N/A |
| Snack | N/A |
| Other | N/A |
| Group: Complete |

### Name

**Type:** Title

The name of the meal plan.

### Recipes

**Type:** Relation

The Recipes that are part of this meal plan.

This Relation property points to the Recipes database.

### Time Est.

**Type:** Formula

The estimated time it will take to make this full meal.

By default, this value matches the highest total time from all of the Recipes attached to this meal plan.

You can input a time into Time Override to override that default time.

CODE

`lets( 		totalTimeRaw, 		if( 			!empty(prop("Time Override")), 			prop("Time Override"), 			prop("Recipes").map(current.prop("Total Time")).max() 		), 		hours, 		floor(totalTimeRaw / 60), 		minutes, 		totalTimeRaw % 60, 		hourPlurality, 		if( 			hours == 1, 			"", 			"s" 		), 		minutePlurality, 		if( 			minutes == 1, 			"", 			"s" 		), 		time, 		ifs( 			totalTimeRaw > 60, 			hours + " hr" + hourPlurality + ", " + minutes + " minute" + minutePlurality, 			totalTimeRaw == 60, 			hours + " hr", 			minutes + " min" + minutePlurality 		), 		badgeColor, 		ifs( 			totalTimeRaw >= 90, "red", 			totalTimeRaw >= 45, "yellow", 			"green" 		), 		time.style( 			"c", 			"b", 			badgeColor, 			badgeColor + "_background" 		) 	)`
Code language: JavaScript (javascript)

### Time Est. (num)

**Type:** Formula

This property returns the same value as the Time Est. property, but as a numeric value instead of a string. Used for sorting meals in Quick/By Time views.

CODE

`if( 	!empty(prop("Time Override")), 	prop("Time Override"), 	prop("Recipes").map(current.prop("Total Time")).max() )`
Code language: JavaScript (javascript)

### Time Override

**Type:** Number

Add a number (in minutes) here to override the default value in the Time Est. property.
