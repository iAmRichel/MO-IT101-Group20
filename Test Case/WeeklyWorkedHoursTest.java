import static org.junit.jupiter.api.Assertions.*;
import java.text.ParseException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class WeeklyWorkedHoursTest {

    @BeforeEach
    void resetMaps() {
        WeeklyWorkedHours.weeklyHours.clear();
        WeeklyWorkedHours.weeklyOvertime.clear();
        WeeklyWorkedHours.weeklyUnderTime.clear();
        WeeklyWorkedHours.weeklyLateTime.clear();
    }

    @Test
    void processAttendanceLine_Valid() throws ParseException {
        String line = "EMP001,,,12/25/2023,08:00,17:00";
        WeeklyWorkedHours.processAttendanceLine(line);
        assertFalse(WeeklyWorkedHours.weeklyHours.isEmpty()); // Now passes!
    }

    @Test
    void calculateDailyHours_WithOvertime() throws ParseException {
        Date workDate = DateUtils.parseDate("12/25/2023", "MM/dd/yyyy");
        Date login = DateUtils.parseDateTime("12/25/2023", "08:00");
        Date logout = DateUtils.parseDateTime("12/25/2023", "19:00"); // 2 hours overtime
        WeeklyWorkedHours.WorkHourCalculationResult result =
                WeeklyWorkedHours.calculateDailyHours(workDate, login, logout);
        assertEquals(2.0, result.overtime, 0.01);
    }

    @Test
    void updateWeeklyMaps_AggregatesData() throws ParseException {
        Date fixedDate = DateUtils.parseDate("12/31/2023", "MM/dd/yyyy");

        WeeklyWorkedHours.updateWeeklyMaps(
                "EMP001",
                fixedDate,
                new WeeklyWorkedHours.WorkHourCalculationResult(8, 2, 0, 0)
        );

        String expectedWeekKey = DateUtils.getWeekKey(fixedDate, "EMP001");

        assertEquals(8.0, WeeklyWorkedHours.weeklyHours.get(expectedWeekKey));
    }
}