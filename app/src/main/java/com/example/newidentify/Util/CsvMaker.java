package com.example.newidentify.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CsvMaker {

    private Context context;

    public CsvMaker(Context context) {
        this.context = context;
    }

    public void makeCSVDouble(ArrayList<Double> doubles, String fileName) {
        new Thread(() -> {
            /** 檔名 */
            String[] title = {"Lead2"};
            StringBuffer csvText = new StringBuffer();
            for (int i = 0; i < title.length; i++) {
                csvText.append(title[i] + ",");
            }
            /** 內容 */
            for (int i = 0; i < doubles.size(); i++) {
                csvText.append("\n" + doubles.get(i));
            }

            ((Activity) context).runOnUiThread(() -> {
                try {
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    builder.detectFileUriExposure();
                    FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                    out.write((csvText.toString().getBytes()));
                    out.close();
                    File fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName);
                    FileOutputStream fos = new FileOutputStream(fileLocation);
                    fos.write(csvText.toString().getBytes());
                    Uri path = Uri.fromFile(fileLocation);
                    Intent fileIntent = new Intent(Intent.ACTION_SEND);
                    fileIntent.setType("text/csv");
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fileIntent.putExtra(Intent.EXTRA_STREAM, path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }//makeCSV

    public void makeCSVFloat(Float[] floats, String fileName) {
        new Thread(() -> {
            /** 檔名 */
            String date = new SimpleDateFormat("yyyyMMddhhmmss",
                    Locale.getDefault()).format(System.currentTimeMillis());

            String[] title = {"Lead2"};
            StringBuffer csvText = new StringBuffer();
            for (int i = 0; i < title.length; i++) {
                csvText.append(title[i] + ",");
            }
            /** 內容 */
            for (int i = 0; i < floats.length; i++) {
                csvText.append("\n" + floats[i]);
            }

            ((Activity) context).runOnUiThread(() -> {
                try {
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    builder.detectFileUriExposure();
                    FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                    out.write((csvText.toString().getBytes()));
                    out.close();
                    File fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName);
                    FileOutputStream fos = new FileOutputStream(fileLocation);
                    fos.write(csvText.toString().getBytes());
                    Uri path = Uri.fromFile(fileLocation);
                    Intent fileIntent = new Intent(Intent.ACTION_SEND);
                    fileIntent.setType("text/csv");
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fileIntent.putExtra(Intent.EXTRA_STREAM, path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }//makeCSV

    public void makeCSVFloatDf(List<Float> df1, List<Float> df2, List<Float> df3, List<Float> df4, String fileName) {
        new Thread(() -> {
            /** 檔名 */
            String date = new SimpleDateFormat("yyyyMMddhhmmss", Locale.getDefault()).format(System.currentTimeMillis());
            String[] titles = {"df1", "df2", "df3", "df4"};
            StringBuffer csvText = new StringBuffer();

            for (int i = 0; i < titles.length; i++) {
                csvText.append(titles[i] + ",");
            }

            int maxSize = Math.max(Math.max(df1.size(), df2.size()), Math.max(df3.size(), df4.size()));
            for (int i = 0; i < maxSize; i++) {

                if (i < df1.size()) {
                    csvText.append(df1.get(i));
                }
                csvText.append(",");

                if (i < df2.size()) {
                    csvText.append(df2.get(i));
                }
                csvText.append(",");

                if (i < df3.size()) {
                    csvText.append(df3.get(i));
                }
                csvText.append(",");

                if (i < df4.size()) {
                    csvText.append(df4.get(i));
                }

                csvText.append("\n");
            }

            ((Activity) context).runOnUiThread(() -> {
                try {
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    builder.detectFileUriExposure();
                    FileOutputStream out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
                    out.write((csvText.toString().getBytes()));
                    out.close();
                    File fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName);
                    FileOutputStream fos = new FileOutputStream(fileLocation);
                    fos.write(csvText.toString().getBytes());
                    Uri path = Uri.fromFile(fileLocation);
                    Intent fileIntent = new Intent(Intent.ACTION_SEND);
                    fileIntent.setType("text/csv");
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT, fileName);
                    fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    fileIntent.putExtra(Intent.EXTRA_STREAM, path);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }).start();
    }

    public void writeRecordToFile(ArrayList<String> strings) {
        String TAG = "writeRecordToFile";

        new Thread(() -> {
            String folderName = "revlis_record";
            String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + folderName;
            // 檢查資料夾是否存在，如果不存在則創建
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                boolean result = directory.mkdirs(); // 嘗試建立目錄
                if (!result) {
                    // 目錄建立失敗的處理
                    Log.d(TAG, "Error: 無法建立資料夾" + directoryPath);
                }
            }

            /** 檔名 */
            String fileName = "revlis_record.csv";
            String filePath = directoryPath + File.separator + fileName;
            File file = new File(filePath);

            try (FileOutputStream fos = new FileOutputStream(file, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                /** 如果檔案不存在或檔案大小為0，則寫入BOM和標題 */
                if (!file.exists() || file.length() == 0) {
                    //寫入UTF-8 BOM
                    writer.write('\ufeff');
                    String[] title = {
                            "時間", // Time
                            "自己當下差異度", // own_diff
                            "與註冊時差異度", // regi_diff
                            "R_V中位數", // RV-med
                            "R_V最大值", // RV-max
                            "R_V標準差", // RV-std
                            "T_V中位數", // TV-med
                            "T_V最大值", // TV-max
                            "T_V標準差", // TV-std
                            "平均半高寬", // avg-halfWidth
                            "R-T電壓差", // RT-VDiff
                            "R-T距離", // RT-Distance
                            "是否為本人" // isYou
                    };
                    writer.write(String.join(",", title) + "\n");
                }
                /** 寫入資料 */
                writer.write(String.join(",", strings) + "\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }



}
