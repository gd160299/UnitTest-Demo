package vn.vnpay.service;

import vn.vnpay.model.Account;

public class InterestCalculator {
    public static double calculateMonthlyInterest(Account account) {
        double interestRate = getInterestRate(account.getAccountType());
        return account.getBalance() * interestRate / 12;
    }

    private static double getInterestRate(String accountType) {
        switch (accountType) {
            case "SAVINGS":
                return 0.05; // 5% mỗi năm
            case "CHECKING":
                return 0.01; // 1% mỗi năm
            default:
                return 0.0;
        }
    }
}
