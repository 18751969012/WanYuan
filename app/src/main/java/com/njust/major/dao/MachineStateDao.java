package com.njust.major.dao;

import com.njust.major.bean.MachineState;


public interface MachineStateDao {

    public  void updateSetting(int counter, int flootNo, int outPosition, String flootPositions);//初始配置更新到数据库

    public void updateTemperature(byte leftTempState, byte leftSetTemp, byte rightTempState, byte rightSetTemp);//更改设定的温控模式和温度,温控模式：0x00=制冷0x01=制热0x02=常温,-20～70

    public void updateLight(int leftLight, int rightLight, int midLight);//照明灯状态，0=关，1=开

    public void updateVersion(String version);//更新版本号

    public void updateState(int state);//更新整个机器的状态

    public void updateCounterState(int leftState, int rightState);//更新左右边柜的状态

    public void updateDoorState(int leftDoor, int rightDoor, int midDoorLock);//更新边柜门开关状态，以及中间门锁状态 0=关门，1=开门

    public void updateCounterDoorState(int leftDoorheat, int rightDoorheat);//更新门加热状态，0=关，1=开

    public void updateMachineState(MachineState machineState);//更新除设定位置相关的其他所有数据

    public MachineState queryMachineState();

}
