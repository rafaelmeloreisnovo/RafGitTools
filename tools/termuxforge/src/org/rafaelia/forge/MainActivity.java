package org.rafaelia.forge;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends Activity {
    static { System.loadLibrary("rafaelia_bridge"); }

    public native String rafaelia_seal();
    public native int phiFromCH(int c_q16, int h_q16);
    public native boolean periodGuard(int n);

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        int c = 0x8000;
        int h = 0x4000;
        int phi = phiFromCH(c, h);

        TextView tv = new TextView(this);
        tv.setText("seal=" + rafaelia_seal()
                + "\nphi_q16=" + phi
                + "\nperiod42=" + periodGuard(84));
        setContentView(tv);
    }
}
