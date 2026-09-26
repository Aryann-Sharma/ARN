package arn;

import java.util.List;

/**
 * Parses and interprets user commands,
 * then updates the task list and UI accordingly.
 */
public final class Parser {
    private final TaskList taskList;
    private final Ui ui;

    public Parser(TaskList taskList, Ui ui) {
        if (taskList == null || ui == null) {
            throw new IllegalArgumentException("Task list and UI are required.");
        }
        this.taskList = taskList;
        this.ui = ui;
    }

    public void parse(String input) throws ArnException {
        if (input == null || input.isBlank()) {
            throw new ArnException("Enter a command to continue.");
        }

        String command = input.trim();
        if (command.equals("bye")) {
            ui.displayBye();
        } else if (command.equals("list")) {
            list();
        } else if (isCommand(command, "mark")) {
            mark(command);
        } else if (isCommand(command, "unmark")) {
            unmark(command);
        } else if (isCommand(command, "todo")) {
            todo(command);
        } else if (isCommand(command, "deadline")) {
            deadline(command);
        } else if (isCommand(command, "event")) {
            event(command);
        } else if (isCommand(command, "delete")) {
            delete(command);
        } else if (isCommand(command, "find")) {
            find(command);
        } else if (command.equals("sort")) {
            sortByDate();
        } else {
            throw new ArnException("Sorry, not a valid command.");
        }
    }

    private void list() {
        if (taskList.size() == 0) {
            ui.displayMsg("Your task list is empty. Add a todo, deadline, or event to get started.");
            return;
        }
        int index = 1;
        for (Task task : taskList.getTasks()) {
            ui.displayMsg(index + ". " + task);
            index++;
        }
    }

    private void mark(String input) throws ArnException {
        if (input.equals("mark")) {
            throw new ArnException("No task number provided to mark.");
        }
        int i = parseTaskIndex(input, "mark");
        taskList.get(i).markAsDone();
        ui.displayMsg("Task marked as done:");
        ui.displayMsg(taskList.get(i).toString());
    }

    private void unmark(String input) throws ArnException {
        if (input.equals("unmark")) {
            throw new ArnException("No task number provided to unmark.");
        }
        int i = parseTaskIndex(input, "unmark");
        taskList.get(i).markAsNotDone();
        ui.displayMsg("Task marked as not done:");
        ui.displayMsg(taskList.get(i).toString());
    }

    private void todo(String input) throws ArnException {
        if (input.equals("todo")) {
            throw new ArnException("Empty task description.");
        }
        String description = input.substring("todo".length()).trim();
        taskList.add(new Todo(description));
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void deadline(String input) throws ArnException {
        int j = input.indexOf("/by");
        if (j == -1) {
            throw new ArnException("Deadline task must have a '/by' clause.");
        }
        String description = input.substring("deadline".length(), j).trim();
        if (description.isEmpty()) {
            throw new ArnException("Empty task description.");
        }
        String date = input.substring(j + 3).trim();
        if (date.isEmpty()) {
            throw new ArnException("Empty date.");
        }
        Deadline deadline = new Deadline(description, date);
        taskList.add(deadline);
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void event(String input) throws ArnException {
        int j = input.indexOf("/from");
        int k = j == -1 ? -1 : input.indexOf("/to", j + "/from".length());
        if (j == -1 || k == -1) {
            throw new ArnException("Event task must have both '/from' and '/to' clauses.");
        }
        String description = input.substring("event".length(), j).trim();
        if (description.isEmpty()) {
            throw new ArnException("Empty task description.");
        }
        String startDate = input.substring(j + 5, k).trim();
        String endDate = input.substring(k + 3).trim();
        if (startDate.isEmpty()) {
            throw new ArnException("Empty start date.");
        }
        if (endDate.isEmpty()) {
            throw new ArnException("Empty end date.");
        }
        Event event = new Event(description, startDate, endDate);
        taskList.add(event);
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void delete(String input) throws ArnException {
        if (input.equals("delete")) {
            throw new ArnException("No task number provided to delete.");
        }
        int i = parseTaskIndex(input, "delete");
        Task t = taskList.remove(i);
        ui.displayMsg("Task removed: " + t.toString());
    }

    private void find(String input) throws ArnException {
        String keyword = input.substring(4).trim();
        if (keyword.isEmpty()) {
            throw new ArnException("Empty keyword.");
        }
        List<Task> matchList = taskList.find(keyword);
        if (matchList.isEmpty()) {
            ui.displayMsg("Sorry, no matching tasks found.");
        } else {
            ui.displayMsg("Here are the matching tasks in your list:");
            int index = 1;
            for (Task task : matchList) {
                ui.displayMsg(index + ". " + task);
                index++;
            }
        }
    }

    private void sortByDate() {
        List<Task> sortList = taskList.sortByDate();
        if (sortList.isEmpty()) {
            ui.displayMsg("No deadlines or events in the list.");
        } else {
            int index = 1;
            for (Task task : sortList) {
                ui.displayMsg(index + ". " + task);
                index++;
            }
        }
    }

    private boolean isCommand(String input, String command) {
        return input.equals(command) || input.startsWith(command + " ");
    }

    private int parseTaskIndex(String input, String command) throws ArnException {
        String taskNumber = input.substring(command.length()).trim();
        if (!taskNumber.matches("\\d+")) {
            throw new ArnException("Task number must be a positive integer.");
        }

        try {
            int oneBasedIndex = Integer.parseInt(taskNumber);
            if (oneBasedIndex < 1) {
                throw new ArnException("Task number must be a positive integer.");
            }
            return oneBasedIndex - 1;
        } catch (NumberFormatException e) {
            throw new ArnException("Task number is too large.");
        }
    }
}
