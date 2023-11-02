package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.model.endpoint.orders.Order;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;
import net.jacobpeterson.alpaca.model.endpoint.positions.Position;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class StrategyManager {
    private final ConfigurationManager configManager;
    private final Map<String, CircularBuffer<StockData>> circularBuffers;
    private final OrderManager orderManager;
    private final TextAreaHolder strategyManagerTattler;

    private final TradingStrategy DAISY;
    Map<String, AbstractMap.SimpleEntry<ZonedDateTime, OrderSide>> positionsToClose = new HashMap<>();
    private ZonedDateTime lastOpenPositionsUpdate = ZonedDateTime.now();
    private List<Position> actualOpenPositions;


    public StrategyManager(ConfigurationManager configManager, Map<String, CircularBuffer<StockData>> circularBuffers, OrderManager orderManager, TextAreaHolder strategyManagerTattler) {
        this.configManager = configManager;
        this.circularBuffers = circularBuffers;
        this.orderManager = orderManager;
        this.strategyManagerTattler = strategyManagerTattler;
        DAISY = new DaisyStrategy(strategyManagerTattler);

        strategyManagerTattler.appendTrunc("- - - - - - - - - - Program Begin at System Time: " + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()));
    }

    public void evaluateStrategy(String security, OrderTimeInForce otif) throws AlpacaClientException, IOException {
        CircularBuffer<StockData> circularBuffer = circularBuffers.get(security);
        List<StrategyWrapper> strategyList = configManager.getStrategiesForTicker(security);
        TradeSignal tradeSignalTmp, tradeSignalActionable = null;

        if (lastOpenPositionsUpdate.plusSeconds(30).isBefore(ZonedDateTime.now())) { // could be on an independent timer instead of relying on a Listener to trigger
            lastOpenPositionsUpdate = ZonedDateTime.now();
            strategyManagerTattler.appendTrunc("\n- - - - - - - - - - begin processing bar batch at system time: " + DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss").format(LocalDateTime.now()));

            cancelAgedOrders();
        }

        for (StrategyWrapper sw : strategyList) {
            tradeSignalTmp = null;

            if (sw.getStrategyName().equals("daisy")) {
                tradeSignalTmp = DAISY.execute(security, sw, circularBuffer);
            } else {
                strategyManagerTattler.appendTrunc("unknown strategy: " + sw.getStrategyName());
            }

            if (tradeSignalTmp != null) { // we got a signal
                if (tradeSignalActionable == null) { // first signal from the strategy set
                    tradeSignalActionable = tradeSignalTmp;
                } else if (tradeSignalTmp.getOrderSide().equals(tradeSignalActionable.getOrderSide())) { // same direction
                    strategyManagerTattler.appendTrunc("overlapping strategy collision on " + security + ". " + tradeSignalTmp.getRationale() + " vs " + tradeSignalActionable.getRationale());
                    tradeSignalActionable = tradeSignalTmp;
                } else { // if opposite signal..
                    strategyManagerTattler.appendTrunc("strategy collision on " + security + ". skipping bar. " + tradeSignalTmp.getRationale() + " vs " + tradeSignalActionable.getRationale());
                    tradeSignalActionable = null;
                    break;
                }
            }
        }

        if (tradeSignalActionable != null) { // there's an order ready to go
            if (positionsToClose.get(security) == null) { // we are flat
                tradeSignalActionable.setTimeInForce(otif);
                Order thisOrder = orderManager.placeLimitOrder(tradeSignalActionable, circularBuffer.get(0).close, circularBuffer.get(0).low, circularBuffer.get(0).high); // send the order to the broker
                strategyManagerTattler.appendTrunc("+ sent order for: " + security + " close: " + circularBuffer.get(0).close +" " + tradeSignalActionable.getRationale());
                positionsToClose.put(security, new AbstractMap.SimpleEntry<>(ZonedDateTime.now().plusMinutes(tradeSignalActionable.getHoldTime()), tradeSignalActionable.getOrderSide()));

            } else { // we got an order request for the security we are already in
                if (positionsToClose.get(security) != null && positionsToClose.get(security).getValue() == tradeSignalActionable.getOrderSide()) { // same direction? extend flatten time
                    strategyManagerTattler.appendTrunc("! extending timeout for open position: " + security + " rationale: " + tradeSignalActionable.getRationale());
                    ZonedDateTime newDateTime = ZonedDateTime.now().plusMinutes(tradeSignalActionable.getHoldTime());
                    AbstractMap.SimpleEntry<ZonedDateTime, OrderSide> oldEntry = positionsToClose.get(security);
                    AbstractMap.SimpleEntry<ZonedDateTime, OrderSide> newEntry = new AbstractMap.SimpleEntry<>(newDateTime, oldEntry.getValue());
                    positionsToClose.put(security, newEntry);
                } else { // opposite direction? report to UI
                    strategyManagerTattler.appendTrunc(". opposite direction order for: " + security + " rationale: " + tradeSignalActionable.getRationale());
                }
            }
        }

        // send flatten orders if needed
        if (positionsToClose.get(security) != null && positionsToClose.get(security).getKey().plusSeconds(30).isBefore(ZonedDateTime.now())) {
            orderManager.flattenPosition(security, circularBuffer.get(0).close, circularBuffer.get(0).low, circularBuffer.get(0).high);
            strategyManagerTattler.appendTrunc("- sent go flat for: " + security + " close: " + circularBuffer.get(0).close);
            positionsToClose.remove(security);
        }
    }

    private void reconcileOpenVsExpectedPositions() {
        ArrayList<String> posListTmp = new ArrayList<>();
        for (Position posTmp: actualOpenPositions) {
            String getSymbol = posTmp.getSymbol();
            if (positionsToClose.get(getSymbol) == null) {
                strategyManagerTattler.appendTrunc("unexpected open position found for: " + posTmp.getSymbol() + "\n   " + posTmp);
                posListTmp.add(posTmp.getSymbol());
            }
        }

        Set<String> s = positionsToClose.keySet();

        for (String securityTmp:s) {
            if (!posListTmp.contains(securityTmp)) {
                strategyManagerTattler.appendTrunc("missing open position for: " + securityTmp + " set to flatten: " + positionsToClose.get(securityTmp));
            }
        }
    }

    private void cancelAgedOrders() throws AlpacaClientException {
        // this cancels aged orders from ALL securities
        for (Order orderTmp : orderManager.getOpenOrders()) {
            // read ALL open trades from broker, close any over 30 seconds old
            if (orderTmp.getCreatedAt().isAfter(ZonedDateTime.now().plusSeconds(30))) {
                strategyManagerTattler.appendTrunc("?? cancel order (attempt) " + orderTmp.getSymbol() + " orderTmp.getId(): " + orderTmp.getId() );
                orderManager.cancelOrder(orderTmp.getId());
                strategyManagerTattler.appendTrunc(":) cancel order (success) : " + orderTmp.getSymbol() + " orderTmp.getId(): " + orderTmp.getId() );
            }
        }
    }

}
