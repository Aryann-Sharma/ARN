package arn;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses and interprets user commands,
 * then updates the task list and UI accordingly.
 */
public final class Parser {
    private static final List<String> COMMANDS = List.of(
            "todo", "deadline", "event", "list", "mark", "unmark", "delete", "edit", "reschedule",
            "undo", "find", "sort", "bye");
    private static final String DEADLINE_EXAMPLE = "deadline Submit report /by 2026-10-02 1800";
    private static final String EVENT_EXAMPLE = "event Meeting /from 2026-10-02 1400 /to 2026-10-02 1500";
    private static final String RESCHEDULE_DEADLINE_EXAMPLE = "reschedule 1 /by 2026-10-05 1800";
    private static final String RESCHEDULE_EVENT_EXAMPLE =
            "reschedule 1 /from 2026-10-05 1400 /to 2026-10-05 1500";
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
            throw new ArnException("Enter a command, for example 'list' or 'todo Read a chapter'.");
        }
        if (input.contains("\n") || input.contains("\r")) {
            throw new ArnException("Enter one command on a single line.");
        }

        String command = input.strip();
        String commandName = command.split("\\p{javaWhitespace}+", 2)[0];
        if (List.of("list", "sort", "bye", "undo").contains(commandName) && !command.equals(commandName)) {
            throw new ArnException("'" + commandName + "' does not take extra text. Enter just '"
                    + commandName + "'.");
        }
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
        } else if (isCommand(command, "edit")) {
            edit(command);
        } else if (isCommand(command, "reschedule")) {
            reschedule(command);
        } else if (command.equals("undo")) {
            ui.requestUndo();
        } else if (isCommand(command, "find")) {
            find(command);
        } else if (command.equals("sort")) {
            sortByDate();
        } else {
            String lowercaseName = commandName.toLowerCase(Locale.ROOT);
            if (COMMANDS.contains(lowercaseName)) {
                throw new ArnException("Command names are case-sensitive. Use '" + lowercaseName
                        + "' instead of '" + commandName + "'.");
            }
            throw new ArnException("Unknown command '" + commandName + "'. Available commands: "
                    + String.join(", ", COMMANDS) + ".");
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
        int i = parseTaskIndex(input, "mark");
        taskList.get(i).markAsDone();
        ui.displayMsg("Task marked as done:");
        ui.displayMsg(taskList.get(i).toString());
    }

    private void unmark(String input) throws ArnException {
        int i = parseTaskIndex(input, "unmark");
        taskList.get(i).markAsNotDone();
        ui.displayMsg("Task marked as not done:");
        ui.displayMsg(taskList.get(i).toString());
    }

    private void todo(String input) throws ArnException {
        if (input.equals("todo")) {
            throw new ArnException("Add a description after 'todo'. Example: todo Read a chapter");
        }
        String description = input.substring("todo".length()).strip();
        taskList.add(new Todo(description));
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void deadline(String input) throws ArnException {
        int j = requireClause(input, BY_CLAUSE, "/by", "deadline".length(), DEADLINE_EXAMPLE);
        rejectRepeatedClause(input, BY_CLAUSE, "/by", j, DEADLINE_EXAMPLE);
        String description = input.substring("deadline".length(), j).strip();
        if (description.isEmpty()) {
            throw new ArnException("Add a deadline description before '/by'. Example: " + DEADLINE_EXAMPLE);
        }
        String date = input.substring(j + 3).strip();
        if (date.isEmpty()) {
            throw new ArnException("Add a due date after '/by'. Example: " + DEADLINE_EXAMPLE);
        }
        Deadline deadline = new Deadline(description, date);
        taskList.add(deadline);
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void event(String input) throws ArnException {
        int j = requireClause(input, FROM_CLAUSE, "/from", "event".length(), EVENT_EXAMPLE);
        int k = findClause(input, TO_CLAUSE, j + "/from".length());
        if (k == -1) {
            int earlierEnd = findClause(input, TO_CLAUSE, "event".length());
            if (earlierEnd != -1 && earlierEnd < j) {
                throw new ArnException("Put '/from' before '/to' so the start date comes first. Example: "
                        + EVENT_EXAMPLE);
            }
            k = requireClause(input, TO_CLAUSE, "/to", j + "/from".length(), EVENT_EXAMPLE);
        }
        rejectRepeatedClause(input, FROM_CLAUSE, "/from", j, EVENT_EXAMPLE);
        rejectRepeatedClause(input, TO_CLAUSE, "/to", k, EVENT_EXAMPLE);
        String description = input.substring("event".length(), j).strip();
        if (description.isEmpty()) {
            throw new ArnException("Add an event description before '/from'. Example: " + EVENT_EXAMPLE);
        }
        String startDate = input.substring(j + 5, k).strip();
        String endDate = input.substring(k + 3).strip();
        if (startDate.isEmpty()) {
            throw new ArnException("Add a start date after '/from' and before '/to'. Example: " + EVENT_EXAMPLE);
        }
        if (endDate.isEmpty()) {
            throw new ArnException("Add an end date after '/to'. Example: " + EVENT_EXAMPLE);
        }
        Event event = new Event(description, startDate, endDate);
        taskList.add(event);
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    private void delete(String input) throws ArnException {
        int i = parseTaskIndex(input, "delete");
        Task t = taskList.remove(i);
        ui.displayMsg("Task removed: " + t.toString());
    }

    private void edit(String input) throws ArnException {
        String[] arguments = input.substring("edit".length()).strip().split("\\p{javaWhitespace}+", 2);
        int index = parseTaskIndex("edit " + arguments[0], "edit");
        Task task = taskList.get(index);
        if (arguments.length < 2 || arguments[1].isBlank()) {
            throw new ArnException("Add a new description after the task number. Example: edit 1 Read chapter two");
        }
        String description = arguments[1].strip();
        if (description.equals(task.getDescription())) {
            ui.displayMsg("The description is unchanged.");
            return;
        }

        Task replacement;
        if (task instanceof Deadline deadline) {
            replacement = new Deadline(description, deadline.formatDate(false));
        } else if (task instanceof Event event) {
            replacement = new Event(description, event.formatStartDate(false), event.formatEndDate(false));
        } else {
            replacement = new Todo(description);
        }
        taskList.replace(index, replacement);
        ui.displayMsg("Task updated:\n" + (index + 1) + ". " + replacement);
    }

    private void reschedule(String input) throws ArnException {
        String[] arguments = input.substring("reschedule".length()).strip().split("\\p{javaWhitespace}+", 2);
        int index = parseTaskIndex("reschedule " + arguments[0], "reschedule");
        Task task = taskList.get(index);
        String dates = arguments.length == 2 ? arguments[1].strip() : "";
        Task replacement;
        if (task instanceof Deadline deadline) {
            int by = requireClause(dates, BY_CLAUSE, "/by", 0, RESCHEDULE_DEADLINE_EXAMPLE);
            if (by != 0) {
                throw new ArnException("Put '/by' immediately after the task number. Example: "
                        + RESCHEDULE_DEADLINE_EXAMPLE);
            }
            rejectRepeatedClause(dates, BY_CLAUSE, "/by", by, RESCHEDULE_DEADLINE_EXAMPLE);
            Deadline updated = new Deadline(task.getDescription(), dates.substring(3).strip());
            if (updated.formatDate(false).equals(deadline.formatDate(false))) {
                ui.displayMsg("The due date is unchanged.");
                return;
            }
            replacement = updated;
        } else if (task instanceof Event event) {
            Event updated = rescheduleEvent(event, dates);
            if (updated.formatStartDate(false).equals(event.formatStartDate(false))
                    && updated.formatEndDate(false).equals(event.formatEndDate(false))) {
                ui.displayMsg("The event dates are unchanged:\n" + (index + 1) + ". " + event);
                return;
            }
            replacement = updated;
        } else {
            throw new ArnException("Todos have no date to reschedule. Choose a deadline or event from 'list'.");
        }
        taskList.replace(index, replacement);
        ui.displayMsg("Task rescheduled:\n" + (index + 1) + ". " + replacement);
    }

    private Event rescheduleEvent(Event event, String dates) throws ArnException {
        int from = findClause(dates, FROM_CLAUSE, 0);
        int to = findClause(dates, TO_CLAUSE, 0);
        if (from != 0 && to != 0) {
            throw new ArnException("After the task number, use '/from' and a start date, '/to' and an end date, "
                    + "or both. Keep spaces around the markers. Example: reschedule 1 /from 2026-10-05");
        }
        if (from >= 0) {
            rejectRepeatedClause(dates, FROM_CLAUSE, "/from", from, RESCHEDULE_EVENT_EXAMPLE);
        }
        if (to >= 0) {
            rejectRepeatedClause(dates, TO_CLAUSE, "/to", to, RESCHEDULE_EVENT_EXAMPLE);
        }
        if (from >= 0 && to >= 0 && to < from) {
            throw new ArnException("When changing both dates, put '/from' before '/to'. Example: "
                    + RESCHEDULE_EVENT_EXAMPLE);
        }
        String start = from < 0 ? null : dates.substring(from + 5, to < 0 ? dates.length() : to).strip();
        String end = to < 0 ? null : dates.substring(to + 3).strip();
        if (start != null && start.isEmpty()) {
            throw new ArnException("Add a start date after '/from', or omit '/from' to keep the current start. "
                    + "Example: reschedule 1 /from 2026-10-05");
        }
        if (end != null && end.isEmpty()) {
            throw new ArnException("Add an end date after '/to', or omit '/to' to keep the current end. "
                    + "Example: reschedule 1 /to 2026-10-06");
        }
        try {
            return event.reschedule(start, end);
        } catch (ArnException e) {
            throw new ArnException(e.getMessage() + " Omitted endpoints stay unchanged, and date-only updates "
                    + "keep existing times. To change both endpoints, supply both '/from' and '/to'.");
        }
    }

    private void find(String input) throws ArnException {
        String keyword = input.substring(4).strip();
        if (keyword.isEmpty()) {
            throw new ArnException("Add text to search for after 'find'. Example: find report");
        }
        List<Task> matchList = taskList.find(keyword);
        if (matchList.isEmpty()) {
            ui.displayMsg("No task descriptions contain '" + keyword + "'. Try another search or use 'list'.");
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

    private int requireClause(String input, Pattern clause, String marker, int start, String example)
            throws ArnException {
        int index = findClause(input, clause, start);
        if (index == -1) {
            throw new ArnException("Use '" + marker + "' as a separate marker, with a space before it"
                    + " and before the date. Example: " + example);
        }
        return index;
    }

    private void rejectRepeatedClause(String input, Pattern clause, String marker, int index, String example)
            throws ArnException {
        if (findClause(input, clause, index + marker.length()) != -1) {
            throw new ArnException("Use '" + marker + "' only once. Example: " + example);
        }
    }

    private int parseTaskIndex(String input, String command) throws ArnException {
        String taskNumber = input.substring(command.length()).strip();
        if (taskNumber.isEmpty()) {
            throw new ArnException("Add a task number after '" + command + "'. Example: " + command
                    + " 1. Use 'list' to see task numbers.");
        }
        if (!taskNumber.matches("\\d+")) {
            throw invalidTaskNumber(command);
        }

        try {
            int oneBasedIndex = Integer.parseInt(taskNumber);
            if (oneBasedIndex < 1) {
                throw invalidTaskNumber(command);
            }
            return oneBasedIndex - 1;
        } catch (NumberFormatException e) {
            throw new ArnException("That task number is too large. Use 'list' to see the available task numbers.");
        }
    }

    private ArnException invalidTaskNumber(String command) {
        return new ArnException("Enter one whole task number after '" + command
                + "', starting at 1. Example: " + command + " 1. Use 'list' to see task numbers.");
    }
}
