package com.veridata.plugins.sunmi;

import android.content.Context;

public class InitSunmiSDK {

    public Boolean load (Context applicationContext) {
        PaymentKernel.initPayKernel(applicationContext);
        return true;
    }
}
