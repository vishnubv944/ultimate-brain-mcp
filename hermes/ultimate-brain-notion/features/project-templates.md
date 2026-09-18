---
title: Project Templates with Pre-Defined Tasks
purpose: How to create project templates that automatically generate tasks — using automations (paid) or manual method (free)
agent_relevant: true
key_topics: [project-templates, pre-defined-tasks, automation, tasks, projects, template]
related_docs: [databases/projects.md, setup/database-templates.md, features/managing-tasks.md]
---
# How to Create a Project Template with Pre-Defined Tasks in Notion

The page provides two methods for creating project templates with predefined tasks in Ultimate Brain 3.0.

## Method 1: Database Automations (Recommended)

This approach requires a Notion paid plan. The process involves:

- Creating an automation triggered when a project is created from your template
- Adding actions that generate tasks automatically in your Tasks database
- Configuring task properties like due dates using relative date formulas
- Setting status and assignee details for each predefined task

The automation can use formulas such as `dateAdd(dateTrigger(), 30, "days")` to establish due dates relative to project creation.

## Method 2: Manual Template Method

For free plan users, this alternative involves:

- Duplicating an existing project template
- Creating standalone task pages within the template
- Dragging tasks outside the database view
- Rebuilding filters when applying the template to new projects

## Key Takeaway

The document notes that "the automation method is significantly more powerful and efficient" but requires a paid Notion account. Automations allow for truly hands-free task generation, while the manual approach requires setup work when creating each new project instance.
