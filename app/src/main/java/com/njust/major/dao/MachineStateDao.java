package com.njust.major.dao;

import com.njust.major.bean.MachineState;


public interface MachineStateDao {

    public  void updateSetting(int counter, int flootNo, int outPosition, String flootPositions);//初始配置更新到数据库

    public void updateDCfan(int leftCompressorDCfanState, int leftCabinetDCfanState, int rightCompressorDCfanState, int rightCabinetDCfanState);//更改压缩机直流风扇状态和边柜直流风扇状态

    public void updateTemperature(int leftTempState, int leftSetTemp, int rightTempState, int rightSetTemp);//更改设定的温控模式和温度,温控模式：0x00=制冷0x01=制热0x02=常温,-20～70

    public void updateMeasureTemp(int leftCompressorTemp, int leftCabinetTemp, int leftCabinetTopTemp, int leftHumidity,
                                  int rightCompressorTemp, int rightCabinetTemp,int rightCabinetTopTemp, int rightHumidity);//更新压缩机温度、边柜温度、边柜顶部温度、湿度测量值

    public void updateLight(int leftLight, int rightLight, int midLight);//照明灯状态，0=关，1=开

    public void updateVersion(String version);//更新版本号

    public void updateState(int state);//更新整个机器的状态

    public void updateCounterState(int leftState, int rightState);//更新左右边柜的状态

    public void updateDoorState(int leftDoor, int rightDoor, int midDoor,int midDoorLock);//更新边柜门开关状态，以及中间门锁状态 0=关门，1=开门

    public void updateCounterDoorState(int leftDoorheat, int rightDoorheat);//更新门加热状态，0=关，1=开

    public void updateRasterState(int leftPushGoodsRaster, int leftOutGoodsRaster, int rightPushGoodsRaster, int rightOutGoodsRaster, int midGetGoodsRaster, int midDropGoodsRaster, int midAntiPinchHandRaster);//更新边柜下货光栅状态、X轴出货光栅状态，中柜取货光栅状态、落货光栅状态、防夹手光栅状态

    public void updateOtherDoorState(int leftOutGoodsDoor, int rightOutGoodsDoor, int midGetDoor, int midDropDoor);//更新出货门、取货门、落货门开关状态

    public void updateMachineState(MachineState machineState);//更新除设定位置相关的其他所有数据

    public MachineState queryMachineState();

}
