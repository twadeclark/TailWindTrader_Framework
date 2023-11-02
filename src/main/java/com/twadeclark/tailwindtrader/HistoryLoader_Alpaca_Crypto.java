package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.common.historical.bar.enums.BarTimePeriod;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBar;
import net.jacobpeterson.alpaca.model.endpoint.marketdata.crypto.historical.bar.CryptoBarsResponse;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import java.time.ZonedDateTime;

public class HistoryLoader_Alpaca_Crypto implements HistoryLoaderInterface {
    private AlpacaAPI alpacaAPI;

    HistoryLoader_Alpaca_Crypto(AlpacaAPI alpacaAPI) {
        this.alpacaAPI = alpacaAPI;
    }

    public int loadHistory(String security, CircularBuffer<StockData> stockDataCB) throws AlpacaClientException {
        CryptoBarsResponse btcBarsResponse = alpacaAPI.cryptoMarketData().getBars(
                security,
                null,
                ZonedDateTime.now().minusDays(3),
                10000,
                null,
                1,
                BarTimePeriod.MINUTE);

        int cnt = 0;

        if (btcBarsResponse.getBars()==null) {return 0;}

        for (CryptoBar cbar : btcBarsResponse.getBars()) {
            stockDataCB.add(cbar.getTimestamp(), cbar.getHigh(), cbar.getLow(), cbar.getClose());
            cnt++;
        }

        return cnt;
    }

    @Override
    public int loadHistoryForSaving(String security, CircularBuffer<StockData> stockDataCB, ZonedDateTime startDate) throws AlpacaClientException {
        CryptoBarsResponse btcBarsResponse = alpacaAPI.cryptoMarketData().getBars(
                security,
                null,
                startDate,
                10000,
                null,
                1,
                BarTimePeriod.MINUTE);

        int cnt = 0;
        for (CryptoBar cbar : btcBarsResponse.getBars()) {
            stockDataCB.add(cbar.getTimestamp(), cbar.getHigh(), cbar.getLow(), cbar.getClose());
            cnt++;
        }

        return cnt;
    }

}
