package com.example.newidentify.mfcc;

import static com.example.newidentify.MainActivity.global_activity;

import android.util.Log;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.example.newidentify.MainActivity;
import com.example.newidentify.process.CalculateDiffMean;
import com.example.newidentify.util.EcgMath;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MFCCProcess {
    public static Python py;
    public static PyObject pyObj;
    private static final Gson gson = new Gson();
    private final CalculateDiffMean calculateDiffMean = new CalculateDiffMean();
    private final EcgMath ecgMath = new EcgMath();
    PyObject hrv_analysis;

    static {
        py = Python.getInstance();
        pyObj = py.getModule("nk2_process");
    }

    public double[] mfccRegister(double[] ecg_signal) {
        PyObject object;
        double[] distanceArray = new double[0]; // 初始化

        try {
            object = pyObj.callAttr("hrv_analysis", ecg_signal, 1000.0);
            if (object == null || object.asList().isEmpty()) {
                Log.e("mfccRegister", "Invalid PyObject: null or empty");
                return distanceArray; // 返回空的數組
            }

            // 將 Python 返回的數據轉換為 Map
            Map<String, List<Double>> rValuesMap = gson.fromJson(String.valueOf(object.asList().get(0)), new TypeToken<Map<String, List<Double>>>() {
            }.getType());
            Map<String, Double> HRVValuesMap = gson.fromJson(String.valueOf(object.asList().get(1)), new TypeToken<Map<String, Double>>() {
            }.getType());

            // 檢查 rValuesMap 和 HRVValuesMap 是否為空
            if (rValuesMap == null || HRVValuesMap == null) {
                Log.e("mfccRegister", "Parsed JSON is null");
                return distanceArray; // 返回空的數組
            }

            List<Double> rPeaksList = rValuesMap.get("r_peaks");
            List<Double> signalMapList = rValuesMap.get("clean_signal");

            // 先檢查資料的品質
            double diffMean = calculateDiffMean.calDiffMean(signalMapList, ecgMath.listDoubleToListInt(rPeaksList));
            if (Math.abs(diffMean) > 0.8) {
                global_activity.runOnUiThread(() -> {
                    MainActivity.txt_detect_result.setText("自我差異度：" + String.format("%.2f", diffMean) + "\n差異度過高");
                    MainActivity.txt_isMe.setText("");
                });
                return distanceArray; // 返回空的數組
            } else {
                global_activity.runOnUiThread(() -> {
                    MainActivity.txt_detect_result.setText("自我差異度：" + String.format("%.2f", diffMean));
                });
            }

            // 開始計算 MFCC
            MFCC mfcc = new MFCC();
            double[] mfcc1 = ecgMath.convertFloatArrayToDoubleArray(
                    mfcc.process(get5RR8000(signalMapList, rPeaksList.get(2), rPeaksList.get(6))));
            double[] mfcc2 = ecgMath.convertFloatArrayToDoubleArray(
                    mfcc.process(get5RR8000(signalMapList, rPeaksList.get(5), rPeaksList.get(9))));
            double[] mfcc3 = ecgMath.convertFloatArrayToDoubleArray(
                    mfcc.process(get5RR8000(signalMapList, rPeaksList.get(8), rPeaksList.get(12))));
            double[] mfcc4 = ecgMath.convertFloatArrayToDoubleArray(
                    mfcc.process(get5RR8000(signalMapList, rPeaksList.get(11), rPeaksList.get(15))));

            // 計算兩組 MFCC 的歐式距離
            double distance1 = calculateEuclideanDistance(mfcc1, mfcc2);
            double distance2 = calculateEuclideanDistance(mfcc3, mfcc4);

            // 計算距離的中位數和平均數
            ArrayList<Double> distances = new ArrayList<>();
            distances.add(distance1);
            distances.add(distance2);
            double medianDistance = calculateMedian(distances);
            double meanDistance = calculateMean(distances);

            distanceArray = new double[]{distance1, distance2};

            // 返回距離值
            global_activity.runOnUiThread(() -> {
                MainActivity.txt_checkID_result.setText("中位：" + String.format("%.2f", medianDistance) +
                        "\n平均：" + String.format("%.2f", meanDistance));
                MainActivity.txt_checkID_status.setText("已註冊完成");
            });

        } catch (Exception e) {
            Log.e("mfccRegister", "Error: " + e.getMessage());
            return distanceArray; // 返回空的數組或在發生錯誤時返回初始值
        }

        return distanceArray;
    }



    // 調用NK2濾波處理
    public ArrayList<double[]> mfccProcess(double[] ecg_signal) {
        try {
            hrv_analysis = pyObj.callAttr("hrv_analysis", ecg_signal, 1000.0);
        } catch (Exception e) {
            Log.e("nk2_filter", "Error: " + e.getMessage());
            return null;
        }

        return getProcessedSignal(hrv_analysis);
    }

    public ArrayList<double[]> getProcessedSignal(PyObject object) {
        // 檢查 object 是否為 null 或者空
        if (object == null || object.asList().isEmpty()) {
            Log.e("getProcessedSignal", "Invalid PyObject: null or empty");
            return null;
        }
        // 將 Python 返回的數據轉換為 Map
        Map<String, List<Double>> rValuesMap = gson.fromJson(String.valueOf(object.asList().get(0)), new TypeToken<Map<String, List<Double>>>() {
        }.getType());
        Map<String, Double> HRVValuesMap = gson.fromJson(String.valueOf(object.asList().get(1)), new TypeToken<Map<String, Double>>() {
        }.getType());

        Log.d("TAG", "getProcessedSignal: " + HRVValuesMap);

        List<Double> rPeaksList = rValuesMap.get("r_peaks");
        List<Double> signalMapList = rValuesMap.get("clean_signal");

        // 檢查解析後的 Map 是否為空
        if (rValuesMap == null || HRVValuesMap == null) {
            Log.e("getProcessedSignal", "Parsed JSON is null");
            return null;
        }
        double Bpm = HRVValuesMap.get("bpm");

        double diffMean = calculateDiffMean.calDiffMean(signalMapList, ecgMath.listDoubleToListInt(rPeaksList));

        if (Math.abs(diffMean) > 0.8) {
            global_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    MainActivity.txt_detect_result.setText("自我差異度：" + String.format("%.2f", diffMean) + "\n差異度過高");
                    MainActivity.txt_isMe.setText("");
                }
            });
            return null;
        } else {
            global_activity.runOnUiThread(() -> {
                MainActivity.txt_detect_result.setText(
                        "自我差異度：" + String.format("%.2f", diffMean) +
                                "\n心率：" + String.format("%.2f", Bpm));
            });
        }

        ArrayList<double[]> arrayList = new ArrayList<>();

        if (rPeaksList.size() < 16) {
            Log.d("getProcessedSignal", "rPeaksList < 16");
            return null;
        } else {
            //將數據段都去計算MFCC
            MFCC mfcc = new MFCC();
            float[] a1;
            float[] a2;
            float[] a3;
            float[] a4;
            float[] a5;
            float[] a6;
            float[] a7;
            float[] a8;

            assert signalMapList != null;

            try {
                a1 = mfcc.process(get5RR8000(signalMapList, rPeaksList.get(2), rPeaksList.get(6)));
                a2 = mfcc.process(get5RR8000(signalMapList, rPeaksList.get(5), rPeaksList.get(9)));
                a3 = mfcc.process(get5RR8000(signalMapList, rPeaksList.get(8), rPeaksList.get(12)));
                a4 = mfcc.process(get5RR8000(signalMapList, rPeaksList.get(11), rPeaksList.get(15)));

                EcgMath ecgMath = new EcgMath();
                a5 = ecgMath.convertDoubleArrayToFloatArray(get5RR8000(signalMapList, rPeaksList.get(2), rPeaksList.get(6)));
                a6 = ecgMath.convertDoubleArrayToFloatArray(get5RR8000(signalMapList, rPeaksList.get(5), rPeaksList.get(9)));
                a7 = ecgMath.convertDoubleArrayToFloatArray(get5RR8000(signalMapList, rPeaksList.get(8), rPeaksList.get(12)));
                a8 = ecgMath.convertDoubleArrayToFloatArray(get5RR8000(signalMapList, rPeaksList.get(11), rPeaksList.get(15)));
            } catch (Exception e) {
                Log.e("getProcessedSignal", "Error: " + e.getMessage());
                return null;
            }
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a1));
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a2));
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a3));
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a4));
            //a5、a6、a7、a8為畫MFCC_DiffMean圖用
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a5));
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a6));
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a7));
            arrayList.add(ecgMath.convertFloatArrayToDoubleArray(a8));

        }

        return arrayList;
    }

    private void checkDiffMean(double diffMean, double Bpm) {
        global_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String s = "自我差異度：" + String.format("%.2f", diffMean);
                String s1 = "心率：" + String.format("%.2f", Bpm);
                if (Math.abs(diffMean) > 0.8) {
                    MainActivity.txt_detect_result.setText(s + "\n差異度過高");
                    MainActivity.txt_isMe.setText("");
                } else {
                    MainActivity.txt_detect_result.setText(s + "\n" + s1);
                }
            }
        });
    }

    private static double linearInterpolate(double x0, double y0, double x1, double y1, double x) {
        if (x1 == x0) {
            return y0;
        }
        return y0 + (x - x0) * (y1 - y0) / (x1 - x0);
    }

    public static double[] get5RR8000(List<Double> dataList, double startIndex, double endIndex) {
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(dataList.size() - 1, endIndex);

        List<Double> dataBetweenR = new ArrayList<>(dataList.subList((int) startIndex, (int) (endIndex + 1)));

        int requiredLength = 8000;
        double[] result = new double[requiredLength];

        if (dataBetweenR.size() < requiredLength) {
            int originalLength = dataBetweenR.size();
            for (int i = 0; i < requiredLength; i++) {
                double x = (double) i * (originalLength - 1) / (requiredLength - 1);
                int x0 = (int) Math.floor(x);
                int x1 = Math.min(x0 + 1, originalLength - 1);
                result[i] = linearInterpolate(x0, dataBetweenR.get(x0), x1, dataBetweenR.get(x1), x);
            }
        } else {
            for (int i = 0; i < requiredLength; i++) {
                if (i < dataBetweenR.size()) {
                    result[i] = dataBetweenR.get(i);
                } else {
                    result[i] = 0;  // 填充剩餘的部分
                }
            }
        }

        return result;
    }

    public double euclideanDistanceProcessor(ArrayList<double[]> firstGroup, ArrayList<double[]> secondGroup) {
        if (firstGroup.size() == 4 && secondGroup.size() == 4) {
            ArrayList<Double> distances = new ArrayList<>();

            for (int i = 0; i < firstGroup.size(); i++) {
                for (int j = 0; j < secondGroup.size(); j++) {
                    double[] a = firstGroup.get(i);
                    double[] b = secondGroup.get(j);

                    if (a.length == b.length) {
                        double distance = calculateEuclideanDistance(a, b);
                        distances.add(distance);

                        if (Arrays.equals(a, b)) {
                            Log.d("Debug", "ED is consistent");
                        } else {
                            Log.d("Debug", "ED mismatch detected");
                        }
                    } else {
                        Log.d("euclideanDistanceProcessor", "Array lengths do not match: " + a.length + "/" + b.length);
                    }
                }
            }

            if (distances.isEmpty()) {
                Log.d("err", "euclideanDistanceProcessor: distances is empty");
                return 9999;
            } else {
                StringBuilder sb = new StringBuilder();

                for (int i = 0; i < distances.size(); i++) {
                    sb.append(distances.get(i)).append(", ");
                    if ((i + 1) % 4 == 0) { // 每4个元素换一行
                        Log.d("rrrr", sb.toString());
                        sb.setLength(0); // 清空StringBuilder
                    }
                }
                Log.d("rrrr", "mean: " + calculateMean(distances));

                return calculateMedian(distances);
            }
        } else {
            Log.d("err", "euclideanDistanceProcessor: " + firstGroup.size() + "/" + secondGroup.size());
            return 9999;
        }
    }

    public static double calculateEuclideanDistance(double[] array1, double[] array2) {
        float sum = 0;
        for (int i = 0; i < array1.length; i++) {
            sum += (float) Math.pow(array1[i] - array2[i], 2);
        }
        return (float) Math.sqrt(sum);
    }

    public static double calculateMedian(ArrayList<Double> values) {
        Collections.sort(values);
        int size = values.size();
        if (size % 2 == 0) {
            return (values.get(size / 2 - 1) + values.get(size / 2)) / 2.0f;
        } else {
            return values.get(size / 2);
        }
    }

    public static double calculateMean(ArrayList<Double> values) {
        double sum = 0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.size();
    }

    public void printSubtractedMatrix(ArrayList<double[]> firstGroup, ArrayList<double[]> secondGroup) {
        if (firstGroup.size() != secondGroup.size()) {
            Log.d("Error", "Matrix sizes do not match.");
            return;
        }

        Log.d("Debug", "Subtracted Matrix:");

        for (int i = 0; i < firstGroup.size(); i++) {
            double[] a = firstGroup.get(i);
            double[] b = secondGroup.get(i);

            if (a.length != b.length) {
                Log.d("Error", "Row lengths do not match at row " + i);
                return;
            }

            double[] resultRow = new double[a.length];
            for (int j = 0; j < a.length; j++) {
                resultRow[j] = a[j] - b[j];
            }

            // 打印这一行的结果
            Log.d("Debug", "Row " + i + ": " + Arrays.toString(resultRow));
        }
    }


}
