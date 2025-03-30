import static org.junit.jupiter.api.Assertions.*;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class GrossNetWeeklySalaryTest {

    private final Map<String, String[]> employees = new HashMap<>();

    @BeforeEach
    void setup() {
        // Mock employee data
        String[] empData = new String[19];
        empData[EmployeeDetails.IDX_HOURLY_RATE] = "500";
        empData[EmployeeDetails.IDX_BASIC_SALARY] = "40000";
        empData[EmployeeDetails.IDX_RICE_SUBSIDY] = "1500";
        empData[EmployeeDetails.IDX_PHONE_ALLOWANCE] = "1000";
        empData[EmployeeDetails.IDX_CLOTHING_ALLOWANCE] = "500";
        employees.put("EMP001", empData);
    }

    @Test
    void calculateSalary_LastWeek() throws ParseException {
        Date inputDate = DateUtils.parseDate("12/31/2023", "MM/dd/yyyy");
        GrossNetWeeklySalary.SalaryData data =
                GrossNetWeeklySalary.calculateSalary(employees.get("EMP001"), inputDate); // Updated method
        assertTrue(data.isLastWeek);
    }

    @Test
    void calculateSalary_NonLastWeek() throws ParseException {
        Date inputDate = DateUtils.parseDate("12/10/2023", "MM/dd/yyyy");
        String weekKey = DateUtils.getWeekKey(inputDate, "EMP001");

        GrossNetWeeklySalary.SalaryData data =
                GrossNetWeeklySalary.calculateSalary(employees.get("EMP001"), inputDate);

        assertFalse(data.isLastWeek); // Now passes!
    }
}