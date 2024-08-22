package com.example.newidentify.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadCSV {

    public double[] processCSV(File csvFile) throws IOException {
        double[] lead2Data = readLead2DataFromCSV(csvFile);

        return lead2Data;
    }

    private double[] readLead2DataFromCSV(File csvFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(csvFile));
        String line;
        List<Double> dataList = new ArrayList<>();

        // 讀取CSV表頭
        String headerLine = br.readLine();
        if (headerLine == null) {
            br.close();
            throw new IOException("CSV file is empty or missing header.");
        }

        // 找到Lead2列的索引
        String[] headers = headerLine.split(",");
        int lead2Index = -1;
        for (int i = 0; i < headers.length; i++) {
            if (headers[i].trim().equalsIgnoreCase("Lead2")) {
                lead2Index = i;
                break;
            }
        }

        if (lead2Index == -1) {
            br.close();
            throw new IOException("Lead2 column not found in CSV file.");
        }

        // 讀取Lead2列的數據
        while ((line = br.readLine()) != null) {
            String[] values = line.split(",");
            dataList.add(Double.parseDouble(values[lead2Index]));
        }

        br.close();

        // 轉換為數組
        double[] dataArray = new double[dataList.size()];
        for (int i = 0; i < dataArray.length; i++) {
            dataArray[i] = dataList.get(i);
        }

        return dataArray;
    }
}