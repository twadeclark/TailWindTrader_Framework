package com.twadeclark.tailwindtrader;

import java.time.ZonedDateTime;

public class StockData {
    ZonedDateTime timestamp;
    double high;
    double low;
    double close;

    StockData(ZonedDateTime timestamp, double high, double low, double close) {
        this.timestamp = timestamp;
        this.high = high;
        this.low = low;
        this.close = close;
    }
}
