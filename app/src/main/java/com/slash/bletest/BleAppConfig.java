package com.slash.bletest;


/**
 * Created by icean on 2017/2/12.
 */

public final class BleAppConfig {
    public static final int BLE_SCAN_TIMEOUT = 20000;

    public static final int BLE_START_SCAN_DEVICE = 0;
    public static final int BLE_FINISH_SCAN_DEVICE = 1;
    public static final int BLE_CONNECT_DEVICE = 2;
    public static final int BLE_DISCONNECT_DEVICE = 3;
    public static final int BLE_CONNECT_GATT = 4;
    public static final int BLE_SERVICES_FOUND = 6;
    public static final int BLE_READ_CHARACTERISTIC = 7;
    public static final int BLE_WRITE_CHARACTERISTIC = 8;

    public static final int LE_OP_RESULT_SUCCESS = 1;
    public static final int LE_OP_RESULT_FALSE = 2;
}
