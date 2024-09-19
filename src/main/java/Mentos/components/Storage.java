package Mentos.components;

import Mentos.MentosException.MentosException;
import Mentos.task.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.io.File;


public class Storage {
    protected final String FILE_PATH;

    public Storage(String FILE_PATH) {
        this.FILE_PATH = System.getProperty("user.dir") + FILE_PATH;
    }

    /**
     * Saves the list of tasks to a file specified by {@code FILE_PATH}.
     * This method writes each task in the {@code tasks} list to the file, with each task on a new line.
     * If the file specified by {@code FILE_PATH} does not exist, an error message will be printed to the console.
     * Note: The method uses a hard-coded file path. Ensure that the directory structure exists before calling this method.
     *
     * @throws IOException if an I/O error occurs while writing to the file.
     */

    public void saveTasksToFile(TaskList tasks) {
        File file = new File(FILE_PATH);
        try {
            // Ensure parent directories exist
            File parentDir = file.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            // Create file if it does not exist
            if (!file.exists()) {
                file.createNewFile();
            }

            // Write tasks to file
            try (FileWriter fileWriter = new FileWriter(file, false)) {
                for (Task task : tasks.getTasks()) {
                    fileWriter.write(task.toString() + "\n");
                }
            }
        } catch (IOException exception) {
            System.out.println("An error occurred while saving tasks: " + exception.getMessage());
        }
    }

    /**
     * Parses the input line to create a ToDo task.
     * The method uses a regex to match the format of a ToDo task and checks
     * whether the task is marked as done.
     *
     * @param line   The input line representing a ToDo task in the format: "[T] [ ] description".
     * @param isDone The status of the task, "X" if the task is done, otherwise empty.
     * @return A ToDo task object with the provided description, or null if the input format is invalid.
     */
    public Task toDoHandler(String line, String isDone) {
        Matcher toDoMatcher = regexHandler(line, "^\\[(?:[T])\\] \\[(?:.)\\] (.*)$");
        if (toDoMatcher == null) {
            return null;
        }
        String desc = toDoMatcher.group(1);
        Task toDoEvent = new ToDo(desc);
        if (isDone.equals("X")) {
            toDoEvent.markAsDone();
        }
        return toDoEvent;
    }

    /**
     * Parses the input line to create an Event task.
     * The method uses a regex to match the format of an Event task and checks
     * whether the task is marked as done.
     *
     * @param line   The input line representing an Event task in the format: "[E] [ ] description (from: start to: end)".
     * @param isDone The status of the task, "X" if the task is done, otherwise empty.
     * @return An Event task object with the provided description, start date, and end date, or null if the input format is invalid.
     */
    public Task eventHandler(String line, String isDone) throws MentosException {
        Matcher eventMatcher = regexHandler(line, "^\\[(?:[E])\\] \\[(?:.)\\] (.*) \\(from: (.*) to: (.*)\\)$");
        if (eventMatcher == null) {
            return null;
        }
        String desc = eventMatcher.group(1);
        String fromDate = eventMatcher.group(2);
        String toDate = eventMatcher.group(3);
        Task eventEvent = new Event(desc, changeFormat(fromDate), changeFormat(toDate));
        if (isDone.equals("X")) {
            eventEvent.markAsDone();
        }
        return eventEvent;
    }

    /**
     * Parses the input line to create a Deadline task.
     * The method uses a regex to match the format of a Deadline task and checks
     * whether the task is marked as done.
     *
     * @param line   The input line representing a Deadline task in the format: "[D] [ ] description (by: date)".
     * @param isDone The status of the task, "X" if the task is done, otherwise empty.
     * @return A Deadline task object with the provided description and due date, or null if the input format is invalid.
     */
    public Task deadlineHandler(String line, String isDone) {
        Matcher deadlineMatcher = regexHandler(line, "^\\[(?:[D])\\] \\[(?:.)\\] (.*) \\(by: (.*)\\)$");
        if (deadlineMatcher == null) {
            return null;
        }
        String desc = deadlineMatcher.group(1);
        String byDate = deadlineMatcher.group(2);
        Task deadlineEvent = new Deadline(desc, changeFormat(byDate));
        if (isDone.equals("X")) {
            deadlineEvent.markAsDone();
        }
        return deadlineEvent;
    }

    /**
     * Loads the list of tasks from a file specified by {@code FILE_PATH}.
     * This method reads each line from the file and attempts to parse it into a {@code Task} object,
     * depending on the task type identified in the line. The supported task types are:
     * To-Do (T)
     * Event (E)
     * Deadline (D)
     * If a line is not in the expected format, it will be skipped, and a warning message will be printed to the console.
     * If the file does not exist or an I/O error occurs, an error message will be printed instead.
     * <p>
     * Note: The method expects the file content to be in a specific format for parsing.
     *
     * @throws IOException if an I/O error occurs while reading the file.
     */
    public ArrayList<Task> loadTasksFromFile() throws MentosException {
        ArrayList<Task> tasks = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Task task = taskFileHandler(line);
                if (task == null) {
                    continue;
                }
                tasks.add(task);
            }
            return tasks;
        } catch (IOException e) {
            throw new MentosException("No Configuration found in " + FILE_PATH);
        }
    }

    /**
     * Handles the task file input by identifying the type of task (ToDo, Event, or Deadline)
     * and processing it accordingly. The method extracts the task type and completion status
     * from the input line using a regular expression and calls the appropriate handler method.
     *
     * @param line The input line representing a task in the format: "[T|E|D] [X| ] description".
     * @return A Task object (ToDo, Event, or Deadline) based on the input, or null if the format is invalid or unrecognized.
     */
    public Task taskFileHandler(String line) throws MentosException {
        Matcher matcher = regexHandler(line, "^\\[([T|E|D])\\] \\[(.)\\] (?:.*)$");
        if (matcher == null) {
            System.out.println("Content not in the right format! Skipping....");
            return null;
        }
        String eventType = matcher.group(1);
        String isDone = matcher.group(2);
        if (eventType.equals("T")) {
            return toDoHandler(line, isDone);
        } else if (eventType.equals("E")) {
            return eventHandler(line, isDone);
        } else if (eventType.equals("D")) {
            return deadlineHandler(line, isDone);
        }
        return null;
    }


    /**
     * Converts a date and time string from one format to another.
     * <p>
     * This method takes a date and time string in the format "MMM d yyyy, HH:mm",
     * parses it into a {@code LocalDateTime} object, and then formats it into
     * the desired output format "yyyy-MM-dd HHmm". The method returns the
     * reformatted date and time string.
     */

    public String changeFormat(String dateTimeStr) {
        DateTimeFormatter inputFormatter = DateTimeFormatter.ofPattern("MMM d yyyy, HH:mm");
        LocalDateTime dateTime = LocalDateTime.parse(dateTimeStr, inputFormatter);
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HHmm");
        return dateTime.format(outputFormatter);
    }

    /**
     * Handles regular expression matching on a given input string.
     * This method compiles the provided regular expression (regex) and
     * attempts to find a match within the input string. If a match is found,
     * it returns the `Matcher` object, allowing further operations like
     * extracting matched groups. If no match is found, it returns `null`.
     *
     * @param input the string to be matched against the regular expression.
     * @param regex the regular expression used for matching.
     * @return `Matcher` object if a match is found; `null` otherwise.
     */
    public Matcher regexHandler(String input, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);
        if (matcher.find()) {
            return matcher;
        }
        return null;
    }
}
