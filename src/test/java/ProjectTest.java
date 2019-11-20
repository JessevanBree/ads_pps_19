import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class ProjectTest {
    private Project project1, project1a, project2, project3;
    private Employee employee1, employee2, employee3;

    @BeforeEach
    private void setup() {
        this.project1 = new Project("P1001", "TestProject-1",
                LocalDate.of(2019,2,1), LocalDate.of(2019,4,30));
        this.project1a = new Project("P100" + 1, "TestProject-1",
                LocalDate.of(2019,2,1), LocalDate.of(2019,4,30));
        this.project2 = new Project("P2002", "TestProject-2",
                LocalDate.of(2019,4,1), LocalDate.of(2019,5,31));
        this.project3 = new Project("P3003", "TestProject-3",
                LocalDate.of(2019,3,15), LocalDate.of(2019,4,15));

        this.employee1 = new Employee(900001,20);
        this.employee2 = new Employee(990002,30);
        this.employee3 = new Employee(990003,40);

        this.project1.addCommitment(this.employee1,3);
        this.project1.addCommitment(this.employee2,4);
        this.project2.addCommitment(this.employee1,1);
        this.project2.addCommitment(this.employee3,8);
    }

    @Test
    void T01_checkBasics() {
        assertEquals("TestProject-1(P1001)", this.project1a.toString());
        assertEquals(this.project1, this.project1a);
        assertEquals(this.project1.hashCode(), this.project1a.hashCode());
        assertTrue(this.project1.equals(this.project1a));
        assertTrue(this.project1 instanceof Comparable);
    }

    @Test
    void T11_checkBudgets() {
        assertEquals((3*20+4*30)*this.project1.getNumWorkingDays(),
                this.project1.calculateManpowerBudget(),"manpower budget project1");
        assertEquals((1*20+8*40)*this.project2.getNumWorkingDays(),
                this.project2.calculateManpowerBudget(),"manpower budget project2");
        assertEquals(0,
                this.project3.calculateManpowerBudget(),"manpower budget project3");
    }
}