package vn.vnpay.service;


import org.junit.Test;
import vn.vnpay.model.Account;

import static org.junit.Assert.assertEquals;


public class InterestCalculatorTest {

    @Test
    public void testCalculateMonthlyInterest_SavingsAccount_ShouldReturnCorrectInterest() {
        Account savingsAccount = new Account("AC123456", "Nguyen Van A", "SAVINGS");
        savingsAccount.setBalance(1200.0); // 1200 * 0.05 / 12 = 5.0
        double interest = InterestCalculator.calculateMonthlyInterest(savingsAccount);
        assertEquals(5.0, interest, 0.0001);
    }

    @Test
    public void testCalculateMonthlyInterest_CheckingAccount_ShouldReturnCorrectInterest() {
        Account checkingAccount = new Account("AC654321", "Tran Thi B", "CHECKING");
        checkingAccount.setBalance(800.0); // 800 * 0.01 / 12 ≈ 0.6667
        double interest = InterestCalculator.calculateMonthlyInterest(checkingAccount);
        assertEquals(0.6667, interest, 0.0001);
    }

    @Test
    public void testCalculateMonthlyInterest_UnknownAccountType_ShouldReturnZeroInterest() {
        Account unknownAccount = new Account("AC000000", "Le Van C", "UNKNOWN");
        unknownAccount.setBalance(1000.0); // 1000 * 0.0 / 12 = 0.0
        double interest = InterestCalculator.calculateMonthlyInterest(unknownAccount);
        assertEquals(0.0, interest, 0.0001);
    }

}
