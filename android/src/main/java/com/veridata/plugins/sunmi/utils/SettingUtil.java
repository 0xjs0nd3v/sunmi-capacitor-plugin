package com.veridata.plugins.sunmi.utils;

import android.text.TextUtils;

import com.veridata.plugins.sunmi.Constant;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.veridata.plugins.sunmi.SunmiPayKernel;

import org.json.JSONObject;

public final class SettingUtil {
    private static final String TAG = Constant.TAG;
    private static final String KEY_BUZZER = "buzzer";
    private static final String KEY_SUPPORT_KEY_PARTITION = "supportKeyPartition";
    private static final String KEY_PSAM_CHANNEL = "psamChannel";
    private static final String KEY_AUTO_RESTORE_NFC = "autoRestoreNfc";

    private static final String KEY_MAX_APP_SELECT_TIME = "maxAppSelectTime";
    private static final String KEY_MAX_APP_FINAL_SELECT_TIME = "maxAppFinalSelectTime";
    private static final String KEY_MAX_CONFIRM_CARD_NO_TIME = "maxConfirmCardNoTime";
    private static final String KEY_MAX_INPUT_PIN_TIME = "maxInputPinTime";
    private static final String KEY_MAX_SIGNATURE_TIME = "maxSignatureTime";
    private static final String KEY_MAX_CERT_VERIFY_TIME = "maxCertVerifyTime";
    private static final String KEY_MAX_ONLINE_TIME = "maxOnlineTime";
    private static final String KEY_MAX_DATA_EXCHANGE_TIME = "maxDataExchangeTime";
    private static final String KEY_TERM_RISK_MANAGEMENT = "termRiskManagement";
    private static final String KEY_MAX_TERM_RISK_TIME = "maxTermRiskTime";
    private static final String KEY_PRE_FIRST_GEN_AC = "preFirstGenAC";
    private static final String KEY_MAX_PRE_FIRST_GEN_AC_TIME = "maxPreFirstGenACTime";

    private static final String KEY_ISOLATE_M1_CPU = "isolateM1AndCPU";
    private static final String KEY_CARD_POLL_INTERVAL_TIME = "cardPollIntervalTime";
    private static final int DEFAULT_CHANNEL_COUNT = 2;
    private static final String KEY_MAG_CARD_ROUND_POLL_TIMES = "magCardRoundPollTimes";


    private SettingUtil() {
        throw new AssertionError("Create instance of SettingUtil is prohibited");
    }

    /** 获取是否支持密钥分区 */
    public static boolean getSupportKeyPartition() {
        try {
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                JSONObject jobj = new JSONObject(jsonStr);
                if (jobj.has(KEY_SUPPORT_KEY_PARTITION)) {
                    LogUtil.e(TAG, "has keypartition");
                    return jobj.getBoolean(KEY_SUPPORT_KEY_PARTITION);
                }
            }
            if (DeviceUtil.isP1N() || DeviceUtil.isP14G()) {//P1N/P14G默认不支持分区
                return false;
            }
            return true;
        } catch (Exception e) {
            LogUtil.e(TAG, "SettingUtil getSupportKeyPartition:" + e);
            e.printStackTrace();
        }
        return true;
    }

    /** 设置是否支持密钥分区 */
    public static void setSupportKeyPartition(boolean enable) {
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_SUPPORT_KEY_PARTITION, enable);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * V1S获取PSAM通道
     * <br/>V1S包含两个PSAM通道：通道1和通道2
     *
     * @return >0-当前PSAM通道，<0-无指定的通道
     */
    public static int getPSAMChannel() {
        int channel = -1;
        try {
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                JSONObject jobj = new JSONObject(jsonStr);
                if (jobj.has(KEY_PSAM_CHANNEL)) {
                    channel = jobj.getInt(KEY_PSAM_CHANNEL);
                    LogUtil.e(TAG, "has psam channel:" + channel);
                    return channel;
                }
            }
        } catch (Exception e) {
            LogUtil.e(TAG, "SettingUtil getPSAMChannel:" + e);
            e.printStackTrace();
        }
        return channel;
    }

    /**
     * V1S切换PSAM通道
     * <br/>V1S包含两个PSAM通道：通道1和通道2
     */
    public static void switchPSAMChannel() {
        try {
            int curChannel = -1;
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
                if (jobj.has(KEY_PSAM_CHANNEL)) {
                    LogUtil.e(TAG, "has keypartition");
                    curChannel = jobj.getInt(KEY_PSAM_CHANNEL);
                }
            }
            if (curChannel < 0) {
                curChannel = 1;
            } else if (curChannel < DEFAULT_CHANNEL_COUNT) {
                curChannel++;
            } else if (curChannel == DEFAULT_CHANNEL_COUNT) {
                curChannel = -1;
            }
            jobj.put(KEY_PSAM_CHANNEL, curChannel);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
            LogUtil.e(TAG, "switch psam channel to " + curChannel + " success");
        } catch (Exception e) {
            LogUtil.e(TAG, "SettingUtil getSupportKeyPartition:" + e);
            e.printStackTrace();
        }
    }

    /** 获取是否自动恢复NFC功能 （当连续读卡NFC功能异常时，是否自动恢复） */
    public static boolean getAutoRestoreNfc() {
        boolean autoRestoreNfc = false;
        try {
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (TextUtils.isEmpty(jsonStr)) {
                return false;
            }
            JSONObject jobj = new JSONObject(jsonStr);
            if (!jobj.has(KEY_AUTO_RESTORE_NFC)) {
                return false;
            }
            autoRestoreNfc = jobj.getBoolean(KEY_AUTO_RESTORE_NFC);
            LogUtil.e(TAG, "autoRestoreNfc:" + autoRestoreNfc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return autoRestoreNfc;
    }

    /**
     * Set whether auto restore the NFC function. Currently,this config is only used on V2Pro.
     * <br/> if set autoRestoreNfc as true, when NFC read card occurred exception,
     * SDK will try to restore NFC function by close NFC and reopen it.
     */
    public static void setAutoRestoreNfc(boolean enable) {
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_AUTO_RESTORE_NFC, enable);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set SDK built-in PinPad mode
     *
     * @param mode the PinPad mode, refer to {@link AidlConstantsV2.PinPadMode}
     * @return code >=0-success, <0-failed
     */
    public static int setPinPadMode(String mode) {
//        int code = -1;
//        try {
//            code = MyApplication.mBasicOptV2.setSysParam(AidlConstantsV2.SysParam.PINPAD_MODE, mode);
//            if (code < 0) {
//                LogUtil.e(TAG, "setPinPadMode failed,code:" + code);
//            }
//        } catch (RemoteException e) {
//            e.printStackTrace();
//        }
//        return code;
        return 0;
    }

    /**
     * Set EMV max app select time
     *
     * @param maxAppSelectTime max app select time, unit: s
     *                         <br/> (1) if set maxAppSelectTime<=60s, SDK use default value 60s.
     *                         <br/> (2) if set maxAppSelectTime>60, SDK use set value.
     *                         <br/>Note: The set maxAppSelectTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxAppSelectTime(int maxAppSelectTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_APP_SELECT_TIME, maxAppSelectTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max app final select time
     *
     * @param maxAppFinalSelectTime max app final select time, unit: s
     *                              <br/> (1) if set maxAppFinalSelectTime<=60s, SDK use default value 60s.
     *                              <br/> (2) if set maxAppFinalSelectTime>60, SDK use set value.
     *                              <br/>Note: The set maxAppFinalSelectTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxAppFinalSelectTime(int maxAppFinalSelectTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_APP_FINAL_SELECT_TIME, maxAppFinalSelectTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max confirm card NO. time
     *
     * @param maxConfirmCardNoTime max confirm card NO. time, unit: s
     *                             <br/> (1) if set maxConfirmCardNoTime<=60s, SDK use default value 60s.
     *                             <br/> (2) if set maxConfirmCardNoTime>60, SDK use set value.
     *                             <br/>Note: The set maxConfirmCardNoTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxConfirmCardNoTime(int maxConfirmCardNoTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_CONFIRM_CARD_NO_TIME, maxConfirmCardNoTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max input PIN time
     *
     * @param maxInputPinTime max input PIN time, unit: s
     *                        <br/> (1) if set maxInputPinTime<=60s, SDK use default value 60s.
     *                        <br/> (2) if set maxInputPinTime>60, SDK use set value.
     *                        <br/>Note: The set maxInputPinTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxInputPinTime(int maxInputPinTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_INPUT_PIN_TIME, maxInputPinTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max signature time
     *
     * @param maxSignatureTime max signature time, unit: s
     *                         <br/> (1) if set maxSignatureTime<=60s, SDK use default value 60s.
     *                         <br/> (2) if set maxSignatureTime>60, SDK use set value.
     *                         <br/>Note: The set maxSignatureTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxSignatureTime(int maxSignatureTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_SIGNATURE_TIME, maxSignatureTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max cert verify time
     *
     * @param maxCertVerifyTime max cert verify time, unit: s
     *                          <br/> (1) if set maxCertVerifyTime<=60s, SDK use default value 60s.
     *                          <br/> (2) if set maxCertVerifyTime>60, SDK use set value.
     *                          <br/>Note: The set maxCertVerifyTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxCertVerifyTime(int maxCertVerifyTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_CERT_VERIFY_TIME, maxCertVerifyTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max online tme
     *
     * @param maxOnlineTime max online time, unit: s
     *                      <br/> (1) if set maxOnlineTime<=60s, SDK use default value 60s.
     *                      <br/> (2) if set maxOnlineTime>60, SDK use set value.
     *                      <br/>Note: The set maxOnlineTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxOnlineTime(int maxOnlineTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_ONLINE_TIME, maxOnlineTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max data exchange tme
     *
     * @param maxDataExchangeTime max data exchange time, unit: s
     *                            <br/> (1) if set maxDataExchangeTime<=60s, SDK use default value 60s.
     *                            <br/> (2) if set maxDataExchangeTime>60, SDK use set value.
     *                            <br/>Note: The set maxDataExchangeTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxDataExchangeTime(int maxDataExchangeTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_DATA_EXCHANGE_TIME, maxDataExchangeTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV enable termRiskManagement callback
     */
    public static int setEmvEnableTermRiskManagementCallback(boolean enable) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_TERM_RISK_MANAGEMENT, enable);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max term risk tme
     *
     * @param maxTermRiskTime max term risk time, unit: s
     *                        <br/> (1) if set maxTermRiskTime<=60s, SDK use default value 60s.
     *                        <br/> (2) if set maxTermRiskTime>60, SDK use set value.
     *                        <br/>Note: The set maxTermRiskTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxTermRiskTime(int maxTermRiskTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_TERM_RISK_TIME, maxTermRiskTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV enable preFirstGenAC callback
     */
    public static int setEmvEnablePreFirstGenACCallback(boolean enable) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_PRE_FIRST_GEN_AC, enable);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Set EMV max term risk tme
     *
     * @param maxPreFirstGenACTime max pre first gen AC time, unit: s
     *                             <br/> (1) if set maxPreFirstGenACTime<=60s, SDK use default value 60s.
     *                             <br/> (2) if set maxPreFirstGenACTime>60, SDK use set value.
     *                             <br/>Note: The set maxPreFirstGenACTime should be a int value, not a String.
     * @return code >=0-success, <0-failed
     */
    public static int setEmvMaxPreFistGenACTime(int maxPreFirstGenACTime) {
        int code = -1;
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAX_PRE_FIRST_GEN_AC_TIME, maxPreFirstGenACTime);
            code = SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return code;
    }

    /**
     * Enable/Disable buzzer beep when check card success
     * <br/> SDK default action is beep the buzzer when check card success
     *
     * @param enable true-enable, false-disable
     */
    public static void setBuzzerEnable(boolean enable) {
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_BUZZER, enable ? 1 : 0);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get flag of distinguish M1 and CPU card when checking card
     *
     * @return true-Distinguish M1 and CPU(SDK default), false-not distinguish
     */
    public static boolean getIsolateM1AndCPU() {
        try {
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (TextUtils.isEmpty(jsonStr)) {
                return true;
            }
            JSONObject jobj = new JSONObject(jsonStr);
            if (!jobj.has(KEY_ISOLATE_M1_CPU)) {
                return true;
            }
            boolean value = jobj.getBoolean(KEY_ISOLATE_M1_CPU);
            LogUtil.e(TAG, "isolateM1AndCPU:" + value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Set flag of distinguish M1 and CPU card when checking card
     *
     * @param isolate true-Distinguish M1 and CPU, false- not distinguish
     */
    public static void setIsolateM1AndCPU(boolean isolate) {
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_ISOLATE_M1_CPU, isolate);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Set card polling interval time
     * SDK default interval time is 50ms
     *
     * @param time Interval time, unit:ms
     */
    public static void setCardPollIntervalTime(int time) {
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_CARD_POLL_INTERVAL_TIME, time);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get card polling interval time
     *
     * @return The interval time, unit:ms
     */
    public static int getCardPollIntervalTime() {
        int defaultValue = 50;
        try {
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (TextUtils.isEmpty(jsonStr)) {
                return defaultValue;
            }
            JSONObject jobj = new JSONObject(jsonStr);
            if (!jobj.has(KEY_CARD_POLL_INTERVAL_TIME)) {
                return defaultValue;
            }
            int value = jobj.getInt(KEY_CARD_POLL_INTERVAL_TIME);
            LogUtil.e(TAG, "cardPollIntervalTime:" + value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

    /**
     * Set magnetic card polling times in a polling round
     * SDK default polling times is 1
     *
     * @param times polling times
     */
    public static void setMagCardRoundPollTimes(int times) {
        try {
            JSONObject jobj = new JSONObject();
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (!TextUtils.isEmpty(jsonStr)) {
                jobj = new JSONObject(jsonStr);
            }
            jobj.put(KEY_MAG_CARD_ROUND_POLL_TIMES, times);
            SunmiPayKernel.basicOptV2.setSysParam(AidlConstantsV2.SysParam.RESERVED, jobj.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Get magnetic card polling times in a polling round
     *
     * @return The polling times, unit:ms
     */
    public static int getMagCardRoundPollTimes() {
        int defaultValue = 1;
        try {
            String jsonStr = SunmiPayKernel.basicOptV2.getSysParam(AidlConstantsV2.SysParam.RESERVED);
            if (TextUtils.isEmpty(jsonStr)) {
                return defaultValue;
            }
            JSONObject jobj = new JSONObject(jsonStr);
            if (!jobj.has(KEY_MAG_CARD_ROUND_POLL_TIMES)) {
                return defaultValue;
            }
            int value = jobj.getInt(KEY_MAG_CARD_ROUND_POLL_TIMES);
            LogUtil.e(TAG, "magCardRoundPollTimes:" + value);
            return value;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return defaultValue;
    }

}
