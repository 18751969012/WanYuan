package com.njust.major.dao;

import android.content.Context;

import com.njust.major.bean.MachineState;


public interface MachineStateDao {

    public  void updateSetting(int counter, int flootNo, int outPosition, String flootPositions);//初始配置更新到数据库

    public void updateDCfan(int leftCompressorDCfanState, int leftCabinetDCfanState, int rightCompressorDCfanState, int rightCabinetDCfanState);//更改压缩机直流风扇状态和边柜直流风扇状态

    public void updateTemperature(int leftTempState, int leftSetTemp, int rightTempState, int rightSetTemp);//更改设定的温控模式和温度,温控模式：0x00=制冷0x01=制热0x02=常温,-20～70

    public void updateMeasureTemp(int leftCompressorTemp, int leftCabinetTemp, int leftCabinetTopTemp, int leftOutCabinetTemp, int leftHumidity,
                                  int rightCompressorTemp, int rightCabinetTemp,int rightCabinetTopTemp, int rightOutCabinetTemp, int rightHumidity);//更新压缩机温度、边柜温度、边柜顶部温度、边柜外部温度、湿度测量值

    public void updateLight(int leftLight, int rightLight, int midLight);//照明灯状态，0=关，1=开

    public void updateVersion(String version);//更新版本号

    public void updateState(int state);//更新整个机器的状态

    public void updateCounterState(int leftState, int rightState);//更新左右边柜的状态

    public void updateDoorState(int midDoor,int midDoorLock);//更新中间门锁、门状态 0=关门，1=开门

    public void updateCounterDoorState(int leftDoorheat, int rightDoorheat);//更新门加热状态，0=关，1=开

    public void updateRasterState(int leftPushGoodsRaster, int leftOutGoodsRaster, int rightPushGoodsRaster, int rightOutGoodsRaster, int midGetGoodsRaster, int midDropGoodsRaster, int midAntiPinchHandRaster);//更新边柜下货光栅状态、X轴出货光栅状态，中柜取货光栅状态、落货光栅状态、防夹手光栅状态

    public void updateOtherDoorState(int leftOutGoodsDoor, int rightOutGoodsDoor, int midGetDoor, int midDropDoor);//更新出货门、取货门、落货门开关状态

    public void updateFan(int leftTempControlAlternatPower, int leftRefrigerationCompressorState, int leftCompressorFanState, int leftHeatingWireState, int leftRecirculatAirFanState,
                          int rightTempControlAlternatPower, int rightRefrigerationCompressorState, int rightCompressorFanState, int rightHeatingWireState, int rightRecirculatAirFanState);//更新温控交流总电源状态、制冷压缩机工作状态、压缩机风扇工作状态、制热电热丝工作状态、循环风风扇工作状态

    public void updateYState( int counter, int liftPlatformDownSwitch, int liftPlatformUpSwitch);//更新Y轴上下止点开关状态

    public void updatePushGoodsRaster(int counter, int pushGoodsRaster);//更新下货光栅即时状态

    public void updateOutGoodsRaster(int counter, int outGoodsRaster);//更新出货光栅传感器即时状态

    public void updateOutGoodsDoorSwitch(int counter, int outGoodsDoorDownSwitch, int outGoodsDoorUpSwitch);//更新出货门上下止点开关即时状态

    public void updateGetGoodsDoorSwitch(int getGoodsDoorDownSwitch, int getGoodsDoorUpSwitch);//更新取货门上下止点开关即时状态

    public void updateElectricQuantity(int electricQuantity);//更新用电量

    public void updateMachineState(MachineState machineState);//更新除设定位置相关的其他所有数据

    public MachineState queryMachineState();

}
