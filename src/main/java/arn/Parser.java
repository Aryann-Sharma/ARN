package arn;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and interprets user commands,
 * then updates the task list and UI accordingly.
 */
public final class Parser {
    private static final Pattern BY_CLAUSE = Pattern.compile("(?<!\\S)/by(?=\\s|$)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern FROM_CLAUSE = Pattern.compile("(?<!\\S)/from(?=\\s|$)", Pattern.UNICODE_CHARACTER_CLASS);
    private static final Pattern TO_CLAUSE = Pattern.compile("(?<!\\S)/to(?=\\s|$)", Pattern.UNICODE_CHARACTER_CLASS);

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
        if (input.contains("\n") || input.contains("\r")) {
            throw new ArnException("Enter one command on a single line.");
        }

        String command = input.strip();
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
        String description = input.substring("todo".length()).strip();
        taskList.add(new Todo(description));
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void deadline(String input) throws ArnException {
        int j = findClause(input, BY_CLAUSE, "deadline".length());
        if (j == -1) {
            throw new ArnException("Deadline task must have a '/by' clause.");
        }
        String description = input.substring("deadline".length(), j).strip();
        if (description.isEmpty()) {
            throw new ArnException("Empty task description.");
        }
        String date = input.substring(j + 3).strip();
        if (date.isEmpty()) {
            throw new ArnException("Empty date.");
        }
        Deadline deadline = new Deadline(description, date);
        taskList.add(deadline);
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void event(String input) throws ArnException {
        int j = findClause(input, FROM_CLAUSE, "event".length());
        int k = j == -1 ? -1 : findClause(input, TO_CLAUSE, j + "/from".length());
        if (j == -1 || k == -1) {
            throw new ArnException("Event task must have both '/from' and '/to' clauses.");
        }
        String description = input.substring("event".length(), j).strip();
        if (description.isEmpty()) {
            throw new ArnException("Empty task description.");
        }
        String startDate = input.substring(j + 5, k).strip();
        String endDate = input.substring(k + 3).strip();
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
        String keyword = input.substring(4).strip();
        if (keyword.isEmpty()) {
            throw new ArnException("Empty keyword.");
        }
        List<Task> matchList = taskList.find(keyword);
        if (matchList.isEmpty()) {
            ui.displayMsg("Sorry, no matching tasks found.");
        } else {
            ui.displayMsg("Here are the matching tasks in your list:");
            List<Task> savedTasks = taskList.getTasks();
            for (Task task : matchList) {
                ui.displayMsg((savedTasks.indexOf(task) + 1) + ". " + task);
            }
        }
    }

    private void sortByDate() {
        List<Task> sortList = taskList.sortByDate();
        if (sortList.isEmpty()) {
            ui.displayMsg("No deadlines or events in the list.");
        } else {
            List<Task> savedTasks = taskList.getTasks();
            for (Task task : sortList) {
                ui.displayMsg((savedTasks.indexOf(task) + 1) + ". " + task);
            }
        }
    }

    private boolean isCommand(String input, String command) {
        return input.equals(command) || (input.startsWith(command)
                && input.length() > command.length()
                && Character.isWhitespace(input.charAt(command.length())));
    }

    private int findClause(String input, Pattern clause, int start) {
        Matcher matcher = clause.matcher(input);
        return matcher.find(start) ? matcher.start() : -1;
    }

    private int parseTaskIndex(String input, String command) throws ArnException {
        String taskNumber = input.substring(command.length()).strip();
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
