---
title: Creating Custom Databases
purpose: How to add entirely new databases to Ultimate Brain and integrate them with existing ones
agent_relevant: true
key_topics: [custom-databases, new-database, integration, relations, adding-databases]
related_docs: [setup/unlocking-databases.md, setup/customizing.md]
---
# Integrating Custom Databases into Ultimate Brain

Ultimate Brain ships with a lot of features by default, but you may still have use cases that it can’t handle right out of the box.

Luckily, it’s easy to **add new functionality** by creating your own custom databases and _integrating_ them into Ultimate Brain’s default databases.

## Example Project: Add a Companies Database

In the video tutorial below, I’ll show you exactly how to do that. In this example project, we’ll upgrade the Ultimate Brain’s CRM by creating a **Companies** database. When you’re finished, you’ll be able to:

-   Create pages for companies you’re working with (i.e. as a consultant)
-   Relate **Projects** and **People** to Companies

Here’s a condensed, text-based companion to the video tutorial.

Before making any changes, consider [backing up your template](https://thomasjfrank.com/docs/ultimate-brain/backing-up-ultimate-brain/) to ensure you have an untampered version.

Start by [unlocking the databases](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/) you’ll be working with. In this case, unlock both the [Projects](https://thomasjfrank.com/docs/ultimate-brain/databases/projects/) and [People](https://thomasjfrank.com/docs/ultimate-brain/databases/people/) databases.

Create a new full-page database called **Companies** beneath the People database. I recommend choosing a red icon for it, as I use red icons for all databases in Ultimate Brain.

Next, create two Relation properties in the Companies database:

1.  Create a **People** relation property, linking to the **People** database. Set it as a two-way relation and name the reverse property Company in the People database.
2.  Create a **Projects** relation property, linking to the **Projects** database. Set it as a two-way relation and name the reverse property Company in the Projects database.

To make it easier to associate companies with projects, customize the Projects database layout:

1.  Open any existing project and click the Customize button above the page title.
2.  In the Layout Builder, add the Company property to the Relations group. Do this by clicking the Property Group, then clicking the Company Property and hitting Add to Layout. Afterwards, click the `•••` button in the new Company section’s box, go to **Advanced**, and click **Add (or Convert) to Relation Group.**
3.  Set the property to show as minimal.

Repeat this process for the People database to add the Company relation property to its layout.

Now, create a template for the Companies database:

1.  In the Companies database, create a new template by clicking the blue ↓ button next to **New**, then clicking Create Template.
2.  Add two sections: “Projects” and “People”.
3.  For each section, create a List-type linked database view of the respective database.
4.  Apply a filter to each view: “Company → Contains → New Page” (Replace “New Page” with the template’s name, if you named it.)
5.  Customize the properties shown in each view as needed.

Set this template as the default for all views in the Companies database.

Finally, customize the layout of the Companies database pages:

1.  Move the “Projects” and “People” relation properties to the Relations group or pin them to the top of the page.
2.  Apply the changes to all pages.

Remember to lock the Projects and People databases again after making these changes.
