package com.njust.major.setting;

import android.util.Log;

import com.njust.SerialPort;

import static com.njust.major.error.errorHandling.byteTo8Byte;


public class SettingReceiveThread extends Thread {

    public boolean sendFlag = false;
    public int gearPosition = 0;

    private SerialPort serialPort485;

    public int[] yResponse = new int[5];
    public String Response;
    public SettingReceiveThread(SerialPort serialPort485) {
        this.serialPort485 = serialPort485;
    }
    public void setPort(SerialPort serialPort485){
        this.serialPort485 = serialPort485;
    }

    @Override
    public void run() {
        super.run();
        while (true){
            while (sendFlag) {
                byte[] rec = serialPort485.receiveData();
                if (rec != null) {
                    if (rec.length > 15) {
                        StringBuilder str1 = new StringBuilder();
                        int length = rec.length;
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.i("happy", "收到串口：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == length && rec[2] == 0x00 && rec[4] == (byte) 0x60 && rec[length - 2] == (byte) 0xF1/* && isVerify(rec)*/) {
                            if (rec[6] == 0x31 && rec[7] == 0x59) {
                                yResponse[0] = (rec[8]&0xff) * 256 + (rec[9]&0xff);
                                yResponse[1] = (rec[10]&0xff) * 256 + (rec[11]&0xff);
                                yResponse[2] = (rec[12]&0xff) * 256 + (rec[13]&0xff);
                                yResponse[3] = (rec[14]&0xff) * 256 + (rec[15]&0xff);
                                yResponse[4] = (rec[16]&0xff) * 256 + (rec[17]&0xff);
                                Log.i("happy", "进来了"+yResponse[4]);
                                gearPosition = yResponse[4];
                                Response = "";
                                byte error1[];
                                error1 = byteTo8Byte(rec[9]);
                                if (error1[0] != (byte)0x00) {
                                    Response += "已执行动作 ";
                                } else {
                                    Response += "未执行动作 ";
                                }
                                if(error1[1] == (byte)0x01){
                                    Response += "Y轴电机过流，";
                                }
                                if(error1[2] == (byte)0x01){
                                    Response += "Y轴电机断路，";
                                }
                                if(error1[3] == (byte)0x01){
                                    Response += "Y轴上止点开关故障，";
                                }
                                if(error1[4] == (byte)0x01){
                                    Response += "Y轴下止点开关故障，";
                                }
                                if(error1[5] == (byte)0x01){
                                    Response += "Y轴电机超时，";
                                }
                                if(error1[6] == (byte)0x01){
                                    Response += "Y轴码盘故障，";
                                }
                                if(error1[7] == (byte)0x01){
                                    Response += "Y轴出货门定位开关故障，";
                                }
                                Response += "\r\n";
                                Response += "电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                Response += "电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                Response += "电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                            }
                        }
                    }
                }

            }
        }
    }

    private static boolean isVerify(byte[] rec) {
        int sum = 0;
        for (int i = 0; i < rec.length - 1; i++) {
            sum = sum + rec[i];
        }
        return sum == rec[rec.length - 1];
    }
}