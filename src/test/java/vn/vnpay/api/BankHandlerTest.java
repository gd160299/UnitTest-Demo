package vn.vnpay.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import vn.vnpay.model.Account;
import vn.vnpay.service.AccountService;
import vn.vnpay.service.InterestCalculator;

import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;

@RunWith(PowerMockRunner.class)
@PrepareForTest({InterestCalculator.class})
public class BankHandlerTest {

    private AccountService accountService;
    private ObjectMapper objectMapper;

    @Before
    public void setUp() {
        accountService = new AccountService();
        objectMapper = new ObjectMapper();
    }

    private EmbeddedChannel createNewChannel() {
        BankHandler handler = new BankHandler(accountService);
        return new EmbeddedChannel(handler);
    }

    private FullHttpRequest createPostRequest(String uri, String body) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.POST, uri,
                Unpooled.copiedBuffer(body, StandardCharsets.UTF_8));
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return request;
    }

    private FullHttpRequest createGetRequest(String uri) {
        FullHttpRequest request = new DefaultFullHttpRequest(
                HttpVersion.HTTP_1_1, HttpMethod.GET, uri);
        request.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json");
        return request;
    }

    @Test
    public void testOpenAccount_WithValidData_ShouldCreateNewAccount() throws Exception {
        // Arrange
        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("ownerName", "Nguyen Van A")
                .put("accountType", "SAVINGS");
        String requestBody = objectMapper.writeValueAsString(jsonNode);

        FullHttpRequest request = createPostRequest("/accounts", requestBody);
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, response.status());
        String content = response.content().toString(StandardCharsets.UTF_8);
        Account account = objectMapper.readValue(content, Account.class);
        assertNotNull(account.getAccountNumber());
        assertEquals("Nguyen Van A", account.getOwnerName());
        assertEquals("SAVINGS", account.getAccountType());
        assertEquals(0.0, account.getBalance(), 0.001);
    }

    @Test
    public void testDeposit_WithValidAmount_ShouldIncreaseBalance() throws Exception {
        // Arrange
        Account account = accountService.openAccount("Nguyen Van A", "SAVINGS");
        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("amount", 500.0);
        String requestBody = objectMapper.writeValueAsString(jsonNode);
        String uri = "/accounts/" + account.getAccountNumber() + "/deposit";

        FullHttpRequest request = createPostRequest(uri, requestBody);
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, response.status());
        assertEquals(500.0, account.getBalance(), 0.001);
    }

    @Test
    public void testWithdraw_WithSufficientBalance_ShouldDecreaseBalance() throws Exception {
        // Arrange
        Account account = accountService.openAccount("Nguyen Van A", "SAVINGS");
        accountService.deposit(account.getAccountNumber(), 500.0);
        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("amount", 200.0);
        String requestBody = objectMapper.writeValueAsString(jsonNode);
        String uri = "/accounts/" + account.getAccountNumber() + "/withdraw";

        FullHttpRequest request = createPostRequest(uri, requestBody);
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, response.status());
        assertEquals(300.0, account.getBalance(), 0.001);
    }

    @Test
    public void testTransfer_WithValidAccountsAndAmount_ShouldTransferFunds() throws Exception {
        // Arrange
        Account fromAccount = accountService.openAccount("Nguyen Van A", "SAVINGS");
        Account toAccount = accountService.openAccount("Tran Thi B", "CHECKING");
        accountService.deposit(fromAccount.getAccountNumber(), 500.0);

        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("fromAccount", fromAccount.getAccountNumber())
                .put("toAccount", toAccount.getAccountNumber())
                .put("amount", 200.0);
        String requestBody = objectMapper.writeValueAsString(jsonNode);
        String uri = "/accounts/transfer";

        FullHttpRequest request = createPostRequest(uri, requestBody);
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, response.status());
        assertEquals(300.0, fromAccount.getBalance(), 0.001);
        assertEquals(200.0, toAccount.getBalance(), 0.001);
    }

    @Test
    public void testApplyInterest_WithMockedInterestCalculator_ShouldReturnSuccess() {
        // Arrange
        Account account = accountService.openAccount("Nguyen Van A", "SAVINGS");
        accountService.deposit(account.getAccountNumber(), 1000.0);

        PowerMockito.mockStatic(InterestCalculator.class);
        PowerMockito.when(InterestCalculator.calculateMonthlyInterest(any(Account.class))).thenReturn(10.0);

        String uri = "/accounts/" + account.getAccountNumber() + "/applyInterest";
        FullHttpRequest request = createPostRequest(uri, "");

        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, response.status());
        assertEquals(1010.0, account.getBalance(), 0.001);

    }

    @Test
    public void testGetAccount_WithValidAccountNumber_ShouldReturnAccountInfo() throws Exception {
        // Arrange
        Account account = accountService.openAccount("Nguyen Van A", "SAVINGS");
        accountService.deposit(account.getAccountNumber(), 500.0);

        String uri = "/accounts/" + account.getAccountNumber();
        FullHttpRequest request = createGetRequest(uri);
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, response.status());
        String content = response.content().toString(StandardCharsets.UTF_8);
        Account fetchedAccount = objectMapper.readValue(content, Account.class);
        assertEquals(account.getAccountNumber(), fetchedAccount.getAccountNumber());
        assertEquals(account.getOwnerName(), fetchedAccount.getOwnerName());
        assertEquals(account.getAccountType(), fetchedAccount.getAccountType());
        assertEquals(account.getBalance(), fetchedAccount.getBalance(), 0.001);
    }

    @Test
    public void testRequest_ToInvalidEndpoint_ShouldReturnNotFound() {
        // Arrange
        FullHttpRequest request = createGetRequest("/invalid");
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.NOT_FOUND, response.status());
        String content = response.content().toString(StandardCharsets.UTF_8);
        assertEquals("\"API không tồn tại\"", content);
    }

    @Test
    public void testDeposit_WithNegativeAmount_ShouldReturnBadRequest() throws Exception {
        // Arrange
        Account account = accountService.openAccount("Nguyen Van A", "SAVINGS");
        JsonNode jsonNode = objectMapper.createObjectNode()
                .put("amount", -100.0);
        String requestBody = objectMapper.writeValueAsString(jsonNode);
        String uri = "/accounts/" + account.getAccountNumber() + "/deposit";

        FullHttpRequest request = createPostRequest(uri, requestBody);
        EmbeddedChannel channel = createNewChannel();

        // Act
        channel.writeInbound(request);
        FullHttpResponse response = channel.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.BAD_REQUEST, response.status());
        String content = response.content().toString(StandardCharsets.UTF_8);
        assertEquals("\"Số tiền gửi phải lớn hơn 0\"", content);
    }

    @Test
    public void testApplyInterest_WithDifferentAccountTypes_ShouldApplyCorrectInterest() {
        // Arrange
        Account savingsAccount = accountService.openAccount("Nguyen Van A", "SAVINGS");
        accountService.deposit(savingsAccount.getAccountNumber(), 1200.0);

        Account checkingAccount = accountService.openAccount("Tran Thi B", "CHECKING");
        accountService.deposit(checkingAccount.getAccountNumber(), 800.0);

        PowerMockito.mockStatic(InterestCalculator.class);
        PowerMockito.when(InterestCalculator.calculateMonthlyInterest(savingsAccount)).thenReturn(5.0); // 1200 * 0.05 / 12 = 5
        PowerMockito.when(InterestCalculator.calculateMonthlyInterest(checkingAccount)).thenReturn(0.6667); // 800 * 0.01 / 12 ≈ 0.6667

        String uriSavings = "/accounts/" + savingsAccount.getAccountNumber() + "/applyInterest";
        FullHttpRequest requestSavings = createPostRequest(uriSavings, "");
        EmbeddedChannel channelSavings = createNewChannel();
        // Act
        channelSavings.writeInbound(requestSavings);
        FullHttpResponse responseSavings = channelSavings.readOutbound();

        String uriChecking = "/accounts/" + checkingAccount.getAccountNumber() + "/applyInterest";
        FullHttpRequest requestChecking = createPostRequest(uriChecking, "");
        EmbeddedChannel channelChecking = createNewChannel();
        // Act
        channelChecking.writeInbound(requestChecking);
        FullHttpResponse responseChecking = channelChecking.readOutbound();

        // Assert
        assertEquals(HttpResponseStatus.OK, responseSavings.status());
        assertEquals(1205.0, savingsAccount.getBalance(), 0.001);

        assertEquals(HttpResponseStatus.OK, responseChecking.status());
        assertEquals(800.6667, checkingAccount.getBalance(), 0.0001);
    }
}



