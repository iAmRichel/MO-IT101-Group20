import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
/**
 * Tracks and calculates weekly work hours including regular hours, overtime,
 * underTime, and late time for all employees based on attendance records.
 *
 * <p>Maintains four static maps tracking different hour types for each employee-week combination.
 * Uses a standardized week key format: "employeeId_YYYY-MM-dd" (date represents Sunday of the week).
 */
public class WeeklyWorkedHours {
    /**
     * Map of regular hours worked per week.
     * Key format: "employeeId_YYYY-MM-dd" (Sunday date of the week)
     */
    public static final Map<String, Double> weeklyHours = new HashMap<>();

    /**
     * Map of overtime hours worked per week.
     * Key format same as weeklyHours.
     */
    public static final Map<String, Double> weeklyOvertime = new HashMap<>();

    /**
     * Map of underTime hours per week.
     * Key format same as weeklyHours.
     */
    public static final Map<String, Double> weeklyUnderTime = new HashMap<>();

    /**
     * Map of late arrival hours per week.
     * Key format same as weeklyHours.
     */
    public static final Map<String, Double> weeklyLateTime = new HashMap<>();

    // Attendance calculation constants
    /** Grace period for late arrivals in minutes */
    private static final int GRACE_PERIOD_MINUTES = 10;

    /** Standard workday start hour (8 AM) */
    private static final int WORKDAY_START_HOUR = 8;

    /** Standard workday end hour (5 PM) */
    private static final int WORKDAY_END_HOUR = 17;

    /** Standard workday end hour (5 PM) */
    private static final int NOON_BREAK_START = 12;

    /** Noon break end hour (1 PM) */
    private static final int NOON_BREAK_END = 13;


    /**
     * Processes an attendance file and populates the weekly hour maps.
     *
     * @param filePath Path to the attendance CSV file (MotorPH)
     * @throws IOException If there's an error reading the file
     *
     * @implNote Expected CSV format:
     * employeeId,date,inTime,outTime
     * Example: "EMP001,2023-05-15,08:05,17:30"
     */
    public static void processAttendanceFile(String filePath) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // Skip header
            String line;
            while ((line = br.readLine()) != null) {
                processAttendanceLine(line);
            }
        }
    }

    /**
     * Processes a single line from the attendance CSV file and updates weekly hour accumulators.
     *
     * @param line A comma-separated line from the attendance file with format:
     *             employeeId,lastName,firstName,date,loginTime,logoutTime[,additionalFields...]
     *
     * @throws ArrayIndexOutOfBoundsException if line has insufficient fields (handled internally)
     *
     * @implSpec The method:
     * 1. Validates input format and skips malformed lines
     * 2. Ignores weekend attendance records
     * 3. Calculates daily work hours including:
     *    - Regular hours (8:00-17:00 minus 1h lunch break)
     *    - Overtime (after 17:00)
     *    - UnderTime (less than 8 working hours)
     *    - Late time (arrival after 8:10)
     * 4. Accumulates results in weekly tracking maps
     *
     * @implNote Important behaviors:
     * - Uses 10-minute grace period before marking late arrivals
     * - Standard workday is 8 hours (9-5 with 1h lunch)
     * - Weekend days (Saturday/Sunday) are automatically skipped
     * - Invalid time ranges (logout before login) are logged and skipped
     */
    static void processAttendanceLine(String line) {
        try {
            // Split CSV line into components
            String[] parts = line.split(",");

            // Skip lines with insufficient data (need at least 6 fields)
            if (parts.length < 6) return;

            // Extract relevant fields from CSV
            String employeeId = parts[0].trim();
            String dateStr = parts[3].trim();       // Date in MM/dd/yyyy format
            String loginTimeStr = parts[4].trim();  // Login time in HH:mm format
            String logoutTimeStr = parts[5].trim(); // Logout time in HH:mm format

            // Parse work date and skip weekends
            Date workDate = DateUtils.parseDate(dateStr, "MM/dd/yyyy");
            if (isWeekend(workDate)) return; // No processing for weekends

            // Create full datetime objects for calculations
            Date loginTime = DateUtils.parseDateTime(dateStr, loginTimeStr);
            Date logoutTime = DateUtils.parseDateTime(dateStr, logoutTimeStr);

            // Validate time range (logout must be after login)
            if (logoutTime.before(loginTime)) {
                System.err.println("Invalid time range: " + line);
                return;
            }

            // Calculate all hour components for this work day
            WorkHourCalculationResult result = calculateDailyHours(workDate, loginTime, logoutTime);

            // Update weekly accumulation maps
            updateWeeklyMaps(employeeId, workDate, result);

            //throws ParseException if date/time parsing fails (handled internally)
            //throws ArrayIndexOutOfBoundsException if line has insufficient fields (handled internally)
        } catch (ParseException | ArrayIndexOutOfBoundsException e) {
            System.err.println("Error processing line: " + line);
        }
    }

    /**
     * Calculates daily work hours including regular, overtime, underTime, and late hours.
     * Accounts for:
     * - Standard work hours (8AM-5PM with 1h lunch break)
     * - 10-minute grace period for late arrivals
     * - Noon break deduction (12PM-1PM)
     */
    static WorkHourCalculationResult calculateDailyHours(Date workDate, Date loginTime, Date logoutTime) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(workDate);

        // Set standard work hours
        cal.set(Calendar.HOUR_OF_DAY, WORKDAY_START_HOUR);
        cal.set(Calendar.MINUTE, 0);
        Date workStart = cal.getTime();

        cal.set(Calendar.HOUR_OF_DAY, WORKDAY_END_HOUR);
        Date workEnd = cal.getTime();

        // Calculate grace period (8:00 AM to 8:10 AM)
        long gracePeriodEnd = workStart.getTime() + (GRACE_PERIOD_MINUTES * 60 * 1000);

        // Calculate late time
        long lateMillis = Math.max(0, loginTime.getTime() - workStart.getTime());
        boolean isOnTime = loginTime.getTime() <= gracePeriodEnd;

        // Adjust effective start time
        long effectiveStart = isOnTime ? workStart.getTime() : loginTime.getTime();

        // Calculate regular time (considering noon break)
        long noonStart = cal.getTimeInMillis();
        cal.set(Calendar.HOUR_OF_DAY, NOON_BREAK_START);
        cal.set(Calendar.HOUR_OF_DAY, NOON_BREAK_END);
        long noonEnd = cal.getTimeInMillis();

        long regularEnd = Math.min(logoutTime.getTime(), workEnd.getTime());
        long breakOverlap = calculateBreakOverlap(effectiveStart, regularEnd, noonStart, noonEnd);
        long regularMillis = (regularEnd - effectiveStart) - breakOverlap;

        // Calculate overtime (only if logged out after work end and was on time)
        long overtimeMillis = isOnTime ? Math.max(0, logoutTime.getTime() - workEnd.getTime()) : 0;

        // Calculate under time (if logged out early)
        long underTimeMillis = logoutTime.before(workEnd) ? (workEnd.getTime() - logoutTime.getTime()) : 0;

        // Convert milliseconds to hours
        double regularHours = Math.max(0, regularMillis) / (1000.0 * 60 * 60);
        double overtimeHours = overtimeMillis / (1000.0 * 60 * 60);
        double underTimeHours = underTimeMillis / (1000.0 * 60 * 60);
        double lateHours = (isOnTime ? 0 : lateMillis) / (1000.0 * 60 * 60);

        return new WorkHourCalculationResult(
                regularHours,
                overtimeHours,
                underTimeHours,
                lateHours
        );
    }

    /**
     * Calculates the overlap duration (in milliseconds) between a work period and a break period.
     *
     * @param start Work period start time (ms)
     * @param end Work period end time (ms)
     * @param breakStart Break period start time (ms)
     * @param breakEnd Break period end time (ms)
     * @return Overlap duration in milliseconds (0 if no overlap)
     */
    private static long calculateBreakOverlap(long start, long end, long breakStart, long breakEnd) {

        // Find latest start and earliest end of the overlapping period
        long overlapStart = Math.max(start, breakStart);
        long overlapEnd = Math.min(end, breakEnd);

        // Return overlap duration (or 0 if no overlap)
        return Math.max(0, overlapEnd - overlapStart);
    }

    /**
     * Checks if the given date falls on a weekend (Saturday or Sunday).
     *
     * @param date The date to check
     * @return true if the date is a weekend day, false otherwise
     */
    private static boolean isWeekend(Date date) {

        // Create calendar instance and set to the specified date
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        // Get day of week (1=Sunday, 2=Monday, ..., 7=Saturday)
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        // Check if it's Saturday (7) or Sunday (1)
        return dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY;
    }

    /**
     * Updates weekly tracking maps with daily work hour calculations for an employee.
     * Aggregates regular, overtime, underTime, and late hours by week.
     *
     * @param employeeId  ID of the employee
     * @param date        Work date being processed
     * @param result      Calculated work hours for the day
     */
    static void updateWeeklyMaps(String employeeId, Date date, WorkHourCalculationResult result) {
        // Generate unique week identifier (e.g., "2023-W14-EMP001")
        String weekKey = DateUtils.getWeekKey(date, employeeId);

        // Aggregate hours by week using Map.merge():
        // - If key exists, adds new hours to existing value
        // - If key doesn't exist, creates new entry with initial hours

        // Update regular hours map
        weeklyHours.merge(weekKey, result.regular, Double::sum);

        // Update overtime hours map
        weeklyOvertime.merge(weekKey, result.overtime, Double::sum);

        // Update underTime hours map
        weeklyUnderTime.merge(weekKey, result.underTime, Double::sum);

        // Update late hours map
        weeklyLateTime.merge(weekKey, result.late, Double::sum);
    }

    /**
     * Immutable container class for storing daily work hour calculation results.
     * All time values are stored in hours with decimal precision.
     */
    static class WorkHourCalculationResult {
        /** Regular working hours (excluding breaks) */
        final double regular;

        /** Overtime hours worked beyond standard schedule */
        final double overtime;

        /** Hours short of standard working time (early logout) */
        final double underTime;

        /** Late arrival time (counted only if beyond grace period) */
        final double late;

        /**
         * Constructs a new WorkHourCalculationResult with the specified time values.
         *
         * @param regular   Regular working hours
         * @param overtime  Overtime hours
         * @param underTime UnderTime hours
         * @param late      Late arrival hours
         */
        WorkHourCalculationResult(double regular, double overtime, double underTime, double late) {
            this.regular = regular;
            this.overtime = overtime;
            this.underTime = underTime;
            this.late = late;
        }
    }
}