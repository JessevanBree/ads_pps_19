
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class PPSTest {
    Project project1, project2, project3;
    Employee employee1, employee2, employee3;
    private PPS pps;

    @BeforeEach
    void setup() {
        this.project1 = new Project("P1001", "TestProject-1",
                LocalDate.of(2019,2,1), LocalDate.of(2019,4,30));
        this.project2 = new Project("P2002", "TestProject-2",
                LocalDate.of(2019,4,1), LocalDate.of(2019,5,31));
        this.project3 = new Project("P3003", "TestProject-3",
                LocalDate.of(2019,3,15), LocalDate.of(2019,4,15));
        this.employee1 = new Employee(60006, 20);
        this.employee2 = new Employee(77007, 25);
        this.employee3 = new Employee(88808, 30);
        this.pps =
                new PPS.Builder()
                    .addEmployee(this.employee1)
                    .addEmployee(this.employee3)
                    .addProject(this.project1, this.employee1)
                    .addProject(this.project2, new Employee(60006))
                        .addProject(this.project3, this.employee2)
                        .addCommitment("P1001", 60006, 4)
                        .addCommitment("P1001", 77007, 3)
                        .addCommitment("P1001", 88808, 2)
                        .addCommitment("P2002", 88808, 3)
                        .addCommitment("P2002", 88808, 1)
                    .build();
    }

    @Test
    void T21_checkPPSBuilder() {
        assertEquals(3, this.pps.getEmployees().size(), this.pps.getEmployees().toString());
        assertEquals(3, this.pps.getProjects().size(), this.pps.getProjects().toString());
        assertEquals((4*20+3*25+2*30)*this.project1.getNumWorkingDays(), this.project1.calculateManpowerBudget());
        assertEquals((4*30)*this.project2.getNumWorkingDays(), this.project2.calculateManpowerBudget());
        assertEquals(this.project1.calculateManpowerBudget()+this.project2.calculateManpowerBudget(),
                        this.employee1.calculateManagedBudget(),"managed budget employee1");
    }

    @Test
    void T31_checkStatistics_e1_p1() {
        PPS pps = PPS.importFromXML("HvA2011_e1_p1.xml");
        pps.printPlanningStatistics();
        assertEquals(69.0, pps.calculateAverageHourlyWage(),"average hourly rate");
        assertEquals("Virtual workplaces - BPH-04(P100564)",
                pps.calculateLongestProject().toString(),"longest project");
        assertEquals(4416, pps.calculateTotalManpowerBudget(),"total manpower budget");
    }

    @Test
    void T32_checkStatistics_e2_p2() {
        PPS pps = PPS.importFromXML("HvA2012_e2_p2.xml");
        pps.printPlanningStatistics();
        assertEquals(48, pps.calculateAverageHourlyWage(),"average hourly rate");
        assertEquals("Floor insulation - MLH-02(P100752)",
                pps.calculateLongestProject().toString(),"longest project");
        assertEquals(23016, pps.calculateTotalManpowerBudget(),"total manpower budget");
    }

    @Test
    void T35_checkStatistics_e5_p5() {
        PPS pps = PPS.importFromXML("HvA2015_e5_p5.xml");
        pps.printPlanningStatistics();
        assertEquals(42.8, pps.calculateAverageHourlyWage(),"average hourly rate");
        assertEquals("Toilets refurbishment - SCP-04(P100424)",
                pps.calculateLongestProject().toString(),"longest project");
        assertEquals(225250, pps.calculateTotalManpowerBudget(),"total manpower budget");
    }
}