import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class WeeklyWorkedHours {

    public static Map<String, Double> weeklyHoursMap = new HashMap<>();
    public static Map<String, Double> weeklyOvertimeMap = new HashMap<>();
    public static Map<String, Double> weeklyUndertimeMap = new HashMap<>();
    public static Map<String, Double> weeklyLatetimeMap = new HashMap<>();


    public static Map<String, Double> monthlyHoursMap = new HashMap<>();
    public static Map<String, Double> monthlyOvertimeMap = new HashMap<>();
    public static Map<String, Double> monthlyUnderTimeMap = new HashMap<>();
    public static Map<String, Double> monthlyLatetimeMap = new HashMap<>();

    public static void calculateHours(String csvFile) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
        SimpleDateFormat dateTimeFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm");

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] parts = line.split(",");
                if (parts.length < 6) {
                    System.err.println("Invalid line: " + line);
                    continue;
                }

                String employeeId = parts[0];
                String dateStr = parts[3];
                String loginTimeStr = parts[4];
                String logoutTimeStr = parts[5];

                Date date;
                try {
                    date = dateFormat.parse(dateStr);
                } catch (ParseException e) {
                    System.err.println("Invalid date format in line: " + line);
                    continue;
                }

                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
                    continue; // Skip weekends
                }

                Date loginDateTime, logoutDateTime;
                try {
                    loginDateTime = dateTimeFormat.parse(dateStr + " " + loginTimeStr);
                    logoutDateTime = dateTimeFormat.parse(dateStr + " " + logoutTimeStr);
                } catch (ParseException e) {
                    System.err.println("Invalid time format in line: " + line);
                    continue;
                }

                long loginMillis = loginDateTime.getTime();
                long logoutMillis = logoutDateTime.getTime();

                if (logoutMillis < loginMillis) {
                    System.err.println("Logout time before login time in line: " + line);
                    continue;
                }

                // Calculate 8:00 AM and 5:00 PM for the current date
                cal.set(Calendar.HOUR_OF_DAY, 8);
                cal.set(Calendar.MINUTE, 0);
                long eightAMMillis = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 17);
                long fivePMMillis = cal.getTimeInMillis();

                long gracePeriodMillis = 10 * 60 * 1000; // 10 minutes
                long eightTenAMMillis = eightAMMillis + gracePeriodMillis;

                boolean isOnTime = (loginMillis <= eightTenAMMillis);

                // Calculate late time if login is after 8:10 AM
                long lateMillis = 0;
                if (!isOnTime) {
                    lateMillis = loginMillis - eightAMMillis;
                }

                long startMillis = isOnTime ? eightAMMillis : loginMillis;
                long endMillisRegular = Math.min(logoutMillis, fivePMMillis);

                // Calculate overtime (only if logged in on time)
                long overtimeMillis = isOnTime ? Math.max(0, logoutMillis - fivePMMillis) : 0;

                // Calculate underTime if logged out early
                long undertimeMillis = (logoutMillis < fivePMMillis) ? (fivePMMillis - logoutMillis) : 0;



                // Calculate noon break overlap
                cal.set(Calendar.HOUR_OF_DAY, 12);
                long noonStartMillis = cal.getTimeInMillis();
                cal.set(Calendar.HOUR_OF_DAY, 13);
                long noonEndMillis = cal.getTimeInMillis();

                long breakOverlap = Math.max(0, Math.min(endMillisRegular, noonEndMillis) - Math.max(startMillis, noonStartMillis));
                long regularMillis = (endMillisRegular - startMillis) - breakOverlap;

                if (regularMillis < 0) {
                    System.err.println("Negative duration in line: " + line);
                    continue;
                }

                // Convert to hours
                double regularHours = regularMillis / (1000.0 * 60 * 60);
                double overtimeHours = overtimeMillis / (1000.0 * 60 * 60);
                double undertimeHours = undertimeMillis / (1000.0 * 60 * 60);
                double lateTimeHours = lateMillis / (1000.0 * 60 * 60);

                // Update weekly aggregates
                cal.setTime(date);
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                String weekKey = employeeId + "_" + dateFormat.format(cal.getTime());

                weeklyHoursMap.merge(weekKey, regularHours, Double::sum);
                weeklyOvertimeMap.merge(weekKey, overtimeHours, Double::sum);
                weeklyUndertimeMap.merge(weekKey, undertimeHours, Double::sum);
                weeklyLatetimeMap.merge(weekKey, lateTimeHours, Double::sum);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void calculateMonthlyHours() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

        // Process regular hours
        for (Map.Entry<String, Double> entry : weeklyHoursMap.entrySet()) {
            String weekKey = entry.getKey();
            double hours = entry.getValue();

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
                int month = cal.get(Calendar.MONTH) + 1; // Adjust to 1-12
                String monthKey = String.format("%s_%04d-%02d", employeeId, year, month);

                monthlyHoursMap.put(monthKey, monthlyHoursMap.getOrDefault(monthKey, 0.0) + hours);
            } catch (ParseException e) {
                System.err.println("Error parsing week start date: " + weekStartStr);
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