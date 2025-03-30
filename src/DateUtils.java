import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
public class DateUtils {
    // Thread-unsafe date formatters (SimpleDateFormat is not thread-safe)
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yyyy");
    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("MM/dd/yyyy HH:mm");

    /**
     * Parses a date string using the given patterns (tries each until success).
     * @throws ParseException if no pattern matches the input
     */
    public static Date parseDate(String input, String... patterns) throws ParseException {
        for (String pattern : patterns) {
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            sdf.setLenient(false);
            try {
                return sdf.parse(input);
            } catch (ParseException ignored) {}
        }
        throw new ParseException("Invalid date format: " + input, 0);
    }

    public static boolean isLastWeekOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        return cal.get(Calendar.DAY_OF_MONTH) > (lastDay - 7);
    }
    /**
     * Checks if the given date falls within the last week of its month.
     * @param date The date to check
     * @return true if the date is within the last 7 days of the month, false otherwise
     */
    public static String getWeekKey(Date date, String employeeId) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);

        // (Monday as first day of the week)
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);

        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY); // Always jump to Monday of the week
        return employeeId + "_" + DATE_FORMAT.format(cal.getTime());
    }

    /**
     * Parses a date and time string into a Date object using the format "MM/dd/yyyy HH:mm".
     * @param dateStr The date part (MM/dd/yyyy)
     * @param timeStr The time part (HH:mm)
     * @return Parsed Date object
     * @throws ParseException If the input format is invalid
     */
    public static Date parseDateTime(String dateStr, String timeStr) throws ParseException {
        return DATE_TIME_FORMAT.parse(dateStr + " " + timeStr);
    }
}