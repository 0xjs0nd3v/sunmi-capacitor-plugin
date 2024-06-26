package com.veridata.plugins.sunmi;

import android.content.Context;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

import com.getcapacitor.JSObject;
import com.getcapacitor.PluginCall;

import com.veridata.plugins.sunmi.helpers.CheckCardCallbackV2Wrapper;
import com.veridata.plugins.sunmi.utils.DeviceUtil;
import com.veridata.plugins.sunmi.utils.LogUtil;
import com.veridata.plugins.sunmi.utils.ThreadPoolUtil;

import com.sunmi.pay.hardware.aidl.AidlConstants;
import com.sunmi.pay.hardware.aidlv2.readcard.CheckCardCallbackV2;

public class SunmiCardReader {

    final int TIMEOUT = 120;
    protected String TAG_CARD_READER = "SunmiCardReader";
    private PluginCall capacitorCall;

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }

    private void checkCard(PluginCall call) {
        try {
            LogUtil.e(TAG_CARD_READER, "Checkcard Called ");

            this.capacitorCall = call;
            if (PaymentKernel.readCardOptV2 == null) {
                call.reject("SDK is not initialized ");
                this.capacitorCall = null;
                return;
            }
            PaymentKernel.readCardOptV2.checkCard(AidlConstants.CardType.MIFARE.getValue(), mReadCardCallback, TIMEOUT);
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
            LogUtil.e(TAG_CARD_READER, "findMagCard,bundle:" + bundle);
            response.put("bundle", bundle);

            capacitorCall.resolve(response);
        }

        @Override
        public void findICCard(String atr) throws RemoteException {
            JSObject response = new JSObject();
            LogUtil.e(TAG_CARD_READER, "findICCard, atr:" + atr);
            response.put("atr", atr);

            capacitorCall.resolve(response);
        }

        @Override
        public void findRFCard(String uuid) throws RemoteException {
            JSObject response = new JSObject();
            LogUtil.e(TAG_CARD_READER, "findRFCard, uuid:" + uuid);
            response.put("uuid", uuid);

            capacitorCall.resolve(response);
            LogUtil.e(TAG_CARD_READER, "Resolved to JS");
        }

        @Override
        public void onError(final int code, final String msg) throws RemoteException {
            LogUtil.e(TAG_CARD_READER, "check card error,code:" + code + " message:" + msg);

            capacitorCall.errorCallback(msg);
            if (code == -30005) {
                LogUtil.e(TAG_CARD_READER, "check card timeout");
                ThreadPoolUtil.executeInSinglePool(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            ThreadPoolUtil.wait(1000);
                            PaymentKernel.readCardOptV2.cancelCheckCard();
                            PaymentKernel.readCardOptV2.checkCard(AidlConstants.CardType.MIFARE.getValue(), mReadCardCallback, TIMEOUT);
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    };

    public void readCard (PluginCall call) {
        call.setKeepAlive(true);
        checkCard(call);
    }

    public void closeCardReader (PluginCall call) {
        try {
            if (this.capacitorCall == null) {
                call.reject("Card reader is not open already");
            } else {
                PaymentKernel.readCardOptV2.cancelCheckCard();
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
}
