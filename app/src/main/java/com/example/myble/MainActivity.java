package com.example.myble;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.XXPermissions;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private TextView tv_device;

    public static final int REQUEST_ENABLE_BT = 0x1;

    private boolean isScanning = false;
    private Handler handler = new Handler();
    private static final long SCAN_PERIOD = 30_000; //扫描时间

    private BluetoothDevice mDevice;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initPermissions();
    }

    private void initView() {
        tv_device = findViewById(R.id.tv_device);
        Button btn_search = findViewById(R.id.btn_search);
        Button btn_stop_search = findViewById(R.id.btn_stop_search);

        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startScan();
            }
        });
        btn_stop_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopScan();
            }
        });
    }

    private BluetoothAdapter getBluetoothAdapter() {
//        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
//        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
//        return bluetoothAdapter;
        // 等价于如下
        return BluetoothAdapter.getDefaultAdapter();
    }

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

    public void clickCheck(View view) {
        checkBLE();
    }

    public void clickCheckEnabled(View view) {
        checkEnabled();
    }

    public void clickConnect(View view) {
        startDeviceActivity();
    }

    // 检查设备是否支持蓝牙
    private void checkBLE() {
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "当前设备支持蓝牙", Toast.LENGTH_SHORT).show();
        }
        if (getBluetoothAdapter() == null) {
            Toast.makeText(this, "当前设备不支持蓝牙", Toast.LENGTH_SHORT).show();
        }
    }

    // 蓝牙是否开启
    private boolean checkEnabled() {
        boolean isEnabled = getBluetoothAdapter().isEnabled();
        Toast.makeText(this, isEnabled ? "蓝牙开启" : "蓝牙未开启", Toast.LENGTH_SHORT).show();
        return isEnabled;
    }

    // 开启蓝牙
    public void clickOepn(View view) {
        if (!checkEnabled()) {
            BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
            // 直接开启蓝牙
            bluetoothAdapter.enable();
        }
    }

    // 开启蓝牙2
    public void clickOpen2(View view) {
        if (!checkEnabled()) {
            // 优雅开启蓝牙
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_ENABLE_BT);
        }
    }

    // 搜索周围BLE设备
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

    // 停止搜索
    public void stopScan() {
        if (isScanning) {
            Log.e("TAG", "停止扫描");
            isScanning = false;
            BluetoothLeScanner bluetoothLeScanner = getBluetoothAdapter().getBluetoothLeScanner();
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }

    // 搜索回调
    private final ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) {
                    super.onScanResult(callbackType, result);
                    BluetoothDevice device = result.getDevice();
                    if (device != null) {
                        StringBuilder builder = new StringBuilder();
                        builder.append("设备名：" + device.getName()).append("\n");
                        builder.append("地址：" + device.getAddress()).append("\n");
                        builder.append("uuids：" + device.getUuids());
                        String deviceStr = builder.toString();
                        Log.e("TAG", "BLE : " + deviceStr);
                        if (filterDevice(device)) {
                            Log.e("TAG", "找到指定设备，停止扫描");
                            stopScan();
                            mDevice = device;
                            tv_device.setText(deviceStr);
                        }
                    }
                }
            };

    private void startDeviceActivity() {
        if (mDevice != null) {
            startActivity(new Intent(this, DeviceControlActivity.class).putExtra("address", mDevice.getAddress()));
        }
    }

    // 过滤设备
    private boolean filterDevice(BluetoothDevice device) {
        return "BLE-EMP-Ui".equals(device.getName());
    }

}