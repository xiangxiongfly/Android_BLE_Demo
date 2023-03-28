package com.example.myble;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class DeviceControlActivity extends AppCompatActivity {
    private boolean connected;
    private String deviceAddress; //BLE地址
    private BluetoothLeService mBluetoothLeService;
    private TextView tv_bluetooth_state;
    private TextView tv_connect_state;
    private Button btn_disconnect;
    private Button btn_connect;
    private TextView tv_uuids;
    private Button btn_request;
    private Button btn_read;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_control);
        initView();
        deviceAddress = getIntent().getStringExtra("address");
        Log.e("TAG", "deviceAddress: " + deviceAddress);
        regStateReceiver();
        Intent serviceIntent = new Intent(this, BluetoothLeService.class);
        bindService(serviceIntent, serviceConnection, Context.BIND_AUTO_CREATE);

        initBLEState();
    }

    private void initView() {
        tv_bluetooth_state = findViewById(R.id.tv_bluetooth_state);
        tv_connect_state = findViewById(R.id.tv_connect_state);
        btn_disconnect = findViewById(R.id.btn_disconnect);
        btn_connect = findViewById(R.id.btn_connect);
        tv_uuids = findViewById(R.id.tv_uuids);
        btn_request = findViewById(R.id.btn_request);
        btn_read = findViewById(R.id.btn_read);

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!connected) {
                    mBluetoothLeService.connect(deviceAddress);
                }
            }
        });
        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (connected) {
                    mBluetoothLeService.disconnect();
                }
            }
        });
        btn_request.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.request();
            }
        });
        btn_read.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetoothLeService.read();
            }
        });
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mBluetoothLeService = null;
        }
    };

    private void initBLEState() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int state = bluetoothAdapter.getState();
        switch (state) {

            case BluetoothAdapter.STATE_TURNING_ON:
                tv_bluetooth_state.setText("手机蓝牙正在开启");
                break;
            case BluetoothAdapter.STATE_ON:
                tv_bluetooth_state.setText("手机蓝牙已开启");
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                tv_bluetooth_state.setText("手机蓝牙正在关闭");
                break;
            case BluetoothAdapter.STATE_OFF:
                tv_bluetooth_state.setText("手机蓝牙已关闭");
                break;
        }
    }

    /**
     * 监听连接状态广播
     */
    private final BroadcastReceiver mUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                tv_connect_state.setText("连接成功");
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                connected = false;
                tv_connect_state.setText("连接断开");
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                displayGattServices(mBluetoothLeService.getGattServices());
            }
        }
    };

    /**
     * 广播action
     */
    private static final IntentFilter getIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        return intentFilter;
    }

    /**
     * 注册连接状态广播
     */
    private void regConnectStateReceiver() {
        registerReceiver(mUpdateReceiver, getIntentFilter());
    }

    /**
     * 注销连接状态广播
     */
    private void unregConnectStateReceiver() {
        unregisterReceiver(mUpdateReceiver);
    }

    /**
     * 监听蓝牙状态广播
     */
    private final BroadcastReceiver mBluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            switch (action) {
                case BluetoothDevice.ACTION_ACL_CONNECTED:
                    Log.e("TAG", "蓝牙设备已连接");
                    tv_bluetooth_state.setText("蓝牙设备已连接");
                    break;
                case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                    Log.e("TAG", "蓝牙设备断开连接");
                    tv_bluetooth_state.setText("蓝牙设备断开连接");
                    break;
                case BluetoothAdapter.ACTION_STATE_CHANGED:
                    int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                    switch (blueState) {
                        case BluetoothAdapter.STATE_TURNING_ON:
                            Log.e("TAG", "手机蓝牙正在开启");
                            tv_bluetooth_state.setText("手机蓝牙正在开启");
                            break;
                        case BluetoothAdapter.STATE_ON:
                            Log.e("TAG", "手机蓝牙已开启");
                            tv_bluetooth_state.setText("手机蓝牙已开启");
                            break;
                        case BluetoothAdapter.STATE_TURNING_OFF:
                            Log.e("TAG", "手机蓝牙正在关闭");
                            tv_bluetooth_state.setText("手机蓝牙正在关闭");
                            mBluetoothLeService.disconnect();
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            Log.e("TAG", "手机蓝牙已关闭");
                            tv_bluetooth_state.setText("手机蓝牙已关闭");
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
    private void regStateReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothStateReceiver, filter);
    }

    /**
     * 注销蓝牙状态广播
     */
    private void unregStateReceiver() {
        unregisterReceiver(mBluetoothStateReceiver);
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
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
        tv_uuids.setText(builder.toString());
    }

    /**
     * onResume()回调时注册广播，并连接蓝牙
     */
    @Override
    protected void onResume() {
        super.onResume();
        regConnectStateReceiver();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.connect(deviceAddress);
        }
    }

    /**
     * onPause()回调是注销广播
     */
    @Override
    protected void onPause() {
        super.onPause();
        unregConnectStateReceiver();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(serviceConnection);
        unregStateReceiver();
    }
}