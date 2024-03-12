package com.example.newidentify.processData;

import android.util.Log;

import com.example.newidentify.MainActivity;
import com.example.newidentify.Util.TinyDB;
import com.github.mikephil.charting.data.Entry;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SignalProcess extends Thread {
    private static final String TAG = "SignalProcess";
    TinyDB tinyDB = new TinyDB(MainActivity.global_activity);

    public ArrayList<Float> averageDiff4NumSelfList = new ArrayList<>();
    public ArrayList<Float> averageDiff4NumSbList = new ArrayList<>();
    public ArrayList<Float> R_MedList = new ArrayList<>();
    public ArrayList<Float> R_VoltMedList = new ArrayList<>();
    public ArrayList<Float> RT_distanceMedList = new ArrayList<>();
    public ArrayList<Integer> halfWidthList = new ArrayList<>();

    public String loadRecord() {
        averageDiff4NumSelfList = tinyDB.getListFloat("averageDiff4NumSelfList");
        averageDiff4NumSbList = tinyDB.getListFloat("averageDiff4NumSbList");
        R_MedList = tinyDB.getListFloat("R_MedList");
        R_VoltMedList = tinyDB.getListFloat("R_VoltMedList");
        RT_distanceMedList = tinyDB.getListFloat("RT_distanceMedList");
        halfWidthList = tinyDB.getListInt("halfWidthList");

        String registerFileCount = String.valueOf(averageDiff4NumSelfList.size());
        String registerEvent;
        if (averageDiff4NumSelfList.size() == 0) {
            registerEvent = "尚未有註冊資料";
        } else {
            registerEvent = "已有" + registerFileCount + "筆註冊資料";
        }
        return registerEvent;
    }

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

}
