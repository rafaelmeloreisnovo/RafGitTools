package __PACKAGE__;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public final class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        TextView text = new TextView(this);
        text.setText("RAFANDROID JNI=" + NativeBridge.health());
        text.setTextSize(22f);
        text.setPadding(32, 32, 32, 32);
        setContentView(text);
    }
}
