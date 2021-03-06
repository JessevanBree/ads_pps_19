import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamConstants;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PPS {
    private static final int MIN_ASSIGNMENT_COUNT = 10;
    private static Random randomizer = new Random();

    private String name;                // the name of the planning system refers to its xml source file
    private int planningYear;                   // the year indicates the period of start and end dates of the projects
    private Set<Employee> employees;
    private Set<Project> projects;

    private PPS() {
        this.name = "none";
        this.planningYear = 2000;
        this.projects = new TreeSet<>();
        this.employees = new TreeSet<>();
    }

    private PPS(String resourceName, int year) {
        this();
        this.name = resourceName;
        this.planningYear = year;
    }

    /**
     * Loads a complete configuration from an XML file
     *
     * @param resourceName the XML file name to be found in the resources folder
     * @return
     */
    public static PPS importFromXML(String resourceName) {
        XMLParser xmlParser = new XMLParser(resourceName);

        try {
            xmlParser.nextTag();
            xmlParser.require(XMLStreamConstants.START_ELEMENT, null, "projectPlanning");
            int year = xmlParser.getIntegerAttributeValue(null, "year", 2000);
            xmlParser.nextTag();

            PPS pps = new PPS(resourceName, year);

            Project.importProjectsFromXML(xmlParser, pps.projects);
            Employee.importEmployeesFromXML(xmlParser, pps.employees, pps.projects);

            return pps;

        } catch (Exception ex) {
            SLF4J.logException("XML error in '" + resourceName + "'", ex);
        }

        return null;
    }

    @Override
    public String toString() {
        return String.format("PPS_e%d_p%d", this.employees.size(), this.projects.size());
    }

    /**
     * Reports the statistics of the project planning year
     */
    public void printPlanningStatistics() {
        System.out.printf("\nProject Statistics of '%s' in the year %d\n",
                this.name, this.planningYear);
        if (this.employees == null || this.projects == null ||
                this.employees.size() == 0 || this.projects.size() == 0) {
            System.out.println("No employees or projects have been set up...");
            return;
        }

        System.out.printf("%d employees have been assigned to %d projects:\n\n",
                this.employees.size(), this.projects.size());
        System.out.printf("1. The average hourly wage of all employees is %.2f\n",
                calculateAverageHourlyWage());
        System.out.printf("2. The longest project is '%s' with %d available working days\n",
                calculateLongestProject(), calculateLongestProject().getNumWorkingDays());
        System.out.printf("3. The following employees have the broadest assignment in no less than %d different projects:\n%s\n",
                MIN_ASSIGNMENT_COUNT, calculateMostInvolvedEmployees().toString());
        System.out.printf("4. The total budget of committed project manpower is %d\n",
                calculateTotalManpowerBudget());
        System.out.printf("5. Below is an overview of total managed budget by junior employees (hourly wage <= 30):\n%s\n",
                calculateManagedBudgetOverview(e -> e.getHourlyWage() <= 30).toString());
        System.out.printf("6. Below is an overview of cumulative monthly project spends:\n%s\n",
                calculateCumulativeMonthlySpends() == null ? "" : calculateCumulativeMonthlySpends().toString());
    }

    /**
     * calculates the average hourly wage of all known employees in this system
     *
     * @return
     */
    public double calculateAverageHourlyWage() {
        return employees.stream() // Stream the content of the Employee set
                .mapToDouble(Employee::getHourlyWage) // Calculate per employee what there hourly wage is
                .average() // Calculate the average wage of all the employees in this stream
                .orElse(0.0); // If there are no employees return the value 0.0
    }

    /**
     * finds the project with the highest number of available working days.
     * (if more than one project with the highest number is found, any one is returned)
     *
     * @return
     */
    public Project calculateLongestProject() {
        return projects.stream() // Stream the content of the projects set
                .max(Comparator.comparing(Project::getNumWorkingDays)) // Check each project for their number of working days and save the highest value
                .orElse(null); // If there are no projects return null
    }

    /**
     * calculates the total budget for assigned employees across all projects and employees in the system
     * based on the registration of committed hours per day per employee,
     * the number of working days in each project
     * and the hourly rate of each employee
     *
     * @return
     */
    public int calculateTotalManpowerBudget() {
        return projects.stream() // Stream the content of the projects set
                .mapToInt(Project::calculateManpowerBudget) // Get the manpower budget of each project in the stream
                .sum(); // Sum up all the manpower budgets of each project in the stream
    }

    /**
     * finds the employees that are assigned to the highest number of different projects
     * (if multiple employees are assigned to the same highest number of projects,
     * all these employees are returned in the set)
     *
     * @return
     */
    public Set<Employee> calculateMostInvolvedEmployees() {
          return employees.stream() // Stream the content of the employees set
                  .filter((employee) -> // Filter each employee in this stream
                          employee.getAssignedProjects().size() >= MIN_ASSIGNMENT_COUNT )

                            // NOTE: Allow the employee object to go through if their assigned project count is higher or equal than MIN_ASSIGNMENT_COUNT
                            // There is also another way to interpret the method filter implementation which is based on the value of the employee with the
                            // highest involvement which can be calculated by replacing MIN_ASSIGNMENT_COUNT with
                            // employees.stream().mapToInt( (e) -> e.getAssignedProjects().size() ).max().getAsInt()
                            // But we used the first solution based on the wording of the output example

                  .collect( Collectors.toCollection( () -> // Collect the filtered employees
                          new TreeSet<>( Comparator.comparing(Employee::getName) ) ) ); // Add the employees to a new treeset based on their name
    }

    /**
     * Calculates an overview of total managed budget per employee that complies with the filter predicate
     * The total managed budget of an employee is the sum of all man power budgets of all projects
     * that are being managed by this employee
     *
     * @param filter
     * @return
     */
    public Map<Employee, Integer> calculateManagedBudgetOverview(Predicate<Employee> filter) {
        return employees.stream() // Stream the content of the employees set
                .filter(filter) // Filter the employees based on the predicate property filter
                .collect(Collectors.toMap( // Add the employees that pass the filter to a new map
                        e -> e,  // The key of the map entry is the employee object
                        Employee::calculateManagedBudget)); // The value of the map entry is the managed budged of that employee
    }

    /**
     * Calculates and overview of total monthly spends across all projects in the system
     * The monthly spend of a single project is the accumulated manpower cost of all employees assigned to the
     * project across all working days in the month.
     *
     * @return
     */
    public Map<Month, Integer> calculateCumulativeMonthlySpends() {
        // TreeMap to store the total amount of monthly spends, sorted ascending by month
        Map<Month, Integer> totalMonthlySpends = new TreeMap<>();

        // Iterate over every project
        projects.forEach(p -> {
            // HashMap to store the total work days for each month
            Map<Month, Integer> workdaysPerMonth = new HashMap<>();

            // Iterate over all the working days of the project
            p.getWorkingDays().forEach(localDate ->
                // For every month, add the amount of workdays to the map
                workdaysPerMonth.merge(localDate.getMonth(), 1, Math::addExact)
            );

            // Iterate over every month with its work days
            workdaysPerMonth.forEach((key, value) ->
                // Go over the committed hours per day for the project
                p.getCommittedHoursPerDay().forEach((employee, integer) -> {
                    // For every worked hour, calculate the costs and add it to the totals map
                    totalMonthlySpends.merge(key, value * (employee.getHourlyWage() * integer), Math::addExact);
                })
            );
        });

        return totalMonthlySpends;
    }

    public String getName() {
        return name;
    }

    public Set<Project> getProjects() {
        return this.projects;
    }

    public Set<Employee> getEmployees() {
        return this.employees;
    }

    /**
     * A builder helper class to compose a small PPS using method-chaining of builder methods
     */
    public static class Builder {
        PPS pps;

        public Builder() {
            this.pps = new PPS();
        }

        /**
         * Add another employee to the PPS being build
         *
         * @param employee
         * @return
         */
        public Builder addEmployee(Employee employee) {
            pps.employees.add(employee);
            return this;
        }

        /**
         * Add another project to the PPS
         * register the specified manager as the manager of the new
         *
         * @param project
         * @param manager
         * @return
         */
        public Builder addProject(Project project, Employee manager) {
            // Check if manager code is already present
            Employee uniqueManager = manager;
            for (Employee m: pps.employees) {
                if(m.getNumber() == manager.getNumber()){
                    uniqueManager = m;
                };
            }
            if (!pps.employees.contains(uniqueManager)) addEmployee(uniqueManager);
            uniqueManager.getManagedProjects().add(project);

            pps.projects.add(project);
            return this;
        }

        /**
         * Add a commitment to work hoursPerDay on the project that is identified by projectCode
         * for the employee who is identified by employeeNr
         * This commitment is added to any other commitment that the same employee already
         * has got registered on the same project,
         *
         * @param projectCode
         * @param employeeNr
         * @param hoursPerDay
         * @return
         */
        public Builder addCommitment(String projectCode, int employeeNr, int hoursPerDay) {
            Project project = null;
            Employee employee = null;

            for (Project p : pps.projects) {
                if (p.getCode().equals(projectCode)) project = p;
            }

            for (Employee e : pps.employees) {
                if (e.getNumber() == employeeNr) employee = e;
            }

            if (project != null && employee != null) {
                project.addCommitment(employee, hoursPerDay);
            }

            return this;
        }

        /**
         * Complete the PPS being build
         *
         * @return
         */
        public PPS build() {
            return this.pps;
        }
    }
}
