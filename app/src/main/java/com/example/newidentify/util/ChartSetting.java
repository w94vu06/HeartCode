package com.example.newidentify.util;

import android.graphics.Color;
import android.util.Log;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.List;

public class ChartSetting {

    int Start = 0;
    int End = 18000;

    static double[] mX = {0.0, 0.0, 0.0, 0.0, 0.0};
    static double[] mY = {0.0, 0.0, 0.0, 0.0, 0.0};
    static int[] mStreamBuf = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20};
    static double[] mAcoef = {0.00001347408952448771,
            0.00005389635809795083,
            0.00008084453714692624,
            0.00005389635809795083,
            0.00001347408952448771};
    static double[] mBcoef = {1.00000000000000000000,
            -3.67172908916193470000,
            5.06799838673418980000,
            -3.11596692520174570000,
            0.71991032729187143000};

    public void initchart(LineChart lineChart) {
        // 允許滑動
        lineChart.setDragEnabled(true);

        // 設定縮放
        lineChart.setScaleEnabled(false);
        lineChart.setPinchZoom(false);

        // 其他圖表設定
        lineChart.setData(new LineData());
        lineChart.getXAxis().setValueFormatter(null);
        lineChart.getXAxis().setDrawLabels(false);
        lineChart.getXAxis().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawGridLines(false);
        lineChart.getAxisLeft().setDrawAxisLine(false);
        lineChart.getAxisLeft().setGranularityEnabled(false);
        lineChart.getXAxis().setDrawAxisLine(false);
        lineChart.getAxisLeft().setDrawLabels(false);
        lineChart.getAxisLeft().setAxisMinimum(1500);
        lineChart.getAxisLeft().setAxisMaximum(2500);
        lineChart.getXAxis().setAxisMinimum(0);
        lineChart.getXAxis().setAxisMaximum(300);
        lineChart.getXAxis().setGranularity(30);
        lineChart.getAxisLeft().setGranularity(250);
        lineChart.getAxisRight().setDrawLabels(false);
        lineChart.getAxisRight().setDrawGridLines(false);
        lineChart.getAxisRight().setDrawAxisLine(false);
        lineChart.getDescription().setText("");
        lineChart.getLegend().setEnabled(false);

        lineChart.invalidate();
    }

    public void diffMeanChart(LineChart lineChart, double[] df1, double[] df2, double[] df3, double[] df4) {
        // 將 float[] 轉換為 ArrayList<Entry>
        ArrayList<Entry> df1_ = convertToEntryList(df1);
        ArrayList<Entry> df2_ = convertToEntryList(df2);
        ArrayList<Entry> df3_ = convertToEntryList(df3);
        ArrayList<Entry> df4_ = convertToEntryList(df4);

        // 創建 LineDataSet
        LineDataSet dataSet1 = createDataSet("df1", Color.parseColor("#7efc91"), df1_);
        LineDataSet dataSet2 = createDataSet("df2", Color.parseColor("#fc7e7e"), df2_);
        LineDataSet dataSet3 = createDataSet("df3", Color.parseColor("#effc7e"), df3_);
        LineDataSet dataSet4 = createDataSet("df4", Color.parseColor("#7ef8fc"), df4_);

        // 配置軸
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setEnabled(false);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        XAxis topAxis = lineChart.getXAxis();
        topAxis.setEnabled(false);

        // 設置數據並刷新圖表
        LineData lineData = new LineData(dataSet1, dataSet2, dataSet3, dataSet4);
        lineChart.setData(lineData);

        lineChart.invalidate();
    }

    public void overlapArrayChart(LineChart lineChart, double[] df1, double[] df2, double[] df3, double[] df4) {
        // 將 float[] 轉換為 ArrayList<Entry>
        ArrayList<Entry> df1_ = convertToEntryList(df1);
        ArrayList<Entry> df2_ = convertToEntryList(df2);
        ArrayList<Entry> df3_ = convertToEntryList(df3);
        ArrayList<Entry> df4_ = convertToEntryList(df4);

        // 創建 LineDataSet
        LineDataSet dataSet1 = createDataSet("df1", Color.parseColor("#7efc91"), df1_);
        LineDataSet dataSet2 = createDataSet("df2", Color.parseColor("#fc7e7e"), df2_);
        LineDataSet dataSet3 = createDataSet("df3", Color.parseColor("#effc7e"), df3_);
        LineDataSet dataSet4 = createDataSet("df4", Color.parseColor("#7ef8fc"), df4_);

        // 配置軸
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.setEnabled(false);
        YAxis rightAxis = lineChart.getAxisRight();
        rightAxis.setEnabled(false);
        XAxis topAxis = lineChart.getXAxis();
        topAxis.setEnabled(false);

        // 設置數據並刷新圖表
        LineData lineData = new LineData(dataSet1, dataSet2, dataSet3, dataSet4);
        lineChart.setData(lineData);

        lineChart.invalidate();
    }

    public void setOverlapChartDescription(LineChart lineChart, String description) {
        Description chartDescription = new Description();
        chartDescription.setText(description);
        chartDescription.setTextColor(Color.BLACK);
        chartDescription.setTextSize(12);
        lineChart.setDescription(chartDescription);
    }

    private ArrayList<Entry> convertToEntryList(double[] data) {
        ArrayList<Entry> entryList = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            if (Double.isNaN(data[i])) {
                entryList.add(new Entry(i, 0));
            } else {
                entryList.add(new Entry(i, (float) data[i]));
            }
        }
        return entryList;
    }

    private LineDataSet createDataSet(String label, int color, List<Entry> entries) {
        LineDataSet dataSet = new LineDataSet(entries, label);

        dataSet.setColor(color);
        dataSet.setLineWidth(2f);
        dataSet.setDrawCircles(false);
        dataSet.setMode(LineDataSet.Mode.LINEAR);

        return dataSet;
    }

    public static double Butterworth(ArrayList<Double> indata) {
        try {
            double deltaTimeinsec = 0.000125;
            double CutOff = 1921;
            double Samplingrate = 1 / deltaTimeinsec;
            Samplingrate = 10000;
            int dF2 = indata.size() - 1;        // The data range is set with dF2
            double[] Dat2 = new double[dF2 + 4]; // Array with 4 extra points front and back
            ArrayList<Double> data = new ArrayList<Double>(); // Ptr., changes passed data
            // Copy indata to Dat2
            for (int r = 0; r < dF2; r++) {
                Dat2[2 + r] = indata.get(r);
            }
            Dat2[1] = Dat2[0] = indata.get(0);
            Dat2[dF2 + 3] = Dat2[dF2 + 2] = indata.get(dF2);
            double pi = 3.14159265358979;
            double wc = Math.tan(CutOff * pi / Samplingrate);
            double k1 = 1.414213562 * wc; // Sqrt(2) * wc
            double k2 = wc * wc;
            double a = k2 / (1 + k1 + k2);
            double b = 2 * a;
            double c = a;
            double k3 = b / k2;
            double d = -2 * a + k3;
            double e = 1 - (2 * a) - k3;
            // RECURSIVE TRIGGERS - ENABLE filter is performed (first, last points constant)
            double[] DatYt = new double[dF2 + 4];
            DatYt[1] = DatYt[0] = indata.get(0);
            for (int s = 2; s < dF2 + 2; s++) {
                DatYt[s] = a * Dat2[s] + b * Dat2[s - 1] + c * Dat2[s - 2]
                        + d * DatYt[s - 1] + e * DatYt[s - 2];
            }
            DatYt[dF2 + 3] = DatYt[dF2 + 2] = DatYt[dF2 + 1];

            // FORWARD filter
            double[] DatZt = new double[dF2 + 2];
            DatZt[dF2] = DatYt[dF2 + 2];
            DatZt[dF2 + 1] = DatYt[dF2 + 3];
            for (int t = -dF2 + 1; t <= 0; t++) {
                DatZt[-t] = a * DatYt[-t + 2] + b * DatYt[-t + 3] + c * DatYt[-t + 4]
                        + d * DatZt[-t + 1] + e * DatZt[-t + 2];
            }

            // Calculated points copied for return
            for (int p = 0; p < dF2; p++) {
                data.add(DatZt[p]);
            }
            return data.get(data.size() - 1);
        } catch (Exception ex) {
            return indata.get(indata.size() - 1);
        }
    }


    public static int getStreamLP(int NewSample) {
        int tmp = 0;
        for (int array = 4; array >= 1; array--) {
            mX[array] = mX[array - 1];
            mY[array] = mY[array - 1];
        }
        mX[0] = (double) (NewSample);
        mY[0] = mAcoef[0] * mX[0];
        for (int i = 1; i <= 4; i++) {
            mY[0] += mAcoef[i] * mX[i] - mBcoef[i] * mY[i];
        }
        for (int array = 20; array >= 1; array--) {
            mStreamBuf[array] = mStreamBuf[array - 1];
        }
        mStreamBuf[0] = NewSample;
        tmp = mStreamBuf[20] + (2000 - (int) (mY[0]));
        return tmp;
    }
}
