import utils.Names;
import utils.XMLParser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

public class Employee implements Comparable<Employee> {
    public static final int MAX_JUNIOR_WAGE = 30;
    public static final int MAX_WAGE = 75;

    private static Random randomizer = new Random();
    private static int lastNumber = 100000;

    private int number;                     // unique identifier of an Employee
    private String name;
    private int hourlyWage;                 // hourly charge rate of an Employee
                                            // used to calculate cost of project manpower
    private Set<Project> managedProjects;   // the projects that are managed by this employee
    private Set<Project> assignedProjects;  // the projects that this employee is working on
                                            // (the project manager is also assigned to his/her project)

    public Employee(int number) {
        this.number = number;
        this.name = Names.nextFullNameWithMI(number);
        lastNumber = Math.max(number, lastNumber);
        this.hourlyWage = 16 + randomizer.nextInt(MAX_WAGE -15);
        this.managedProjects = new HashSet<>();
        this.assignedProjects = new HashSet<>();
    }

    public Employee(int number, int hourlyWage) {
        this(number);
        this.hourlyWage = hourlyWage;
    }

    public Employee(int number, String name, int hourlyWage) {
        this(number, hourlyWage);
        this.name = name;
    }

    public Employee() {
        this(lastNumber + 1 + randomizer.nextInt(8));
    }

    @Override
    public int compareTo(Employee o) {
        return this.getName().compareTo(o.getName());
//        return this.number - o.number;
    }

    //  make sure Employees can be added to a HashMap, HashSet
    //  every employee shall have a unique number
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Employee employee = (Employee) o;
        return number == employee.number &&
                name.equals(employee.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, number);
    }

    // make sure Employees can be printed. The format is 'name(number)'
    @Override
    public String toString() {
        return name + "(" + getNumber() + ")";
    }

    /**
     * Calculates the total budget of all committed manpower
     * across all projects that this employee is managing
     * this is a.k.a. the total budget responsibility of the employee
     * @return
     */
    public int calculateManagedBudget() {
        return managedProjects.stream().mapToInt(Project::calculateManpowerBudget).sum();
    }

    public int getNumber() {
        return number;
    }

    public String getName() {
        return name;
    }

    public int getHourlyWage() {
        return hourlyWage;
    }

    public Set<Project> getManagedProjects() {
        return managedProjects;
    }

    public Set<Project> getAssignedProjects() {
        return assignedProjects;
    }

    // Below are helper attributes and methods for sample generation
    // and XML import and export

    public static Set<Employee> importEmployeesFromXML(XMLParser xmlParser, Set<Employee> employees,
                    Set<Project> projects) throws XMLStreamException {
        if (xmlParser.nextBeginTag("employees")) {
            xmlParser.nextTag();
            if (employees != null) {
                Employee employee;
                while ((employee = importFromXML(xmlParser, projects)) != null) {
                    employees.add(employee);
                }
            }
            xmlParser.findAndAcceptEndTag("employees");
        }
        return employees;
    }

    public static Employee importFromXML(XMLParser xmlParser, Set<Project> projects) throws XMLStreamException {
        if (xmlParser.nextBeginTag("employee")) {
            int number = xmlParser.getIntegerAttributeValue(null, "number", 0);
            xmlParser.nextTag();

            String name = "";
            if (xmlParser.nextBeginTag("name")) {
                name = xmlParser.getElementText();
                xmlParser.findAndAcceptEndTag("name");
            }

            int hourlyRate = 1;
            if (xmlParser.nextBeginTag("hourlyWage")) {
                hourlyRate = Integer.valueOf(xmlParser.getElementText());
                xmlParser.findAndAcceptEndTag("hourlyWage");
            }

            Employee employee = new Employee(number, name, hourlyRate);

            if (xmlParser.nextBeginTag("managedProjects")) {
                xmlParser.nextTag();
                Project project;
                while ((project = Project.importReferenceFromXML(xmlParser, projects)) != null) {
                    employee.managedProjects.add(project);

                    // replace the placeholder references in the project, if any
                    project.updateReferences(employee);
                }
                xmlParser.findAndAcceptEndTag("managedProjects");
            }
            if (xmlParser.nextBeginTag("allocatedProjects")) {
                xmlParser.nextTag();
                Project project;
                while ((project = Project.importReferenceFromXML(xmlParser, projects)) != null) {
                    employee.assignedProjects.add(project);

                    // replace the placeholder references in the project, if any
                    project.updateReferences(employee);
                }
                xmlParser.findAndAcceptEndTag("allocatedProjects");
            }

            xmlParser.findAndAcceptEndTag("employee");
            return employee;
        }
        return null;
    }

    public void updateReferences(Project project) {
        // replace the employee's references to project
        if (this.managedProjects.removeIf(p -> p.equals(project))) {
            this.managedProjects.add(project);
        }
        if (this.assignedProjects.removeIf(p -> p.equals(project))) {
            this.assignedProjects.add(project);
        }
    }
}
