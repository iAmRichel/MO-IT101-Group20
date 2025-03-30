
/**
 * Calculates SSS contribution using tiered rates:
 * - PHP 135 if salary < 3,250
 * - PHP 135 + (PHP 22.50 per PHP 500 increment above 3,250)
 * - Max 44 increments (salary >= 24,750)
 */
public class DeductionsCalculator {
    public static double calculateSSS(double salary) {
        if (salary < 3250) return 135.0;
        int steps = Math.min((int) ((salary - 3250) / 500) + 1, 44);
        return 135.0 + steps * 22.50;
    }

    /**
     * Calculates employee's PhilHealth contribution (1.5% of salary).
     * Employer matches an equal 1.5% (total 3% contribution).
     * @param salary Monthly salary (>= 0)
     * @return Employee's share of PhilHealth contribution
     */
    public static double calculatePhilHealth(double salary) {
        return (salary * 0.03) / 2;
    }
    /**
     * Calculates income tax using Philippine progressive tax brackets (2023).
     * Rates: 0% (<₱20,833), 20-35% in brackets with base tax amounts.
     * @param taxableIncome Annual income (₱)
     * @return Tax due (₱)
     */
    public static double calculateTax(double taxableIncome) {
        if (taxableIncome <= 20832) return 0.0;
        if (taxableIncome <= 33333) return (taxableIncome - 20833) * 0.20;
        if (taxableIncome <= 66667) return ((taxableIncome - 33333) * 0.25) + 2500;
        if (taxableIncome <= 166667) return ((taxableIncome - 66667) * 0.30) + 10833;
        if (taxableIncome <= 666667) return ((taxableIncome - 166667) * 0.32) + 40833.33;
        return ((taxableIncome - 666667) * 0.35) + 200833.33;
    }
    /**
     * Fixed monthly Pag-IBIG (HDMF) employee contribution (₱100).
     * Applies to most employees regardless of salary.
     */
    public static final double PAG_IBIG_EMPLOYEE = 100.0;
}