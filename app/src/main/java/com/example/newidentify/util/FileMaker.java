package com.example.newidentify.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FileMaker {

    private Context context;

    public FileMaker(Context context) {
        this.context = context;
    }

    public void makeCSVDoubleArrayList(ArrayList<Double> doubles, String fileName) {
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

    public void makeCSVDoubleArray(double[] doubles, String fileName) {
        new Thread(() -> {
            /** 檔名 */
            String[] title = {"Lead2"};
            StringBuffer csvText = new StringBuffer();
            for (int i = 0; i < title.length; i++) {
                csvText.append(title[i] + ",");
            }
            /** 內容 */
            for (int i = 0; i < doubles.length; i++) {
                csvText.append("\n" + doubles[i]);
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

    public void makeCSVFloatArrayList(ArrayList<Float> doubles, String fileName) {
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

    public void makeCSVFloatArray(float[] floats, String fileName) {
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

    public void makeCSVFloatArray(Float[] floats, String fileName) {
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

    public void writeRecordToFile(List<Map<String, Double>> strings) {
        String TAG = "writeRecordToFile";
        new Thread(() -> {
            String folderName = "revlis_record";
            String directoryPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + folderName;
            // 檢查資料夾是否存在，如果不存在則創建
            File directory = new File(directoryPath);
            if (!directory.exists()) {
                boolean result = directory.mkdirs(); // 嘗試建立目錄
                // 目錄建立失敗的處理
            }

            /** 檔名 */
            String date = new SimpleDateFormat("yyyyMMddhhmmss",
                    Locale.getDefault()).format(System.currentTimeMillis());

            String fileName = date + "_revlis_record.csv";
            String filePath = directoryPath + File.separator + fileName;
            File file = new File(filePath);

            try (FileOutputStream fos = new FileOutputStream(file, true);
                 OutputStreamWriter writer = new OutputStreamWriter(fos, "UTF-8")) {
                /** 如果檔案不存在或檔案大小為0，則寫入BOM和標題 */
                if (!file.exists() || file.length() == 0) {
                    //寫入UTF-8 BOM
                    writer.write('\ufeff');
                    String[] title = {
                            "狀態", // Time
                            "bpm",
                            "ibi",
                            "sdnn",
                            "sdsd",
                            "rmssd",
                            "pnn20",
                            "pnn50",
                            "hr_mad",
                            "sd1",
                            "sd2",
                            "s",
                            "sd1/sd2",
                            "iqrnn",
                            "ap_en",
                            "shan_en",
                            "fuzzy_en",
                            "af",
                            "DiffSelf",
                            "R_Med",
                            "HalfWidth",
                            "threshold"
                    };
                    /** 寫入標題 */
                    writer.write(String.join(",", title) + "\n");
                }
                /** 寫入資料 */
                // Write data
//                for (int i = 0; i < dataLists.size(); i++) {
//                    Map<String, Double> vector = dataLists.get(i);
//                    writer.write(states[i] + ",");
//                    for (String header : headers) {
//                        if (!header.equals("狀態")) { // Skip the state header
//                            writer.write(vector.get(header) + ",");
//                        }
//                    }
//                    writer.write("\n");
//                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void writeVectorsToCSV(List<Double> registerVector1List, List<Double> registerVector2List, List<Double> registerVector3List, List<Double> loginVectorList) {
        String[] headers = {
                "status",

                "bpm",
                "mean_nn",
                "sdnn",
                "sdsd",
                "rmssd",
                "sd1",
                "sd2",
                "sd1/sd2",
                "shan_en",
                "af",
                "t_area",
                "t_height",
                "pqr_angle",
                "qrs_angle",
                "rst_angle",
                "r_med",
                "voltStd",
                "halfWidth",
                "distance",
                "threshold"};
        String[] states = {"regi1", "regi2", "regi3", "login"};
        String storageDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String fileName = "vectors_" + timeStamp + ".csv";
        String filePath = storageDir + "/" + fileName;

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write headers
            writer.write(String.join(",", headers) + "\n");

            // Write data for each state
            for (int stateIndex = 0; stateIndex < states.length; stateIndex++) {
                writer.write(states[stateIndex]);
                List<Double> currentList;
                switch (stateIndex) {
                    case 0:
                        currentList = registerVector1List;
                        break;
                    case 1:
                        currentList = registerVector2List;
                        break;
                    case 2:
                        currentList = registerVector3List;
                        break;
                    case 3:
                        currentList = loginVectorList;
                        break;
                    default:
                        throw new IllegalStateException("Invalid state index: " + stateIndex);
                }
                for (Double value : currentList) {
                    writer.write("," + value);
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveMFCCVectorsToCSV(ArrayList<float[]> registerVectorList, ArrayList<float[]> loginVectorList, float distance) {
        String[] states = {"regi1", "regi2", "regi3", "regi4", "login1", "login2", "login3", "login4", "distance"};
        String storageDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        String timeStamp = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(new Date());
        String fileName = "MFCCVectors_" + timeStamp + ".csv";
        String filePath = storageDir + "/" + fileName;

        try (FileWriter writer = new FileWriter(filePath)) {
            // Write headers
            writer.write("State,Values\n");

            // Write data for each state
            int stateIndex = 0;

            // Check that registerVectorList and loginVectorList contain at least 4 elements
            if (registerVectorList.size() >= 4 && loginVectorList.size() >= 4) {
                for (int i = 0; i < 4; i++) {
                    // Write registration data
                    writer.write(states[stateIndex++] + ",");
                    float[] registerVector = registerVectorList.get(i);
                    for (float value : registerVector) {
                        writer.write(value + ",");
                    }
                    writer.write("\n");
                }

                for (int i = 0; i < 4; i++) {
                    // Write login data
                    writer.write(states[stateIndex++] + ",");
                    float[] loginVector = loginVectorList.get(i);
                    for (float value : loginVector) {
                        writer.write(value + ",");
                    }
                    writer.write("\n");
                }

                // Write distance
                writer.write(states[stateIndex] + "," + distance);
            } else {
                Log.e("saveMFCCVectorsToCSV", "Insufficient data: registerVectorList and/or loginVectorList do not contain enough elements.");
            }
        } catch (IOException e) {
            e.printStackTrace();
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
