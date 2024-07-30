package com.veridata.plugins.sunmi;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;
import com.veridata.plugins.sunmi.helpers.CheckCardCallbackV2Wrapper;
import com.veridata.plugins.sunmi.utils.LogUtil;
import com.veridata.plugins.sunmi.utils.ThreadPoolUtil;
import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;

import com.veridata.plugins.sunmi.utils.DeviceUtil;

import java.util.Locale;

public class SunmiModules {

    private static final int TDK_INDEX = 19;
    final int TIMEOUT = 120;
    protected String TAG = "SunmiCardReader";
    private PluginCall capacitorCall;

    public Boolean initSunmiSDK (Context applicationContext) {
        SunmiPayKernel.initPayKernel(applicationContext);
        return true;
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
//            Bundle bundle = new Bundle();
//            bundle.putInt("cardType", AidlConstants.CardType.MAGNETIC.getValue());
//            bundle.putInt("encKeySystem", AidlConstants.Security.SEC_MKSK);
//            bundle.putInt("encKeyIndex", TDK_INDEX);
//            bundle.putInt("encKeyAlgType", AidlConstants.Security.KEY_ALG_TYPE_3DES);
//            bundle.putInt("encMode", AidlConstants.Security.DATA_MODE_ECB);
//            bundle.putByteArray("encIv", new byte[16]);
//            bundle.putByte("encPaddingMode", (byte) 0);
//            bundle.putInt("encMaskStart", 6);
//            bundle.putInt("encMaskEnd", 4);
//            bundle.putChar("encMaskWord", '*');
//            bundle.putInt("ctrCode", 0);
//            bundle.putInt("stopOnError", 0);
//            SunmiPayKernel.readCardOptV2.checkCardEnc(bundle, mReadCardCallback, TIMEOUT);
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
            JSObject response = new JSObject();
            LogUtil.e(TAG, "findICCard, atr:" + atr);
            response.put("atr", atr);
            capacitorCall.resolve(response);
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
            JSObject response = new JSObject();
            LogUtil.e(TAG, "findRFCard, uuid:" + uuid);
            response.put("uuid", uuid);
            capacitorCall.resolve(response);
            LogUtil.e(TAG, "Resolved to JS");
        }

        @Override
        public void onError(final int code, final String msg) throws RemoteException {
            LogUtil.e(TAG, "check card error,code:" + code + " message:" + msg);

            capacitorCall.errorCallback(msg);
            if (code == -30005) {
                LogUtil.e(TAG, "check card timeout");
                ThreadPoolUtil.executeInSinglePool(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ThreadPoolUtil.wait(1000);
                            SunmiPayKernel.readCardOptV2.cancelCheckCard();
                            SunmiPayKernel.readCardOptV2.checkCard(AidlConstants.CardType.MIFARE.getValue(), mReadCardCallback, TIMEOUT);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    };

    public void readCard (PluginCall call) {
        String cardType = call.getString("cardType");
        LogUtil.e(TAG, "cardType is " + cardType);
        call.setKeepAlive(true);
        if (cardType.equals("NFC")) {
            checkNFCCard(call);
        } else if (cardType.equals("MAGNETIC")) {
            checkMagneticCard(call);
        } else if (cardType.equals("IC")) {
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
                SunmiPayKernel.readCardOptV2.cancelCheckCard();
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

    public static String null2String(String str) {
        return str == null ? "" : str;
    }
}
