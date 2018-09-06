package com.njust.major.setting;

import android.util.Log;

import com.njust.SerialPort;

import static com.njust.major.error.errorHandling.byteTo8Byte;


public class SettingTestThread extends Thread {

    public boolean sendflag = false;
    private SerialPort serialPort;

    public String outFoodResponse;
    public String getFoodResponse;
    public String fallFoodResponse;
    public String foodRoadResponse;
    public String xResponse;

    public SettingTestThread() {

    }

    public void init(SerialPort serialPort) {
        this.serialPort = serialPort;
        sendflag = true;
    }
    public void setPort(SerialPort serialPort485){
        this.serialPort = serialPort485;
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            while (sendflag) {
                byte[] rec = serialPort.receiveData();
                if (rec != null) {
                    if (rec.length >= 15) {
                        StringBuilder str1 = new StringBuilder();
                        int length = rec.length;
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
                        }
                        Log.i("happy", "收到串口：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == length && rec[2] == 0x00 && rec[4] == (byte) 0x60 && rec[length - 2] == (byte) 0xF1 /*&& isVerify(rec)*/) {
                            Log.i("happy", "进行改变");
                            if (rec[6] == 0x31 && rec[7] == 0x58) {
                                xResponse = "";
                                byte error1[];
                                error1 = byteTo8Byte(rec[9]);
                                if (error1[0] != (byte)0x00) {
                                    xResponse += "已执行动作 ";
                                } else {
                                    xResponse += "未执行动作 ";
                                }
                                if(error1[1] == (byte)0x01){
                                    xResponse += "X轴电机过流，";
                                }
                                if(error1[2] == (byte)0x01){
                                    xResponse += "X轴电机断路，";
                                }
                                if(error1[3] == (byte)0x01){
                                    xResponse += "X轴出货光栅未检测到货物，";
                                }
                                if(error1[4] == (byte)0x01){
                                    xResponse += "X轴出货光栅货物遮挡超时，";
                                }
                                if(error1[5] == (byte)0x01){
                                    xResponse += "X轴出货光栅故障，";
                                }
                                if(error1[6] == (byte)0x01){
                                    xResponse += "X轴电机超时，";
                                }
                                xResponse += "\r\n";
                                xResponse += "X轴电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                xResponse += "X轴电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                xResponse += "X轴电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                            }
                            if (rec[6] == 0x31 && rec[7] == 0x5A) {
                                outFoodResponse = "";
                                byte error1[];
                                error1 = byteTo8Byte(rec[9]);
                                if (error1[0] != (byte)0x00) {
                                    outFoodResponse += "已执行动作 ";
                                } else {
                                    outFoodResponse += "未执行动作 ";
                                }
                                if(error1[1] == (byte)0x01){
                                    outFoodResponse += "出货门电机过流，";
                                }
                                if(error1[2] == (byte)0x01){
                                    outFoodResponse += "出货门电机断路，";
                                }
                                if(error1[3] == (byte)0x01){
                                    outFoodResponse += "出货门前止点开关故障，";
                                }
                                if(error1[4] == (byte)0x01){
                                    outFoodResponse += "出货门后止点开关故障，";
                                }
                                if(error1[5] == (byte)0x01){
                                    outFoodResponse += "开、关出货门超时，";
                                }
                                if(error1[6] == (byte)0x01){
                                    outFoodResponse += "出货门半开、半关，";
                                }
                                xResponse += "\r\n";
                                xResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                xResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                xResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                            }
                            if (rec[6] == 0x35 && rec[7] == 0x4D) {
                                getFoodResponse = "";
                                byte error1[];
                                byte error2[];
                                error1 = byteTo8Byte(rec[9]);
                                error2 = byteTo8Byte(rec[8]);
                                if (error1[0] != (byte)0x00) {
                                    getFoodResponse += "已执行动作 ";
                                } else {
                                    getFoodResponse += "未执行动作 ";
                                }
                                if(error1[1] == (byte)0x01){
                                    getFoodResponse += "取货门电机过流，";
                                }
                                if(error1[2] == (byte)0x01){
                                    getFoodResponse += "取货门电机断路，";
                                }
                                if(error1[3] == (byte)0x01){
                                    getFoodResponse += "取货门上止点开关故障，";
                                }
                                if(error1[4] == (byte)0x01){
                                    getFoodResponse += "取货门下止点开关故障，";
                                }
                                if(error1[5] == (byte)0x01){
                                    getFoodResponse += "开、关取货门超时，";
                                }
                                if(error1[6] == (byte)0x01){
                                    getFoodResponse += "取货门半开、半关，";
                                }
                                if(error1[7] == (byte)0x01){
                                    getFoodResponse += "取货仓光栅检测无货物，";
                                }
                                if(error2[0] == (byte)0x01){
                                    getFoodResponse += "取货仓光栅检测有货物，";
                                }
                                if(error2[1] == (byte)0x01){
                                    getFoodResponse += "取货篮光栅故障，";
                                }
                                if(error2[2] == (byte)0x01){
                                    getFoodResponse += "防夹手光栅检测到遮挡物，";
                                }
                                if(error2[3] == (byte)0x01){
                                    getFoodResponse += "防夹手光栅故障，";
                                }
                                getFoodResponse += "\r\n";
                                getFoodResponse += "取货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                getFoodResponse += "取货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                getFoodResponse += "取货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                            }
                            if (rec[6] == 0x35 && rec[7] == 0x46) {
                                fallFoodResponse = "";
                                byte error1[];
                                byte error2[];
                                error1 = byteTo8Byte(rec[9]);
                                error2 = byteTo8Byte(rec[8]);
                                if (error1[0] != (byte)0x00) {
                                    fallFoodResponse += "已执行动作，";
                                } else {
                                    fallFoodResponse += "未执行动作，";
                                }
                                if(error1[1] == (byte)0x01){
                                    fallFoodResponse += "落货门电机过流，";
                                }
                                if(error1[2] == (byte)0x01){
                                    fallFoodResponse += "落货门电机断路，";
                                }
                                if(error1[3] == (byte)0x01){
                                    fallFoodResponse += "落货门上止点开关故障，";
                                }
                                if(error1[4] == (byte)0x01){
                                    fallFoodResponse += "落货门下止点开关故障，";
                                }
                                if(error1[5] == (byte)0x01){
                                    fallFoodResponse += "开、关落货门超时，";
                                }
                                if(error1[6] == (byte)0x01){
                                    fallFoodResponse += "落货门半开、半关，";
                                }
                                if(error1[7] == (byte)0x01){
                                    fallFoodResponse += "落货仓光栅检测无货物，";
                                }
                                if(error2[0] == (byte)0x01){
                                    fallFoodResponse += "落货仓光栅检测有货物，";
                                }
                                if(error2[1] == (byte)0x01){
                                    fallFoodResponse += "落货篮光栅故障，";
                                }
                                fallFoodResponse += "\r\n";
                                fallFoodResponse += "落货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                fallFoodResponse += "落货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                fallFoodResponse += "落货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                            }
                            if (rec[6] == 0x0A && rec[7] == 0x50) {
                                foodRoadResponse = "";
                                byte error1[];
                                error1 = byteTo8Byte(rec[9]);
                                if (error1[0] != (byte)0x00) {
                                    foodRoadResponse += "已执行动作 ";
                                } else {
                                    foodRoadResponse += "未执行动作 ";
                                }
                                if(error1[1] == (byte)0x01){
                                    foodRoadResponse += "货道电机过流，";
                                }
                                if(error1[2] == (byte)0x01){
                                    foodRoadResponse += "货道电机断路，";
                                }
                                if(error1[3] == (byte)0x01){
                                    foodRoadResponse += "货道超时（无货物输出的超时），";
                                }
                                if(error1[4] == (byte)0x01){
                                    foodRoadResponse += "商品超时（货物遮挡光栅超时），";
                                }
                                if(error1[5] == (byte)0x01){
                                    foodRoadResponse += "弹簧电机1反馈开关故障，";
                                }
                                if(error1[6] == (byte)0x01){
                                    foodRoadResponse += "弹簧电机2反馈开关故障，";
                                }
                                if(error1[7] == (byte)0x01){
                                    foodRoadResponse += "货道下货光栅故障，";
                                }
                                xResponse += "\r\n";
                                xResponse += "货道电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                xResponse += "货道电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                xResponse += "货道电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
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