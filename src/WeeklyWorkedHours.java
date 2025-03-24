import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * This class calculates weekly and monthly worked hours, including overtime, undertime and late time
 * based on employee attendance records from a CSV file.
 */

public class WeeklyWorkedHours {

    // Maps to store weekly calculations for each employee
    public static Map<String, Double> weeklyHoursMap = new HashMap<>();
    public static Map<String, Double> weeklyOvertimeMap = new HashMap<>();
    public static Map<String, Double> weeklyUndertimeMap = new HashMap<>();
    public static Map<String, Double> weeklyLatetimeMap = new HashMap<>();

    // Maps to store monthly calculations for each employee
    public static Map<String, Double> monthlyHoursMap = new HashMap<>();
    public static Map<String, Double> monthlyOvertimeMap = new HashMap<>();
    public static Map<String, Double> monthlyUnderTimeMap = new HashMap<>();
    public static Map<String, Double> monthlyLatetimeMap = new HashMap<>();
    
   
    /**
     * Reads a CSV file and calculates worked hours, overtime, undertime, and late time for each employee.
     * @param attendanceFile path to the CSV file containing attendance data.
     */
    public static void calculateHours(String attendanceFile) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

        try (BufferedReader br = new BufferedReader(new FileReader(attendanceFile))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false; //skip the header line
                    continue;
                }

                // Split CSV line into parts
                String[] parts = line.split(",");
                if (parts.length < 6) {
                    System.err.println("Invalid line: " + line);
                    continue;
                }

                String employeeId = parts[0];
                String dateString = parts[3];
                String timeInString = parts[4];
                String timeOutString = parts[5];

                // Parse date from the CSV record
                Date date;
                try {
                    date = dateFormat.parse(dateString);
                } catch (ParseException e) {
                    System.err.println("Invalid date format in line: " + line);
                    continue;
                }

                // Check if the day is weekend (Saturday or Sunday)
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    continue; // Skip weekends
                }

                // Parse time-in and time-out timestamps
                Date timeInDateTime, timeOutDateTime;
                try {
                    timeInDateTime = dateTimeFormat.parse(dateString + " " + timeInString);
                    timeOutDateTime = dateTimeFormat.parse(dateString + " " + timeOutString);
                } catch (ParseException e) {
                    System.err.println("Invalid time format in line: " + line);
                    continue;
                }

                long timeIn = timeInDateTime.getTime();
                long timeOut = timeOutDateTime.getTime();

                if (timeOut < timeIn) {
                    System.err.println("Logout time before login time in line: " + line);
                    continue;
                }

                // Define standard working hours (8:00 AM - 5:00 PM)
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                long startOfShift = cal.getTimeInMillis();
                
                cal.set(Calendar.HOUR_OF_DAY, 17);
                long endOfShift = cal.getTimeInMillis();

                long gracePeriodDuration = 10 * 60 * 1000; // 10 minutes
                long gracePeriodTime = startOfShift + gracePeriodDuration; //8:10

                // Determine if the employee is on time
                boolean isOnTime = (timeIn <= gracePeriodTime);

                // Calculate late time if login is after 8:10 AM
                long lateTimeHoursInMs = 0;
                if (!isOnTime) {
                    lateTimeHoursInMs = timeIn - startOfShift;
                }

                // Assign appropriate shift start and end times
                long assignedStart = isOnTime ? startOfShift : timeIn; // Assigns startOfShift if on time and timeIn if not, ensures grace period is applied
                long assignedEnd = Math.min(timeOut, endOfShift); // Assigns timeOut if the employee times out before endOfShift

                // Calculate overtime (only if logged in on time)
                long overtimeHoursInMs = isOnTime ? Math.max(0, timeOut - endOfShift) : 0;

                // Calculate underTime if logged out early
                long undertimeHoursInMs = (timeOut < endOfShift) ? (endOfShift - timeOut) : 0;



                // Calculate noon break overlap
                cal.set(Calendar.HOUR_OF_DAY, 12);
                long noonBreakStart = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 13);
                long noonBreakEnd = cal.getTimeInMillis();

                long breakOverlap = Math.max(0, Math.min(assignedEnd, noonBreakEnd) - Math.max(assignedStart, noonBreakStart));
                long regularHoursInMs = (assignedEnd - assignedStart) - breakOverlap;

                if (regularHoursInMs < 0) {
                    System.err.println("Negative duration in line: " + line);
                    continue;
                }

                // Convert milliseconds to hours
                double regularHours = regularHoursInMs / (1000.0 * 60 * 60);
                double overtimeHours = overtimeHoursInMs / (1000.0 * 60 * 60);
                double undertimeHours = undertimeHoursInMs / (1000.0 * 60 * 60);
                double lateTimeHours = lateTimeHoursInMs / (1000.0 * 60 * 60);

                // Generate a unique key for each week's data
                cal.setTime(date);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                String weekKey = employeeId + "_" + dateFormat.format(cal.getTime());

                // Update weekly aggregates
                weeklyHoursMap.merge(weekKey, regularHours, Double::sum);
                weeklyOvertimeMap.merge(weekKey, overtimeHours, Double::sum);
                weeklyUndertimeMap.merge(weekKey, undertimeHours, Double::sum);
                weeklyLatetimeMap.merge(weekKey, lateTimeHours, Double::sum);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
    * Aggregates weekly worked hours, overtime, undertime, and late time into monthly totals.
    */
    public static void calculateMonthlyHours() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        // Process all weekly data and sum them into monthly totals
        for (Map.Entry<String, Double> entry : weeklyHoursMap.entrySet()) {
            String weekKey = entry.getKey();
            double hours = entry.getValue();

            String[] parts = weekKey.split("_");
            if (parts.length != 2) {
                System.err.println("Invalid week key: " + weekKey);
                continue;
            }
            String employeeId = parts[0];
            String weekStartString = parts[1];

            try {
                Date weekStartDate = dateFormat.parse(weekStartString);
                Calendar cal = Calendar.getInstance();
                cal.setTime(weekStartDate);
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1; // Adjust to 1-12
                String monthKey = String.format("%s_%04d-%02d", employeeId, year, month);

                monthlyHoursMap.put(monthKey, monthlyHoursMap.getOrDefault(monthKey, 0.0) + hours);
            } catch (ParseException e) {
                System.err.println("Error parsing week start date: " + weekStartString);
            }
        }

        // Process overtime hours
        for (Map.Entry<String, Double> entry : weeklyOvertimeMap.entrySet()) {
            String weekKey = entry.getKey();
            double overtime = entry.getValue();

            String[] parts = weekKey.split("_");
            if (parts.length != 2) {
                System.err.println("Invalid week key: " + weekKey);
                continue;
            }
            String employeeId = parts[0];
            String weekStartStr = parts[1];

            try {
                Date weekStartDate = dateFormat.parse(weekStartStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(weekStartDate);
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                String monthKey = String.format("%s_%04d-%02d", employeeId, year, month);

                monthlyOvertimeMap.put(monthKey, monthlyOvertimeMap.getOrDefault(monthKey, 0.0) + overtime);


            } catch (ParseException e) {
                System.err.println("Error parsing week start date: " + weekStartStr);
            }
        }
        // Process Monthly under Time
        for (Map.Entry<String, Double> entry : weeklyUndertimeMap.entrySet()) {
            String weekKey = entry.getKey();
            double overtime = entry.getValue();

            String[] parts = weekKey.split("_");
            if (parts.length != 2) {
                System.err.println("Invalid week key: " + weekKey);
                continue;
            }
            String employeeId = parts[0];
            String weekStartStr = parts[1];

            try {
                Date weekStartDate = dateFormat.parse(weekStartStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(weekStartDate);
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                String monthKey = String.format("%s_%04d-%02d", employeeId, year, month);

                monthlyUnderTimeMap.put(monthKey, monthlyUnderTimeMap.getOrDefault(monthKey, 0.0) + overtime);


            } catch (ParseException e) {
                System.err.println("Error parsing week start date: " + weekStartStr);
            }
        }
        // Process Monthly Late Time
        for (Map.Entry<String, Double> entry : weeklyLatetimeMap.entrySet()) {
            String weekKey = entry.getKey();
            double overtime = entry.getValue();

            String[] parts = weekKey.split("_");
            if (parts.length != 2) {
                System.err.println("Invalid week key: " + weekKey);
                continue;
            }
            String employeeId = parts[0];
            String weekStartStr = parts[1];

            try {
                Date weekStartDate = dateFormat.parse(weekStartStr);
                Calendar cal = Calendar.getInstance();
                cal.setTime(weekStartDate);
                int year = cal.get(Calendar.YEAR);
                int month = cal.get(Calendar.MONTH) + 1;
                String monthKey = String.format("%s_%04d-%02d", employeeId, year, month);

                monthlyLatetimeMap.put(monthKey, monthlyLatetimeMap.getOrDefault(monthKey, 0.0) + overtime);


            } catch (ParseException e) {
                System.err.println("Error parsing week start date: " + weekStartStr);
            }
        }
    }
}
