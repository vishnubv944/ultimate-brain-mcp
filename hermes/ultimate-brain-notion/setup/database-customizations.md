---
title: Database Customizations — Relation Limits
purpose: Advanced database customization options including relation property limits and display settings
agent_relevant: true
key_topics: [database, relations, limits, customization, relation-limit, advanced]
related_docs: [setup/unlocking-databases.md, setup/adding-properties.md]
---
# Database Customizations

On this page, you’ll find quick tips on making small, database-related customizations in Ultimate Brain.

The sections on this page are typically created after we answer a similar question in our [customer community](https://community.thomasjfrank.com/), which you can access once you’ve [purchased Ultimate Brain](https://thomasjfrank.com/brain/).

**Note:** Before making a change to a database, you’ll always need to [unlock that database](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/) first.

## Change a Relation Property’s Page Limit

Relation properties can link to an unlimited number of pages within their connected database by default.

However, you can also change the **Limit** setting in a Relation property to **1 Page**. This creates a **1-to-many relationship**.

Some Relation properties in Ultimate Brain already have this 1-page limit setting. The Project Relation in the Tasks database is a good example; in almost all cases, a task should only belong to one project. If you that certain Relation properties in Ultimate Brain only allow you to choose a single page, this is why.

If you like, you can change this setting on any Relation property.

[Here’s an example video on Loom, where you’ll see me change this setting to allow multiple Tag pages to be related to a single Project](https://www.loom.com/share/0e48ce1115fc4d0eaa4a5e48a2f171e7).

To do this yourself (for _any_ Relation property), start by navigating to the database that contains the Relation property and [unlocking it](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/).

1.  Go to the Ultimate Brain homepage
2.  Scroll down to find the [Databases & Components](https://thomasjfrank.com/docs/ultimate-brain/pages/databases-components/) page
3.  Locate the [Projects database](https://thomasjfrank.com/docs/ultimate-brain/databases/projects/)
4.  Click the **Locked** button at the top of the page to unlock it

Once the database is unlocked, modify the Relation property:

1.  Click on the property you want to change
2.  Select **Edit property**
3.  Look for the **Limit** option in the property settings
4.  Change the setting from **1 Page** to **No Limit**

To test the change, open any page in the database. You’ll now see a **New** button instead of a **Replace** button next to your Relation property. Click **New** to create a _brand-new_ page in the Relation’s connected database, or click the **magnifying glass** (🔎) to link another existing page.
