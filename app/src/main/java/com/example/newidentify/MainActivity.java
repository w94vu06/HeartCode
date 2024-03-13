package com.example.newidentify;

import static com.example.newidentify.Util.ChartSetting.Butterworth;
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
import com.example.newidentify.Util.CleanFile;
import com.example.newidentify.Util.TinyDB;
import com.example.newidentify.processData.DecodeCha;
import com.example.newidentify.processData.FindPeaks;
import com.example.newidentify.Util.FileMaker;
import com.example.newidentify.processData.SignalProcess;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.BarLineChartTouchListener;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    /**
     * UI
     **/
    Button btn_detect, btn_clean, btn_stop;
    TextView txt_result;
    TextView txt_average;
    TextView txt_checkID_status, txt_checkID_result;
    TextView txt_Register_values;

    /**
     * choose Device Dialog
     */

    Dialog deviceDialog;

    /**
     * Parameter
     **/
    public String fileName = "";
    public String diffValueSB = "註冊筆";

    public float averageDiff4Num_self;
    public float R_Med;
    public float R_VoltMed;
    public float RT_distanceMed;
    public float halfWidth;

    private ArrayList<Float> averageDiff4NumSelfList = new ArrayList<>();
    private ArrayList<Float> averageDiff4NumSbList = new ArrayList<>();
    private ArrayList<Float> R_MedList = new ArrayList<>();
    private ArrayList<Float> R_VoltMedList = new ArrayList<>();
    private ArrayList<Float> RT_distanceMedList = new ArrayList<>();
    private ArrayList<Float> halfWidthList = new ArrayList<>();


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
    //    public static LineChart chart_df2;
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
    private SignalProcess signalProcess;
    private FindPeaks findPeaks;
    public CleanFile cleanFile;
    private FileMaker fileMaker = new FileMaker(this);


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
        lineChart = findViewById(R.id.linechart);
        chart_df = findViewById(R.id.chart_df);
//        chart_df2 = findViewById(R.id.chart_df2);
        chart_df3 = findViewById(R.id.chart_df3);

        initchart();//初始化圖表
        initObject();//初始化物件
        initDeviceDialog();//裝置選擇Dialog
        checkAndDisplayRegistrationStatus();//檢查註冊狀態

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取得應用程式專用的外部資料夾
        File externalFilesDir = getExternalFilesDir(null);
        if (externalFilesDir != null && externalFilesDir.isDirectory()) {
            // 列出所有文件
            File[] files = externalFilesDir.listFiles();
            if (files != null) {
                for (File file : files) {
                    // 檢查檔案名稱是否符合特定的前綴和後綴
                    if (file.getName().endsWith(".lp4") && file.getName().startsWith("l_") || file.getName().endsWith(".cha") && file.getName().startsWith("l_")) {
                        // 刪除符合條件的文件
                        boolean deleted = file.delete();
                        if (!deleted) {
                            Log.e("DeleteTempFiles", "Failed to delete file: " + file.getAbsolutePath());
                        }
                    }
                }
            }
        }
        if (deviceDialog != null && deviceDialog.isShowing()) {
            deviceDialog.dismiss();
        }
//        ShowToast("已清除暫存檔案");
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
        txt_average = findViewById(R.id.txt_average);
        txt_countDown = findViewById(R.id.txt_countDown);
        txt_BleStatus = findViewById(R.id.txt_BleStatus);
        txt_checkID_status = findViewById(R.id.txt_checkID_status);
        txt_checkID_result = findViewById(R.id.txt_checkID_result);
        txt_Register_values = findViewById(R.id.txt_Register_values);

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopALL();
            }
        });
        btn_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    tinyDB.clear();
                    editor.clear();
                    editor.apply();
                    ShowToast("已清除註冊檔案");
                    txt_checkID_status.setText("尚未有註冊資料");
                    txt_checkID_result.setText("");
                    txt_Register_values.setText("");
                });
//                processAllCHAFilesInDirectory(Environment.getExternalStorageDirectory().getAbsolutePath() + "/5cha");
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
                txt_Register_values.setText("");
                txt_average.setText("");
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
                    if (!bt4.file_data.isEmpty()) {
                        saveLP4(bt4.file_data);
                    } else {
                        ShowToast("檔案大小為0");
                    }
                    bt4.Delete_AllRecor(this);
                }

                if (step[0] == 4) {
                    bt4.Delete_AllRecor(this);
                    bt4.file_data.clear();
                    bt4.Buffer_Array.clear();
                }
                step[0]++;
            }
        });
    }

    private void saveLP4(ArrayList<Byte> file_data) {
        String tempFilePath = null; // 用於保存生成的臨時文件路徑
        try {
            // 格式化當前日期時間作為文件名一部分
            String date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(System.currentTimeMillis());
            String prefix = "l_" + date + "_888888_"; // 檔案前綴
            String suffix = ".lp4"; // 檔案後綴

            // 在應用專用的外部文件夾中建立臨時文件
            File tempFile = File.createTempFile(prefix, suffix, getExternalFilesDir(null));
            tempFilePath = tempFile.getAbsolutePath(); // 獲取臨時文件的完整路徑

            // 將byte列表數據寫入臨時文件
            fileMaker.saveByteArrayToFile(file_data, tempFile);

            runOnUiThread(() -> {
                // 更新UI，例如顯示保存成功的提示
                ShowToast("計算中...");
                MediaScannerConnection.scanFile(this, new String[]{tempFile.getAbsolutePath()}, null, null);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        readLP4(tempFilePath);
    }

    private void readLP4(String tempFilePath) {
        File file = new File(tempFilePath);
        if (!file.exists()) {
            Log.e("LP4FileNotFound", "LP4檔案不存在：" + tempFilePath);
            return;
        }

        // decpEcgFil將LP4檔案解碼為CHA格式的方法
        decpEcgFile(tempFilePath);

        // 根據LP4檔案路徑建立對應的CHA檔案名稱
        String chaFileName = file.getName().replace(".lp4", ".CHA");
        String chaFilePath = new File(file.getParent(), chaFileName).getAbsolutePath();

        Log.d("FilePaths", "LP4 Path: " + tempFilePath);
        Log.d("FilePaths", "CHA Path: " + chaFilePath);

        // 繼續處理CHA檔案，例如讀取和分析
        readCHA(chaFilePath);
    }

    public void readCHA(String chaFilePath) {
        File chaFile = new File(chaFilePath);
        if (!chaFile.exists()) {
            Log.e("CHAFileNotFound", "CHA檔案不存在：" + chaFilePath);
            return;
        }
        fileName = chaFile.getName();//取得檔案名稱
        DecodeCha decodeCha = new DecodeCha(chaFilePath);
        decodeCha.run();
        calculateRR(decodeCha.finalCHAData);
    }

    public void calculateRR(List<Float> dataList) {
        if (dataList == null || dataList.isEmpty()) {
            Log.e("EmptyDataList", "dataList為空");
            return;
        }
        try {
            findPeaks = new FindPeaks(dataList);
            findPeaks.run();
            findPeaks.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        if (findPeaks != null && findPeaks.ecgSignal != null) {
            calMidError(findPeaks.ecgSignal);
            chartSetting.markRT(chart_df3, findPeaks.ecgSignal, findPeaks.rWaveIndices, findPeaks.tWaveIndices, findPeaks.qWaveIndices);
//            fileMaker.makeCSVFloatArray(findPeaks.ecgSignal, "ecgSignal.csv");
        } else {
            Log.e("NullObjectReference", "findPeaks or findPeaks.ecgSignal is null");
        }
    }

    public void calMidError(Float[] floats) {
        List<Integer> R_index = findPeaks.rWaveIndices;
        Log.d("gggg", "calMidError: " + R_index.size());
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (R_index.size() > 12) {
                    //取得已經過濾過的RR100(4張圖)
                    List<Float> df1 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(10), R_index.get(12));
                    List<Float> df2 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(3), R_index.get(5));
                    List<Float> df3 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(6), R_index.get(8));
                    List<Float> df4 = signalProcess.getReduceRR100(Arrays.asList(floats), R_index.get(8), R_index.get(10));

                    chartSetting.overlapChart(chart_df, df1, df2, df3, df4, Color.CYAN, Color.RED);

                    //計算自己的差異
                    float diff12 = signalProcess.calMidDiff(df1, df2);
                    float diff13 = signalProcess.calMidDiff(df1, df3);
                    float diff14 = signalProcess.calMidDiff(df1, df4);
                    float diff23 = signalProcess.calMidDiff(df2, df3);

                    //計算(最新一筆)
                    averageDiff4Num_self = (diff12 + diff13 + diff14 + diff23) / 4;
                    R_Med = findPeaks.calculateMedian(findPeaks.rWavePeaks);
                    R_VoltMed = findPeaks.calVoltDiffMed(findPeaks.ecgSignal, findPeaks.rWaveIndices, findPeaks.tWaveIndices);
                    RT_distanceMed = findPeaks.calDistanceDiffMed(findPeaks.rWaveIndices, findPeaks.tWaveIndices);
                    halfWidth = findPeaks.calculateHalfWidths(findPeaks.ecgSignal, findPeaks.rWaveIndices);
                    Log.d("why0", "R_Med: "+R_Med+"R_VoltMed:   "+R_VoltMed+"RT_distanceMed:   "+RT_distanceMed+"halfWidth:   "+halfWidth);
                    // 儲存測量結果並檢查註冊狀態
                    saveResultAndCheckRegistrationStatus();
                    //畫圖
//            chartSetting.overlapChart(chart_df2, diffValueSB, df2, df3, df4, Color.BLACK, Color.parseColor("#F596AA"));
                }
            }
        }).start();

    }

    public void saveResultAndCheckRegistrationStatus() {
        if (Math.abs(averageDiff4Num_self) < 1 && halfWidth < 100) {
            averageDiff4NumSelfList.add(averageDiff4Num_self);
            R_MedList.add(R_Med);
            R_VoltMedList.add(R_VoltMed);
            RT_distanceMedList.add(RT_distanceMed);
            halfWidthList.add(halfWidth);
            saveMeasurementResultsToTinyDB();// 將測量結果保存到TinyDB
            // 更新UI
            if (averageDiff4NumSelfList.size() > 3) {
                txt_checkID_status.setText("繼續量測下一筆吧!");
                txt_checkID_result.setText("量測成功!");
            }else {
                txt_checkID_status.setText("註冊還需: (" + (averageDiff4NumSelfList.size()) + "/3)"); // 更新註冊狀態
                txt_checkID_result.setText("量測成功!");
            }
        }else {
            txt_checkID_result.setText("量測失敗，請重新測量");
            txt_checkID_status.setText("註冊還需: (" + averageDiff4NumSelfList.size() + "/3)"); // 量測失敗，不更新註冊狀態
        }

        if (averageDiff4NumSelfList.size() == 3){
            txt_checkID_status.setText("註冊還需: (" + (averageDiff4NumSelfList.size()) + "/3)");
            txt_checkID_status.setText("註冊完成");
        }

        // 更新UI
        runOnUiThread(() -> updateUIWithMeasureResultsAndOutputCSV());
    }

    private void updateUIWithMeasureResultsAndOutputCSV() {
        String diff_UIValue;
        String diff_value_toExcel;
        String r_value = "\nR電壓中位數:" + R_Med;
        String r_halfWidth = "\n半高寬:" + halfWidth;
        String rt_voltMed = "/RT電壓差:" + R_VoltMed;
        String rt_distanceMed = "/RT距離:" + RT_distanceMed;

        ArrayList<Float> numSelfList = tinyDB.getListFloat("averageDiff4NumSelfList");

        if (numSelfList.size() > 3) {
            float selfDiff = calDiffArray(averageDiff4Num_self, R_Med, halfWidth, R_VoltMed, RT_distanceMed);
            diff_UIValue = String.format("自己當下差異度: %s/與註冊時差異度: %s", averageDiff4Num_self, selfDiff + r_value + r_halfWidth + rt_voltMed + rt_distanceMed);
            diff_value_toExcel = fileName + "," + averageDiff4Num_self + "," + selfDiff + "," + R_Med + "," + halfWidth + "," + R_VoltMed + "," + RT_distanceMed;
        } else {
            diff_UIValue = String.format("自己當下差異度: %s", averageDiff4Num_self + r_value + r_halfWidth + rt_voltMed + rt_distanceMed);
            diff_value_toExcel = fileName + "," + averageDiff4Num_self + "," + diffValueSB + "," + R_Med + "," + halfWidth + "," + R_VoltMed + "," + RT_distanceMed;
        }
        txt_result.setText(diff_UIValue);
        recordOutputToCSV(diff_value_toExcel);

        if (numSelfList.size() == 4) {
            minusListLastOne();// 減少最後一筆資料
        }
        saveMeasurementResultsToTinyDB();
    }

    private float calDiffArray(float self, float R_Med, float halfWidth, float R_VoltMed, float RT_distanceMed) {
        ArrayList<Float> averageDiff4NumSelfList = tinyDB.getListFloat("averageDiff4NumSelfList");
        ArrayList<Float> R_MedList = tinyDB.getListFloat("R_MedList");
        ArrayList<Float> halfWidthList = tinyDB.getListFloat("halfWidthList");
        ArrayList<Float> R_VoltMedList = tinyDB.getListFloat("R_VoltMedList");
        ArrayList<Float> RT_distanceMedList = tinyDB.getListFloat("RT_distanceMedList");

        float averageSelf = findPeaks.calculateAverageFloat(averageDiff4NumSelfList);
        float averageRMed = findPeaks.calculateAverageFloat(R_MedList);
        float averageHalfWidth = findPeaks.calculateAverageFloat(halfWidthList);
        float averageRVoltMed = findPeaks.calculateAverageFloat(R_VoltMedList);
        float averageRTDistanceMed = findPeaks.calculateAverageFloat(RT_distanceMedList);

        float selfDiff = (self - averageSelf) / averageSelf;
        float RMedDiff = (R_Med - averageRMed) / averageRMed;
        float halfWidthDiff = (float)(halfWidth - averageHalfWidth) / averageHalfWidth;
        Log.d("halfwidth", "halfwidth: " + halfWidth + " - " + averageHalfWidth + " / " + averageHalfWidth);
        float RVoltMedDiff = (R_VoltMed - averageRVoltMed) / averageRVoltMed;
        float RTDistanceMedDiff = (RT_distanceMed - averageRTDistanceMed) / averageRTDistanceMed;
        txt_average.setText("註冊標準" +"\n自己當下差異度: " + averageSelf + "/與註冊時差異度: " + averageSelf + "\nR電壓中位數差異: " + averageRMed + "/半高寬差異: " + averageHalfWidth + "\nR到T電壓差異: " + averageRVoltMed + "/R到T距離差異: " + averageRTDistanceMed);
        saveDiffStandardToTinyDB(selfDiff, RMedDiff, halfWidthDiff, RVoltMedDiff, RTDistanceMedDiff);// 將差異度標準保存到TinyDB

        String resultText = String.format("與註冊3筆比較" + "\n自己當下差異度: %.3f/與註冊時差異度: %.3f\nR電壓中位數差異: %.3f/半高寬差異: %.3f\nR到T電壓差異: %.3f/R到T距離差異: %.3f",
                selfDiff, selfDiff, RMedDiff, halfWidthDiff, RVoltMedDiff, RTDistanceMedDiff);

        runOnUiThread(() -> txt_Register_values.setText(resultText));
        return selfDiff;
    }

    public void minusListLastOne() {
        if (!averageDiff4NumSelfList.isEmpty()) {
            averageDiff4NumSelfList.remove(averageDiff4NumSelfList.size() - 1);
        }
        if (!R_MedList.isEmpty()) {
            R_MedList.remove(R_MedList.size() - 1);
        }
        if (!halfWidthList.isEmpty()) {
            halfWidthList.remove(halfWidthList.size() - 1);
        }
        if (!R_VoltMedList.isEmpty()) {
            R_VoltMedList.remove(R_VoltMedList.size() - 1);
        }
        if (!RT_distanceMedList.isEmpty()) {
            RT_distanceMedList.remove(RT_distanceMedList.size() - 1);
        }
    }

    public void recordOutputToCSV(String finalResult) {
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

                fileMaker.writeRecordToFile(recordList);
            }
        }).start();
    }

    public void saveMeasurementResultsToTinyDB() {
        tinyDB.putListFloat("averageDiff4NumSelfList", averageDiff4NumSelfList);
        tinyDB.putListFloat("averageDiff4NumSbList", averageDiff4NumSbList);
        tinyDB.putListFloat("R_MedList", R_MedList);
        tinyDB.putListFloat("R_VoltMedList", R_VoltMedList);
        tinyDB.putListFloat("RT_distanceMedList", RT_distanceMedList);
        tinyDB.putListFloat("halfWidthList", halfWidthList);
    }

    private void saveDiffStandardToTinyDB(float selfDiff, float RMedDiff, float halfWidthDiff, float RVoltMedDiff, float RTDistanceMedDiff) {
        tinyDB.putFloat("selfDiffRule", selfDiff);
        tinyDB.putFloat("RMedDiffRule", RMedDiff);
        tinyDB.putFloat("halfWidthDiffRule", halfWidthDiff);
        tinyDB.putFloat("RVoltMedDiffRule", RVoltMedDiff);
        tinyDB.putFloat("RTDistanceMedDiffRule", RTDistanceMedDiff);
    }

    public void checkAndDisplayRegistrationStatus() {
        averageDiff4NumSelfList = tinyDB.getListFloat("averageDiff4NumSelfList");
        averageDiff4NumSbList = tinyDB.getListFloat("averageDiff4NumSbList");
        R_MedList = tinyDB.getListFloat("R_MedList");
        R_VoltMedList = tinyDB.getListFloat("R_VoltMedList");
        RT_distanceMedList = tinyDB.getListFloat("RT_distanceMedList");
        halfWidthList = tinyDB.getListFloat("halfWidthList");

        if (averageDiff4NumSelfList.size() == 0) {
            runOnUiThread(() -> {
                txt_checkID_status.setText("尚未有註冊資料");
            });
        } else {
            runOnUiThread(() -> {
                txt_checkID_status.setText("註冊所需檔案(" + averageDiff4NumSelfList.size() + "/3)");
            });
        }

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

    /**
     * 遍歷指定目錄下的所有文件，並對每個CHA文件執行readCHA操作。
     *
     * @param directoryPath 要遍歷的目錄路徑。
     */
    public void processAllCHAFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        // 確保該路徑是目錄
        if (directory.isDirectory()) {
            // 取得目錄下所有檔案（和目錄）
            File[] files = directory.listFiles();

            // 確保files不為null
            if (files != null) {
                for (File file : files) {
                    Log.d("qweqwe", ": " + file);
                    // 確保是檔案而不是目錄，並且檔案名稱以.cha結尾
                    if (file.isFile() && file.getName().endsWith(".CHA")) {
                        // 取得檔案的絕對路徑和檔案名
                        String filePath = file.getParent();
                        String fileName = file.getName();

                        // 對每個CHA檔案執行readCHA操作
                        readCHA(filePath);
                        String s = filePath + File.separator + fileName;
                    }
                }
            } else {
                Log.e("ProcessCHAError", "指定目錄沒有找到檔案：" + directoryPath);
            }
        } else {
            Log.e("ProcessCHAError", "指定的路徑不是一個目錄：" + directoryPath);
        }
    }

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