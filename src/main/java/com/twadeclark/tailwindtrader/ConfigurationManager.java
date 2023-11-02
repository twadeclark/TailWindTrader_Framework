package com.twadeclark.tailwindtrader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigurationManager {

    private final Map<String, List<StrategyWrapper>> tickerStrategies = new HashMap<>();
//    public List<String> Broker;
    public String Broker;


    public ConfigurationManager(String jsonFilePath) {
//        this.tickerStrategies = FileUtilities.loadTickerStrategies(jsonFilePath);
        this.Broker = FileUtilities.loadTickerStrategies(jsonFilePath, tickerStrategies);
    }

//    private Map<String, List<StrategyWrapper>> loadTickerStrategies(String jsonFilePath) {
//        Gson gson = new Gson();
//        Map<String, List<StrategyWrapper>> strategies;
//
//        try (FileReader reader = new FileReader(jsonFilePath)) {
//            Type configType = new TypeToken<Map<String, Object>>() {}.getType();
//            Map<String, Object> config = gson.fromJson(reader, configType);
//
//            Broker = ((List<String>) config.get("broker")).get(0);
//            config.remove("broker");
//
//            strategies = gson.fromJson(new Gson().toJson(config), new TypeToken<Map<String, List<StrategyWrapper>>>() {}.getType());
//        } catch (IOException e) {
//            e.printStackTrace();
//            throw new RuntimeException("Failed to load configuration file: " + jsonFilePath, e);
//        }
//
//        return strategies;
//    }

    public List<String> getAllTickerSymbols() {
        return new ArrayList<>(tickerStrategies.keySet());
    }

    public List<StrategyWrapper> getStrategiesForTicker(String tickerSymbol) {
        return tickerStrategies.get(tickerSymbol);
    }


}
