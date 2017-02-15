package com.slash.bletest;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.slash.IBleCallBack;
import com.slash.IBleInterface;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by icean on 2017/2/12.
 */

public class BleService extends Service {

    public class BleBinder extends IBleInterface.Stub{

        private Context mContext;
        private BluetoothAdapter mLeAdapter;
        private BluetoothGatt mCurrentGatt;
        private BluetoothGattCharacteristic mCurrentCharacteristic;

        private boolean mHasLeFeature;
        private String mRemoteDeviceAddress;

        private ArrayList<IBleCallBack> mOpCallbacks;

        private Handler mBleBinderHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                mLeAdapter.stopLeScan(null);
                notificationLeOp(BleAppConfig.BLE_FINISH_SCAN_DEVICE, BleAppConfig.LE_OP_RESULT_SUCCESS);
            }
        };

        private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
            @Override
            public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                handleLeDeviceFound(device);
            }
        };

        private BluetoothGattCallback mLeGattCallback = new BluetoothGattCallback() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                if (BluetoothProfile.STATE_CONNECTED == newState){//Connect to device success
                    notificationLeOp(BleAppConfig.BLE_CONNECT_DEVICE, BleAppConfig.LE_OP_RESULT_SUCCESS);
                    mCurrentGatt.discoverServices();//start to search service in this GATT. It will notification result to 'onServicesDiscovered'
                } else if (BluetoothProfile.STATE_DISCONNECTED == newState){//Disconnect with this GATT
                    notificationLeOp(BleAppConfig.BLE_DISCONNECT_DEVICE, BleAppConfig.LE_OP_RESULT_SUCCESS);
                    mCurrentGatt.disconnect();
                    mCurrentGatt = null;
                }
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    notificationLeOp(BleAppConfig.BLE_SERVICES_FOUND, BleAppConfig.LE_OP_RESULT_SUCCESS);
                } else {
                    notificationLeOp(BleAppConfig.BLE_SERVICES_FOUND, BleAppConfig.LE_OP_RESULT_FALSE);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                if (BluetoothGatt.GATT_SUCCESS == status){
                    handleLeRead(characteristic);
                } else {
                    notificationLeOp(BleAppConfig.BLE_READ_CHARACTERISTIC, BleAppConfig.LE_OP_RESULT_FALSE);
                }
            }

            @Override
            public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicWrite(gatt, characteristic, status);
                if (BluetoothGatt.GATT_SUCCESS == status) {
                    notificationLeOp(BleAppConfig.BLE_WRITE_CHARACTERISTIC, BleAppConfig.LE_OP_RESULT_SUCCESS);
                } else {
                    notificationLeOp(BleAppConfig.BLE_WRITE_CHARACTERISTIC, BleAppConfig.LE_OP_RESULT_FALSE);
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                handleLeRead(characteristic);
            }
        };

        public BleBinder(Context ctx) {
            mContext = ctx;
            mLeAdapter = ((BluetoothManager)ctx.getSystemService(BLUETOOTH_SERVICE)).getAdapter();
            mHasLeFeature = ctx.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
            mOpCallbacks = new ArrayList<IBleCallBack>();
        }

        @Override
        public boolean hasLeFeature() throws RemoteException {
            return mHasLeFeature;
        }

        @Override
        public boolean isLeEnable() throws RemoteException {
            return mLeAdapter.enable();
        }

        @Override
        public boolean startDiscoveryDevice(int scan_timeout) throws RemoteException {
            if (null != mLeAdapter) {
                if (mLeAdapter.isDiscovering()) {
                    mLeAdapter.stopLeScan(mLeScanCallback);
                }
            }

            boolean is_start_scan_success = mLeAdapter.startLeScan(mLeScanCallback);
            if (is_start_scan_success) {
                notificationLeOp(BleAppConfig.BLE_START_SCAN_DEVICE, BleAppConfig.LE_OP_RESULT_SUCCESS);
                if (scan_timeout == -1) {
                    mBleBinderHandler.sendEmptyMessageDelayed(0, BleAppConfig.BLE_SCAN_TIMEOUT);
                } else {
                    mBleBinderHandler.sendEmptyMessageDelayed(0, scan_timeout);
                }
            } else {
                notificationLeOp(BleAppConfig.BLE_START_SCAN_DEVICE, BleAppConfig.LE_OP_RESULT_FALSE);
            }
            return is_start_scan_success;
        }

        @Override
        public boolean connectToDevice(String remote_address) throws RemoteException {
            if (!TextUtils.isEmpty(remote_address)) {
                if (!TextUtils.isEmpty(mRemoteDeviceAddress) && mRemoteDeviceAddress.equals(remote_address) && null != mCurrentGatt) {
                    if (mCurrentGatt.connect()) {
                        return true;
                    } else {
                        return false;
                    }
                }

                BluetoothDevice current_device = mLeAdapter.getRemoteDevice(remote_address);
                if (null == current_device) {
                    notificationLeOp(BleAppConfig.BLE_CONNECT_DEVICE, BleAppConfig.LE_OP_RESULT_FALSE);
                    return false;
                }
                mCurrentGatt = current_device.connectGatt(mContext, false, mLeGattCallback);
                mRemoteDeviceAddress = remote_address;
                return true;
            }
            return false;
        }

        @Override
        public boolean connectToGatt(String UUID) throws RemoteException {
            if (null == mLeAdapter || null == mCurrentGatt) {
                return false;
            }
            BluetoothGattCharacteristic current_characteristic = getTargetCharacteristic(UUID);
            if (null == current_characteristic) {
                notificationLeOp(BleAppConfig.BLE_CONNECT_GATT, BleAppConfig.LE_OP_RESULT_FALSE);
                return false;
            } else {
                mCurrentGatt.setCharacteristicNotification(current_characteristic, true);
                notificationLeOp(BleAppConfig.BLE_CONNECT_GATT, BleAppConfig.LE_OP_RESULT_SUCCESS);
                return true;
            }
        }

        @Override
        public boolean disconnect() throws RemoteException {
            if (null != mCurrentGatt){
                mCurrentGatt.disconnect();
                mCurrentGatt.close();
                mRemoteDeviceAddress = null;
                return true;
            }
            return false;
        }

        @Override
        public void readCharacteristic() throws RemoteException {
            if (null == mCurrentGatt || null == mCurrentCharacteristic){
                notificationLeOp(BleAppConfig.BLE_READ_CHARACTERISTIC, BleAppConfig.LE_OP_RESULT_FALSE);
            } else {
                mCurrentGatt.readCharacteristic(mCurrentCharacteristic);
            }
        }

        @Override
        public void writeCharacteristic() throws RemoteException {
            if (null == mCurrentGatt || null == mCurrentCharacteristic){
                notificationLeOp(BleAppConfig.BLE_WRITE_CHARACTERISTIC, BleAppConfig.LE_OP_RESULT_FALSE);
            } else {
                mCurrentGatt.readCharacteristic(mCurrentCharacteristic);
            }
        }

        @Override
        public void addBleOpCallback(IBleCallBack call_back) throws RemoteException {
            if (null == mOpCallbacks)
                mOpCallbacks = new ArrayList<IBleCallBack>();

            if (!mOpCallbacks.contains(call_back))
                mOpCallbacks.add(call_back);
        }

        @Override
        public void removeBleOpCallback(IBleCallBack call_back) throws RemoteException {
            if (null == mOpCallbacks)
                mOpCallbacks = new ArrayList<IBleCallBack>();

            if (mOpCallbacks.contains(call_back))
                mOpCallbacks.remove(call_back);
        }

        private BluetoothGattCharacteristic getTargetCharacteristic(String target_uuid){
            if (TextUtils.isEmpty(target_uuid))
                return null;

            List<BluetoothGattService> all_services = mCurrentGatt.getServices();
            if (null == all_services || all_services.size() <= 0)
                return null;

            BluetoothGattCharacteristic target_characteristic = null;
            for (BluetoothGattService current_service : all_services) {
                List<BluetoothGattCharacteristic> all_characteristics = current_service.getCharacteristics();
                for (BluetoothGattCharacteristic current_characteristics : all_characteristics){
                    String current_c_uuid = current_characteristics.getUuid().toString();
                    if (target_uuid.equals(current_c_uuid)) {
                        target_characteristic = current_characteristics;
                    }
                }
            }
            return target_characteristic;
        }

        private void notificationLeOp(int op, int op_result) {
            if (null != mOpCallbacks && mOpCallbacks.size() > 0) {
                for (IBleCallBack op_callback : mOpCallbacks) {
                    try {
                        op_callback.onBleOpResult(op, op_result);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
            }
        }

        private void handleLeDeviceFound(BluetoothDevice current_device) {
            if (null != mOpCallbacks && mOpCallbacks.size() > 0) {
                for (IBleCallBack op_callback : mOpCallbacks) {
                    try {
                        op_callback.onDeviceScan(current_device.getName(), current_device.getAddress(),
                                current_device.getBluetoothClass().toString());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        private void handleLeRead(BluetoothGattCharacteristic characteristic) {
            if (null != mOpCallbacks && mOpCallbacks.size() > 0) {
                for (IBleCallBack op_callback : mOpCallbacks) {
                    try {
                        op_callback.onReadCharacteristic(characteristic.getValue());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private BleBinder mBleBinder;

    @Override
    public void onCreate() {
        super.onCreate();
        mBleBinder = new BleBinder(this);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBleBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
