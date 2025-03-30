import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class DeductionsCalculatorTest {

    @Test
    void calculateSSS_MinValue() {
        assertEquals(135.0, DeductionsCalculator.calculateSSS(3000));
    }

    @Test
    void calculatePhilHealth() {
        assertEquals((30000 * 0.03) / 2, DeductionsCalculator.calculatePhilHealth(30000));
    }

    @Test
    void calculateTax_LowBracket() {
        assertEquals(0.0, DeductionsCalculator.calculateTax(20000));
    }

    @Test
    void calculateTax_HighBracket() {
        double tax = DeductionsCalculator.calculateTax(700000);
        assertTrue(tax > 200000);
    }

    @Test
    void pagIbigConstant() {
        assertEquals(100.0, DeductionsCalculator.PAG_IBIG_EMPLOYEE);
    }
}