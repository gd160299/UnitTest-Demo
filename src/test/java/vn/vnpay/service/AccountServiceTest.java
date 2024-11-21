package vn.vnpay.service;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import vn.vnpay.model.Account;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InterestCalculator.class})
public class AccountServiceTest {

    private AccountService accountService;
    private Account account;

    @Before
    public void setUp() {
        accountService = new AccountService();
        account = accountService.openAccount("Nguyen Van A", "SAVINGS");
    }

    @Test
    public void testOpenAccount_WithValidOwnerName_ShouldCreateNewAccount() {
        assertNotNull(account);
        assertNotNull(account.getAccountNumber());
        assertEquals("Nguyen Van A", account.getOwnerName());
        assertEquals("SAVINGS", account.getAccountType());
        assertEquals(0.0, account.getBalance(), 0.001);
    }

    @Test
    public void testDeposit_WithValidAmount_ShouldIncreaseBalance() {
        accountService.deposit(account.getAccountNumber(), 500.0);
        assertEquals(500.0, account.getBalance(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testDeposit_WithNegativeAmount_ShouldThrowException() {
        accountService.deposit(account.getAccountNumber(), -100.0);
    }

    @Test
    public void testWithdraw_WithSufficientBalance_ShouldDecreaseBalance() {
        accountService.deposit(account.getAccountNumber(), 500.0);
        accountService.withdraw(account.getAccountNumber(), 200.0);
        assertEquals(300.0, account.getBalance(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithdraw_WithInsufficientBalance_ShouldThrowException() {
        accountService.withdraw(account.getAccountNumber(), 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWithdraw_WithZeroOrNegativeAmount_ShouldThrowException() {
        accountService.withdraw(account.getAccountNumber(), 0);
    }

    @Test
    public void testTransfer_WithValidAccountsAndAmount_ShouldTransferFunds() {
        Account toAccount = accountService.openAccount("Tran Thi B", "CHECKING");
        accountService.deposit(account.getAccountNumber(), 500.0);

        accountService.transfer(account.getAccountNumber(), toAccount.getAccountNumber(), 200.0);

        assertEquals(300.0, account.getBalance(), 0.001);
        assertEquals(200.0, toAccount.getBalance(), 0.001);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransfer_WithInsufficientBalance_ShouldThrowException() {
        Account toAccount = accountService.openAccount("Tran Thi B", "SAVINGS");

        accountService.transfer(account.getAccountNumber(), toAccount.getAccountNumber(), 100.0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTransfer_ToSameAccount_ShouldThrowException() {
        accountService.deposit(account.getAccountNumber(), 500.0);

        accountService.transfer(account.getAccountNumber(), account.getAccountNumber(), 100.0);
    }

    @Test
    public void testGetAccount_WithValidAccountNumber_ShouldReturnAccountInfo() {
        Account fetchedAccount = accountService.getAccount(account.getAccountNumber());
        assertEquals(account.getAccountNumber(), fetchedAccount.getAccountNumber());
        assertEquals(account.getOwnerName(), fetchedAccount.getOwnerName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetAccount_WithInvalidAccountNumber_ShouldThrowException() {
        accountService.getAccount("NonExistentAccount");
    }

    @Test
    public void testApplyMonthlyInterest_WithMockedInterestCalculator_ShouldIncreaseBalance() {
        accountService.deposit(account.getAccountNumber(), 1000.0);

        PowerMockito.mockStatic(InterestCalculator.class);
        PowerMockito.when(InterestCalculator.calculateMonthlyInterest(Mockito.any(Account.class))).thenReturn(10.0);
        accountService.applyMonthlyInterest(account.getAccountNumber());

        assertEquals(1010.0, account.getBalance(), 0.001);
        PowerMockito.verifyStatic(InterestCalculator.class);
        InterestCalculator.calculateMonthlyInterest(account);
    }
}