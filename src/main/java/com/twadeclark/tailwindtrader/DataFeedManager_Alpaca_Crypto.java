package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.assets.Asset;
import net.jacobpeterson.alpaca.model.endpoint.assets.enums.AssetClass;
import net.jacobpeterson.alpaca.model.endpoint.assets.enums.AssetStatus;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.MarketDataMessage;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.realtime.enums.MarketDataMessageType;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.realtime.bar.CryptoBarMessage;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;
import net.jacobpeterson.alpaca.websocket.marketdata.MarketDataListener;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DataFeedManager_Alpaca_Crypto implements DataFeedManagerInterface {
    private AlpacaAPI alpacaAPI;
    private MarketDataListener marketDataListener;
    private BiConsumer<String, StockData> priceUpdateHandler;
    private final ConfigurationManager configManager;
    private final StrategyManager strategyManager;
    private final Map<String, CircularBuffer<StockData>> marketDataBuffers;
    private Long[] lastEventNanoTime;
    private TextAreaHolder dataFeedManagerTattler;

    public DataFeedManager_Alpaca_Crypto(AlpacaAPI alpacaAPI, ConfigurationManager configManager, StrategyManager strategyManager, Map<String, CircularBuffer<StockData>> marketDataBuffers, Long[] lastEventNanoTime, TextAreaHolder dataFeedManagerTattler) {
        this.alpacaAPI = alpacaAPI;
        this.configManager = configManager;
        this.strategyManager = strategyManager;
        this.marketDataBuffers = marketDataBuffers;
        marketDataListener = this::onStockMarketData;
        this.lastEventNanoTime = lastEventNanoTime;
        this.dataFeedManagerTattler = dataFeedManagerTattler;
    }

    public List<Asset> ActiveAssetsList() throws AlpacaClientException {
        return alpacaAPI.assets().get(AssetStatus.ACTIVE, AssetClass.CRYPTO);
    }

    public void connect() {
        alpacaAPI.cryptoMarketDataStreaming().setListener(marketDataListener);
        alpacaAPI.cryptoMarketDataStreaming().connect();
        alpacaAPI.cryptoMarketDataStreaming().waitForAuthorization(5, TimeUnit.SECONDS);
        if (!alpacaAPI.cryptoMarketDataStreaming().isValid()) { // this is fatal error so halt
            dataFeedManagerTattler.appendTrunc("Websocket not valid!");
            throw new RuntimeException("Websocket not valid!");
        }
    }

    public void disconnect() {
        alpacaAPI.cryptoMarketDataStreaming().disconnect();
    }

    public void subscribe(List<String> securities) {
        alpacaAPI.cryptoMarketDataStreaming().subscribe(null, null, securities);
        dataFeedManagerTattler.appendTrunc("subscription sent for: " + securities);
    }

    private void onStockMarketData(MarketDataMessageType messageType, MarketDataMessage message) {
        try {
            if (messageType == MarketDataMessageType.BAR) {
                lastEventNanoTime[0] = System.nanoTime(); // reset UI timer

                String output = "";
                String input = message.toString();
                Pattern pattern = Pattern.compile("\\[(.*?)\\]");
                Matcher matcher = pattern.matcher(input);
                while (matcher.find()) {
                    output += matcher.group(1);
                }

                dataFeedManagerTattler.appendTrunc("(" + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()) + ") " + output.replace(",","\t"));

                CryptoBarMessage cryptoBarMessage = (CryptoBarMessage) message;
                String security = cryptoBarMessage.getSymbol();

                ZonedDateTime timestamp = cryptoBarMessage.getTimestamp();
                double high = cryptoBarMessage.getHigh();
                double low = cryptoBarMessage.getLow();
                double close = cryptoBarMessage.getClose();

                CircularBuffer<StockData> marketDataBuffer = marketDataBuffers.get(security);

                if (marketDataBuffer != null) {
                    marketDataBuffer.add(timestamp, high, low, close);
                    strategyManager.evaluateStrategy(security, OrderTimeInForce.GOOD_UNTIL_CANCELLED);
                }
            } else {
                dataFeedManagerTattler.appendTrunc("non-bar: " + messageType + " " + message);
            }
        } catch (Exception e) { // this is the final top level exception catcher
            dataFeedManagerTattler.appendTrunc("toString: " + e.toString() + "\ngetLocalizedMessage: " + e.getLocalizedMessage() + "\ngetMessage: " + e.getMessage());
        }
    }

}
