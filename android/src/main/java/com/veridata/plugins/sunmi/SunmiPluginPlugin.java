package com.veridata.plugins.sunmi;

import android.content.Context;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import com.veridata.plugins.sunmi.utils.LogUtil;

@CapacitorPlugin(name = "SunmiPlugin")
public class SunmiPluginPlugin extends Plugin {

    private SunmiCardReader sunmiCardReader = new SunmiCardReader();

    @PluginMethod
    public void echo(PluginCall call) {
        String value = call.getString("value");

        JSObject ret = new JSObject();
        ret.put("value", sunmiCardReader.echo(value));
        call.resolve(ret);
    }

    @PluginMethod
    public void initSunmiSDK (PluginCall call) {
        try {
            Context applicationContext = this.getActivity().getApplicationContext();
            PaymentKernel.initPayKernel(applicationContext);

            LogUtil.i("SunmiSDK is connected? ", PaymentKernel.isConnected() + "");

            if (!PaymentKernel.isConnected()) {
                throw new RuntimeException("InitPayKernel failed");
            }
            call.resolve();
        } catch (Exception e) {
            call.reject("Can not initialize payment kernel");
        }
    }

    @PluginMethod(returnType = PluginMethod.RETURN_CALLBACK)
    public void readCard (PluginCall call) {
        sunmiCardReader.readCard(call);
    }

    @PluginMethod
    public void closeCardReader (PluginCall call) {
        sunmiCardReader.closeCardReader(call);
    }

    @PluginMethod
    public void getDeviceModel (PluginCall call) {
        try {
            JSObject response = sunmiCardReader.getDeviceModel();
            call.resolve(response);
        } catch (Exception e) {
            LogUtil.e("Getting Device Model ", e.getMessage());
            call.reject("Can not get the device model", e);
        }
    }
}
