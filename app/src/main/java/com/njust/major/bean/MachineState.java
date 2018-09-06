package com.njust.major.bean;


public class MachineState {

    private int _id; //设备编号0
    private String machineID; //机器id
    private String version; //版本
    private int vmState; //设备状态
    private int leftState;//边柜状态0 = 正常开启，1 = 关闭
    private int rightState;

    //各部分状态相关
    private byte leftTempState;//温控模式：0x00=制冷0x01=制热0x02=常温
    private byte leftSetTemp;//-20～70
    private byte leftCabinetTemp;//边柜温度，有符号数，127=故障
    private byte leftCabinetTopTemp;//边柜顶部温度，有符号数，127=故障
    private byte leftCompressorTemp;//压缩机温度，有符号数，127=故障
    private byte leftCompressorDCfanState;//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
    private byte leftCabinetDCfanState;//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
    private int leftDoor;//门开关状态，0=关门，1=开门
    private int leftDoorheat;//边柜门加热状态，0=关，1=开
    private int leftHumidity;//湿度测量值，%RH
    private int leftLight;//照明灯状态，0=关，1=开
    private int leftPushGoodsRaster;//下货光栅状态，0=正常，1=故障
    private int leftOutGoodsRaster;//X轴出货光栅状态，0=正常，1=故障

    private byte rightTempState;//温控模式：0x00=制冷0x01=制热0x02=常温
    private byte rightSetTemp;//-20～70
    private byte rightCabinetTemp;//边柜温度，有符号数，127=故障
    private byte rightCabinetTopTemp;//边柜顶部温度，有符号数，127=故障
    private byte rightCompressorTemp;//压缩机温度，有符号数，127=故障
    private byte rightCompressorDCfanState;//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
    private byte rightCabinetDCfanState;//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
    private int rightDoor;//门开关状态，0=关门，1=开门
    private int rightDoorheat;//边柜门加热状态，0=关，1=开
    private int rightHumidity;//湿度测量值，%RH
    private int rightLight;//照明灯状态，0=关，1=开
    private int rightPushGoodsRaster;//下货光栅状态，0=正常，1=故障
    private int rightOutGoodsRaster;//X轴出货光栅状态，0=正常，1=故障

    private int midLight;//中间照明灯状态，0=关，1=开
    private int midDoorLock;//中间门锁状态，0=上锁，1=开锁
    private int midGetGoodsRaster;//中间取货光栅状态，0=正常，1=故障
    private int midDropGoodsRaster;//中间落货光栅状态，0=正常，1=故障
    private int midAntiPinchHandRaster;//中间防夹手光栅状态，0=正常，1=故障

    //位置相关
    private int leftOutPosition;//Y轴电机出货口位置
    private int[] leftFlootPosition;//每一层的位置
    private int leftFlootNo;//层数
//    private int[] leftFloorMotorType;//电机类型 6层*10+列，1=单弹簧机，2=双弹簧机，3=窄履带机，4=宽弹簧机
//    private int[] leftFloorColWidth;//格位宽度 6层*10+列

    private int rightOutPosition;//Y轴电机出货口位置
    private int[] rightFlootPosition;//每一层的位置
    private int rightFlootNo;//层数
//    private int[] rightFloorMotorType;//电机类型 6层*10+列，1=单弹簧机，2=双弹簧机，3=窄履带机，4=宽弹簧机
//    private int[] rightFloorColWidth;//格位宽度 6层*10+列

    public  MachineState(){

    }
    public MachineState(int _id, String machineID, String version, int vmState, int leftState, int rightState, byte leftTempState, byte leftSetTemp, byte leftCabinetTemp, byte leftCabinetTopTemp, byte leftCompressorTemp, byte leftCompressorDCfanState, byte leftCabinetDCfanState, int leftDoor, int leftDoorheat, int leftHumidity, int leftLight, int leftPushGoodsRaster, int leftOutGoodsRaster, byte rightTempState, byte rightSetTemp, byte rightCabinetTemp, byte rightCabinetTopTemp, byte rightCompressorTemp, byte rightCompressorDCfanState, byte rightCabinetDCfanState, int rightDoor, int rightDoorheat, int rightHumidity, int rightLight, int rightPushGoodsRaster, int rightOutGoodsRaster, int midLight, int midDoorLock, int midGetGoodsRaster, int midDropGoodsRaster, int midAntiPinchHandRaster, int leftOutPosition, int[] leftFlootPosition, int leftFlootNo,int rightOutPosition, int[] rightFlootPosition, int rightFlootNo) {
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
        this.leftDoor = leftDoor;
        this.leftDoorheat = leftDoorheat;
        this.leftHumidity = leftHumidity;
        this.leftLight = leftLight;
        this.leftPushGoodsRaster = leftPushGoodsRaster;
        this.leftOutGoodsRaster = leftOutGoodsRaster;
        this.rightTempState = rightTempState;
        this.rightSetTemp = rightSetTemp;
        this.rightCabinetTemp = rightCabinetTemp;
        this.rightCabinetTopTemp = rightCabinetTopTemp;
        this.rightCompressorTemp = rightCompressorTemp;
        this.rightCompressorDCfanState = rightCompressorDCfanState;
        this.rightCabinetDCfanState = rightCabinetDCfanState;
        this.rightDoor = rightDoor;
        this.rightDoorheat = rightDoorheat;
        this.rightHumidity = rightHumidity;
        this.rightLight = rightLight;
        this.rightPushGoodsRaster = rightPushGoodsRaster;
        this.rightOutGoodsRaster = rightOutGoodsRaster;
        this.midLight = midLight;
        this.midDoorLock = midDoorLock;
        this.midGetGoodsRaster = midGetGoodsRaster;
        this.midDropGoodsRaster = midDropGoodsRaster;
        this.midAntiPinchHandRaster = midAntiPinchHandRaster;
        this.leftOutPosition = leftOutPosition;
        this.leftFlootPosition = leftFlootPosition;
        this.leftFlootNo = leftFlootNo;
        this.rightOutPosition = rightOutPosition;
        this.rightFlootPosition = rightFlootPosition;
        this.rightFlootNo = rightFlootNo;
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

    public byte getLeftTempState() {
        return leftTempState;
    }

    public void setLeftTempState(byte leftTempState) {
        this.leftTempState = leftTempState;
    }

    public byte getLeftSetTemp() {
        return leftSetTemp;
    }

    public void setLeftSetTemp(byte leftSetTemp) {
        this.leftSetTemp = leftSetTemp;
    }

    public byte getLeftCabinetTemp() {
        return leftCabinetTemp;
    }

    public void setLeftCabinetTemp(byte leftCabinetTemp) {
        this.leftCabinetTemp = leftCabinetTemp;
    }

    public byte getLeftCabinetTopTemp() {
        return leftCabinetTopTemp;
    }

    public void setLeftCabinetTopTemp(byte leftCabinetTopTemp) {
        this.leftCabinetTopTemp = leftCabinetTopTemp;
    }

    public byte getLeftCompressorTemp() {
        return leftCompressorTemp;
    }

    public void setLeftCompressorTemp(byte leftCompressorTemp) {
        this.leftCompressorTemp = leftCompressorTemp;
    }

    public byte getLeftCompressorDCfanState() {
        return leftCompressorDCfanState;
    }

    public void setLeftCompressorDCfanState(byte leftCompressorDCfanState) {
        this.leftCompressorDCfanState = leftCompressorDCfanState;
    }

    public byte getLeftCabinetDCfanState() {
        return leftCabinetDCfanState;
    }

    public void setLeftCabinetDCfanState(byte leftCabinetDCfanState) {
        this.leftCabinetDCfanState = leftCabinetDCfanState;
    }

    public int getLeftDoor() {
        return leftDoor;
    }

    public void setLeftDoor(int leftDoor) {
        this.leftDoor = leftDoor;
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

    public byte getRightTempState() {
        return rightTempState;
    }

    public void setRightTempState(byte rightTempState) {
        this.rightTempState = rightTempState;
    }

    public byte getRightSetTemp() {
        return rightSetTemp;
    }

    public void setRightSetTemp(byte rightSetTemp) {
        this.rightSetTemp = rightSetTemp;
    }

    public byte getRightCabinetTemp() {
        return rightCabinetTemp;
    }

    public void setRightCabinetTemp(byte rightCabinetTemp) {
        this.rightCabinetTemp = rightCabinetTemp;
    }

    public byte getRightCabinetTopTemp() {
        return rightCabinetTopTemp;
    }

    public void setRightCabinetTopTemp(byte rightCabinetTopTemp) {
        this.rightCabinetTopTemp = rightCabinetTopTemp;
    }

    public byte getRightCompressorTemp() {
        return rightCompressorTemp;
    }

    public void setRightCompressorTemp(byte rightCompressorTemp) {
        this.rightCompressorTemp = rightCompressorTemp;
    }

    public byte getRightCompressorDCfanState() {
        return rightCompressorDCfanState;
    }

    public void setRightCompressorDCfanState(byte rightCompressorDCfanState) {
        this.rightCompressorDCfanState = rightCompressorDCfanState;
    }

    public byte getRightCabinetDCfanState() {
        return rightCabinetDCfanState;
    }

    public void setRightCabinetDCfanState(byte rightCabinetDCfanState) {
        this.rightCabinetDCfanState = rightCabinetDCfanState;
    }

    public int getRightDoor() {
        return rightDoor;
    }

    public void setRightDoor(int rightDoor) {
        this.rightDoor = rightDoor;
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
}
