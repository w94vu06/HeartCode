package com.example.newidentify.process;


import static com.example.newidentify.MainActivity.chart_df2;
import static com.example.newidentify.MainActivity.global_activity;
import static com.example.newidentify.MainActivity.txt_detect_result;

import com.example.newidentify.util.ChartSetting;
import com.example.newidentify.util.EcgMath;
import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CalculateDiffMean extends Thread {

    EcgMath ecgMath = new EcgMath();

    public double calDiffMean(List<Double> doubles, List<Integer> R_index) {
        if (R_index.size() < 12) {
            return 9999f; // 或者拋出一個異常
        }

        double[] df1 = ecgMath.listDoubleToDoubleArray(getReduceRR100(doubles, R_index.get(2), R_index.get(6)));
        double[] df2 = ecgMath.listDoubleToDoubleArray(getReduceRR100(doubles, R_index.get(5), R_index.get(9)));
        double[] df3 = ecgMath.listDoubleToDoubleArray(getReduceRR100(doubles, R_index.get(8), R_index.get(12)));
        double[] df4 = ecgMath.listDoubleToDoubleArray(getReduceRR100(doubles, R_index.get(11), R_index.get(15)));

        double diff12 = calMidDiff(df1, df2);
        double diff13 = calMidDiff(df1, df3);
        double diff14 = calMidDiff(df1, df4);
        double diff23 = calMidDiff(df2, df3);

        double diffMean = (diff12 + diff13 + diff14 + diff23) / 4;

        if (Math.abs(diffMean) > 0.8) {
            ChartSetting chartSetting = new ChartSetting();
            global_activity.runOnUiThread(() -> {
                txt_detect_result.setText("自我差異度過高");
                chartSetting.diffMeanChart(chart_df2, df1, df2, df3, df4);
            });
        }

        return diffMean;
    }

    // 這邊是降採樣的方法
    public List<Double> getReduceRR100(List<Double> dataList, int startIndex, int endIndex) {
        // 創建一個列表來存儲兩個R點之間的數據片段
        List<Entry> dataBetweenTwoR = new ArrayList<>();

        // 確保startIndex和endIndex在數據範圍內，防止越界
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(dataList.size() - 1, endIndex);

        // 將startIndex到endIndex之間的數據轉換為Entry對象並加入列表
        for (int i = startIndex; i <= endIndex; i++) {
            int xOffset = i - startIndex;  // 計算相對於startIndex的偏移量
            dataBetweenTwoR.add(new Entry(xOffset, dataList.get(i).floatValue())); // 創建Entry對象並加入列表
        }

        // 將提取出的數據片段進行降採樣，並返回結果
        return reduceSampling(dataBetweenTwoR);
    }

    private List<Double> reduceSampling(List<Entry> input) {
        int originalLength = input.size(); // 原始數據片段的長度
        int targetLength = Math.max(originalLength / 100, 100); // 目標降採樣後的長度，至少為100

        List<Double> result = new ArrayList<>(); // 創建結果列表來存儲降採樣後的數據
        int step = originalLength / targetLength; // 計算每個降採樣區間的步長

        // 遍歷原始數據片段以進行降採樣
        for (int i = 0; i < originalLength; i++) {
            double sum = 0; // 用於計算區間內數據的總和
            int count = 0;  // 用於計算區間內數據的數量

            // 聚合區間內的數據，計算該區間的平均值
            for (double j = i * step; j < (i + 1) * step && j < originalLength; j++) {
                sum += input.get((int) j).getY(); // 累加區間內的數據
                count++; // 增加數據計數
            }

            // 如果該區間內有數據，則計算平均值並加入結果列表
            if (count > 0) {
                result.add(sum / count);
            }
        }

        // 返回降採樣後的數據片段，通常是中間的50個點
        return get50Point(result);
    }


    private List<Double> get50Point(List<Double> result) {
        int midPoint = result.size() / 2;
        int startIndex = midPoint - 50;
        int endIndex = midPoint + 50;

        // 確保 startIndex 和 endIndex 在有效範圍內
        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(result.size() - 1, endIndex);

        List<Double> result2 = new ArrayList<>();
        for (int i = startIndex; i <= endIndex; i++) {
            result2.add(result.get(i));
        }
        if (result2.size() > 100) {
            result2 = result2.subList(0, 100);
        } else if (result2.size() < 100) {
            while (result2.size() < 100) {
                // add a average value
                double sum = 0;
                for (double value : result2) {
                    sum += value;
                }
                result2.add(sum / result2.size());
            }
        }
        return result2;
    }

    // 計算兩個列表的中位數差異
    public double calMidDiff(double[] data1, double[] data2) {
        // 檢查兩個列表的大小是否相同，如果不同，可能需要進行錯誤處理
        if (data1.length != data2.length) {
            return 0;
        }

        // 創建存儲中位數差異的列表
        List<Double> differences = new ArrayList<>();

        // 計算差異
        for (int i = 0; i < data1.length; i++) {
            double diff = (data2[i] - data1[i]) / data1[i];
            differences.add(diff);
        }

        // 計算中位數
        double midDiff = calMid(differences);

        return midDiff;
    }

    public double calMid(List<Double> values) {
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
}
