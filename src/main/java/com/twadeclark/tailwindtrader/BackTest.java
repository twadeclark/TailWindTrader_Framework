package com.twadeclark.tailwindtrader;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.ScatterChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.text.NumberFormat;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class BackTest {

    public volatile Boolean ProcessStrategyRun = true;

    public BackTest() {
    }

    private CircularBuffer<StockData> loadedFileFullDataCircularBufferForBulkRun;

    //public Task<Long> bulkRunTask(ObservableList<String> selectedItems, double pctUpTradeMinimum, double pnLperTradeMinimum, int decilesMinimum, TextArea bulkRunMessagesTextArea, TextField BulkRunUpdateTextField, ProgressBar loadProgressBar) {
    public Task<Long> bulkRunTask(ObservableList<String> selectedItems, TextArea bulkRunMessagesTextArea, TextField BulkRunUpdateTextField, ProgressBar loadProgressBar) {
        long timeStart = System.currentTimeMillis();

        Task<Long> task = new Task<>() {
            @Override
            protected Long call() {
                try {
                    TextAreaTattler("loading " + selectedItems.size() + " files\n", bulkRunMessagesTextArea);

                    for (Object o : selectedItems) {
                        // select file
                        String selectedFile = o.toString();




                        // load file into circ buff
                        TextAreaTattler("  " + selectedFile + "\n", bulkRunMessagesTextArea);
                        Map<String, Integer> patternAggregate = new HashMap<>();

//                        Task<CircularBuffer<StockData>> loadTask = loadFileIntoBuffer(selectedFile, patternAggregate);
//                        loadTask.setOnSucceeded(event -> { loadedFileFullDataCircularBufferForBulkRun = loadTask.getValue(); });
//                        Thread loadThread = new Thread(loadTask);
//                        loadThread.setDaemon(true);
//                        loadThread.start();
//                        loadThread.join(); // Wait for task to complete
                        loadedFileFullDataCircularBufferForBulkRun = FileUtilities.loadFileIntoBuffer(selectedFile, patternAggregate);

                        TextAreaTattler("  patterns found: " + patternAggregate.size() + "\n", bulkRunMessagesTextArea);

                        // get history length
                        List<String> historyToTest = getHistoryToTest("123");

                        // run backtest
                        String security = selectedFile.split("_")[0];
                        TextAreaTattler("  run backtest for: " + security + " \n", bulkRunMessagesTextArea);
                        Map<String, ArrayList<BacktestTrade>> backtestTradeMap = new HashMap<>();
                        ArrayList<BacktestResult> backtestResults = new ArrayList<>();

                        Boolean detailedMessages = false;

                        Task<StringBuilder> processTask = quickProcessStrategyTask(
                                security,
                                loadedFileFullDataCircularBufferForBulkRun,
                                new ArrayList<>(patternAggregate.keySet()),
                                historyToTest,
                                backtestTradeMap,
                                backtestResults,
                                detailedMessages,
                                BulkRunUpdateTextField
                        );

                        try {
                            loadProgressBar.progressProperty().bind(processTask.progressProperty()); // this line fails sometimes, but doesn't hurt anything if it does
                        } catch (Exception e) {}

                        processTask.setOnSucceeded(event -> {
                            StringBuilder sb = processTask.getValue();
                            TextAreaTattler(sb + "  end of backtest\n", bulkRunMessagesTextArea);
                        });

                        Thread processThread = new Thread(processTask);
                        processThread.setDaemon(true);
                        processThread.start();
                        processThread.join(); // Wait for task to complete

//                        loadProgressBar.progressProperty().unbind();

                        // save bulk results
                        saveBulkResults(backtestResults, security);

                        // top tiers
//                        ListView patternsForConfigFileListView = new ListView();
//                        ObservableList<BacktestResult> btr = FXCollections.observableArrayList(backtestResults);
//                        ObservableList<String> topTiersObservableList = getTopTiersPatternList(pctUpTradeMinimum, pnLperTradeMinimum, decilesMinimum, btr, patternsForConfigFileListView);
//                        TextAreaTattler("  top tiers: " + topTiersObservableList.size() + " \n", bulkRunMessagesTextArea);

                        // generate strategy file
//                        savePatternsInTickerStrategyFile(security, topTiersObservableList, bulkRunMessagesTextArea);
//                        TextAreaTattler("  file saved (" + (System.currentTimeMillis() - timeStart) + " ms elapsed)\n\n", bulkRunMessagesTextArea);

                        TextAreaTattler("  backtestResults saved for " + security + " (" + (System.currentTimeMillis() - timeStart) + " ms elapsed)\n\n", bulkRunMessagesTextArea);

                    }
                } catch (Exception e) {
                    TextAreaTattler("\n\n" + e, bulkRunMessagesTextArea);
                }

                TextAreaTattler("all done\n", bulkRunMessagesTextArea);

                return System.currentTimeMillis() - timeStart;
            }
        };
        return task;
    }

    public void saveBulkResults(ArrayList<BacktestResult> backtestResults, String security) throws IOException {
        // PatternHistory, UpTrades, DownTrades, PctUpTrade, PnL, PnLperTrade, UpDeciles, DownDeciles, FinalDecilePnL
        StringBuilder outVal = new StringBuilder("PatternHistory, UpTrades, DownTrades, PctUpTrade, PnL, PnLperTrade, UpDeciles, DownDeciles, FinalDecilePnL\n");

        for (BacktestResult btr:backtestResults) {

            outVal.append(
                    btr.getPatternHistory() + "," +
                    btr.getUpTrades() + "," +
                    btr.getDownTrades() + "," +
                    btr.getPctUpTrade() + "," +
                    btr.getPnL() + "," +
                    btr.getPnLperTrade() + "," +
                    btr.getUpDeciles() + "," +
                    btr.getDownDeciles() + "," +
                    btr.getFinalDecilePnL() + "\n"
            );
        }

        String filename = "results/" + security + "_BacktestResults.csv";
        FileUtilities.writeStringToFile(filename, outVal.toString());
    }

    private void TextAreaTattler(String s, TextArea tmpTA) {
        Platform.runLater(() -> {
            tmpTA.appendText(s);
            tmpTA.setScrollTop(Double.MAX_VALUE);
        });
    }

    public Task<StringBuilder> quickProcessStrategyTask(String security, CircularBuffer loadedFileFullDataCircularBuffer, List<String> patternsToTest, List<String> historyToTest, Map<String, ArrayList<BacktestTrade>> outBacktestTradeMap, ArrayList<BacktestResult> outBacktestResults, Boolean detailedMessages, TextField bulkRunUpdateTextField) {
        StringBuilder sb = new StringBuilder();
        long timeStartTotal = System.currentTimeMillis();
        int totalTests = patternsToTest.size() * historyToTest.size();

        TextAreaHolder tah = new TextAreaHolder(null); // set this to null so we don't update any UI elements in the strategy
        TradingStrategy DAISY = new DaisyStrategy(tah);

        Task<StringBuilder> task = new Task<>() {
            @Override
            protected StringBuilder call() {
                try {
                    int testCnt = 0;

                    for (Object patternLoop: patternsToTest) {
                        String patternTmp = patternLoop.toString().split("=")[0];

                        for (String historyTmp: historyToTest) {
                            updateProgress(++testCnt, totalTests);

                            String patternHistory = patternTmp + historyTmp;
                            int finalTestCnt = testCnt;
                            String s = security + " (" + finalTestCnt + "/" + totalTests + ") " + patternHistory;
                            Platform.runLater(() -> {
                                bulkRunUpdateTextField.setText(s);
                            });

                            // do not run if we already have this patternHistory loaded
                            if ( haveWeAlreadyProcessedIt(patternHistory, outBacktestResults) ) { continue; } // must be a function call so the continue hits the correct for loop

                            // run the strategy
                            StrategyWrapper sw = BackTest.getStrategyWrapper(patternTmp, historyTmp);
                            long timeStartOneRun = System.currentTimeMillis();
                            int signalCnt = 0;
                            CircularBuffer cbTmp = new CircularBuffer(loadedFileFullDataCircularBuffer.Size());
                            ArrayList<BacktestTrade> tradeLog = new ArrayList();

                            for (int i = loadedFileFullDataCircularBuffer.Size()-1; i>=0; i--) {
                                if(!ProcessStrategyRun) {
                                    String sTmp = "  " + security + " stopped early: " + i;
                                    Platform.runLater(() -> {
                                        bulkRunUpdateTextField.setText(sTmp);
                                    });

                                    sb.append(sTmp);
                                    return sb;
                                }

                                if (loadedFileFullDataCircularBuffer.get(i) != null) {
                                    ZonedDateTime z = loadedFileFullDataCircularBuffer.get(i).timestamp;
                                    double high = loadedFileFullDataCircularBuffer.get(i).high;
                                    double low = loadedFileFullDataCircularBuffer.get(i).low;
                                    double close = loadedFileFullDataCircularBuffer.get(i).close;
                                    cbTmp.add(z,high,low,close);

                                    TradeSignal tradeSignalTmp = DAISY.execute(security, sw, cbTmp);

                                    if (tradeSignalTmp != null) {
                                        signalCnt++;
                                        tradeLog.add(new BacktestTrade(z, close, (Integer) sw.getParameter("holdTime")));
                                    }

                                    // check if any trades need to be closed
                                    for (BacktestTrade bt : tradeLog) {
                                        if (bt.closePrice == null) {
                                            boolean stillOn = false;

                                            Double tradeHi = bt.openPrice;
                                            Double tradeLo = bt.openPrice;

                                            for (int tmp=1;tmp<=bt.holdTime;tmp++) {
                                                if (cbTmp.get(tmp) != null) {
                                                    ZonedDateTime foo = bt.openTimestamp;
                                                    ZonedDateTime bar = cbTmp.get(tmp).timestamp;
                                                    if (foo.isAfter(bar)) { stillOn=true; }

                                                    tradeHi = Math.max(tradeHi, cbTmp.get(tmp).high);
                                                    tradeLo = Math.min(tradeLo, cbTmp.get(tmp).low);
                                                }
                                            }

                                            if (!stillOn) {
                                                bt.closeTimestamp = cbTmp.get(0).timestamp;
                                                bt.closePrice = cbTmp.get(0).close;
                                                bt.tradeHigh = tradeHi - bt.openPrice;
                                                bt.tradeLow = tradeLo - bt.openPrice;
                                            }
                                        }
                                    }
                                }
                            }

                            // look at all closed trades for the whole run and tally them to pnl
                            int up=0, dn=0;
                            double pnl = 0.0f;
                            ArrayList<Double> pnlRunningTot = new ArrayList<>();
//                            DescriptiveStatistics stats = new DescriptiveStatistics();

                            for (BacktestTrade bt : tradeLog) {
                                if (bt.closePrice == null) {
                                } else {
                                    if (bt.closePrice > bt.openPrice) { up++; }
                                    if (bt.closePrice < bt.openPrice) { dn++; }
                                    pnl += bt.closePrice - bt.openPrice;
                                    pnlRunningTot.add(pnl);
//                                    stats.addValue(bt.closePrice - bt.openPrice);
                                }
                            }

                            int upDeciles = 0;
                            int downDeciles = 0;
                            int lineCnt = pnlRunningTot.size();
                            double finalDecilePnL = 0.0d;

                            if (lineCnt > 0) {
                                double lastTime = pnlRunningTot.get(0);

                                for (int i=1;i<=10;i++) {
                                    int foo = Math.min(i * lineCnt / 12, lineCnt - 1) ;
                                    double thisTime = pnlRunningTot.get(foo);
                                    if (thisTime > lastTime) { upDeciles++; }
                                    if (thisTime < lastTime) { downDeciles++; }
                                    lastTime = thisTime;
                                }

                                finalDecilePnL = pnlRunningTot.get(pnlRunningTot.size() - 1) - lastTime;
                            }

//                            double Variance = stats.getVariance();
//                            double mean = stats.getMean();
//                            double stdDev = stats.getStandardDeviation();
//                            double coVar = stdDev / mean;
//                            double GeometricMean = stats.getGeometricMean();
//                            double Kurtosis = stats.getKurtosis();
//                            double PopulationVariance = stats.getPopulationVariance();
//                            double QuadraticMean = stats.getQuadraticMean();
//                            double Skewness = stats.getSkewness();

//                            System.out.println("pattern: " + patternTmp + historyTmp);
//                            System.out.println("getGeometricMean: " + stats.getGeometricMean());
//                            System.out.println("getKurtosis: " + stats.getKurtosis());
//                            System.out.println("Mean: " + mean);
//                            System.out.println("getPopulationVariance: " + stats.getPopulationVariance());
//                            System.out.println("getQuadraticMean: " + stats.getQuadraticMean());
//                            System.out.println("getSkewness: " + stats.getSkewness());
//                            System.out.println("Standard Deviation: " + stdDev);
//                            System.out.println("Variance: " + stats.getVariance());
//                            System.out.println("Coefficient of Variation: " + coVar);
//                            System.out.println();

//                            outBacktestResults.add(new BacktestResult(testCnt, patternTmp + historyTmp, up, dn, pnl,
//                                    upDeciles, downDeciles, coVar, Variance, mean, stdDev,
//                                    GeometricMean, Kurtosis, PopulationVariance, QuadraticMean, Skewness));

                            outBacktestTradeMap.put(patternTmp + historyTmp, tradeLog);
                            outBacktestResults.add(new BacktestResult(testCnt, patternTmp + historyTmp, up, dn, pnl, upDeciles, downDeciles, finalDecilePnL));

                            if (detailedMessages) {
                                sb.append(
                                        "(" + testCnt + "/" + totalTests + ")" +
                                                "\tparameters:\t" + patternTmp + historyTmp +
                                                "\trun time:\t" + (System.currentTimeMillis() - timeStartOneRun) +
                                                "\tsignals:\t" + signalCnt +
                                                "\ttrades:\t" + tradeLog.size() + "\t(up:" + up + " dn:" + dn + ")" +
                                                "\tPnL:\t" + String.format("%.2f", pnl) +
                                                "\tPnL / trade:\t" + String.format("%.3f", pnl / (double) tradeLog.size()) +
                                                "\n");
                            }
                        }
                    }
                } catch (Exception e) {
                    sb.append(e + "\n");
                }

                String sTmp = security + " run time: " + (System.currentTimeMillis() - timeStartTotal) + " milliseconds (" + (System.currentTimeMillis() - timeStartTotal) / totalTests + " ms ave)\n";
                Platform.runLater(() -> {
                    bulkRunUpdateTextField.setText(sTmp);
                });

                sb.append(sTmp);
                return sb;
            }
        };
        return task;
    }

    private static Boolean haveWeAlreadyProcessedIt(String patternHistory, ArrayList<BacktestResult> backtestResults) {
        for (BacktestResult btr:backtestResults) {
            if (btr.getPatternHistory().equals(patternHistory)) { return true; }
        }
        return false;
    }


    public void savePatternsInTickerStrategyFile(String security, ObservableList patternsForConfigFile, TextArea LoadFileDetailsTextArea) {
        String filename = "strategy/" + security + "_TickerStrategy.txt";

        try {
            StringBuilder outVal = new StringBuilder("****************************** APPENDED: " + ZonedDateTime.now().toLocalDateTime() + " ******************************\n");
            outVal.append("  \"");
            outVal.append(security.toUpperCase());
            outVal.append("\": [\n ");

            for(Object o:patternsForConfigFile) {
                String input = o.toString();
                System.out.println("input: |" + input + "|");

                Pattern pattern = Pattern.compile("(\\w+)\\s-\\s(\\d{1,3})(\\w*)");
                Matcher matcher = pattern.matcher(input);

                if (matcher.matches()) {
                    String letters = matcher.group(1);
                    int digit = Integer.parseInt(matcher.group(2));
                    String restOfString = matcher.group(3);

                    System.out.println("Letters: " + letters);
                    System.out.println("Digit: " + digit);
                    System.out.println("Rest of String: " + restOfString);

                    stratPiece(outVal, letters, digit, restOfString);
                } else {
                    TextAreaTattler("Input doesn't match the expected format: " + input + "\n", LoadFileDetailsTextArea);
                }
            }

            outVal.delete(outVal.length()-2,outVal.length()-1); // this removes the final unneeded comma

            outVal.append("  ],\n");
            System.out.println(outVal);

            File file = new File(filename);
            System.out.println(file.getAbsolutePath());

            FileWriter writer = new FileWriter(file, true);
            writer.write(outVal.toString());
            writer.close();
            TextAreaTattler("strategy appended to: " + filename + "\n", LoadFileDetailsTextArea);
        } catch (IOException e) {
            TextAreaTattler("error appending to: " + filename + "" + e + "\n", LoadFileDetailsTextArea);
        }
    }

    public void saveTopPctInTickerStrategyFile(String broker, ObservableList configFileList, TextArea generateStrategyFileTextArea) { // todo
        try {
            if (configFileList.size()==0) {return;}
            StringBuilder outVal = new StringBuilder();
            outVal.append("{\n");
            outVal.append("  \"broker\": [\"" + broker + "\"],\n");

            Platform.runLater(() -> { generateStrategyFileTextArea.appendText("starting for " + broker + "\n"); });

            for (Object item:configFileList) { // file loop
                String datafile = item.toString();
                String security = datafile.split("_")[0];

                Platform.runLater(() -> { generateStrategyFileTextArea.appendText("  " + datafile + "\n"); });

                ArrayList<BacktestResult> backtestResults = FileUtilities.getBacktestResults(datafile);

                String tmppp = "    patterns: " + backtestResults.size() + "\n";
                Platform.runLater(() -> { generateStrategyFileTextArea.appendText(tmppp); });

                backtestResults = (ArrayList<BacktestResult>) backtestResults.stream()
                        .filter(result -> (result.getUpDeciles() + result.getDownDeciles()) == 10)
                        .collect(Collectors.toList());
                Collections.sort(backtestResults, Comparator.comparingDouble(BacktestResult::getPctUpTrade));

                int percent = (int) Math.ceil(backtestResults.size() * 0.03); // todo read actual pct from UI

                String tmp =   "        using:  " + percent + "\n";
                Platform.runLater(() -> { generateStrategyFileTextArea.appendText(tmp); });

                outVal.append("  \"");
                outVal.append(security.toUpperCase());
                outVal.append("\": [\n ");

                Pattern pattern = Pattern.compile("(\\d{1,3})([a-zA-Z]{0,3})");
                int i;

                // Iterate over the first 3% of the list
                for (i = 0; i < percent; i++) { // short list
                    BacktestResult btr = backtestResults.get(i);
                    String patternHistory = btr.getPatternHistory();
                    Matcher matcher = pattern.matcher(patternHistory);

                    if (matcher.matches()) {
                        String direction = "short";
                        String group1 = matcher.group(1);
                        int barPattern = Integer.parseInt(group1);
                        String recordUpDown = matcher.group(2);

                        stratPiece(outVal, direction, barPattern, recordUpDown);
                    } else {
                        System.out.println("Input doesn't match the expected format: " + btr.getPatternHistory() );
                    }
                }

                // Iterate over the last 3% of the list
                for (i = backtestResults.size() - percent; i < backtestResults.size(); i++) { // long list
                    BacktestResult btr = backtestResults.get(i);
                    Matcher matcher = pattern.matcher(btr.getPatternHistory());

                    if (matcher.matches()) {
                        String direction = "long";
                        int barPattern = Integer.parseInt(matcher.group(1));
                        String recordUpDown = matcher.group(2);

                        stratPiece(outVal, direction, barPattern, recordUpDown);
                    } else {
                        System.out.println("Input doesn't match the expected format: " + btr.getPatternHistory() );
                    }
                }

                outVal.delete(outVal.length()-2,outVal.length()-1); // this removes the final unneeded comma
                outVal.append("  ],\n");
            }

            outVal.delete(outVal.length()-2,outVal.length()-1); // this removes the final unneeded comma
            outVal.append("}\n");

            ZonedDateTime now = ZonedDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMMdd_HHmmssZ", Locale.ENGLISH);
            String formattedDateTime = now.format(formatter);
            String filename = "strategy/TickerStrategy_" + broker + "_" + formattedDateTime + ".txt";
            File file = new File(filename);
            System.out.println(file.getAbsolutePath());

            FileWriter writer = new FileWriter(file, true);
            writer.write(outVal.toString());
            writer.close();

            Platform.runLater(() -> { generateStrategyFileTextArea.appendText("done for: " + filename + "\n"); });
        } catch (IOException e) {
            System.out.println(e);
            Platform.runLater(() -> { generateStrategyFileTextArea.appendText("\n\n" + e + "\n\n" ); });
        }
    }


    private static void stratPiece(StringBuilder outVal, String direction, int barPattern, String recordUpDown) {
        outVal.append("    {\n");
        outVal.append("      \"strategyName\": \"daisy\",\n");
        outVal.append("      \"parameters\": {\n");
        outVal.append("        \"direction\": \"");
        outVal.append(direction);
        outVal.append("\",\n");
        outVal.append("        \"positionSize\": 1,\n");
        outVal.append("        \"barPattern\": \"");
        outVal.append(barPattern);
        outVal.append("\",\n");
        outVal.append("        \"recordUpDown\": \"");
        outVal.append(recordUpDown);
        outVal.append("\",\n");
        outVal.append("        \"holdTime\": 7\n");
        outVal.append("      }\n");
        outVal.append("    },\n");
    }

    public ObservableList<String> getTopTiersPatternList(double pctStratsToKeepTextField, ObservableList<BacktestResult> backtestResultObservableList, ListView PatternsForConfigFileListView) {
        ArrayList patternList = new ArrayList<>();
        ObservableList<String> existingItems = PatternsForConfigFileListView.getItems();


        ObservableList<String> items = FXCollections.observableArrayList(patternList);
        return items;
    }


    public static void loadDetailsGraph (BacktestResult btr, Map<String, ArrayList<BacktestTrade>> backtestTradeMap, ScatterChart PnLScatterChart, LineChart PnLlineChart) {
        String patternHistory = btr.getPatternHistory();
        ArrayList<BacktestTrade> tradeLog = backtestTradeMap.get(patternHistory);

        XYChart.Series series = new XYChart.Series();
        XYChart.Series seriesLine = new XYChart.Series();

        Double dd = 0.0d;

        for (int i=0;i<tradeLog.size();i++) {
            BacktestTrade bt = tradeLog.get(i);
            if (bt != null) {
                Double d = bt.getPnL();

                if (d != null) {
                    dd += d;
                    series.getData().add(new XYChart.Data<>(i, d));
                    seriesLine.getData().add(new XYChart.Data<>(i, dd));
                }
            }
        }

        PnLScatterChart.getData().clear();
        PnLScatterChart.getData().addAll(series);
        series.setName("pattern: " + patternHistory);

        PnLlineChart.getData().clear();
        PnLlineChart.getData().addAll(seriesLine);
        seriesLine.setName("pattern: " + patternHistory);
    }

    public static void loadTradeLogTableView(BacktestResult btr, TableView<BacktestTrade> TradeLogTableView, Map<String, ArrayList<BacktestTrade>> backtestTradeMap) {
        if (TradeLogTableView.getColumns().size() == 0) { // todo RIGHT side column list
            TableColumn<BacktestTrade, ZonedDateTime> openTimestampColumn = new TableColumn<>("openTimestamp");
            openTimestampColumn.setCellValueFactory(new PropertyValueFactory<>("openTimestamp"));

            TableColumn<BacktestTrade, Double> holdTimeColumn = new TableColumn<>("holdTime");
            holdTimeColumn.setCellValueFactory(new PropertyValueFactory<>("holdTime"));

            TableColumn<BacktestTrade, Double> openPriceColumn = new TableColumn<>("openPrice");
            openPriceColumn.setCellValueFactory(new PropertyValueFactory<>("openPrice"));

            TableColumn<BacktestTrade, Double> closePriceColumn = new TableColumn<>("closePrice");
            closePriceColumn.setCellValueFactory(new PropertyValueFactory<>("closePrice"));

            TableColumn<BacktestTrade, Double> PnLColumn = new TableColumn<>("PnL");
            PnLColumn.setCellValueFactory(new PropertyValueFactory<>("PnL"));
            BackTest.setTwoDigitPrecisionForBacktestTrade(PnLColumn);

            TableColumn<BacktestTrade, Double> tradeHighColumn = new TableColumn<>("tradeHigh");
            tradeHighColumn.setCellValueFactory(new PropertyValueFactory<>("tradeHigh"));
            BackTest.setTwoDigitPrecisionForBacktestTrade(tradeHighColumn);

            TableColumn<BacktestTrade, Double> tradeLowColumn = new TableColumn<>("tradeLow");
            tradeLowColumn.setCellValueFactory(new PropertyValueFactory<>("tradeLow"));
            BackTest.setTwoDigitPrecisionForBacktestTrade(tradeLowColumn);

            TradeLogTableView.getColumns().addAll(openTimestampColumn, holdTimeColumn, openPriceColumn, closePriceColumn, PnLColumn, tradeHighColumn, tradeLowColumn);
        }

        String patternHistory = btr.getPatternHistory();
        ArrayList<BacktestTrade> tradeLog = backtestTradeMap.get(patternHistory);

        ObservableList<BacktestTrade> data = FXCollections.observableArrayList();
        data.addAll(tradeLog);

        TradeLogTableView.setItems(data);
    }

    public static void setThreeDigitPrecisionForBacktestResult(TableColumn<BacktestResult, Double> tmpColumn) {
        tmpColumn.setCellFactory(column -> new TableCell<>() {
            private final NumberFormat nf = NumberFormat.getNumberInstance();

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    nf.setMaximumFractionDigits(3);
                    nf.setMinimumFractionDigits(3);
                    setText(nf.format(item));
                }
            }
        });
    }

    private static void setTwoDigitPrecisionForBacktestTrade(TableColumn<BacktestTrade, Double> tmpColumn) {
        tmpColumn.setCellFactory(column -> new TableCell<>() {
            private final NumberFormat nf = NumberFormat.getNumberInstance();

            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    nf.setMaximumFractionDigits(2);
                    nf.setMinimumFractionDigits(2);
                    setText(nf.format(item));
                }
            }
        });
    }

    private static StrategyWrapper getStrategyWrapper(String patternTmp, String historyTmp) {
        StrategyWrapper sw = new StrategyWrapper();
        sw.parameters = new HashMap<>();
        sw.parameters.put("direction", "long");
        sw.parameters.put("holdTime", 7); // todo change holdtime here if needed
        sw.parameters.put("positionSize", 1);
        sw.parameters.put("barPattern", patternTmp);
        sw.parameters.put("recordUpDown", historyTmp);
        return sw;
    }

    public static List<String> getHistoryToTest(String inVal) {
        List<String> historyToTest = new ArrayList<>();
        historyToTest.add(""); // default case

        if (inVal.contains("1")) {
            historyToTest.add("u");
            historyToTest.add("d");
        }

        if (inVal.contains("2")) {
            historyToTest.add("uu");
            historyToTest.add("ud");
            historyToTest.add("du");
            historyToTest.add("dd");
        }

        if (inVal.contains("3")) {
            historyToTest.add("uuu");
            historyToTest.add("uud");
            historyToTest.add("udu");
            historyToTest.add("udd");
            historyToTest.add("duu");
            historyToTest.add("dud");
            historyToTest.add("ddu");
            historyToTest.add("ddd");
        }

        if (inVal.contains("4")) {
            historyToTest.add("uuuu");
            historyToTest.add("uuud");
            historyToTest.add("uudu");
            historyToTest.add("uudd");
            historyToTest.add("uduu");
            historyToTest.add("udud");
            historyToTest.add("uddu");
            historyToTest.add("uddd");
            historyToTest.add("duuu");
            historyToTest.add("duud");
            historyToTest.add("dudu");
            historyToTest.add("dudd");
            historyToTest.add("dduu");
            historyToTest.add("ddud");
            historyToTest.add("dddu");
            historyToTest.add("dddd");
        }

        return historyToTest;
    }

//    public static ObservableList<String> getStockPriceDataFilenames() {
//        File folder = new File(STOCKPRICEDATA_PATHNAME);
//        String[] filenames = folder.list();
//        return FXCollections.observableArrayList(filenames);
//    }

}
