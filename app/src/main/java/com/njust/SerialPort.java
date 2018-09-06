package com.njust;

import java.io.UnsupportedEncodingException;


public class SerialPort extends SerialPortJNI {

    private int Port;
    private String data;

    public SerialPort(int port, int rate, int nBits, char nEvent, int nStop) {
        super();
        this.Port = port;
        super.open(port, rate, nBits, nEvent, nStop);
    }

    public void close() {
        super.close(Port);
    }

    public void sendData(byte[] buf, int length) {
        super.write(Port, buf, length);
    }

    public byte[] receiveData() {
        return super.read(Port);
    }

    public String receiveData(String charsetName) {
        byte[] buf = super.read(Port);
        if (buf != null) {
            int length = buf.length;
            try {
                data = new String(buf, 0, length, charsetName);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            return data;
        } else {
            return null;
        }
    }
}






















