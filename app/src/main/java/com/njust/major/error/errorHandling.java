package com.njust.major.error;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;


import com.njust.major.bean.MachineState;
import com.njust.major.bean.Malfunction;
import com.njust.major.bean.Transaction;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.MalfunctionDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.MalfunctionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.util.Util;


import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.njust.VMApplication.OutGoodsThreadFlag;
import static com.njust.VMApplication.current_transaction_order_number;

public class errorHandling {
	/**
	 * 错误处理
	 * 说明：根据动作应答的上行数据进行错误处理
	 * @param module 组件，是指某一中心执行机构（电机）及相关传感器、反馈开关或一组功能相近部件的组合。
	 * @param rec 九字节的组件应答数据
	 * */
	public errorHandling(Context context,int counter, byte module, byte[] rec) {
		MachineStateDao machineStateDao = new MachineStateDaoImpl(context);
		MalfunctionDao malfunctionDao = new MalfunctionDaoImpl(context);
		TransactionDao transactionDao = new TransactionDaoImpl(context);
		Malfunction malfunction = new Malfunction();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
		Transaction transaction = transactionDao.queryLastedTransaction();
		byte error1[];
		byte error2[];
		String Counter;
		if(counter == 0){
			Counter = "中柜";
			machineStateDao.updateState(1);
		}else{
			Counter = counter == 1? "左柜":"右柜";
			if(counter == 1){
				machineStateDao.updateCounterState(1,0);
			}else {
				machineStateDao.updateCounterState(0,1);
			}
		}
		switch (module) {
            /*字符“Y”，表示驱动Y轴电机的应答*/
			case 0x59: {
				String log = Counter + "检测到错误：";
				error1 = byteTo8Byte(rec[2]);
				if (error1[0] != (byte)0x00) {
					log += "已执行动作，";
				} else {
					log += "未执行动作，";
				}
				if(error1[1] == (byte)0x01){
					log += "Y轴电机过流，";
				}
				if(error1[2] == (byte)0x01){
					log += "Y轴电机断路，";
				}
				if(error1[3] == (byte)0x01){
					log += "Y轴上止点开关故障，";
				}
				if(error1[4] == (byte)0x01){
					log += "Y轴下止点开关故障，";
				}
				if(error1[5] == (byte)0x01){
					log += "Y轴电机超时，";
				}
				if(error1[6] == (byte)0x01){
					log += "Y轴码盘故障，";
				}
				if(error1[7] == (byte)0x01){
					log += "Y轴出货门定位开关故障，";
				}
				log += "停止出货。";
				malfunction.setTransactionID(current_transaction_order_number);
				String time = sdf.format(Calendar.getInstance().getTime());
				malfunction.setErrorTime(time);
				malfunction.setCounter(counter);
				malfunction.setErrorModule(Counter+"Y轴电机");
				malfunction.setErrorDescription(log);
				malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
				malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
				malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
				malfunctionDao.addMalfunction(malfunction);
				log += " Y轴电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
						+" Y轴电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
						+" Y轴电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
				Log.w("happy", ""+log);
				Util.WriteFile(log);
				break;
			}
            /*字符“P”，表示驱动货道电机的应答*/
			case 0x50: {
				String log = Counter + "检测到错误：";
				error1 = byteTo8Byte(rec[2]);
				if (error1[0] != (byte)0x00) {
					log += "已执行动作，";
				} else {
					log += "未执行动作，";
				}
				if(error1[1] == (byte)0x01){
					log += "货道电机过流，";
				}
				if(error1[2] == (byte)0x01){
					log += "货道电机断路，";
				}
				if(error1[3] == (byte)0x01){
					log += "货道超时（无货物输出的超时），";
				}
				if(error1[4] == (byte)0x01){
					log += "商品超时（货物遮挡光栅超时），";
				}
				if(error1[5] == (byte)0x01){
					log += "弹簧电机1反馈开关故障，";
				}
				if(error1[6] == (byte)0x01){
					log += "弹簧电机2反馈开关故障，";
				}
				if(error1[7] == (byte)0x01){
					log += "货道下货光栅故障，";
				}
				log += "停止出货。";
				malfunction.setTransactionID(current_transaction_order_number);
				String time = sdf.format(Calendar.getInstance().getTime());
				malfunction.setErrorTime(time);
				malfunction.setCounter(counter);
				malfunction.setErrorModule(Counter+"货道电机");
				malfunction.setErrorDescription(log);
				malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
				malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
				malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
				malfunctionDao.addMalfunction(malfunction);
				log += " 货道电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
						+" 货道电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
						+" 货道电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
				Log.w("happy", ""+log);
				Util.WriteFile(log);
				break;
			}
            /*字符“X”，表示驱动X轴电机的应答*/
			case 0x58: {
				String log = Counter + "检测到错误：";
				error1 = byteTo8Byte(rec[2]);
				if (error1[0] != (byte)0x00) {
					log += "已执行动作，";
				} else {
					log += "未执行动作，";
				}
				if(error1[1] == (byte)0x01){
					log += "X轴电机过流，";
				}
				if(error1[2] == (byte)0x01){
					log += "X轴电机断路，";
				}
				if(error1[3] == (byte)0x01){
					log += "X轴出货光栅未检测到货物，";
				}
				if(error1[4] == (byte)0x01){
					log += "X轴出货光栅货物遮挡超时，";
				}
				if(error1[5] == (byte)0x01){
					log += "X轴出货光栅故障，";
				}
				if(error1[6] == (byte)0x01){
					log += "X轴电机超时，";
				}
				log += "停止出货。";
				malfunction.setTransactionID(current_transaction_order_number);
				String time = sdf.format(Calendar.getInstance().getTime());
				malfunction.setErrorTime(time);
				malfunction.setCounter(counter);
				malfunction.setErrorModule(Counter+"X轴电机");
				malfunction.setErrorDescription(log);
				malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
				malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
				malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
				malfunctionDao.addMalfunction(malfunction);
				log += " 货道电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
						+" 货道电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
						+" 货道电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
				Log.w("happy", ""+log);
				Util.WriteFile(log);
				break;
			}
            /*字符“Z”，表示驱动出货门电机的应答*/
			case 0x5A: {
				String log = Counter + "检测到错误：";
				error1 = byteTo8Byte(rec[2]);
				if (error1[0] != (byte)0x00) {
					log += "已执行动作，";
				} else {
					log += "未执行动作，";
				}
				if(error1[1] == (byte)0x01){
					log += "出货门电机过流，";
				}
				if(error1[2] == (byte)0x01){
					log += "出货门电机断路，";
				}
				if(error1[3] == (byte)0x01){
					log += "出货门前止点开关故障，";
				}
				if(error1[4] == (byte)0x01){
					log += "出货门后止点开关故障，";
				}
				if(error1[5] == (byte)0x01){
					log += "开、关出货门超时，";
				}
				if(error1[6] == (byte)0x01){
					log += "出货门半开、半关，";
				}
				log += "停止出货。";
				malfunction.setTransactionID(current_transaction_order_number);
				String time = sdf.format(Calendar.getInstance().getTime());
				malfunction.setErrorTime(time);
				malfunction.setCounter(counter);
				malfunction.setErrorModule(Counter+"出货门电机");
				malfunction.setErrorDescription(log);
				malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
				malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
				malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
				malfunctionDao.addMalfunction(malfunction);
				log += " 出货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
						+" 出货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
						+" 出货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
				Log.w("happy", ""+log);
				Util.WriteFile(log);
				break;
			}
            /*字符“M”，表示驱动取货门电机的应答*/
			case 0x4D: {
				String log = Counter + "检测到错误：";
				error1 = byteTo8Byte(rec[2]);
				error2 = byteTo8Byte(rec[1]);
				if (error1[0] != (byte)0x00) {
					log += "已执行动作，";
				} else {
					log += "未执行动作，";
				}
				if(error1[1] == (byte)0x01){
					log += "取货门电机过流，";
				}
				if(error1[2] == (byte)0x01){
					log += "取货门电机断路，";
				}
				if(error1[3] == (byte)0x01){
					log += "取货门上止点开关故障，";
				}
				if(error1[4] == (byte)0x01){
					log += "取货门下止点开关故障，";
				}
				if(error1[5] == (byte)0x01){
					log += "开、关取货门超时，";
				}
				if(error1[6] == (byte)0x01){
					log += "取货门半开、半关，";
				}
				if(error1[7] == (byte)0x01){
					log += "取货仓光栅检测无货物，";
				}
				if(error2[0] == (byte)0x01){
					log += "取货仓光栅检测有货物，";
				}
				if(error2[1] == (byte)0x01){
					log += "取货篮光栅故障，";
				}
				if(error2[2] == (byte)0x01){
					log += "防夹手光栅检测到遮挡物，";
				}
				if(error2[3] == (byte)0x01){
					log += "防夹手光栅故障，";
				}
				log += "停止出货。";
				malfunction.setTransactionID(current_transaction_order_number);
				String time = sdf.format(Calendar.getInstance().getTime());
				malfunction.setErrorTime(time);
				malfunction.setCounter(counter);
				malfunction.setErrorModule(Counter+"取货门电机");
				malfunction.setErrorDescription(log);
				malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
				malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
				malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
				malfunctionDao.addMalfunction(malfunction);
				log += " 取货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
						+" 取货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
						+" 取货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
				Log.w("happy", ""+log);
				Util.WriteFile(log);
				break;
			}
            /*字符“F”，表示驱动落货门电机的应答*/
			case 0x46: {
				String log = Counter + "检测到错误：";
				error1 = byteTo8Byte(rec[2]);
				error2 = byteTo8Byte(rec[1]);
				if (error1[0] != (byte)0x00) {
					log += "已执行动作，";
				} else {
					log += "未执行动作，";
				}
				if(error1[1] == (byte)0x01){
					log += "落货门电机过流，";
				}
				if(error1[2] == (byte)0x01){
					log += "落货门电机断路，";
				}
				if(error1[3] == (byte)0x01){
					log += "落货门上止点开关故障，";
				}
				if(error1[4] == (byte)0x01){
					log += "落货门下止点开关故障，";
				}
				if(error1[5] == (byte)0x01){
					log += "开、关落货门超时，";
				}
				if(error1[6] == (byte)0x01){
					log += "落货门半开、半关，";
				}
				if(error1[7] == (byte)0x01){
					log += "落货仓光栅检测无货物，";
				}
				if(error2[0] == (byte)0x01){
					log += "落货仓光栅检测有货物，";
				}
				if(error2[1] == (byte)0x01){
					log += "落货篮光栅故障，";
				}
				log += "停止出货。";
				malfunction.setTransactionID(current_transaction_order_number);
				String time = sdf.format(Calendar.getInstance().getTime());
				malfunction.setErrorTime(time);
				malfunction.setCounter(counter);
				malfunction.setErrorModule(Counter+"落货门电机");
				malfunction.setErrorDescription(log);
				malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
				malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
				malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
				malfunctionDao.addMalfunction(malfunction);
				log += " 落货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
						+" 落货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
						+" 落货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
				Log.w("happy", ""+log);
				Util.WriteFile(log);
				break;
			}
			default:break;
		}
		transaction.setComplete(1);
		transaction.setError(1);
		transactionDao.updateTransaction(transaction);
		OutGoodsThreadFlag = false;
		SystemClock.sleep(20);
		Intent intent = new Intent();
		intent.setAction("njust_outgoods_complete");
		intent.putExtra("transaction_order_number", current_transaction_order_number);
		intent.putExtra("outgoods_status", "fail");
		context.sendBroadcast(intent);
	}

	/**
	 * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
	 * @param b 1个字节byte数据
	 * */
	public static byte[] byteTo8Byte(byte b) {
		byte[] array = new byte[8];
		for (int i = 0; i <= 7; i++) {
			array[i] = (byte)(b & 1);
			b = (byte) (b >> 1);
		}
		return array;
	}
}
