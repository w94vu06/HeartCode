package com.example.newidentify.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FileMaker {

    private Context context;

    public FileMaker(Context context) {
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

    public void makeCSVFloat(ArrayList<Float> floats, String fileName) {
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
            for (int i = 0; i < floats.size(); i++) {
                csvText.append("\n" + floats.get(i));
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

    public void makeCSVFloatArray( Float[] floats, String fileName) {
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
            String finalFileName = date + fileName;

            ((Activity) context).runOnUiThread(() -> {
                try {
                    StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
                    StrictMode.setVmPolicy(builder.build());
                    builder.detectFileUriExposure();
                    FileOutputStream out = context.openFileOutput(finalFileName, Context.MODE_PRIVATE);
                    out.write((csvText.toString().getBytes()));
                    out.close();
                    File fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), finalFileName);
                    FileOutputStream fos = new FileOutputStream(fileLocation);
                    fos.write(csvText.toString().getBytes());
                    Uri path = Uri.fromFile(fileLocation);
                    Intent fileIntent = new Intent(Intent.ACTION_SEND);
                    fileIntent.setType("text/csv");
                    fileIntent.putExtra(Intent.EXTRA_SUBJECT, finalFileName);
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
            String fileName = "revlis_record_rv.csv";
            String filePath = directoryPath + File.separator + fileName;
            File file = new File(filePath);

            try (FileOutputStream fos = new FileOutputStream(file, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                /** 如果檔案不存在或檔案大小為0，則寫入BOM和標題 */
                if (!file.exists() || file.length() == 0) {
                    //寫入UTF-8 BOM
                    writer.write('\ufeff');
                    String[] title = {
                            "檔名", // Time
                            "自己當下差異度", // own_diff
                            "與註冊時差異度", // regi_diff
                            "R電壓中位數", // RV-med
                            "心率",
                            "RMSSD" ,
                            "SDNN",
                            "T電壓中位數", // TV-med
                            "半高寬", // avg-halfWidth
                            "RT電壓差", // RT-VDiff
                            "RT距離", // RT-Distance
                            "心臟代號",// HeartCode
                            "是否為本人",// isYou
                            "閥值"
                    };
                    /** 寫入標題 */
                    writer.write(String.join(",", title) + "\n");
                }
                /** 寫入資料 */
                writer.write(String.join(",", strings) + "\n");

            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void migrateAndDeleteOldRecordFile() {
        String TAG = "migrateAndDeleteOldRecordFile";
        String folderName = "revlis_record";
        String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + folderName;

        // 原始檔案和新檔案的路徑
        String oldFilePath = directoryPath + File.separator + "revlis_record.csv";
        String newFilePath = directoryPath + File.separator + "revlis_record_zh.csv";

        File oldFile = new File(oldFilePath);
        File newFile = new File(newFilePath);

        // 檢查原始檔案是否存在
        if (oldFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(oldFile), "UTF-8"));
                 FileOutputStream fos = new FileOutputStream(newFile, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {

                // 如果新檔案不存在或檔案大小為0，則寫入BOM和標題
                if (!newFile.exists() || newFile.length() == 0) {
                    writer.write('\ufeff');
                    String[] title = {
                            "時間", "自己當下差異度", "與註冊時差異度", "R_V中位數", "R_V最大值", "R_V標準差",
                            "T_V中位數", "T_V最大值", "T_V標準差", "平均半高寬", "R-T電壓差", "R-T距離", "是否為本人"
                    };
                    writer.write(String.join(",", title) + "\n");
                }

                String line;
                reader.readLine(); // 跳過原始檔案的標題行
                while ((line = reader.readLine()) != null) {
                    writer.write(line + "\n"); // 寫入新文件
                }

                // 資料遷移完成後刪除原文件
                if (!oldFile.delete()) {
                    Log.d(TAG, "Failed to delete old record file: " + oldFilePath);
                }

            } catch (IOException e) {
                Log.e(TAG, "Error migrating record file", e);
            }
        } else {
            Log.d(TAG, "Old record file does not exist: " + oldFilePath);
        }
    }

    public void saveByteArrayToFile(ArrayList<Byte> byteList, File file) throws IOException {
        byte[] byteArray = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            Byte byteValue = byteList.get(i);
            byteArray[i] = (byteValue != null) ? byteValue : 0;
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(byteArray);
        }
    }

}
