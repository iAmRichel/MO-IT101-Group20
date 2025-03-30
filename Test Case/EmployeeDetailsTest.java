import static org.junit.jupiter.api.Assertions.*;
import java.io.IOException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EmployeeDetailsTest {

    @Test
    void readEmployeeDetails_ValidFile() throws IOException {
        Map<String, String[]> employees =
                EmployeeDetails.readEmployeeDetails("Test Case/resources/valid_employees.csv");
        assertFalse(employees.isEmpty());
    }

    @Test
    void readEmployeeDetails_InvalidFile() {
        assertThrows(IOException.class, () ->
                EmployeeDetails.readEmployeeDetails("invalid_path.csv")
        );
    }

    @Test
    void parseEmployeeLine_ValidData() throws Exception {
        String line = "EMP001,Doe,John,01/01/1990,\"Address\",123456,SSS123";
        String[] data = EmployeeDetails.parseEmployeeLine(line);
        assertEquals("EMP001", data[EmployeeDetails.IDX_EMPLOYEE_ID]);
    }

    @Test
    void parseEmployeeLine_InvalidData() {
        String line = ",,,,"; // Missing employee ID
        assertThrows(
                EmployeeDetails.InvalidEmployeeRecordException.class,
                () -> EmployeeDetails.parseEmployeeLine(line)
        );
    }
}