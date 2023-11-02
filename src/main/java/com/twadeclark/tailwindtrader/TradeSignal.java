package com.twadeclark.tailwindtrader;

import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderSide;
import net.jacobpeterson.alpaca.model.endpoint.orders.enums.OrderTimeInForce;

public class TradeSignal {
    private OrderSide orderSide;
    private String security;
    private double quantity;
    private double limitPrice;
    private String rationale;
    private OrderTimeInForce timeInForce;
    private int holdTime;

    public TradeSignal(String security, Double quantity, OrderSide orderSide, OrderTimeInForce timeInForce, double limitPrice, String rationale, int holdTime) {
        this.security = security;
        this.quantity = quantity;
        this.orderSide = orderSide;
        this.timeInForce = timeInForce;
        this.limitPrice = limitPrice;
        this.rationale = rationale;
        this.holdTime = holdTime;
    }

    public String getSecurity() {
        return security;
    }
    public double getQuantity() {
        return quantity;
    }
    public OrderSide getOrderSide() {
        return orderSide;
    }
    public OrderTimeInForce getTimeInForce() {
        return timeInForce;
    }
    public double getLimitPrice() {
        return limitPrice;
    }
    public String getRationale() { return rationale; }
    public int getHoldTime() { return holdTime; }

    public void setTimeInForce(OrderTimeInForce timeInForce) { this.timeInForce = timeInForce; }

}
