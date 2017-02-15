// IBleInterface.aidl
package com.slash;

// Declare any non-default types here with import statements
import com.slash.IBleCallBack;

interface IBleInterface {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    //void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat, double aDouble, String aString);

    /**
    * Determine whether there is LE feature in this device
    */
    boolean hasLeFeature();

    /**
    * Determine whether bluetooth switch had been ON now
    */
    boolean isLeEnable();

    /**
    *
    */
    boolean startDiscoveryDevice(int scan_timeout);
    boolean connectToDevice(String remote_address);
    boolean connectToGatt(String UUID);
    boolean disconnect();
    void readCharacteristic();
    void writeCharacteristic();
    void addBleOpCallback(in IBleCallBack call_back);
    void removeBleOpCallback(in IBleCallBack call_back);
}
