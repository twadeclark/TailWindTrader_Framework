package com.twadeclark.tailwindtrader;

import java.time.ZonedDateTime;

public class BacktestTrade {
    public ZonedDateTime openTimestamp;
    public ZonedDateTime closeTimestamp;
    public Double openPrice;
    public Double closePrice;
    public int holdTime;
    public Double PnL;
    public Double tradeHigh;
    public Double tradeLow;


    public BacktestTrade(ZonedDateTime openTimestamp, Double openPrice, int holdTime) {
        this.openTimestamp = openTimestamp;
        this.openPrice = openPrice;
        this.holdTime = holdTime;
    }

    public Double getPnL() {
        if (closePrice==null || openPrice==null) {
            return null;
        } else {
            return closePrice-openPrice;
        }
    }

    public ZonedDateTime getOpenTimestamp() {
        return openTimestamp;
    }
    public ZonedDateTime getCloseTimestamp() {
        return closeTimestamp;
    }
    public Double getOpenPrice() {
        return openPrice;
    }
    public Double getClosePrice() {
        return closePrice;
    }
    public Double getTradeHigh() {
        return tradeHigh;
    }
    public Double getTradeLow() {
        return tradeLow;
    }
    public int getHoldTime() {
        return holdTime;
    }

}
