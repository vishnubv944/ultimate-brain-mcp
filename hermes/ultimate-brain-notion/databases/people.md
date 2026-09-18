---
title: People Database — Full Schema Reference
purpose: Complete property reference for the People (CRM) database
agent_relevant: true
key_topics: [people, crm, contacts, database, schema, properties, relationship, pipeline, birthday, check-in]
related_docs: [features/overview.md]
---
# People

The **People** database stores all of your contact records. ([Here’s how to find it in your template](https://thomasjfrank.com/docs/ultimate-brain/unlocking-databases/#locating-a-source-database))

Below you’ll find a reference guide for all database templates and properties in the People database. Properties are in alphabetical order.

## Database Templates

These are the database templates that can be found in this database. Refer to our guide on [working with database templates](https://thomasjfrank.com/docs/ultimate-brain/working-with-database-templates/) if you’d like to edit them or create new ones.

### Person

This template add a set of useful database views to a Person’s page:

-   Meeting Notes – any notes with the Meeting type that are related to this Person
-   Notes – any notes that are related to this Person
-   Project Notes – any notes related to a Project that this Person is part of
-   Projects – any Projects to which this Person is related
-   Tasks – any Tasks to which this Person is related

Additionally, this template adds useful sections for jotting down quick notes and gift ideas.

## Properties

### Birthday

**Type:** Date

This person’s birthday.

Note: The Next Birthday property will show their upcoming birthday. You can use it in calendar views – however, I recommend using an actual calendar app as well, as it will allow you to create repeating reminder notifications.

### Check-In

**Type:** Date

If you’d like to schedule a check-in with this person, add a date here.

### Company

**Type:** Rich Text

The company this person works at.

Note: You can also use the People database to create entries for entire companies. This may be helpful if you’re working with a client company and have multiple contacts there.

### Created

**Type:** Created Time

The date on which this contact page was created.

### Edited

**Type:** Last Edited Time

The date on which this contact page was last updated.

### Email

**Type:** Email

This person’s email.

### Full Name

**Type:** Title

The full name of the person in first name, last name order.

### Instagram

**Type:** Url

This person’s Instagram profile link.

### Interests

**Type:** Multi Select

This person’s interests. Noting these here may be helpful for planning events or buying gifts in the future.

This property has no entries by default. Unlock the People database (UB Home → Databases at the bottom of the page → People) to edit this property.

### Last Check-In

**Type:** Date

Use this date property to record your most recent check-in or interaction with this person.

This property can be useful for making sure you’re regularly checking in with friends, family, or clients.

### LinkedIn

**Type:** Url

This person’s LinkedIn profile link.

### Location

**Type:** Rich Text

The location where this person lives or works. Typically, it’s most useful to put a city, state, and country here (for U.S. contacts), or the equivalent for other countries.

### Name (Last, First)

**Type:** Formula

This person’s name in Last, First order.

The formula in this property can handle many types of names – but in edge cases, you can add a name to the Surname property.

If you do, it will be used as the Last name in this formula.

CODE

`ifs( 	prop("Full Name").empty(), 	"", 	prop("Surname"), 	prop("Surname") + ", " + prop("Full Name").format().replace( 		prop("Surname").format() + "$", 		"" 	), 	lets( 		suffixes, 		["Jr.", "Sr.", "II", "III", "IV", "V", "MD", "PhD", "DDS", "Esq.", "OBE", "QC"], 		parts, 		prop("Full Name").split(" "), 		suffixFlag, 		if( 			suffixes.includes(parts.last()), 			true, 			false 		), 		if( 			parts.length() == 1, 			prop("Full Name"), 			if( 				suffixFlag, 				parts.at(parts.length() - 2) + ", " + parts.slice(0, parts.length() - 2).join(" ") + " " + parts.last(), 				parts.last() + ", " + parts.slice(0, parts.length() - 1).join(" ") 			) 		) 	) )`
Code language: JavaScript (javascript)

### Next Birthday

**Type:** Formula

This person’s next birthday. Calculated using the Birthday property, along with today’s date.

CODE

`let( 	birthday, 	parseDate(year(now()) + "-" + prop("Birthday").formatDate("MM-DD")), 	if( 		dateBetween(now(),birthday,"days") > 0, 		birthday.dateAdd(1,"years"), 		birthday 	) )`
Code language: JavaScript (javascript)

### Notes

**Type:** Relation

Any Notes related to this person’s page. This Relation property connected to the People relation property in the All Notes database.

### Phone

**Type:** Phone Number

This person’s phone number.

### Pipeline Status

**Type:** Status

The current status of a potential client or customer. This property is primarily useful for tracking sales pipelines for people that have a Relationship value of Client or Customer.

| Name | Description |
| --- | --- |
| Group: To-do |
| Prospect | This person is a prospect whom you haven’t contacted yet. |
| Group: In progress |
| Contacted | You’ve reached out to this person. |
| Negotiating | You’re currently negotiating a deal with this person. |
| Group: Complete |
| Rejected | This person rejected your pitch, and further negotiations aren’t happening right now. |
| Closed | You’ve successfully closed a deal with this person. |

### Projects

**Type:** Relation

Any projects associated with this person. This relation property connects to the People relation property in the Projects database.

### Relationship

**Type:** Multi Select

Your relationship(s) to this person.

| Name | Description |
| --- | --- |
| Family | N/A |
| Friend | N/A |
| Colleague | N/A |
| Client | N/A |
| Customer | N/A |
| Business Partner | N/A |
| Vendor | N/A |
| Senpai | N/A |
| Teacher | N/A |

### Surname

**Type:** Rich Text

This person’s surname, a.k.a. last name.

You only need to enter a surname here if the Name (Last, First) property isn’t deriving it correctly from the Full Name property.

“Jean-Claude Van Damme” is a good example; “Van Damme” is his surname.

### Tags

**Type:** Relation

Any Tags associated with this person. Best used to associate People with Tags that have the Area type when practicing PARA organization.

This Relation property connects to the People Relation property in the Tags database.

### Tasks

**Type:** Relation

Any tasks associated with this person. This can use used in the Delegation page of the Process (GTD) dashboard for noting who you’ve delegated tasks to.

This relation property connects to the People relation property in the All Tasks database.

### Title

**Type:** Rich Text

If this person has a job title, enter it here. E.g. “Founder” or “Lead Crocodile Wrangler”.

### Twitter/X

**Type:** Url

This person’s Twitter/X profile link.

### Website

**Type:** Url

This person’s website.
