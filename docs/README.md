# Arn User Guide


![Arn logo](../src/main/resources/images/ArnLogo.png)


Arn is a chatbot that takes care of your scheduling needs, whether that be your friend's birthday party, or your exam date! 

## Using the interface

Type a command in the input area and press **Enter** or select **Send**. The refreshed interface also includes:

- **Show tasks** to run `list`
- **By date** to run `sort`
- **Examples** to display command suggestions
- A live count of locally saved tasks
- Distinct visual feedback for successful commands and errors

Commands are case-sensitive and should be entered in lowercase. Dates use `yyyy-mm-dd` or `yyyy-mm-dd hhmm` in 24-hour time. For events, the start and end must both include a time or both omit it, and the end cannot be earlier than the start.

## List

list

Lists all tasks in their saved order. Task numbers shown here are the numbers used by `mark`, `unmark`, and `delete`.

Example: list


```text
1. [T][ ] gym
2. [D][ ] insurance (by Jun 3 2025)
```

## Adding Todo tasks

todo TASK

Adds a task of type ToDo to list (i.e. no associated dates)

Example: todo gym

```text
added: [T][ ] gym
```

## Adding Deadline tasks

deadline TASK /by DATE

Adds a task of type Deadline to list (i.e. task with a deadline).
DATE is in the format yyyy-mm-dd or yyyy-mm-dd hhmm. 

Example: deadline insurance /by 2025-06-03

```text
added: [D][ ] insurance (by Jun 3 2025)
```

Example: deadline assignment /by 2025-07-03 2359

```text
added: [D][ ] assignment (by Jul 3 2025, 11:59pm)
```

## Adding Event tasks

event TASK /from START_DATE /to END_DATE

Adds a task of type Event to list (i.e. task with start and end dates).
START_DATE and END_DATE use the format yyyy-mm-dd or yyyy-mm-dd hhmm. Both values must use the same format.

Example: event party /from 2025-05-02 /to 2025-05-03

```text
added: [E][ ] party (from May 2 2025 to May 3 2025)
```

Example: event meeting /from 2025-05-09 1600 /to 2025-05-09 1800

```text
added: [E][ ] meeting (from May 9 2025, 4:00pm to May 9 2025, 6:00pm)
```

## Marking tasks

mark TASK_INDEX

Marks a task in the list as "done". 
TASK_INDEX is index of a particular task in list in the range 1..n (where n is number of tasks in the list)

Example: mark 2

```text
Task marked as done:
[D][X] insurance (by Jun 3 2025)
```

## Unmarking tasks

unmark TASK_INDEX

Marks a task in the list as "not done". 
TASK_INDEX is index of a particular task in list in the range 1..n (where n is number of tasks in the list)

Example: unmark 2

```text
Task marked as not done:
[D][ ] insurance (by Jun 3 2025)
```

## Deleting tasks

delete TASK_INDEX

Deletes a task in the list.
TASK_INDEX is index of a particular task in list in the range 1..n (where n is number of tasks in the list)

Example: delete 2

```text
Task removed: [D][ ] insurance (by Jun 3 2025)
```

## Finding tasks

find TASK_DESCRIPTION

Outputs a list of tasks that match the given description

Example: find meeting

```text
Here are the matching tasks in your list:
1. [E][ ] meeting (from May 9 2025, 4:00pm to May 9 2025, 6:00pm)
```

## Sorting tasks by dates

sort

Displays deadlines and events chronologically. Event tasks are ordered by their start date. Todo tasks are excluded. This command does not change the saved order used by `list`.

Example: sort

```text
1. [E][ ] meeting (from May 9 2025, 4:00pm to May 9 2025, 6:00pm)
2. [D][ ] insurance (by Jun 3 2025)
```

## Saying goodbye

`bye`

Displays Arn's farewell message:

```text
Bye. Hope to see you again soon!
```

In the JavaFX application, close the window when you are finished.

## Task notation

- `[T]`, `[D]`, and `[E]` identify todos, deadlines, and events.
- `[ ]` means the task is incomplete.
- `[X]` means the task is complete.

## Saving data

Arn saves valid commands to `data/arn.txt`, relative to the directory from which the application is launched. The file is created automatically if it does not exist.



