import java.io.IOException;
import java.text.ParseException;
import java.util.*;
/**
 * This class calculates the gross and net weekly salary for employees based on their worked hours.
 * It reads employee details and attendance records from CSV files, allows user to select an employee,
 * and calculates their salary considering regular and overtime hours with a fixed overtime rate.
 * Key features:
 * - Reads employee data from CSV files
 * - Processes weekly attendance records
 * - Interactive employee selection
 * - Salary calculation with overtime pay
 * - Handles potential file and parsing errors
 */
public class GrossNetWeeklySalary {
    // Overtime rate multiplier (1.25 = time and a quarter)
    private static final double OVERTIME_RATE = 1.25;

    // Main method - program entry point
    public static void main(String[] args) {
        try {
            // Read employee details and process attendance records
            Map<String, String[]> employees = EmployeeDetails.readEmployeeDetails("src/MotorPH Employee Data.csv");
            WeeklyWorkedHours.processAttendanceFile("src/MotorPH Employee attendance record.csv");

            // Get employee ID from user input
            Scanner scanner = new Scanner(System.in);
            String employeeId = promptEmployeeId(scanner, employees);
            if (employeeId == null) return;

            // Display employee details and calculate salary
            displayEmployeeDetails(employees.get(employeeId));
            processSalaryCalculation(scanner, employeeId, employees.get(employeeId));

        } catch (IOException | ParseException e) {
            System.err.println("Error: " + e.getMessage());
        }
    }

    private static String promptEmployeeId(Scanner scanner, Map<String, String[]> employees) {
        System.out.print("Enter employee ID: ");
        String id = scanner.nextLine().trim();
        if (!employees.containsKey(id)) {
            System.out.println("Employee not found.");
            return null;
        }
        return id;
    }

    /**
     * Displays the details of an employee by delegating to {@link EmployeeDetails#displayEmployeeDetails(String[])}.
     * This method serves as a wrapper to maintain consistent access to employee data display functionality.
     *
     * @param empData An array of Strings containing the employee's details in the following order:
     *                [0] - Employee ID
     *                [1] - Last Name
     *                [2] - First Name
     *                ... (other employee attributes)
     * @throws NullPointerException if empData is null
     */
    private static void displayEmployeeDetails(String[] empData) {
        EmployeeDetails.displayEmployeeDetails(empData);
    }

    /**
     * Prompts the user to enter an employee ID and checks if it exists in the system.
     *
     * @param scanner   The Scanner object for reading user input.
     * @param employeeId  A Map containing employee data with IDs as keys.
     * @return The validated employee ID if found, otherwise null.
     */
    private static void processSalaryCalculation(Scanner scanner, String employeeId, String[] empData)
            throws ParseException {
        Date inputDate = getInputDate(scanner);

        // Calculate salary based on employee data and input date
        SalaryData salaryData = calculateSalary(empData, inputDate);
        salaryData.isLastWeek = DateUtils.isLastWeekOfMonth(inputDate);

        // Check if this is the last week of the month for special deductions
        if (salaryData.isLastWeek) {
            applyMonthlyDeductions(empData, salaryData);
        } else {
            // For non-last weeks, net pay equals gross pay (no deductions)
            salaryData.netPay = salaryData.grossWeekly;
        }
        // Display the calculated salary results to the user
        displayResults(salaryData);
        // Close the scanner to prevent resource leaks
        scanner.close();
    }
    /**
     * Prompts the user to input a date and parses it into a Date object.
     * Accepts multiple date formats for flexibility.
     *
     * @param scanner The Scanner object used to read user input
     * @return A parsed Date object representing the user's input
     * @throws ParseException If the input string cannot be parsed into a valid date
     *                      using the supported formats
     *
     * @see DateUtils#parseDate(String, String...)
     *
     * Example usage:
     * <pre>
     * {@code
     * // Will accept either "12/31/2023" or "12-31-2023"
     * Date payDate = getInputDate(scanner);
     * }
     * </pre>
     *
     * Supported formats:
     * - MM/dd/yyyy (e.g., 12/31/2023)
     * - MM-dd-yyyy (e.g., 12-31-2023)
     */
    private static Date getInputDate(Scanner scanner) throws ParseException {
        System.out.print("\nEnter date (MM/dd/yyyy or MM-dd-yyyy): ");
        return DateUtils.parseDate(scanner.nextLine(), "MM/dd/yyyy", "MM-dd-yyyy");
    }
    /**
     * Calculates weekly salary components for an employee including:
     * - Basic pay (1/4 of monthly salary)
     * - Overtime pay (time and a quarter)
     * - Deductions for late/underTime hours
     * - Gross weekly pay
     *
     * @param empData Employee data array containing financial information
     * @param inputDate Date used to determine the work week
     * @return SalaryData object containing all calculated salary components
     * @throws RuntimeException if financial data in empData is improperly formatted
     */
    static SalaryData calculateSalary(String[] empData, Date inputDate) {
        SalaryData data = new SalaryData();
        String weekKey = DateUtils.getWeekKey(inputDate, empData[EmployeeDetails.IDX_EMPLOYEE_ID]);
        data.isLastWeek = DateUtils.isLastWeekOfMonth(inputDate);

        try {
            data.hourlyRate = Double.parseDouble(empData[EmployeeDetails.IDX_HOURLY_RATE].replace(",", ""));
            data.basicSalary = Double.parseDouble(empData[EmployeeDetails.IDX_BASIC_SALARY].replace(",", ""));
            data.riceSubsidy = Double.parseDouble(empData[EmployeeDetails.IDX_RICE_SUBSIDY].replace(",", ""));
            data.phoneAllowance = Double.parseDouble(empData[EmployeeDetails.IDX_PHONE_ALLOWANCE].replace(",", ""));
            data.clothingAllowance = Double.parseDouble(empData[EmployeeDetails.IDX_CLOTHING_ALLOWANCE].replace(",", ""));

            data.regularHours = WeeklyWorkedHours.weeklyHours.getOrDefault(weekKey, 0.0);
            data.overtimeHours = WeeklyWorkedHours.weeklyOvertime.getOrDefault(weekKey, 0.0);
            data.underTime = WeeklyWorkedHours.weeklyUnderTime.getOrDefault(weekKey, 0.0);
            data.lateHours = WeeklyWorkedHours.weeklyLateTime.getOrDefault(weekKey, 0.0);

            // Core calculations
            data.basicPay = data.basicSalary / 4;
            data.lateDeduction = data.lateHours * data.hourlyRate;
            data.underTimeDeduction = data.underTime * data.hourlyRate;
            data.overtimePay = data.overtimeHours * data.hourlyRate * OVERTIME_RATE;
            data.grossWeekly = data.basicPay - data.lateDeduction - data.underTimeDeduction + data.overtimePay;

        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid financial data format in employee record", e);
        }
        return data;
    }

    private static void applyMonthlyDeductions(String[] empData, SalaryData data) {
        // Add monthly allowance to gross pay
        double totalAllowance = data.riceSubsidy + data.phoneAllowance + data.clothingAllowance;
        data.grossWeekly += totalAllowance / 4;

        // Calculate deductions
        data.sss = DeductionsCalculator.calculateSSS(data.basicSalary);
        data.philhealth = DeductionsCalculator.calculatePhilHealth(data.basicSalary);
        data.pagibig = DeductionsCalculator.PAG_IBIG_EMPLOYEE;

        double taxableIncome = data.basicSalary - (data.sss + data.philhealth + data.pagibig);
        data.tax = DeductionsCalculator.calculateTax(taxableIncome);

        data.totalDeductions = data.sss + data.philhealth + data.pagibig + data.tax;
        data.netPay = data.grossWeekly - data.totalDeductions;
    }
    /**
     * Applies monthly deductions and allowances to the salary calculation.
     * This method is specifically called for the last week of each month to process:
     * - Monthly allowance pro-rating
     * - Mandatory government deductions (SSS, PhilHealth, Pag-IBIG)
     * - Income tax calculation
     * - Net pay computation
     *
     * @param data SalaryData object to be modified with deductions and final net pay
     *
     * @implNote The method:
     * 1. Adds 1/4 of monthly allowances (rice, phone, clothing) to gross pay
     * 2. Calculates mandatory deductions
     * 3. Computes taxable income after deductions
     * 4. Calculates withholding tax
     * 5. Determines final net pay
     */
    private static void displayResults(SalaryData data) {
        System.out.println("\nAttendance Summary:");
        System.out.printf("%-25s: %.2f hrs%n", "Regular Hours", data.regularHours);
        System.out.printf("%-25s: %.2f hrs%n", "Over time Hours", data.overtimeHours);
        System.out.printf("%-25s: %.2f hrs%n", "Under time Hours", data.underTime);
        System.out.printf("%-25s: %.2f hrs%n", "Late Hours", data.lateHours);

        System.out.println("\nSalary Breakdown:");
        System.out.printf("%-25s: PHP %,.2f%n", "Basic Pay", data.basicPay);
        System.out.printf("%-25s: PHP %,.2f%n", "Late Deduction", data.lateDeduction);
        System.out.printf("%-25s: PHP %,.2f%n", "Under time Deduction", data.underTimeDeduction);
        System.out.printf("%-25s: PHP %,.2f%n", "Overtime Pay", data.overtimePay);

        // Only display monthly allowances during the last week payroll processing
        if (data.isLastWeek) {
            // Calculate sum of all monthly allowances
            double totalAllowance = data.riceSubsidy + data.phoneAllowance + data.clothingAllowance;
            // Format and display the total monthly allowance with:
            // - Left-aligned 25-character label
            // - Philippine Peso currency format
            // - 2 decimal places
            // - Thousands separator
            System.out.printf("%-25s: PHP %,.2f%n", "Monthly Allowance", totalAllowance);
        }

        /*
          Displays all applicable deductions for the payroll period.
          For last week of month: Shows full breakdown of government deductions and tax.
          For regular weeks: Indicates no deductions are applied.

          @param data SalaryData object containing:
         *            - isLastWeek: Flag indicating if processing last week
         *            - sss: SSS deduction amount
         *            - philhealth: PhilHealth deduction amount
         *            - pagibig: Pag-IBIG deduction amount
         *            - tax: Withholding tax amount
         */
        System.out.println("\nDeductions:");
        if (data.isLastWeek) {
            System.out.printf("%-25s: PHP %,.2f%n", "SSS", data.sss);
            System.out.printf("%-25s: PHP %,.2f%n", "PhilHealth", data.philhealth);
            System.out.printf("%-25s: PHP %,.2f%n", "Pag-ibig", data.pagibig);
            System.out.printf("%-25s: PHP %,.2f%n", "Withholding Tax", data.tax);
        } else {
            System.out.println("No deductions applied for non-last week");
        }

        System.out.println("\n" + "=".repeat(50));
        System.out.printf("%-25s: PHP %,.2f%n", "NET PAY", data.netPay);
        System.out.println("=".repeat(50));
    }

    /**
     * Represents all salary-related data for payroll processing.
     * Contains three categories of fields:
     * 1. Input values from employee records
     * 2. Calculated salary components
     * 3. Deductions and net pay information
     */
    static class SalaryData {
        // --- Input Values ---
        /** Hourly wage rate for the employee */
        double hourlyRate;
        /** Monthly base salary before deductions */
        double basicSalary;
        /** Monthly rice subsidy allowance */
        double riceSubsidy;
        /** Monthly phone allowance */
        double phoneAllowance;
        /** Monthly clothing allowance */
        double clothingAllowance;

        // --- Calculated Values ---
        /** Regular hours worked in the week */
        double regularHours;

        /** Overtime hours worked in the week */
        double overtimeHours;

        /** Total underTime hours for the week */
        double underTime;

        /** Total late hours for the week */
        double lateHours;

        /** Weekly basic pay (basicSalary / 4) */
        double basicPay;

        /** Deduction for late arrivals */
        double lateDeduction;

        /** Deduction for underTime */
        double underTimeDeduction;

        /** Overtime pay (overtimeHours * hourlyRate * 1.25) */
        double overtimePay;

        /** Gross weekly pay before deductions */
        double grossWeekly;

        // --- Deductions ---
        /** SSS (Social Security System) contribution */
        double sss;

        /** PhilHealth health insurance contribution */
        double philhealth;

        /** Pag-IBIG (HDMF) housing fund contribution */
        double pagibig;

        /** Withholding tax amount */
        double tax;

        /** Sum of all deductions */
        double totalDeductions;

        /** Final take-home pay after all deductions */
        double netPay;

        // --- Flags ---
        /** Indicates if processing the last week of the month */
        boolean isLastWeek;
    }
}