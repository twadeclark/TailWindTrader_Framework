package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import java.time.ZonedDateTime;

public interface HistoryLoaderInterface {
    int loadHistory(String security, CircularBuffer<StockData> stockDataCB) throws AlpacaClientException;
    int loadHistoryForSaving(String security, CircularBuffer<StockData> stockDataCB, ZonedDateTime startDate) throws AlpacaClientException;
}
