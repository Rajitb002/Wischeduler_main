package com.example.wischeduler;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A single-file implementation of a complex, smart scheduling algorithm.
 * This version includes robust error checking and fallback values for parsing times.
 */
public class smartScheduler {

    public String generateSchedule(String prefJson, String assignJson, String eventsJson, String socialJson) {
        try {
            Gson gson = new Gson();

            // Parse preferences
            List<Map<String,String>> prefQAs = parseQuestionsAndAnswers(prefJson);
            UserPreferences userPreferences = convertToUserPreferences(prefQAs);

            // Parse tasks (assignments/quizzes)
            List<Task> tasks = parseTasks(assignJson);

            // Parse calendar events
            List<CalendarEvent> calendarEvents = parseCalendarEvents(eventsJson);

            // Parse social events
            List<SocialEvent> socialEvents = parseSocialEvents(socialJson);

            // Compute effective durations
            for (Task task : tasks) {
                double eff = computeEffectiveDuration(task);
                task.setEffectiveDurationHours(eff);
            }

            // Sort tasks by deadline safely (some tasks may have fallback deadlines)
            tasks.sort(Comparator.comparing(Task::getDeadlineZDT));

            // Build schedule
            List<ScheduledBlock> finalSchedule = buildSchedule(userPreferences, tasks, calendarEvents, socialEvents);

            // Output JSON
            JsonObject outputRoot = new JsonObject();
            JsonArray scheduleArr = new JsonArray();
            Gson outputGson = new GsonBuilder().setPrettyPrinting().create();
            for (ScheduledBlock sb : finalSchedule) {
                JsonObject obj = new JsonObject();
                obj.addProperty("title", sb.title);
                obj.addProperty("type", sb.type);
                obj.addProperty("startTime", sb.startTime);
                obj.addProperty("endTime", sb.endTime);
                obj.addProperty("description", sb.description);
                scheduleArr.add(obj);
            }
            outputRoot.add("schedule", scheduleArr);

            return outputGson.toJson(outputRoot);

        } catch (Exception e) {
            e.printStackTrace();
            // On any error, return an empty schedule array
            return "[]";
        }
    }

    private List<Map<String,String>> parseQuestionsAndAnswers(String prefJson) {
        Gson gson = new Gson();
        JsonElement root = gson.fromJson(prefJson, JsonElement.class);
        JsonArray arr = null;

        if (root == null) {
            throw new IllegalArgumentException("Preferences JSON is null");
        }

        if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            arr = obj.getAsJsonArray("preferences");
            if (arr == null) {
                arr = obj.getAsJsonArray("questions_and_answers");
            }
        } else if (root.isJsonArray()) {
            arr = root.getAsJsonArray();
        }

        if (arr == null) {
            throw new IllegalArgumentException("No valid preferences found in JSON.");
        }

        List<Map<String,String>> result = new ArrayList<>();
        for (JsonElement el : arr) {
            if (el.isJsonObject()) {
                JsonObject prefObj = el.getAsJsonObject();
                String question = prefObj.has("question") ? prefObj.get("question").getAsString() : "";
                String answer = prefObj.has("answer") ? prefObj.get("answer").getAsString() : "";
                Map<String,String> map = new HashMap<>();
                map.put("question", question);
                map.put("answer", answer);
                result.add(map);
            }
        }

        return result;
    }

    private UserPreferences convertToUserPreferences(List<Map<String,String>> qaList) {
        Map<String,String> qaMap = new HashMap<>();
        for (Map<String,String> entry : qaList) {
            qaMap.put(entry.get("question"), entry.get("answer"));
        }

        return new UserPreferencesInternal(
                qaMap.getOrDefault("When do you prefer to study?", "Morning (6 AM - 12 PM)"),
                qaMap.getOrDefault("How long can you study in one sitting without losing focus?", "1-2 hours"),
                qaMap.getOrDefault("How much advance preparation do you need for exams or quizzes?", "1-2 weeks"),
                qaMap.getOrDefault("Do you prefer to complete assignments:", "Early, well before the deadline"),
                qaMap.getOrDefault("What is your preferred sleep time?", "11:00 PM"),
                qaMap.getOrDefault("What is your preferred wake-up time?", "6:30 AM"),
                qaMap.getOrDefault("How would you like to spend your weekends?", "A balance of both")
        );
    }

    private List<Task> parseTasks(String assignJson) {
        Gson gson = new Gson();
        JsonElement root = gson.fromJson(assignJson, JsonElement.class);
        List<Task> tasks = new ArrayList<>();

        if (root == null) return tasks;

        JsonArray arr = null;
        if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            arr = obj.getAsJsonArray("tasks");
            if (arr == null) {
                arr = obj.getAsJsonArray("calendarEvents");
            }
        } else if (root.isJsonArray()) {
            arr = root.getAsJsonArray();
        }

        if (arr != null) {
            for (JsonElement el : arr) {
                Task t = gson.fromJson(el, Task.class);
                tasks.add(t);
            }
        }

        return tasks;
    }

    private List<CalendarEvent> parseCalendarEvents(String eventsJson) {
        Gson gson = new Gson();
        JsonElement root = gson.fromJson(eventsJson, JsonElement.class);
        List<CalendarEvent> events = new ArrayList<>();

        if (root == null) return events;

        JsonArray arr = null;
        if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            arr = obj.getAsJsonArray("calendarEvents");
            if (arr == null) {
                arr = obj.getAsJsonArray("events");
            }
        } else if (root.isJsonArray()) {
            arr = root.getAsJsonArray();
        }

        if (arr != null) {
            for (JsonElement el : arr) {
                CalendarEvent ce = gson.fromJson(el, CalendarEvent.class);
                events.add(ce);
            }
        }

        return events;
    }

    private List<SocialEvent> parseSocialEvents(String socialJson) {
        Gson gson = new Gson();
        JsonElement root = gson.fromJson(socialJson, JsonElement.class);
        List<SocialEvent> events = new ArrayList<>();

        if (root == null) return events;

        JsonArray arr = null;
        if (root.isJsonArray()) {
            arr = root.getAsJsonArray();
        } else if (root.isJsonObject()) {
            JsonObject obj = root.getAsJsonObject();
            arr = obj.getAsJsonArray("socialEvents");
            if (arr == null) {
                arr = obj.getAsJsonArray("events");
            }
        }

        if (arr != null) {
            for (JsonElement el : arr) {
                SocialEvent se = gson.fromJson(el, SocialEvent.class);
                events.add(se);
            }
        }

        return events;
    }

    private double computeEffectiveDuration(Task task) {
        double baseTime = "assignment".equalsIgnoreCase(task.getType()) ? 3.0 : 1.0;
        int lvl = task.getCourseLevel();
        double multiplier;
        if (lvl >= 400) multiplier = 2.0;
        else if (lvl >= 200) multiplier = 1.5;
        else multiplier = 1.0;
        return baseTime * multiplier;
    }

    private List<ScheduledBlock> buildSchedule(UserPreferences userPreferences, List<Task> tasks, List<CalendarEvent> calendarEvents, List<SocialEvent> socialEvents) {
        List<ScheduledBlock> finalSchedule = new ArrayList<>();

        TimeWindow studyWindow = getStudyWindow(userPreferences.getPreferredStudyTime());
        LocalTime wakeTime = parseTime(userPreferences.getPreferredWakeTime());
        LocalTime sleepTime = parseTime(userPreferences.getPreferredSleepTime());

        // Clamp study window by wake/sleep
        if (studyWindow.start.isBefore(wakeTime)) studyWindow.start = wakeTime;
        if (studyWindow.end.isAfter(sleepTime)) studyWindow.end = sleepTime;

        int focusMinutes = getFocusMinutes(userPreferences.getFocusDuration());

        List<BusyBlock> busyTimes = getBusyBlocks(calendarEvents, socialEvents);

        boolean preferLate = userPreferences.getAssignmentCompletionPreference().toLowerCase().contains("closer to the deadline");

        ZonedDateTime now = ZonedDateTime.now(ZoneId.systemDefault());
        ZonedDateTime end = now.plusDays(30);
        List<Task> unscheduled = new ArrayList<>(tasks);

        while (!unscheduled.isEmpty() && now.isBefore(end)) {
            LocalDate currentDate = now.toLocalDate();
            boolean isWeekend = (currentDate.getDayOfWeek() == DayOfWeek.SATURDAY || currentDate.getDayOfWeek() == DayOfWeek.SUNDAY);
            TimeWindow todayWindow = new TimeWindow(studyWindow.start, studyWindow.end);
            applyWeekendPreference(userPreferences.getWeekendPreference(), isWeekend, todayWindow);

            List<FreeBlock> freeBlocks = computeFreeBlocks(currentDate, todayWindow, busyTimes);

            List<Task> scheduledToday = new ArrayList<>();

            for (Task task : unscheduled) {
                if (finalSchedule.stream().anyMatch(sb -> sb.title.equals(task.getTitle()))) continue;

                ZonedDateTime deadline = task.getDeadlineZDT();
                double neededHours = task.getEffectiveDurationHours();
                int neededMinutes = (int)(neededHours * 60);
                List<Integer> sessions = breakIntoSessions(neededMinutes, focusMinutes);

                long daysUntilDeadline = Duration.between(now, deadline).toDays();
                if (preferLate && daysUntilDeadline > 3) {
                    // Wait until closer to the deadline
                    continue;
                }

                List<ScheduledBlock> placed = placeTaskSessions(task, sessions, currentDate, freeBlocks);
                if (placed != null) {
                    finalSchedule.addAll(placed);
                    scheduledToday.add(task);
                    freeBlocks = recomputeFreeBlocks(freeBlocks, placed);
                }
            }

            unscheduled.removeAll(scheduledToday);
            now = now.plusDays(1);
        }

        return finalSchedule;
    }

    private void applyWeekendPreference(String weekendPreference, boolean isWeekend, TimeWindow todayWindow) {
        if (!isWeekend) return;
        weekendPreference = weekendPreference.toLowerCase();
        if (weekendPreference.contains("relaxation")) {
            // Minimal work on weekends
            LocalTime shortenedEnd = todayWindow.start.plusHours(1);
            if (shortenedEnd.isBefore(todayWindow.end)) {
                todayWindow.end = shortenedEnd;
            }
        } else if (weekendPreference.contains("balance")) {
            // Half the available window
            long totalMin = Duration.between(todayWindow.start, todayWindow.end).toMinutes();
            LocalTime mid = todayWindow.start.plusMinutes(totalMin / 2);
            todayWindow.end = mid;
        }
    }

    private List<ScheduledBlock> placeTaskSessions(Task task, List<Integer> sessions, LocalDate date, List<FreeBlock> freeBlocks) {
        List<ScheduledBlock> result = new ArrayList<>();

        for (int sessionMinutes : sessions) {
            boolean placed = false;
            for (FreeBlock fb : freeBlocks) {
                int totalNeeded = sessionMinutes + 15; // session + break
                if (fb.durationMinutes() >= totalNeeded) {
                    ZonedDateTime start = ZonedDateTime.of(date, fb.start, ZoneId.systemDefault());
                    ZonedDateTime end = start.plusMinutes(sessionMinutes);
                    ScheduledBlock sb = new ScheduledBlock(
                            task.getTitle(),
                            task.getType(),
                            start,
                            end,
                            task.getDescription() != null ? task.getDescription() : ""
                    );
                    result.add(sb);
                    fb.start = end.toLocalTime().plusMinutes(15);
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                return null;
            }
        }
        return result;
    }

    private List<FreeBlock> recomputeFreeBlocks(List<FreeBlock> oldBlocks, List<ScheduledBlock> placed) {
        return oldBlocks.stream().filter(fb -> fb.durationMinutes() > 0).collect(Collectors.toList());
    }

    private List<Integer> breakIntoSessions(int totalMinutes, int focusMinutes) {
        List<Integer> sessions = new ArrayList<>();
        int remaining = totalMinutes;
        while (remaining > focusMinutes) {
            sessions.add(focusMinutes);
            remaining -= focusMinutes;
        }
        if (remaining > 0) sessions.add(remaining);
        return sessions;
    }

    private int getFocusMinutes(String focusPref) {
        focusPref = focusPref.toLowerCase();
        if (focusPref.contains("less than 30")) return 25;
        if (focusPref.contains("30-60")) return 45;
        if (focusPref.contains("1-2 hours")) return 90;
        return 150; // more than 2 hours
    }

    private TimeWindow getStudyWindow(String pref) {
        pref = pref.toLowerCase();
        if (pref.contains("morning")) return new TimeWindow(LocalTime.of(6,0), LocalTime.of(12,0));
        if (pref.contains("afternoon")) return new TimeWindow(LocalTime.of(12,0), LocalTime.of(18,0));
        if (pref.contains("evening")) return new TimeWindow(LocalTime.of(18,0), LocalTime.of(23,59));
        if (pref.contains("late night")) return new TimeWindow(LocalTime.of(0,0), LocalTime.of(3,0));
        return new TimeWindow(LocalTime.of(6,0), LocalTime.of(12,0));
    }

    private LocalTime parseTime(String timeStr) {
        if (timeStr == null || timeStr.trim().isEmpty()) {
            // Default if not specified
            return LocalTime.of(6,0);
        }
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
        return LocalTime.parse(timeStr.trim(), fmt);
    }

    private List<BusyBlock> getBusyBlocks(List<CalendarEvent> calEvents, List<SocialEvent> socEvents) {
        List<BusyBlock> blocks = new ArrayList<>();
        for (CalendarEvent ce : calEvents) {
            ZonedDateTime st = parseZonedDateTime(ce.getStartTime());
            ZonedDateTime et = parseZonedDateTime(ce.getEndTime());
            if (st != null && et != null && !et.isBefore(st)) {
                blocks.add(new BusyBlock(st, et));
            }
        }
        for (SocialEvent se : socEvents) {
            LocalDate d = se.getDateLocal();
            LocalTime st = se.getStartLocalTime();
            LocalTime et = se.getEndLocalTime();
            if (d != null && st != null && et != null && !et.isBefore(st)) {
                ZonedDateTime startZ = ZonedDateTime.of(d, st, ZoneId.systemDefault());
                ZonedDateTime endZ = ZonedDateTime.of(d, et, ZoneId.systemDefault());
                blocks.add(new BusyBlock(startZ,endZ));
            }
        }
        return blocks;
    }

    private List<FreeBlock> computeFreeBlocks(LocalDate date, TimeWindow window, List<BusyBlock> busyTimes) {
        ZonedDateTime dayStart = ZonedDateTime.of(date, window.start, ZoneId.systemDefault());
        ZonedDateTime dayEnd = ZonedDateTime.of(date, window.end, ZoneId.systemDefault());

        List<BusyBlock> dayBusy = busyTimes.stream()
                .filter(b -> !b.end.isBefore(dayStart) && !b.start.isAfter(dayEnd))
                .sorted(Comparator.comparing(b -> b.start))
                .collect(Collectors.toList());

        // Merge overlapping
        List<BusyBlock> merged = new ArrayList<>();
        for (BusyBlock b : dayBusy) {
            if (merged.isEmpty()) merged.add(b);
            else {
                BusyBlock last = merged.get(merged.size() - 1);
                if (!b.start.isAfter(last.end)) {
                    if (b.end.isAfter(last.end)) {
                        last.end = b.end;
                    }
                } else {
                    merged.add(b);
                }
            }
        }

        List<FreeBlock> freeBlocks = new ArrayList<>();
        ZonedDateTime current = dayStart;
        for (BusyBlock b : merged) {
            if (b.start.isAfter(current)) {
                freeBlocks.add(new FreeBlock(current.toLocalTime(), b.start.toLocalTime()));
            }
            current = b.end;
        }
        if (current.isBefore(dayEnd)) {
            freeBlocks.add(new FreeBlock(current.toLocalTime(), dayEnd.toLocalTime()));
        }

        return freeBlocks;
    }

    private ZonedDateTime parseZonedDateTime(String zdtStr) {
        if (zdtStr == null || zdtStr.trim().isEmpty()) {
            return null;
        }
        try {
            return ZonedDateTime.parse(zdtStr.trim());
        } catch (Exception e) {
            // If parsing fails, return null
            return null;
        }
    }

    // Internal classes

    private class UserPreferences {
        public String getPreferredStudyTime() {return null;}
        public String getFocusDuration(){return null;}
        public String getPrepTimeForExams(){return null;}
        public String getAssignmentCompletionPreference(){return null;}
        public String getPreferredSleepTime(){return null;}
        public String getPreferredWakeTime(){return null;}
        public String getWeekendPreference(){return null;}
    }

    private class UserPreferencesInternal extends UserPreferences {
        private String preferredStudyTime;
        private String focusDuration;
        private String prepTimeForExams;
        private String assignmentCompletionPreference;
        private String preferredSleepTime;
        private String preferredWakeTime;
        private String weekendPreference;

        public UserPreferencesInternal(String pst, String fd, String pte, String acp, String pstime, String pwtime, String wendPref) {
            this.preferredStudyTime = pst;
            this.focusDuration = fd;
            this.prepTimeForExams = pte;
            this.assignmentCompletionPreference = acp;
            this.preferredSleepTime = pstime;
            this.preferredWakeTime = pwtime;
            this.weekendPreference = wendPref;
        }

        @Override public String getPreferredStudyTime() { return preferredStudyTime; }
        @Override public String getFocusDuration() { return focusDuration; }
        @Override public String getPrepTimeForExams() { return prepTimeForExams; }
        @Override public String getAssignmentCompletionPreference() { return assignmentCompletionPreference; }
        @Override public String getPreferredSleepTime() { return preferredSleepTime; }
        @Override public String getPreferredWakeTime() { return preferredWakeTime; }
        @Override public String getWeekendPreference() { return weekendPreference; }
    }

    private class Task {
        private String id;
        private String title;
        private String type; // "Assignment" or "Quiz"
        private String deadline;
        private int courseLevel;
        private String description;
        private String url;

        // Some tasks do not have 'deadline', but have 'startTime'/'endTime' instead.
        // We'll store them as well:
        private String startTime;
        private String endTime;

        private double effectiveDurationHours;

        public String getTitle() {return title;}
        public String getType() {return type;}
        public int getCourseLevel() {return courseLevel;}
        public String getDescription(){return description;}

        public double getEffectiveDurationHours(){return effectiveDurationHours;}
        public void setEffectiveDurationHours(double h){this.effectiveDurationHours = h;}

        public ZonedDateTime getDeadlineZDT() {
            ZonedDateTime dt = null;
            // Prefer 'deadline' if provided
            if (deadline != null && !deadline.trim().isEmpty()) {
                dt = parseZDT(deadline);
            }
            // If no deadline, try 'endTime'
            if (dt == null && endTime != null && !endTime.trim().isEmpty()) {
                dt = parseZDT(endTime);
            }
            // If still null, try 'startTime'
            if (dt == null && startTime != null && !startTime.trim().isEmpty()) {
                dt = parseZDT(startTime);
            }
            // If all fail, use a fallback far in the future
            if (dt == null) {
                dt = ZonedDateTime.now(ZoneId.systemDefault()).plusDays(30);
            }
            return dt;
        }

        private ZonedDateTime parseZDT(String text) {
            if (text == null || text.trim().isEmpty()) return null;
            try {
                return ZonedDateTime.parse(text.trim());
            } catch (Exception e) {
                // fallback
                return null;
            }
        }
    }

    private class CalendarEvent {
        private String eventName;
        private String startTime;
        private String endTime;

        public String getStartTime() {return startTime;}
        public String getEndTime() {return endTime;}
    }

    private class SocialEvent {
        private String title;
        private String date;       // e.g. "2024-12-09" or "12/09/2024"
        private String time;       // e.g. "8:30 a.m.-6 p.m."
        private String location;
        private String link;
        private String description;
        private String cost;

        public LocalDate getDateLocal() {
            if (date == null || date.trim().isEmpty()) return null;
            try {
                return LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                // Try MM/dd/yyyy
                try {
                    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
                    return LocalDate.parse(date, fmt);
                } catch (Exception ex) {
                    return null;
                }
            }
        }

        public LocalTime getStartLocalTime() {
            return parseEventTime(true);
        }

        public LocalTime getEndLocalTime() {
            return parseEventTime(false);
        }

        private LocalTime parseEventTime(boolean start) {
            // time might be something like "8:30 a.m.-6 p.m."
            // Let's split by '-'
            if (time == null || time.trim().isEmpty()) return null;
            String[] parts = time.split("-");
            String segment;
            if (start) {
                segment = parts[0].trim();
            } else {
                if (parts.length < 2) return null;
                segment = parts[1].trim();
            }

            // Normalize AM/PM: remove periods, e.g. "a.m." -> "AM"
            segment = segment.replaceAll("\\.", "").toUpperCase();

            // Common formats: "8:30 AM", "12 PM"
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("[hh:mm a][h:mm a][hh a][h a]", Locale.US);
            try {
                return LocalTime.parse(segment, fmt);
            } catch (Exception e) {
                // If fails, try a different approach or default
                // default to 12:00 if unparseable
                return LocalTime.of(start ? 8 : 17, 0);
            }
        }
    }

    private class ScheduledBlock {
        String title;
        String type;
        String startTime;
        String endTime;
        String description;
        public ScheduledBlock(String title, String type, ZonedDateTime start, ZonedDateTime end, String desc) {
            this.title = title;
            this.type = type;
            this.startTime = start.toString();
            this.endTime = end.toString();
            this.description = desc;
        }
    }

    private class TimeWindow {
        LocalTime start;
        LocalTime end;
        TimeWindow(LocalTime s, LocalTime e) {this.start=s; this.end=e;}
    }

    private class BusyBlock {
        ZonedDateTime start;
        ZonedDateTime end;
        BusyBlock(ZonedDateTime s, ZonedDateTime e){this.start=s;this.end=e;}
    }

    private class FreeBlock {
        LocalTime start;
        LocalTime end;
        FreeBlock(LocalTime s, LocalTime e){this.start=s;this.end=e;}
        int durationMinutes() {
            return (int)Duration.between(start,end).toMinutes();
        }
    }
}
