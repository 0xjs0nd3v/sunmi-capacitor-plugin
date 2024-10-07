package com.veridata.plugins.sunmi;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.text.TextUtils;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.AidlErrorCodeV2;
import com.sunmi.pay.hardware.aidlv2.bean.PinPadConfigV2;
import com.sunmi.pay.hardware.aidlv2.pinpad.PinPadListenerV2;
import com.veridata.plugins.sunmi.helpers.CheckCardCallbackV2Wrapper;
import com.veridata.plugins.sunmi.helpers.PinPadListenerV2Wrapper;
import com.veridata.plugins.sunmi.utils.ByteUtil;
import com.veridata.plugins.sunmi.utils.LogUtil;
import com.veridata.plugins.sunmi.utils.SettingUtil;
import com.veridata.plugins.sunmi.utils.ThreadPoolUtil;


import com.sunmi.pay.hardware.aidl.bean.CardInfo;
import com.sunmi.pay.hardware.aidlv2.AidlConstantsV2;
import com.sunmi.pay.hardware.aidlv2.bean.EMVCandidateV2;
import com.sunmi.pay.hardware.aidlv2.emv.EMVListenerV2;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;

import com.veridata.plugins.sunmi.utils.DeviceUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SunmiModules {
    final int TIMEOUT = 60;
    protected String TAG = "SunmiCardReader";
    private PluginCall capacitorCall;

    private int mCardType;  // card type
    private String mCardNo; // card number
    private int mPinType;   // 0-online pin, 1-offline pin
    private String mCertInfo;

    private int mAppSelect = 0;

    private int mProcessStep;

    private Map<String, String> configMap;

    private static final int EMV_APP_SELECT = 1;
    private static final int EMV_FINAL_APP_SELECT = 2;
    private static final int EMV_CONFIRM_CARD_NO = 3;
    private static final int EMV_CERT_VERIFY = 4;
    private static final int EMV_SHOW_PIN_PAD = 5;
    private static final int EMV_ONLINE_PROCESS = 6;
    private static final int EMV_SIGNATURE = 7;
    private static final int EMV_TRANS_SUCCESS = 888;
    private static final int EMV_TRANS_FAIL = 999;
    private static final int REMOVE_CARD = 1000;
    private static final int EMV_RETURN_CARD_NUMBER = 0;

    private static final int PIN_CLICK_NUMBER = 50;
    private static final int PIN_CLICK_PIN = 51;
    private static final int PIN_CLICK_CONFIRM = 52;
    private static final int PIN_CLICK_CANCEL = 53;
    private static final int PIN_ERROR = 54;

    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case EMV_FINAL_APP_SELECT:
//                    importFinalAppSelectStatus(0);
                    break;
                case EMV_CONFIRM_CARD_NO:

                    break;
                case EMV_CERT_VERIFY:

                    break;
                case EMV_SHOW_PIN_PAD:
                    initPinPad();
                    break;
                case EMV_ONLINE_PROCESS:
//                    mockRequestToServer();

                    // skip/failed
                    importOnlineProcessStatus(-1);
                    break;
                case EMV_SIGNATURE:
//                    importSignatureStatus(0);

                    // skip/failed
                    importSignatureStatus(1);
                    break;
                case PIN_CLICK_NUMBER:
                    break;
                case PIN_CLICK_PIN:
//                    importPinInputStatus(0);
                    break;
                case PIN_CLICK_CONFIRM:
                    importPinInputStatus(2);
                    break;
                case PIN_CLICK_CANCEL:
                    importPinInputStatus(1);
                    break;
                case PIN_ERROR:
                    importPinInputStatus(3);
                    break;
                case EMV_TRANS_FAIL:
                    mProcessStep = 0;
                    break;
                case EMV_TRANS_SUCCESS:
//                    checkAndRemoveCard();
                    break;
                case REMOVE_CARD:
                    checkAndRemoveCard();
                    break;
                case EMV_RETURN_CARD_NUMBER:
                    checkAndRemoveCard();
                    JSObject response = new JSObject();
                    response.put("track1", "");
                    response.put("track2", "");
                    response.put("track3", "");
                    response.put("name", "");
                    response.put("pan", mCardNo);
                    response.put("panMasked", "");
                    capacitorCall.resolve(response);
                    break;
            }
        }
    };

    public Boolean initSunmiSDK (Context applicationContext) {
        SunmiPayKernel.initPayKernel(applicationContext);
        initData();
        return true;
    }

    /**
     * Set tlv essential tlv data
     */
    private void initEmvTlvData() {
        try {
            // set PayPass(MasterCard) tlv data
            String[] tagsPayPass = {"DF8117", "DF8118", "DF8119", "DF811F", "DF811E", "DF812C",
                    "DF8123", "DF8124", "DF8125", "DF8126",
                    "DF811B", "DF811D", "DF8122", "DF8120", "DF8121"};
            String[] valuesPayPass = {"E0", "F8", "F8", "E8", "00", "00",
                    "000000000000", "000000100000", "999999999999", "000000100000",
                    "30", "02", "0000000000", "000000000000", "000000000000"};
            SunmiPayKernel.emvOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, tagsPayPass, valuesPayPass);

            // set AMEX(AmericanExpress) tlv data
            String[] tagsAE = {"9F6D", "9F6E", "9F33", "9F35", "DF8168", "DF8167", "DF8169", "DF8170"};
            String[] valuesAE = {"C0", "D8E00000", "E0E888", "22", "00", "00", "00", "60"};
            SunmiPayKernel.emvOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_AE, tagsAE, valuesAE);

            String[] tagsJCB = {"9F53", "DF8161"};
            String[] valuesJCB = {"708000", "7F00"};
            SunmiPayKernel.emvOptV2.setTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_JCB, tagsJCB, valuesJCB);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }


    public void readCard (PluginCall call) {
        String cardType = call.getString("cardType");
        LogUtil.e(TAG, "cardType is " + cardType);
        call.setKeepAlive(true);
        mCardNo = null;
        if (cardType.equals("All")) {
            checkCard(call);
        } else if (cardType.equals("NFC")) {
            mCardType = AidlConstantsV2.CardType.NFC.getValue();
            checkNFCCard(call);
        } else if (cardType.equals("MAGNETIC")) {
            mCardType = AidlConstantsV2.CardType.MAGNETIC.getValue();
            checkMagneticCard(call);
        } else if (cardType.equals("IC")) {
            mCardType = AidlConstantsV2.CardType.IC.getValue();
            checkICCard(call);
        } else {
            call.reject("Card type not found");
        }
    }

    public void closeCardReader (PluginCall call) {
        try {
            if (this.capacitorCall == null) {
                call.reject("Card reader is not open already");
            } else {
                if (mCardType == AidlConstantsV2.CardType.NFC.getValue()) {
                    SunmiPayKernel.readCardOptV2.cardOff(AidlConstantsV2.CardType.NFC.getValue());
                    SunmiPayKernel.readCardOptV2.cancelCheckCard();
                } else if (mCardType == AidlConstantsV2.CardType.IC.getValue()) {
                    SunmiPayKernel.readCardOptV2.cardOff(AidlConstantsV2.CardType.IC.getValue());
                    SunmiPayKernel.readCardOptV2.cancelCheckCard();
                } else {
                    SunmiPayKernel.readCardOptV2.cancelCheckCard();
                }
                call.resolve();
            }
        } catch (RemoteException e) {
            call.reject("Can not cancel check card");
            LogUtil.e("RemoteException while canceling the check card", e.getMessage());
        }
    }

    public JSObject getDeviceModel () {
        JSObject objectResponse = new JSObject();
        objectResponse.put("model", DeviceUtil.getModel());
        objectResponse.put("isP2", DeviceUtil.isP2());
        objectResponse.put("isP1N", DeviceUtil.isP1N());
        objectResponse.put("isP2Lite", DeviceUtil.isP2Lite());
        objectResponse.put("isP2Pro", DeviceUtil.isP2Pro());
        objectResponse.put("isP14G", DeviceUtil.isP14G());
        return objectResponse;
    }

    public JSObject getSysParam (String key) throws RemoteException {
        String result = SunmiPayKernel.basicOptV2.getSysParam(key);
        JSObject objectResponse = new JSObject();
        objectResponse.put("result", result);
        return objectResponse;
    }

    private void checkCard(PluginCall call) {
        try {
            LogUtil.e(TAG, "Checkcard Called ");

            this.capacitorCall = call;
            if (SunmiPayKernel.readCardOptV2 == null) {
                call.reject("SDK is not initialized ");
                this.capacitorCall = null;
                return;
            }
            SunmiPayKernel.emvOptV2.initEmvProcess();
            initEmvTlvData();
            int cardType =  AidlConstantsV2.CardType.IC.getValue() | AidlConstantsV2.CardType.NFC.getValue();
            SunmiPayKernel.readCardOptV2.checkCard(cardType, mReadCardCallback, TIMEOUT);
        } catch (RemoteException e) {
            call.reject(e.getMessage());
            this.capacitorCall = null;
            e.printStackTrace();
        }
    }

    private void checkNFCCard(PluginCall call) {
        try {
            LogUtil.e(TAG, "Checkcard Called ");

            this.capacitorCall = call;
            if (SunmiPayKernel.readCardOptV2 == null) {
                call.reject("SDK is not initialized ");
                this.capacitorCall = null;
                return;
            }
            SunmiPayKernel.emvOptV2.initEmvProcess();
            initEmvTlvData();
            SunmiPayKernel.readCardOptV2.checkCard(AidlConstants.CardType.NFC.getValue(), mReadCardCallback, TIMEOUT);
        } catch (RemoteException e) {
            call.reject(e.getMessage());
            this.capacitorCall = null;
            e.printStackTrace();
        }
    }

    private void checkMagneticCard(PluginCall call) {
        try {
            LogUtil.e(TAG, "Checkcard Called ");

            this.capacitorCall = call;
            if (SunmiPayKernel.readCardOptV2 == null) {
                call.reject("SDK is not initialized ");
                this.capacitorCall = null;
                return;
            }
            SunmiPayKernel.readCardOptV2.checkCard(AidlConstants.CardType.MAGNETIC.getValue(), mReadCardCallback, TIMEOUT);
        } catch (RemoteException e) {
            call.reject(e.getMessage());
            this.capacitorCall = null;
            e.printStackTrace();
        }
    }

    private void checkICCard(PluginCall call) {
        try {
            LogUtil.e(TAG, "Checkcard Called ");

            this.capacitorCall = call;
            if (SunmiPayKernel.readCardOptV2 == null) {
                call.reject("SDK is not initialized ");
                this.capacitorCall = null;
                return;
            }
            SunmiPayKernel.emvOptV2.initEmvProcess();
            initEmvTlvData();
            SunmiPayKernel.readCardOptV2.checkCard(AidlConstants.CardType.IC.getValue(), mReadCardCallback, TIMEOUT);
        } catch (RemoteException e) {
            call.reject(e.getMessage());
            this.capacitorCall = null;
            e.printStackTrace();
        }
    }

    private final CheckCardCallbackV2 mReadCardCallback = new CheckCardCallbackV2Wrapper() {
        @Override
        public void findMagCard(Bundle bundle) throws RemoteException {
            JSObject response = new JSObject();
            LogUtil.e(TAG, "findMagCard,bundle:" + bundle);
//            response.put("bundle", bundle);

            if (bundle != null) {
                String track1 = null2String(bundle.getString("TRACK1"));
                String track2 = null2String(bundle.getString("TRACK2"));
                String track3 = null2String(bundle.getString("TRACK3"));

                int code1 = bundle.getInt("track1ErrorCode");
                int code2 = bundle.getInt("track2ErrorCode");
                int code3 = bundle.getInt("track3ErrorCode");

                String name = bundle.getString("name");
                String panMasked = bundle.getString("pan");
                String expire = bundle.getString("expire");

                String[] track2Parts = track2.split("=");
                String pan = track2Parts[0];

                LogUtil.e(TAG, String.format(Locale.getDefault(),
                        "track1ErrorCode:%d,track1:%s\ntrack2ErrorCode:%d,track2:%s\ntrack3ErrorCode:%d,track3:%s",
                        code1, track1, code2, track2, code3, track3));

                response.put("track1", track1 + "");
                response.put("track2", track2 + "");
                response.put("track3", track3 + "");
                response.put("name", name + "");
                response.put("pan", pan + "");
                response.put("panMasked", panMasked + "");
                response.put("expire", expire + "");
            } else {
                response.put("track1", "");
                response.put("track2", "");
                response.put("track3", "");
                response.put("name", "");
                response.put("pan", "");
                response.put("panMasked", "");
            }
            capacitorCall.resolve(response);
        }

        @Override
        public void findICCard(String atr) throws RemoteException {
//            JSObject response = new JSObject();
            LogUtil.e(TAG, "findICCard, atr:" + atr);
//            response.put("atr", atr);
//            capacitorCall.resolve(response);
            //IC card Beep buzzer when check card success
            SunmiPayKernel.basicOptV2.buzzerOnDevice(1, 2750, 200, 0);
            mCardType = AidlConstantsV2.CardType.IC.getValue();
            transactProcess();
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
//            JSObject response = new JSObject();
            LogUtil.e(TAG, "findRFCard, uuid:" + uuid);
//            response.put("uuid", uuid);
//            capacitorCall.resolve(response);
//            LogUtil.e(TAG, "Resolved to JS");
            mCardType = AidlConstantsV2.CardType.NFC.getValue();
            transactProcess();
        }

        @Override
        public void onError(final int code, final String msg) throws RemoteException {
            LogUtil.e(TAG, "check card error,code:" + code + " message:" + msg);

            capacitorCall.errorCallback(msg);

            if (code == -30005) {
                LogUtil.e(TAG, "check card timeout");
            }

            try {
                if (mCardType == AidlConstantsV2.CardType.NFC.getValue()) {
                    SunmiPayKernel.readCardOptV2.cardOff(AidlConstantsV2.CardType.NFC.getValue());
                    SunmiPayKernel.readCardOptV2.cancelCheckCard();
                } else if (mCardType == AidlConstantsV2.CardType.IC.getValue()) {
                    SunmiPayKernel.readCardOptV2.cardOff(AidlConstantsV2.CardType.IC.getValue());
                    SunmiPayKernel.readCardOptV2.cancelCheckCard();
                } else {
                    SunmiPayKernel.readCardOptV2.cancelCheckCard();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    };

    public static String null2String(String str) {
        return str == null ? "" : str;
    }

    /**
     * Do essential initialization, client App should
     * initialize their own data at this step.
     * Note: this method just show how to initialize data before
     * the emv process, the initialized data may not useful for your
     * App. Please init your own data.
     */
    private void initData() {
        // disable check card buzzer
        SettingUtil.setBuzzerEnable(false);
        // Client should config their own countryCode,capability,etc...
        configMap = EmvUtil.getConfig(EmvUtil.COUNTRY_PHILIPPINES);
        ThreadPoolUtil.executeInCachePool(
                () -> {
                    EmvUtil.initKey();
                    EmvUtil.initAidAndRid();
                    EmvUtil.setTerminalParam(configMap);
                }
        );
    }

    /**
     * Start emv transact process
     */
    private void transactProcess() {
        LogUtil.e(Constant.TAG, "transactProcess");
        try {
            Bundle bundle = new Bundle();
            bundle.putString("amount", "0");
            bundle.putString("transType", "00");
            //flowType:0x01-emv standard, 0x04：NFC-Speedup
            //Note:(1) flowType=0x04 only valid for QPBOC,PayPass,PayWave contactless transaction
            //     (2) set fowType=0x04, only EMVListenerV2.onRequestShowPinPad(),
            //         EMVListenerV2.onCardDataExchangeComplete() and EMVListenerV2.onTransResult() may will be called.
            if (mCardType == AidlConstantsV2.CardType.NFC.getValue()) {
                bundle.putInt("flowType", AidlConstantsV2.EMV.FlowType.TYPE_NFC_SPEEDUP);
            } else {
                bundle.putInt("flowType", AidlConstantsV2.EMV.FlowType.TYPE_EMV_STANDARD);
            }
            bundle.putInt("cardType", mCardType);
//            bundle.putBoolean("preProcessCompleted", false);
//            bundle.putInt("emvAuthLevel", 0);
//            addTransactionStartTimes();
            SunmiPayKernel.emvOptV2.transactProcessEx(bundle, mEMVListener);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * EMV process callback
     */
    private final EMVListenerV2 mEMVListener = new EMVListenerV2.Stub() {
        /**
         * Notify client to do multi App selection, this method may called when card have more than one Application
         * <br/> For Contactless and flowType set as AidlConstants.FlowType.TYPE_NFC_SPEEDUP, this
         * method will not be called
         *
         * @param appNameList   The App list for selection
         * @param isFirstSelect is first time selection
         */
        @Override
        public void onWaitAppSelect(List<EMVCandidateV2> appNameList, boolean isFirstSelect) throws RemoteException {
            LogUtil.e(Constant.TAG, "onWaitAppSelect isFirstSelect:" + isFirstSelect);
            mProcessStep = EMV_APP_SELECT;
            String[] candidateNames = getCandidateNames(appNameList);
            mHandler.obtainMessage(EMV_APP_SELECT, candidateNames).sendToTarget();
        }

        /**
         * Notify client the final selected Application
         * <br/> For Contactless and flowType set as AidlConstants.FlowType.TYPE_NFC_SPEEDUP, this
         * method will not be called
         *
         * @param tag9F06Value The final selected Application id
         */
        @Override
        public void onAppFinalSelect(String tag9F06Value) throws RemoteException {
            LogUtil.e(Constant.TAG, "onAppFinalSelect tag9F06Value:" + tag9F06Value);
            if (tag9F06Value != null && tag9F06Value.length() > 0) {
                boolean isUnionPay = tag9F06Value.startsWith("A000000333");
                boolean isVisa = tag9F06Value.startsWith("A000000003");
                boolean isMaster = tag9F06Value.startsWith("A000000004")
                        || tag9F06Value.startsWith("A000000005");
                boolean isAmericanExpress = tag9F06Value.startsWith("A000000025");
                boolean isJCB = tag9F06Value.startsWith("A000000065");
                boolean isRupay = tag9F06Value.startsWith("A000000524");
                boolean isPure = tag9F06Value.startsWith("D999999999")
                        || tag9F06Value.startsWith("D888888888")
                        || tag9F06Value.startsWith("D777777777")
                        || tag9F06Value.startsWith("D666666666")
                        || tag9F06Value.startsWith("A000000615");
                String paymentType = "unknown";
                if (isUnionPay) {
                    paymentType = "UnionPay";
                    mAppSelect = 0;
                } else if (isVisa) {
                    paymentType = "Visa";
                    mAppSelect = 1;
                } else if (isMaster) {
                    paymentType = "MasterCard";
                    mAppSelect = 2;
                } else if (isAmericanExpress) {
                    paymentType = "AmericanExpress";
                } else if (isJCB) {
                    paymentType = "JCB";
                } else if (isRupay) {
                    paymentType = "Rupay";
                } else if (isPure) {
                    paymentType = "Pure";
                }
                LogUtil.e(Constant.TAG, "detect " + paymentType + " card");
            }
            mProcessStep = EMV_FINAL_APP_SELECT;
            mHandler.obtainMessage(EMV_FINAL_APP_SELECT, tag9F06Value).sendToTarget();
        }

        /**
         * Notify client to confirm card number
         * <br/> For Contactless and flowType set as AidlConstants.FlowType.TYPE_NFC_SPEEDUP, this
         * method will not be called
         *
         * @param cardNo The card number
         */
        @Override
        public void onConfirmCardNo(String cardNo) throws RemoteException {
            LogUtil.e(Constant.TAG, "onConfirmCardNo cardNo:" + cardNo);
            mCardNo = cardNo;
            mProcessStep = EMV_CONFIRM_CARD_NO;
            mHandler.obtainMessage(EMV_CONFIRM_CARD_NO).sendToTarget();
//            importCardNoStatus(0);
        }

        /**
         * Notify client to input PIN
         *
         * @param pinType    The PIN type, 0-online PIN，1-offline PIN
         * @param remainTime The the remain retry times of offline PIN, for online PIN, this param
         *                   value is always -1, and if this is the first time to input PIN, value
         *                   is -1 too.
         */
        @Override
        public void onRequestShowPinPad(int pinType, int remainTime) throws RemoteException {
            LogUtil.e(Constant.TAG, "onRequestShowPinPad pinType:" + pinType + " remainTime:" + remainTime);
            mPinType = pinType;
            if (mCardNo == null) {
                mCardNo = getCardNo();
            }
            mProcessStep = EMV_SHOW_PIN_PAD;
            mHandler.obtainMessage(EMV_SHOW_PIN_PAD).sendToTarget();
        }

        /**
         * Notify  client to do signature
         */
        @Override
        public void onRequestSignature() throws RemoteException {
            LogUtil.e(Constant.TAG, "onRequestSignature");
            mProcessStep = EMV_SIGNATURE;
            mHandler.obtainMessage(EMV_SIGNATURE).sendToTarget();
        }

        /**
         * Notify client to do certificate verification
         *
         * @param certType The certificate type, refer to AidlConstants.CertType
         * @param certInfo The certificate info
         */
        @Override
        public void onCertVerify(int certType, String certInfo) throws RemoteException {
            LogUtil.e(Constant.TAG, "onCertVerify certType:" + certType + " certInfo:" + certInfo);
            mCertInfo = certInfo;
            mProcessStep = EMV_CERT_VERIFY;
            mHandler.obtainMessage(EMV_CERT_VERIFY).sendToTarget();
        }

        /**
         * Notify client to do online process
         */
        @Override
        public void onOnlineProc() throws RemoteException {
            LogUtil.e(Constant.TAG, "onOnlineProcess");
            mProcessStep = EMV_ONLINE_PROCESS;
            mHandler.obtainMessage(EMV_ONLINE_PROCESS).sendToTarget();
        }

        /**
         * Notify client EMV kernel and card data exchange finished, client can remove card
         */
        @Override
        public void onCardDataExchangeComplete() throws RemoteException {
            LogUtil.e(Constant.TAG, "onCardDataExchangeComplete");
            if (mCardType == AidlConstantsV2.CardType.NFC.getValue()) {
                //NFC card Beep buzzer to notify remove card
                SunmiPayKernel.basicOptV2.buzzerOnDevice(1, 2750, 200, 0);
            }
        }

        /**
         * Notify client EMV process ended
         *
         * @param code The transaction result code, 0-success, 1-offline approval, 2-offline denial,
         *             4-try again, other value-error code
         * @param desc The corresponding message of this code
         */
        @Override
        public void onTransResult(int code, String desc) throws RemoteException {
            if (mCardNo == null) {
                mCardNo = getCardNo();
            }
            LogUtil.e(Constant.TAG, "mCardNo: " + mCardNo);
            LogUtil.e(Constant.TAG, "onTransResult code:" + code + " desc:" + desc);
            LogUtil.e(Constant.TAG, "***************************************************************");
            LogUtil.e(Constant.TAG, "****************************End Process************************");
            LogUtil.e(Constant.TAG, "***************************************************************");
            if (code == 0) {
                mHandler.obtainMessage(EMV_TRANS_SUCCESS, code, code, desc).sendToTarget();
            } else {
                mHandler.obtainMessage(EMV_TRANS_FAIL, code, code, desc).sendToTarget();
            }

            // return card number
            mProcessStep = EMV_RETURN_CARD_NUMBER;
            mHandler.obtainMessage(EMV_RETURN_CARD_NUMBER).sendToTarget();
        }

        /**
         * Notify client the confirmation code verified(See phone)
         */
        @Override
        public void onConfirmationCodeVerified() throws RemoteException {
            LogUtil.e(Constant.TAG, "onConfirmationCodeVerified");

            byte[] outData = new byte[512];
            int len = SunmiPayKernel.emvOptV2.getTlv(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, "DF8129", outData);
            if (len > 0) {
                byte[] data = new byte[len];
                System.arraycopy(outData, 0, data, 0, len);
                String hexStr = ByteUtil.bytes2HexStr(data);
                LogUtil.e(Constant.TAG, "DF8129: " + hexStr);
            }
            // card off
            SunmiPayKernel.readCardOptV2.cardOff(mCardType);
        }

        /**
         * Notify client to exchange data
         * <br/> This method only used for Russia MIR
         *
         * @param cardNo The card number
         */
        @Override
        public void onRequestDataExchange(String cardNo) throws RemoteException {
            LogUtil.e(Constant.TAG, "onRequestDataExchange,cardNo:" + cardNo);
            SunmiPayKernel.emvOptV2.importDataExchangeStatus(0);
        }

        @Override
        public void onTermRiskManagement() throws RemoteException {
            LogUtil.e(Constant.TAG, "onTermRiskManagement");
            SunmiPayKernel.emvOptV2.importTermRiskManagementStatus(0);
        }

//        @Override
//        public void onPreFirstGenAC() throws RemoteException {
//            LogUtil.e(Constant.TAG, "onPreFirstGenAC");
//            SunmiPayKernel.emvOptV2.importPreFirstGenACStatus(0);
//        }
//
//        @Override
//        public void onDataStorageProc(String[] containerID, String[] containerContent) throws RemoteException {
//            LogUtil.e(Constant.TAG, "onDataStorageProc,");
//            //此回调为Dpas2.0专用
//            //根据需求配置tag及values
//            String[] tags = new String[0];
//            String[] values = new String[0];
//            SunmiPayKernel.emvOptV2.importDataStorage(tags, values);
//        }

    };

    /** getCard number */
    private String getCardNo() {
        LogUtil.e(Constant.TAG, "getCardNo");
        try {
            String[] tagList = {"57", "5A"};
            byte[] outData = new byte[256];
            int len = SunmiPayKernel.emvOptV2.getTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL, tagList, outData);
            if (len <= 0) {
                LogUtil.e(Constant.TAG, "getCardNo error,code:" + len);
                return "";
            }
            byte[] bytes = Arrays.copyOf(outData, len);
            Map<String, TLV> tlvMap = TLVUtil.buildTLVMap(bytes);
            if (!TextUtils.isEmpty(Objects.requireNonNull(tlvMap.get("57")).getValue())) {
                TLV tlv57 = tlvMap.get("57");
                CardInfo cardInfo = parseTrack2(tlv57.getValue());
                return cardInfo.cardNo;
            }
            if (!TextUtils.isEmpty(Objects.requireNonNull(tlvMap.get("5A")).getValue())) {
                return Objects.requireNonNull(tlvMap.get("5A")).getValue();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return "";
    }

    /**
     * Parse track2 data
     */
    public static CardInfo parseTrack2(String track2) {
        LogUtil.e(Constant.TAG, "track2:" + track2);
        String track_2 = stringFilter(track2);
        int index = track_2.indexOf("=");
        if (index == -1) {
            index = track_2.indexOf("D");
        }
        CardInfo cardInfo = new CardInfo();
        if (index == -1) {
            return cardInfo;
        }
        String cardNumber = "";
        if (track_2.length() > index) {
            cardNumber = track_2.substring(0, index);
        }
        String expiryDate = "";
        if (track_2.length() > index + 5) {
            expiryDate = track_2.substring(index + 1, index + 5);
        }
        String serviceCode = "";
        if (track_2.length() > index + 8) {
            serviceCode = track_2.substring(index + 5, index + 8);
        }
        LogUtil.e(Constant.TAG, "cardNumber:" + cardNumber + " expireDate:" + expiryDate + " serviceCode:" + serviceCode);
        cardInfo.cardNo = cardNumber;
        cardInfo.expireDate = expiryDate;
        cardInfo.serviceCode = serviceCode;
        return cardInfo;
    }

    /**
     * remove characters not number,=,D
     */
    static String stringFilter(String str) {
        String regEx = "[^0-9=D]";
        Pattern p = Pattern.compile(regEx);
        Matcher matcher = p.matcher(str);
        return matcher.replaceAll("").trim();
    }

    /**
     * Start show PinPad
     */
    private void initPinPad() {
        LogUtil.e(Constant.TAG, "initPinPad");
        try {
            PinPadConfigV2 pinPadConfig = new PinPadConfigV2();
            pinPadConfig.setPinPadType(0);
            pinPadConfig.setPinType(mPinType);
            pinPadConfig.setOrderNumKey(false);
            byte[] panBytes = mCardNo.substring(mCardNo.length() - 13, mCardNo.length() - 1).getBytes("US-ASCII");
            pinPadConfig.setPan(panBytes);
            pinPadConfig.setTimeout(60 * 1000); // input password timeout
            pinPadConfig.setPinKeyIndex(12);    // pik index
            pinPadConfig.setMaxInput(12);
            pinPadConfig.setMinInput(0);
            pinPadConfig.setKeySystem(0);
            pinPadConfig.setAlgorithmType(0);
            SunmiPayKernel.pinPadOptV2.initPinPad(pinPadConfig, mPinPadListener);

            // skip/failed - input PIN timeout
            importPinInputStatus(4);
        } catch (Exception e) {
            e.printStackTrace();
            mHandler.obtainMessage(PIN_ERROR, Integer.MIN_VALUE, Integer.MIN_VALUE, "initPinPad() failure").sendToTarget();
        }
    }

    /**
     * Input pin callback
     */
    private final PinPadListenerV2 mPinPadListener = new PinPadListenerV2Wrapper()
    {

        @Override
        public void onPinLength(int len) {
            LogUtil.e(Constant.TAG, "onPinLength:" + len);
            mHandler.obtainMessage(PIN_CLICK_NUMBER, len).sendToTarget();
        }

        @Override
        public void onConfirm(int i, byte[] pinBlock) {
            if (pinBlock != null) {
                String hexStr = ByteUtil.bytes2HexStr(pinBlock);
                LogUtil.e(Constant.TAG, "onConfirm pin block:" + hexStr);
                mHandler.obtainMessage(PIN_CLICK_PIN, pinBlock).sendToTarget();
            } else {
                mHandler.obtainMessage(PIN_CLICK_CONFIRM).sendToTarget();
            }
        }

        @Override
        public void onCancel() {
            LogUtil.e(Constant.TAG, "onCancel");
            mHandler.obtainMessage(PIN_CLICK_CANCEL).sendToTarget();
        }

        @Override
        public void onError(int code) {
            LogUtil.e(Constant.TAG, "onError:" + code);
            String msg = AidlErrorCodeV2.valueOf(code).getMsg();
            mHandler.obtainMessage(PIN_ERROR, code, code, msg).sendToTarget();
        }
    };

    /**
     * Notify emv process the Application select result
     *
     * @param selectIndex the index of selected App, start from 0
     */
    private void importAppSelect(int selectIndex) {
        LogUtil.e(Constant.TAG, "importAppSelect selectIndex:" + selectIndex);
        try {
            SunmiPayKernel.emvOptV2.importAppSelect(selectIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify emv process the final Application select result
     *
     * @param status 0:success, other value:failed
     */
    private void importFinalAppSelectStatus(int status) {
        try {
            LogUtil.e(Constant.TAG, "importFinalAppSelectStatus status:" + status);
            SunmiPayKernel.emvOptV2.importAppFinalSelectStatus(status);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify emv process the card number confirm status
     *
     * @param status 0:success, other value:failed
     */
    private void importCardNoStatus(int status) {
        LogUtil.e(Constant.TAG, "importCardNoStatus status:" + status);
        try {
            SunmiPayKernel.emvOptV2.importCardNoStatus(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify emv process the certification verify status
     *
     * @param status 0:success, other value:failed
     */
    private void importCertStatus(int status) {
        LogUtil.e(Constant.TAG, "importCertStatus status:" + status);
        try {
            SunmiPayKernel.emvOptV2.importCertStatus(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Notify emv process the PIN input result
     *
     * @param inputResult 0:success,1:input PIN canceled,2:input PIN skipped,3:PINPAD problem,4:input PIN timeout
     */
    private void importPinInputStatus(int inputResult) {
        LogUtil.e(Constant.TAG, "importPinInputStatus:" + inputResult);
        try {
            SunmiPayKernel.emvOptV2.importPinInputStatus(mPinType, inputResult);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Import signature result to emv process
     *
     * @param status 0:success, other value:failed
     */
    private void importSignatureStatus(int status) {
        LogUtil.e(Constant.TAG, "importSignatureStatus status:" + status);
        try {
            SunmiPayKernel.emvOptV2.importSignatureStatus(status);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Import online process result data(eg: field 55 ) to emv process.
     * if no date to import, set param tags and values as empty array
     *
     * @param status 0:online approval, 1:online denial, 2:online failed
     */
    private void importOnlineProcessStatus(int status) {
        LogUtil.e(Constant.TAG, "importOnlineProcessStatus status:" + status);
        try {
            String[] tags = {"71", "72", "91", "8A", "89"};
            String[] values = {"", "", "", "", ""};
            byte[] out = new byte[1024];
            int len = SunmiPayKernel.emvOptV2.importOnlineProcStatus(status, tags, values, out);
            if (len < 0) {
                LogUtil.e(Constant.TAG, "importOnlineProcessStatus error,code:" + len);
            } else {
                byte[] bytes = Arrays.copyOf(out, len);
                String hexStr = ByteUtil.bytes2HexStr(bytes);
                LogUtil.e(Constant.TAG, "importOnlineProcessStatus outData:" + hexStr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Mock a POSP to do some data exchange(online process), we don't have a really POSP,
     * client should connect to a really POSP at this step.
     */
    private void mockRequestToServer() {
        new Thread(() -> {
            try {
                if (AidlConstantsV2.CardType.MAGNETIC.getValue() != mCardType) {
                    getTlvData();
                }
                Thread.sleep(1000);
                // notice  ==  import the online result to SDK and end the process.
                importOnlineProcessStatus(0);
            } catch (Exception e) {
                e.printStackTrace();
                importOnlineProcessStatus(-1);
            }
        }).start();
    }

    /**
     * Read we interested tlv data
     */
    private void getTlvData() {
        try {
            String[] tagList = {
                    "DF02", "5F34", "9F06", "FF30", "FF31", "95", "9B", "9F36", "9F26",
                    "9F27", "DF31", "5A", "57", "5F24", "9F1A", "9F33", "9F35", "9F40",
                    "9F03", "9F10", "9F37", "9C", "9A", "9F02", "5F2A", "82", "9F34", "9F1E",
                    "84", "4F", "9F66", "9F6C", "9F09", "9F41", "9F63", "5F20", "9F12", "50",
            };
            byte[] outData = new byte[2048];
            Map<String, TLV> map = new TreeMap<>();
            int tlvOpCode;
            if (AidlConstantsV2.CardType.NFC.getValue() == mCardType) {
                if (mAppSelect == 2) {
                    tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS;
                } else if (mAppSelect == 1) {
                    tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_PAYWAVE;
                } else {
                    tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL;
                }
            } else {
                tlvOpCode = AidlConstantsV2.EMV.TLVOpCode.OP_NORMAL;
            }
            int len = SunmiPayKernel.emvOptV2.getTlvList(tlvOpCode, tagList, outData);
            if (len > 0) {
                byte[] bytes = Arrays.copyOf(outData, len);
                String hexStr = ByteUtil.bytes2HexStr(bytes);
                Map<String, TLV> tlvMap = TLVUtil.buildTLVMap(hexStr);
                map.putAll(tlvMap);
            }

            // payPassTags
            String[] payPassTags = {
                    "DF811E", "DF812C", "DF8118", "DF8119", "DF811F", "DF8117", "DF8124",
                    "DF8125", "9F6D", "DF811B", "9F53", "DF810C", "9F1D", "DF8130", "DF812D",
                    "DF811C", "DF811D", "9F7C",
            };
            len = SunmiPayKernel.emvOptV2.getTlvList(AidlConstantsV2.EMV.TLVOpCode.OP_PAYPASS, payPassTags, outData);
            if (len > 0) {
                byte[] bytes = Arrays.copyOf(outData, len);
                String hexStr = ByteUtil.bytes2HexStr(bytes);
                Map<String, TLV> tlvMap = TLVUtil.buildTLVMap(hexStr);
                map.putAll(tlvMap);
            }

            final StringBuilder sb = new StringBuilder();
            Set<String> keySet = map.keySet();
            for (String key : keySet) {
                TLV tlv = map.get(key);
                sb.append(key);
                sb.append(":");
                if (tlv != null) {
                    String value = tlv.getValue();
                    sb.append(value);
                }
                sb.append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Create Candidate names
     */
    private String[] getCandidateNames(List<EMVCandidateV2> candiList) {
        if (candiList == null || candiList.size() == 0) return new String[0];
        String[] result = new String[candiList.size()];
        for (int i = 0; i < candiList.size(); i++) {
            EMVCandidateV2 candi = candiList.get(i);
            String name = candi.appPreName;
            name = TextUtils.isEmpty(name) ? candi.appLabel : name;
            name = TextUtils.isEmpty(name) ? candi.appName : name;
            name = TextUtils.isEmpty(name) ? "" : name;
            result[i] = name;
            LogUtil.e(Constant.TAG, "EMVCandidateV2: " + name);
        }
        return result;
    }

    /** Check and notify remove card */
    private void checkAndRemoveCard() {
        try {
            int status = SunmiPayKernel.readCardOptV2.getCardExistStatus(mCardType);
            if (status < 0) {
                LogUtil.e(Constant.TAG, "getCardExistStatus error, code:" + status);
                return;
            }
            if (status == AidlConstantsV2.CardExistStatus.CARD_PRESENT) {
                SunmiPayKernel.basicOptV2.buzzerOnDevice(1, 2750, 200, 0);
                mHandler.sendEmptyMessageDelayed(REMOVE_CARD, 1000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
