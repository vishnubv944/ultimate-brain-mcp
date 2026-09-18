---
title: Recurring Tasks
purpose: How to set up and use recurring tasks — intervals, recur units, specific days, automations, and task history
agent_relevant: true
key_topics: [recurring-tasks, recur-interval, recur-unit, automation, next-due, task-history, days, enforce-schedule]
related_docs: [databases/tasks.md, features/managing-tasks.md]
---
# Recurring Tasks

Ultimate Brain comes with out-of-the-box support for recurring tasks. They just _work._ 😎

When you give a task a **Due Date** and a **Recur Interval**, each time you set that task’s Status to **Done,** Notion will automatically update the Due Date and set the Status back to To Do.

In short, recurring tasks in Ultimate Brain work just like they do in any other task management app. It comes with a built-in automation to process tasks as you finish them, which means there’s no need for a third-party app like Zapier or Pipedream to be in the mix.

Ultimate Brain also has the most powerful recurring tasks formula you’re ever going to see ([you can see it here](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/#next-due)), which means it:

-   Supports advanced intervals like _Last Weekday of the Month_
-   Supports recurring on specific days of the week, e.g. _“Every Mon, Weds, Fri”_
-   Intelligently handles overdue tasks

## How to Create Recurring Tasks

Creating a recurring task in Ultimate Brain is easy. As long as a task has a **Due Date** and a **Recur Interval,** it’s a recurring task.

Want to make a task that repeats every day? Give it a Due Date, then set its Recur Interval to `1`. You’re done.

Of course, you can get a lot fancier with your intervals if you want. Here’s the complete guide to setting up recurring tasks:

Open up any task after you’ve created it, and you’ll see a **Recurring Task Properties** section in the property panel. This section shows three properties you can edit:

-   **Recur Interval** – the number of units to recur by
-   **Recur Unit** – the unit, e.g. _“Day(s)”, “Week(s)”,_ or _“Month(s) on the Last Weekday”_
-   **Days** – lets you set specific days of the week (this property was previously called **Days (Only if Set to 1 Days(s))**. If you duplicated your template prior to **January 20, 2025,** this will be its name.)

**Hint:** If you don’t see the property panel, hit the `•••` button next to the pinned properties under the page title. Alternatively, use the **⌘**/Ctrl + Shift + \\ keyboard shortcut.

Once you’ve added both a **Due Date** and a **Recur Interval**, you’ll see a **Next Due** property show up as well. This is the date on which the task is _next due,_ after the current due date. When Notion automatically processes the task, it’ll use the Next Due date to set the new Due Date.

By default, tasks recur every _X_ days, where _X_ is the number you entered in **Recur Interval.**

You can change this by setting the **Recur Unit** property. Here are a few examples:

| Recur Interval | Recur Unit | Interval |
| --- | --- | --- |
| 1 | Day(s) | Daily |
| 3 | Week(s) | Every 3 weeks |
| 2 | Month(s) on the Last Weekday | Every 2 months on the last weekday of the month |

But what if you want a task to only recur on certain days – like Monday, Wednesday, and Friday?

This is where the **Days** property comes in. This property has two functions:

First, it can make a task recur on _multiple days of the week_ – for example, Mon/Wed/Friday. Enable this by setting **Recur Unit** to **Day(s)** and **Recur Interval** to `1`, then choosings the days you want for the task.

Second, it can be used with the “Nth Weekday of Month” option in **Recur Unit** to make a task recur only during a specific week of the month – for example, “Every 3rd Thursday of the month”. You can enable a schedule like this with the following settings:

-   Recur Inverval: 3
-   Recur Unit: Nth Weekday of Month
-   Days: Thursday

For this type of schedule, only a single day of the week is supported.

If you’d like to make the task recur on the **first** week of the month, set Recur Interval to `1`. For the **last** week of the month, set Recur Interval to `5`.

**Note:** Prior to **January 20, 2025**, this property was called **Days (Only if Set to 1 Days(s))**. If your template uses this name, it will only support choosing specific days of the week for a task to recur – and only when Recur Interval is 1 and Recur Unit if Day(s).

## Automatic Recurring Task Processing

Ultimate Brain comes with a database automation that will process recurring tasks immediately after their **Status** is set to **Done.**

Most views in Ultimate Brain show the **Status** property as a checkbox, so this means the automation will trigger when you check off a task.

The automation does the following:

-   Sets the **Due Date** property to the current value of **Next Due**
-   Sets **Status** back to **To Do**

This automation will _only_ trigger for tasks that have a **Recur Interval** of `1` or greater.

In general, this automation should just work for you. If you do want to edit it, note that you’ll need to be on a **paid** Notion plan. This is because [database automations](https://thomasjfrank.com/notion-database-automations-the-complete-guide/) require a paid plan to be edited or created – though folks on the free plan can use automations that ship with templates.

## Advanced Recurring Tasks with Task History

Ultimate Brain 3.0 also ships with an Advanced Recurring Tasks automation. Much like the one described above, it triggers when a recurring task’s Status is set to Done, and it sets the Status back to To Do while updating the Due date.

Additionally, it enables **Task History.** When the automation runs, it will create a _duplicate_ of the recurring task, which will inherit the Due date from the recurring task, a Completion Date, and some of the other properties from the recurring task.

You can then apply the **Recurring Task w/ History** page template to your original recurring task, and you’ll see a list of all instances when you’ve completed that task in the past.

This automation is disabled by default in Ultimate Brain.

To enable it, follow these steps:

1.  Unlock your Tasks database.
2.  Click the ⚡️ icon on any view of the Tasks database to access its Automations.
3.  Click the Recurring Tasks (Simple) automation and make it **inactive.**
4.  Click the Recurring Tasks (Advanced) automation and make it **active.**

You can do this process even if you’re on Notion’s free plan. However, keep in mind that actually _editing_ the automations, or creating new ones, requires being on a paid Notion plan.

If you’re on a paid Notion plan, it’s also possible to tweak this automation so that the duplicated page becomes the recurring task, while the original task remains Done. This can be beneficial if you need to record unique information on the page body for each instance of a recurring task. See the accordion toggle below labeled **Using the Duplicated Task as the New Recurring Task** for more details.

## Fully Understanding Recurring Tasks

If you would like to fully understand how recurring tasks work in Notion, and learn how to set them up from scratch yourself, I’ve published a YouTube video that will teach you everything you need to know:

Additionally, you can check out my [full article on recurring tasks in Notion](https://thomasjfrank.com/notion-automated-recurring-tasks/) to learn more.

## Advanced Customization

Below, you’ll find some additional guidance around customizing the way recurring tasks work. These sections are based on answers we’ve written for customers in our [support community](https://community.thomasjfrank.com/c/ub-support/).


In the video below, you can see me building the Advanced Recurring Tasks Automation (with Task History) shown above from scratch. You likely won’t need to do this, as the automation comes standard with Ultimate Brain 3.0, but I’ve posted it here for anyone on a legacy template who may want to build it.

[Watch the full video on Loom.](https://www.loom.com/share/c2d91ee87eae4377b13d54bca74b2980)


If you want, you can modify Ultimate Brain to calculate the next due date of a recurring task solely based on the date that you complete it.

By default, the Next Due property in the Tasks database will intelligently calculate the next due date of a recurring task based on _both_ the currently set due date and the current date.

For example, if you have a weekly task that was due on Monday and you complete it on Thursday, three days late, the next due date will be the following Monday. If you completed the task 10 days late on the _following_ Thursday, then the next due date would be the Monday after that.

Essentially, the Due Date property helps to set the interval of the recurring task, and then the current date is used to help count how many intervals the next due date should move forward to ensure that the next due date is always in the future.

Most modern task management apps use this logic when calculating the next due dates of recurring tasks. However, if you want the next due dates of recurring tasks to be calculated only based on the completion date, you can do that easily. Before you begin, ensure your [Tasks](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/) database [is unlocked](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/). If you’re not sure how to do this, you can refer to our doc on database unlocking for guidance.

The first step is to find the [Next Due](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/#next-due) formula in your Tasks database. This is the formula we’ll be adjusting to change how recurring tasks behave. Once you’ve located it, we’ll focus on changing the `dueProp` variable.

Currently, the `dueProp` variable uses the value of the [Due](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/#due) property. We’re going to change it to use [today()](https://thomasjfrank.com/formulas/functions/today/) instead. Replace the current `dueProp` definition with:

CODE

`dueProp, today(),`
Code language: JavaScript (javascript)

This modification will cause the formula to use today’s date as the basis for calculating the Next Due date, regardless of the task’s original Due date.

If you want to get a bit fancier, you can add conditional logic to only use today’s date for overdue tasks while keeping the original due date for tasks due in the future. To do this, replace the `dueProp` definition with:

CODE

`dueProp, if(prop("Due") > today(), prop("Due"), today()),`
Code language: JavaScript (javascript)

This conditional statement checks if the due date is in the future. If so, it uses the actual due date. If not (meaning the task is overdue or due today), it uses today’s date.


By default, the Advanced Recurring Tasks Automation in Ultimate Brain will create a duplicate of each recurring task that you check off.

The duplicate is considered the **historical record**, so it does not itself become a recurring task. Instead, its status is automatically set to Done, it gets a completion date, and it serves as a historical marker for a completed instance of that recurring task.

However, it is possible to reverse how this automation works. In the video below, I’ll show you how to tweak the advanced recurring tasks automation so that the duplicated page becomes the recurring task itself, and the page that triggered the automation becomes the historical record.

This can be beneficial if each individual instance of a recurring task requires unique information to be placed in its page.

One of our customers asked:

> “Is it possible to make it if I complete a daily task after midnight it would change it to the same day? I usually stay up pretty late and recurring tasks would skip a day. I don’t like manually setting the next date if after midnight. If there’s a way to delay the Next Due date by 4 hours I’d love that.”

Fortunately, this is possible! It just involves editing the Next Due formula. _You’ll need to unlock your Tasks database before you can edit this formula._

If you take a look at the [Next Due](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/#next-due) formula’s code, you’ll find a line like this (you may want to use **⌘**/Ctrl + F to find it):

CODE

`timeNow, now(),`
Code language: JavaScript (javascript)

This section defines a variable (within [lets](https://thomasjfrank.com/formulas/functions/lets/)) that gets the current date and time from the [now](https://thomasjfrank.com/formulas/functions/now/) function.

Therefore, we can change this line to the following:

CODE

`timeNow, if( 	now().formatDate("H").toNumber() < 4, 	dateSubtract(now(), 1, "days"), 	now() ),`
Code language: JavaScript (javascript)

Now, the value of the `timeNow` variable is dynamically determined based on the current time of day. If the current time is earlier than 4am, `timeNow` is one day earlier than the current date. This will cause the Next Due formula to calculate the next due date as if it was still yesterday.

**Note:** This solution has only been tested with the native database automation that processes recurring tasks directly in Notion. It has not been tested with the older Pipedream script. You’d need to update the [Next Due API](https://thomasjfrank.com/docs/ultimate-brain/databases/tasks/#next-due-api) property in the same manner described above for this to work, and even then it may not work due to how the Notion API handles time values.


Traditional calendar apps like Google Calendar allow you to set an end date on repeating events. For example, a repeating event like “MATH 101” could be set to occur every M/W/F _until_ May 15th.

This useful feature is missing from many traditional task management apps (though not all). But can you add it to Notion?

As it turns out, you can! In the video below, I’ll show you how to edit your copy of Ultimate Brain’s Tasks database so you can set:

-   An **End Date** for recurring tasks (e.g. the last occurrence is May 15th)
-   A **Limit** for recurring tasks (e.g. only do this task 5 times)

_Note that you’ll need to be on a paid Notion plan to do this, as it involves editing the Recurring Tasks automation. Creating and editing Automations [requires a paid Notion plan](https://www.notion.com/help/database-automations#who-can-use-database-automations)._

To make this upgrade work better with the Advanced Recurring Tasks automation (which enables Task History), you’ll want to do the following:

First, set your custom formula for **Recur Interval** to the following. Note that this you won’t need the **Limit Reached** variable step anymore, as this does essentially the same thing – with two notable changes:

1.  The limit is reached if Next Due is `>=` the Ends On date (in the video above, we use `>`)
2.  The limit is reached if Occurrence Limit is `2`

Next, set Occurrence Limit to the following custom formula value. If there is an occurrence limit set on a task, this will cause it to **decrement** each time the automation runs.

Finally, make sure that Due and Status are set back to their original values:

-   Due should be `Trigger Page.Next Due`
-   Status should be `To Do`

With these changes, the automation will no longer set conditional values on Due or Status. Instead, the automation will only strip off the recur interval when the limit is reached.

The upshot is that the Task will be due one more time, but it will no longer be recurring. We need to do this because when Task History is enabled, running the automation as shown in the video above (on the **final** instance of the Task) will result in a final Task History entry being created, even though the Task itself should function as this final entry.

In effect, you’d have two instances for the final occurrence of the Task.

With this change, the limit is instead reached on the penultimate instance of the task. This allows the task itself to function as the final instance. It effectively becomes a 1-time task once it reaches its final scheduled occurrence.

We need to make this change because Notion Automations does not provide us with tools to conditionally create new pages. We can’t tell the automation to only create a new page if certain conditions are met. A page will be created when the automation runs, no matter what.

**Note:** There is one small downside to this change. If you’re going to set an end date for a recurring task, the end date needs to be on one of the dates where the recurring task would be scheduled. If the end date is in between occurrences of the recurring task, then you’ll end up with one more occurrence than you intended to have. You could overcome this issue by creating a “Next Next Due” formula property that calculates the 2nd upcoming occurrence of the task; however, this feels very inelegant to me, and I worry about it creating potential performance issues for folks using slower computers.


Do you have recurring tasks that would have subtasks?

In general, my recommendation is to make those “subtasks” into simple to-do boxes because of a few key limitations in Notion’s automation capabilities, which make combining actual subtasks and recurring tasks difficult.

When building an automation to process a recurring task, we set its Due date to the Next Due date and we change its Status back to “To Do”.

So in order to process related Sub-Tasks, we would also want to change their due dates and update their statuses as well.

But when editing other pages in a database from an automation, we currently cannot filter that set of pages that we’re going to edit by a relation to the page which triggered the automation. This is a **major** limitation in Notion automations, and it currently blocks many kinds of workflows – this one included.

However, if you absolutely need these recurring tasks and sub-tasks to work together, there is a way to do it.

[In this video, I explain that key limitation in more detail and provide a workaround.](https://www.loom.com/share/128cc9d40051481297415199ccd6650b)

## FAQs

This section contains additional reference information about recurring tasks in Notion, along with answers to commonly asked questions.


When you create a recurring event in a calendar app, like Google Calendar, you see that event **each time** it’s set to occur.

Naturally, you’d expect to see the same thing in **Calendar views** within Notion when you create a recurring task. But instead, you only see the recurring task on the next day it’s due. What gives?

This comes down to a **fundamental difference** between how calendar apps work compared to other apps, like Notion, that merely offer a _calendar view_ in addition to other types of views (e.g. Table, Board).

In both types of apps, each event/task is a **row** in the app’s backend **database.** That database row stores information about the event/task’s **date.**

But here’s where calendar apps go a step further. Calendar apps, like Google Calendar, Apple Calendar, and Outlook Calendar, implement something called [RRULES](https://datatracker.ietf.org/doc/html/rfc2445#section-4.8.5.4) (or Recurrence Rules).

These essentially tell the calendar app’s UI how to show a **single database row** in multiple slots on the calendar. Here’s an [easier-to-read blog post on how RRULES work](https://www.nylas.com/blog/calendar-events-rrules/) if you’re curious.

The upshot is that **Notion can’t show recurring tasks on multiple days** because Notion doesn’t support RRULES. Most task management apps don’t.

Additionally, even [Notion Calendar](https://thomasjfrank.com/notion-calendar) won’t show your recurring tasks from Notion across multiple days. This is because the data is still coming from Notion itself, meaning it lacks RRULES.

However, Notion Calendar _will_ show recurring events from your Google Calendar account across multiple days, because those are actual calendar events with full RRULE support.

**So here’s my recommendation:**

-   If it’s a recurring **event**, and you want to see it on the calendar across every occurrence, **create it in Google Calendar.**
-   If it’s a recurring **task,** and all that matters is that you know when it’s due next, **create it in Notion.**

Luckily, you can use Notion Calendar to view both types of entries in one place!


Not every kind of recurrence schedule is possible using our recurring tasks setup. While we’ve invested literally hundreds of hours in solving this problem in Notion (seriously, _hundreds_ of hours), there are still certain intervals that are currently too complex to achieve.

These include (but are not limited to):

-   “Every other Tuesday and Thursday” – multiple days of the week + custom week interval is not possible
-   “2nd Tuesday of every 4th Month” – Nth week of the month + custom month interval is not possible
-   “Every 3 Wednesdays” – specific day of the week, counting multiple weeks. Not even Todoist can do this without issues, due to ambiguity around how many weeks to count if the current task’s due date is shifted off-schedule. Supporting this would require a separate “original due date” property. In general, only calendar apps support this kind of schedule.
