package com.njust.major.setting;


import android.os.SystemClock;
import android.util.Log;


import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;

import static com.njust.VMApplication.midZNum;
import static com.njust.VMApplication.rimZNum1;
import static com.njust.VMApplication.rimZNum2;


public class SettingTestThread extends Thread {

    public boolean sendflag = false;
    private SerialPort serialPort;
    private MotorControl mMotorControl;

    public String outFoodResponse;
    public String getFoodResponse;
    public String fallFoodResponse;
    public String foodRoadResponse;
    public String xResponse;


    public boolean openGetFoodFlag;
    public boolean closeGetFoodFlag;
    public boolean openFallFoodFlag;
    public boolean closeFallFoodFlag;

    public boolean foodRoadFlag1;
    public boolean openOutFoodFlag1;
    public boolean closeOutFoodFlag1;
    public boolean xFlag1;
    public boolean foodRoadFlag2;
    public boolean openOutFoodFlag2;
    public boolean closeOutFoodFlag2;
    public boolean xFlag2;

    private int delay =150;


    public SettingTestThread(MotorControl motorControl) {
        this.mMotorControl = motorControl;
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
            while (sendflag) {
                while (foodRoadFlag1){
                    mMotorControl.query((byte)0x0C,(byte)0x80,rimZNum1);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[7] == (byte)0x50){
                                if(rec[16] == (byte)0x02){
                                    rimZNum1++;
                                    foodRoadFlag1 = false;
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
                                    foodRoadResponse += "\r\n";
                                    foodRoadResponse += "货道电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                    foodRoadResponse += "货道电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                    foodRoadResponse += "货道电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                }
                            }
                        }
                    }
                }
                while(openGetFoodFlag){
                    mMotorControl.query((byte)0x07,(byte)0xE0,midZNum);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                                if(rec[16] == (byte)0x02){
                                    midZNum++;
                                    openGetFoodFlag = false;
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
                            }
                        }
                    }
                }
                while(closeGetFoodFlag){
                    mMotorControl.query((byte)0x08,(byte)0xE0,midZNum);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                                if(rec[16] == (byte)0x02){
                                    midZNum++;
                                    closeGetFoodFlag = false;
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
                            }
                        }
                    }
                }
                while(openOutFoodFlag1){
                    mMotorControl.query((byte)0x04,(byte)0xC0,rimZNum1);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                                if(rec[16] == (byte)0x02){
                                    rimZNum1++;
                                    openOutFoodFlag1 = false;
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
                                    outFoodResponse += "\r\n";
                                    outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                }
                            }
                        }
                    }
                }
                while(closeOutFoodFlag1){
                    mMotorControl.query((byte)0x05,(byte)0xC0,rimZNum1);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                                if(rec[16] == (byte)0x02){
                                    rimZNum1++;
                                    closeOutFoodFlag1 = false;
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
                                    outFoodResponse += "\r\n";
                                    outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                }
                            }
                        }
                    }
                }
                while(openFallFoodFlag){
                    mMotorControl.query((byte)0x09,(byte)0xE0,midZNum);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                                if(rec[16] == (byte)0x02){
                                    midZNum++;
                                    openFallFoodFlag = false;
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
                            }
                        }
                    }
                }
                while(closeFallFoodFlag){
                    mMotorControl.query((byte)0x0A,(byte)0xE0,midZNum);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x6C && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                                if(rec[16] == (byte)0x02){
                                    midZNum++;
                                    closeFallFoodFlag = false;
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
                            }
                        }
                    }
                }
                while(xFlag1){
                    mMotorControl.query((byte)0x02,(byte)0xC0,rimZNum1);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                                if(rec[16] == (byte)0x02){
                                    rimZNum1++;
                                    xFlag1 = false;
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
                            }
                        }
                    }
                }
                while(foodRoadFlag2){
                    mMotorControl.query((byte)0x0C,(byte)0x81,rimZNum2);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[7] == (byte)0x50){
                                if(rec[16] == (byte)0x02){
                                    rimZNum2++;
                                    foodRoadFlag2 = false;
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
                                    foodRoadResponse += "\r\n";
                                    foodRoadResponse += "货道电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                    foodRoadResponse += "货道电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                    foodRoadResponse += "货道电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                }
                            }
                        }
                    }
                }
                while(openOutFoodFlag2){
                    mMotorControl.query((byte)0x04,(byte)0xC1,rimZNum2);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                                if(rec[16] == (byte)0x02){
                                    rimZNum2++;
                                    openOutFoodFlag2 = false;
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
                                    outFoodResponse += "\r\n";
                                    outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                }
                            }
                        }
                    }
                }
                while(closeOutFoodFlag2){
                    mMotorControl.query((byte)0x05,(byte)0xC1,rimZNum2);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                                if(rec[16] == (byte)0x02){
                                    rimZNum2++;
                                    closeOutFoodFlag2 = false;
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
                                    outFoodResponse += "\r\n";
                                    outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                    outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                }
                            }
                        }
                    }
                }
                while(xFlag2){
                    mMotorControl.query((byte)0x02,(byte)0xC1,rimZNum2);
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "查询485收到串口："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                                if(rec[16] == (byte)0x02){
                                    rimZNum2++;
                                    xFlag2 = false;
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
                            }
                        }
                    }
                }
            }
    }
    /**
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
     * @param b 1个字节byte数据
     * */
    private static byte[] byteTo8Byte(byte b) {
        byte[] array = new byte[8];
        for (int i = 0; i <= 7; i++) {
            array[i] = (byte)(b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }
}