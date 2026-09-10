# App backlog

Running list of things to build / fix in the Android app. Work through them
one at a time; move finished items to **Done** with the commit hash.

## To do

### 1. Long-press quick-edit for tasks & notes
Long-pressing a task row or a note row (in any list) should open a quick
property editor — the way Todoist / TickTick do it — without navigating into
the full detail screen.

- Trigger: long-press on the row (`combinedClickable`), keep the normal tap =
  open detail.
- Surface: a modal bottom sheet with the item title at the top and a row of
  property chips (reuse `DetailKit` `SelectChip` / `DateChip` / `PropertyChip`).
- Tasks: Status, Due date, Project, Priority, My Day. Optionally quick actions
  (Complete, Archive, Open full details).
- Notes: Type, Date, Project, Favorite.
- Writes go through the existing optimistic VM setters
  (`setTaskProjectRelation`, `setTaskDueDate`, `updateTaskPriority`,
  `updateTaskStatus`, `toggleMyDay`, `setNoteType`, `setNoteDate`,
  `setNoteProjectRelation`, `toggleNoteFavorite`).
- Wire it into: Tasks list, Today (browse + shortlist rows), Project detail
  task list, Notes list. Prefer centralising the sheet (VM state +
  render once in `MainActivity`) so new list screens get it for free.

## Done

_(nothing yet)_
