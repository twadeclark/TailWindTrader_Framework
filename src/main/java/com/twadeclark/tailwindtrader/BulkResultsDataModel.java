package com.twadeclark.tailwindtrader;

public class BulkResultsDataModel {
    private Integer start;
    private Integer end;
    private Integer tradeTotal;
    private Double finalPnLsum;
    private Double pnLperTrade;
//        private final SimpleStringProperty myString;
//        private final SimpleIntegerProperty myInteger;
//        private final SimpleDoubleProperty myDouble;

    public BulkResultsDataModel(Integer start, Integer end, Integer tradeTotal, Double finalPnLsum, Double pnLperTrade) {
        this.start = start;
        this.end = end;
        this.tradeTotal = tradeTotal;
        this.finalPnLsum = finalPnLsum;
        this.pnLperTrade = pnLperTrade;
//            this.myString = new SimpleStringProperty(myString);
//            this.myInteger = new SimpleIntegerProperty(myInteger);
//            this.myDouble = new SimpleDoubleProperty(myDouble);
    }

    // start, end, tradeTotal, finalPnLsum, pnLperTrade
    public Integer getStart() {
        return start;
    }

    public Integer getEnd() {
        return end;
    }

    public Integer getTradeTotal() {
        return tradeTotal;
    }

    public Double getFinalPnLsum() {
        return finalPnLsum;
    }

    public Double getPnLperTrade() {
        return pnLperTrade;
    }
//        public String getMyString() { return myString.get(); }
//        public Integer getMyInteger() { return myInteger.get(); }
//        public Double getMyDouble() { return myDouble.get(); }

    public void setStart(Integer value) {
        start = value;
    }

    public void setEnd(Integer value) {
        end = value;
    }

    public void setTradeTotal(Integer value) {
        tradeTotal = value;
    }

    public void setFinalPnLsum(Double value) {
        finalPnLsum = value;
    }

    public void setPnLperTrade(Double value) {
        pnLperTrade = value;
    }

//        public void setMyString(String value) { myString.set(value); }
//        public void setMyInteger(Integer value) { myInteger.set(value); }
//        public void setMyDouble(Double value) { myDouble.set(value); }
}
