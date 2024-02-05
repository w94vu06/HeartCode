package com.example.newidentify.processData;

import android.util.Log;

import com.github.mikephil.charting.data.Entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.me.berndporr.iirj.Butterworth;

public class Processor15SecThread extends Thread {

    private static final String TAG = "Process15secData";

    private final List<Float> inputDataList;

    private float minValue = Float.MAX_VALUE;
    private float maxValue = Float.MIN_VALUE;

    private float minValueA = Float.MAX_VALUE;
    private float maxValueA = Float.MIN_VALUE;

    private float minValueB = Float.MAX_VALUE;
    private float maxValueB = Float.MIN_VALUE;

    //回傳結果
    public float averageDiff4Num_self;
    public Float[] processedDataArray;
    public Float[] returnFiltedArray;

    public List<Float> peakListTop = new ArrayList<>();//峰值
    public List<Float> rPeakTop = new ArrayList<>();//R點
    public List<Integer> rIndexTop = new ArrayList<>();//R點索引
    public List<Integer> rriTop = new ArrayList<>();//RR間距

    public Processor15SecThread(List<Float> inputDataList) {
        this.inputDataList = inputDataList;
    }

    @Override
    public void run() {
        super.run();
        processInputData();
    }

    private void processInputData() {
//        List<Float> bandStop = new ArrayList<>(Arrays.asList(butter_bandStop_filter(inputDataList, 55, 65, 100, 1)));

        Float[] filteredData1 = butter_bandpass_filter(inputDataList, 2, 10, 100, 1);
        Float[] filteredData2 = butter_bandpass_filter2(inputDataList, 2, 10, 100, 1);

        if (!Float.isNaN(maxValueA)) {
            Log.d("maxValueA", "isNOTNULL");
            maxValue = maxValueA;
            minValue = minValueA;
        } else {
            Log.d("maxValueA", "isNULL");
            filteredData1 = filteredData2;
            maxValue = maxValueB;
            minValue = minValueB;
        }

        if (filteredData1.length >= 1500) {
            processedDataArray = Arrays.copyOfRange(filteredData1, 100, 1500);
            returnFiltedArray = processedDataArray;//返回濾波過後的資料
        } else {
            handleDataError();
        }

        findAndCalculatePeaks(processedDataArray, 1.5);

        resetValues();
    }

    private void handleDataError() {
        Log.d(TAG, "length: " + processedDataArray.length);
        try {
            throw new Exception("資料錯誤");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void resetValues() {
        minValue = Float.MAX_VALUE;
        maxValue = 0;
        minValueA = Float.MAX_VALUE;
        maxValueA = 0;
        minValueB = Float.MAX_VALUE;
        maxValueB = 0;
    }

    private void findAndCalculatePeaks(Float[] data, double peakThresholdFactor) {
        identifyPeaks(data, peakThresholdFactor);
        calculatePeakList();

        if (rIndexTop.size() <= 9) {
            resetPeakLists();
            identifyPeaks(data, 2.5);
        }
        Log.d("tttt", "rIndexTop.size: " + rIndexTop.size());
        Log.d("tttt", "rIndexTop: " + rIndexTop);
        Log.d("tttt", "rriTop: " + rriTop);
        if (rIndexTop.size() > 10) {
            List<Float> df1 = getReduceRR100(Arrays.asList(processedDataArray), rIndexTop.get(0), rIndexTop.get(2));
            List<Float> df2 = getReduceRR100(Arrays.asList(processedDataArray), rIndexTop.get(3), rIndexTop.get(5));
            List<Float> df3 = getReduceRR100(Arrays.asList(processedDataArray), rIndexTop.get(6), rIndexTop.get(8));
            List<Float> df4 = getReduceRR100(Arrays.asList(processedDataArray), rIndexTop.get(9), rIndexTop.get(10));

            float diff12 = calMidDiff(df1, df2);
            float diff13 = calMidDiff(df1, df3);
            float diff14 = calMidDiff(df1, df4);
            float diff23 = calMidDiff(df2, df3);
            averageDiff4Num_self = (diff12 + diff13 + diff14 + diff23) / 4;
            Log.d("hhhh", "diff12: " + diff12 + "\ndiff13:" + diff13 + "\ndiff14:" + diff14 + "\ndiff23:" + diff23 + "\naverage:" + averageDiff4Num_self);
        } else {
            averageDiff4Num_self = -1f;
            Log.e(TAG, "R點數量未超過10");
        }

    }

    private void resetPeakLists() {
        peakListTop.clear();
        rPeakTop.clear();
        rIndexTop.clear();
        rriTop.clear();
    }

    // 找出峰值
    private void identifyPeaks(Float[] data, double peakThresholdFactor) {
        int chunkSize = 350;
        for (int i = 0; i < data.length; i += chunkSize) {
            int endIndex = Math.min(i + chunkSize, data.length);
            Float[] chunk = Arrays.copyOfRange(data, i, endIndex);

            float maxValue = Float.NEGATIVE_INFINITY;
            float minValue = Float.POSITIVE_INFINITY;
            for (Float value : chunk) {
                maxValue = Math.max(maxValue, value);
                minValue = Math.min(minValue, value);
            }

            for (Float value : chunk) {
                if (value > maxValue / peakThresholdFactor) {
                    peakListTop.add(value);
                } else {
                    peakListTop.add(0F);
                }
            }
        }
    }

    // 計算RR間距
    public void calculatePeakList() {
        float maxFloat = 0;
        boolean insideR = false;

        rriTop.clear();
        rPeakTop.clear();
        rIndexTop.clear();

        for (int i = 0; i < peakListTop.size(); i++) {
            float value = peakListTop.get(i);

            if (value != 0) {
                maxFloat = Math.max(maxFloat, value);
                insideR = true;
            } else if (insideR) {
                rPeakTop.add(maxFloat);
                rIndexTop.add(i - 1);
                maxFloat = 0;
                insideR = false;
            }
        }

        for (int i = 0; i < rIndexTop.size() - 1; i++) {
            rriTop.add((rIndexTop.get(i + 1)) - rIndexTop.get(i));
        }
    }

    public List<Float> getReduceRR100(List<Float> dataList, int startIndex, int endIndex) {
        List<Float> dataBetweenTwoR = new ArrayList<>();

        startIndex = Math.max(0, startIndex);
        endIndex = Math.min(dataList.size() - 1, endIndex);

        for (int i = startIndex; i <= endIndex; i++) {
            dataBetweenTwoR.add(dataList.get(i));
        }

        return get50Point(dataBetweenTwoR);
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

    /**
     * 巴特沃斯濾波器
     */
    public Float[] butter_bandpass_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandPass(order, fs, centerFrequency, widthFrequency);
        int in = 0;
        Float[] floatArray = new Float[data.size()];
        for (float a : data) {
            float b = (float) butterworth.filter(a);
            float c = b * b;
            float d = (float) butterworth.filter(c);
            minValueA = Math.min(minValueA, d);
            maxValueA = Math.max(maxValueA, d);
            floatArray[in] = d;
            in++;
        }
        return floatArray;
    }

    public Float[] butter_bandpass_filter2(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandPass(order, fs, centerFrequency, widthFrequency);
        int in = 0;
        Float[] floatArray2 = new Float[data.size()];
        for (float x : data) {
            float y = (float) butterworth.filter(x);
            floatArray2[in] = y;
            in++;
            minValueB = Math.min(minValueB, y);
            maxValueB = Math.max(maxValueB, y);
        }
        return floatArray2;
    }

    public Float[] butter_bandStop_filter(List<Float> data, int lowCut, int highCut, int fs, int order) {
        Butterworth butterworth = new Butterworth();
        float widthFrequency = highCut - lowCut;
        float centerFrequency = (highCut + lowCut) / 2;
        butterworth.bandStop(order, fs, centerFrequency, widthFrequency);
        int in = 0;
        Float[] floatArray = new Float[data.size()];
        for (float a : data) {
            float b = (float) butterworth.filter(a);
            floatArray[in] = b;
            in++;
        }
        return floatArray;
    }

}

