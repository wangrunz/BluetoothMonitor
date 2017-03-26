package wangrunz.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyBluetoothBackgroundReceiver extends BroadcastReceiver {
    private static final String TAG = "BackgroundReceiver";
    BluetoothAdapter mBluetoothAdapter;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive: " + action);
        if (action.equals(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)) {
            //not working
            String command = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
            int type = intent.getIntExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE, -1);
            Log.d(TAG, "onReceive: " + command + " type: " + type);
        } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            Log.d(TAG, device.getName() + " status changed: " + state);
        }
    }
}
