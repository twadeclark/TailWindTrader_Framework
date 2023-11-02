package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.model.endpoint.assets.Asset;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import java.util.List;

public interface DataFeedManagerInterface {

    void connect();
    void disconnect();
    void subscribe(List<String> securities);
    List<Asset> ActiveAssetsList() throws AlpacaClientException;
}
