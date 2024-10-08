package com.example.newidentify.bluetooth;

import static com.example.newidentify.MainActivity.DrawChart;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;


@SuppressLint("MissingPermission")
public class BT4 extends Service {

    private final Activity global_activity;

    public BT4(Activity activity) {
        this.global_activity = activity;
    }

    public final static String BLE_CONNECTED = "BLE_CONNECTED";
    public final static String BLE_TRY_CONNECT = "BLE_TRY_CONNECT";
    public final static String BLE_DISCONNECTED = "BLE_DISCONNECTED";
    public final static String BLE_READ_FILE = "BLE_READ_FILE";
    public final static String BLE_READ_BATTERY = "BLE_READ_BATTERY";

    public static UUID CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
    public static String UUID_Notify = "49535343-1e4d-4bd9-ba61-23c647249616";
    public static String UUID_Write = "49535343-8841-43f4-a8d4-ecbe34729bb3";

    //判斷封包長度  7 or 515
    public int datalength = 7;
    public ArrayList<Byte> Buffer_Array = new ArrayList<Byte>();
    public boolean isWave = false;
    public boolean continueWave = false;
    public ArrayList<Byte> wave_array = new ArrayList<Byte>();

    //傳輸檔案使用
    int file_index = 0;
    public ArrayList<Byte> file_data = new ArrayList<Byte>();

    //判斷是否連線
    public boolean isConnected = false;
    public boolean alreadyscan = false;
    public boolean is5SecOver = false;

    //BLE
    BluetoothGattCharacteristic gattCharacteristic_char6;
    BluetoothGattCharacteristic gattCharacteristic_write;
    public BluetoothGatt mBluetoothGatt;
    BluetoothDevice newBluetoothDevice = null;
    BluetoothManager mBluetoothManager;
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBluetoothDevices = new ArrayList<BluetoothDevice>();
    public String deviceName = "";

    //詢問檔案筆數
    public int ECG_Count = 0;
    public int Battery_Percent = 0;
    public int File_Count = 0;

    public void Bluetooth_init() {
        int permission = ActivityCompat.checkSelfPermission(global_activity, Manifest.permission.ACCESS_FINE_LOCATION);

        if (permission != 0) {
            ActivityCompat.requestPermissions(global_activity, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, 350);
        } else {
            //搜尋
            if (!isConnected) {
                startScan();
                LocationManager locationManager = (LocationManager) global_activity.getSystemService(Context.LOCATION_SERVICE);
                if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    Intent viewIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    global_activity.startActivity(viewIntent);
                }
            }
        }
    }

    public void startScan() {
        mBluetoothManager = (BluetoothManager) global_activity.getSystemService(BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        alreadyscan = true;
        int postms = 0;
        if (!mBluetoothAdapter.isEnabled()) {
            mBluetoothAdapter.enable();
        } else {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
                }
            }, postms);
        }
    }

    BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            global_activity.runOnUiThread(new Runnable() { //使用runOnUiThread方法，其功能等同於WorkThread透過Handler將資訊傳到MainThread(UiThread)中，
                //詳細可進到runOnUiThread中觀察
                @Override
                public void run() {
                    if (!mBluetoothDevices.contains(device)) { //利用contains判斷是否有搜尋到重複的device
                        //如沒重複則添加到bluetoothdevices中
                        if (device.getName() != null) {
                            mBluetoothDevices.add(device);
                            if (device.getName().equals(deviceName)) {
                                String intentAction;
                                intentAction = BLE_TRY_CONNECT;
                                broadcastUpdate(intentAction);

                                newBluetoothDevice = device;
                                mBluetoothGatt = newBluetoothDevice.connectGatt(global_activity, false, gattCallback);
                            }
                        }
                    }
                }
            });
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context contextlocal, Intent intent) {
            String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        //Indicates the local Bluetooth adapter is off.

                        break;

                    case BluetoothAdapter.STATE_TURNING_ON:
                        //Indicates the local Bluetooth adapter is turning on. However local clients should wait for STATE_ON before attempting to use the adapter.

                        break;

                    case BluetoothAdapter.STATE_ON:
                        //Indicates the local Bluetooth adapter is on, and ready for use.
//                        mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
                        global_activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                                mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
                            }
                        });
//                        startScan();
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        //Indicates the local Bluetooth adapter is turning off. Local clients should immediately attempt graceful disconnection of any remote links.

                        break;
                }
            }
        }
    };

    BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            Log.i("xxxxx", "StatusAAA: " + status);

            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    mBluetoothGatt.discoverServices();
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:

                    mBluetoothAdapter.startLeScan(mLeScanCallback);//開始搜尋BLE設備
                    mBluetoothDevices.clear();
                    isConnected = false;
                    mBluetoothGatt.close();
                    close();
                    break;
                default:

            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
            for (BluetoothGattService gattService : gattServices) {
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    if (gattCharacteristic.getUuid().toString().equals(UUID_Notify)) {
                        // 把char1 保存起来以方便后面读写数据时使用
                        gattCharacteristic_char6 = gattCharacteristic;

                    }
                    if (gattCharacteristic.getUuid().toString().equals(UUID_Write)) {

                        // 把char1 保存起来以方便后面读写数据时使用
                        gattCharacteristic_write = gattCharacteristic;
                    }
                }
            }//for

            mBluetoothGatt.setCharacteristicNotification(gattCharacteristic_char6, true);
            BluetoothGattDescriptor descriptor = gattCharacteristic_char6.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_DESCRIPTOR_UUID);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            isConnected = true;
            String intentAction;
            intentAction = BLE_CONNECTED;
            broadcastUpdate(intentAction);
            global_activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    txt_BleStatus.setText("已連線");

                }
            });

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            gatt.disconnect();
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            byte[] buffer = characteristic.getValue();
            //收到資料
            for (int i = 0; i < buffer.length; i++) {
                Buffer_Array.add(buffer[i]);
            }
        }
    };

    public int byteArrayToInt(byte[] b) {
        if (b.length == 4)
            return b[0] << 24 | (b[1] & 0xff) << 16 | (b[2] & 0xff) << 8 | (b[3] & 0xff);
        else if (b.length == 2) return 0x00 << 24 | 0x00 << 16 | (b[0] & 0xff) << 8 | (b[1] & 0xff);

        else if (b.length == 1) return 0x00 << 24 | 0x00 << 16 | (0x00 & 0xff) << 8 | (b[0] & 0xff);

        return 0;
    }

    TimerTask task = null;
    Timer timer = null;
    int timeout = 50;
    int wait = 20;
    int retry = 3;
    int counter = 0;
    int size = 0;

    public void writeBLE(byte[] cmd, final Handler back_handler) {
        if (task != null) {
            task.cancel();
            timer.cancel();
        }
        //清除
        Buffer_Array = new ArrayList<Byte>();
        byte[] rawArray = cmd;
        byte cs = 0;
        for (int i = 0; i < cmd.length - 1; i++) {
            cs = (byte) (cmd[i] + cs);
        }
        rawArray[rawArray.length - 1] = cs;
        String result = "";
        for (int i = 0; i < rawArray.length; i++)
            result += "  " + Integer.toString((rawArray[i] & 0xff) + 0x100, 16).substring(1);
//        Log.d(bluetooth_Tag, rawArray.length + "  發指令 = " + result + "");
        gattCharacteristic_write.setValue(cmd);
        mBluetoothGatt.writeCharacteristic(gattCharacteristic_write);
        if (isWave) {
            back_handler.sendMessage(new Message());
            return;
        }
        timeout = 50;
        wait = 20;
        retry = 3;
        counter = 0;
        size = datalength;
        task = new TimerTask() {
            @Override
            public void run() {
                counter += 1;
                if (Buffer_Array.size() == size) {
                    byte cs = 0;
                    for (int i = 0; i < Buffer_Array.size() - 1; i++) {
                        cs = (byte) (Buffer_Array.get(i) + cs);
                    }
                    if ((cs) == Buffer_Array.get(Buffer_Array.size() - 1)) {
                        task.cancel();
                        timer.cancel();
                        Message msg = new Message();
                        msg.arg1 = 1;
                        datalength = 7;
                        back_handler.sendMessage(msg);
                    } else {
                        retry -= 1;
                        if (retry > 0) {

                            wait = 20;
                            Buffer_Array = new ArrayList<Byte>();
                            mBluetoothGatt.writeCharacteristic(gattCharacteristic_write);
                        } else {
                            task.cancel();
                            timer.cancel();
                            close();
                        }
                    }
                }
                if (counter % timeout == 0) {
                    wait -= 1;
                    if (wait > 0) {
                        return;
                    }
                    retry -= 1;
                    if (retry > 0) {
                        wait = 20;
                        Buffer_Array = new ArrayList<Byte>();
                        mBluetoothGatt.writeCharacteristic(gattCharacteristic_write);
                    } else {
                        task.cancel();
                        timer.cancel();
                        close();
                    }
                }
            }
        };//Timer
        timer = new Timer();
        timer.schedule(task, 0, 1);
    }//send command

    @SuppressLint("HandlerLeak")
    public void Set_PassKey(byte[] cmd, final Handler read_handler) {
        datalength = 67;

        byte[] send = {(byte) 0xaa, 0x05, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
//        new byte[]
        System.arraycopy(cmd, 0, send, 2, cmd.length);
        writeBLE(send, new Handler() {
            @Override
            public void handleMessage(Message msg2) {
                Message back = new Message();
                back.arg1 = 1;
                read_handler.sendMessage(back);
            }
        });
    }

    @SuppressLint("HandlerLeak")
    public void Record_Size(final Handler read_handler) {
        try {
            writeBLE(new byte[]{(byte) 0xaa, 0x20, 0, 0, 0, 0, 0x00}, new Handler() {
                @Override
                public void handleMessage(Message msg2) {
                    ECG_Count = byteArrayToInt(new byte[]{Buffer_Array.get(2)});
                    read_handler.sendMessage(new Message());
                }
            });
        } catch (Exception e) {
        }
    }


    public byte[] send_BTCommand(byte[] cmd) {
        //清除
        //Buffer_Array = new ArrayList<Byte>();
        byte[] rawArray = cmd;
        byte cs = 0;
        for (int i = 0; i < cmd.length - 1; i++) {
            cs = (byte) (cmd[i] + cs);
        }
        rawArray[rawArray.length - 1] = cs;
        String result = "";
        for (int i = 0; i < rawArray.length; i++)
            result += "  " + Integer.toString((rawArray[i] & 0xff) + 0x100, 16).substring(1);
//        Log.d(bluetooth_Tag, rawArray.length + "  發指令 = " + result + "");
        gattCharacteristic_write.setValue(cmd);
        mBluetoothGatt.writeCharacteristic(gattCharacteristic_write);
        //int wait = 350;
        while (true) {
            SystemClock.sleep(100);
            break;
        }
        byte[] ret = new byte[]{0x00};
        while (true) {
            SystemClock.sleep(300);
            break;
        }
        if (Buffer_Array.size() == datalength) {
            ret = new byte[Buffer_Array.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = Buffer_Array.get(i);
            }
        }
        return ret;
    }//send command

    @SuppressLint("HandlerLeak")
    public void ReadBattery(Handler read_handler) {
        writeBLE(new byte[]{(byte) 0xaa, 0x03, 0, 0, 0, 0, 0x00}, new Handler() {
            @Override
            public void handleMessage(Message msg2) {
                int value = byteArrayToInt(new byte[]{Buffer_Array.get(3), Buffer_Array.get(2)});
//                Log.d(bluetooth_Tag, "batteryPower = " + value);
                if (value >= 1990) {
//                    Log.d(bluetooth_Tag, "battery100");
                    Battery_Percent = 100;
                } else if (value >= 1955 && value < 1990) {
//                    Log.d(bluetooth_Tag, "battery75");
                    Battery_Percent = 75;
                } else if (value >= 1920 && value < 1955) {
//                    Log.d(bluetooth_Tag, "battery50");
                    Battery_Percent = 50;
                } else if (value >= 1880 && value < 1920) {
//                    Log.d(bluetooth_Tag, "battery25");
                    Battery_Percent = 25;
                } else {
//                    Log.d(bluetooth_Tag, "battery0");
                    Battery_Percent = 0;
                }
                read_handler.sendMessage(new Message());
            }
        });
    }

    @SuppressLint("HandlerLeak")
    public void WhenConnectBLE(final Handler read_handler) {
        try {
            final int[] step = {0};
            Record_Size(new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    if (step[0] == 0) {
//                        Delete_AllRecor(this);
                    }
                    if (step[0] == 1) {
                        ReadBattery(this);
                    }
                    if (step[0] == 2) {
//                        read_handler.sendMessage(new Message());
                    }
                    step[0]++;
                }
            });
        } catch (Exception e) {
//            ShowToast("請先讓藍芽初始化");
        }

    }

    public void StopMeasure(final Handler read_handler) {
        try {
            isWave = false;
            send_BTCommand(new byte[]{(byte) 0xaa, 0x10, 0x01, 0x01, 0x00, 0x00, 0x00});
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    send_BTCommand(new byte[]{(byte) 0xaa, 0x10, 0x01, 0x01, 0x00, 0x00, 0x00});
                }
            }, 500);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    send_BTCommand(new byte[]{(byte) 0xaa, 0x10, 0x01, 0x01, 0x00, 0x00, 0x00});
                }
            }, 1000);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    send_BTCommand(new byte[]{(byte) 0xaa, 0x10, 0x01, 0x01, 0x00, 0x00, 0x00});
                }
            }, 1500);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    send_BTCommand(new byte[]{(byte) 0xaa, 0x10, 0x01, 0x01, 0x00, 0x00, 0x00});
                }
            }, 2000);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    send_BTCommand(new byte[]{(byte) 0xaa, 0x10, 0x01, 0x01, 0x00, 0x00, 0x00});
                }
            }, 2500);

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
//                    Log.d(bluetooth_Tag, "已成功送出7次停止");
                    read_handler.sendMessage(new Message());
                }
            }, 3000);
        } catch (Exception e) {
//            ShowToast("" + e);
        }
    }


    @SuppressLint("HandlerLeak")
    public void startWave(boolean isRecord, final Handler read_handler) {
        try {
            isWave = true;
            final String[] finalTimeNow = {""};
            byte[] outBuf = {-86, 0x10, 0x00, 0x02, 0, 0, -68};
            if (isRecord) {
                outBuf = new byte[]{-86, 0x10, 0x02, 0x02, 0, 0, -68};
            }
            writeBLE(outBuf, new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            wave_array = new ArrayList<Byte>();
                            final int[] wavc30 = {6};
                            int stopc = 1;
                            int stoph = 1;

                            while (isWave) {
                                try {
                                    //是否有遺漏封包，有遺漏就不畫
                                    while (true) {
                                        if (Buffer_Array.size() > 0) {
                                            if (Buffer_Array.get(0) == null) {
                                                Buffer_Array.remove(0);
                                            } else if (Buffer_Array.get(0) == -86) {
                                                break;
                                            } else {
                                                Buffer_Array.remove(0);
                                            }
                                        } else {
                                            break;
                                        }
                                    }
                                    if (Buffer_Array.size() >= 7) {
                                        boolean isnull = false;
                                        //是否有遺漏封包，有遺漏就不畫
                                        for (int i = 0; i < 7; i++) {
                                            if (Buffer_Array.get(i) == null) {
                                                isnull = true;
                                                break;
                                            }
                                        }
                                        if (isnull || !is5SecOver) {
                                            for (int i = 0; i < 7; i++) {
                                                Buffer_Array.remove(0);
                                            }
                                        } else {
//                                            Log.d("wwwww", "Not NULL  畫圖");
                                            String result = "";
                                            for (int i = 0; i < 7; i++) {
                                                result += "  " + Integer.toString((Buffer_Array.get(0) & 0xff) + 0x100, 16).substring(1);
                                                wave_array.add(Buffer_Array.get(0));
                                                Buffer_Array.remove(0);
                                            }

                                            if (wave_array.size() >= 7) {
//                                                Log.d(bluetooth_Tag, "wave_array = " + wave_array.size() + "     bytesAvailable = " + Buffer_Array.size());
                                                byte[] ecgbyte = new byte[7];

                                                ecgbyte[0] = wave_array.get(0);
                                                ecgbyte[1] = wave_array.get(1);
                                                ecgbyte[2] = wave_array.get(2);
                                                ecgbyte[3] = wave_array.get(3);
                                                ecgbyte[4] = wave_array.get(4);
                                                ecgbyte[5] = wave_array.get(5);
                                                ecgbyte[6] = wave_array.get(6);

                                                String valid = Integer.toString((wave_array.get(0) & 0xff) + 0x100, 16).substring(1)
                                                             + Integer.toString((wave_array.get(1) & 0xff) + 0x100, 16).substring(1)
                                                             + Integer.toString((wave_array.get(2) & 0xff) + 0x100, 16).substring(1)
                                                             + Integer.toString((wave_array.get(6) & 0xff) + 0x100, 16).substring(1);

                                                wave_array.remove(0);
                                                wave_array.remove(0);
                                                wave_array.remove(0);
                                                wave_array.remove(0);
                                                wave_array.remove(0);
                                                wave_array.remove(0);
                                                wave_array.remove(0);

                                                if (valid.equals("aaa00800") || valid.equals("aaa00801") && is5SecOver) {
                                                    DrawChart(ecgbyte);
                                                }
                                            }
                                        }
                                    }
                                }//try
                                catch (Exception e) {
                                    e.printStackTrace();
                                }
                                SystemClock.sleep(10);
                            }//while
                        }
                    }).start();
                }
            });
        } catch (Exception e) {
//            ShowToast("請先連接裝置");
        }
    }

    @SuppressLint("HandlerLeak")
    public void ReadData(final Handler read_handler) {
        int x1 = file_index % 256;
        int x2 = file_index / 256;
        datalength = 515;
        writeBLE(new byte[]{(byte) 0xaa, 0x25, 0x00, (byte) x1, (byte) x2, 0, 0}, new Handler() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void handleMessage(Message msg) {
                file_index = file_index + 2;

                for (int i = 2; i < 514; i++) {
                    if (file_data.size() < File_Count) {
                        file_data.add(Buffer_Array.get(i));
                    }
                }
                global_activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String intentAction;
                        intentAction = BLE_READ_FILE;
                        broadcastUpdate(intentAction);
                    }
                });

                if (file_data.size() == File_Count) {
                    read_handler.sendMessage(new Message());
                } else {
                    ReadData(read_handler);
                }
            }
        });
    }

    @SuppressLint("HandlerLeak")
    public void Open_File(final Handler read_handler) {
        writeBLE(new byte[]{(byte) 0xaa, 0x24, 0, 0, 0, 0, 0x00}, new Handler() {
            @Override
            public void handleMessage(Message msg2) {
                file_data = new ArrayList<>();
                file_index = 0;

                read_handler.sendMessage(new Message());
            }
        });
    }

    @SuppressLint("HandlerLeak")
    public void File_Size(final Handler read_handler) {
        writeBLE(new byte[]{(byte) 0xaa, 0x21, 0, 0, 0, 0, 0x00}, new Handler() {
            @Override
            public void handleMessage(Message msg2) {
                File_Count = byteArrayToInt(new byte[]{Buffer_Array.get(5), Buffer_Array.get(4), Buffer_Array.get(3), Buffer_Array.get(2)});
//                Log.d(bluetooth_Tag, "檔案大小 = " + File_Count);
                read_handler.sendMessage(new Message());
            }
        });
    }

    @SuppressLint("HandlerLeak")
    public void Delete_AllRecor(final Handler read_handler) {
        ECG_Count = ECG_Count - 1;
        if (ECG_Count < 0) {
            read_handler.sendMessage(new Message());
            return;
        }
        writeBLE(new byte[]{(byte) 0xaa, 0x26, (byte) ECG_Count, 0, 0, 0, 0}, new Handler() {
            @Override
            public void handleMessage(Message msg2) {
                if (ECG_Count == 0) {
                    read_handler.sendMessage(new Message());
                    return;
                }
                if (ECG_Count > 0) {
                    Delete_AllRecor(read_handler);
                }
            }
        });
    }

    public void close() {
//        Log.d(bluetooth_Tag, "CloseCloseCloseCloseAAA");
        alreadyscan = false;
        isWave = false;
        if (mBluetoothAdapter != null) {
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        mBluetoothGatt.disconnect();
        isConnected = false;

        global_activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String intentAction;
                intentAction = BLE_DISCONNECTED;
                broadcastUpdate(intentAction);
//                txt_BleStatus.setText("已斷線");
            }
        });
    }

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        global_activity.sendBroadcast(intent);
    }


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}

