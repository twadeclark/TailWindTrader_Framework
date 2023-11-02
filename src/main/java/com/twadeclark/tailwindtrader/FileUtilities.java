package com.twadeclark.tailwindtrader;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FileUtilities {

    public static final Path TRADELOG_FILENAME = Paths.get("trades.txt");
    public static final String STOCKPRICEDATA_PATHNAME = "data";
    public static final String RESULTS_PATHNAME = "results";

    public static ObservableList<String> getFilenames(String pathName) {
        File folder = new File(pathName);
        File[] listOfFiles = folder.listFiles();
        ObservableList<String> filenames = FXCollections.observableArrayList();

        for (File filenameTmp : listOfFiles) {
            if (filenameTmp.isFile()) {
                filenames.add(filenameTmp.getName());
            }
        }
        return filenames;
    }

    public static void writeStringToFile(String filename, String contents) throws IOException {
        File file = new File(filename);
        System.out.println(file.getAbsolutePath());

        FileWriter writer = new FileWriter(file, false);
        writer.write(contents);
        writer.close();
    }

    public static CircularBuffer<StockData> loadFileIntoBuffer(String selectedFile, Map<String, Integer> outPatternAggregate) throws IOException {
        Path datafile = Path.of(STOCKPRICEDATA_PATHNAME, selectedFile);
        CircularBuffer<StockData> loadedFileFullDataCircularBuffer = null;

        BufferedReader reader = new BufferedReader(new FileReader(datafile.toString()));
        int datafileLineCount = (int) Files.lines(datafile).count();
        //                    TextAreaTattler("File lines: " + datafileLineCount + "\n", LoadFileDetailsTextArea);
        loadedFileFullDataCircularBuffer = new CircularBuffer<>(datafileLineCount);

//        long fileSize = Files.size(datafile);
//        long bytesRead = 0;
        Integer lastPattern = 0, lastLastPattern = 0;

        String line;
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(",");
            ZonedDateTime timestamp = ZonedDateTime.parse(split[0]);
            double close = Double.parseDouble(split[1]);
            double high = Double.parseDouble(split[2]);
            double low = Double.parseDouble(split[3]);
            loadedFileFullDataCircularBuffer.add(timestamp, high, low, close);

            Integer thisPattern = 0;

            int cnt;
            String tmpPattern;

            tmpPattern = thisPattern.toString();
            cnt = outPatternAggregate.get(tmpPattern) == null ? 1 : (outPatternAggregate.get(tmpPattern) + 1);
            outPatternAggregate.put(tmpPattern, cnt);

            tmpPattern = lastPattern.toString() + tmpPattern;
            cnt = outPatternAggregate.get(tmpPattern) == null ? 1 : (outPatternAggregate.get(tmpPattern) + 1);
            outPatternAggregate.put(tmpPattern, cnt);

            tmpPattern = lastLastPattern.toString() + tmpPattern;
            cnt = outPatternAggregate.get(tmpPattern) == null ? 1 : (outPatternAggregate.get(tmpPattern) + 1);
            outPatternAggregate.put(tmpPattern, cnt);

            lastLastPattern = lastPattern;
            lastPattern = thisPattern;

//            bytesRead += line.getBytes().length + 1; // +1 for newline character
        }

        return loadedFileFullDataCircularBuffer;
    }

    public static ArrayList<BacktestResult> bulkResultsFilesSelected(String selectedFile, Map<String, ArrayList<BacktestResult>> backtestResultsMap) {
//        Map<String, ArrayList<BacktestResult>> backtestResultsMap = new HashMap<>();

        ArrayList<BacktestResult> backtestResults;

        if (backtestResultsMap.get(selectedFile) == null) {
            backtestResults = new ArrayList<>();
            Path datafile = Path.of(RESULTS_PATHNAME, selectedFile);

            try {
                BufferedReader reader = new BufferedReader(new FileReader(datafile.toString()));
                int cnt = 0;
                String line;
                reader.readLine(); // first line is column headers

                while ((line = reader.readLine()) != null) {
                    String[] split = line.split(",");

                    String PatternHistory = split[0];
                    int UpTrades = Integer.parseInt(split[1]);
                    int DownTrades = Integer.parseInt(split[2]);
//                    double PctUpTrade = Double.parseDouble(split[3]);
                    double PnL = Double.parseDouble(split[4]);
//                    double PnLperTrade = Double.parseDouble(split[5]);
                    int UpDeciles = Integer.parseInt(split[6]);
                    int DownDeciles = Integer.parseInt(split[7]);
                    double finalDecilePnL = Double.parseDouble(split[8]);

                    BacktestResult btr = new BacktestResult(cnt++, PatternHistory, UpTrades, DownTrades, PnL, UpDeciles, DownDeciles, finalDecilePnL);
                    backtestResults.add(btr);
                }

                backtestResultsMap.put(selectedFile, backtestResults);
            } catch (IOException e) {
                System.out.println(e);
            }
        } else {
            backtestResults = backtestResultsMap.get(selectedFile);
        }

        return backtestResults;
    }

    public static String loadTickerStrategies(String jsonFilePath, Map<String, List<StrategyWrapper>> tickerStrategies) {
        Gson gson = new Gson();
//        Map<String, List<StrategyWrapper>> strategies;
        String broker = "";

        try (FileReader reader = new FileReader(jsonFilePath)) {
            Type configType = new TypeToken<Map<String, Object>>() {}.getType();
            Map<String, Object> config = gson.fromJson(reader, configType);

            broker = ((List<String>) config.get("broker")).get(0);
            config.remove("broker");

            Map<String, List<StrategyWrapper>> tickerStrategiesTmp = gson.fromJson(new Gson().toJson(config), new TypeToken<Map<String, List<StrategyWrapper>>>() {}.getType());

            for (Map.Entry<String, List<StrategyWrapper>> entry : tickerStrategiesTmp.entrySet()) { // test this to make sure the right values get passed back
                tickerStrategies.put(entry.getKey(), entry.getValue());
            }

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load configuration file: " + jsonFilePath, e);
        }

        return broker;
    }

    public static ArrayList<BacktestResult> getBacktestResults(String datafile) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(RESULTS_PATHNAME + "\\" + datafile.toString()));
//        BufferedReader reader = new BufferedReader(new FileReader(Path.of(RESULTS_PATHNAME, datafile.toString()).toFile()));
        String line = reader.readLine(); // we have to burn the first line of the csv file that has the column headings
        int cnt = 0;

        ArrayList<BacktestResult> backtestResults = new ArrayList<>();

        // PatternHistory, UpTrades, DownTrades, PctUpTrade, PnL, PnLperTrade, UpDeciles, DownDeciles, FinalDecilePnL
        while ((line = reader.readLine()) != null) {
            String[] split = line.split(",");

            String PatternHistory = split[0];
            int UpTrades = Integer.parseInt(split[1]);
            int DownTrades = Integer.parseInt(split[2]);
//                    double PctUpTrade = Double.parseDouble(split[3]);
            double PnL = Double.parseDouble(split[4]);
//                    double PnLperTrade = Double.parseDouble(split[5]);
            int UpDeciles = Integer.parseInt(split[6]);
            int DownDeciles = Integer.parseInt(split[7]);
            double finalDecilePnL = Double.parseDouble(split[8]);

            BacktestResult btr = new BacktestResult(cnt++, PatternHistory, UpTrades, DownTrades, PnL, UpDeciles, DownDeciles, finalDecilePnL);
            backtestResults.add(btr);
        }
        return backtestResults;
    }

}
