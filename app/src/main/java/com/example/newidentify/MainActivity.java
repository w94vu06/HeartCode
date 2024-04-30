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
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
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
import com.example.newidentify.util.ChartSetting;
import com.example.newidentify.util.CleanFile;
import com.example.newidentify.util.EcgMath;
import com.example.newidentify.util.FindPeaksCallback;
import com.example.newidentify.util.TinyDB;
import com.example.newidentify.processData.DecodeCha;
import com.example.newidentify.processData.FindPeaks;
import com.example.newidentify.util.FileMaker;
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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;


public class MainActivity extends AppCompatActivity implements FindPeaksCallback {
    private SharedPreferences preferences;
    private SharedPreferences.Editor editor;
    private HeartRateData heartRateData;
    private Gson gson = new Gson();

    // UI
    private Button btn_detect, btn_clean, btn_stop;
    private TextView txt_result;
    private TextView txt_checkID_status, txt_checkID_result;

    // choose Device Dialog
    private Dialog deviceDialog;

    // Parameter
    public String fileName = "";
    public boolean isFinishRegistered = false; // 是否已註冊

    // Used to load the 'newidentify' library on application startup
    static {
        System.loadLibrary("newidentify");
        System.loadLibrary("lp4tocha");
    }

    // BLE
    public static Activity global_activity;
    public static TextView txt_countDown;
    static BT4 bt4;
    private TinyDB tinyDB;
    public static TextView txt_BleStatus;
    public static TextView txt_BleStatus_battery;


    // 畫心電圖使用
    private Handler measurementHandler = new Handler(Looper.getMainLooper());
    private final Handler countDownHandler = new Handler();
    public CountDownTimer countDownTimer; //倒數
    boolean isCountDownRunning = false;
    boolean isMeasurementOver = false;
    private static final int COUNTDOWN_INTERVAL = 1000;
    private static final int COUNTDOWN_TOTAL_TIME = 30000;

    public static LineChart lineChart;
    public static LineChart chart_df;
    public static LineChart chart_df2;
    private ChartSetting chartSetting;

    public static ArrayList<Entry> chartSet1Entries = new ArrayList<Entry>();
    public static ArrayList<Double> oldValue = new ArrayList<Double>();
    public static LineDataSet chartSet1 = new LineDataSet(null, "");


    // L2D
    private SignalProcess signalProcess;
    private FindPeaks findPeaks;
    private EcgMath ecgMath = new EcgMath();
    public CleanFile cleanFile;
    private FileMaker fileMaker = new FileMaker(this);
    public ArrayList<Float> rawEcgSignal = new ArrayList<>();

    private String hrvString;
    private ArrayList<String> registerData = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //初始化SharedPreference
        preferences = getSharedPreferences("my_preferences", MODE_PRIVATE);
        editor = preferences.edit();

        global_activity = this;

        //初始化物件
        bt4 = new BT4(global_activity);
        tinyDB = new TinyDB(global_activity);
        deviceDialog = new Dialog(global_activity);
        signalProcess = new SignalProcess();
        cleanFile = new CleanFile();
        chartSetting = new ChartSetting();
        findPeaks = new FindPeaks();

        lineChart = findViewById(R.id.linechart);
        chart_df = findViewById(R.id.chart_df);
        chart_df2 = findViewById(R.id.chart_df2);

        initchart();//初始化圖表
        initObject();//初始化物件
        initDeviceDialog();//裝置選擇Dialog
        checkAndDisplayRegistrationStatus();//檢查註冊狀態
        Log.d("ssss", "isFinishRegistered: " + isFinishRegistered);

        //初始化python環境
        if (!Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }
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
        Log.d("dddd", "onDestroy: " + externalFilesDir);
//        if (externalFilesDir != null && externalFilesDir.isDirectory()) {
//            // 列出所有文件
//            File[] files = externalFilesDir.listFiles();
//            if (files != null) {
//                for (File file : files) {
//                    // 檢查檔案名稱是否符合特定的前綴和後綴
//                    if (file.getName().endsWith(".lp4") && file.getName().startsWith("l_") || file.getName().endsWith(".cha") && file.getName().startsWith("l_")) {
//                        // 刪除符合條件的文件
//                        boolean deleted = file.delete();
//                        if (!deleted) {
//                            Log.e("DeleteTempFiles", "Failed to delete file: " + file.getAbsolutePath());
//                        }
//                    }
//                }
//            }
//        }
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
        txt_result = findViewById(R.id.txt_result);
//        txt_average = findViewById(R.id.txt_average);
        txt_countDown = findViewById(R.id.txt_countDown);
        txt_BleStatus = findViewById(R.id.txt_BleStatus);
        txt_BleStatus_battery = findViewById(R.id.txt_BleStatus_battery);
        txt_checkID_status = findViewById(R.id.txt_checkID_status);
        txt_checkID_result = findViewById(R.id.txt_checkID_result);
        initBtn();
    }

    public void initBtn() {
        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                stopALL();
                processAllCHAFilesInDirectory(Environment.getExternalStorageDirectory().getAbsolutePath() + "/5cha");
            }
        });
        btn_clean.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runOnUiThread(() -> {
                    tinyDB.clear();
                    ShowToast("已清除註冊檔案");
                    txt_checkID_status.setText("尚未有註冊資料");
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
                    if (bt4.isConnected) {
                        bt4.ReadBattery(batteryHandler);
                        txt_BleStatus_battery.setText(bt4.Battery_Percent + "%");
                        Log.d("bbbb", "onClick: " + bt4.Battery_Percent);
                    }

                } else if (checkedRadioButtonId == R.id.radioButtonDevice2) {
                    bt4.deviceName = "WTK230";
                    bt4.Bluetooth_init();

                    deviceDialog.dismiss();
                    if (bt4.isConnected) {
                        bt4.ReadBattery(batteryHandler);
                        txt_BleStatus_battery.setText(bt4.Battery_Percent + "%");
                        Log.d("bbbb", "onClick: " + bt4.Battery_Percent);
                    }
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
        if (requestCode == 350) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("wwwww", "搜尋設備");
            }
        }
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

    /**
     * 量測與畫圖
     */
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
            private int presetTime = 6000;
            private int remainingTime = COUNTDOWN_TOTAL_TIME;
            boolean isToastShown = false;

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
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                processLP4(bt4.file_data);
                            }
                        }).start();
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

            runOnUiThread(() -> {
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
        new Thread(new Runnable() {
            @Override
            public void run() {
                fileName = chaFile.getName();//取得檔案名稱
                DecodeCha decodeCha = new DecodeCha(chaFilePath);
                decodeCha.run();
                rawEcgSignal = findPeaks.filedData(decodeCha.rawEcgSignal);
                calculateWithPython(ecgMath.arrayListFloatToDoubleArray(rawEcgSignal));
            }
        }).start();
    }

    public void calculateWithPython(double[] ecg_signal) {
        if (ecg_signal == null || ecg_signal.length == 0 || ecg_signal.equals("NaN")) {
            Log.e("EmptyDataList", "dataList有誤");
            return;
        }
        Log.d("python", "ecg_signal.length: " + ecg_signal.length);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 獲取 Python 實例和模塊
                Python py = Python.getInstance();
                PyObject pyObj = py.getModule("hrv_analysis");

                // 調用 Python 函數並獲取結果
                PyObject result = pyObj.callAttr("hrv_analysis", ecg_signal, 1000.0);
                getHRVData(result);
                try {

                } catch (Exception e) {
                    Log.e("PythonError", e.toString());
                    txt_result.setText("量測失敗");
                }
            }
        }).start();
    }

    public void getHRVData(PyObject result) {
        PyObject hrv = result.asList().get(0);
        PyObject r_peaks = result.asList().get(1);
        PyObject r_value = result.asList().get(2);

        hrvString = hrv.toString();//將hrv轉為字串

        Log.d("python", "hrv: " + hrv);
        Log.d("python", "r_peaks: " + r_peaks.toString());
        Log.d("python", "r_value: " + r_value.toString());

        String hrvJsonString = hrv.toString().replaceAll("nan", "null");

        heartRateData = gson.fromJson(hrvJsonString, HeartRateData.class);

        Map<String, List<Integer>> rPeaksMap = gson.fromJson(r_peaks.toString(), new TypeToken<Map<String, List<Integer>>>() {
        }.getType());
        List<Integer> rPeaksList = rPeaksMap.get("r_peaks");

        Map<String, List<Double>> rValuesMap = gson.fromJson(r_value.toString(), new TypeToken<Map<String, List<Double>>>() {
        }.getType());
        List<Double> rValuesList = rValuesMap.get("r_values");

        rPeaksList = filterRPeaks(rPeaksList, 250);
        rValuesList = findRPeaks(rawEcgSignal, rPeaksList);

        assert rPeaksList != null;
        assert rValuesList != null;

        calculateValues(rPeaksList, rValuesList);
    }

    public List<Integer> filterRPeaks(List<Integer> rPeaksList, int minDistance) {
        List<Integer> filteredRPeaks = new ArrayList<>();
        int previousRPeak = -1;

        for (Integer rPeak : rPeaksList) {
            if (previousRPeak == -1 || rPeak - previousRPeak >= minDistance) {
                filteredRPeaks.add(rPeak);
                previousRPeak = rPeak;
            }
        }

        return filteredRPeaks;
    }

    //根據R波的位置找到R波的值
    public List<Double> findRPeaks(ArrayList<Float> rawEcgSignal, List<Integer> rPeaksList) {
        List<Double> rValuesList = new ArrayList<>();
        for (int i = 0; i < rawEcgSignal.size(); i++) {
            if (rPeaksList.contains(i)) {
                rValuesList.add((double) rawEcgSignal.get(i));
            }
        }
        return rValuesList;
    }

    // 計算各項數據
    public void calculateValues(List<Integer> r_indices, List<Double> r_values) {
        Collections.sort(r_indices);
        heartRateData.setDiffSelf(signalProcess.calDiffSelf(rawEcgSignal, r_indices));
        heartRateData.setR_Med(ecgMath.calculateMedian(ecgMath.listDoubleToListFloat(r_values)));
        heartRateData.setHalfWidth(findPeaks.calculateHalfWidths(rawEcgSignal, r_indices));
        chartSetting.markRT(chart_df2, rawEcgSignal, r_indices);

        setRegisterData();
    }

    public void setRegisterData() {
        if (!isFinishRegistered) {
            if (Math.abs(heartRateData.getDiffSelf()) < 1) {
                addRegisterList();
                if (registerData.size() == 3) {
                    isFinishRegistered = true;
                    txt_checkID_status.setText("註冊完成");
                    txt_checkID_result.setText("量測成功!");
                }
                if (registerData.size() < 3) {
                    txt_checkID_status.setText("註冊還需: (" + (registerData.size()) + "/3)");
                    txt_checkID_result.setText("量測成功!");
                }
            } else {
                txt_checkID_result.setText("參數不在範圍內!");
                txt_checkID_status.setText("註冊還需: (" + registerData.size() + "/3)");
            }
        } else {
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
            jsonObject.put("ibi", heartRateData.getIbi());
            jsonObject.put("sdnn", heartRateData.getSdnn());
            jsonObject.put("sdsd", heartRateData.getSdsd());
            jsonObject.put("rmssd", heartRateData.getRmssd());
            jsonObject.put("pnn20", heartRateData.getPnn20());
            jsonObject.put("pnn50", heartRateData.getPnn50());
            jsonObject.put("hr_mad", heartRateData.getHrMad());
            jsonObject.put("sd1", heartRateData.getSd1());
            jsonObject.put("sd2", heartRateData.getSd2());
            jsonObject.put("sd1/sd2", heartRateData.getSd1sd2());
            jsonObject.put("breathingrate", heartRateData.getBreathingrate());
            jsonObject.put("DiffSelf", heartRateData.getDiffSelf());
            jsonObject.put("R_Med", heartRateData.getR_Med());
            jsonObject.put("HalfWidth", heartRateData.getHalfWidth());

            // 添加到 registerData
            registerData.add(jsonObject.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (registerData.size() > 4) {
            registerData.remove(3);
        }
        tinyDB.putListString("registerData", registerData);

        Log.d("HRVList", "registerData: " + registerData);
        Log.d("HRVList", "registerData.size: " + registerData.size());
    }

    public void showDetectOnUI() {

        runOnUiThread(() -> {
            txt_result.setText("當下量測\n" +
                    "BPM: " + String.format("%.2f", heartRateData.getBpm()) + "/" +
                    "IBI: " + String.format("%.2f", heartRateData.getIbi()) + "\n" +
                    "SDNN: " + String.format("%.2f", heartRateData.getSdnn()) + "/" +
                    "SDSD: " + String.format("%.2f", heartRateData.getSdsd()) + "\n" +
                    "RMSSD: " + String.format("%.2f", heartRateData.getRmssd()) + "/" +
                    "PNN20: " + String.format("%.2f", heartRateData.getPnn20()) + "\n" +
                    "PNN50: " + String.format("%.2f", heartRateData.getPnn50()) + "/" +
                    "HR_MAD: " + String.format("%.2f", heartRateData.getHrMad()) + "\n" +
                    "SD1: " + String.format("%.2f", heartRateData.getSd1()) + "/" +
                    "SD2: " + String.format("%.2f", heartRateData.getSd2()) + "\n" +
                    "SD1/SD2: " + String.format("%.2f", heartRateData.getSd1sd2()) + "/" +
                    "BreathingRate: " + String.format("%.2f", heartRateData.getBreathingrate()) + "\n" +
                    "DiffSelf: " + String.format("%.2f", heartRateData.getDiffSelf()) + "/" +
                    "R_Med: " + String.format("%.2f", heartRateData.getR_Med()) + "\n" +
                    "HalfWidth: " + String.format("%.2f", heartRateData.getHalfWidth())
            );
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

        // 計算歐式距離
        double distance1 = calculateEuclideanDistance(registerVector1, loginVector);
        double distance2 = calculateEuclideanDistance(registerVector2, loginVector);
        double distance3 = calculateEuclideanDistance(registerVector3, loginVector);

        // 設定閾值並進行比較
        double threshold = 200.0; // 假設的閾值，根據實際需要調整
        String s = "threshold: " + threshold + "\ndistance1: " + distance1 + "\ndistance2: " + distance2 + "\ndistance3: " + distance3;
        Log.d("distance", s);
        txt_checkID_status.setText(String.format("%.2f|%.2f|%.2f = %.2f", distance1, distance2, distance3, (distance1 + distance2 + distance3) / 3));
        if ((distance1 + distance2 + distance3) / 3 < threshold) {
            txt_checkID_result.setText("本人");
        } else {
            txt_checkID_result.setText("非本人");
        }

        //get Map value
        List<Double> registerVector1List = getMapValue(registerVector1);
        List<Double> registerVector2List = getMapValue(registerVector2);
        List<Double> registerVector3List = getMapValue(registerVector3);
        List<Double> loginVectorList = getMapValue(loginVector);
        registerVector1List.add(distance1);
        registerVector2List.add(distance2);
        registerVector3List.add(distance3);

        loginVectorList.add(threshold);
        fileMaker.writeVectorsToCSV(registerVector1List, registerVector2List, registerVector3List, loginVectorList);
        keepListIsThree(registerData, 3);
        tinyDB.putListString("registerData", registerData);
    }

    public double calculateEuclideanDistance(Map<String, Double> vector1, Map<String, Double> vector2) {
        double sum = 0.0;
        for (String key : vector1.keySet()) {
            Double v1 = vector1.get(key);
            Double v2 = vector2.get(key);
            if (v1 != null && v2 != null) {
                sum += Math.pow(v1 - v2, 2);
                Log.d("verity", key + ": v1 - v2: " + v1 + " - " + v2 + " = " + Math.pow(v1 - v2, 2) + " sum: " + sum);
            } else {
                Log.d("calculateEuclideanDistance", "calculateEuclideanDistance: " + key + " is null");
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

//                fileMaker.writeRecordToFile(recordList);
            }
        }).start();
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
    public void processAllCHAFilesInDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        // 確保該路徑是目錄
        if (directory.isDirectory()) {
            // 取得目錄下所有檔案（和目錄）
            File[] files = directory.listFiles();

            // 確保files不為null
            if (files != null) {
                for (File file : files) {
                    Log.d("AllCha", ": " + file);
                    // 確保是檔案而不是目錄，並且檔案名稱以.cha結尾
                    if (file.isFile() && file.getName().endsWith(".CHA")) {
                        // 取得檔案的絕對路徑和檔案名
                        String filePath = file.getParent();
                        String fileName = file.getName();

                        // 對每個CHA檔案執行readCHA操作
                        readCHA(file.getAbsolutePath());
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

    @Override
    public void onFindPeaksCallback(int[] peaks) {
    }

    @Override
    public void onFindPeaksErrorCallback(String message) {
        txt_result.setText(message);
    }

    public static void ShowToast(final String message) {
        global_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(global_activity, message, Toast.LENGTH_LONG).show();

            }
        });
    }//ShowToast


}