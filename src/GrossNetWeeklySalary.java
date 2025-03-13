import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GrossNetWeeklySalary {

    public static void main(String[] args) {
        String employeeDataFile = "src/MotorPH Employee Data.csv";
        String attendanceFile = "src/MotorPH Employee attendance record.csv";
        Map<String, String[]> employeeDetailsMap = new HashMap<>();

        // Read employee details
        try (BufferedReader br = new BufferedReader(new FileReader(employeeDataFile))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < data.length; i++) {
                    data[i] = data[i].replace("\"", "").trim();
                }
                employeeDetailsMap.put(data[0], data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter employee ID: ");
        String inputEmployeeId = scanner.nextLine().trim();

        // Check if employee exists
        String[] empDetails = employeeDetailsMap.get(inputEmployeeId);
        if (empDetails == null) {
            System.out.println("Employee not found.");
            scanner.close();
            return;
        }

        WeeklyWorkedHours.calculateHours(attendanceFile);

        // Display employee details first
        displayEmployeeDetails(empDetails);

        System.out.println("\nEmployee Weekly Salary (Note : Pick a month and point out any date within the chosen week.)");
        System.out.print("Enter date (MM/dd/yyyy): ");
        String inputDateStr = scanner.nextLine().trim();

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");
            Date inputDate = dateFormat.parse(inputDateStr);

            WeeklyWorkedHours.calculateMonthlyHours();

            // 2. Generate month key (employeeId_YYYY-MM)
            Calendar monthCal = Calendar.getInstance();
            monthCal.setTime(inputDate);
            int year = monthCal.get(Calendar.YEAR);
            int month = monthCal.get(Calendar.MONTH) + 1;
            String monthKey = String.format("%s_%04d-%02d", inputEmployeeId, year, month);

            // 3. Get monthly hours from maps
            Double monthlyRegular = WeeklyWorkedHours.monthlyHoursMap.get(monthKey);
            Double monthlyOvertime = WeeklyWorkedHours.monthlyOvertimeMap.get(monthKey);
            Double monthlyUnderTime = WeeklyWorkedHours.monthlyUnderTimeMap.get(monthKey);
            Double monthlyLateTime = WeeklyWorkedHours.monthlyLatetimeMap.get(monthKey);

            Calendar cal = Calendar.getInstance();
            cal.setTime(inputDate);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
            String weekKey = inputEmployeeId + "_" + dateFormat.format(cal.getTime());

            Double regularHours = WeeklyWorkedHours.weeklyHoursMap.get(weekKey);
            Double overtimeHours = WeeklyWorkedHours.weeklyOvertimeMap.get(weekKey);
            Double weeklyUnderTime = WeeklyWorkedHours.weeklyUndertimeMap.get(weekKey);
            Double weeklyLateTime = WeeklyWorkedHours.weeklyLatetimeMap.get(weekKey);

            // Get financial data from employee details
            double hourlyRate = Double.parseDouble(empDetails[empDetails.length - 1].replace(",", ""));
            double basicSalary = Double.parseDouble(empDetails[empDetails.length - 6].replace(",", ""));
            double riceSubsidy = Double.parseDouble(empDetails[14].replace(",", ""));
            double phoneAllowance = Double.parseDouble(empDetails[15].replace(",", ""));
            double clothingAllowance = Double.parseDouble(empDetails[16].replace(",", ""));

            // Calculate SSS contribution based on monthly basic salary
            double sssContribution = calculateSSSContribution(basicSalary);
            double taxRate= calculateTaxRate(basicSalary);

            // Monthly allowance
            double totalAllowance = (riceSubsidy + phoneAllowance + clothingAllowance);

            // government deduction
            double philHealth =  (basicSalary * 0.03) /2 ;
            double pagIbig = 100.00;


            if (regularHours == null || overtimeHours == null) {
                System.out.println("No attendance data found for employee " + inputEmployeeId);
                System.out.println("Or You entered invalid Date Format " + inputEmployeeId);
            } else {

                double OVERTIME_RATE_MULTIPLIER = 1.25;
                double underTime = weeklyUnderTime * hourlyRate;
                double basicPay = (basicSalary / 4);
                double late = weeklyLateTime * hourlyRate;
                double overtimePay = overtimeHours * hourlyRate * OVERTIME_RATE_MULTIPLIER;
                double monthlyLatePay = monthlyLateTime * hourlyRate;
                double monthlyUnderTimePay = monthlyUnderTime * hourlyRate;
                double monthlyOverTimePay = monthlyOvertime * hourlyRate;
                double grossMonthlyIncome = basicSalary + totalAllowance + overtimePay;
                double monthlyAllowance = (riceSubsidy + phoneAllowance + clothingAllowance);
                double grossWeekly = ((basicPay - late - underTime) + overtimePay);
                double totalWeeklySalary = grossWeekly - sssContribution - philHealth - pagIbig;
                double grossMonthly = ((basicPay - late - underTime) + overtimePay + totalAllowance);
                double totalMandatoryDeductions = sssContribution + philHealth + pagIbig;
                double totalMonthlyDeductions = grossMonthly - totalMandatoryDeductions;

                boolean isLastWeekOfMonth = isLastWeekOfMonth(inputDate);
                double philhealth = 0.0;
                sssContribution = 0.0;
                double pagibigEmployee = 0.0;
                double totalDeductions = 0.0;
                double monthlyTaxableIncome = 0.0;
                taxRate = 0.0;
                double totalPay = 0.0;


                if (isLastWeekOfMonth) {
                    pagibigEmployee = 100.00;
                    sssContribution = calculateSSSContribution(basicSalary);
                    philhealth = (basicSalary * 0.03) / 2;
                    totalDeductions = (monthlyLatePay + monthlyUnderTimePay + sssContribution + philhealth + pagibigEmployee);
                    monthlyTaxableIncome = grossMonthlyIncome - totalDeductions;
                    taxRate = calculateTaxRate(monthlyTaxableIncome);
                    totalPay = totalMonthlyDeductions - calculateTaxRate(monthlyTaxableIncome);
                }

                /* 4. Print monthly hours
                if (monthlyRegular != null && monthlyOvertime != null && monthlyUnderTime != null) {
                    System.out.println("\nMonthly Worked Hours (Aggregated):");
                    System.out.printf("Regular Hours: %.2f hrs%n", monthlyRegular);
                    System.out.printf("Overtime Hours: %.2f hrs%n", monthlyOvertime);
                    System.out.printf("Under Time Hours: %.2f hrs%n", monthlyUnderTime);
                    System.out.printf("Late Time Hours: %.2f hrs%n", monthlyLateTime);
                }
                 */

                // Actual weekly Hours
                System.out.printf("%-20s: %.2f hrs%n", "Actual Worked Hours", regularHours);
                System.out.printf("%-20s: %.2f hrs%n", "Actual Overtime", overtimeHours);
                System.out.printf("%-20s: %.2f hrs%n", "Actual Under Time", weeklyUnderTime);
                System.out.printf("%-20s: %.2f hrs%n", "Actual Late Time", weeklyLateTime);

                System.out.printf("\nTotal salary for employee %s (Week of %s):%n", inputEmployeeId, dateFormat.format(cal.getTime()));
                System.out.printf("%-20s: PHP %,.2f%n", "+Basic Pay", basicPay);
                System.out.printf("%-20s: PHP %,.2f%n", "-Late", late);
                System.out.printf("%-20s: PHP %,.2f%n", "-Under time", underTime);
                System.out.printf("%-20s: PHP %,.2f%n", "+Over Time", overtimePay);

                if (isLastWeekOfMonth) {

                    System.out.printf("%-20s: PHP %,.2f%n", "+Monthly Allowance", monthlyAllowance);

                } else {

                    System.out.printf("%-20s: PHP %,.2f%n", "+Monthly Allowance",0.0);
                }





                System.out.println("\nDeductions:");
                System.out.printf("%-28s: PHP %,.2f%n", "- SSS Contribution", sssContribution);
                System.out.printf("%-28s: PHP %,.2f%n", "- PhilHealth Contribution", philhealth);
                System.out.printf("%-28s: PHP %,.2f%n", "- Pag-ibig Contribution",  pagibigEmployee);
                System.out.printf("%-28s: PHP %,.2f%n", "- Withholding Tax",  taxRate);
                System.out.println("-".repeat(44));


                if (isLastWeekOfMonth) {

                    System.out.printf("\n%-28s: PHP %,.2f%n", "Take Home Pay",  totalPay);
                    System.out.println("═".repeat(44));

                } else {


                    System.out.printf("\nTake Home Pay             : PHP %,.2f%n", grossWeekly);
                    System.out.println("═".repeat(44));
                }


                if (isLastWeekOfMonth) {
                     /* Computation for gross monthly income, taxable income , monthly deduction, withholding tax.

                    System.out.printf("\n+ Monthly Basic salary    : PHP %,.2f%n", basicSalary);
                    System.out.printf("+ Monthly Over Time       : PHP %,.2f%n", monthlyOverTimePay);
                    System.out.printf("+ Monthly Allowance       : PHP %,.2f%n", totalAllowance);
                    System.out.println("-".repeat(44));
                    System.out.printf("  Gross Monthly Income    : PHP %,.2f%n", grossMonthlyIncome);
                    System.out.println("\nDeductions (Last Week of the Month):");
                    System.out.printf("- Monthly Late            : PHP %,.2f%n", monthlyLatePay);
                    System.out.printf("- Monthly Under Time      : PHP %,.2f%n", monthlyUnderTimePay);
                    System.out.printf("- SSS Contribution        : PHP %,.2f%n", sssContribution);
                    System.out.printf("- PhilHealth Contribution : PHP %,.2f%n", philhealth);
                    System.out.printf("- Pag-ibig Contribution   : PHP %,.2f%n", pagibigEmployee);
                    System.out.println("-".repeat(44));
                    System.out.printf("Total Deductions          : PHP %,.2f%n", totalDeductions);
                    System.out.printf("Monthly Taxable Income    : PHP %,.2f%n", monthlyTaxableIncome);
                    System.out.printf("Withholding Tax           : PHP %,.2f%n", taxRate);

                      */


                } else {
                    System.out.println("\nNo deductions applied (Not the last week of the month).");
                }


            }
        } catch (ParseException | NumberFormatException e) {
            System.out.println(e instanceof ParseException
                    ? "Invalid date format. Use MM/dd/yyyy."
                    : "Error parsing financial data");
        }

        scanner.close();
    }

    private static void displayEmployeeDetails(String[] emp) {
        System.out.println("\nEmployee Details:");
        System.out.printf("%-30s: %s%n", "Employee #", emp[0]);
        System.out.printf("%-30s: %s%n", "Last Name", emp[1]);
        System.out.printf("%-30s: %s%n", "First Name", emp[2]);
        System.out.printf("%-30s: %s%n", "Birthday", emp[3]);
        System.out.printf("%-30s: %s%n", "Address", emp[4]);
        System.out.printf("%-30s: %s%n", "Phone Number", emp[5]);
        System.out.printf("%-30s: %s%n", "SSS #", emp[6]);
        System.out.printf("%-30s: %s%n", "Philhealth #", emp[7]);
        System.out.printf("%-30s: %s%n", "TIN #", emp[8]);
        System.out.printf("%-30s: %s%n", "Pag-ibig #", emp[9]);
        System.out.printf("%-30s: %s%n", "Status", emp[10]);
        System.out.printf("%-30s: %s%n", "Position", emp[11]);
        System.out.printf("%-30s: %s%n", "Immediate Supervisor", emp[12]);
        System.out.printf("%-30s: %s%n", "Basic Salary", emp[13]);
        System.out.printf("%-30s: %s%n", "Rice Subsidy", emp[14]);
        System.out.printf("%-30s: %s%n", "Phone Allowance", emp[15]);
        System.out.printf("%-30s: %s%n", "Clothing Allowance", emp[16]);
        System.out.printf("%-30s: %s%n", "Gross Semi-monthly Rate", emp[17]);
        System.out.printf("%-30s: %s%n", "Hourly Rate", emp[18]);
        System.out.println("______________________");
    }

    private static boolean isLastWeekOfMonth(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        int lastDayOfMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        return currentDay > (lastDayOfMonth - 7);
    }

    public static double calculateSSSContribution(double totalSalary) {
        if (totalSalary < 3250) return 135.0;
        int steps = Math.min((int) ((totalSalary - 3250) / 500) + 1, 44);
        return 135.0 + steps * 22.50;
    }

    public static double calculateTaxRate(double grossSalary) {
        if (grossSalary <= 20832) {
            return 0.0; // No tax for salaries below or equal to 20,832
        } else if (grossSalary > 20833 && grossSalary <= 33333) {
            return (grossSalary - 20833) * 0.20; // 20% tax for this bracket
        } else if (grossSalary > 33333 && grossSalary <= 66667) {
            return ((grossSalary - 33333) * 0.25) + 2500; // 25% tax for this bracket + fixed amount
        } else if (grossSalary > 66667 && grossSalary <= 166667) {
            return ((grossSalary- 66667) * 0.30) + 10833; // 30% tax for this bracket + fixed amount
        } else if (grossSalary > 166667 && grossSalary <= 666667) {
            return ((grossSalary - 166667) * 0.32) + 40833.33; // 32% tax for this bracket + fixed amount
        } else if (grossSalary > 666667) {
            return ((grossSalary - 666667) * 0.35) + 200833.33; // 35% tax for this bracket + fixed amount
        } else {
            return 0.0; // Default case (should not be reached)
        }
    }


}

