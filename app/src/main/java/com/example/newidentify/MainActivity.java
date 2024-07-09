package com.example.newidentify;

import static com.example.newidentify.util.ChartSetting.Butterworth;
import static com.example.newidentify.util.ChartSetting.getStreamLP;
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
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RadioButton;
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
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.gson.Gson;
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
    private Button btn_clean;
    private Button btn_stop;
    private TextView txt_isMe;
    private TextView txt_result;
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

    static {
        System.loadLibrary("newidentify");
        System.loadLibrary("lp4tocha");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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
                    if (file.getName().endsWith(".lp4") && file.getName().startsWith("l_")) {
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
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    private void initBroadcast() {
        //註冊廣播過濾
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
        btn_clean = findViewById(R.id.btn_clean);
        btn_detect = findViewById(R.id.btn_detect);
        btn_stop = findViewById(R.id.btn_stop);
        txt_isMe = findViewById(R.id.txt_isMe);
        txt_result = findViewById(R.id.txt_result);
        txt_countDown = findViewById(R.id.txt_countDown);
        txt_BleStatus = findViewById(R.id.txt_BleStatus);
        txt_BleStatus_battery = findViewById(R.id.txt_BleStatus_battery);
        txt_checkID_status = findViewById(R.id.txt_checkID_status);
        txt_checkID_result = findViewById(R.id.txt_checkID_result);

        initBtn();

//        initializeFileQueue(Environment.getExternalStorageDirectory().getAbsolutePath() + "/testIdentifyApp" + "/register_Andy");
//        initializeFileQueue(Environment.getExternalStorageDirectory().getAbsolutePath() + "/testIdentifyApp" + "/login_Andy");
        initializeFileQueue(Environment.getExternalStorageDirectory().getAbsolutePath() + "/testIdentifyApp" + "/login_Aaron");
//        initializeFileQueue(Environment.getExternalStorageDirectory().getAbsolutePath() + "/testIdentifyApp" + "/login_peggy");
    }

    public void initBtn() {
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopALL();
                processNextFile();
            }
        });

        btn_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    tinyDB.clear();
                    ShowToast("已清除註冊檔案");
                    txt_checkID_status.setText("尚未有註冊資料");
                    txt_isMe.setText("");
                    txt_checkID_result.setText("");
                    cleanRegistrationData();
                    isFinishRegistered = false;
                });

            }
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
                Handler batteryHandler = new Handler(Looper.getMainLooper());
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
                    bt4.deviceName = "";
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
    }

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
                txt_isMe.setText("");
                txt_checkID_status.setText("");
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

        txt_result.setText("量測結果");
        countDownHandler.postDelayed(new Runnable() {
            private int presetTime = 5000;
            private int remainingTime = COUNTDOWN_TOTAL_TIME;
            boolean isToastShown = false;

            @Override
            public void run() {
                if (!isToastShown) {
                    Toast.makeText(global_activity, "請將手指放置於裝置上，量測馬上開始", Toast.LENGTH_LONG).show();
                    isToastShown = true;
                }
                if (presetTime <= 0) {//倒數6秒結束才開始跑波
                    bt4.isSixSecOver = true;
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
                    bt4.isSixSecOver = false;
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
                        runOnUiThread(() -> ShowToast("檔案大小為0"));
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

    private void processLP4(ArrayList<Byte> file_data) {
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

            // 通知系統掃描新生成的文件
            runOnUiThread(() -> {
                MediaScannerConnection.scanFile(this, new String[]{tempFile.getAbsolutePath()},
                        null, null);
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        readLP4(tempFilePath);
    }

    private void readLP4(String tempFilePath) {
        File file = new File(tempFilePath);
        Log.d("path", "readLP4: " + tempFilePath);
        if (!file.exists()) {
            Log.e("LP4FileNotFound", "LP4檔案不存在：" + tempFilePath);
            return;
        }
        // decpEcgFil將LP4檔案解碼為CHA格式的方法
        decpEcgFile(tempFilePath);
        readCHA(tempFilePath.replace(".lp4", ".cha"));
    }

    public void readCHA(String chaFilePath) {
        File chaFile = new File(chaFilePath);
        if (!chaFile.exists()) {
            Log.e("CHAFileNotFound", "CHA檔案不存在：" + chaFilePath);
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                DecodeCha decodeCha = new DecodeCha(chaFilePath);
                decodeCha.run();
                rawEcgSignal = decodeCha.ecgSignal;

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        calculateWithPython(rawEcgSignal);
                    }
                }).start();
            }
        }).start();
    }


    public void calculateWithPython(double[] ecg_signal) {
        if (ecg_signal == null || ecg_signal.length == 0 || ecg_signal.equals("NaN")) {
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
                    for (int i = 0; i < ecg_signal.length; i++) {
                        ecg_signal[i] *= 1;
                    }
                    PyObject hrv_analysis = pyObj.callAttr("hrv_analysis", ecg_signal, 1000.0);
                    getHRVFeature(hrv_analysis);
                } catch (Exception e) {
                    Log.e("PythonError", "Exception type: " + e.getClass().getSimpleName());
                    Log.e("PythonError", "Exception message: " + e.getMessage());
                    Log.e("PythonError", "Stack trace: ", e);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            txt_result.setText("量測失敗");
                        }
                    });
                }
            }
        }).start();
    }

    public void getHRVFeature(PyObject result) {
        PyObject hrv = result.asList().get(0);
        PyObject r_peaks = result.asList().get(1);
        PyObject r_value = result.asList().get(2);
        Log.d("getHRVData", "getHRVData: " + result);
        String hrvJsonString = hrv.toString().replaceAll("nan", "null").replaceAll("masked", "null");

        heartRateData = gson.fromJson(hrvJsonString, HeartRateData.class);

        if (heartRateData.getBpm() == 0.0) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    txt_result.setText("量測失敗");
                }
            });
            return;
        }

        Map<String, List<Integer>> rPeaksMap = gson.fromJson(r_peaks.toString(), new TypeToken<Map<String, List<Integer>>>() {
        }.getType());
        List<Integer> rPeaksList = rPeaksMap.get("r_peaks");

        Map<String, List<Double>> rValuesMap = gson.fromJson(r_value.toString(), new TypeToken<Map<String, List<Double>>>() {
        }.getType());
        List<Double> rValuesList = rValuesMap.get("r_values");

        assert rPeaksList != null;
        assert rValuesList != null;

        calculateValues(rPeaksList, rValuesList);
    }

    // 計算各項數據
    public void calculateValues(List<Integer> r_indices, List<Double> r_values) {
        Collections.sort(r_indices);
        // 檢查 R 波的數量是否足夠
        float diffSelf = calculateDiffSelf.calDiffSelf(ecgMath.doubleArrayToArrayListFloat(rawEcgSignal), r_indices);
        if (diffSelf == 9999f) {
            txt_result.setText("量測失敗");
            return;
        }
        heartRateData.setDiffSelf(diffSelf);
        heartRateData.setR_Med(ecgMath.calculateMedian(ecgMath.listDoubleToListFloat(r_values)));
        heartRateData.setHalfWidth(ecgMath.calculateHalfWidths(ecgMath.doubleArrayToArrayListFloat(rawEcgSignal), r_indices));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                chartSetting.markR(chart_df2, ecgMath.doubleArrayToArrayListFloat(rawEcgSignal), r_indices);
                setRegisterData();
            }
        });
    }

    public void setRegisterData() {
        Log.d("regi", "setRegisterData: " + Math.abs(heartRateData.getDiffSelf()));
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

    public void addRegisterList() {
        JSONObject jsonObject = new JSONObject();
        try {

            jsonObject.put("bpm", heartRateData.getBpm());
            jsonObject.put("mean_nn", heartRateData.getMean_nn());
            jsonObject.put("sdnn", heartRateData.getSdnn());
            jsonObject.put("sdsd", heartRateData.getSdsd());
            jsonObject.put("rmssd", heartRateData.getRmssd());
            jsonObject.put("pnn20", heartRateData.getPnn20());
            jsonObject.put("pnn50", heartRateData.getPnn50());
            jsonObject.put("hr_mad", heartRateData.getHrMad());
            jsonObject.put("sd1", heartRateData.getSd1());
            jsonObject.put("sd2", heartRateData.getSd2());
            jsonObject.put("sd1/sd2", heartRateData.getSd1sd2());
            jsonObject.put("iqrnn", heartRateData.getIqrnn());
            jsonObject.put("ap_en", heartRateData.getAp_en());
            jsonObject.put("shan_en", heartRateData.getShan_en());
            jsonObject.put("fuzzy_en", heartRateData.getFuzzy_en());
            jsonObject.put("samp_en", heartRateData.getSamp_en());
            jsonObject.put("ulf", heartRateData.getUlf());
            jsonObject.put("vlf", heartRateData.getVlf());
            jsonObject.put("lf", heartRateData.getLf());
            jsonObject.put("hf", heartRateData.getHf());
            jsonObject.put("tp", heartRateData.getTp());
            jsonObject.put("lfhf", heartRateData.getLfhf());
            jsonObject.put("lfn", heartRateData.getLfn());
            jsonObject.put("hfn", heartRateData.getHfn());
            jsonObject.put("ln_hf", heartRateData.getLn_hf());
            jsonObject.put("sdann1", heartRateData.getSdann1());
            jsonObject.put("sdann2", heartRateData.getSdann2());
            jsonObject.put("sdann5", heartRateData.getSdann5());
            jsonObject.put("af", heartRateData.getAf());

            jsonObject.put("diffSelf", heartRateData.getDiffSelf());
            jsonObject.put("r_med", heartRateData.getR_Med());
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
                            "SD1/SD2: " + String.format("%.2f", heartRateData.getSd1sd2()) + "\n" +
                            "IQRNN: " + String.format("%.2f", heartRateData.getIqrnn()) + "\n" +
                            "AP_EN: " + String.format("%.2f", heartRateData.getAp_en()) + "\n" +
                            "SHAN_EN: " + String.format("%.2f", heartRateData.getShan_en()) + "\n" +
                            "FUZZY_EN: " + String.format("%.2f", heartRateData.getFuzzy_en()) + "\n" +
                            "SAMP_EN: " + String.format("%.2f", heartRateData.getSamp_en()) + "\n" +
                            "ULF: " + String.format("%.2f", heartRateData.getUlf()) + "\n" +
                            "VLF: " + String.format("%.2f", heartRateData.getVlf()) + "\n" +
                            "LF: " + String.format("%.2f", heartRateData.getLf()) + "\n" +
                            "HF: " + String.format("%.2f", heartRateData.getHf()) + "\n" +
                            "TP: " + String.format("%.2f", heartRateData.getTp()) + "\n" +
                            "LF/HF: " + String.format("%.2f", heartRateData.getLfhf()) + "\n" +
                            "LFN: " + String.format("%.2f", heartRateData.getLfn()) + "\n" +
                            "HFN: " + String.format("%.2f", heartRateData.getHfn()) + "\n" +
                            "LN_HF: " + String.format("%.2f", heartRateData.getLn_hf()) + "\n" +
                            "SDANN1: " + String.format("%.2f", heartRateData.getSdann1()) + "\n" +
                            "SDANN2: " + String.format("%.2f", heartRateData.getSdann2()) + "\n" +
                            "SDANN5: " + String.format("%.2f", heartRateData.getSdann5()) + "\n" +
                            "AF: " + String.format("%.2f", heartRateData.getAf());
                } else {
                    s = "參數計算異常";
                    txt_isMe.setText("");
                }
                txt_result.setText(s);
            } catch (Exception e) {
                Log.e("showDetectOnUI", "showDetectOnUI: " + e);
            }
        });
    }

    public void euclideanDistance() {
        ArrayList<String> registerData = tinyDB.getListString("registerData");
        List<Map<String, Double>> dataLists = new ArrayList<>();

        // 解析 JSON 數據
        for (String jsonData : registerData) {
            Map<String, Double> dataMap = gson.fromJson(jsonData, new TypeToken<Map<String, Double>>() {
            }.getType());
            dataLists.add(dataMap);
        }

        // 提取向量
        Map<String, Double> registerVector1 = dataLists.get(0);
        Map<String, Double> registerVector2 = dataLists.get(1);
        Map<String, Double> registerVector3 = dataLists.get(2);
        Map<String, Double> loginVector = dataLists.get(3);

        // 計算加權歐幾里得距離
        double distance1 = calculateWeightedEuclideanDistance(registerVector1, loginVector);
        double distance2 = calculateWeightedEuclideanDistance(registerVector2, loginVector);
        double distance3 = calculateWeightedEuclideanDistance(registerVector3, loginVector);
        double distance = (distance1 + distance2 + distance3) / 3;

        // 設定閾值並進行比較
        double distanceInside1 = calculateWeightedEuclideanDistance(registerVector1, registerVector2);
        double distanceInside2 = calculateWeightedEuclideanDistance(registerVector2, registerVector3);
        double distanceInside3 = calculateWeightedEuclideanDistance(registerVector1, registerVector3);

        double meanDistance = (distanceInside1 + distanceInside2 + distanceInside3) / 3;
        double distanceThreshold = meanDistance * 1;

        Log.d("threshold_ori", "原來的閥值: " + meanDistance);
        if (distanceThreshold < 110) {
            distanceThreshold = 110;
        }

        double threshold = distanceThreshold; // 假設的閾值，根據實際需要調整

        String s = "threshold: " + threshold + "\ndistance1: " + distance1 + "\ndistance2: " + distance2 + "\ndistance3: " + distance3;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txt_checkID_status.setText(String.format("%.2f|%.2f|%.2f = %.2f", distance1, distance2, distance3, distance));
                txt_checkID_result.setText(String.format("界定值: %.5f", threshold));
                if (distance <= threshold) {
                    txt_isMe.setText("本人");
                } else {
                    txt_isMe.setText("非本人");
                }

                if (Math.abs(loginVector.get("r_med")) < 0.5) {
                    txt_isMe.append("\n振幅過小!!!!");
                }
            }
        });

        //get Map value
        List<Double> registerVector1List = getMapValue(registerVector1);
        List<Double> registerVector2List = getMapValue(registerVector2);
        List<Double> registerVector3List = getMapValue(registerVector3);
        List<Double> loginVectorList = getMapValue(loginVector);
        registerVector1List.add(distance1);
        registerVector1List.add(threshold);

        registerVector2List.add(distance2);
        registerVector3List.add(distance3);

        loginVectorList.add(distance);
        fileMaker.writeVectorsToCSV(registerVector1List, registerVector2List, registerVector3List, loginVectorList);
        keepListIsThree(registerData, 3);
        tinyDB.putListString("registerData", registerData);
    }

    public double calculateWeightedEuclideanDistance(Map<String, Double> vector1, Map<String, Double> vector2) {
        Map<String, Double> weights = new HashMap<>();
        weights.put("mean_nn", 2.0);
        weights.put("hr_mad", 2.0);
        weights.put("sd2", 2.0);
        weights.put("diffSelf", 2.0);
        weights.put("r_med", 100.0);

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
        return Math.sqrt(sum);
    }


    public void keepListIsThree(List<?> list, int size) {
        while (list.size() > size) {
            Log.d("keep", "keepListIsThree: " + list.size());
            list.remove(list.size() - 1);
        }
    }

    public List<Double> getMapValue(Map<String, Double> map) {
        List<Double> valueList = new ArrayList<>();
        for (Map.Entry<String, Double> entry : map.entrySet()) {
            String key = entry.getKey();
            Double value = entry.getValue();
            valueList.add(value);
        }
        return valueList;
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
        readCHA(file.getAbsolutePath());
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
                // 對每個CHA檔案執行readCHA操作
                readCHA(file.getAbsolutePath());
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