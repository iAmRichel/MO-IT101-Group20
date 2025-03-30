import static org.junit.jupiter.api.Assertions.*;
import java.text.ParseException;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DateUtilsTest {

    @Test
    void parseDate_ValidFormats() throws ParseException {
        Date date1 = DateUtils.parseDate("12/31/2023", "MM/dd/yyyy");
        Date date2 = DateUtils.parseDate("12-31-2023", "MM-dd-yyyy");
        assertNotNull(date1);
        assertNotNull(date2);
    }

    @Test
    void parseDate_InvalidFormat() {
        assertThrows(ParseException.class, () ->
                DateUtils.parseDate("2023-31-12", "MM/dd/yyyy", "MM-dd-yyyy")
        );
    }

    @Test
    void isLastWeekOfMonth_True() throws ParseException {
        Date date = DateUtils.parseDate("12/25/2023", "MM/dd/yyyy"); // Last week of Dec 2023
        assertTrue(DateUtils.isLastWeekOfMonth(date));
    }

    @Test
    void isLastWeekOfMonth_False() throws ParseException {
        Date date = DateUtils.parseDate("12/20/2023", "MM/dd/yyyy"); // Not last week
        assertFalse(DateUtils.isLastWeekOfMonth(date));
    }

    @Test
    void getWeekKey_CorrectFormat() throws ParseException {
        Date date = DateUtils.parseDate("12/31/2023", "MM/dd/yyyy"); // Sunday
        String weekKey = DateUtils.getWeekKey(date, "EMP001");
        assertEquals("EMP001_12/25/2023", weekKey);
    }
}