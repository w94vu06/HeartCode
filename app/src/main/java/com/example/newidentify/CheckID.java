package com.example.newidentify;

import static com.example.newidentify.MainActivity.anaEcgFile;
import static com.example.newidentify.MainActivity.decpEcgFile;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import com.example.newidentify.Util.CheckIDCallback;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CheckID {
    private final CheckIDCallback callback;
    private final Context context;
    private TinyDB tinyDB;

    public CheckID(CheckIDCallback callback, Context context) {
        this.callback = callback;
        this.context = context;
        this.tinyDB = new TinyDB(context);
    }
    private String path, filePath, fileName;
    private Map<String, String> dataHashMap = new HashMap<>();
    public ArrayList<Double> heartRateArray = new ArrayList<>();
    private ArrayList<Double> stdArray = new ArrayList<>();
    private ArrayList<Double> CVIArray = new ArrayList<>();
    private ArrayList<Double> GIArray = new ArrayList<>();

    double maxHR, maxStd, maxCVI, maxGI;
    double minHR, minStd, minCVI, minGI;
    double ValueHR, ValueStd, ValueCVI, ValueGI;

    public void readRecord() {
        heartRateArray = tinyDB.getListDouble("heartRateArray");
        stdArray = tinyDB.getListDouble("stdArray");
        CVIArray = tinyDB.getListDouble("CVIArray");
        GIArray = tinyDB.getListDouble("GIArray");

        String registerFileCount = String.valueOf(heartRateArray.size());

        if (heartRateArray.size() == 0) {
            callback.onStatusUpdate("尚未有註冊資料");
        } else {
            callback.onStatusUpdate("已有" + registerFileCount + "筆註冊資料");
        }
    }

    public void loadFilePath(String dir) {
        if (dir == null || dir.isEmpty()) {
            Log.d("gggg", "loadFilePath 檔案發生問題，請重新量測");
            return;
        }
        File file = new File(dir);
        fileName = file.getName();
        filePath = dir;
        path = filePath.substring(0, filePath.length() - fileName.length());

        initCheck();

    }

    private void initCheck() {
        if (fileName == null) {
            Log.d("gggg", "initCheck 發生問題，請重新量測");
            callback.onCheckIDError("檔案發生問題，請重新量測");
            return;
        }

        String extension = fileName.substring(fileName.lastIndexOf(".") + 1);
        if (extension.equalsIgnoreCase("lp4")) {
            try {
                decpEcgFile(filePath);
                fileName = fileName.replace(".lp4", ".cha");
                initIdentify();
            } catch (Exception e) {
                Log.d("gggg", "decpEcgFile 發生問題，請重新量測");
                callback.onCheckIDError("處理檔案時發生錯誤: " + e.getMessage());
            }
        } else if (extension.equalsIgnoreCase("cha")) {
            initIdentify();
        } else {
            callback.onCheckIDError("檔案格式錯誤，請重新量測");
        }
    }

    private void initIdentify() {
        int x = anaEcgFile(fileName, path);
        if (x == 1) {
            Log.d("gggg", "anaEcgFile 錯誤，請重新量測");
            callback.onCheckIDError("檔案訊號錯誤，請換個檔案繼續");
        }
        filePath = path;
        fileName = fileName.substring(0, fileName.length() - 4);
        File file = new File(filePath + "/r_" + fileName + ".txt");
        if (file.isFile() && file.exists()) {
            try {
                parseFile(file);
            } catch (Exception e) {
                Log.e("catchError", e.toString());
            }
        }
    }

    private void parseFile(File file) throws IOException, FileNotFoundException {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // 將每一行拆分為名稱和值，然後將它們添加到dataMap中
                String[] parts = line.split(",");
                for (String part : parts) {
                    String[] nameValue = part.split(":");
                    if (nameValue.length == 2) {
                        String name = nameValue[0].trim(); // 去除名稱前後的空格
                        String value = nameValue[1].trim(); // 去除值前後的空格
                        dataHashMap.put(name, value);
                    }
                }
            }
            updateValues();
        }
    }

    private void updateValues() {
        int dataCollectionLimit = 5;
        double Max = Double.parseDouble(dataHashMap.get("Max"));
        ValueHR = Double.parseDouble(dataHashMap.get("Average"));
        ValueStd = Double.parseDouble(dataHashMap.get("Standard Deviation"));
        ValueCVI = Double.parseDouble(dataHashMap.get("CVI"));
        ValueGI = Double.parseDouble(dataHashMap.get("GI"));

        Log.d("vvvv", "Average: "+ValueHR);
        Log.d("vvvv", "Standard Deviation: "+ValueStd);
        Log.d("vvvv", "CVI: "+ValueCVI);
        Log.d("vvvv", "GI: "+ValueGI);

        if (Max > 180 && Max / ValueHR > 1.4) {
            Log.d("gggg", "dataBad: ");
            callback.onResult("量測失敗，請重新量測");
        } else {
            Log.d("gggg", "dataGood: ");
            if (heartRateArray.size() < dataCollectionLimit) {
                heartRateArray.add(ValueHR);
                stdArray.add(ValueStd);
                CVIArray.add(ValueCVI);
                GIArray.add(ValueGI);

                Log.d("cccc", "updateValues: "+heartRateArray.toString());
                Log.d("cccc", "updateValues: "+stdArray.toString());
                Log.d("cccc", "updateValues: "+CVIArray.toString());
                Log.d("cccc", "updateValues: "+GIArray.toString());

                saveRecord();
            }
        }

        if (heartRateArray.size() >= dataCollectionLimit) {//計算通過標準
            calculatePassStandard();
        }

        String s = String.format("所需檔案數量: (%d/%d)\n", heartRateArray.size(), dataCollectionLimit);
        Log.d("gggg", s);
        callback.onStatusUpdate(s);
    }

    private static final double HR_RANGE_FACTOR = 0.158;
    private static final double STD_RANGE_FACTOR = 1.3;
    private static final double CVI_RANGE_FACTOR = 0.37;
    private static final double GI_RANGE_FACTOR = 0.6;

    private void calculatePassStandard() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            double averageHR = calculateAverage(heartRateArray);
            maxHR = averageHR + averageHR * HR_RANGE_FACTOR;
            minHR = averageHR - averageHR * HR_RANGE_FACTOR;

            double averageStd = calculateAverage(stdArray);
            maxStd = averageStd + averageStd * STD_RANGE_FACTOR;
            minStd = averageStd - averageStd * STD_RANGE_FACTOR;

            double averageCVI = calculateAverage(CVIArray);
            maxCVI = averageCVI + averageCVI * CVI_RANGE_FACTOR;
            minCVI = averageCVI - averageCVI * CVI_RANGE_FACTOR;

            double averageGI = calculateAverage(GIArray);
            maxGI = averageGI + averageGI * GI_RANGE_FACTOR;
            minGI = averageGI - averageGI * GI_RANGE_FACTOR;

            checkIDResult();
        }
    }

    private double calculateAverage(ArrayList<Double> values) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return values.stream().mapToDouble(Double::valueOf).average().getAsDouble();
        } else {
            double sum = 0.0;
            for (Double value : values) {
                sum += value;
            }
            return sum / values.size();
        }
    }

    private void checkIDResult() {
        if (ValueHR > minHR &&
                ValueHR < maxHR &&
                ValueStd > minStd &&
                ValueStd < maxStd &&
                ValueGI > minGI &&
                ValueGI < maxGI &&
                ValueCVI > minCVI &&
                ValueCVI < maxCVI) {
            Log.d("gggg", "本人");
            callback.onResult("本人");
        } else {
            Log.d("gggg", "非本人");
            callback.onResult("非本人");
        }
    }

    public void saveRecord() {
        tinyDB.putListDouble("heartRateArray", heartRateArray);
        tinyDB.putListDouble("stdArray", stdArray);
        tinyDB.putListDouble("CVIArray", CVIArray);
        tinyDB.putListDouble("GIArray", GIArray);
    }

    public void cleanRecord() {
        heartRateArray.clear();
        stdArray.clear();
        CVIArray.clear();
        GIArray.clear();
        tinyDB.clear();
    }

}
