package com.twadeclark.tailwindtrader;

// __ to create standalone jar file __
//
// open Terminal window, run this:
//      mvn clean package
//
// this file:
//      TailWindTrader-1.0-SNAPSHOT-jar-with-dependencies.jar
// will be in this folder:
//      C:\Users\twade\git\TailWindTrader\target
//  copy the jar file to C:\Users\twade\git\TailWindTrader where data, results, strategy folders are
//
// to run the app make a batch file called run.bat with this:
//      java --module-path "C:\Program Files\Java\javafx-sdk-19\lib" --add-modules javafx.controls,javafx.fxml -jar TailWindTrader-1.0-SNAPSHOT-jar-with-dependencies.jar
//



import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import net.jacobpeterson.alpaca.AlpacaAPI;
import net.jacobpeterson.alpaca.model.endpoint.assets.Asset;
import net.jacobpeterson.alpaca.rest.AlpacaClientException;

import javax.swing.*;
import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import javafx.collections.ListChangeListener;

import java.sql.DriverManager;
import java.sql.Connection;

public class HelloController implements Initializable {
    private final int TickerHistoryLength = 10000;

    AlpacaAPI alpacaAPI = new AlpacaAPI();
    private DataFeedManagerInterface dataFeedManager;
    private TextAreaHolder LoadoutChecklistTattler;
    private TextAreaHolder OrderManagerTattler;
    private TextAreaHolder StrategyManagerTattler;
    private TextAreaHolder DataFeedManagerTattler;
    private AnimationTimer timer;
    public volatile Long[] lastEventNanoTime = new Long[1];
    private long lastUpdateTime;


    // Trade tab
    @FXML
    private Label welcomeText;
    @FXML
    private Button StartButton;
    @FXML
    private Button StopButton;
    @FXML
    private Button BrokerAssetsButton;
    @FXML
    private Label lastBarLabel;
    @FXML
    private TextArea LoadoutChecklistTextArea;
    @FXML
    private TextArea OrderManagerTextArea;
    @FXML
    private TextArea StrategyManagerTextArea;
    @FXML
    private TextArea DataFeedManagerTextArea;
    @FXML
    private TextArea ScratchTextArea;


    // Load tab
    @FXML
    private TextField TickerToLoad;
    @FXML
    private ComboBox HistoryLength;
    @FXML
    private ComboBox BrokerSelect;
    @FXML
    private Button LoadDataFromBrokerButton;
    @FXML
    private TextArea LoadedDataPreview;
    @FXML
    private TextField PctStratsToKeepTextField;
    @FXML
    private Button BulkRunButton;
    @FXML
    private Button StopBulkRunButton;
    @FXML
    private TextField BulkRunUpdateTextField;
    @FXML
    private ProgressBar loadProgressBar;


    // Bulk tab
    BackTestViewer backTestViewer = new BackTestViewer();
    @FXML
    private ListView SavedPriceDataFilesListView;
    @FXML
    private VBox BulkRunVBox;
    @FXML
    private ListView ResultsFilesListView;
    @FXML
    private TextArea GenerateStrategyFileTextArea;
    @FXML
    private ComboBox SelectBrokerComboBox;


    // Backtest tab
    @FXML
    private Button RunBacktestButton;
    @FXML
    private Button StopBacktestButton;
    @FXML
    private Button ShowFileListButton;
    @FXML
    private Button LoadFileButton;
    @FXML
    private Button ClearLoadButton;
    @FXML
    private Button AddPatternToListButton;
    @FXML
    private Button RemovePatternFromListButton;
    @FXML
    private Button GeneratePatternsForConfigFileButton;
    @FXML
    private TextArea ResultsTextArea;
    @FXML
    private TextArea LoadFileDetailsTextArea;
    @FXML
    private ListView FileSelectListView;
    @FXML
    private ListView PatternQtyListView;
    @FXML
    private ListView PatternsForConfigFileListView;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private TextField TopPatternsToRun;
    @FXML
    private ComboBox HistoryLengthCB;
    @FXML
    private TableView BigResultsTableView;
    @FXML
    private TableView TradeLogTableView;
    @FXML
    private ScatterChart PnLScatterChart;
    @FXML
    private LineChart PnLlineChart;
    @FXML
    private TextField Security;


    // View tab
    @FXML
    private ListView BulkResultsFilesListView;
    @FXML
    private TableView BulkResultsTableView;
    @FXML
    private TableView BulkResultsCompareTableView;
    @FXML
    private StackedBarChart VisualizedStackedBarChart;
    @FXML
    private CheckBox FilterResultsCheckBox;
    @FXML
    private CheckBox FilterResultsHide18CheckBox;
    @FXML
    private TextField FinalDecileTotalTextField;
    @FXML
    private TextField TotalTradesTextField;
    @FXML
    private ScatterChart<Number, Number> BulkViewScatterChart;
//    @FXML
//    private BarChart VisualizedBarChart;
    @FXML
    private TableView BulkResultsScatterTableView;
    @FXML
    private TableView<BulkResultsDataModel> myTable;
    @FXML
    private TextField SortingBinsTextField;


    // VIEW BULK BACKTEST CONTROLS

    @FXML
    private void onGenerateStrategyFileButtonClick () {
        String broker = SelectBrokerComboBox.getValue().toString();
        ObservableList<String> configFileList = ResultsFilesListView.getSelectionModel().getSelectedItems();
        backTest.saveTopPctInTickerStrategyFile(broker, configFileList, GenerateStrategyFileTextArea);
    }

    @FXML
    private void RefreshResultsFilesTableView () {
        ObservableList<String> filenames = FileUtilities.getFilenames(FileUtilities.RESULTS_PATHNAME);
        ResultsFilesListView.setItems(filenames);
        ResultsFilesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    private static void setColumnsForBulkResultsScatterTableView(TableView bigResultsTableView) { // right side on View tab
        TableColumn<BulkResultsDataModel, Integer> startColumn = new TableColumn<>("start");
        startColumn.setCellValueFactory(new PropertyValueFactory<>("start"));

        TableColumn<BulkResultsDataModel, Integer> endColumn = new TableColumn<>("end");
        endColumn.setCellValueFactory(new PropertyValueFactory<>("end"));

        TableColumn<BulkResultsDataModel, Integer> tradeTotalColumn = new TableColumn<>("tradeTotal");
        tradeTotalColumn.setCellValueFactory(new PropertyValueFactory<>("tradeTotal"));

        TableColumn<BulkResultsDataModel, Double> finalPnLsumColumn = new TableColumn<>("finalPnLsum");
        finalPnLsumColumn.setCellValueFactory(new PropertyValueFactory<>("finalPnLsum"));

        TableColumn<BulkResultsDataModel, Double> pnLperTradeColumn = new TableColumn<>("pnLperTrade");
        pnLperTradeColumn.setCellValueFactory(new PropertyValueFactory<>("pnLperTrade"));

        bigResultsTableView.getColumns().addAll(startColumn, endColumn, tradeTotalColumn, finalPnLsumColumn, pnLperTradeColumn);
    }

    @FXML
    private void onVisualizedBarChartClicked () {
        Object o = BulkResultsTableView.getSortOrder();

        if (!o.toString().equals("[]")) { // gross
            TableColumn<BacktestResult, ?> mainSortColumn = (TableColumn<BacktestResult, ?>) BulkResultsTableView.getSortOrder().get(0);
            redrawBulkViewBarChart(mainSortColumn.getSortType());
        }
    }

    @FXML
    private void onBulkResultsScatterTableViewClicked () {
        Object o = BulkResultsTableView.getSortOrder();
        String s = o.toString();

        if (!s.equals("[]")) { // gross
            TableColumn<BacktestResult, ?> mainSortColumn = (TableColumn<BacktestResult, ?>) BulkResultsTableView.getSortOrder().get(0);
            redrawBulkViewBarChart(mainSortColumn.getSortType());
        }
    }

    private void redrawBulkViewBarChart(TableColumn.SortType column) {
        if (BulkResultsScatterTableView.getColumns().size() == 0) { setColumnsForBulkResultsScatterTableView(BulkResultsScatterTableView); }
        BulkResultsScatterTableView.getItems().clear();

        List<BacktestResult> results = new ArrayList<>(BulkResultsTableView.getItems());
        ObservableList<BulkResultsDataModel> data = FXCollections.observableArrayList();

        int Num_Bars = 12;
        try { Num_Bars = Integer.parseInt(SortingBinsTextField.getText()); } catch (NumberFormatException e) {}

        int size = results.size();
        int partitionSize = size / Num_Bars;

        for (int i = 0; i < Num_Bars; i++) {
            int start = i * partitionSize;
            int end = (i == Num_Bars - 1) ? size : start + partitionSize;
            List<BacktestResult> sublist = results.subList(start, end);
            double finalPnLsum = sublist.stream().mapToDouble(BacktestResult::getFinalDecilePnL).sum();

            int upTradeSum = sublist.stream().mapToInt(BacktestResult::getUpTrades).sum();
            int downTradeSum = sublist.stream().mapToInt(BacktestResult::getDownTrades).sum();

            data.add(new BulkResultsDataModel(start, end, upTradeSum+downTradeSum, finalPnLsum, finalPnLsum / ((double)(upTradeSum+downTradeSum) / 10.0d) ));

//            XYChart.Series series = new XYChart.Series();
//            String s = "";
//            s = (i * (100/Num_Bars)) + " - " + ((i + 1) * (100/Num_Bars));
//            s = start + "-" + end;

//            series.setName(s);
//            series.getData().add(new XYChart.Data(sum, s));
//            data.add(new MyDataModel(start, end, upTradeSum+downTradeSum, finalPnLsum, 1.0d )); // start, end, tradeTotal, finalPnLsum, PnLperTrade
//            VisualizedBarChart.getData().add(series);
        }

        BulkResultsScatterTableView.setItems(data);

//        for (Object o : VisualizedBarChart.getData()) {
//            XYChart.Series<Number, String> series = (XYChart.Series<Number, String>) o;
//
//            for (XYChart.Data<Number, String> data : series.getData()) {
//                Node node = data.getNode();
//
//                node.setOnMouseClicked(event -> {
//                    String clickedOn = series.getName();
//                    System.out.println("You clicked on: " + clickedOn);
//
//                    Integer startIndex = Integer.parseInt(clickedOn.split("-")[0]);
//                    Integer endIndex = Integer.parseInt(clickedOn.split("-")[1]);
//
//                    BulkResultsTableView.getSelectionModel().clearSelection();
//
//                    for (int i = startIndex; i <= endIndex; i++) {
//                        BulkResultsTableView.getSelectionModel().select(i);
//                    }
//
//                });
//            }
//        }
    }

    @FXML
    public void BulkResultsFilesButtonClick() {
        if (BulkResultsTableView.getColumns().size() == 0) {
            setColumnsForResultsTableView(BulkResultsTableView);
            setColumnsForResultsTableView(BulkResultsCompareTableView);

            BulkResultsTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

            BulkResultsTableView.getSelectionModel().getSelectedItems().addListener(new ListChangeListener<BacktestResult>() {
                @Override
                public void onChanged(Change<? extends BacktestResult> c) {
                    Double totalPnL = 0.0d;
                    Integer totalTrades = 0;
                    XYChart.Series<Number, Number> series = new XYChart.Series<>();

                    for (Object selectedItemTmp : BulkResultsTableView.getSelectionModel().getSelectedItems()) {
                        BacktestResult selectedItem = (BacktestResult)selectedItemTmp;
                        totalPnL += selectedItem.getFinalDecilePnL();
                        totalTrades += selectedItem.getDownTrades() + selectedItem.getUpTrades();

                        double xValue = selectedItem.getPctUpTrade();
                        double yValue = selectedItem.getFinalDecilePnL();
                        series.getData().add(new XYChart.Data<>(xValue, yValue));
                    }

                    FinalDecileTotalTextField.setText(totalPnL.toString());
                    TotalTradesTextField.setText(totalTrades.toString());

                    try {
                        Collections.reverse(series.getData());
                    } catch (Exception e) {
                        System.out.println("error with Collections.reverse(series.getData());");
                    }

                    BulkViewScatterChart.getData().clear();
                    BulkViewScatterChart.getData().addAll(series);
                    series.setName("Bulk Run");
                }
            });
        }

        BulkResultsFilesListView.setItems(FileUtilities.getFilenames(FileUtilities.RESULTS_PATHNAME));

        Map<String, ArrayList<BacktestResult>> backtestResultsMap = new HashMap<>();

        BulkResultsFilesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            ArrayList<BacktestResult> backtestResults = FileUtilities.bulkResultsFilesSelected(newSelection.toString(), backtestResultsMap);

            if (FilterResultsCheckBox.isSelected()) {
                // weeds out the patterns with not much history
                backtestResults = (ArrayList<BacktestResult>) backtestResults.stream()
                        .filter(result -> (result.getUpDeciles() + result.getDownDeciles()) == 10)
                        .collect(Collectors.toList());
            }

            if (FilterResultsHide18CheckBox.isSelected()) {
                // weeds out pattern 1 and pattern 8
                backtestResults = (ArrayList<BacktestResult>) backtestResults.stream()
                        .filter(result -> (!Objects.equals(result.getPattern(), "1") && !Objects.equals(result.getPattern(), "8")) )
                        .collect(Collectors.toList());
            }

            ObservableList<BacktestResult> backtestResultObservableList = FXCollections.observableArrayList();
            backtestResultObservableList.addAll(backtestResults);
            BulkResultsTableView.setItems(backtestResultObservableList);
        });

        BulkResultsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            try {
                BacktestResult btr = (BacktestResult) newSelection;
                if (newSelection != null) {
                    Map<String, BacktestResult> filteredMapByPatternHistory = backTestViewer.filterMapByPatternHistory(btr.getPatternHistory(), backtestResultsMap);
                    ObservableList<BacktestResult> backtestResultObservableList = backTestViewer.createObservableList(filteredMapByPatternHistory);
                    BulkResultsCompareTableView.setItems(backtestResultObservableList);

                    // Get the sort order of BulkResultsTableView
                    ObservableList<TableColumn<BacktestResult, ?>> sortOrder = BulkResultsTableView.getSortOrder();

                    // Map to the corresponding columns in BulkResultsCompareTableView and set the sort order
                    BulkResultsCompareTableView.getSortOrder().setAll(
                            sortOrder.stream()
                                    .map(column -> {
                                        TableColumn<BacktestResult, ?> correspondingColumn = getColumnByName(BulkResultsCompareTableView, column.getText());
                                        correspondingColumn.setSortType(column.getSortType());
                                        return correspondingColumn;
                                    })
                                    .collect(Collectors.toList())
                    );

                    // Trigger the sorting
                    BulkResultsCompareTableView.sort();

                    CategoryAxis xAxis = new CategoryAxis();
                    NumberAxis yAxis = new NumberAxis();
                    xAxis.setLabel("Quarters");

                    //Creating the StackedBar chart.
                    VisualizedStackedBarChart.setTitle("Ups & Downs");
                    VisualizedStackedBarChart.getData().clear();
                    VisualizedStackedBarChart.setAnimated(false);

                    XYChart.Series<String, Number> seriesDown = new XYChart.Series<String, Number>();
                    XYChart.Series<String, Number> seriesUp = new XYChart.Series<String, Number>();
                    seriesDown.setName("Down");
                    seriesUp.setName("Up");

                    seriesDown.getData().add(new XYChart.Data<String, Number>("Trades", 1000 * (1 - btr.getPctUpTrade())));
                    seriesDown.getData().add(new XYChart.Data<String, Number>("Deciles", 100 * btr.getDownDeciles()));

                    seriesUp.getData().add(new XYChart.Data<String, Number>("Trades", 1000 * btr.getPctUpTrade()));
                    seriesUp.getData().add(new XYChart.Data<String, Number>("Deciles", 100 * btr.getUpDeciles()));


                    int posPnL = 0;
                    int negPnL = 0;

                    for (BacktestResult btrTmp:backtestResultObservableList) {
                        if (btrTmp.getPnL() > 0) {
                            posPnL++;
                        } else if (btrTmp.getPnL() < 0) {
                            negPnL++;
                        }
                    }

                    seriesDown.getData().add(new XYChart.Data<String, Number>("PnL", 1000 * ((float)negPnL / ((float)posPnL + (float)negPnL)) ));
                    seriesUp.getData().add(new XYChart.Data<String, Number>("PnL", 1000 * ((float)posPnL / ((float)posPnL + (float)negPnL)) ));

                    //Adding data to StackedBarChart
                    VisualizedStackedBarChart.getData().addAll(seriesDown, seriesUp);
                }
            } catch (Exception e) {
                System.out.println(e);
            }

        });
    }

    public TableColumn<BacktestResult, ?> getColumnByName(TableView<BacktestResult> tableView, String name) {
        for (TableColumn<BacktestResult, ?> col : tableView.getColumns()) {
            if (col.getText().equals(name)) {
                return col;
            }
        }
        return null;
    }


    // BACKTEST CONTROLS

    BackTest backTest = new BackTest();

    @FXML
    public void TopTiersButtonClick() {
        double pctStratsToKeepTextField = Double.parseDouble(PctStratsToKeepTextField.getText());
        ObservableList<BacktestResult> backtestResultObservableList = BigResultsTableView.getItems();
        backTest.getTopTiersPatternList(pctStratsToKeepTextField, backtestResultObservableList, PatternsForConfigFileListView);
    }

    @FXML
    public void onGeneratePatternsForConfigFileButtonClick() {
        backTest.savePatternsInTickerStrategyFile(Security.getText(), PatternsForConfigFileListView.getItems(), LoadFileDetailsTextArea);
    }

    @FXML
    public void onAddPatternToListButtonClick() {
        BacktestResult btr = (BacktestResult) BigResultsTableView.getSelectionModel().getSelectedItem();
        String selectedPattern = btr.getPatternHistory();
        String selectedPatternToString = btr.getPnL() > 0 ? "long - " : "short - ";
        selectedPatternToString += selectedPattern;

        if (!PatternsForConfigFileListView.getItems().contains(selectedPatternToString)) {
            PatternsForConfigFileListView.getItems().add(selectedPatternToString);
        }
    }

    @FXML
    public void onRemovePatternFromListButtonClick() {
        Object o = PatternsForConfigFileListView.getSelectionModel().getSelectedItem();
        PatternsForConfigFileListView.getItems().remove(o);
        PatternsForConfigFileListView.getSelectionModel().clearSelection();
    }

    @FXML
    public void onShowFileListButtonClick() {
        ObservableList<String> items = FileUtilities.getFilenames(FileUtilities.STOCKPRICEDATA_PATHNAME);
        FileSelectListView.setItems(items);
        RunBacktestButton.setOnAction(this::runBacktest);
    }

    @FXML
    public void onClearLoadButtonClick() {
        int choice = JOptionPane.showConfirmDialog(null, "Do you want to proceed?", "Confirmation", JOptionPane.YES_NO_OPTION);

        if (choice == JOptionPane.YES_OPTION) {
            LoadFileButton.setDisable(false);
            ClearLoadButton.setDisable(true);

            PnLScatterChart.getData().clear();
            PnLlineChart.getData().clear();
            PatternQtyListView.setItems(null);
            TradeLogTableView.setItems(null);
        }
    }

    private CircularBuffer<StockData> loadedFileFullDataCircularBuffer;

    @FXML
    public void onLoadFileButtonClick() {
        try {
            long timeStart = System.currentTimeMillis();

            LoadFileButton.setDisable(true);
            ClearLoadButton.setDisable(false);

            String selectedFile = (String) FileSelectListView.getSelectionModel().getSelectedItem();
            if (selectedFile == null || selectedFile.equals("")) {
                return;
            }
            LoadFileDetailsTextArea.setText(selectedFile + "\n");
            Security.setText(selectedFile.split("_")[0]);

            Map<String, Integer> patternAggregate = new HashMap<>();

            loadedFileFullDataCircularBuffer = FileUtilities.loadFileIntoBuffer(selectedFile, patternAggregate);
            LoadFileDetailsTextArea.appendText("load file run time: " + (System.currentTimeMillis() - timeStart) + " milliseconds" + "\n");
            LoadFileDetailsTextArea.appendText("Total patterns: " + patternAggregate.size() + "\n");

            List<Map.Entry<String, Integer>> sortedEntries = patternAggregate.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(Collectors.toList());

            ObservableList<Map.Entry<String, Integer>> items = FXCollections.observableArrayList(sortedEntries);
            PatternQtyListView.setItems(items);
        } catch (Exception e) {
            ResultsTextArea.appendText("\n\n" + e);
        }
    }

    private void runBacktest(ActionEvent event) {
        try {
            int topFewPatterns = Math.min(PatternQtyListView.getItems().size(), Integer.parseInt(TopPatternsToRun.getText()));
            List<String> patternsToTest = new ArrayList<>();
            patternsToTest.addAll(PatternQtyListView.getItems().subList(0, topFewPatterns));

            List<String> historyToTest = BackTest.getHistoryToTest(HistoryLengthCB.getValue().toString());

            quickProcessStrategy(Security.getText(), loadedFileFullDataCircularBuffer, patternsToTest, historyToTest);
        } catch (Exception e) {
            ResultsTextArea.appendText("\n\n" + e);
        }
    }

    private Map<String, ArrayList<BacktestTrade>> backtestTradeMap = new HashMap<>();

    private void quickProcessStrategy(String security, CircularBuffer loadedFileFullDataCircularBuffer, List<String> patternsToTest, List<String> historyToTest) {
        RunBacktestButton.setDisable(true);
        StopBacktestButton.setDisable(false);

        ArrayList<BacktestResult> backtestResults = new ArrayList<>();

        Task<StringBuilder> task = backTest.quickProcessStrategyTask(security, loadedFileFullDataCircularBuffer, patternsToTest, historyToTest, backtestTradeMap, backtestResults, true, BulkRunUpdateTextField);

        task.setOnSucceeded(event -> {
            RunBacktestButton.setDisable(false);
            StopBacktestButton.setDisable(true);

            StringBuilder sb = task.getValue();
            ResultsTextArea.appendText(sb.toString());

            fillBigResultsTableView(backtestResults);
        });

        StopBacktestButton.setOnAction(actionEvent -> {
            backTest.ProcessStrategyRun = false;
        });

        progressBar.progressProperty().bind(task.progressProperty());
        backTest.ProcessStrategyRun = true;

        new Thread(task).start();
    }

    private void fillBigResultsTableView(ArrayList<BacktestResult> backtestResults) {
        if (BigResultsTableView.getColumns().size() == 0) { // todo LEFT side column list
            setColumnsForResultsTableView(BigResultsTableView);

            BigResultsTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    try {
                        BackTest.loadTradeLogTableView((BacktestResult) newSelection, TradeLogTableView, backtestTradeMap);
                        BackTest.loadDetailsGraph((BacktestResult) newSelection, backtestTradeMap, PnLScatterChart, PnLlineChart);
                    } catch (Exception e) {
                        LoadedDataPreview.appendText("\n\n" + e.toString());
                    }
                }
            });

            PatternQtyListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    try {
                        String pattern = newSelection.toString().split("=")[0];

                        List<String> patternsToTest = new ArrayList<>();
                        patternsToTest.add(pattern);
                        List<String> historyToTest = BackTest.getHistoryToTest(HistoryLengthCB.getValue().toString());

                        quickProcessStrategy(Security.getText(), loadedFileFullDataCircularBuffer, patternsToTest, historyToTest);
                    } catch (Exception e) {
                        LoadedDataPreview.appendText("\n\n" + e);
                    }
                }
            });

            PatternsForConfigFileListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    try {
                        String pattern = newSelection.toString().split(" - ")[1];

                        for (Object o : BigResultsTableView.getItems()) {
                            BacktestResult btr = (BacktestResult) o;
                            if (btr.getPatternHistory().equals(pattern)) {
                                BigResultsTableView.getSelectionModel().select(o);
                                BigResultsTableView.scrollTo(o);
                                break;
                            }
                        }


                    } catch (Exception e) {
                        LoadedDataPreview.appendText("\n\n" + e.toString());
                    }
                }
            });

            BigResultsTableView.setOnKeyPressed((KeyEvent keyEvent) -> {
                if (keyEvent.getCode() == KeyCode.ENTER) {
                    onAddPatternToListButtonClick();
                }
            });

        }

        ObservableList<BacktestResult> backtestResultObservableList = FXCollections.observableArrayList();
        backtestResultObservableList.addAll(backtestResults);
        BigResultsTableView.setItems(backtestResultObservableList);
    }

    private static void setColumnsForResultsTableView(TableView bigResultsTableView) {  // todo LEFT side column list
        TableColumn<BacktestResult, String> patternHistoryColumn = new TableColumn<>("PatternHistory");
        patternHistoryColumn.setCellValueFactory(new PropertyValueFactory<>("PatternHistory"));

        TableColumn<BacktestResult, Integer> upTradesColumn = new TableColumn<>("UpTrades");
        upTradesColumn.setCellValueFactory(new PropertyValueFactory<>("UpTrades"));

        TableColumn<BacktestResult, Integer> downTradesColumn = new TableColumn<>("DownTrades");
        downTradesColumn.setCellValueFactory(new PropertyValueFactory<>("DownTrades"));

        TableColumn<BacktestResult, Double> pctUpTradeColumn = new TableColumn<>("PctUpTrade");
        pctUpTradeColumn.setCellValueFactory(new PropertyValueFactory<>("PctUpTrade"));
        BackTest.setThreeDigitPrecisionForBacktestResult(pctUpTradeColumn);

        TableColumn<BacktestResult, Double> PnLColumn = new TableColumn<>("PnL");
        PnLColumn.setCellValueFactory(new PropertyValueFactory<>("PnL"));
        BackTest.setThreeDigitPrecisionForBacktestResult(PnLColumn);

        TableColumn<BacktestResult, Double> PnLperTradeColumn = new TableColumn<>("PnLperTrade");
        PnLperTradeColumn.setCellValueFactory(new PropertyValueFactory<>("PnLperTrade"));
        BackTest.setThreeDigitPrecisionForBacktestResult(PnLperTradeColumn);

        TableColumn<BacktestResult, Integer> UpDecilesColumn = new TableColumn<>("UpDeciles");
        UpDecilesColumn.setCellValueFactory(new PropertyValueFactory<>("UpDeciles"));

        TableColumn<BacktestResult, Integer> DownDecilesColumn = new TableColumn<>("DownDeciles");
        DownDecilesColumn.setCellValueFactory(new PropertyValueFactory<>("DownDeciles"));

        TableColumn<BacktestResult, Integer> FinalDecilePnLColumn = new TableColumn<>("FinalDecilePnL");
        FinalDecilePnLColumn.setCellValueFactory(new PropertyValueFactory<>("FinalDecilePnL"));

//            TableColumn<BacktestResult, Double> CoVarColumn = new TableColumn<>("CoVar");
//            CoVarColumn.setCellValueFactory(new PropertyValueFactory<>("CoVar"));
//            setThreeDigitPrecisionForBacktestResult(CoVarColumn);
//
//            TableColumn<BacktestResult, Double> VarianceColumn = new TableColumn<>("Variance");
//            VarianceColumn.setCellValueFactory(new PropertyValueFactory<>("Variance"));
//            setThreeDigitPrecisionForBacktestResult(VarianceColumn);
//
//            TableColumn<BacktestResult, Double> MeanColumn = new TableColumn<>("Mean");
//            MeanColumn.setCellValueFactory(new PropertyValueFactory<>("Mean"));
//            setThreeDigitPrecisionForBacktestResult(MeanColumn);
//
//            TableColumn<BacktestResult, Double> StdDevColumn = new TableColumn<>("StdDev");
//            StdDevColumn.setCellValueFactory(new PropertyValueFactory<>("StdDev"));
//            setThreeDigitPrecisionForBacktestResult(StdDevColumn);
//
//            TableColumn<BacktestResult, Double> GeometricMeanColumn = new TableColumn<>("GeometricMean");
//            GeometricMeanColumn.setCellValueFactory(new PropertyValueFactory<>("GeometricMean"));
//            setThreeDigitPrecisionForBacktestResult(GeometricMeanColumn);
//
//            TableColumn<BacktestResult, Double> KurtosisColumn = new TableColumn<>("Kurtosis");
//            KurtosisColumn.setCellValueFactory(new PropertyValueFactory<>("Kurtosis"));
//            setThreeDigitPrecisionForBacktestResult(KurtosisColumn);
//
//            TableColumn<BacktestResult, Double> PopulationVarianceColumn = new TableColumn<>("PopulationVariance");
//            PopulationVarianceColumn.setCellValueFactory(new PropertyValueFactory<>("PopulationVariance"));
//            setThreeDigitPrecisionForBacktestResult(PopulationVarianceColumn);
//
//            TableColumn<BacktestResult, Double> QuadraticMeanColumn = new TableColumn<>("QuadraticMean");
//            QuadraticMeanColumn.setCellValueFactory(new PropertyValueFactory<>("QuadraticMean"));
//            setThreeDigitPrecisionForBacktestResult(QuadraticMeanColumn);
//
//            TableColumn<BacktestResult, Double> SkewnessColumn = new TableColumn<>("Skewness");
//            SkewnessColumn.setCellValueFactory(new PropertyValueFactory<>("Skewness"));
//            setThreeDigitPrecisionForBacktestResult(SkewnessColumn);


//            bigResultsTableView.getColumns().addAll(patternHistoryColumn, upTradesColumn, downTradesColumn, pctUpTradeColumn, PnLColumn, PnLperTradeColumn,
//                    UpDecilesColumn, DownDecilesColumn, CoVarColumn, VarianceColumn, MeanColumn, StdDevColumn,
//                    GeometricMeanColumn, KurtosisColumn, PopulationVarianceColumn, QuadraticMeanColumn, SkewnessColumn);
        bigResultsTableView.getColumns().addAll(patternHistoryColumn, upTradesColumn, downTradesColumn, pctUpTradeColumn, PnLColumn, PnLperTradeColumn,
                UpDecilesColumn, DownDecilesColumn, FinalDecilePnLColumn);
    }


    // LOAD CONTROLS

    @FXML
    public void BulkRunButtonClick() {
        BulkRunButton.setDisable(true);
        backTest.ProcessStrategyRun = true;
        ObservableList<String> selectedItems = SavedPriceDataFilesListView.getSelectionModel().getSelectedItems();

        // Setup tasks
        ExecutorService executor = Executors.newFixedThreadPool(12); // the number of threads
        BulkRunVBox.getChildren().clear();

        for (Object o : selectedItems) {
            String selectedFile = o.toString();
            TextField myTF = new TextField();
            BulkRunVBox.getChildren().add(myTF);
            myTF.setText("..waiting to run.. " + selectedFile);

            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    // load file into circ buff
                    Platform.runLater(() -> {
                        myTF.setText("loading file: " + selectedFile);
                    });

                    Map<String, Integer> patternAggregate = new HashMap<>();
                    CircularBuffer<StockData> loadedFileFullDataCircularBufferForBulkRun = FileUtilities.loadFileIntoBuffer(selectedFile, patternAggregate);

                    // get history length
                    List<String> historyToTest = BackTest.getHistoryToTest("123");

                    // run backtest
                    String security = selectedFile.split("_")[0];
                    Map<String, ArrayList<BacktestTrade>> backtestTradeMap = new HashMap<>();
                    ArrayList<BacktestResult> backtestResults = new ArrayList<>();

                    Boolean detailedMessages = false;

                    Task<StringBuilder> processTask = backTest.quickProcessStrategyTask(
                            security,
                            loadedFileFullDataCircularBufferForBulkRun,
                            new ArrayList<>(patternAggregate.keySet()),
                            historyToTest,
                            backtestTradeMap,
                            backtestResults,
                            detailedMessages,
                            myTF
                    );

                    processTask.setOnSucceeded(event -> {
                        StringBuilder sb = processTask.getValue();
                    });

                    Thread processThread = new Thread(processTask);
                    processThread.start();
                    processThread.join(); // Wait for task to complete

                    // save bulk results
                    backTest.saveBulkResults(backtestResults, security);

                    return null;
                }
            };

            executor.submit(task);
        }

        executor.shutdown(); // Always remember to shutdown executor when you're done with it
    }

    @FXML
    public void onRefreshSavedPriceDataButtonClick() {
        ObservableList<String> filenames = FileUtilities.getFilenames("data");

        SavedPriceDataFilesListView.setItems(filenames);
        SavedPriceDataFilesListView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    }

    @FXML
    public void StopBulkRunButtonClick() {
        backTest.ProcessStrategyRun = false;
    }

    @FXML
    public void onLoadDataFromBrokerButtonClick() throws SQLException {

        String jdbcUrl = "jdbc:sqlite:/contacts.db";
        Connection connection = DriverManager.getConnection(jdbcUrl);


        if (true)
        return;




        long timeStart = System.currentTimeMillis();

        Task<Long> task = new Task<>() {
            @Override
            protected Long call() {
                try {
                    if (alpacaAPI == null) {
                        alpacaAPI = new AlpacaAPI();
                    }

                    if (TickerToLoad.getText().equals("")) {
                        return 0L;
                    }

                    HistoryLoaderInterface historyLoader;
                    if (BrokerSelect.getValue().equals("Alpaca Stock")) {
                        historyLoader = new HistoryLoader_Alpaca_Stocks(alpacaAPI);
                    } else if (BrokerSelect.getValue().equals("Alpaca Crypto")) {
                        historyLoader = new HistoryLoader_Alpaca_Crypto(alpacaAPI);
                    } else {
                        throw new RuntimeException("!!! BROKER NOT FOUND !!! ");
                    }

                    int histLen = Integer.parseInt(HistoryLength.getValue().toString()) * 31 * 24 * 60; // max possible qty of minutes

                    String[] tickerLoop = new String[]{TickerToLoad.getText()};

                    if (TickerToLoad.getText().contains(",")) {
                        tickerLoop = TickerToLoad.getText().split(",");
                    }

                    for (String thisTicker : tickerLoop) {
                        thisTicker = thisTicker.replace("/", "");
                        CircularBuffer<StockData> stockDataCB = new CircularBuffer<>(histLen);
                        ZonedDateTime dateFrom = ZonedDateTime.now().minusMonths(Integer.parseInt(HistoryLength.getValue().toString()));

                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                        String formattedDateTime = formatter.format(dateFrom);
                        String filename = "data/" + thisTicker + "_" + formattedDateTime + ".csv";

                        Platform.runLater(() -> {
                            LoadedDataPreview.appendText(filename + "\n");
                            LoadedDataPreview.setScrollTop(Double.MAX_VALUE);
                        });

                        int recordsLoaded = 0;

                        for (int loadTmp = 0; loadTmp < histLen / 10000; loadTmp++) {
                            try {
                                recordsLoaded += historyLoader.loadHistoryForSaving(thisTicker, stockDataCB, dateFrom);
                            } catch (Exception e) {
                                LoadedDataPreview.appendText("    error loading stock: " + thisTicker + "\n    " + e + "\n");
                                LoadedDataPreview.setScrollTop(Double.MAX_VALUE);
                                break;
                            }

                            String s1 = "dateFrom=" + dateFrom + "    timestamp=" + stockDataCB.get(0).timestamp + "    recordsLoaded=" + recordsLoaded + "\n";

                            Platform.runLater(() -> {
                                LoadedDataPreview.appendText(s1);
                                LoadedDataPreview.setScrollTop(Double.MAX_VALUE);
                            });

                            if (dateFrom.toEpochSecond() == stockDataCB.get(0).timestamp.toEpochSecond()) {
                                break;
                            }

                            dateFrom = stockDataCB.get(0).timestamp;
                        }

                        if (recordsLoaded > 0) {
                            StringBuilder csvFormattedData = new StringBuilder();

                            for (int i = stockDataCB.Size() - 1; i >= 0; i--) {
                                if (stockDataCB.get(i) != null) {
                                    csvFormattedData.append(stockDataCB.get(i).timestamp).append(",");
                                    csvFormattedData.append(stockDataCB.get(i).close).append(",");
                                    csvFormattedData.append(stockDataCB.get(i).high).append(",");
                                    csvFormattedData.append(stockDataCB.get(i).low).append("\n");
                                }
                            }

                            FileUtilities.writeStringToFile(filename, csvFormattedData.toString());

                            Platform.runLater(() -> {
                                LoadedDataPreview.appendText("File written successfully: " + filename + "\n\n");
                                LoadedDataPreview.setScrollTop(Double.MAX_VALUE);
                            });
                        } else {
                            Platform.runLater(() -> {
                                LoadedDataPreview.appendText("File skipped. No results returned.\n\n");
                                LoadedDataPreview.setScrollTop(Double.MAX_VALUE);
                            });
                        }

                    }
                } catch (RuntimeException | IOException e) {
                    LoadedDataPreview.appendText("\n" + e + "\n");
                }

                return System.currentTimeMillis() - timeStart;
            }
        };

        task.setOnSucceeded(event -> {
            long returnedValue = task.getValue();
            LoadedDataPreview.appendText("total load time: " + returnedValue + " milliseconds" + "\n");
        });

        new Thread(task).start();
    }



    // TRADE CONTROLS
    @FXML
    public void onStartButtonClick() throws AlpacaClientException {
        loadAndTrade();
    }

    private void loadAndTrade() throws AlpacaClientException {
        StartButton.setDisable(true);
        StopButton.setDisable(false);

        alpacaAPI = new AlpacaAPI();

        LoadoutChecklistTattler = new TextAreaHolder(LoadoutChecklistTextArea);
        OrderManagerTattler = new TextAreaHolder(OrderManagerTextArea);
        StrategyManagerTattler = new TextAreaHolder(StrategyManagerTextArea);
        DataFeedManagerTattler = new TextAreaHolder(DataFeedManagerTextArea);

        LoadoutChecklistTattler.appendTrunc("account=" + alpacaAPI.account().get().getAccountNumber());

        // Load configuration
        ConfigurationManager tickerStrategyJson = new ConfigurationManager("TickerStrategy.json");
        LoadoutChecklistTattler.appendTrunc("strategy loaded for: " + tickerStrategyJson.getAllTickerSymbols());
        LoadoutChecklistTattler.appendTrunc("connecting to broker: " + tickerStrategyJson.Broker);

        HistoryLoaderInterface historyLoader;
        if (tickerStrategyJson.Broker.equals("Alpaca_Crypto")) {
            historyLoader = new HistoryLoader_Alpaca_Crypto(alpacaAPI);
        } else if (tickerStrategyJson.Broker.equals("Alpaca_Stocks")) {
            historyLoader = new HistoryLoader_Alpaca_Stocks(alpacaAPI);
        } else {
            throw new RuntimeException("!!! BROKER NOT FOUND !!! " + tickerStrategyJson.Broker);
        }

        // Initialize and populate Circular Buffers
        Map<String, CircularBuffer<StockData>> circularBuffers = new ConcurrentHashMap<>();
        int recordsLoaded;
        for (String security : tickerStrategyJson.getAllTickerSymbols()) {
            circularBuffers.put(security, new CircularBuffer<>(TickerHistoryLength));
            CircularBuffer<StockData> stockDataCB = circularBuffers.get(security);

            long loadTimeStart = System.currentTimeMillis();

            recordsLoaded = historyLoader.loadHistory(security, stockDataCB);

            if (recordsLoaded > 0 && stockDataCB.get(0) != null && stockDataCB.get(0).timestamp != null ) {
                LoadoutChecklistTattler.appendTrunc("load success: " + security + " load time: " + String.format("%.3f", (double)(System.currentTimeMillis()-loadTimeStart)/1000)  + " records: " + recordsLoaded + " through: " + stockDataCB.get(0).timestamp);
            } else if (recordsLoaded == 0) {
                LoadoutChecklistTattler.appendTrunc("!!! LOAD FAIL !!! NO RECORDS LOADED !!! " + security);
                throw new RuntimeException("!!! LOAD FAIL !!! NO RECORDS LOADED !!! " + security);
            }
        }

        // Initialize Order Manager
        OrderManager orderManager = new OrderManager(OrderManagerTattler, FileUtilities.TRADELOG_FILENAME);

        // Initialize Strategy Manager
        StrategyManager strategyManager = new StrategyManager(tickerStrategyJson, circularBuffers, orderManager, StrategyManagerTattler);

        // Initialize Data Feed Manager
        if (tickerStrategyJson.Broker.equals("Alpaca_Crypto")) {
            dataFeedManager = new DataFeedManager_Alpaca_Crypto(alpacaAPI, tickerStrategyJson, strategyManager, circularBuffers, lastEventNanoTime, DataFeedManagerTattler);
        } else if (tickerStrategyJson.Broker.equals("Alpaca_Stocks")) {
            dataFeedManager = new DataFeedManager_Alpaca_Stocks(alpacaAPI, tickerStrategyJson, strategyManager, circularBuffers, lastEventNanoTime, DataFeedManagerTattler);
        } else {
            throw new RuntimeException("!!! DATA FEED NOT FOUND !!! " + tickerStrategyJson.Broker);
        }

        dataFeedManager.connect();
        dataFeedManager.subscribe(tickerStrategyJson.getAllTickerSymbols());

        lastBarTimer(); // timer
    }

    private void lastBarTimer() {
        lastEventNanoTime[0] = System.nanoTime();
        lastUpdateTime = lastEventNanoTime[0];

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                if (now - lastUpdateTime >= 1_000_000_000) {
                    long secondsSinceLastEvent = (now - lastEventNanoTime[0]) / 1_000_000_000;
                    lastBarLabel.setText("last bar: " + Long.toString(secondsSinceLastEvent));
                    lastUpdateTime = now;
                }
            }
        };
        timer.start();
    }

    @FXML
    protected void onStopButtonClick() {
        StopButton.setDisable(true);
        shutdownSequence();
    }

    public void shutdownSequence() {
        try { dataFeedManager.disconnect(); } catch (Exception e) { }
        try { alpacaAPI.getOkHttpClient().dispatcher().executorService().shutdown(); } catch (Exception e) { }
        try { alpacaAPI.getOkHttpClient().connectionPool().evictAll(); } catch (Exception e) { }
        try { timer.stop(); } catch (Exception e) { }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
    }

    @FXML
    public void onBrokerAssetsButtonClick() throws AlpacaClientException {
        StringBuilder shortList = new StringBuilder();
        StringBuilder detailList = new StringBuilder();

        List<Asset> assetList = dataFeedManager.ActiveAssetsList();
        for (Asset a:assetList) {
            shortList.append(a.getSymbol()).append(",");
            detailList.append("symbol=").append(a.getSymbol()).append("\t");
            detailList.append("name=").append(a.getName()).append("\t");
            detailList.append("tradable=").append(a.getTradable()).append("\t");
            detailList.append("marginable=").append(a.getMarginable()).append("\t");
            detailList.append("shortable=").append(a.getShortable()).append("\t");
            detailList.append("assetClass=").append(a.getAssetClass()).append("\t");
            detailList.append("exchange=").append(a.getExchange()).append("\t");
            detailList.append("status=").append(a.getStatus()).append("\t");
            detailList.append("easyToBorrow=").append(a.getEasyToBorrow()).append("\t");
            detailList.append("fractionable=").append(a.getFractionable()).append("\n");
        }

        ScratchTextArea.setText(shortList.toString() + "\n");
        if (assetList.size() < 100) {
            ScratchTextArea.appendText(detailList.toString());
        }
    }

    public HelloController() {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                ScratchTextArea.setText("app started: " + ZonedDateTime.now());
            }
        });
    }

}

