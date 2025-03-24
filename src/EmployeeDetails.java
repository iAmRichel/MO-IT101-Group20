import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
* EmployeeDetails class is responsible for reading employee details from a CSV file
* and allowing the user to search for an employee by their Employee Number.
*/
public class EmployeeDetails {

    public static void main(String[] args) {
        EmployeeViewDetails(args); //Calls the EmployeeViewDetails method to handle employee details retrieval
    }
    
    public static void EmployeeViewDetails(String[] args) {
        //Define file path for the employee details CSV file
        String employeeDetailsFile = "src/MotorPH Employee Data.csv"; 
        List<String[]> employees = new ArrayList<>();

        // Read CSV file and store data in a list
        try (BufferedReader br = new BufferedReader(new FileReader(employeeDetailsFile))) {
            String line;
            br.readLine(); // Skip header line
            
            // Read each line and parse data
            while ((line = br.readLine()) != null) {
                //Split CSV line, handling commas inside quoted values
                String[] data = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);

                // Remove unnecessary quotes from data
                for (int i = 0; i < data.length; i++) {
                    data[i] = data[i].replace("\"", "");
                }
                // Add processed employee data to the list
                employees.add(data);
            }
        } catch (IOException e) {
            //Print error if file cannot be read
            e.printStackTrace();
            return;
        }

        // Prompt user for Employee Number input
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter Employee Number: ");
        String empNumber = scanner.nextLine().trim();

        // Search for the employee in the list
        boolean found = false;
        for (String[] emp : employees) {
            if (emp[0].equals(empNumber)) { //Compare with Employee Number
                displayEmployeeDetails(emp);  // Call method to display details
                found = true;
                break;
            }
        }
        //Display message if employee is not found
        if (!found) {
            System.out.println("Employee not found.");
        }

        //Close scanner to prevent resource leak
        scanner.close();
    }

    /**
    * Displays the details of an employee in a formatted manner.
    *
    * @param emp Array containing employee details extracted from the CSV file
    */
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
