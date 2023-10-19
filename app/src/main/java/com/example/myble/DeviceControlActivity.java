package com.example.myble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myble.databinding.ActivityDeviceControlBinding;

import java.util.List;

public class DeviceControlActivity extends AppCompatActivity {
    private ActivityDeviceControlBinding viewBinding;
    private boolean isConnect; //蓝牙是否连接
    private String deviceAddress; //BLE地址
    private BluetoothLeService mBluetoothLeService;
    private Intent intent;
    private int deviceType; //设备类型

    public static void actionStart(Context context, String address, int deviceType) {
        context.startActivity(new Intent(context, DeviceControlActivity.class)
                .putExtra("address", address)
                .putExtra("deviceType", deviceType)
        );
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewBinding = ActivityDeviceControlBinding.inflate(getLayoutInflater());
        setContentView(viewBinding.getRoot());
        initExtras();
        regConnectStateReceiver();
        regBLEStateReceiver();
        startBLEService();
        initBLEState();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            connectBLE();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private void initExtras() {
        deviceAddress = getIntent().getStringExtra("address");
        deviceType = getIntent().getIntExtra("deviceType", -1);
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_connect:
                connectBLE();
                break;
            case R.id.btn_disconnect:
                disconnectBLE();
                break;
            case R.id.btn_request:
                mBluetoothLeService.request();
                break;
            case R.id.btn_read:
                mBluetoothLeService.read();
                break;
        }
    }

    private void initBLEState() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int state = bluetoothAdapter.getState();
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                viewBinding.tvBluetoothState.setText("手机蓝牙正在开启");
                break;
            case BluetoothAdapter.STATE_ON:
                viewBinding.tvBluetoothState.setText("手机蓝牙已开启");
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                viewBinding.tvBluetoothState.setText("手机蓝牙正在关闭");
                break;
            case BluetoothAdapter.STATE_OFF:
                viewBinding.tvBluetoothState.setText("手机蓝牙已关闭");
                break;
        }
    }

    /**
     * 开启Service
     */
    private void startBLEService() {
        intent = new Intent(this, BluetoothLeService.class);
        intent.putExtra("deviceType", deviceType);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * 停止Service
     */
    private void stopBLEService() {
        stopService(intent);
        unbindService(serviceConnection);
    }

    /**
     * 连接BLE
     */
    private void connectBLE() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.connect(deviceAddress);
        }
    }

    /**
     * 断开BLE
     */
    private void disconnectBLE() {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.disconnect();
        }
    }

    /**
     * 广播action
     */
    private static IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_WRITE);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_CHANGED);
        return intentFilter;
    }

    /**
     * 注册连接状态广播
     */
    private void regConnectStateReceiver() {
        registerReceiver(mConnectStateReceiver, getIntentFilter());
    }

    /**
     * 注销连接状态广播
     */
    private void unregConnectStateReceiver() {
        unregisterReceiver(mConnectStateReceiver);
    }

    /**
     * 监听连接状态广播
     */
    private final BroadcastReceiver mConnectStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                isConnect = true;
                viewBinding.tvConnectState.setText("连接成功");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                isConnect = false;
                viewBinding.tvConnectState.setText("连接断开");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                showGattServices(mBluetoothLeService.getGattServices());
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_WRITE.equals(action)) {
                String message = intent.getStringExtra("message");
                viewBinding.tvRequest.setText(message);
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_CHANGED.equals(action)) {
                String message = intent.getStringExtra("message");
                viewBinding.tvReceiver.setText(message);
            }
        }
    };

    /**
     * 监听蓝牙状态广播
     */
    private final BroadcastReceiver mBLEStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.e("TAG", "蓝牙设备已连接");
                    viewBinding.tvBluetoothState.setText("蓝牙设备已连接");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.e("TAG", "蓝牙设备断开连接");
                    viewBinding.tvBluetoothState.setText("蓝牙设备断开连接");
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.e("TAG", "手机蓝牙正在开启");
                            viewBinding.tvBluetoothState.setText("手机蓝牙正在开启");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.e("TAG", "手机蓝牙已开启");
                            viewBinding.tvBluetoothState.setText("手机蓝牙已开启");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e("TAG", "手机蓝牙正在关闭");
                            viewBinding.tvBluetoothState.setText("手机蓝牙正在关闭");
                            disconnectBLE();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.e("TAG", "手机蓝牙已关闭");
                            viewBinding.tvBluetoothState.setText("手机蓝牙已关闭");
                            mBluetoothLeService.close();
                            break;
                    }
                    break;
            }
        }
    };

    /**
     * 注册蓝牙状态广播
     */
    private void regBLEStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBLEStateReceiver, filter);
    }

    /**
     * 注销蓝牙状态广播
     */
    private void unregBLEStateReceiver() {
        unregisterReceiver(mBLEStateReceiver);
    }

    private void showGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (BluetoothGattService gattService : gattServices) {
            String serviceUuid = gattService.getUuid().toString();
            builder.append("服务uuid: " + serviceUuid).append("\n");
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                String uuid = gattCharacteristic.getUuid().toString();
                builder.append("\t\t\t特征uuid: " + uuid).append("\n");
            }
        }
        viewBinding.tvUuids.setText(builder.toString());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnectBLE();
        stopBLEService();
        unregConnectStateReceiver();
        unregBLEStateReceiver();
    }
}