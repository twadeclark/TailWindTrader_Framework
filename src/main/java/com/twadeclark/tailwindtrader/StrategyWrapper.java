package com.twadeclark.tailwindtrader;

import java.util.Map;

public class StrategyWrapper {
    public String strategyName;
    public Map<String, Object> parameters;

    public String getStrategyName() {
        return strategyName;
    }
    public Object getParameter(String parameterName) {
        return parameters.get(parameterName);
    }

}
