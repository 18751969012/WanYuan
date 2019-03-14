package com.njust.major.bean;


import java.util.Arrays;

public class MachineState {

    private int _id; //设备编号0
    private String machineID; //机器id
    private String version; //版本
    private int vmState; //设备状态
    private int leftState;//边柜状态0 = 正常开启，1 = 关闭
    private int rightState;

    //各部分状态相关
    private int leftTempState;//温控模式：0=制冷1=制热2=常温
    private int leftSetTemp;//设定温度:-20～70
    private int leftCabinetTemp;//边柜温度：有符号数（22222为故障）
    private int leftCabinetTopTemp;//边柜顶部温度：有符号数（22222为故障）
    private int leftCompressorTemp;//压缩机温度：有符号数（22222为故障）
    private int leftCompressorDCfanState;//压缩机直流风扇状态：0=未启动1=正常2=异常
    private int leftCabinetDCfanState;//边柜直流风扇状态：0=未启动1=正常2=异常
    private int leftOutCabinetTemp;//外部温度测量值：有符号数（22222为温湿度传感器故障）
    private int leftDoorheat;//边柜门加热状态：0=关1=开
    private int leftHumidity;//湿度测量值：100以内代表百分比（22222为温湿度传感器故障）
    private int leftLight;//照明灯状态：0=关1=开
    private int leftPushGoodsRaster;//下货光栅状态：0=正常1=故障
    private int leftOutGoodsRaster;//X轴出货光栅状态：0=正常1=故障
    private int leftOutGoodsDoor;//出货门开关状态：0=关1=开2=半开半关

    private int rightTempState;//温控模式：0=制冷1=制热2=常温
    private int rightSetTemp;//设定温度:-20～70
    private int rightCabinetTemp;//边柜温度：有符号数（22222为故障）
    private int rightCabinetTopTemp;//边柜顶部温度：有符号数（22222为故障）
    private int rightCompressorTemp;//压缩机温度：有符号数（22222为故障）
    private int rightCompressorDCfanState;//压缩机直流风扇状态：0=未启动1=正常2=异常
    private int rightCabinetDCfanState;//边柜直流风扇状态：0=未启动1=正常2=异常
    private int rightOutCabinetTemp;//外部温度测量值：有符号数（22222为温湿度传感器故障）
    private int rightDoorheat;//边柜门加热状态：0=关1=开
    private int rightHumidity;//湿度测量值：100以内代表百分比（22222为温湿度传感器故障）
    private int rightLight;//照明灯状态：0=关1=开
    private int rightPushGoodsRaster;//下货光栅状态：0=正常1=故障
    private int rightOutGoodsRaster;//X轴出货光栅状态：0=正常1=故障
    private int rightOutGoodsDoor;//出货门开关状态：0=关1=开2=半开半关

    private int midLight;//中间照明灯状态：0=关1=开
    private int midDoorLock;//中间门锁状态：0=上锁1=开锁
    private int midDoor;//中间门开关状态：1=关门0=开门
    private int midGetGoodsRaster;//中间取货光栅状态：0=正常1=故障
    private int midDropGoodsRaster;//中间落货光栅状态：0=正常1=故障
    private int midAntiPinchHandRaster;//中间防夹手光栅状态：0=正常1=故障
    private int midGetDoor;//取货门开关状态：0=关1=开2=半开半关
    private int midDropDoor;//落货门开关状态：0=关1=开2=半开半关

    //位置相关
    private int leftOutPosition;//Y轴电机出货口位置
    private int[] leftFlootPosition;//每一层的位置
    private int leftFlootNo;//层数

    private int rightOutPosition;//Y轴电机出货口位置
    private int[] rightFlootPosition;//每一层的位置
    private int rightFlootNo;//层数

    //更新单片机通讯协议后新加的字段
    private int leftTempControlAlternatPower;//温控交流总电源状态（0=断开，1=接通）
    private int leftRefrigerationCompressorState;//制冷压缩机工作状态（0=停止，1=运转）
    private int leftCompressorFanState;//压缩机风扇工作状态（0=停止，1=运转）
    private int leftHeatingWireState;//制热电热丝工作状态（0=断开，1=接通）
    private int leftRecirculatAirFanState;//循环风风扇工作状态（0=停止，1=运转）

    private int leftLiftPlatformDownSwitch;//升降台下止点开关即时状态（0=开，1=合）
    private int leftLiftPlatformUpSwitch;//升降台上止点开关即时状态（0=开，1=合）
    private int leftLiftPlatformOutGoodsSwitch;//升降台出货口开关即时状态（0=开，1=合）
    private int leftOutGoodsRasterImmediately;//出货光栅传感器即时状态（0=透光，1=遮光）
    private int leftPushGoodsRasterImmediately;//下货光栅即时状态（0=透光，1=遮光）
    private int leftMotorFeedbackState1;//电机1反馈状态（0=凸台位，1=缺口位）
    private int leftMotorFeedbackState2;//电机2反馈状态（0=凸台位，1=缺口位）
    private int leftOutGoodsDoorDownSwitch;//出货门下止点开关即时状态（0=开，1=合）
    private int leftOutGoodsDoorUpSwitch;//出货门上止点开关即时状态（0=开，1=合）

    private int rightTempControlAlternatPower;//温控交流总电源状态（0=断开，1=接通）
    private int rightRefrigerationCompressorState;//制冷压缩机工作状态（0=停止，1=运转）
    private int rightCompressorFanState;//压缩机风扇工作状态（0=停止，1=运转）
    private int rightHeatingWireState;//制热电热丝工作状态（0=断开，1=接通）
    private int rightRecirculatAirFanState;//循环风风扇工作状态（0=停止，1=运转）

    private int rightLiftPlatformDownSwitch;//升降台下止点开关即时状态（0=开，1=合）
    private int rightLiftPlatformUpSwitch;//升降台上止点开关即时状态（0=开，1=合）
    private int rightLiftPlatformOutGoodsSwitch;//升降台出货口开关即时状态（0=开，1=合）
    private int rightOutGoodsRasterImmediately;//出货光栅传感器即时状态（0=透光，1=遮光）
    private int rightPushGoodsRasterImmediately;//下货光栅即时状态（0=透光，1=遮光）
    private int rightMotorFeedbackState1;//电机1反馈状态（0=凸台位，1=缺口位）
    private int rightMotorFeedbackState2;//电机2反馈状态（0=凸台位，1=缺口位）
    private int rightOutGoodsDoorDownSwitch;//出货门下止点开关即时状态（0=开，1=合）
    private int rightOutGoodsDoorUpSwitch;//出货门上止点开关即时状态（0=开，1=合）


    private int midGetDoorWaitClose;//取货门待关门标志（0=正常，1=待关门）
    private int midGetDoorDownSwitch;//取货门下止点开关即时状态（0=开，1=合）
    private int midGetDoorUpSwitch;//取货门上止点开关即时状态（0=开，1=合）
    private int midDropDoorDownSwitch;//落货门下止点开关即时状态（0=开，1=合）
    private int midDropDoorUpSwitch;//落货门上止点开关即时状态（0=开，1=合）


    private int electricQuantity;//机器用电量，保存值为扩大一百倍

    public MachineState() {
    }

    public MachineState(int _id, String machineID, String version, int vmState, int leftState, int rightState, int leftTempState, int leftSetTemp, int leftCabinetTemp, int leftCabinetTopTemp, int leftCompressorTemp, int leftCompressorDCfanState, int leftCabinetDCfanState, int leftOutCabinetTemp, int leftDoorheat, int leftHumidity, int leftLight, int leftPushGoodsRaster, int leftOutGoodsRaster, int leftOutGoodsDoor, int rightTempState, int rightSetTemp, int rightCabinetTemp, int rightCabinetTopTemp, int rightCompressorTemp, int rightCompressorDCfanState, int rightCabinetDCfanState, int rightOutCabinetTemp, int rightDoorheat, int rightHumidity, int rightLight, int rightPushGoodsRaster, int rightOutGoodsRaster, int rightOutGoodsDoor, int midLight, int midDoorLock, int midDoor, int midGetGoodsRaster, int midDropGoodsRaster, int midAntiPinchHandRaster, int midGetDoor, int midDropDoor, int leftOutPosition, int[] leftFlootPosition, int leftFlootNo, int rightOutPosition, int[] rightFlootPosition, int rightFlootNo, int leftTempControlAlternatPower, int leftRefrigerationCompressorState, int leftCompressorFanState, int leftHeatingWireState, int leftRecirculatAirFanState, int leftLiftPlatformDownSwitch, int leftLiftPlatformUpSwitch, int leftLiftPlatformOutGoodsSwitch, int leftOutGoodsRasterImmediately, int leftPushGoodsRasterImmediately, int leftMotorFeedbackState1, int leftMotorFeedbackState2, int leftOutGoodsDoorDownSwitch, int leftOutGoodsDoorUpSwitch, int rightTempControlAlternatPower, int rightRefrigerationCompressorState, int rightCompressorFanState, int rightHeatingWireState, int rightRecirculatAirFanState, int rightLiftPlatformDownSwitch, int rightLiftPlatformUpSwitch, int rightLiftPlatformOutGoodsSwitch, int rightOutGoodsRasterImmediately, int rightPushGoodsRasterImmediately, int rightMotorFeedbackState1, int rightMotorFeedbackState2, int rightOutGoodsDoorDownSwitch, int rightOutGoodsDoorUpSwitch, int midGetDoorWaitClose, int midGetDoorDownSwitch, int midGetDoorUpSwitch, int midDropDoorDownSwitch, int midDropDoorUpSwitch, int electricQuantity) {
        this._id = _id;
        this.machineID = machineID;
        this.version = version;
        this.vmState = vmState;
        this.leftState = leftState;
        this.rightState = rightState;
        this.leftTempState = leftTempState;
        this.leftSetTemp = leftSetTemp;
        this.leftCabinetTemp = leftCabinetTemp;
        this.leftCabinetTopTemp = leftCabinetTopTemp;
        this.leftCompressorTemp = leftCompressorTemp;
        this.leftCompressorDCfanState = leftCompressorDCfanState;
        this.leftCabinetDCfanState = leftCabinetDCfanState;
        this.leftOutCabinetTemp = leftOutCabinetTemp;
        this.leftDoorheat = leftDoorheat;
        this.leftHumidity = leftHumidity;
        this.leftLight = leftLight;
        this.leftPushGoodsRaster = leftPushGoodsRaster;
        this.leftOutGoodsRaster = leftOutGoodsRaster;
        this.leftOutGoodsDoor = leftOutGoodsDoor;
        this.rightTempState = rightTempState;
        this.rightSetTemp = rightSetTemp;
        this.rightCabinetTemp = rightCabinetTemp;
        this.rightCabinetTopTemp = rightCabinetTopTemp;
        this.rightCompressorTemp = rightCompressorTemp;
        this.rightCompressorDCfanState = rightCompressorDCfanState;
        this.rightCabinetDCfanState = rightCabinetDCfanState;
        this.rightOutCabinetTemp = rightOutCabinetTemp;
        this.rightDoorheat = rightDoorheat;
        this.rightHumidity = rightHumidity;
        this.rightLight = rightLight;
        this.rightPushGoodsRaster = rightPushGoodsRaster;
        this.rightOutGoodsRaster = rightOutGoodsRaster;
        this.rightOutGoodsDoor = rightOutGoodsDoor;
        this.midLight = midLight;
        this.midDoorLock = midDoorLock;
        this.midDoor = midDoor;
        this.midGetGoodsRaster = midGetGoodsRaster;
        this.midDropGoodsRaster = midDropGoodsRaster;
        this.midAntiPinchHandRaster = midAntiPinchHandRaster;
        this.midGetDoor = midGetDoor;
        this.midDropDoor = midDropDoor;
        this.leftOutPosition = leftOutPosition;
        this.leftFlootPosition = leftFlootPosition;
        this.leftFlootNo = leftFlootNo;
        this.rightOutPosition = rightOutPosition;
        this.rightFlootPosition = rightFlootPosition;
        this.rightFlootNo = rightFlootNo;
        this.leftTempControlAlternatPower = leftTempControlAlternatPower;
        this.leftRefrigerationCompressorState = leftRefrigerationCompressorState;
        this.leftCompressorFanState = leftCompressorFanState;
        this.leftHeatingWireState = leftHeatingWireState;
        this.leftRecirculatAirFanState = leftRecirculatAirFanState;
        this.leftLiftPlatformDownSwitch = leftLiftPlatformDownSwitch;
        this.leftLiftPlatformUpSwitch = leftLiftPlatformUpSwitch;
        this.leftLiftPlatformOutGoodsSwitch = leftLiftPlatformOutGoodsSwitch;
        this.leftOutGoodsRasterImmediately = leftOutGoodsRasterImmediately;
        this.leftPushGoodsRasterImmediately = leftPushGoodsRasterImmediately;
        this.leftMotorFeedbackState1 = leftMotorFeedbackState1;
        this.leftMotorFeedbackState2 = leftMotorFeedbackState2;
        this.leftOutGoodsDoorDownSwitch = leftOutGoodsDoorDownSwitch;
        this.leftOutGoodsDoorUpSwitch = leftOutGoodsDoorUpSwitch;
        this.rightTempControlAlternatPower = rightTempControlAlternatPower;
        this.rightRefrigerationCompressorState = rightRefrigerationCompressorState;
        this.rightCompressorFanState = rightCompressorFanState;
        this.rightHeatingWireState = rightHeatingWireState;
        this.rightRecirculatAirFanState = rightRecirculatAirFanState;
        this.rightLiftPlatformDownSwitch = rightLiftPlatformDownSwitch;
        this.rightLiftPlatformUpSwitch = rightLiftPlatformUpSwitch;
        this.rightLiftPlatformOutGoodsSwitch = rightLiftPlatformOutGoodsSwitch;
        this.rightOutGoodsRasterImmediately = rightOutGoodsRasterImmediately;
        this.rightPushGoodsRasterImmediately = rightPushGoodsRasterImmediately;
        this.rightMotorFeedbackState1 = rightMotorFeedbackState1;
        this.rightMotorFeedbackState2 = rightMotorFeedbackState2;
        this.rightOutGoodsDoorDownSwitch = rightOutGoodsDoorDownSwitch;
        this.rightOutGoodsDoorUpSwitch = rightOutGoodsDoorUpSwitch;
        this.midGetDoorWaitClose = midGetDoorWaitClose;
        this.midGetDoorDownSwitch = midGetDoorDownSwitch;
        this.midGetDoorUpSwitch = midGetDoorUpSwitch;
        this.midDropDoorDownSwitch = midDropDoorDownSwitch;
        this.midDropDoorUpSwitch = midDropDoorUpSwitch;
        this.electricQuantity = electricQuantity;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getMachineID() {
        return machineID;
    }

    public void setMachineID(String machineID) {
        this.machineID = machineID;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getVmState() {
        return vmState;
    }

    public void setVmState(int vmState) {
        this.vmState = vmState;
    }

    public int getLeftState() {
        return leftState;
    }

    public void setLeftState(int leftState) {
        this.leftState = leftState;
    }

    public int getRightState() {
        return rightState;
    }

    public void setRightState(int rightState) {
        this.rightState = rightState;
    }

    public int getLeftTempState() {
        return leftTempState;
    }

    public void setLeftTempState(int leftTempState) {
        this.leftTempState = leftTempState;
    }

    public int getLeftSetTemp() {
        return leftSetTemp;
    }

    public void setLeftSetTemp(int leftSetTemp) {
        this.leftSetTemp = leftSetTemp;
    }

    public int getLeftCabinetTemp() {
        return leftCabinetTemp;
    }

    public void setLeftCabinetTemp(int leftCabinetTemp) {
        this.leftCabinetTemp = leftCabinetTemp;
    }

    public int getLeftCabinetTopTemp() {
        return leftCabinetTopTemp;
    }

    public void setLeftCabinetTopTemp(int leftCabinetTopTemp) {
        this.leftCabinetTopTemp = leftCabinetTopTemp;
    }

    public int getLeftCompressorTemp() {
        return leftCompressorTemp;
    }

    public void setLeftCompressorTemp(int leftCompressorTemp) {
        this.leftCompressorTemp = leftCompressorTemp;
    }

    public int getLeftCompressorDCfanState() {
        return leftCompressorDCfanState;
    }

    public void setLeftCompressorDCfanState(int leftCompressorDCfanState) {
        this.leftCompressorDCfanState = leftCompressorDCfanState;
    }

    public int getLeftCabinetDCfanState() {
        return leftCabinetDCfanState;
    }

    public void setLeftCabinetDCfanState(int leftCabinetDCfanState) {
        this.leftCabinetDCfanState = leftCabinetDCfanState;
    }

    public int getLeftOutCabinetTemp() {
        return leftOutCabinetTemp;
    }

    public void setLeftOutCabinetTemp(int leftOutCabinetTemp) {
        this.leftOutCabinetTemp = leftOutCabinetTemp;
    }

    public int getLeftDoorheat() {
        return leftDoorheat;
    }

    public void setLeftDoorheat(int leftDoorheat) {
        this.leftDoorheat = leftDoorheat;
    }

    public int getLeftHumidity() {
        return leftHumidity;
    }

    public void setLeftHumidity(int leftHumidity) {
        this.leftHumidity = leftHumidity;
    }

    public int getLeftLight() {
        return leftLight;
    }

    public void setLeftLight(int leftLight) {
        this.leftLight = leftLight;
    }

    public int getLeftPushGoodsRaster() {
        return leftPushGoodsRaster;
    }

    public void setLeftPushGoodsRaster(int leftPushGoodsRaster) {
        this.leftPushGoodsRaster = leftPushGoodsRaster;
    }

    public int getLeftOutGoodsRaster() {
        return leftOutGoodsRaster;
    }

    public void setLeftOutGoodsRaster(int leftOutGoodsRaster) {
        this.leftOutGoodsRaster = leftOutGoodsRaster;
    }

    public int getLeftOutGoodsDoor() {
        return leftOutGoodsDoor;
    }

    public void setLeftOutGoodsDoor(int leftOutGoodsDoor) {
        this.leftOutGoodsDoor = leftOutGoodsDoor;
    }

    public int getRightTempState() {
        return rightTempState;
    }

    public void setRightTempState(int rightTempState) {
        this.rightTempState = rightTempState;
    }

    public int getRightSetTemp() {
        return rightSetTemp;
    }

    public void setRightSetTemp(int rightSetTemp) {
        this.rightSetTemp = rightSetTemp;
    }

    public int getRightCabinetTemp() {
        return rightCabinetTemp;
    }

    public void setRightCabinetTemp(int rightCabinetTemp) {
        this.rightCabinetTemp = rightCabinetTemp;
    }

    public int getRightCabinetTopTemp() {
        return rightCabinetTopTemp;
    }

    public void setRightCabinetTopTemp(int rightCabinetTopTemp) {
        this.rightCabinetTopTemp = rightCabinetTopTemp;
    }

    public int getRightCompressorTemp() {
        return rightCompressorTemp;
    }

    public void setRightCompressorTemp(int rightCompressorTemp) {
        this.rightCompressorTemp = rightCompressorTemp;
    }

    public int getRightCompressorDCfanState() {
        return rightCompressorDCfanState;
    }

    public void setRightCompressorDCfanState(int rightCompressorDCfanState) {
        this.rightCompressorDCfanState = rightCompressorDCfanState;
    }

    public int getRightCabinetDCfanState() {
        return rightCabinetDCfanState;
    }

    public void setRightCabinetDCfanState(int rightCabinetDCfanState) {
        this.rightCabinetDCfanState = rightCabinetDCfanState;
    }

    public int getRightOutCabinetTemp() {
        return rightOutCabinetTemp;
    }

    public void setRightOutCabinetTemp(int rightOutCabinetTemp) {
        this.rightOutCabinetTemp = rightOutCabinetTemp;
    }

    public int getRightDoorheat() {
        return rightDoorheat;
    }

    public void setRightDoorheat(int rightDoorheat) {
        this.rightDoorheat = rightDoorheat;
    }

    public int getRightHumidity() {
        return rightHumidity;
    }

    public void setRightHumidity(int rightHumidity) {
        this.rightHumidity = rightHumidity;
    }

    public int getRightLight() {
        return rightLight;
    }

    public void setRightLight(int rightLight) {
        this.rightLight = rightLight;
    }

    public int getRightPushGoodsRaster() {
        return rightPushGoodsRaster;
    }

    public void setRightPushGoodsRaster(int rightPushGoodsRaster) {
        this.rightPushGoodsRaster = rightPushGoodsRaster;
    }

    public int getRightOutGoodsRaster() {
        return rightOutGoodsRaster;
    }

    public void setRightOutGoodsRaster(int rightOutGoodsRaster) {
        this.rightOutGoodsRaster = rightOutGoodsRaster;
    }

    public int getRightOutGoodsDoor() {
        return rightOutGoodsDoor;
    }

    public void setRightOutGoodsDoor(int rightOutGoodsDoor) {
        this.rightOutGoodsDoor = rightOutGoodsDoor;
    }

    public int getMidLight() {
        return midLight;
    }

    public void setMidLight(int midLight) {
        this.midLight = midLight;
    }

    public int getMidDoorLock() {
        return midDoorLock;
    }

    public void setMidDoorLock(int midDoorLock) {
        this.midDoorLock = midDoorLock;
    }

    public int getMidDoor() {
        return midDoor;
    }

    public void setMidDoor(int midDoor) {
        this.midDoor = midDoor;
    }

    public int getMidGetGoodsRaster() {
        return midGetGoodsRaster;
    }

    public void setMidGetGoodsRaster(int midGetGoodsRaster) {
        this.midGetGoodsRaster = midGetGoodsRaster;
    }

    public int getMidDropGoodsRaster() {
        return midDropGoodsRaster;
    }

    public void setMidDropGoodsRaster(int midDropGoodsRaster) {
        this.midDropGoodsRaster = midDropGoodsRaster;
    }

    public int getMidAntiPinchHandRaster() {
        return midAntiPinchHandRaster;
    }

    public void setMidAntiPinchHandRaster(int midAntiPinchHandRaster) {
        this.midAntiPinchHandRaster = midAntiPinchHandRaster;
    }

    public int getMidGetDoor() {
        return midGetDoor;
    }

    public void setMidGetDoor(int midGetDoor) {
        this.midGetDoor = midGetDoor;
    }

    public int getMidDropDoor() {
        return midDropDoor;
    }

    public void setMidDropDoor(int midDropDoor) {
        this.midDropDoor = midDropDoor;
    }

    public int getLeftOutPosition() {
        return leftOutPosition;
    }

    public void setLeftOutPosition(int leftOutPosition) {
        this.leftOutPosition = leftOutPosition;
    }

    public int getLeftFlootPosition(int floor) {
        return leftFlootPosition[floor];
    }
    public void setLeftFlootPosition(String str) {
        if (str.equals("")) return;
        String[] strs = str.split(" ");
        this.leftFlootPosition = new int[strs.length];
        for (int i = 0; i < strs.length; i++){
            this.leftFlootPosition[i] = Integer.parseInt(strs[i]);
        }
    }

    public int getLeftFlootNo() {
        return leftFlootNo;
    }

    public void setLeftFlootNo(int leftFlootNo) {
        this.leftFlootNo = leftFlootNo;
    }

    public int getRightOutPosition() {
        return rightOutPosition;
    }

    public void setRightOutPosition(int rightOutPosition) {
        this.rightOutPosition = rightOutPosition;
    }

    public int getRightFlootPosition(int floor) {
        return rightFlootPosition[floor];
    }

    public void setRightFlootPosition(String str) {
        if (str.equals("")) return;
        String[] strs = str.split(" ");
        this.rightFlootPosition = new int[strs.length];
        for (int i = 0; i < strs.length; i++){
            this.rightFlootPosition[i] = Integer.parseInt(strs[i]);
        }
    }

    public int getRightFlootNo() {
        return rightFlootNo;
    }

    public void setRightFlootNo(int rightFlootNo) {
        this.rightFlootNo = rightFlootNo;
    }

    public int getLeftTempControlAlternatPower() {
        return leftTempControlAlternatPower;
    }

    public void setLeftTempControlAlternatPower(int leftTempControlAlternatPower) {
        this.leftTempControlAlternatPower = leftTempControlAlternatPower;
    }

    public int getLeftRefrigerationCompressorState() {
        return leftRefrigerationCompressorState;
    }

    public void setLeftRefrigerationCompressorState(int leftRefrigerationCompressorState) {
        this.leftRefrigerationCompressorState = leftRefrigerationCompressorState;
    }

    public int getLeftCompressorFanState() {
        return leftCompressorFanState;
    }

    public void setLeftCompressorFanState(int leftCompressorFanState) {
        this.leftCompressorFanState = leftCompressorFanState;
    }

    public int getLeftHeatingWireState() {
        return leftHeatingWireState;
    }

    public void setLeftHeatingWireState(int leftHeatingWireState) {
        this.leftHeatingWireState = leftHeatingWireState;
    }

    public int getLeftRecirculatAirFanState() {
        return leftRecirculatAirFanState;
    }

    public void setLeftRecirculatAirFanState(int leftRecirculatAirFanState) {
        this.leftRecirculatAirFanState = leftRecirculatAirFanState;
    }

    public int getLeftLiftPlatformDownSwitch() {
        return leftLiftPlatformDownSwitch;
    }

    public void setLeftLiftPlatformDownSwitch(int leftLiftPlatformDownSwitch) {
        this.leftLiftPlatformDownSwitch = leftLiftPlatformDownSwitch;
    }

    public int getLeftLiftPlatformUpSwitch() {
        return leftLiftPlatformUpSwitch;
    }

    public void setLeftLiftPlatformUpSwitch(int leftLiftPlatformUpSwitch) {
        this.leftLiftPlatformUpSwitch = leftLiftPlatformUpSwitch;
    }

    public int getLeftLiftPlatformOutGoodsSwitch() {
        return leftLiftPlatformOutGoodsSwitch;
    }

    public void setLeftLiftPlatformOutGoodsSwitch(int leftLiftPlatformOutGoodsSwitch) {
        this.leftLiftPlatformOutGoodsSwitch = leftLiftPlatformOutGoodsSwitch;
    }

    public int getLeftOutGoodsRasterImmediately() {
        return leftOutGoodsRasterImmediately;
    }

    public void setLeftOutGoodsRasterImmediately(int leftOutGoodsRasterImmediately) {
        this.leftOutGoodsRasterImmediately = leftOutGoodsRasterImmediately;
    }

    public int getLeftPushGoodsRasterImmediately() {
        return leftPushGoodsRasterImmediately;
    }

    public void setLeftPushGoodsRasterImmediately(int leftPushGoodsRasterImmediately) {
        this.leftPushGoodsRasterImmediately = leftPushGoodsRasterImmediately;
    }

    public int getLeftMotorFeedbackState1() {
        return leftMotorFeedbackState1;
    }

    public void setLeftMotorFeedbackState1(int leftMotorFeedbackState1) {
        this.leftMotorFeedbackState1 = leftMotorFeedbackState1;
    }

    public int getLeftMotorFeedbackState2() {
        return leftMotorFeedbackState2;
    }

    public void setLeftMotorFeedbackState2(int leftMotorFeedbackState2) {
        this.leftMotorFeedbackState2 = leftMotorFeedbackState2;
    }

    public int getLeftOutGoodsDoorDownSwitch() {
        return leftOutGoodsDoorDownSwitch;
    }

    public void setLeftOutGoodsDoorDownSwitch(int leftOutGoodsDoorDownSwitch) {
        this.leftOutGoodsDoorDownSwitch = leftOutGoodsDoorDownSwitch;
    }

    public int getLeftOutGoodsDoorUpSwitch() {
        return leftOutGoodsDoorUpSwitch;
    }

    public void setLeftOutGoodsDoorUpSwitch(int leftOutGoodsDoorUpSwitch) {
        this.leftOutGoodsDoorUpSwitch = leftOutGoodsDoorUpSwitch;
    }

    public int getRightTempControlAlternatPower() {
        return rightTempControlAlternatPower;
    }

    public void setRightTempControlAlternatPower(int rightTempControlAlternatPower) {
        this.rightTempControlAlternatPower = rightTempControlAlternatPower;
    }

    public int getRightRefrigerationCompressorState() {
        return rightRefrigerationCompressorState;
    }

    public void setRightRefrigerationCompressorState(int rightRefrigerationCompressorState) {
        this.rightRefrigerationCompressorState = rightRefrigerationCompressorState;
    }

    public int getRightCompressorFanState() {
        return rightCompressorFanState;
    }

    public void setRightCompressorFanState(int rightCompressorFanState) {
        this.rightCompressorFanState = rightCompressorFanState;
    }

    public int getRightHeatingWireState() {
        return rightHeatingWireState;
    }

    public void setRightHeatingWireState(int rightHeatingWireState) {
        this.rightHeatingWireState = rightHeatingWireState;
    }

    public int getRightRecirculatAirFanState() {
        return rightRecirculatAirFanState;
    }

    public void setRightRecirculatAirFanState(int rightRecirculatAirFanState) {
        this.rightRecirculatAirFanState = rightRecirculatAirFanState;
    }

    public int getRightLiftPlatformDownSwitch() {
        return rightLiftPlatformDownSwitch;
    }

    public void setRightLiftPlatformDownSwitch(int rightLiftPlatformDownSwitch) {
        this.rightLiftPlatformDownSwitch = rightLiftPlatformDownSwitch;
    }

    public int getRightLiftPlatformUpSwitch() {
        return rightLiftPlatformUpSwitch;
    }

    public void setRightLiftPlatformUpSwitch(int rightLiftPlatformUpSwitch) {
        this.rightLiftPlatformUpSwitch = rightLiftPlatformUpSwitch;
    }

    public int getRightLiftPlatformOutGoodsSwitch() {
        return rightLiftPlatformOutGoodsSwitch;
    }

    public void setRightLiftPlatformOutGoodsSwitch(int rightLiftPlatformOutGoodsSwitch) {
        this.rightLiftPlatformOutGoodsSwitch = rightLiftPlatformOutGoodsSwitch;
    }

    public int getRightOutGoodsRasterImmediately() {
        return rightOutGoodsRasterImmediately;
    }

    public void setRightOutGoodsRasterImmediately(int rightOutGoodsRasterImmediately) {
        this.rightOutGoodsRasterImmediately = rightOutGoodsRasterImmediately;
    }

    public int getRightPushGoodsRasterImmediately() {
        return rightPushGoodsRasterImmediately;
    }

    public void setRightPushGoodsRasterImmediately(int rightPushGoodsRasterImmediately) {
        this.rightPushGoodsRasterImmediately = rightPushGoodsRasterImmediately;
    }

    public int getRightMotorFeedbackState1() {
        return rightMotorFeedbackState1;
    }

    public void setRightMotorFeedbackState1(int rightMotorFeedbackState1) {
        this.rightMotorFeedbackState1 = rightMotorFeedbackState1;
    }

    public int getRightMotorFeedbackState2() {
        return rightMotorFeedbackState2;
    }

    public void setRightMotorFeedbackState2(int rightMotorFeedbackState2) {
        this.rightMotorFeedbackState2 = rightMotorFeedbackState2;
    }

    public int getRightOutGoodsDoorDownSwitch() {
        return rightOutGoodsDoorDownSwitch;
    }

    public void setRightOutGoodsDoorDownSwitch(int rightOutGoodsDoorDownSwitch) {
        this.rightOutGoodsDoorDownSwitch = rightOutGoodsDoorDownSwitch;
    }

    public int getRightOutGoodsDoorUpSwitch() {
        return rightOutGoodsDoorUpSwitch;
    }

    public void setRightOutGoodsDoorUpSwitch(int rightOutGoodsDoorUpSwitch) {
        this.rightOutGoodsDoorUpSwitch = rightOutGoodsDoorUpSwitch;
    }

    public int getMidGetDoorWaitClose() {
        return midGetDoorWaitClose;
    }

    public void setMidGetDoorWaitClose(int midGetDoorWaitClose) {
        this.midGetDoorWaitClose = midGetDoorWaitClose;
    }

    public int getMidGetDoorDownSwitch() {
        return midGetDoorDownSwitch;
    }

    public void setMidGetDoorDownSwitch(int midGetDoorDownSwitch) {
        this.midGetDoorDownSwitch = midGetDoorDownSwitch;
    }

    public int getMidGetDoorUpSwitch() {
        return midGetDoorUpSwitch;
    }

    public void setMidGetDoorUpSwitch(int midGetDoorUpSwitch) {
        this.midGetDoorUpSwitch = midGetDoorUpSwitch;
    }

    public int getMidDropDoorDownSwitch() {
        return midDropDoorDownSwitch;
    }

    public void setMidDropDoorDownSwitch(int midDropDoorDownSwitch) {
        this.midDropDoorDownSwitch = midDropDoorDownSwitch;
    }

    public int getMidDropDoorUpSwitch() {
        return midDropDoorUpSwitch;
    }

    public void setMidDropDoorUpSwitch(int midDropDoorUpSwitch) {
        this.midDropDoorUpSwitch = midDropDoorUpSwitch;
    }

    public int getElectricQuantity() {
        return electricQuantity;
    }

    public void setElectricQuantity(int electricQuantity) {
        this.electricQuantity = electricQuantity;
    }

    @Override
    public String toString() {
        return "MachineState{" +
                "_id=" + _id +
                ", machineID='" + machineID + '\'' +
                ", version='" + version + '\'' +
                ", vmState=" + vmState +
                ", leftState=" + leftState +
                ", rightState=" + rightState +
                ", leftTempState=" + leftTempState +
                ", leftSetTemp=" + leftSetTemp +
                ", leftCabinetTemp=" + leftCabinetTemp +
                ", leftCabinetTopTemp=" + leftCabinetTopTemp +
                ", leftCompressorTemp=" + leftCompressorTemp +
                ", leftCompressorDCfanState=" + leftCompressorDCfanState +
                ", leftCabinetDCfanState=" + leftCabinetDCfanState +
                ", leftOutCabinetTemp=" + leftOutCabinetTemp +
                ", leftDoorheat=" + leftDoorheat +
                ", leftHumidity=" + leftHumidity +
                ", leftLight=" + leftLight +
                ", leftPushGoodsRaster=" + leftPushGoodsRaster +
                ", leftOutGoodsRaster=" + leftOutGoodsRaster +
                ", leftOutGoodsDoor=" + leftOutGoodsDoor +
                ", rightTempState=" + rightTempState +
                ", rightSetTemp=" + rightSetTemp +
                ", rightCabinetTemp=" + rightCabinetTemp +
                ", rightCabinetTopTemp=" + rightCabinetTopTemp +
                ", rightCompressorTemp=" + rightCompressorTemp +
                ", rightCompressorDCfanState=" + rightCompressorDCfanState +
                ", rightCabinetDCfanState=" + rightCabinetDCfanState +
                ", rightOutCabinetTemp=" + rightOutCabinetTemp +
                ", rightDoorheat=" + rightDoorheat +
                ", rightHumidity=" + rightHumidity +
                ", rightLight=" + rightLight +
                ", rightPushGoodsRaster=" + rightPushGoodsRaster +
                ", rightOutGoodsRaster=" + rightOutGoodsRaster +
                ", rightOutGoodsDoor=" + rightOutGoodsDoor +
                ", midLight=" + midLight +
                ", midDoorLock=" + midDoorLock +
                ", midDoor=" + midDoor +
                ", midGetGoodsRaster=" + midGetGoodsRaster +
                ", midDropGoodsRaster=" + midDropGoodsRaster +
                ", midAntiPinchHandRaster=" + midAntiPinchHandRaster +
                ", midGetDoor=" + midGetDoor +
                ", midDropDoor=" + midDropDoor +
                ", leftOutPosition=" + leftOutPosition +
                ", leftFlootPosition=" + Arrays.toString(leftFlootPosition) +
                ", leftFlootNo=" + leftFlootNo +
                ", rightOutPosition=" + rightOutPosition +
                ", rightFlootPosition=" + Arrays.toString(rightFlootPosition) +
                ", rightFlootNo=" + rightFlootNo +
                ", leftTempControlAlternatPower=" + leftTempControlAlternatPower +
                ", leftRefrigerationCompressorState=" + leftRefrigerationCompressorState +
                ", leftCompressorFanState=" + leftCompressorFanState +
                ", leftHeatingWireState=" + leftHeatingWireState +
                ", leftRecirculatAirFanState=" + leftRecirculatAirFanState +
                ", leftLiftPlatformDownSwitch=" + leftLiftPlatformDownSwitch +
                ", leftLiftPlatformUpSwitch=" + leftLiftPlatformUpSwitch +
                ", leftLiftPlatformOutGoodsSwitch=" + leftLiftPlatformOutGoodsSwitch +
                ", leftOutGoodsRasterImmediately=" + leftOutGoodsRasterImmediately +
                ", leftPushGoodsRasterImmediately=" + leftPushGoodsRasterImmediately +
                ", leftMotorFeedbackState1=" + leftMotorFeedbackState1 +
                ", leftMotorFeedbackState2=" + leftMotorFeedbackState2 +
                ", leftOutGoodsDoorDownSwitch=" + leftOutGoodsDoorDownSwitch +
                ", leftOutGoodsDoorUpSwitch=" + leftOutGoodsDoorUpSwitch +
                ", rightTempControlAlternatPower=" + rightTempControlAlternatPower +
                ", rightRefrigerationCompressorState=" + rightRefrigerationCompressorState +
                ", rightCompressorFanState=" + rightCompressorFanState +
                ", rightHeatingWireState=" + rightHeatingWireState +
                ", rightRecirculatAirFanState=" + rightRecirculatAirFanState +
                ", rightLiftPlatformDownSwitch=" + rightLiftPlatformDownSwitch +
                ", rightLiftPlatformUpSwitch=" + rightLiftPlatformUpSwitch +
                ", rightLiftPlatformOutGoodsSwitch=" + rightLiftPlatformOutGoodsSwitch +
                ", rightOutGoodsRasterImmediately=" + rightOutGoodsRasterImmediately +
                ", rightPushGoodsRasterImmediately=" + rightPushGoodsRasterImmediately +
                ", rightMotorFeedbackState1=" + rightMotorFeedbackState1 +
                ", rightMotorFeedbackState2=" + rightMotorFeedbackState2 +
                ", rightOutGoodsDoorDownSwitch=" + rightOutGoodsDoorDownSwitch +
                ", rightOutGoodsDoorUpSwitch=" + rightOutGoodsDoorUpSwitch +
                ", midGetDoorWaitClose=" + midGetDoorWaitClose +
                ", midGetDoorDownSwitch=" + midGetDoorDownSwitch +
                ", midGetDoorUpSwitch=" + midGetDoorUpSwitch +
                ", midDropDoorDownSwitch=" + midDropDoorDownSwitch +
                ", midDropDoorUpSwitch=" + midDropDoorUpSwitch +
                ", electricQuantity=" + electricQuantity +
                '}';
    }
}
