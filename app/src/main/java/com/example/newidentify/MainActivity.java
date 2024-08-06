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
import com.example.newidentify.process.HeartRateData;
import com.example.newidentify.util.ChartSetting;
import com.example.newidentify.util.CleanFile;
import com.example.newidentify.util.EcgMath;
import com.example.newidentify.util.TinyDB;
import com.example.newidentify.process.DecodeCha;
import com.example.newidentify.util.FileMaker;
import com.example.newidentify.process.CalculateDiffSelf;
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
    private TextView txt_isMe;
    private TextView txt_detect_result;
    private TextView txt_checkID_status;
    private TextView txt_checkID_result;
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
    public static ArrayList<Entry> chartSet1Entries = new ArrayList<Entry>();
    public static ArrayList<Double> oldValue = new ArrayList<Double>();

    private CalculateDiffSelf calculateDiffSelf;
    private EcgMath ecgMath = new EcgMath();
    public CleanFile cleanFile;
    private FileMaker fileMaker = new FileMaker(this);
    public double[] rawEcgSignal;

    private ArrayList<String> registerData = new ArrayList<>();

    // Python 相關變數
    public static Python py;
    public static PyObject pyObj;
    public double nk_process_time; // 計算NK2計算時間
    public long startTime; // 開始時間


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
        calculateDiffSelf = new CalculateDiffSelf();
        cleanFile = new CleanFile();
        chartSetting = new ChartSetting();
        lineChart = findViewById(R.id.linechart);
        chart_df = findViewById(R.id.chart_df);
        chart_df2 = findViewById(R.id.chart_df2);

        initchart();//初始化圖表
        initObject();//初始化物件

        initDeviceDialog();//裝置選擇Dialog
        checkAndDisplayRegistrationStatus();//檢查註冊狀態

        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
        py = Python.getInstance();
        pyObj = py.getModule("hrv_analysis");
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
                processAllCHAFilesInDirectory(Environment.getExternalStorageDirectory().getAbsolutePath() + "/testIdentifyApp" + "/s");
            }
        });

        btn_clear_registry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    tinyDB.clear();
                    ShowToast("已清除註冊檔案");

                    resetUI();
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
        registerData = tinyDB.getListString("registerData");

        if (registerData.isEmpty()) {
            Log.d("HRVList", "registerData.size: " + registerData.size());
            txt_checkID_status.setText("尚未有註冊資料");
        } else {
            txt_checkID_status.setText("註冊所需檔案(" + registerData.size() + "/3)");
            if (registerData.size() >= 3) {
                isFinishRegistered = true;
                txt_checkID_status.setText("註冊完成");
            }
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

                    oldValue.add((double) ch4);

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
//            Log.d("wwwww", "eeeeeerrr = " + ex.toString());
        }
    }


    @SuppressLint("HandlerLeak")
    public void startWaveMeasurement(View view) {
        bt4.Bluetooth_init();

        if (bt4.isConnected && !isCountDownRunning) {
            runOnUiThread(() -> {
                resetUI();
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
            bt4.isSixSecOver = true;
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
            bt4.isSixSecOver = false;
            txt_countDown.setText(String.valueOf(presetTime / 1000));
            presetTime -= COUNTDOWN_INTERVAL;
            countDownHandler.postDelayed(this, COUNTDOWN_INTERVAL);
        }
    }

    /**
     * 停止量測
     */
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
                processEcgSignal(rawEcgSignal);
            }).start();
        } else {
            Log.e("CHAFileNotFound", "CHA檔案不存在：" + chaFilePath);
        }
    }

    private void processEcgSignal(double[] ecgSignal) {
        new Thread(() -> {
            calculateWithPython(ecgSignal);
        }).start();
    }

    public void calculateWithPython(double[] ecg_signal) {
        if (ecg_signal == null || ecg_signal.length == 0) {
            Log.e("EmptyDataList", "dataList有誤");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 獲取 Python 實例和模塊
                try {
                    // 調用 Python 函數並獲取結果
                    ShowToast("計算中...");
                    startTime = System.currentTimeMillis(); // 紀錄開始時間

                    PyObject hrv_analysis = pyObj.callAttr("hrv_analysis", ecg_signal, 1000.0);

                    String date = new SimpleDateFormat("yyyyMMddHHmmss", Locale.getDefault()).format(System.currentTimeMillis());
                    fileMaker.makeCSVDoubleArray(ecg_signal, date + "_ecg_signal.csv");

                    getHRVFeature(hrv_analysis);
                } catch (Exception e) {
                    Log.e("PythonError", "Exception type: " + e.getClass().getSimpleName());
                    Log.e("PythonError", "Exception message: " + e.getMessage());
                    Log.e("PythonError", "Stack trace: ", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt_detect_result.setText("量測失敗");
                        }
                    });
                }
            }
        }).start();
    }

    /**
     * 取得 HRV 特徵
     */
    public void getHRVFeature(PyObject result) {
        long endTime = System.currentTimeMillis(); // 紀錄結束時間
        nk_process_time = (double) (endTime - startTime) / 1000; // 計算時間差
        Log.d("time", "nk_process_time: " + nk_process_time);

        PyObject hrv = result.asList().get(0);
        PyObject r_peaks = result.asList().get(1);
        PyObject r_value = result.asList().get(2);

        Log.d("getHRVData", "getHRVData: " + result);
        String hrvJsonString = hrv.toString().replaceAll("nan", "null").replaceAll("masked", "null").replaceAll("inf", "null");

        heartRateData = gson.fromJson(hrvJsonString, HeartRateData.class);

        if (heartRateData.getBpm() == 0.0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txt_detect_result.setText("量測失敗");
                }
            });
            return;
        }
        // 將 Python 返回的數據轉換為 Map
        Map<String, List<Integer>> rPeaksMap = gson.fromJson(r_peaks.toString(), new TypeToken<Map<String, List<Integer>>>() {
        }.getType());
        List<Integer> rPeaksList = rPeaksMap.get("r_peaks");

        Map<String, List<Double>> rValuesMap = gson.fromJson(r_value.toString(), new TypeToken<Map<String, List<Double>>>() {
        }.getType());
        List<Double> rValuesList = rValuesMap.get("r_values");

        Type signalMapType = new TypeToken<Map<String, List<Double>>>() {
        }.getType();
        Map<String, List<Double>> signalMap = gson.fromJson(String.valueOf(result.asList().get(3)), signalMapType);

        // 獲取 "ECG_Raw" 對應的 List<Double>
        List<Double> signalMapList = signalMap.get("ECG_Raw");

        heartRateData.setSignals(signalMapList);

        assert rPeaksList != null;
        assert rValuesList != null;

        calculateValues(rPeaksList, rValuesList);
    }

    /**
     * 計算特徵
     */
    public void calculateValues(List<Integer> r_indices, List<Double> r_values) {
        Collections.sort(r_indices);
        float diffSelf = calculateDiffSelf.calDiffSelf(ecgMath.doubleArrayToArrayListFloat(rawEcgSignal), r_indices);
        float halfWidth = ecgMath.calculateHalfWidths(ecgMath.doubleArrayToArrayListFloat(rawEcgSignal), r_indices);

        heartRateData.setDiffSelf(diffSelf);
        heartRateData.setR_Med(ecgMath.calculateMedian(ecgMath.listDoubleToListFloat(r_values)));
        heartRateData.setHalfWidth(halfWidth);
        heartRateData.setVoltStd(EcgMath.calculateStandardDeviation(rawEcgSignal));

        if (diffSelf == 9999f || Math.abs(diffSelf) > 1.5 || halfWidth == 0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txt_detect_result.setText("訊號穩定度過差");
                }
            });
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
//                chartSetting.markR(chart_df2, ecgMath.listDoubleToArrayListFloat(heartRateData.getSignals()), r_indices);
//                chartSetting.markRT(chart_df2, ecgMath.listDoubleToArrayListFloat(heartRateData.getSignals()), r_indices, heartRateData.getT_onsets(), heartRateData.getT_peaks(), heartRateData.getT_offsets());
                chartSetting.markRT(chart_df2, ecgMath.doubleArrayToListFloat(rawEcgSignal), r_indices, heartRateData.getT_onsets(), heartRateData.getT_peaks(), heartRateData.getT_offsets());
                setRegisterData();
            }
        });
    }

    /**
     * 設置註冊數據
     */
    public void setRegisterData() {
        if (!isFinishRegistered) {
            if (Math.abs(heartRateData.getDiffSelf()) < 1 && heartRateData.getBpm() < 125 && heartRateData.getRmssd() < 100) {
                addRegisterList();
                if (registerData.size() == 3) {
                    isFinishRegistered = true;
                    txt_checkID_status.setText("註冊完成");
                    txt_isMe.setText("量測成功!");
                }
                if (registerData.size() < 3) {
                    txt_checkID_status.setText("註冊還需: (" + (registerData.size()) + "/3)");
                    txt_isMe.setText("量測成功!");
                }
            } else {
                txt_isMe.setText("參數不在範圍內!");
                txt_checkID_status.setText("註冊還需: (" + registerData.size() + "/3)");
            }
        } else if (heartRateData.getBpm() < 125) {
            addRegisterList();
            if (registerData.size() >= 4) {
                euclideanDistance();
            }
        }
        showDetectOnUI();
    }

    /**
     * 從 JSON 中獲取數據列表，並加進註冊數據
     */
    public void addRegisterList() {
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("bpm", heartRateData.getBpm());
            jsonObject.put("mean_nn", heartRateData.getMean_nn());

            jsonObject.put("sdnn", heartRateData.getSdnn());
            jsonObject.put("sdsd", heartRateData.getSdsd());
            jsonObject.put("rmssd", heartRateData.getRmssd());

//            jsonObject.put("pnn20", heartRateData.getPnn20());
//            jsonObject.put("pnn50", heartRateData.getPnn50());
//            jsonObject.put("hr_mad", heartRateData.getHrMad());
            jsonObject.put("sd1", heartRateData.getSd1());
            jsonObject.put("sd2", heartRateData.getSd2());
            jsonObject.put("sd1/sd2", heartRateData.getSd1sd2());

            jsonObject.put("shan_en", heartRateData.getShan_en());
            jsonObject.put("af", heartRateData.getAf());

            jsonObject.put("t_area", heartRateData.getT_area());
            jsonObject.put("t_height", heartRateData.getT_height());

            jsonObject.put("pqr_angle", heartRateData.getPqr_angle());
            jsonObject.put("qrs_angle", heartRateData.getQrs_angle());
            jsonObject.put("rst_angle", heartRateData.getRst_angle());

//            jsonObject.put("diffSelf", heartRateData.getDiffSelf());
            jsonObject.put("r_med", heartRateData.getR_Med());
            jsonObject.put("voltStd", heartRateData.getVoltStd());
            jsonObject.put("halfWidth", heartRateData.getHalfWidth());

            // 添加到 registerData
            registerData.add(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (registerData.size() > 4) {
            registerData.remove(3);
        }
        tinyDB.putListString("registerData", registerData);
    }

    public void showDetectOnUI() {
        runOnUiThread(() -> {
            try {
                String s = "";
                if (heartRateData.getBpm() < 125) {
                    s = "當下量測\n" +
                            "BPM: " + String.format("%.2f", heartRateData.getBpm()) + "/" +
                            "MEANNN: " + String.format("%.2f", heartRateData.getMean_nn()) + "\n" +
                            "SDNN: " + String.format("%.2f", heartRateData.getSdnn()) + "/" +
                            "SDSD: " + String.format("%.2f", heartRateData.getSdsd()) + "\n" +
                            "RMSSD: " + String.format("%.2f", heartRateData.getRmssd()) + "/" +
                            "PNN20: " + String.format("%.2f", heartRateData.getPnn20()) + "\n" +
                            "PNN50: " + String.format("%.2f", heartRateData.getPnn50()) + "/" +
                            "HR_MAD: " + String.format("%.2f", heartRateData.getHrMad()) + "\n" +
                            "SD1: " + String.format("%.2f", heartRateData.getSd1()) + "/" +
                            "SD2: " + String.format("%.2f", heartRateData.getSd2()) + "\n" +
                            "SD1/SD2: " + String.format("%.2f", heartRateData.getSd1sd2()) + "/" +
                            "SHAN_EN: " + String.format("%.2f", heartRateData.getShan_en()) + "\n" +
                            "AF: " + String.format("%.2f", heartRateData.getAf()) + "/" +
//                            "T_Area: " + String.format("%.2f", heartRateData.getT_area()) + "\n" +
//                            "T_Height: " + String.format("%.2f", heartRateData.getT_height()) + "/" +
                            "PQR_Angle: " + String.format("%.2f", heartRateData.getPqr_angle()) + "\n" +
                            "QRS_Angle: " + String.format("%.2f", heartRateData.getQrs_angle()) + "/" +
                            "RST_Angle: " + String.format("%.2f", heartRateData.getRst_angle()) + "\n" +

                            "DiffSelf: " + String.format("%.2f", heartRateData.getDiffSelf()) + "\n" +
                            "R_Med: " + String.format("%.2f", heartRateData.getR_Med()) + "/" +
                            "VoltStd: " + String.format("%.2f", heartRateData.getVoltStd()) + "\n" +
                            "HalfWidth: " + String.format("%.2f", heartRateData.getHalfWidth());
                } else {
                    s = "參數計算異常";
                    txt_isMe.setText("");
                }
                txt_detect_result.setText(s);
                txt_detect_result.append("\n計算時間" + nk_process_time + "秒");
            } catch (Exception e) {
                Log.e("showDetectOnUI", "showDetectOnUI: " + e);
            }
        });
    }

    /**
     * 歐式距離
     */
    public void euclideanDistance() {
        List<Map<String, Double>> dataLists = getDataListsFromJson();

        if (dataLists.size() < 4) {
            Log.e("DataError", "註冊數據不足");
            return;
        }

        Map<String, Double> registerVector1 = dataLists.get(0);
        Map<String, Double> registerVector2 = dataLists.get(1);
        Map<String, Double> registerVector3 = dataLists.get(2);
        Map<String, Double> loginVector = dataLists.get(3);

        double distance = calculateAverageDistance(registerVector1, registerVector2, registerVector3, loginVector);
        double threshold = calculateDistanceThreshold(registerVector1, registerVector2, registerVector3);

        displayResults(distance, threshold, loginVector, registerVector1, registerVector2, registerVector3);
        saveResultsToFile(registerVector1, registerVector2, registerVector3, loginVector, distance, threshold);
        updateRegisterData(dataLists);
    }

    private List<Map<String, Double>> getDataListsFromJson() {
        ArrayList<String> registerData = tinyDB.getListString("registerData");
        List<Map<String, Double>> dataLists = new ArrayList<>();

        for (String jsonData : registerData) {
            JsonObject jsonObject = gson.fromJson(jsonData, JsonObject.class);
            Map<String, Double> dataMap = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> entry : jsonObject.entrySet()) {
                JsonElement element = entry.getValue();
                if (element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber()) {
                    dataMap.put(entry.getKey(), element.getAsDouble());
                }
            }
            dataLists.add(dataMap);
        }
        return dataLists;
    }

    private double calculateAverageDistance(Map<String, Double> vector1, Map<String, Double> vector2, Map<String, Double> vector3, Map<String, Double> loginVector) {
        double distance1 = calculateWeightedEuclideanDistance(vector1, loginVector);
        double distance2 = calculateWeightedEuclideanDistance(vector2, loginVector);
        double distance3 = calculateWeightedEuclideanDistance(vector3, loginVector);
        return (distance1 + distance2 + distance3) / 3;
    }

    private double calculateDistanceThreshold(Map<String, Double> vector1, Map<String, Double> vector2, Map<String, Double> vector3) {
        double distanceInside1 = calculateWeightedEuclideanDistance(vector1, vector2);
        double distanceInside2 = calculateWeightedEuclideanDistance(vector2, vector3);
        double distanceInside3 = calculateWeightedEuclideanDistance(vector1, vector3);

        double meanDistance = (distanceInside1 + distanceInside2 + distanceInside3) / 3;

        double distanceThreshold = Math.max(meanDistance, 110);// 設定閥值，小於110就設110

        Log.d("threshold_ori", "原來的閥值: " + meanDistance);
        return distanceThreshold;
    }

    @SafeVarargs
    private final void displayResults(double distance, double threshold, Map<String, Double> loginVector, Map<String, Double>... registerVectors) {
        runOnUiThread(() -> {
            txt_checkID_status.setText(String.format("%.2f|%.2f|%.2f = %.2f", calculateWeightedEuclideanDistance(registerVectors[0], loginVector), calculateWeightedEuclideanDistance(registerVectors[1], loginVector), calculateWeightedEuclideanDistance(registerVectors[2], loginVector), distance));
            txt_checkID_result.setText(String.format("界定值: %.5f", threshold));
            if (distance <= threshold) {
                txt_isMe.setText("本人");
            } else {
                txt_isMe.setText("非本人");
            }

            if (Math.abs(loginVector.get("r_med")) < 0.5) {
                txt_isMe.append("\n振幅過小!!!!");
            }
        });
    }

    /**
     * 將結果輸出成csv
     */
    private void saveResultsToFile(Map<String, Double> registerVector1, Map<String, Double> registerVector2, Map<String, Double> registerVector3, Map<String, Double> loginVector, double distance, double threshold) {
        List<Double> registerVector1List = getMapValues(registerVector1);
        List<Double> registerVector2List = getMapValues(registerVector2);
        List<Double> registerVector3List = getMapValues(registerVector3);
        List<Double> loginVectorList = getMapValues(loginVector);

        registerVector1List.add(distance);
        registerVector1List.add(threshold);

        registerVector2List.add(calculateWeightedEuclideanDistance(registerVector2, loginVector));
        registerVector3List.add(calculateWeightedEuclideanDistance(registerVector3, loginVector));
        loginVectorList.add(distance);

        fileMaker.writeVectorsToCSV(registerVector1List, registerVector2List, registerVector3List, loginVectorList);
    }

    private void updateRegisterData(List<Map<String, Double>> dataLists) {
        ArrayList<String> registerData = new ArrayList<>();
        for (Map<String, Double> dataMap : dataLists) {
            registerData.add(gson.toJson(dataMap));
        }
        keepRegisterListIsThree(registerData, 3);
        tinyDB.putListString("registerData", registerData);
    }

    private List<Double> getMapValues(Map<String, Double> map) {
        return new ArrayList<>(map.values());
    }

    public double calculateWeightedEuclideanDistance(Map<String, Double> vector1, Map<String, Double> vector2) {
        Map<String, Double> weights = getStringDoubleMap();

        double sum = 0.0;
        for (String key : vector1.keySet()) {
            Double v1 = vector1.get(key);
            Double v2 = vector2.get(key);
            if (v1 != null && v2 != null) {
                double weight = 0; // 默認權重為1
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    weight = weights.getOrDefault(key, 1.0);
                }
                sum += weight * Math.pow(v1 - v2, 2);
            }
        }

        // 增加 R_Med 差異過大的處罰
        double rMedDiff = Math.abs(vector1.get("r_med") - vector2.get("r_med"));
        if (rMedDiff > 0.55) { // 如果差異過大
            sum += 100000 * Math.pow(rMedDiff, 2); // 使用一個很大的權重來放大距離
        }

        return Math.sqrt(sum);
    }

    // 權重
    private static @NonNull Map<String, Double> getStringDoubleMap() {
        Map<String, Double> weights = new HashMap<>();
        // 這些參數不應該為1，給予較高的權重
        weights.put("sdnn", 3.0);
        weights.put("sdsd", 3.0);
        weights.put("rmssd", 3.0);
        weights.put("sd1", 3.0);
        weights.put("sd2", 3.0);
        weights.put("t_area", 3.0);
        weights.put("t_height", 3.0);
        weights.put("voltStd", 5.0);

        // 給R_Med 高權重，但高權重會顯著影響結果
        weights.put("r_med", 1000.0);
        return weights;
    }

    /**
     * 保持註冊數據只有3筆
     */
    public void keepRegisterListIsThree(List<?> list, int size) {
        while (list.size() > size) {
            list.remove(list.size() - 1);
        }
    }

    /**
     * 遍歷指定目錄下的所有文件，並對每個CHA文件執行processChaFile操作。
     */

    private Queue<File> fileQueue = new ArrayDeque<>();
    private Set<String> processedFiles = new HashSet<>();

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