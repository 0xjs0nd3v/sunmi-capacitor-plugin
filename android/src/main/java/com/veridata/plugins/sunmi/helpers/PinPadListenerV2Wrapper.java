package com.veridata.plugins.sunmi.helpers;

import android.os.RemoteException;

import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2;

public class PinPadListenerV2Wrapper extends PinPadListenerV2.Stub {
    /**
     * 回调当前输密位数，如果按退格，返回length为0
     *
     * @param length 当前输PIN长度
     */
    @Override
    public void onPinLength(int length) throws RemoteException {

    }

    /**
     * 输PIN确认
     *
     * @param type PIN类型：0-联机PIN，1-脱机PIN
     */
    @Override
    public void onConfirm(int type, byte[] pinBlock) throws RemoteException {

    }

    /**
     * 输PIN取消
     */
    @Override
    public void onCancel() throws RemoteException {

    }

    /**
     * 输PIN出错
     *
     * @param errCode 错误码
     */
    @Override
    public void onError(int errCode) throws RemoteException {

    }

    /**
     * 视障模式手指滑过按键
     *
     * @param event             事件类型：
     *                          4-视障模式-触碰到ENTER键
     *                          5-视障模式-触碰到CLEAR键
     *                          6-视障模式-触碰到CANCEL键
     *                          7-视障模式-触碰到无效键值区域（已废弃）
     *                          8-视障模式-当前输入PIN长度已达上限
     *                          9-视障模式-触碰到数字键，需要播放beep
     *                          10-视障模式-手指触碰上方无效区域
     *                          11-视障模式-手指触碰下方无效区域，暂不支持
     *                          12-视障模式-手指触碰左侧无效区域
     *                          13-视障模式-手指触碰右侧无效区域
     * @param data，与event相关的数据： event=5，data[0]=已输入的PIN长度
     *                          event!=5，data值为null，无效值
     */
//    @Override
//    public void onHover(int event, byte[] data) throws RemoteException {
//
//    }
}
