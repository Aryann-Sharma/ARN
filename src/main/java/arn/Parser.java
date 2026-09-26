package arn;

import java.util.ArrayList;

/**
 * Parses and interprets user commands,
 * then updates the task list and UI accordingly.
 */
public class Parser {
    protected TaskList taskList;
    protected Ui ui;

    public Parser (TaskList taskList, Ui ui) {
        this.taskList = taskList;
        this.ui = ui;
    }

    public void parse(String input) throws ArnException {
        assert input != null : "input must not be null";
        if (input.equals("bye")) {
            ui.displayBye();
        } else if (input.equals("list")) {
            list();
        } else if (input.startsWith("mark ")) {
            mark(input);
        } else if (input.startsWith("unmark ")) {
            unMark(input);
        } else if (input.startsWith("todo ")) {
            todo(input);
        } else if (input.startsWith("deadline ")) {
            deadline(input);
        } else if (input.startsWith("event ")) {
            event(input);
        } else if (input.startsWith("delete ")) {
            delete(input);
        } else if (input.startsWith("find ")) {
            find(input);
        } else if (input.equals("sort")) {
            sortByDate();
        } else {
            throw new ArnException("Sorry, not a valid command.");
        }
    }

    public void list() {
        if (taskList.size() == 0) {
            ui.displayMsg("Your task list is empty. Add a todo, deadline, or event to get started.");
            return;
        }
        int index = 1;
        for (Task task : taskList.get()) {
            if (task == null) {
                break;
            }
            ui.displayMsg(index + ". " + task);
            index++;
        }
    }

    public void mark(String input) throws ArnException {
        if (input.trim().equals("mark")) {
            throw new ArnException("No task number provided to mark");
        }
        String[] parts = input.split(" ");
        int i = Integer.parseInt(parts[1]) - 1;
        try {
            taskList.get(i).markAsDone();
            ui.displayMsg("Task marked as done:");
            ui.displayMsg(taskList.get(i).toString());
        } catch (ArnException e) {
            throw e;
        }
    }

    public void unMark(String input) throws ArnException {
        if (input.trim().equals("unmark")) {
            throw new ArnException("No task number provided to mark");
        }
        String[] parts = input.split(" ");
        int i = Integer.parseInt(parts[1]) - 1;
        try {
            taskList.get(i).markAsNotDone();
            ui.displayMsg("Task marked as not done:");
            ui.displayMsg(taskList.get(i).toString());
        } catch (ArnException e) {
            throw e;
        }
    }

    public void todo(String input) throws ArnException {
        if (input.trim().equals("todo")) {
            throw new ArnException("Empty task description.");
        }
        int i = input.indexOf("todo ");
        String description = input.substring(i + 5);
        taskList.add(new Todo(description));
        ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
    }

    public void deadline(String input) throws ArnException {
        int i = input.indexOf("deadline ");
        int j = input.indexOf("/by");
        if (j == -1) {
            throw new ArnException("Deadline task must have a '/by' clause.");
        }
        String description = input.substring(i + 9, j).trim();
        if (description.isEmpty()) {
            throw new ArnException("Empty task description.");
        }
        String date = input.substring(j + 3).trim();
        if (date.isEmpty()) {
            throw new ArnException("Empty date.");
        }
        try {
            Deadline d = new Deadline(description, date);
            taskList.add(d);
            ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
        } catch (ArnException e) {
            throw e;
        }
    }

    public void event(String input) throws ArnException {
        int i = input.indexOf("event ");
        int j = input.indexOf("/from");
        int k = input.indexOf("/to");
        if (j == -1 || k == -1) {
            throw new ArnException("Event task must have both '/from' and '/to' clauses.");
        }
        String description = input.substring(i + 6, j).trim();
        if (description.isEmpty()) {
            throw new ArnException("Empty task description");
        }
        String startDate = input.substring(j + 5, k).trim();
        String endDate = input.substring(k + 3).trim();
        if (startDate.isEmpty()) {
            throw new ArnException("Empty start date.");
        }
        if (endDate.isEmpty()) {
            throw new ArnException("Empty end date.");
        }
        try {
            Event e = new Event(description, startDate, endDate);
            taskList.add(e);
            ui.displayMsg("added: " + taskList.get(taskList.size() - 1));
        } catch (ArnException e) {
            throw e;
        }
    }

    public void delete(String input) throws ArnException {
        if (input.trim().equals("delete")) {
            throw new ArnException("Task number to delete not provided");
        }
        String[] parts = input.split(" ");
        int i = Integer.parseInt(parts[1]) - 1;
        try {
            Task t = taskList.remove(i);
            ui.displayMsg("Task removed: " + t.toString());
        } catch (ArnException e) {
            throw e;
        }
    }

    public void find(String input) throws ArnException {
        String keyword = input.substring(4).trim();
        if (keyword.isEmpty()) {
            throw new ArnException("Empty keyword.");
        }
        ArrayList<Task> matchList = taskList.find(keyword);
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

    public void sortByDate() {
        ArrayList<Task> sortList = taskList.sortByDate();
        if (sortList.isEmpty()) {
            ui.displayMsg("No deadlines or events in the list");
        } else {
            int index = 1;
            for (Task task : sortList) {
                ui.displayMsg(index + ". " + task);
                index++;
            }
        }
    }
}
