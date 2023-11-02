package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.common.enums.SortDirection;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.common.enums.Exchange;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.quote.LatestStockQuoteResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.quote.LatestCryptoQuoteResponse;
import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.CurrentOrderStatus;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.positions.Position;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import java.util.AbstractMap.SimpleEntry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderManager {
    private final AlpacaAPI alpacaAPI;
    private final TextAreaHolder orderManagerTattler;
    private final Path TRADELOG_FILENAME;
    private final Map<String, Double> tradeHistory;

    public OrderManager(TextAreaHolder orderManagerTattler, Path path) {
        alpacaAPI = new AlpacaAPI();
        this.orderManagerTattler = orderManagerTattler;
        this.TRADELOG_FILENAME = path;
        tradeHistory = new HashMap<>();
    }

    public LatestStockQuoteResponse getStockBidAsk(String ticker) {
        LatestStockQuoteResponse quote = null;
        try {
            quote = alpacaAPI.stockMarketData().getLatestQuote(ticker);
            System.out.println(ticker + "  Bid: " + quote.getQuote().getBidPrice() + "  Ask: " + quote.getQuote().getAskPrice());
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }

        return quote;
    }

    public LatestCryptoQuoteResponse getCryptoBidAsk(String ticker) {
        LatestCryptoQuoteResponse quote = null;
        try {
            quote = alpacaAPI.cryptoMarketData().getLatestQuote(ticker, Exchange.fromValue(""));
            System.out.println(ticker + "  Bid: " + quote.getQuote().getBidPrice() + "  Ask: " + quote.getQuote().getAskPrice());
        } catch (AlpacaClientException e) {
            throw new RuntimeException(e);
        }

        return quote;
    }

    public SimpleEntry<Double, Double> getQuote(String security) {
        Double bid = 0.0d;
        Double ask = 0.0d;

        try {
            if (security.length() > 5) { // todo this seems to be good enough
                LatestCryptoQuoteResponse cryptoQuoteResponse = getCryptoBidAsk(security);
                bid = cryptoQuoteResponse.getQuote().getBidPrice();
                ask = cryptoQuoteResponse.getQuote().getAskPrice();
            } else {
                LatestStockQuoteResponse stockQuoteResponse = getStockBidAsk(security);
                bid = stockQuoteResponse.getQuote().getBidPrice();
                ask = stockQuoteResponse.getQuote().getAskPrice();
            }
        } catch (Exception e) {
        }

        return new SimpleEntry<>(bid, ask);
    }

    public Order placeLimitOrder(TradeSignal trade, double close, double low, double high) throws AlpacaClientException, IOException {
        Order tmpOrder = null;
        String msg = " limit order=" + trade.getSecurity()+
                        " close="+close+
                        " quantity="+trade.getQuantity()+
                        " orderSide="+trade.getOrderSide()+
                        " HoldTime="+trade.getHoldTime()+
                        " timeInForce="+trade.getTimeInForce()+
                        " limitPrice="+trade.getLimitPrice()+
                        " rationale="+trade.getRationale();

        Double bid = 0.0d;
        Double ask = 0.0d;
        try {
            SimpleEntry<Double, Double> q = getQuote(trade.getSecurity());
            bid = q.getKey();
            ask = q.getValue();
        } catch (Exception e) {
        }

        tmpOrder = alpacaAPI.orders().requestLimitOrder(trade.getSecurity(), trade.getQuantity(), trade.getOrderSide(), trade.getTimeInForce(), trade.getLimitPrice(), true);
        msg = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()) +
                "\topen\tsymbol=" + trade.getSecurity() +
                "\tpattern=" + trade.getRationale() +
                "\tside=" + trade.getOrderSide() +
                "\tclose=" + close +
                "\tbid=" + bid +
                "\task=" + ask +
                "\tlow=" + low +
                "\thigh=" + high;

        Files.write(TRADELOG_FILENAME, Arrays.asList(msg), StandardCharsets.UTF_8,
                Files.exists(TRADELOG_FILENAME) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

        orderManagerTattler.appendTrunc(msg);

        if (trade.getOrderSide() == OrderSide.BUY) {
            tradeHistory.put(trade.getSecurity(), close);
        } else {
            tradeHistory.put(trade.getSecurity(), -close);
        }

        return tmpOrder;
    }

    public Order flattenPosition (String security, double close, double low, double high) throws AlpacaClientException {
        Order ordTmp;
        try {
            ordTmp = alpacaAPI.positions().close(security, null, 100d);

            Double closeAtTradeStart = null;
            if (tradeHistory.get(security) != null) {
                closeAtTradeStart = tradeHistory.get(security);
            }

            Double bid = 0.0d;
            Double ask = 0.0d;
            try {
                SimpleEntry<Double, Double> q = getQuote(security);
                bid = q.getKey();
                ask = q.getValue();
            } catch (Exception e) {
            }

            String msg = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()) +
                    "\tclose\tsymbol=" + security + "\t     \t";

            msg += closeAtTradeStart==null ? "n/a" : closeAtTradeStart>0 ? "side=sell" : "side=buy" ; // closeAtTradeStart is negative if opening trade was short

            msg += "\tclose=" + close +
                    "\tbid=" + bid +
                    "\task=" + ask +
                    "\tlow=" + low +
                    "\thigh=" + high;
            msg += "\tcloseAtTradeStart=";
            msg += closeAtTradeStart==null ? "n/a\t" : (Math.abs(closeAtTradeStart) + "\tdiff=" + Double.parseDouble(String.format("%.2f", close-Math.abs(closeAtTradeStart))) );

            msg += closeAtTradeStart==null ? "n/a" : (closeAtTradeStart>0 && close-Math.abs(closeAtTradeStart)>0) || (closeAtTradeStart<0 && close-Math.abs(closeAtTradeStart)<0) ? "\tW" : "\tL" ;
            msg += "\t";
            msg += closeAtTradeStart==null ? "n/a" : (closeAtTradeStart>0 && close-Math.abs(closeAtTradeStart)>0) || (closeAtTradeStart<0 && close-Math.abs(closeAtTradeStart)<0) ? Double.parseDouble(String.format("%.2f", close-Math.abs(closeAtTradeStart))) : -Double.parseDouble(String.format("%.2f", close-Math.abs(closeAtTradeStart))) ;

            Files.write(TRADELOG_FILENAME, Arrays.asList(msg), StandardCharsets.UTF_8, Files.exists(TRADELOG_FILENAME) ? StandardOpenOption.APPEND : StandardOpenOption.CREATE);

            orderManagerTattler.appendTrunc(msg);
        } catch (AlpacaClientException e) {
            if (e.getAPIResponseMessage().contains("position not found")) {
                orderManagerTattler.appendTrunc("!!! flatten position missing for: " + security + "\n" + e.getMessage());
                return null;
            } else {
                orderManagerTattler.appendTrunc("!!! Error AlpacaClientException flattening position for: " + security + "\n" + e.getMessage());
                throw new AlpacaClientException(e);
            }
        } catch (IOException e) {
            orderManagerTattler.appendTrunc("!!! Error IOException flattening position for: " + security + "\n" + e.getMessage());
            throw new RuntimeException(e);
        }
        return ordTmp;
    }

    public List<Order> getOpenOrders() throws AlpacaClientException {
        return alpacaAPI.orders().get(
                CurrentOrderStatus.OPEN,
                null,
                null,
                ZonedDateTime.of(2021, 7, 6, 0, 0, 0, 0, ZoneId.of("America/New_York")),
                SortDirection.ASCENDING,
                true,
                null
        );
    }

    public void cancelOrder (String orderToCancel) throws AlpacaClientException {
        alpacaAPI.orders().cancel(orderToCancel);
        orderManagerTattler.appendTrunc("cancel order done for: " + orderToCancel + " local time: " + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format( ZonedDateTime.now() ) );
    }

    public List<Position> getOpenPositions() throws AlpacaClientException {
        return alpacaAPI.positions().get();
    }

}
