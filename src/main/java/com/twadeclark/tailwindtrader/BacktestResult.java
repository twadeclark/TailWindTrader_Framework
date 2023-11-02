package com.twadeclark.tailwindtrader;

public class BacktestResult {
    private final int testCnt;
    private String patternHistory;
    private final int upTrades;
    private final int downTrades;
    private final double pctUpTrade;
    private final double PnL;
    private final double PnLperTrade;
    private final int upDeciles;
    private final int downDeciles;
    private final double finalDecilePnL;

//    private final double coVar;
//    private final double variance;
//    private final double mean;
//    private final double stdDev;
//    private final double geometricMean;
//    private final double kurtosis;
//    private final double populationVariance;
//    private final double quadraticMean;
//    private final double skewness;

//    public BacktestResult(int testCnt, String patternHistory, int upTrades, int downTrades, double pnL,
//                          int upDeciles, int downDeciles, double coVar, double variance, double mean, double stdDev,
//                          double geometricMean, double kurtosis, double populationVariance, double quadraticMean, double skewness) {
public BacktestResult(int testCnt, String patternHistory, int upTrades, int downTrades, double pnL,
                      int upDeciles, int downDeciles, double finalDecilePnL) {
        this.testCnt = testCnt;
        this.patternHistory = patternHistory;
        this.upTrades = upTrades;
        this.downTrades = downTrades;
    this.finalDecilePnL = finalDecilePnL;
    this.pctUpTrade = (double) upTrades / (double) (upTrades + downTrades);
        this.PnL = pnL;
        this.PnLperTrade = pnL / (double) (upTrades + downTrades);
        this.upDeciles = upDeciles;
        this.downDeciles = downDeciles;

//        this.coVar = coVar;
//        this.variance = variance;
//        this.mean = mean;
//        this.stdDev = stdDev;
//        this.geometricMean = geometricMean;
//        this.kurtosis = kurtosis;
//        this.populationVariance = populationVariance;
//        this.quadraticMean = quadraticMean;
//        this.skewness = skewness;
    }

    public BacktestResult cloneWithoutPatternHistory() {
        BacktestResult copy = new BacktestResult(testCnt, patternHistory, upTrades, downTrades, PnL, upDeciles, downDeciles, finalDecilePnL);
        return copy;
    }

    public void setPatternHistory(String inVal) { patternHistory = inVal; }

    public int getTestCnt() {
        return testCnt;
    }
    public String getPatternHistory() {
        return patternHistory;
    }
    public int getUpTrades() {
        return upTrades;
    }
    public int getDownTrades() {
        return downTrades;
    }
    public double getPctUpTrade() {
        return pctUpTrade;
    }
    public double getPnL() {
        return PnL;
    }
    public double getPnLperTrade() {
        return PnLperTrade;
    }
    public int getUpDeciles() {
        return upDeciles;
    }
    public int getDownDeciles() {
        return downDeciles;
    }
    public double getFinalDecilePnL() {
        return finalDecilePnL;
    }

    public String getPattern() {
        String numbers = patternHistory.replaceAll("\\D", "");
        return numbers;
    }

//    public double getCoVar() {
//        return coVar;
//    }
//    public double getVariance() {
//        return variance;
//    }
//    public double getMean() {
//        return mean;
//    }
//    public double getStdDev() {
//        return stdDev;
//    }
//    public double getGeometricMean() {
//        return geometricMean;
//    }
//    public double getKurtosis() {
//        return kurtosis;
//    }
//    public double getPopulationVariance() {
//        return populationVariance;
//    }
//    public double getQuadraticMean() {
//        return quadraticMean;
//    }
//    public double getSkewness() {
//        return skewness;
//    }
}
