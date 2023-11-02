package com.twadeclark.tailwindtrader;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Collectors;

public class BackTestViewer {

//    private Map<String, ArrayList<BacktestResult>> backtestResultsMap = new HashMap<>();


//    public Map<String, BacktestResult> filterMapByPatternHistory(String patternHistory) {
//        return backtestResultsMap.entrySet()
//                .stream()
//                .filter(entry -> entry.getValue().stream().anyMatch(backtestResult -> backtestResult.getPatternHistory().equals(patternHistory)))
//                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream().filter(backtestResult -> backtestResult.getPatternHistory().equals(patternHistory)).findFirst().orElse(null)));
//    }
    public Map<String, BacktestResult> filterMapByPatternHistory(String patternHistory, Map<String, ArrayList<BacktestResult>> backtestResultsMap) {
        return backtestResultsMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().stream().anyMatch(backtestResult -> backtestResult.getPatternHistory().equals(patternHistory)))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().stream()
                        .filter(backtestResult -> backtestResult.getPatternHistory().equals(patternHistory))
                        .map(BacktestResult::cloneWithoutPatternHistory)
                        .findFirst().orElse(null)));
    }

    public ObservableList<BacktestResult> createObservableList(Map<String, BacktestResult> map) {
        ObservableList<BacktestResult> list = FXCollections.observableArrayList();

        map.forEach((key, value) -> {
            value.setPatternHistory(key); // Overwrite the patternHistory
            list.add(value);
        });

        return list;
    }


//    public ArrayList<BacktestResult> bulkResultsFilesSelected(String selectedFile) {
////        String selectedFile = newSelection.toString();
//        ArrayList<BacktestResult> backtestResults;
//
//        if (backtestResultsMap.get(selectedFile) == null) {
//            backtestResults = new ArrayList<>();
//            Path datafile = Path.of("results", selectedFile);
//
//            try {
//                BufferedReader reader = new BufferedReader(new FileReader(datafile.toString()));
//                int cnt = 0;
//                String line;
//                reader.readLine(); // first line is column headers
//
//                while ((line = reader.readLine()) != null) {
//                    String[] split = line.split(",");
//
//                    String PatternHistory = split[0];
//                    int UpTrades = Integer.parseInt(split[1]);
//                    int DownTrades = Integer.parseInt(split[2]);
////                    double PctUpTrade = Double.parseDouble(split[3]);
//                    double PnL = Double.parseDouble(split[4]);
////                    double PnLperTrade = Double.parseDouble(split[5]);
//                    int UpDeciles = Integer.parseInt(split[6]);
//                    int DownDeciles = Integer.parseInt(split[7]);
//                    double finalDecilePnL = Double.parseDouble(split[8]);
//
//                    BacktestResult btr = new BacktestResult(cnt++, PatternHistory, UpTrades, DownTrades, PnL, UpDeciles, DownDeciles, finalDecilePnL);
//                    backtestResults.add(btr);
//                }
//
//                backtestResultsMap.put(selectedFile, backtestResults);
//            } catch (IOException e) {
//                System.out.println(e);
//            }
//        } else {
//            backtestResults = backtestResultsMap.get(selectedFile);
//        }
//
//        return backtestResults;
//    }

//    public ObservableList<String> getBulkBasktestFiles() {
//        ObservableList<String> filenames;
//        File folder = new File("results");
//        File[] listOfFiles = folder.listFiles();
//        filenames = FXCollections.observableArrayList();
//
//        for (File filenameTmp : listOfFiles) {
//            if (filenameTmp.isFile()) {
//                filenames.add(filenameTmp.getName());
//            }
//        }
//        return filenames;
//    }

}
