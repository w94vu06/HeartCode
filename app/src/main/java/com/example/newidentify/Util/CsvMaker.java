package com.example.newidentify.Util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            String date = new SimpleDateFormat("yyyyMMddhhmmss",
                    Locale.getDefault()).format(System.currentTimeMillis());
            String[] title = {"Lead2"};
            StringBuffer csvText = new StringBuffer();
            for (int i = 0; i < title.length; i++) {
                csvText.append(title[i] + ",");
            }
            /** 內容 */
            for (int i = 0; i < doubles.size(); i++) {
                csvText.append("\n" + doubles.get(i));
            }

            ((Activity) context).runOnUiThread(() ->{
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

            ((Activity) context).runOnUiThread(() ->{
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
}
