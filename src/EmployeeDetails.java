import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class EmployeeDetails {

    public static void main(String[] args) {
        EmployeeViewDetails(args);
    }

    public static void EmployeeViewDetails(String[] args) {
        String csvFile = "src/MotorPH Employee Data.csv";
        List<String[]> employees = new ArrayList<>();

        // Read CSV file
        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {
            String line;
            br.readLine(); // Skip header line
            while ((line = br.readLine()) != null) {
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
                for (int i = 0; i < data.length; i++) {
                    data[i] = data[i].replace("\"", "");
                }
                employees.add(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // Get user input
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Employee Number: ");
        String empNumber = scanner.nextLine().trim();

        // Search and display employee details
        boolean found = false;
        for (String[] emp : employees) {
            if (emp[0].equals(empNumber)) {
                displayEmployeeDetails(emp);  // Call new method
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Employee not found.");
        }

        scanner.close();
    }

    // method to display employee details
    private static void displayEmployeeDetails(String[] emp) {
        System.out.println("\nEmployee Details Found:");
        System.out.println("Employee #: " + emp[0]);
        System.out.println("Last Name: " + emp[1]);
        System.out.println("First Name: " + emp[2]);
        System.out.println("Birthday: " + emp[3]);
        System.out.println("Address: " + emp[4]);
        System.out.println("Phone Number: " + emp[5]);
        System.out.println("SSS #: " + emp[6]);
        System.out.println("Philhealth #: " + emp[7]);
        System.out.println("TIN #: " + emp[8]);
        System.out.println("Pag-ibig #: " + emp[9]);
        System.out.println("Status: " + emp[10]);
        System.out.println("Position: " + emp[11]);
        System.out.println("Immediate Supervisor: " + emp[12]);
        System.out.println("Basic Salary: " + emp[13]);
        System.out.println("Rice Subsidy: " + emp[14]);
        System.out.println("Phone Allowance: " + emp[15]);
        System.out.println("Clothing Allowance: " + emp[16]);
        System.out.println("Gross Semi-monthly Rate: " + emp[17]);
        System.out.println("Hourly Rate: " + emp[18]);
    }
}