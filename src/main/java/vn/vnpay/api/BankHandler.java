package vn.vnpay.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import vn.vnpay.model.Account;
import vn.vnpay.service.AccountService;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class BankHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

    private final AccountService accountService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, RouteHandler> routeHandlers = new HashMap<>();

    public BankHandler(AccountService accountService) {
        this.accountService = accountService;
        initializeRoutes();
    }

    private void initializeRoutes() {
        routeHandlers.put("POST /accounts", (ctx, request) -> {
            try {
                handleCreateAccount(ctx, request);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        routeHandlers.put("POST /accounts/transfer", this::handleTransfer);
        routeHandlers.put("GET /accounts/{accountNumber}", this::handleGetAccount);
        routeHandlers.put("POST /accounts/{accountNumber}/deposit", this::handleDeposit);
        routeHandlers.put("POST /accounts/{accountNumber}/withdraw", this::handleWithdraw);
        routeHandlers.put("POST /accounts/{accountNumber}/applyInterest", this::handleApplyInterest);
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        HttpMethod method = request.method();

        // Xử lý URI để trích xuất các tham số
        String routeKey = method.name() + " " + normalizeUri(uri);
        RouteHandler handler = routeHandlers.get(routeKey);

        if (handler != null) {
            try {
                handler.handle(ctx, request);
            } catch (IllegalArgumentException e) {
                FullHttpResponse response = buildResponse(HttpResponseStatus.BAD_REQUEST, "\"" + e.getMessage() + "\"");
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            } catch (Exception e) {
                FullHttpResponse response = buildResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR, "\"Lỗi server\"");
                e.printStackTrace();
                ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
            }
        } else {
            FullHttpResponse response = buildResponse(HttpResponseStatus.NOT_FOUND, "\"API không tồn tại\"");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
        }
    }

    private String normalizeUri(String uri) {
        if (uri.matches("/accounts/[^/]+/deposit")) {
            return "/accounts/{accountNumber}/deposit";
        } else if (uri.matches("/accounts/[^/]+/withdraw")) {
            return "/accounts/{accountNumber}/withdraw";
        } else if (uri.matches("/accounts/[^/]+/applyInterest")) {
            return "/accounts/{accountNumber}/applyInterest";
        } else if (uri.equals("/accounts/transfer")) { // Kiểm tra exact match trước
            return "/accounts/transfer";
        } else if (uri.matches("/accounts/[^/]+")) {
            return "/accounts/{accountNumber}";
        } else {
            return uri;
        }
    }


    private void handleCreateAccount(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String body = request.content().toString(StandardCharsets.UTF_8);
        JsonNode jsonNode = objectMapper.readTree(body);
        String ownerName = jsonNode.get("ownerName").asText();
        String accountType = jsonNode.get("accountType").asText();
        Account account = accountService.openAccount(ownerName, accountType);
        String responseBody = objectMapper.writeValueAsString(account);
        FullHttpResponse response = buildResponse(HttpResponseStatus.OK, responseBody);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleTransfer(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String body = request.content().toString(StandardCharsets.UTF_8);
        JsonNode jsonNode = objectMapper.readTree(body);
        String fromAccount = jsonNode.get("fromAccount").asText();
        String toAccount = jsonNode.get("toAccount").asText();
        double amount = jsonNode.get("amount").asDouble();
        accountService.transfer(fromAccount, toAccount, amount);
        FullHttpResponse response = buildResponse(HttpResponseStatus.OK, "\"Chuyển tiền thành công\"");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleGetAccount(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        String accountNumber = uri.split("/")[2];
        Account account = accountService.getAccount(accountNumber);
        String responseBody = objectMapper.writeValueAsString(account);
        FullHttpResponse response = buildResponse(HttpResponseStatus.OK, responseBody);
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleDeposit(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        String accountNumber = uri.split("/")[2];
        String body = request.content().toString(StandardCharsets.UTF_8);
        JsonNode jsonNode = objectMapper.readTree(body);
        double amount = jsonNode.get("amount").asDouble();
        accountService.deposit(accountNumber, amount);
        FullHttpResponse response = buildResponse(HttpResponseStatus.OK, "\"Gửi tiền thành công\"");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleWithdraw(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
        String uri = request.uri();
        String accountNumber = uri.split("/")[2];
        String body = request.content().toString(StandardCharsets.UTF_8);
        JsonNode jsonNode = objectMapper.readTree(body);
        double amount = jsonNode.get("amount").asDouble();
        accountService.withdraw(accountNumber, amount);
        FullHttpResponse response = buildResponse(HttpResponseStatus.OK, "\"Rút tiền thành công\"");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private void handleApplyInterest(ChannelHandlerContext ctx, FullHttpRequest request) {
        String uri = request.uri();
        String accountNumber = uri.split("/")[2];
        accountService.applyMonthlyInterest(accountNumber);
        FullHttpResponse response = buildResponse(HttpResponseStatus.OK, "\"Áp dụng lãi suất thành công\"");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    private FullHttpResponse buildResponse(HttpResponseStatus status, String content) {
        ByteBuf contentBuffer = Unpooled.copiedBuffer(content, StandardCharsets.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(
                HttpVersion.HTTP_1_1, status, contentBuffer);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "application/json; charset=UTF-8");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, contentBuffer.readableBytes());
        return response;
    }
}
