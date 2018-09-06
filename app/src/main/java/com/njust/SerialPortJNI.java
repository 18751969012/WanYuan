package com.njust;



public class SerialPortJNI {
    static {
     //   Log.w("NativeClass", "before load library");
        System.loadLibrary("native-lib");
    //    Log.w("NativeClass", "after load library");
    }

    public native int open(int Port, int Rate, int nBits, char nEvent, int nStop);

    public native int close(int Port);

    public native byte[] read(int Port);

    public native int write(int Port, byte[] buffer, int len);
}
