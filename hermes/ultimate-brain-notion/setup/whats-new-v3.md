---
title: What's New in Ultimate Brain 3.0
purpose: Overview of major changes and new features introduced in Ultimate Brain 3.0
agent_relevant: true
key_topics: [v3, changelog, new-features, people-crm, redesign, time-tracking, my-day, books, recipes]
related_docs: [setup/changelog.md, features/overview.md]
---
# What’s New in Ultimate Brain 3.0

Ultimate Brain 3.0, launched in November of 2024, is a ground-up redesign of Ultimate Brain.

If you’re coming from a previous version of Ultimate Brain, you’ll be happy to know that UB 3.0 retains all the core pieces – tasks, notes, projects, goals, PARA organization, and more.

All of these pieces are laid out in a much more intuitive, easy-to-use way to takes better advantage of core Notion features, such as Side Peek for databases.

In addition, UB 3.0 comes with many new features. Here are a few at a glance:

-   A redesigned, action-oriented Home Page
-   Native recurring tasks support (no third-party tools needed!)
-   The **People** dashboard – a full-fledged personal CRM
-   The **Books** dashboard – a new, expanded book tracker
-   The **Recipes** dashboard – a new, expanded recipe tracker with meal-planning features
-   Full support for Notion’s new Layouts feature
-   A brand-new **My Day** experience, laser-focused on helping you prioritize your day’s tasks

Aside from the template itself, we’ve been hard at work on [Flylighter](https://flylighter.com/) – our super-customizable Notion web clipper that makes capturing research and highlights to Notion fast and easy. While you don’t need UB 3.0 to use Flylighter, the two tools make a killer combo.

Below, you’ll find more detailed sections going over the feature listed above, along with some other changes.

You can also check out this tour of all the new features. I created this video when Ultimate Brain 3.0 was in beta (roughly three weeks before final release), but nothing shown in the video has been changed since then.

If you don’t have your own copy of Ultimate Brain yet, [you can get the latest version right now](https://thomasjfrank.com/brain/).

## Action-Oriented Homepage

In Ultimate Brain 2.0, we introduced **Ultimate Brain Lite** – a stripped-down dashboard within UB that displayed only the essentials: Tasks, Notes, Projects, Areas, and Resources.

The feedback on UB Lite was so positive that the same concept now underpins UB as a whole. Now, the UB “home page” is no longer a giant list of pages (which overwhelmed some users).

Instead, it’s an immediately-useful dashboard that lets you jump right into:

-   Tasks
-   Notes
-   Projects
-   Tags (which is the Areas/Resources database, renamed for clarity and added flexibility)

For most people, these are the most-accessed parts of UB. With this change, you can essentially run your life from a single page. Once you need more flexibility, _then_ you can dig into UB’s many dashboards. But they won’t overwhelm you from the get-go.

We’ve also included a **Dashboard Component Library,** full of additional components that you can _optionally_ copy and paste onto your UB home page. These are perfectly-tuned and ready to go; you’ll find components for Goals, People, Books, and Recipes.

## An Intuitive Template Layout

Another massive change you’ll notice in UB 3.0 is the lack of multi-column dashboards and layouts. Nearly everything in UB 3.0 is single-column.

I made this change because of Notion’s ultra-useful Side Peek mode. Side Peek works best on single-column pages with List views, which will resize themselves responsively when the Side Peek panel opens up.

On desktop and in the browser, this allows you to open up any Project, Tag, Note, or Task to see its details – while still having access to everything else. This means you can very quickly switch between different notes, tasks, etc.

To further support this change, each database view on the homepage has an expanded set of tabs. These allow you to easily find nearly anything you’re looking for.

Want multi-column layouts anyway? It’s extremely easy to customize your UB pages in order to get them. Just type `/2c` on any block, then drag your headings and database views into the columns.

## Native Recurring Tasks

Notion’s new formula automations _finally_ enable native recurring task processing. Finally!

Ultimate Brain already contains the most powerful set of recurring-task properties and formulas you’ll find in any Notion template. (Martin from our team has spent over 4 months in total working on these formulas and our previous automations around them)

Now, UB 3.0 ships with a built-in Automation that allows these formulas to process your recurring tasks automatically – no Pipedream or Make.com scripts needed! Once you set a task to Done, it’ll be automatically updated with the Next Due date, and its Status will be set back to To Do.

If you’ve ever used recurring tasks in Todoist, TickTick, or another dedicated to-do app, you’ll finally feel right at home.

And since we’ve built this automation for you, you’ll be able to use it even if you’re on Notion’s free plan. While the creating and editing Automations are reserved for users on Notion’s paid plans, Automations that come in templates will work even if you’re on the free plan.

Even better, Ultimate Brain 3.0 also ships with an optional **Advanced Recurring Tasks** automation that adds **Task History.** This allows you to see each time you’ve completed a recurring task in the past, directly on that recurring task’s main page. [Learn how to enable this included automation here.](https://thomasjfrank.com/docs/ultimate-brain/recurring-tasks/#advanced-recurring-tasks-with-task-history)

**Note:** We’re exploring other Automations that we could ship in our templates, but we’re typically cautious about including them because users would be forced to upgrade to a paid plan in order to add them to their own templates. Where possible, we try to make our current Notion templates work 100% without needing a paid Notion plan. However, you can expect to see more **tutorials** around Automations in the near future.

## PARA Improvements

Ultimate Brain originally shipped with a database called **Areas/Resources.** Somewhat confusingly-named, it’s a database that combines both Areas and Resources from the [PARA Method](https://thomasjfrank.com/productivity/how-to-easily-organize-your-life-with-the-para-method/).

This database is now simply called **Tags.** It still supports Areas and Resources – and comes with built-in tempaltes for each – but it has a simpler name that represents the additional flexibility Notion offers.

The Tags database comes with three default Types (and templates for each):

1.  Area – an ongoing sphere of reponsibility in your life (e.g. Health, Business)
2.  Resource – a topical area of interest (e.g. Gardening, Streaming)
3.  Entity – a way to tag “types” of content (e.g. Essays, Apps, Cool Websites)

The default Area template has also been upgraded include a People section, which allows you to associate CRM records with specific Areas.

## People: A Complete Personal CRM

Ultimate Brain now includes a contact tracker. The **People** dashboard lets you build out a personal CRM, where each Person record can have:

-   Title, company, location info
-   Contact details – phone, email, LinkedIn, Instagram, Twitter, website, etc
-   Relationship type
-   Interests
-   Gift Ideas

You can also create Meeting Notes directly from a Person page, and you can ensure you stay in touch with people using the Check-In property to record check-in dates. You can also easily use these dates to schedule check-ins on Notion Calendar.

Finally, the Pipeline view will let you track outreach efforts for potential customers or clients. Together with the Check-In property and calendar views, this view makes the People dashboard a powerful, lightweight CRM for certain business use cases.

In the near future, we’ll be adding special data types to [Flylighter](https://flylighter.com/) in order to make it even easier to capture CRM records directly to Ultimate Brain from your browser.

## Books: Library and Reading Tracker

Ultimate Brain 3.0 comes with **Books,** a complete book tracking system. This is a huge upgrade from the handful of book-related properties that previously in the Notes database.

Books includes three databases:

-   Books – tracks title, author, publish date, page counts, ISBN number, owned status, shelves, and more
-   Genres – lets you easily add books to one or more genres for better organization
-   Reading Log – tracks reading sessions for specific books, allowing you to note start/end pages per-session

With Books, you have a single place to collect your notes on all the books you’re reading. You can also use it to build up a TBR (To Be Read list), note which books you’re currently reading, and keep track of the ones you’ve finished.

The **Shelf** property lets you create custom collections like “Favorites” and “Books to Gift” – the latter being a great shelf for holidays.

Best of all, you can easily share your Books databases with other people without exposing the rest of your Ultimate Brain systems. Since Books uses its own set of databases, you can easily share them without sharing anything else.

In the near future, I’ll be creating a tutorial that will show you how to **scan book barcodes** with an iPhone to add books directly to Notion. We’ll also add a similar automation to [Flylighter](https://flylighter.com/) a bit later on, so you can add books directly from sites like Goodreads and Amazon!

## Recipes: Capture Recipes and Create Meal Plans

Ultimate Brain 3.0 comes with **Recipes,** a complete recipe tracking and meal-planning system. This is a huge upgrade from the handful of recipe-related properties that previously in the Notes database.

Recipes includes three databases:

-   Recipes – tracks recipe URLs, servings, cook time, meal time, etc
-   Recipe Tags – lets you easily organize recipes with custom tags. Create tags for courses, (Appetizer, Beverage, Side, Entree), styles (Japanese, Szechuan, Mexican, New American), etc.
-   Meal Plans – combines multiple Recipes to create go-to meal plans, which you can then schedule on Notion Calendar.

Just like with Books, you can share your entire Recipes dashboard and databases without exposing the rest of your Ultimate Brain.

A major reason Recipes got this redesign was so I could share my own recipes with my friends and family. I couldn’t do that in the past, when recipes were just part of the Notes database.

You can easily capture recipes from websites to your Recipes database using Flylighter. In the near future, we’ll also be shipping improvements to [Flylighter](https://flylighter.com/) that make it even easier to grab “recipe cards” from websites, and to map pieces of data to Notion properties! (e.g. servings and prep/cook time)

## My Day and My Week

The **My Day** dashboard has been completely re-designed in Ultimate Brain 3.0.

It is now a rigid, opinionated dashboard designed to help you:

-   Know exactly what you need to work on today
-   Create a list of **only** those tasks
-   Clear that list by the end of the day – no matter what

My Day uses the same Tasks database you’ll see in every other part of Ultimate Brain. This makes it an _optional_ way of managing what you need to get done. If you want, you can use the normal Task views instead.

However, if you want a tool that will help you **prioritize** your tasks and **stick to your plan**, My Day is for you. It’s built on a simple, three-step process:

1.  **Plan** – Look over your tasks and projects. If a task should be done today, check the My Day checkbox property.
2.  **Execute** – All tasks with My Day checked show up in the Execute section, which is grouped by Status. As you do your tasks, check them off. Ideally, they’ll all be in the Done group by the end of the day.
3.  **Wrap Up** – Update any task details as needed; for example, move Due Dates on tasks that need to be delayed. Finally, click the **Clear My Day** button to un-check My Day on all tasks, no matter their status.

Hitting **Clear My Day** at the end of each day is crucial. It prevents you from letting tasks pile up over time, and ensures the Execute view truly represents your plan for each day. After hitting the button, you can easily add tasks _back_ to Execute if you plan on doing them tomorrow.

In addition to My Day, UB 3.0 ships with a brand-new **My Week** dashboard.

My Week is designed to help you keep your productivity system organized and running smoothly. We’ve all experienced **digital cruft** – that gumming-up of your systems as tasks, notes, and files pile up until you don’t know where anything is or what’s important anymore.

My Week comes with a set of views designed to guide you through the process of **clearing to neutral** – or as a professional chef would say, tending to your _mise-en-place_:

-   The **Clear & Reset** section encourages you to clear out inboxes, reschedule (or delete) overdue tasks, and make sure Projects, Tags, and Goals are organized.
-   The **Reflect & Set Intent** section allows you to look over the past week’s Journals and Meetings notes. You can also review all notes from the last week, and optionally create a **week journal** entry.
-   The **Plan the Week** section lets you look over the _upcoming_ week, scheduling task due dates as needed.

Again, My Day and My Week are optional. Like the **Process (GTD)** dashboard, they offer an alternative way of managing your digital productivity system. They leverage the power of Notion, using the same underlying databases that are used through Ultimate Brain.

This means you can use them when they’re useful, and stick to the more flexible, easily-accessible parts of UB at all other times.

## Additional Features and Upgrades

In addition to the major features above, UB 3.0 has received dozes of other minor upgrades and changes. Here are a few worth noting:

-   Notion Calendar Support – most databases have pre-made calendar views, which allow you to view their content in Notion Calendar by default.
-   The Tasks database is now a “tasks” database, meaning your tasks will show up in Notion Home.
-   There’s a new “My Day” checkbox on Tasks, which adds tasks to the Execute view in My Day.

## What Got Removed?

If you’re coming from a previous version of Ultimate Brain, here’s a quick list of some features that have either been removed or significantly change in UB 3.0:

-   **Dashboard** – The Ultimate Brain homepage now acts as the Dashboard.
-   **Ultimate Brain Lite** – With the new, streamlined homepage, there’s no longer any need for a separate Lite view.
-   **Plan** – Renamed to **My Year.**
-   **Cold Tasks** – These caused significant confusion, so they’ve been removed. Removing them also allows the filter sets in nearly all Task views to be far simpler.
-   **Areas** and **Resources** pages – You’ll now find these nested under the **Tags** page.
-   **Quick Links** – Removed, but it’s very easy to create this page yourself!
-   **Scratchpad** – Removed, but it’s very easy to create this page yourself!
-   **“Done” checkbox property in Tasks** – Replaced by the Status property (previously called Kanban Status), which is shown as a checkbox on most views.
-   **Recipe and Books properties in Notes** – Now, Recipes and Books have their own sets of databases. This de-clutters the Notes database, and allows for far more features in Recipes and Books. It also allows you to **share** your Recipes/Books databases without needing to share the rest of UB.

## How to Upgrade

Ultimate Brain 3.0 is a fundamental redesign of Ultimate Brain. Instead of making changes directly to the previous version of the template, I rebuilt it from scratch. This allowed me to make conscious decisions at every stage of the design process.

For this reason, directly upgrading a previous version of the template isn’t advised. Instead, I advise taking one of two paths:

1.  Just start using UB 3.0 from a blank slate, and bring in information from your previous template as-needed. (I effectively did this when I moved to Notion from Evernote, and I don’t regret it).
2.  Transfer the data from your copy of Ultimate Brain to a fresh copy of UB 3.0. [You can find our transfer guide here](https://thomasjfrank.com/docs/ultimate-brain/transfer-guide/).

I did tried doing a direct upgrade of our production version of Creator’s Companion, manually upgrading it to match Creator’s Companion 3.0 (launching alongside UB 3.0). It took a full week! By contrast, a data transfer takes about an hour.

If you really want to manually upgrade your current copy to UB 3.0, the only realistic way to do it is to download a **reference copy** of UB 3.0, then refer to it in order to modify your database properties and then create a full set of new pages.

Making direct changes to your current database views and pages is technically possible – but in the same way that counting the grains of sand on a beach is technically possible. There are so many changes that attempting to do it would be far too complex to be worth it.
