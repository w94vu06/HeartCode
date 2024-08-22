package com.example.newidentify;

import static com.example.newidentify.util.ChartSetting.Butterworth;
import static com.example.newidentify.util.ChartSetting.getStreamLP;
import static java.lang.Math.abs;
import static java.lang.Math.getExponent;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.example.newidentify.bluetooth.BT4;
import com.example.newidentify.mfcc.MFCCProcess;
import com.example.newidentify.util.ReadCSV;
import com.example.newidentify.process.HeartRateData;
import com.example.newidentify.util.ChartSetting;
import com.example.newidentify.util.CleanFile;
import com.example.newidentify.util.EcgMath;
import com.example.newidentify.util.TinyDB;
import com.example.newidentify.process.DecodeCha;
import com.example.newidentify.util.FileMaker;
import com.example.newidentify.process.CalculateDiffMean;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.listener.BarLineChartTouchListener;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity {
    private static final org.apache.commons.logging.Log log = LogFactory.getLog(MainActivity.class);
    // HeartRateData 物件，用於儲存心率數據
    private HeartRateData heartRateData;
    // Gson 物件
    private Gson gson = new Gson();
    // TinyDB 物件
    private TinyDB tinyDB;

    // UI 元素
    private Button btn_detect;
    private Button btn_clear_registry;
    private Button btn_stop;
    public static TextView txt_isMe;
    public static TextView txt_detect_result;
    public static TextView txt_checkID_status;
    public static TextView txt_checkID_result;
    public static TextView txt_BleStatus;
    public static TextView txt_BleStatus_battery;

    // 選擇裝置的對話框
    private Dialog deviceDialog;

    // 參數
    public boolean isFinishRegistered = false; // 是否已註冊完成的標誌

    // BLE 相關變數
    public static Activity global_activity;
    public static TextView txt_countDown;
    public static BT4 bt4;

    // 繪製 ECG 圖表所需的變數
    private final Handler countDownHandler = new Handler();
    public CountDownTimer countDownTimer; // 倒數計時器
    boolean isCountDownRunning = false;
    boolean isMeasurementOver = false;

    private static final int TOAST_DISPLAY_TIME = 5000;
    private static final int COUNTDOWN_INTERVAL = 1000;
    private static final int COUNTDOWN_TOTAL_TIME = 30000;

    public static LineChart lineChart;
    public static LineChart chart_df;
    public static LineChart chart_df2;
    public static LineDataSet chartSet1 = new LineDataSet(null, "");
    private ChartSetting chartSetting;
    public static ArrayList<Entry> chartSet1Entries = new ArrayList<>();
    public static ArrayList<Double> oldValue = new ArrayList<>();

    private CalculateDiffMean calculateDiffMean;
    private EcgMath ecgMath = new EcgMath();
    public CleanFile cleanFile;
    private FileMaker fileMaker = new FileMaker(this);
    public double[] rawEcgSignal;

    private ArrayList<String> registerData = new ArrayList<>();
    private ArrayList<ArrayList<double[]>> mfccArrayList = new ArrayList<>();

    // Python 相關變數
    public static Python py;
    public static PyObject pyObj;

    private Queue<File> fileQueue = new ArrayDeque<>();
    private Set<String> processedFiles = new HashSet<>();

    static {
        System.loadLibrary("newidentify");
        System.loadLibrary("lp4tocha");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // 初始化全域變數
        global_activity = this;

        //初始化物件
        bt4 = new BT4(global_activity);
        tinyDB = new TinyDB(global_activity);
        deviceDialog = new Dialog(global_activity);
        calculateDiffMean = new CalculateDiffMean();
        cleanFile = new CleanFile();
        chartSetting = new ChartSetting();
        lineChart = findViewById(R.id.linechart);
        chart_df = findViewById(R.id.chart_df);
        chart_df2 = findViewById(R.id.chart_df2);

        initchart();//初始化圖表
        initObject();//初始化物件
        initDeviceDialog();//裝置選擇Dialog

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        initBroadcast();
        setScreenOn();
        //延遲載入圖表
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndDisplayRegistrationStatus();//檢查註冊狀態
            }
        }, 1000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (countDownTimer != null) {
            stop_detection();
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
        stop_detection();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (deviceDialog != null && deviceDialog.isShowing()) {
            deviceDialog.dismiss();
        }
    }

    /**
     * 註冊廣播過濾，用於接收藍芽連接狀態
     */
    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void initBroadcast() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BT4.BLE_CONNECTED);
        intentFilter.addAction(BT4.BLE_TRY_CONNECT);
        intentFilter.addAction(BT4.BLE_DISCONNECTED);
        intentFilter.addAction(BT4.BLE_READ_FILE);
        intentFilter.addAction(BT4.BLE_READ_BATTERY);
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
            } else if (BT4.BLE_READ_BATTERY.equals(action)) {
                txt_BleStatus_battery.setText(bt4.Battery_Percent + "%");
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
        txt_isMe = findViewById(R.id.txt_isMe);
        txt_detect_result = findViewById(R.id.txt_detect_result);
        txt_checkID_status = findViewById(R.id.txt_checkID_status);
        txt_checkID_result = findViewById(R.id.txt_checkID_result);

        txt_countDown = findViewById(R.id.txt_countDown);
        txt_BleStatus = findViewById(R.id.txt_BleStatus);
        txt_BleStatus_battery = findViewById(R.id.txt_BleStatus_battery);

        initBtn();
    }

    public void initBtn() {
        btn_stop = findViewById(R.id.btn_stop);
        btn_clear_registry = findViewById(R.id.btn_clear_registry);
        btn_detect = findViewById(R.id.btn_detect);

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop_detection(); //停止量測

                HandlerThread handlerThread = new HandlerThread("MFCCThread");
                handlerThread.start();
                Handler handler = new Handler(handlerThread.getLooper());

                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        processChaFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/l_20240821140649_888888_1187332922578169732.cha");
//                        calculateMFCCFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/20240718161704_ecg_signal.csv");
                    }
                });
            }
        });

        btn_clear_registry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    tinyDB.clear();
                    ShowToast("已清除註冊檔案");

                    resetUI();
                    chart_df.clear();
                    chart_df2.clear();
                    cleanRegistrationData();

                    isFinishRegistered = false;
                });
            }
        });

    }

    // 初始化圖表
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

        chartSet1Entries.clear(); // 清除圖表數據
        oldValue.clear(); // 清除圖表數據

        lineChart.invalidate();
    }

    public void resetUI() {
        runOnUiThread(() -> {
            txt_isMe.setText("");
            txt_detect_result.setText("");
            txt_checkID_result.setText("");
            txt_checkID_status.setText("");
        });
    }

    public void cleanRegistrationData() {
        // 將註冊標準值清空並保存到 TinyDB
        registerData.clear();
        tinyDB.putListString("registerData", registerData);

        mfccArrayList.clear();
        tinyDB.putDoubleArrayListArray("mfccArrayList", mfccArrayList);
    }

    /**
     * 初始化裝置選擇Dialog
     **/
    public void initDeviceDialog() {
        deviceDialog.setContentView(R.layout.dialog_device);
        deviceDialog.setCancelable(false);

        RadioGroup devicesRadioGroup = deviceDialog.findViewById(R.id.devicesRadioGroup);
        Button completeButton = deviceDialog.findViewById(R.id.completeButton);

        completeButton.setOnClickListener(view -> {
            int checkedRadioButtonId = devicesRadioGroup.getCheckedRadioButtonId();
            if (checkedRadioButtonId == R.id.radioButtonDevice1) {
                setDeviceNameAndInit("CmateH");
            } else if (checkedRadioButtonId == R.id.radioButtonDevice2) {
                setDeviceNameAndInit("WTK230");
            } else {
                bt4.deviceName = "";
                Toast.makeText(global_activity, "請選擇一個裝置", Toast.LENGTH_SHORT).show();
            }
        });

        deviceDialog.show();
    }

    private void setDeviceNameAndInit(String deviceName) {
        bt4.deviceName = deviceName;
        bt4.Bluetooth_init();
        deviceDialog.dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    /**
     * 檢查註冊狀態
     */
    public void checkAndDisplayRegistrationStatus() {
        //確認MFCC註冊狀態
        ArrayList<double[]> registeredData = new ArrayList<>();// 讀取註冊資料
        registeredData.add(tinyDB.getDoubleArray("mfccRegiList1"));
        registeredData.add(tinyDB.getDoubleArray("mfccRegiList2"));
        registeredData.add(tinyDB.getDoubleArray("mfccRegiList3"));
        registeredData.add(tinyDB.getDoubleArray("mfccRegiList4"));

        if (registeredData.get(0) != null) {
            mfccArrayList.add(registeredData);
            txt_checkID_status.setText("註冊完成");
            double[] regiDrawList1 = tinyDB.getDoubleArray("mfccDrawList1");
            double[] regiDrawList2 = tinyDB.getDoubleArray("mfccDrawList2");
            double[] regiDrawList3 = tinyDB.getDoubleArray("mfccDrawList3");
            double[] regiDrawList4 = tinyDB.getDoubleArray("mfccDrawList4");
            runOnUiThread(() -> {
                chartSetting.overlapArrayChart(chart_df, regiDrawList1, regiDrawList2, regiDrawList3, regiDrawList4);
                chartSetting.setOverlapChartDescription(chart_df, "註冊MFCC圖");
            });
        } else {
            txt_checkID_status.setText("尚未有註冊資料");
        }
    }

    public static void DrawChart(byte[] result) {
        try {
            global_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < result.length; i++) ;
                    double ch2 = 0;

                    ch2 = bt4.byteArrayToInt(new byte[]{result[4]}) * 128 + bt4.byteArrayToInt(new byte[]{result[5]});
                    ch2 = ch2 * 1.7;

                    double ch4 = getStreamLP((int) ch2);

                    if (ch4 >= 2500) {
                        ch4 = 2500;
                    } else if (ch4 <= 1600) {
                        ch4 = 1600;
                    }

                    // 計算平均值
                    double sum = 0;
                    for (double value : oldValue) {
                        sum += value;
                    }
                    double average = !oldValue.isEmpty() ? sum / oldValue.size() : 0;

                    // 如果ch4 小於平均值 就等於平均值
                    if (ch4 < average * 0.9) {
                        ch4 = average * 0.9;
                    }

                    if (chartSet1Entries.size() >= 300) {
                        ArrayList<Entry> temp = new ArrayList<Entry>();
                        ArrayList<Double> temp_old = new ArrayList<Double>();
                        for (int i = 1; i < chartSet1Entries.size(); i++) {
                            if (i < oldValue.size()) {
                                Entry chartSet1Entrie = new Entry(temp.size(), chartSet1Entries.get(i).getY());
                                temp.add(chartSet1Entrie);
                                temp_old.add(oldValue.get(i));
                            }
                        }
                        chartSet1Entries = temp;
                        oldValue = temp_old;
                    }

                    oldValue.add(ch4);

                    double nvalue = (oldValue.get(oldValue.size() - 1));

                    if (oldValue.size() > 1) {
                        nvalue = Butterworth(oldValue);
                    }
                    if (oldValue.size() > 110) {
                        Entry chartSet1Entrie = new Entry(chartSet1Entries.size(), (float) nvalue);
                        chartSet1Entries.add(chartSet1Entrie);
//                        Log.d("drawData", "run: "+nvalue);
                    }
                    chartSet1.setValues(chartSet1Entries);
                    lineChart.setData(new LineData(chartSet1));
                    lineChart.setVisibleXRangeMinimum(300);
                    lineChart.invalidate();
                }
            });

        } catch (Exception ex) {
            Log.e("DrawChartError", "繪製圖表時出錯", ex);
        }
    }


    @SuppressLint("HandlerLeak")
    public void startWaveMeasurement(View view) {
        bt4.Bluetooth_init();

        if (bt4.isConnected && !isCountDownRunning) {
            runOnUiThread(() -> {
                resetUI();
                chart_df2.clear();
                initchart();
            });
            isMeasurementOver = false;
            startCountDownTimer();
            bt4.startWave(true, new Handler());
        } else {
            ShowToast("請先連接裝置");
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

    private void startCountDownTimer() {
        isCountDownRunning = true;
        isMeasurementOver = false;

        txt_detect_result.setText("量測結果");
        countDownHandler.postDelayed(new CountDownRunnable(), COUNTDOWN_INTERVAL);
    }

    private class CountDownRunnable implements Runnable {
        private int presetTime = TOAST_DISPLAY_TIME; //量測前倒數
        private int remainingTime = COUNTDOWN_TOTAL_TIME; //總倒數時間
        private boolean isToastShown = false;

        @Override
        public void run() {
            if (!isToastShown) {
                showToast();
                isToastShown = true;
            }

            if (presetTime <= 0) {
                startMeasurementCountdown();
            } else {
                updatePresetTimeCountdown();
            }
        }

        private void showToast() {
            Toast.makeText(global_activity, "請將手指放置於裝置上，量測馬上開始", Toast.LENGTH_LONG).show();
        }

        private void startMeasurementCountdown() {
            bt4.is5SecOver = true;
            if (remainingTime <= 0) {
                endMeasurement();
            } else {
                updateMeasurementCountdown();
            }
        }

        private void endMeasurement() {
            txt_countDown.setText("30");
            stopWaveMeasurement();
            isCountDownRunning = false;
            isMeasurementOver = true;
        }

        private void updateMeasurementCountdown() {
            txt_countDown.setText(String.valueOf(remainingTime / 1000));
            remainingTime -= COUNTDOWN_INTERVAL;
            countDownHandler.postDelayed(this, COUNTDOWN_INTERVAL);
        }

        private void updatePresetTimeCountdown() {
            bt4.is5SecOver = false;
            txt_countDown.setText(String.valueOf(presetTime / 1000));
            presetTime -= COUNTDOWN_INTERVAL;
            countDownHandler.postDelayed(this, COUNTDOWN_INTERVAL);
        }
    }

    private void stop_detection() {
        if (isCountDownRunning) {
            countDownHandler.removeCallbacksAndMessages(null); // 移除倒數
            isCountDownRunning = false;
            txt_countDown.setText("30");
            stopWaveMeasurement();
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
                    bt4.File_Size(this);
                }

                if (step[0] == 1) {
                    bt4.Open_File(this);
                }

                if (step[0] == 2) {
                    bt4.ReadData(this);
                }

                if (step[0] == 3) {
                    if (!bt4.file_data.isEmpty()) {
                        processLP4(bt4.file_data);
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

    private void processLP4(ArrayList<Byte> fileData) {
        String tempFilePath = createTempFile(fileData);
        if (tempFilePath != null) {
            processTempFile(tempFilePath);
        }
    }

    /**
     * 建立臨時檔案
     */
    private String createTempFile(ArrayList<Byte> fileData) {
        String tempFilePath = null;
        try {
            String date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(System.currentTimeMillis());
            String prefix = "l_" + date + "_888888_";
            String suffix = ".lp4";

            File tempFile = File.createTempFile(prefix, suffix, getExternalFilesDir(null));
            tempFilePath = tempFile.getAbsolutePath();

            fileMaker.saveByteArrayToFile(fileData, tempFile);
        } catch (IOException e) {
            Log.e("TempFileCreationError", "臨時文件創建失敗", e);
        }
        return tempFilePath;
    }

    /**
     * 將臨時檔案解碼並處理
     */
    private void processTempFile(String tempFilePath) {
        if (fileExists(tempFilePath)) {
            decodeEcgFile(tempFilePath);
            String chaFilePath = tempFilePath.replace(".lp4", ".cha");
            processChaFile(chaFilePath);
        } else {
            Log.e("LP4FileNotFound", "LP4檔案不存在：" + tempFilePath);
        }
    }

    private boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private void decodeEcgFile(String filePath) {
        try {
            decpEcgFile(filePath);
        } catch (Exception e) {
            Log.e("EcgFileDecodingError", "LP4檔案解碼失敗：" + filePath, e);
        }
    }

    private void processChaFile(String chaFilePath) {
        if (fileExists(chaFilePath)) {
            new Thread(() -> {
                DecodeCha decodeCha = new DecodeCha(chaFilePath);
                decodeCha.run();

                rawEcgSignal = decodeCha.ecgSignal;
                calculateMFCC(rawEcgSignal);
            }).start();
        } else {
            Log.e("CHAFileNotFound", "CHA檔案不存在：" + chaFilePath);
        }
    }

    public void calculateMFCCFile(String filePath) {
        ReadCSV csv = new ReadCSV();
        File csvFile = new File(filePath);

        try {
            // 取得MFCC特徵列表
            double[] mfccCSV = csv.processCSV(csvFile);
            calculateMFCC(mfccCSV);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void calculateMFCC(double[] doubles) {
        ArrayList<ArrayList<float[]>> regiList = tinyDB.getFloatArrayListArray("mfccArrayList");

        if (regiList.isEmpty()) {
            // 量測一筆，將一筆資料拆成兩段以計算閥值
//            registerMFCC(doubles);
            MFCCProcess mfccProcess = new MFCCProcess();
            mfccProcess.mfccRegister(doubles);
        } else {
            loginMFCC(doubles);
        }
    }

    public void registerMFCC(double[] doubles) {
        MFCCProcess MFCCProcess = new MFCCProcess();
        // 註冊資料
        ArrayList<double[]> mfccRegiList = MFCCProcess.mfccProcess(doubles);

        mfccArrayList.add(mfccRegiList);

        if (!mfccArrayList.isEmpty()) {

            tinyDB.putDoubleArrayListArray("mfccArrayList", mfccArrayList);

            tinyDB.putDoubleArray("mfccRegiList1", mfccRegiList.get(0));
            tinyDB.putDoubleArray("mfccRegiList2", mfccRegiList.get(1));
            tinyDB.putDoubleArray("mfccRegiList3", mfccRegiList.get(2));
            tinyDB.putDoubleArray("mfccRegiList4", mfccRegiList.get(3));

            tinyDB.putDoubleArray("mfccDrawList1", mfccRegiList.get(4));
            tinyDB.putDoubleArray("mfccDrawList2", mfccRegiList.get(5));
            tinyDB.putDoubleArray("mfccDrawList3", mfccRegiList.get(6));
            tinyDB.putDoubleArray("mfccDrawList4", mfccRegiList.get(7));

            double[] savedData = tinyDB.getDoubleArray("mfccRegiList1");
            double[] savedData1 = tinyDB.getDoubleArray("mfccRegiList2");
            double[] savedData2 = tinyDB.getDoubleArray("mfccRegiList3");
            double[] savedData3 = tinyDB.getDoubleArray("mfccRegiList4");

        } else {
            txt_checkID_status.setText("註冊失敗");
            Log.e("mfcc", "registerMFCC: 註冊失敗");
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    txt_checkID_status.setText("註冊完成");
                    chartSetting.overlapArrayChart(chart_df, mfccRegiList.get(4), mfccRegiList.get(5), mfccRegiList.get(6), mfccRegiList.get(7));
                    chartSetting.setOverlapChartDescription(chart_df, "註冊MFCC圖");
                } catch (Exception e) {
                    Log.d("mfcc", "registerMFCC: " + e);
                    txt_detect_result.setText("MFCC計算失敗");
                }
            }
        });
    }

    public void loginMFCC(double[] doubles) {
        MFCCProcess mfccProcess = new MFCCProcess();
        double distance = 9999;
        ArrayList<double[]> mfccLoginList = mfccProcess.mfccProcess(doubles);
        try {
            ArrayList<double[]> registeredData = new ArrayList<>();// 第一筆為註冊資料
            registeredData.add(tinyDB.getDoubleArray("mfccRegiList1"));
            registeredData.add(tinyDB.getDoubleArray("mfccRegiList2"));
            registeredData.add(tinyDB.getDoubleArray("mfccRegiList3"));
            registeredData.add(tinyDB.getDoubleArray("mfccRegiList4"));

            ArrayList<double[]> loginData = new ArrayList<>(); // 第二筆為登入資料
            loginData.add(mfccLoginList.get(0));
            loginData.add(mfccLoginList.get(1));
            loginData.add(mfccLoginList.get(2));
            loginData.add(mfccLoginList.get(3));

            // 計算歐式距離
            distance = mfccProcess.euclideanDistanceProcessor(registeredData, loginData);

        } catch (Exception e) {
            Log.d("mfcc", "loginMFCC: " + e);
        }

        double finalDistance = distance;
        showMFCCResult(finalDistance, mfccLoginList);
    }

    private void showMFCCResult(double finalDistance, ArrayList<double[]> mfccLoginList) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    txt_checkID_result.setText("歐式相似度: " + String.format("%.2f", finalDistance));
                    if (finalDistance > 190) {
                        txt_isMe.setText("非本人");
                    } else {
                        txt_isMe.setText("本人");
                    }
                    chartSetting.overlapArrayChart(chart_df2, mfccLoginList.get(4), mfccLoginList.get(5), mfccLoginList.get(6), mfccLoginList.get(7));
                    chartSetting.setOverlapChartDescription(chart_df2, "登入MFCC圖");
                } catch (Exception e) {
                    Log.d("text", "run: " + e);
                }
            }
        });
        // 刪除登入資料
        keepRegisterListIsOne(mfccArrayList, 1);
        tinyDB.putDoubleArrayListArray("mfccArrayList", mfccArrayList);
    }

    /**
     * 保持註冊數據只有1筆
     */
    public void keepRegisterListIsOne(List<?> list, int size) {
        while (list.size() > size) {
            list.remove(list.size() - 1);
        }
    }


    /**
     * 遍歷指定目錄下的所有文件，並對每個CHA文件執行processChaFile操作。
     */

    public void initializeFileQueue(String directoryPath) {
        File directory = new File(directoryPath);
        // 確保該路徑是目錄
        if (directory.isDirectory()) {
            // 取得目錄下所有檔案（和目錄）
            File[] files = directory.listFiles();

            // 確保files不為null
            if (files != null) {
                for (File file : files) {
                    // 將檔案加入隊列
                    fileQueue.add(file);
                }
            } else {
                Log.e("ProcessCHAError", "指定目錄沒有找到檔案：" + directoryPath);
            }
        } else {
            Log.e("ProcessCHAError", "指定的路徑不是一個目錄：" + directoryPath);
        }
    }

    public void processNextFile() {
        if (!fileQueue.isEmpty()) {
            File file = fileQueue.poll();
            if (file != null && !processedFiles.contains(file.getName())) {
                // 處理文件的邏輯
                processFile(file);
                // 標記文件為已處理
                processedFiles.add(file.getName());
            } else {
                Log.i("ProcessCHAInfo", "文件已經被處理過：" + (file != null ? file.getName() : "未知文件"));
            }
        } else {
            Log.i("ProcessCHAInfo", "沒有更多文件可以處理");
        }
    }

    private void processFile(File file) {
        processChaFile(file.getAbsolutePath());
    }

    public void processAllCHAFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        // 確保該路徑是目錄
        if (directory.isDirectory()) {
            // 取得目錄下所有檔案（和目錄）
            File[] files = directory.listFiles();

            // 確保files不為null
            if (files != null) {
                Queue<File> fileQueue = new ArrayDeque<>();
                for (File file : files) {
                    // 將檔案加入隊列
                    fileQueue.add(file);
                }

                // 處理隊列中的每個文件
                processFilesQueue(fileQueue);
            } else {
                Log.e("ProcessCHAError", "指定目錄沒有找到檔案：" + directoryPath);
            }
        } else {
            Log.e("ProcessCHAError", "指定的路徑不是一個目錄：" + directoryPath);
        }
    }

    private void processFilesQueue(Queue<File> fileQueue) {
        while (!fileQueue.isEmpty()) {
            File file = fileQueue.poll();

            // 確保是檔案而不是目錄，並且檔案名稱以.cha結尾
            if (file.isFile() && (file.getName().endsWith(".cha") || file.getName().endsWith(".CHA"))) {
                // 對每個CHA檔案執行processChaFile操作
                processChaFile(file.getAbsolutePath());
            }
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