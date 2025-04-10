package com.zktecodevice.device_monitoring;
import com.sun.jna.Library;
import com.sun.jna.Native;

public class CatracaSDK {
    public interface Plcommpro extends Library {
        Plcommpro INSTANCE = Native.load("plcommpro", Plcommpro.class);

        int Connect(String parameters);

        int Disconnect(int handle);

        int GetRTLog(int handle, byte[] buffer, int bufferSize);

        int ControlDevice(int handle, int operationId, int param1, int param2, int param3, int param4, String options);
    }
}
