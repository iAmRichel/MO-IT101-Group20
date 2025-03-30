import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EmployeeDetails {
    // Constants for accessing employee data fields by index
    public static final int IDX_EMPLOYEE_ID = 0;
    public static final int IDX_LAST_NAME = 1;
    public static final int IDX_FIRST_NAME = 2;
    public static final int IDX_BIRTHDAY = 3;
    public static final int IDX_ADDRESS = 4;
    public static final int IDX_PHONE = 5;
    public static final int IDX_SSS = 6;
    public static final int IDX_PHILHEALTH = 7;
    public static final int IDX_TIN = 8;
    public static final int IDX_PAGIBIG = 9;
    public static final int IDX_STATUS = 10;
    public static final int IDX_POSITION = 11;
    public static final int IDX_SUPERVISOR = 12;
    public static final int IDX_BASIC_SALARY = 13;
    public static final int IDX_RICE_SUBSIDY = 14;
    public static final int IDX_PHONE_ALLOWANCE = 15;
    public static final int IDX_CLOTHING_ALLOWANCE = 16;
    public static final int IDX_GROSS_SEMI_MONTHLY = 17;
    public static final int IDX_HOURLY_RATE = 18;

    // Minimum required fields for valid employee data
    private static final int MIN_REQUIRED_FIELDS = 19;
    // Labels corresponding to each employee data field
    private static final String[] LABELS = {
            "Employee #", "Last Name", "First Name", "Birthday", "Address",
            "Phone Number", "SSS #", "Philhealth #", "TIN #", "Pag-ibig #",
            "Status", "Position", "Immediate Supervisor", "Basic Salary",
            "Rice Subsidy", "Phone Allowance", "Clothing Allowance",
            "Gross Semi-monthly Rate", "Hourly Rate"
    };

    /**
     * Reads employee details from a CSV file and returns them as a map (ID -> employee data).
     * Skips invalid records and logs errors.
     */
    public static Map<String, String[]> readEmployeeDetails(String filePath) throws IOException {
        Map<String, String[]> employeeMap = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            br.readLine(); // Skip header
            String line;
            int lineNumber = 1;

            while ((line = br.readLine()) != null) {
                lineNumber++;
                try {
                    String[] employeeData = parseEmployeeLine(line);
                    if (isValidEmployeeRecord(employeeData)) {
                        employeeMap.put(employeeData[IDX_EMPLOYEE_ID], employeeData);
                    } else {
                        System.err.println("Skipping invalid record at line " + lineNumber);
                    }
                } catch (InvalidEmployeeRecordException e) {
                    System.err.println("Error processing line " + lineNumber + ": " + e.getMessage());
                }
            }
        }
        return employeeMap;
    }

    /**
     * Parses a CSV line into employee data fields, handling quoted values.
     * Trims whitespace and validates required fields (e.g., employee ID).
     * @throws InvalidEmployeeRecordException if parsing fails or required data is missing
     */
    static String[] parseEmployeeLine(String line) throws InvalidEmployeeRecordException {
        try {
            // Split CSV line while handling quoted fields
            String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

            // Clean and validate fields
            for (int i = 0; i < data.length; i++) {
                data[i] = data[i].replace("\"", "").trim();
                if (i == IDX_EMPLOYEE_ID && data[i].isEmpty()) {
                    throw new InvalidEmployeeRecordException("Missing employee ID");
                }
            }
            return data;
        } catch (Exception e) {
            throw new InvalidEmployeeRecordException("Failed to parse line: " + e.getMessage());
        }
    }

    /**
     * Validates basic employee record requirements:
     * - Non-null data array
     * - Contains minimum required fields
     * - Employee ID is not empty
     */
    private static boolean isValidEmployeeRecord(String[] data) {
        return data != null &&
                data.length >= MIN_REQUIRED_FIELDS &&
                !data[IDX_EMPLOYEE_ID].isEmpty();
    }
    /**
     * Displays formatted employee details using corresponding labels.
     * Shows error message if data is invalid or incomplete.
     */
    public static void displayEmployeeDetails(String[] empData) {
        if (empData == null || empData.length < LABELS.length) {
            System.out.println("Invalid employee data");
            return;
        }

        System.out.println("\nEmployee Details:");
        for (int i = 0; i < LABELS.length; i++) {
            System.out.printf("%-30s: %s%n", LABELS[i],
                    (i < empData.length) ? empData[i] : "N/A");
        }
        System.out.println("______________________");
    }

    // Custom exception for better error handling
    static class InvalidEmployeeRecordException extends Exception {
        public InvalidEmployeeRecordException(String message) {
            super(message);
        }
    }
}