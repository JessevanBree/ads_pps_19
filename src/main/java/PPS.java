import utils.SLF4J;
import utils.XMLParser;

import javax.xml.stream.XMLStreamConstants;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class PPS {

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

        System.out.printf("1. The average hourly wage of all employees is %f\n", calculateAverageHourlyWage());
        System.out.printf("2. The longest project is '%s' with %d available working days\n", calculateLongestProject(), calculateLongestProject().getNumWorkingDays());
        System.out.printf("3. The following employees have the broadest assignment in no less than %d different projects:\n%s\n", 10, calculateMostInvolvedEmployees().toString());
        System.out.printf("4. The total budget of committed project manpower is %d\n", calculateTotalManpowerBudget());
        System.out.printf("5. Below is an overview of total managed budget by junior employees (hourly wage <= 30):\n%s\n", calculateManagedBudgetOverview(e -> e.getHourlyWage() <= 30).toString());
        System.out.printf("6. Below is an overview of cumulative monthly project spends:\n%s", calculateCumulativeMonthlySpends() == null ? "" : calculateCumulativeMonthlySpends().toString());
    }

    /**
     * calculates the average hourly wage of all known employees in this system
     *
     * @return
     */
    public double calculateAverageHourlyWage() {
        return employees.stream().mapToDouble(Employee::getHourlyWage).average().orElse(0.0);
    }

    /**
     * finds the project with the highest number of available working days.
     * (if more than one project with the highest number is found, any one is returned)
     *
     * @return
     */
    public Project calculateLongestProject() {
        return projects.stream().max(Comparator.comparing(Project::getNumWorkingDays)).orElse(null);
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
        return projects.stream()
                .mapToInt(Project::calculateManpowerBudget)
                .sum();
    }

    /**
     * finds the employees that are assigned to the highest number of different projects
     * (if multiple employees are assigned to the same highest number of projects,
     * all these employees are returned in the set)
     *
     * @return
     */
    public Set<Employee> calculateMostInvolvedEmployees() {
        // 2 Versions possible
            // 1 With hardcoded limit on minimum assigned projects
          return employees.stream().filter(
          (employee) -> employee.getAssignedProjects().size() >= 10 )
          .collect( Collectors.toCollection( () -> new TreeSet<>( Comparator.comparing(Employee::getName) ) ) );

        // 2 based on the employee with the highest number of assigned projects
//        return employees.stream().filter(
//                (employee) -> employee.getAssignedProjects().size()
//                        >= employees.stream().mapToInt( (e) -> e.getAssignedProjects().size() ).max().getAsInt() )
//                .collect( Collectors.toCollection( () -> new TreeSet<>( Comparator.comparing(Employee::getName) ) ) );
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
        return employees.stream()
                .filter(filter)
                .collect(Collectors.toMap(e -> e,  Employee::calculateManagedBudget));
    }

    /**
     * Calculates and overview of total monthly spends across all projects in the system
     * The monthly spend of a single project is the accumulated manpower cost of all employees assigned to the
     * project across all working days in the month.
     *
     * @return
     */
    public Map<Month, Integer> calculateCumulativeMonthlySpends() {
        // TODO J
//        LocalDate d = LocalDate.now().getMonthValue();
//        return projects.stream().collect(HashMap::new, (m, p) ->
//                Arrays.stream(calculateMonths(p.getStartDate(), p.getEndDate()))
//                        .map((mo) -> m.put(mo, p.getWorkingDays()
//                                .stream().mapToInt(i -> i.getDayOfYear()).sum())), HashMap::putAll);

//        return Arrays.stream(Month.values()).collect(HashMap::new, (map, m) -> map.put(m, 0), HashMap::putAll).merge();
        return new HashMap<>();
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
