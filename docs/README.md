# Arn User Guide


![Arn logo](../src/main/resources/images/ArnLogo.png)


Arn is a chatbot that takes care of your scheduling needs, whether that be your friend's birthday party, or your exam date! 

## Using the interface

Type a command in the input area and press **Enter** or select **Send**. The refreshed interface also includes:

- **Show tasks** to run `list`
- **Upcoming** to run `sort`
- **Examples** to display command suggestions
- A live count of locally saved tasks
- Distinct visual feedback for successful commands and errors

Dates use `yyyy-mm-dd` or `yyyy-mm-dd hhmm` in 24-hour time.

## List

list

Lists out all the tasks.

Example: list


```
Prints list of tasks
```

## Adding Todo tasks

todo TASK

Adds a task of type ToDo to list (i.e. no associated dates)

Example: todo gym

```
added: [T][] gym
```

## Adding Deadline tasks

deadline TASK /by DATE

Adds a task of type Deadline to list (i.e. task with a deadline).
DATE is in the format yyyy-mm-dd or yyyy-mm-dd hhmm. 

Example: deadline insurance /by 2025-06-03

```
added: [D][] insurance (by Jun 3 2025)
```

Example: deadline assignment /by 2025-07-03 2359

```
added: [D][] assignment (by Jul 3 2025, 11:59pm)
```

## Adding Event tasks

event TASK /from START_DATE /to END_DATE

Adds a task of type Event to list (i.e. task with start and end dates).
START_DATE and END_DATE is in the format yyyy-mm-dd or yyyy-mm-dd hhmm. 

Example: event party /from 2025-05-02 /to 2025-05-03

```
added: [E][] party (from May 2 2025 to May 3 2025)
```

Example: event meeting /from 2025-05-09 1600 /to 2025-05-09 1800

```
added: [E][] meeting (from May 9 2025, 4:00pm to May 9 2025, 6:00pm)
```

## Marking tasks 

mark TASK_INDEX

Marks a task in the list as "done". 
TASK_INDEX is index of a particular task in list in the range 1..n (where n is number of tasks in the list)

Example: mark 2

```
Task marked as done:
[D][X] insurance (by Jun 3 2025)
```

## Unmarking tasks

unmark TASK_INDEX

Marks a task in the list as "not done". 
TASK_INDEX is index of a particular task in list in the range 1..n (where n is number of tasks in the list)

Example: unmark 2

```
Task marked as not done:
[D][] insurance (by Jun 3 2025)
```

## Deleting tasks

delete TASK_INDEX

Deletes a task in the list.
TASK_INDEX is index of a particular task in list in the range 1..n (where n is number of tasks in the list)

Example: delete 2

```
Task removed: [D][] insurance (by Jun 3 2025)
```

## Finding tasks

find TASK_DESCRIPTION

Outputs a list of tasks that match the given description

Example: find meeting

```
Here are the matching tasks in your list:
1. [E][] meeting (from May 9 2025, 4:00pm to May 9 2025, 6:00pm)
```

## Sorting tasks by dates

sort

Chronologically sorts any tasks of type deadline or of type event by their dates and outputs that list.
Start date is used in the sorting for event tasks (not end date).
Todo tasks are excluded

Example: sort

```
Sorts the task list in chronological order of dates and outputs the list
```



