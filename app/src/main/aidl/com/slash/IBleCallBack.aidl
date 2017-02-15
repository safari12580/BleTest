// IBleCallBack.aidl
package com.slash;

// Declare any non-default types here with import statements

interface IBleCallBack {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    //void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString);

    void onDeviceScan(String device_name, String device_address, String device_class);
    void onBleOpResult(int op_code, int op_result);
    void onReadCharacteristic(in byte[] ble_value);
}
