package com.veridata.plugins.sunmi;

import android.util.Log;

public class SunmiPlugin {

    public String echo(String value) {
        Log.i("Echo", value);
        return value;
    }
}
