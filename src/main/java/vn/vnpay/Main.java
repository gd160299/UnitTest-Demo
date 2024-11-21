package vn.vnpay;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;
        new BankServer(port).start();
    }
}