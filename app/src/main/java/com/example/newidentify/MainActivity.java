package com.example.newidentify;

import static com.example.newidentify.processData.SignalProcess.Butterworth;
import static java.lang.Math.abs;
import static java.lang.Math.getExponent;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.newidentify.Util.ChartSetting;
import com.example.newidentify.Util.CheckIDCallback;
import com.example.newidentify.Util.CleanFile;
import com.example.newidentify.Util.TinyDB;
import com.example.newidentify.processData.DecodeCha;
import com.example.newidentify.processData.FindPeaks;
import com.example.newidentify.Util.CsvMaker;
import com.example.newidentify.processData.SignalProcess;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.BarLineChartTouchListener;
import com.obsez.android.lib.filechooser.ChooserDialog;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class MainActivity extends AppCompatActivity implements CheckIDCallback {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    /**
     * UI
     **/
    Button btn_detect, btn_clean, btn_stop;
    TextView txt_fileName, txt_result;
    TextView txt_checkID_status, txt_checkID_result;
    TextView txt_Register_values, txt_Measure_values;

    /**
     * choose Device Dialog
     */

    Dialog deviceDialog;

    /**
     * Parameter
     **/
    private String fileName;
    private String filePath;
    private String path;
    String diff_value_toExcel = "";
    int averageHalfWidthValue = 0;

    private String getfileName;
    private String getFilePath = "";
    private static ArrayList<Float> save15secWaveData = new ArrayList<>();//取15秒的資料

    // Used to load the 'newidentify' library on application startup.
    static {
        System.loadLibrary("newidentify");
        System.loadLibrary("lp4tocha");
    }

    /**
     * BLE
     */
    public static Activity global_activity;
    public static TextView txt_countDown;
    static BT4 bt4;
    TinyDB tinyDB;
    public static TextView txt_BleStatus;
    //    public static TextView Percent_Text;

    ///////////////////////
    //////畫心電圖使用///////
    //////////////////////
    private final Handler countDownHandler = new Handler();
    private boolean isToastShown = false;
    public CountDownTimer countDownTimer;//倒數
    boolean isCountDownRunning = false;
    boolean isMeasurementOver = false;
    boolean isPreChecked = false;
    private static final int COUNTDOWN_INTERVAL = 1000;
    private static final int COUNTDOWN_TOTAL_TIME = 30000;

    public static LineChart lineChart;
    public static LineChart chart_df;
    public static LineChart chart_df2;
    public static LineChart chart_df3;
    private ChartSetting chartSetting;

    public static ArrayList<Entry> chartSet1Entries = new ArrayList<Entry>();
    public static ArrayList<Double> oldValue = new ArrayList<Double>();
    public static LineDataSet chartSet1 = new LineDataSet(null, "");

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
    /**
     * L2D
     */
    private DecodeCha decodeCha;
    private SignalProcess signalProcess;
    private FindPeaks findPeaks;
    private CleanFile cleanFile;
    private CsvMaker csvMaker = new CsvMaker(this);
    private CheckID checkID;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化SharedPreference
        preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        editor = preferences.edit();

        global_activity = this;
        bt4 = new BT4(global_activity);
        tinyDB = new TinyDB(global_activity);
        deviceDialog = new Dialog(global_activity);
        signalProcess = new SignalProcess();
        cleanFile = new CleanFile();
        chartSetting = new ChartSetting();
        checkID = new CheckID(this, this);
        lineChart = findViewById(R.id.linechart);
        chart_df = findViewById(R.id.chart_df);
        chart_df2 = findViewById(R.id.chart_df2);
        chart_df3 = findViewById(R.id.chart_df3);

        initchart();//初始化圖表
        initObject();//初始化物件
        initDeviceDialog();//裝置選擇Dialog

        checkID.readRecord();//讀取註冊檔案存檔

        txt_checkID_status.setText(checkID.recordResult);//讀取註冊檔案
        txt_Register_values.setText(checkID.recordValue);//讀取以儲存數據
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBroadcast();
        setScreenOn();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            stopALL();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        setScreenOff();
        if (bt4.isConnected) {
            bt4.close();
        }
        unregisterReceiver(getReceiver);
        stopALL();
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void initBroadcast() {
        //註冊廣播過濾
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BT4.BLE_CONNECTED);
        intentFilter.addAction(BT4.BLE_TRY_CONNECT);
        intentFilter.addAction(BT4.BLE_DISCONNECTED);
        intentFilter.addAction(BT4.BLE_READ_FILE);
        registerReceiver(getReceiver, intentFilter);
    }

    /**
     * 藍芽已連接/已斷線資訊回傳
     */
    private final BroadcastReceiver getReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BT4.BLE_CONNECTED.equals(action)) {
                txt_BleStatus.setText("已連線");
            } else if (BT4.BLE_DISCONNECTED.equals(action)) {
                txt_BleStatus.setText("已斷線");
            } else if (BT4.BLE_TRY_CONNECT.equals(action)) {
                Toast.makeText(global_activity, "搜尋到裝置，連線中", Toast.LENGTH_SHORT).show();
            } else if (BT4.BLE_READ_FILE.equals(action)) {
                txt_countDown.setText((bt4.file_data.size() * 100 / bt4.File_Count) + " %");
            }
        }
    };

    /**
     * 螢幕恆亮
     **/
    public void setScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void setScreenOff() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initObject() {
        btn_clean = findViewById(R.id.btn_clean);
        btn_detect = findViewById(R.id.btn_detect);
        btn_stop = findViewById(R.id.btn_stop);
        txt_result = findViewById(R.id.txt_result);
        txt_countDown = findViewById(R.id.txt_countDown);
        txt_BleStatus = findViewById(R.id.txt_BleStatus);
        txt_checkID_status = findViewById(R.id.txt_checkID_status);
        txt_checkID_result = findViewById(R.id.txt_checkID_result);
        txt_Register_values = findViewById(R.id.txt_Register_values);
        txt_Measure_values = findViewById(R.id.txt_Measure_values);

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopALL();
            }
        });
        btn_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tinyDB.clear();
                editor.clear();
                editor.apply();
                checkID.cleanRecord();
                ShowToast("已清除註冊檔案");
                txt_checkID_status.setText("尚未有註冊資料");
            }
        });
    }

    /**
     * 初始化裝置選擇Dialog
     **/
    public void initDeviceDialog() {
        deviceDialog.setContentView(R.layout.dialog_device);
        // 初始化元件
        RadioGroup devicesRadioGroup = deviceDialog.findViewById(R.id.devicesRadioGroup);
        RadioButton radioButtonDevice1 = deviceDialog.findViewById(R.id.radioButtonDevice1);
        RadioButton radioButtonDevice2 = deviceDialog.findViewById(R.id.radioButtonDevice2);
        Button completeButton = deviceDialog.findViewById(R.id.completeButton);
        deviceDialog.setCancelable(false);

        // 設置按鈕點擊監聽器
        completeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // 在這裡處理完成按鈕的點擊事件
                // 檢查哪個 RadioButton 被選中
                int checkedRadioButtonId = devicesRadioGroup.getCheckedRadioButtonId();
                if (checkedRadioButtonId == R.id.radioButtonDevice1) {
                    bt4.deviceName = "CmateH";
                    bt4.Bluetooth_init();
                    deviceDialog.dismiss();

                } else if (checkedRadioButtonId == R.id.radioButtonDevice2) {
                    bt4.deviceName = "WTK230";
                    bt4.Bluetooth_init();
                    deviceDialog.dismiss();
                    // 處理選中 Device 2 的邏輯
                } else {
                    // 提示用戶選擇一個裝置
                    Toast.makeText(global_activity, "請選擇一個裝置", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // 顯示對話框
        deviceDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 350) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("wwwww", "搜尋設備");
            }
        }
    }

    public static void DrawChart(byte[] result) {
        try {
            global_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String receive = "";
                    for (int i = 0; i < result.length; i++)
                        receive += "  " + Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1);
                    double ch2 = 0;
                    ch2 = bt4.byteArrayToInt(new byte[]{result[4]}) * 128 + bt4.byteArrayToInt(new byte[]{result[5]});
                    ch2 = ch2 * 1.7;
                    int ch4 = getStreamLP((int) ch2);
                    if (ch4 >= 2500) {
                        ch4 = 2500;
                    } else if (ch4 <= 1500) {
                        ch4 = 1500;
                    }
                    if (chartSet1Entries.size() >= 300) {
                        ArrayList<Entry> temp = new ArrayList<Entry>();
                        ArrayList<Double> temp_old = new ArrayList<Double>();
                        for (int i = 1; i < chartSet1Entries.size(); i++) {
                            Entry chartSet1Entrie = new Entry(temp.size(), chartSet1Entries.get(i).getY());
                            temp.add(chartSet1Entrie);
                            temp_old.add(oldValue.get(i));
                        }
                        chartSet1Entries = temp;
                        oldValue = temp_old;
                    }

                    oldValue.add((double) ch4);

                    double nvalue = (oldValue.get(oldValue.size() - 1));

                    if (oldValue.size() > 1) {
                        nvalue = Butterworth(oldValue);
                    }
                    if (oldValue.size() > 110) {
                        Entry chartSet1Entrie = new Entry(chartSet1Entries.size(), (float) nvalue);
                        chartSet1Entries.add(chartSet1Entrie);
                        if (save15secWaveData.size() < 1500) {
                            save15secWaveData.add((float) nvalue);
                        }
                    }
                    chartSet1.setValues(chartSet1Entries);
                    lineChart.setData(new LineData(chartSet1));
                    lineChart.setVisibleXRangeMinimum(300);
                    lineChart.invalidate();
                }
            });
        } catch (Exception ex) {
//            Log.d("wwwww", "eeeeeerrr = " + ex.toString());
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

    /**
     * 量測與畫圖
     */
    @SuppressLint("HandlerLeak")
    public void startWaveMeasurement(View view) {
        bt4.Bluetooth_init();
        if (bt4.isConnected) {
            runOnUiThread(() -> {
                chartSet1Entries.clear(); // 清除上次的數據
                oldValue.clear(); // 清除上次的數據
                txt_checkID_result.setText("");
                txt_checkID_status.setText("");
                txt_Measure_values.setText("當下數據\n");
                initchart();
            });
            if (!isCountDownRunning) {
                isMeasurementOver = false;
                startCountDown();
                bt4.startWave(true, new Handler());
            }
        } else {
            ShowToast("請先連接裝置");
        }
    }

    private void startCountDown() {
        isCountDownRunning = true;
        isMeasurementOver = false;
        isPreChecked = false;
        txt_result.setText("量測結果");
        countDownHandler.postDelayed(new Runnable() {
            private int presetTime = 6000;
            private int remainingTime = COUNTDOWN_TOTAL_TIME;

            @Override
            public void run() {
                if (!isToastShown) {
                    Toast.makeText(global_activity, "請將手指放置於裝置上，量測馬上開始", Toast.LENGTH_LONG).show();
                    isToastShown = true;
                }
                if (presetTime <= 0) {//倒數6秒結束才開始跑波
                    bt4.isTenSec = true;
                    if (remainingTime <= 0) {//結束的動作
                        txt_countDown.setText("30");
                        stopWaveMeasurement();
                        isCountDownRunning = false;
                        isMeasurementOver = true;
                    } else {//秒數顯示
                        txt_countDown.setText(String.valueOf(remainingTime / 1000));
                        remainingTime -= COUNTDOWN_INTERVAL;
                        countDownHandler.postDelayed(this, COUNTDOWN_INTERVAL);
                    }
                } else {
                    bt4.isTenSec = false;
                    txt_countDown.setText(String.valueOf(presetTime / 1000));
                    presetTime -= COUNTDOWN_INTERVAL;
                    countDownHandler.postDelayed(this, COUNTDOWN_INTERVAL);
                }
            }
        }, COUNTDOWN_INTERVAL);
    }

    //把能停的都停下來
    private void stopALL() {
        if (isCountDownRunning) {
            countDownHandler.removeCallbacksAndMessages(null); // 移除倒數
            isCountDownRunning = false;
            txt_countDown.setText("30");
            stopWaveMeasurement();
            btn_detect.setText("量測");
        }

    }

    @SuppressLint("HandlerLeak")
    public void stopWaveMeasurement() {
        if (bt4.isWave) {
            ShowToast("正在停止跑波...");
            bt4.StopMeasure(new Handler() {
                @Override
                public void handleMessage(Message msg2) {
                    ShowToast("完成停止跑波");
                    if (isMeasurementOver) {
                        processMeasurementData();
                    }
                }
            });
        } else {
            ShowToast("尚未開始跑波");
        }
    }

    /**
     * 處理量測檔案
     */
    @SuppressLint("HandlerLeak")
    private void processMeasurementData() {
        final int[] step = {0};
        bt4.Record_Size(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (step[0] == 0) {
                    Log.d("wwwww", "讀取檔案大小");
                    bt4.File_Size(this);
                }

                if (step[0] == 1) {
                    Log.d("wwwww", "打開檔案");
                    bt4.Open_File(this);
                }

                if (step[0] == 2) {
                    Log.d("wwwww", "讀取檔案");
                    bt4.ReadData(this);
                }

                if (step[0] == 3) {
                    if (bt4.file_data.size() > 0) {
                        saveLP4(bt4.file_data);
                    } else {
                        ShowToast("檔案大小為0");
                    }
                    bt4.Delete_AllRecor(this);
                }

                if (step[0] == 4) {
                    readLP4(getFilePath);
                    checkID.loadFilePath(getFilePath);
                    bt4.Delete_AllRecor(this);
                    bt4.file_data.clear();
                    bt4.Buffer_Array.clear();
                }
                step[0]++;
            }
        });
    }

    private void saveLP4(ArrayList<Byte> file_data) {
        try {
            String date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(System.currentTimeMillis());
            String fileName = "r_" + date + "_888888.lp4";
            getfileName = fileName;

            // 直接在外部存儲目錄中創建文件
            File fileLocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), fileName);

            saveByteArrayToFile(file_data, fileLocation);

            runOnUiThread(() -> {
                // 在主執行緒中處理 UI 相關的操作
                ShowToast("LP4儲存成功");
                MediaScannerConnection.scanFile(this, new String[]{fileLocation.getAbsolutePath()}, null, (path, uri) -> {
                    getFilePath = path;
                });
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 儲存檔案
     */
    private void saveByteArrayToFile(ArrayList<Byte> byteList, File file) throws IOException {
        byte[] byteArray = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            Byte byteValue = byteList.get(i);
            byteArray[i] = (byteValue != null) ? byteValue : 0;
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write(byteArray);
        }
    }

    private void readLP4(String filePath) {
        File file = new File(filePath);
        decpEcgFile(filePath);
        String fileName = file.getName();
        int y = fileName.length();
        String j = fileName.substring(0, y - 4);
        fileName = j + ".CHA"; // Ensure the consistent file extension

        // Debugging: Output file paths
        Log.d("FilePaths", "LP4 Path: " + filePath);
        Log.d("FilePaths", "CHA Path: " + fileName);

        readCHA(fileName);
    }

    /**
     * 讀取CHA
     */
    public void readCHA(String fileName) {
        // Debugging: Output CHA file path
        Log.d("FilePaths", "CHA Path in readCHA: " + fileName);

        if (!fileName.isEmpty()) {
            File chaFile = new File(Environment.getExternalStorageDirectory(), fileName);
            Log.d("FilePaths", "chaFile: " + chaFile);
            if (chaFile.exists()) {
                decodeCha = new DecodeCha(chaFile.getAbsolutePath());
                try {
                    decodeCha.run();
                    decodeCha.join();
                    ShowToast("匯入CHA成功");
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                calculateRR(decodeCha.finalCHAData);
            } else {
                Log.e("CHAFileNotFound", "CHA檔案不存在：" + fileName);
            }
        } else {
            Log.e("EmptyFileName", "文件名稱為空");
        }
    }

    private void calculateRR(List<Float> dataList) {
        if (dataList == null || dataList.size() == 0) {
            Log.e("EmptyDataList", "dataList為空");
        } else {
            try {
                findPeaks = new FindPeaks(dataList);
                findPeaks.run();
                //makeCsv  ArrayList<Double>
                ArrayList<Double> doubles = floatArrayToDoubleList(findPeaks.ecg_signal_origin);
                String date = new SimpleDateFormat("yyyyMMddhhmmss",
                        Locale.getDefault()).format(System.currentTimeMillis());
//                csvMaker.makeCSVDouble(doubles, "original_" + date + ".csv");
                //畫圖
                chartSetting.markRT(chart_df3, findPeaks.ecg_signal_origin, findPeaks.R_index_up, findPeaks.T_index_up);

                calMidError(findPeaks.ecg_signal_origin);

            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }

    public void calMidError(Float[] floats) {
        List<Integer> R_index = findPeaks.R_index_up;
        if (R_index.size() > 10) {

            //取得已經過濾過的RR100(4張圖)
            List<Float> df1 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(10), R_index.get(12));
            List<Float> df2 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(3), R_index.get(5));
            List<Float> df3 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(6), R_index.get(8));
            List<Float> df4 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(8), R_index.get(10));
            //取註冊數據
            if (tinyDB.getListFloat("df1").size() < 10) {
                tinyDB.putString("chooserFileName", getfileName);
                tinyDB.putListFloat("df1", df1);
                tinyDB.putListFloat("df2", df2);
                tinyDB.putListFloat("df3", df3);
                tinyDB.putListFloat("df4", df4);
            } else {
                Log.d("TinyDB", "已經有第一段數據了");
            }
            //輸出自己與別人的差異
            List<Float> df1_ = tinyDB.getListFloat("df1");

            //計算自己的差異
            float diff12 = signalProcess.calMidDiff(df1, df2);
            float diff13 = signalProcess.calMidDiff(df1, df3);
            float diff14 = signalProcess.calMidDiff(df1, df4);
            float diff23 = signalProcess.calMidDiff(df2, df3);
            //計算別人的差異(只用df1)
            float diff11_ = signalProcess.calMidDiff(df1_, df1);
            float diff12_ = signalProcess.calMidDiff(df1_, df2);
            float diff13_ = signalProcess.calMidDiff(df1_, df3);
            float diff14_ = signalProcess.calMidDiff(df1_, df4);

            //畫圖
            chartSetting.overlapChart(chart_df, df1, df2, df3, df4, Color.CYAN, Color.RED);
            chartSetting.overlapChart(chart_df2, df1_, df2, df3, df4, Color.BLACK, Color.parseColor("#F596AA"));

            //計算平均差異
            float averageDiff4Num_self = (diff12 + diff13 + diff14 + diff23) / 4;
            float averageDiff4Num_sb = (diff12_ + diff13_ + diff14_ + diff11_) / 4;

            //計算R T的中位數、最大值、標準差
            float median_R = findPeaks.calculateMedian(findPeaks.R_dot_up);
            float max_R = findPeaks.calculateMax(findPeaks.R_dot_up);
            float std_R = findPeaks.calculateSTD(findPeaks.R_dot_up);
            float median_T = findPeaks.calculateMedian(findPeaks.T_dot_up);
            float max_T = findPeaks.calculateMax(findPeaks.T_dot_up);
            float std_T = findPeaks.calculateSTD(findPeaks.T_dot_up);
            float voltMed = findPeaks.calVoltDiffMed(findPeaks.ecg_signal_origin, findPeaks.R_index_up, findPeaks.T_index_up);
            float distanceMed = findPeaks.calDistanceDiffMed(findPeaks.R_index_up, findPeaks.T_index_up);
            //計算R的半高寬
            List<Integer> halfWidth = findPeaks.calculateHalfWidths(findPeaks.ecg_signal_origin, findPeaks.R_index_up);
            averageHalfWidthValue = findPeaks.calculateHalfWidthsAverage(halfWidth);
            //輸出
            Log.d("hhhh", "diff12: " + diff12 + "\ndiff13:" + diff13 + "\ndiff14:" + diff14 + "\ndiff23:" + diff23 + "\naverage:" + averageDiff4Num_self);
            String r_value = "\nRV-med:" + median_R + "/RV-max:" + max_R + "/RV-std:" + std_R;
            String t_value = "\nTV-med:" + median_T + "/TV-max:" + max_T + "/TV-std:" + std_T;
            String r_halfWidth = "\nR-halfWidth:" + averageHalfWidthValue;
            String rt_voltMed = "\nRT-電壓差:" + voltMed;
            String rt_distanceMed = "\nRT-時間差:" + distanceMed;
            String diff_value = String.format("自己當下差異度: %s/與註冊時差異度: %s", averageDiff4Num_self, averageDiff4Num_sb + r_value + t_value + r_halfWidth + rt_voltMed + rt_distanceMed);
            txt_result.setText(diff_value);

            diff_value_toExcel = averageDiff4Num_self + "," + averageDiff4Num_sb + "," + median_R + "," + max_R + "," + std_R + "," + median_T + "," + max_T + "," + std_T + "," + averageHalfWidthValue + "," + voltMed + "," + distanceMed;
        }
    }

    public ArrayList<Double> floatArrayToDoubleList(Float[] arrayList) {
        // 將 ArrayList<Byte> 轉換為 byte 數組
        ArrayList<Double> doubleArray = new ArrayList<>();
        for (int i = 0; i < arrayList.length; i++) {
            doubleArray.add(Double.valueOf(arrayList[i]));
        }
        return doubleArray;
    }

    public void initchart() {
        chartSetting.initchart(lineChart);

        chartSet1.setColor(Color.BLACK);
        chartSet1.setDrawCircles(false);
        chartSet1.setDrawFilled(false);
        chartSet1.setFillAlpha(0);
        chartSet1.setCircleRadius(0);
        chartSet1.setLineWidth((float) 1.5);
        chartSet1.setDrawValues(false);
        chartSet1.setDrawFilled(true);

        float scaleX = lineChart.getScaleX();
        if (scaleX == 1)
            lineChart.zoomToCenter(5, 1f);
        else {
            BarLineChartTouchListener barLineChartTouchListener = (BarLineChartTouchListener) lineChart.getOnTouchListener();
            barLineChartTouchListener.stopDeceleration();
            lineChart.fitScreen();
        }

        lineChart.invalidate();
    }


    @Override
    public void onStatusUpdate(String result) {
        runOnUiThread(() -> {
            Log.d("IDCallback", "onStatusUpdate: " + result);
            txt_checkID_status.setText(result);
        });
    }

    @Override
    public void onCheckIDError(String result) {
        runOnUiThread(() -> {
            Log.d("IDCallback", "onCheckIDError: " + result);
            txt_checkID_status.setText(result);
            if (result.equals("量測失敗")) {
                txt_Measure_values.setText("計算錯誤");
            }
        });
    }

    @Override
    public void onResult(String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {//量測結果
                String isYouString = "";
                if (result.isEmpty() || result.equals("null")) {
                    isYouString = "量測失敗";
                } else {
                    isYouString = result;
                }
                txt_checkID_result.setText(isYouString);
                String dateTime = new SimpleDateFormat("yyyyMMddHHmmss",
                        Locale.getDefault()).format(System.currentTimeMillis());

                recordOutput(dateTime + "," + diff_value_toExcel + "," + isYouString);
            }
        });
    }

    public void recordOutput(String finalResult) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<String> recordList = new ArrayList<>();

                // 使用 split 方法根據逗號分隔 finalResult 字串
                String[] results = finalResult.split(",");

                // 將分割後的結果添加到 recordList 中
                for (String result : results) {
                    recordList.add(result.trim()); // trim() 移除前後的空白字符
                }

                // 接下來可以將 recordList 中的數據寫入到文件中
                csvMaker.writeRecordToFile(recordList); // 假設有一個寫入文件的方法
            }
        }).start();
    }

    @Override
    public void onDetectData(String result, String result2) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result.isEmpty() || result.equals("null")) {
                    txt_Register_values.setText("已註冊數據: 無");
                } else {
                    txt_Register_values.setText("已註冊數據: \n" + result);
                }
                if (result2.isEmpty() || result2.equals("null")) {
                    txt_Measure_values.setText("量測資料: 無");
                } else {
                    txt_Measure_values.setText("目前量測: \n" + result2);
                }
            }
        });
    }

    @Override
    public void onDetectDataError(String result) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (result.isEmpty() || result.equals("null")) {
                    txt_Measure_values.setText("目前量測: 無");
                } else {
                    txt_Measure_values.setText("目前量測: \n" + result);
                }
            }
        });
    }

    public static native int anaEcgFile(String name, String path);

    public static native int decpEcgFile(String path);

    public static void ShowToast(final String message) {
        global_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(global_activity, message, Toast.LENGTH_LONG).show();
            }
        });
    }//ShowToast


}