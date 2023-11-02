package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.StockBarsResponse;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.stock.historical.bar.enums.BarAdjustment;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import java.time.ZonedDateTime;

public class HistoryLoader_Alpaca_Stocks implements HistoryLoaderInterface {
    private AlpacaAPI alpacaAPI;

    HistoryLoader_Alpaca_Stocks(AlpacaAPI alpacaAPI) {
        this.alpacaAPI = alpacaAPI;
    }

    public int loadHistoryForSaving(String security, CircularBuffer<StockData> stockDataCB, ZonedDateTime startDate) throws AlpacaClientException {
        StockBarsResponse btcBarsResponse = alpacaAPI.stockMarketData().getBars(
                security,
                startDate,
                ZonedDateTime.now().minusMinutes(20), // wtf we can't load history within the last 15 minutes, but their clock can desync from ours
                10000,
                null,
                1,
                BarTimePeriod.MINUTE,
                BarAdjustment.SPLIT,
                null
        );

        int cnt = 0;
        for (StockBar cbar : btcBarsResponse.getBars()) {
            stockDataCB.add(cbar.getTimestamp(), cbar.getHigh(), cbar.getLow(), cbar.getClose());
            cnt++;
        }

        return cnt;
    }

    public int loadHistory(String security, CircularBuffer<StockData> stockDataCB) throws AlpacaClientException {
        StockBarsResponse btcBarsResponse = alpacaAPI.stockMarketData().getBars(
                security,
                ZonedDateTime.now().minusDays(1),
                ZonedDateTime.now().minusMinutes(16), // wtf we can't load history within the last 15 minutes, so we have to make it 16 minutes to be safe
                null,
                null,
                1,
                BarTimePeriod.MINUTE,
                BarAdjustment.SPLIT,
                null
        );

        int cnt = 0;
        for (StockBar cbar : btcBarsResponse.getBars()) {
            stockDataCB.add(cbar.getTimestamp(), cbar.getHigh(), cbar.getLow(), cbar.getClose());
            cnt++;
        }

        return cnt;
    }
}
