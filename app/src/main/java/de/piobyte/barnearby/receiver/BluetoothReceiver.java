package de.piobyte.barnearby.receiver;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

public class BluetoothReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (TextUtils.equals(BluetoothAdapter.ACTION_STATE_CHANGED, action)) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            switch (state) {
                case BluetoothAdapter.STATE_TURNING_ON:
                    Log.i("Bluetooth", "Turning on...");
                    break;
                case BluetoothAdapter.STATE_ON:
                    Log.i("Bluetooth", "On");
                    break;
                case BluetoothAdapter.STATE_TURNING_OFF:
                    Log.i("Bluetooth", "Turning off...");
                    break;
                case BluetoothAdapter.STATE_OFF:
                    Log.i("Bluetooth", "Off");
                    break;
            }
        }
    }
}