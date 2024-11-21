package vn.vnpay.service;

import vn.vnpay.model.Account;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AccountService {
    private final ConcurrentHashMap<String, Account> accounts = new ConcurrentHashMap<>();

    public Account openAccount(String ownerName, String accountType) {
        String accountNumber = generateAccountNumber();
        Account account = new Account(accountNumber, ownerName, accountType);
        accounts.put(accountNumber, account);
        return account;
    }

    public void deposit(String accountNumber, double amount) {
        Account account = getAccount(accountNumber);
        if (amount <= 0) throw new IllegalArgumentException("Số tiền gửi phải lớn hơn 0");
        synchronized (account) {
            account.setBalance(account.getBalance() + amount);
        }
    }

    public void withdraw(String accountNumber, double amount) {
        Account account = getAccount(accountNumber);
        if (amount <= 0) throw new IllegalArgumentException("Số tiền rút phải lớn hơn 0");
        synchronized (account) {
            if (account.getBalance() < amount) throw new IllegalArgumentException("Số dư không đủ");
            account.setBalance(account.getBalance() - amount);
        }
    }

    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        Account fromAccount = getAccount(fromAccountNumber);
        Account toAccount = getAccount(toAccountNumber);
        if (fromAccountNumber.equals(toAccountNumber))
            throw new IllegalArgumentException("Không thể chuyển tiền cho cùng một tài khoản");
        synchronized (fromAccount) {
            withdraw(fromAccountNumber, amount);
        }
        synchronized (toAccount) {
            deposit(toAccountNumber, amount);
        }
    }

    public Account getAccount(String accountNumber) {
        Account account = accounts.get(accountNumber);
        if (account == null) throw new IllegalArgumentException("Tài khoản không tồn tại");
        return account;
    }

    public void applyMonthlyInterest(String accountNumber) {
        Account account = getAccount(accountNumber);
        double interest = InterestCalculator.calculateMonthlyInterest(account);
        synchronized (account) {
            account.setBalance(account.getBalance() + interest);
        }
    }

    private String generateAccountNumber() {
        // Giả lập việc tạo số tài khoản
        return "AC" + UUID.randomUUID();
    }
}
