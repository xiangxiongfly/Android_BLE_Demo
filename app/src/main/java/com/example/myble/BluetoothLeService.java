package com.example.myble;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_GATT_SERVICES_WRITE =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_WRITE";
    public final static String ACTION_GATT_SERVICES_CHANGED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_CHANGED";

    // 体温 服务UUID
    private static final String SERVICE_UUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    // 体温 特征UUID
    private static final String CHARACTERISTIC_UUID = "0000fff1-0000-1000-8000-00805f9b34fb";

    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    private static final int STATE_DISCONNECTED = 0; //蓝牙断开状态
    private static final int STATE_CONNECTED = 2; //蓝牙连接状态
    private int connectionState; //连接状态

    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic characteristic;

    private Binder binder = new LocalBinder();
    private int deviceType;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        deviceType = intent.getIntExtra("deviceType", -1);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    /**
     * 连接gatt服务
     */
    public boolean connect(final String address) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
            // 连接Gatt服务，用于通信
            mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
            return true;
        } catch (IllegalArgumentException e) {
            Log.e("TAG", "连接异常");
            return false;
        }
    }

    /**
     * 断开连接
     */
    public void disconnect() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.disconnect();
    }

    /**
     * 发送指令
     */
    public void request() {
        writeCharacteristic(characteristic, BytesUtils.hexStringToBytes("938e0400080410"));
    }

    /**
     * 读取数据
     */
    public void read() {
        readCharacteristic(characteristic);
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        /**
         * 连接状态监听
         */
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            Log.e("TAG", "onConnectionStateChange");
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                // 成功连接Gatt服务
                Log.e("TAG", "成功连接Gatt服务");
                connectionState = STATE_CONNECTED;
                broadcastUpdate(ACTION_GATT_CONNECTED);
                // 发现BLE提供的服务
                mBluetoothGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                // 与GATT服务断开连接
                Log.e("TAG", "与GATT服务断开连接");
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(ACTION_GATT_DISCONNECTED);
                mBluetoothGatt = null;
            }
        }

        /**
         * 发现服务和特征，并订阅通知
         */
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(SERVICE_UUID));
                characteristic = service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));
                // 设置订阅通知
                setCharacteristicNotification(characteristic, true);
            } else {
                Log.e("TAG", "onServicesDiscovered received: " + status);
            }
        }

        /**
         * 发送读指令监听
         */
        @Override
        public void onCharacteristicRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value, int status) {
            super.onCharacteristicRead(gatt, characteristic, value, status);
            Log.e("TAG", "onCharacteristicRead");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                byte[] data = value;
                // 处理读取到的数据
                Log.e("TAG", "获取特征数据：" + BytesUtils.bytesToHexString(data));
            }
        }

        /**
         * 发送写指令监听
         */
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
            Log.e("TAG", "onCharacteristicWrite");
            Log.e("TAG", "获取写指令：" + BytesUtils.bytesToHexString(characteristic.getValue()));
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.e("TAG", "写入成功");
                broadcastUpdate(ACTION_GATT_SERVICES_WRITE, BytesUtils.bytesToHexString(characteristic.getValue()));
            } else {
                Log.e("TAG", "写入失败");
            }
        }

        /**
         * 数据变化监听
         * 获取从BLE设备的数据
         */
        @Override
        public void onCharacteristicChanged(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            super.onCharacteristicChanged(gatt, characteristic, value);
            Log.e("TAG", "onCharacteristicChanged");
            Log.e("TAG", "从设备接收数据：" + BytesUtils.bytesToHexString(value));
            broadcastUpdate(ACTION_GATT_SERVICES_CHANGED, BytesUtils.bytesToHexString(value));
        }

        /**
         * 订阅成功监听
         */
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
            Log.e("TAG", "onDescriptorWrite");
        }

        @Override
        public void onDescriptorRead(@NonNull BluetoothGatt gatt, @NonNull BluetoothGattDescriptor descriptor, int status, @NonNull byte[] value) {
            super.onDescriptorRead(gatt, descriptor, status, value);
            Log.e("TAG", "onDescriptorRead");
        }
    };

    public List<BluetoothGattService> getGattServices() {
        if (mBluetoothGatt == null) {
            return null;
        }
        return mBluetoothGatt.getServices();
    }


    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    /**
     * 关闭Gatt连接
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    /**
     * 广播更新
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, String message) {
        final Intent intent = new Intent(action);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    /**
     * 读取BEL设备的特征值
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothGatt != null && characteristic != null) {
            mBluetoothGatt.readCharacteristic(characteristic);
        }
    }

    /**
     * 向BLE设备写入特征值
     */
    public void writeCharacteristic(BluetoothGattCharacteristic characteristic, byte[] data) {
        if (mBluetoothGatt != null && characteristic != null) {
            characteristic.setValue(data);
            mBluetoothGatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * 订阅通知，监听 BLE 设备特征值变化
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
        BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    }
}