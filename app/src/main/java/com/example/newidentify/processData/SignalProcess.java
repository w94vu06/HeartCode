package com.example.newidentify.processData;

import static com.example.newidentify.MainActivity.chart_df;
import static com.example.newidentify.MainActivity.global_activity;

import android.graphics.Color;

import com.example.newidentify.util.ChartSetting;
import com.github.mikephil.charting.data.Entry;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SignalProcess extends Thread {
    private static final String TAG = "SignalProcess";


    public List<Float> getReduceRR100(List<Float> dataList, int startIndex, int endIndex) {
        List<Entry> dataBetweenTwoR = new ArrayList<>();

        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(dataList.size() - 1, endIndex);

        for (int i = startIndex; i <= endIndex; i++) {
            int xOffset = i - startIndex;
            dataBetweenTwoR.add(new Entry(xOffset, dataList.get(i)));
        }

        return reduceSampling(dataBetweenTwoR);
    }

    private List<Float> reduceSampling(List<Entry> input) {
        int originalLength = input.size();
        int targetLength = Math.max(originalLength / 100, 100);

        List<Float> result = new ArrayList<>();
        int step = originalLength / targetLength;
        for (int i = 0; i < originalLength; i++) {
            float sum = 0;
            int count = 0;

            for (float j = i * step; j < (i + 1) * step && j < originalLength; j++) {
                sum += input.get((int) j).getY();
                count++;
            }

            if (count > 0) {
                result.add(sum / count);
            }
        }
        return get50Point(result);
    }

    private List<Float> get50Point(List<Float> result) {
        int midPoint = result.size() / 2;
        int startIndex = midPoint - 50;
        int endIndex = midPoint + 50;

        // 確保 startIndex 和 endIndex 在有效範圍內
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(result.size() - 1, endIndex);

        List<Float> result2 = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            result2.add(result.get(i));
        }
        if (result2.size() > 100) {
            result2 = result2.subList(0, 100);
        } else if (result2.size() < 100) {
            while (result2.size() < 100) {
                // add a average value
                float sum = 0;
                for (float value : result2) {
                    sum += value;
                }
                result2.add(sum / result2.size());
            }
        }
        return result2;
    }

    // 計算兩個列表的中位數差異
    public float calMidDiff(List<Float> data1, List<Float> data2) {
        // 檢查兩個列表的大小是否相同，如果不同，可能需要進行錯誤處理
        if (data1.size() != data2.size()) {
            return 0;
        }

        // 創建存儲中位數差異的列表
        List<Float> differences = new ArrayList<>();

        // 計算差異
        for (int i = 0; i < data1.size(); i++) {
            float diff = (data2.get(i) - data1.get(i)) / data1.get(i);
            differences.add(diff);
        }

        // 計算中位數
        float midDiff = calMid(differences);

        return midDiff;
    }

    public float calMid(List<Float> values) {
        // 對值進行排序
        Collections.sort(values);

        int size = values.size();

        // 計算中位數
        if (size % 2 == 0) {
            // 如果列表大小為偶數，取中間兩個值的平均
            int middleIndex1 = size / 2 - 1;
            int middleIndex2 = size / 2;
            return (values.get(middleIndex1) + values.get(middleIndex2)) / 2.0f;
        } else {
            // 如果列表大小為奇數，取中間值
            int middleIndex = size / 2;
            return values.get(middleIndex);
        }
    }

    public float calDiffSelf(ArrayList<Float> floats, List<Integer> R_index) {
    List<Float> df1 = getReduceRR100(floats, R_index.get(10), R_index.get(12));
    List<Float> df2 = getReduceRR100(floats, R_index.get(3), R_index.get(5));
    List<Float> df3 = getReduceRR100(floats, R_index.get(6), R_index.get(8));
    List<Float> df4 = getReduceRR100(floats, R_index.get(8), R_index.get(10));

    float diff12 = calMidDiff(df1, df2);
    float diff13 = calMidDiff(df1, df3);
    float diff14 = calMidDiff(df1, df4);
    float diff23 = calMidDiff(df2, df3);

    float diffSelf = (diff12 + diff13 + diff14 + diff23) / 4;

    ChartSetting chartSetting = new ChartSetting();
    global_activity.runOnUiThread(() -> {
        chartSetting.overlapChart(chart_df, df1, df2, df3, df4, Color.CYAN, Color.RED);
    });

    return diffSelf;
}

    public double[] convertFloatsToDoubles(ArrayList<Float> input) {
        if (input == null) {
            return null; // 或處理null的情況
        }
        double[] output = new double[input.size()];
        for (int i = 0; i < input.size(); i++) {
            output[i] = input.get(i); // 自動將float轉換為double
        }
        return output;
    }

    public void getFFT(double[] signal) {
        // 這裡應該是從你的ECG設備或數據集載入的信號數據
        double sampleRate = 1000.0; // 採樣率
        signal = Arrays.copyOfRange(signal, 4000, 20384);
        Complex[] fftResult = performFFT(signal);
        // 計算每個點的頻率
        double[] frequency = calculateFrequency(fftResult.length, sampleRate);

        // 查找最大幅度的頻率
        double maxMagnitude = Double.NEGATIVE_INFINITY;
        int maxIndex = -1;
        for (int i = 0; i < fftResult.length; i++) {
            double magnitude = fftResult[i].abs();
            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude;
                maxIndex = i;
            }
        }

        double dominantFrequency = frequency[maxIndex];
        System.out.println("主導頻率: " + dominantFrequency + " Hz");
    }

    public static Complex[] performFFT(double[] signal) {
        FastFourierTransformer transformer = new FastFourierTransformer(DftNormalization.STANDARD);
        return transformer.transform(signal, TransformType.FORWARD);
    }

    public static double[] calculateFrequency(int n, double sampleRate) {
        double[] frequency = new double[n];
        for (int i = 0; i < n; i++) {
            frequency[i] = i * sampleRate / n;
        }
        return frequency;
    }

}
