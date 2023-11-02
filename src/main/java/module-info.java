module com.twadeclark.tailwindtrader {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.google.gson;
    requires alpaca.java;
    requires java.desktop;
    requires okhttp3;
    requires okio;
    requires commons.math3;
    requires java.sql;

    opens com.twadeclark.tailwindtrader to javafx.fxml, com.google.gson;
    exports com.twadeclark.tailwindtrader;
}