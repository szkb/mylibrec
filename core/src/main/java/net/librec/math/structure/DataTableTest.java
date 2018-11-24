package net.librec.math.structure;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.Map;
import java.util.Set;

/**
 * @author szkb
 * @date 2018/11/13 21:20
 */
public class DataTableTest {

    public static void main(String[] args) {
        Table<String, String, String> employeeTable = HashBasedTable.create();

        //initialize the table with employee details
        employeeTable.put("IBM", "101", "Mahesh");
        employeeTable.put("IBM", "102", "Ramesh");
        employeeTable.put("IBM", "103", "Suresh");

        employeeTable.put("Microsoft", "111", "Sohan");
        employeeTable.put("Microsoft", "112", "Mohan");
        employeeTable.put("Microsoft", "113", "Rohan");

        employeeTable.put("TCS", "121", "Ram");
        employeeTable.put("TCS", "122", "Shyam");
        employeeTable.put("TCS", "123", "Sunil");

        //get Map corresponding to IBM
//        Map<String, String> ibmEmployees = employeeTable.row("IBM");
//
//        System.out.println("List of IBM Employees");
//        for (Map.Entry<String, String> entry : ibmEmployees.entrySet()) {
//            System.out.println("Emp Id: " + entry.getKey() + ", Name: " + entry.getValue());
//        }
//
//        //get all the unique keys of the table
//        Set<String> employers = employeeTable.rowKeySet();
//        System.out.print("Employers: ");
//        for (String employer : employers) {
//            System.out.print(employer + " ");
//        }
//        System.out.println();
//
//        //get a Map corresponding to 102
        Map<String, String> EmployerMap = employeeTable.column("102");
        for (Map.Entry<String, String> entry : EmployerMap.entrySet()) {
            System.out.println("Employer: " + entry.getKey() + ", Name: " + entry.getValue());
        }
    }

}
