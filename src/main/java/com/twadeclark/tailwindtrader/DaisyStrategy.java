package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;

public class DaisyStrategy implements TradingStrategy {

    private final TextAreaHolder strategyManagerTattler;


    public DaisyStrategy(TextAreaHolder strategyManagerTattler) {
        this.strategyManagerTattler = strategyManagerTattler;
    }

    @Override
    public TradeSignal execute(String security, StrategyWrapper sw, CircularBuffer<StockData> circularBuffer) {

        // do all logic to determine if a trade should be executed
        //   return TradeSignal to trade
        //   return null for no trade

        strategyManagerTattler.appendTrunc("daisy strategy running on:" + security);

        if (false) {
            Double positionSize = 0.0d;
            double limitPrice = 0.0d;
            int holdTime = 0;
            String barPattern = "";

            return new TradeSignal(
                    security,
                    positionSize,
                    OrderSide.SELL,
                    OrderTimeInForce.GOOD_UNTIL_CANCELLED, // for stocks: OrderTimeInForce.DAY
                    limitPrice,
                    barPattern,
                    holdTime
            );
        }

        return null;

    }

}
