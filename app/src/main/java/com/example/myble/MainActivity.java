package com.example.myble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myble.databinding.ActivityMainBinding;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final int BLE_TYPE_TW = 0x11;
    private static final int BLE_TYPE_NJ = 0x12;

    public static final int REQUEST_ENABLE_BT = 0x1;
    private static final long SCAN_PERIOD = 30_000; //扫描时间

    private boolean isScanning = false;
    private final Handler handler = new Handler();

    private ActivityMainBinding viewBinding;
    private static int deviceType;
    private static String deviceAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        initPermissions();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_support:
                supporBLE();
                break;
            case R.id.btn_enable:
                checkEnabled();
                break;
            case R.id.btn_open_ble1:
                openBLE1();
                break;
            case R.id.btn_open_ble2:
                openBLE2();
                break;
            case R.id.btn_start_scan:
                startScan();
                break;
            case R.id.btn_stop_scan:
                stopScan();
                break;
        }
    }

    /**
     * 申请权限
     */
    private void initPermissions() {
        //BLUETOOTH_ADVERTISE、BLUETOOTH_CONNECT、BLUETOOTH_SCAN、ACCESS_FINE_LOCATION权限是运行时权限
        XXPermissions.with(this)
                .permission(Permission.BLUETOOTH_ADVERTISE, Permission.BLUETOOTH_CONNECT, Permission.BLUETOOTH_SCAN, Permission.ACCESS_FINE_LOCATION, Permission.ACCESS_FINE_LOCATION)
                .request(new OnPermissionCallback() {
                    @Override
                    public void onGranted(@NonNull List<String> permissions, boolean allGranted) {
                    }

                    @Override
                    public void onDenied(@NonNull List<String> permissions, boolean doNotAskAgain) {
                    }
                });
    }

    public void clickConnect(View view) {
        startDeviceActivity();
    }

    /**
     * 检查设备是否支持蓝牙
     */
    private boolean supporBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) || getBluetoothAdapter() == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            return false;
        } else {
            Toast.makeText(this, "当前设备支持蓝牙", Toast.LENGTH_SHORT).show();
            return true;
        }
    }

    /**
     * 获取蓝牙适配器
     */
    private BluetoothAdapter getBluetoothAdapter() {
//        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
//        return bluetoothAdapter;

        // 上面代码等价于如下
        return BluetoothAdapter.getDefaultAdapter();
    }

    /**
     * 检查蓝牙是否开启
     */
    private boolean checkEnabled() {
        boolean isEnabled = getBluetoothAdapter().isEnabled();
        Toast.makeText(this, isEnabled ? "已开启" : "未开启", Toast.LENGTH_SHORT).show();
        return isEnabled;
    }

    /**
     * 打开蓝牙，方式一
     */
    private void openBLE1() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        // 直接开启蓝牙，Android13不支持了
        boolean enable = bluetoothAdapter.enable();
        if (enable) {
            Toast.makeText(this, "开启蓝牙了", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 打开蓝牙，方式二
     */
    private void openBLE2() {
        Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(intent, REQUEST_ENABLE_BT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT && resultCode == RESULT_OK) {
            Toast.makeText(this, "开启蓝牙了", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 开始扫描蓝牙设备
     */
    private void startScan() {
        if (!isScanning) {
            Log.e("TAG", "开始扫描");
            isScanning = true;
            BluetoothLeScanner bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    isScanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);
            bluetoothLeScanner.startScan(leScanCallback);
        }
    }

    /**
     * 停止扫描蓝牙设备
     */
    public void stopScan() {
        if (isScanning) {
            Log.e("TAG", "停止扫描");
            isScanning = false;
            handler.removeCallbacksAndMessages(null);
            BluetoothLeScanner bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    /**
     * 扫描结果回调
     */
    private final ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            if (device != null) {
                String deviceInfoStr = "\n" +
                        "设备名：" + device.getName() + "\n" +
                        "地址：" + device.getAddress() + "\n" +
                        "uuids：" + Arrays.toString(device.getUuids());
                Log.e("TAG", "BLE : " + deviceInfoStr);
                if (filterDevice(device.getName())) {
                    Log.e("TAG", "找到指定设备，停止扫描");
                    stopScan();
                    deviceType = getDeviceType(device.getName());
                    deviceAddress = device.getAddress();
                    viewBinding.tvDevice.setText(deviceInfoStr);
                }
            }
        }
    };

    private void startDeviceActivity() {
        if (deviceType != -1) {
            DeviceControlActivity.actionStart(this, deviceAddress, deviceType);
        }
    }

    /**
     * 过滤指定设备
     */
    private boolean filterDevice(String deviceName) {
        return "BLE-EMP-Ui".equals(deviceName) || "Bluetooth BP".equals(deviceName);
    }

    /**
     * 获取设备类型
     */
    private int getDeviceType(String deviceName) {
        if ("BLE-EMP-Ui".equals(deviceName)) {
            viewBinding.tvTitle.setText("尿检仪");
            return BLE_TYPE_NJ;
        } else if ("Bluetooth BP".equals(deviceName)) {
            viewBinding.tvTitle.setText("体温枪");
            return BLE_TYPE_TW;
        } else {
            return -1;
        }
    }

}