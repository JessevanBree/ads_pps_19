import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.Alphanumeric.class)
class EmployeeTest {
    private Project project1, project2, project3;
    private Employee employee1, employee1a, employee2, employee3;

    @BeforeEach
    private void setup() {
        this.employee1 = new Employee(900001,20);
        this.employee1a = new Employee(900001,20);
        this.employee2 = new Employee(990002,30);
        this.employee3 = new Employee(990003,40);

        this.project1 = new Project("P1001", "TestProject-1",
                LocalDate.of(2019,2,1), LocalDate.of(2019,4,30));
        this.project2 = new Project("P2002", "TestProject-2",
                LocalDate.of(2019,4,1), LocalDate.of(2019,5,31));
        this.project3 = new Project("P3003", "TestProject-3",
                LocalDate.of(2019,3,15), LocalDate.of(2019,4,15));

        this.employee1.getManagedProjects().addAll(Set.of(this.project1, this.project2));
        this.project1.addCommitment(this.employee1,3);
        this.project1.addCommitment(this.employee2,4);
        this.project2.addCommitment(this.employee1,1);
        this.project2.addCommitment(this.employee3,8);
    }

    @Test
    void T01_checkBasics() {
        assertEquals("Mary N. PETERSON(900001)", this.employee1.toString());
        assertEquals(this.employee1, this.employee1a);
        assertEquals(this.employee1.hashCode(), this.employee1a.hashCode());
        assertTrue(this.employee1.equals(this.employee1a));
        assertTrue(this.employee1 instanceof Comparable);
    }

    @Test
    void T11_checkBudgets() {
        assertEquals((3*20+4*30)*this.project1.getNumWorkingDays()+(1*20+8*40)*this.project2.getNumWorkingDays(),
                this.employee1.calculateManagedBudget(),"managed budget");
        assertEquals(0,
                this.employee2.calculateManagedBudget(),"managed budget");
    }
}