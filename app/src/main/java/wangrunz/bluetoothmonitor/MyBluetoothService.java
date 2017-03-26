package wangrunz.bluetoothmonitor;

import android.app.Service;
import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAssignedNumbers;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothHealth;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MyBluetoothService extends Service {
    private static final String TAG = "MyBluetoothService";
    private final IBinder mBinder = new MyServiceBinder();
    private MyBluetoothBackgroundReceiver mMyBluetoothBackgroundReceiver;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothHeadset mBluetoothHeadset;
    private BluetoothA2dp mBluetoothA2dp;
    private BluetoothHealth mBluetoothHealth;
    private BluetoothProfile.ServiceListener mProfileListener;
    private BluetoothGatt mBluetoothGatt;
    BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //super.onConnectionStateChange(gatt, status, newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d("GATT", "services Connected");
                List<BluetoothGattService> gattServices = mBluetoothGatt.getServices();
                if (gattServices == null) {
                    Log.d("GATT", "currently no services");
                } else {
                    int count = gattServices.size();
                    Log.d("GATT", count + " services");
                    for (BluetoothGattService gattService : gattServices) {
                        Log.d("GATT", "Service:" + Long.toHexString(gattService.getUuid().getMostSignificantBits()).substring(0, 4));
                    }
                }
                Log.d("GATT", "dodiscoverServices:" + mBluetoothGatt.discoverServices());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //super.onServicesDiscovered(gatt, status);
            Log.d("GATT", "services discovered" + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            UUID uuid = characteristic.getUuid();
            Log.d("GATT characteristic", uuid.toString());
        }
    };

    public MyBluetoothService() {
    }

    public void connectGatt(Context context, BluetoothDevice device) {
        mBluetoothGatt = device.connectGatt(context, false, mGattCallback);
    }

    public void closeGatt() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public Set<BluetoothDevice> getConnectedDevices() {
        Set<BluetoothDevice> connectedDevices = new HashSet<>();
        if (mBluetoothA2dp != null) {
            connectedDevices.addAll(mBluetoothA2dp.getConnectedDevices());
        }
        if (mBluetoothHeadset != null) {
            connectedDevices.addAll(mBluetoothHeadset.getConnectedDevices());
        }
        if (mBluetoothHealth != null) {
            connectedDevices.addAll(mBluetoothHealth.getConnectedDevices());
        }
        return connectedDevices;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "service activated");
        mMyBluetoothBackgroundReceiver = new MyBluetoothBackgroundReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY + '.' + BluetoothAssignedNumbers.APPLE);
        intentFilter.addCategory(Intent.CATEGORY_DEFAULT);
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
        registerReceiver(mMyBluetoothBackgroundReceiver, intentFilter);
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        mProfileListener = new BluetoothProfile.ServiceListener() {
            @Override
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    mBluetoothHeadset = (BluetoothHeadset) proxy;
                } else if (profile == BluetoothProfile.A2DP) {
                    mBluetoothA2dp = (BluetoothA2dp) proxy;
                } else if (profile == BluetoothProfile.HEALTH) {
                    mBluetoothHealth = (BluetoothHealth) proxy;
                }
            }

            @Override
            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    mBluetoothHeadset = null;
                } else if (profile == BluetoothProfile.A2DP) {
                    mBluetoothA2dp = null;
                } else if (profile == BluetoothProfile.HEALTH) {
                    mBluetoothHealth = null;
                }
            }
        };


        if (!mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET)) {
            Log.d(TAG, "bluetooth health profile not available");
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "service onstart");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mMyBluetoothBackgroundReceiver);
        Log.d(TAG, "service done");
        super.onDestroy();
    }

    public class MyServiceBinder extends Binder {
        MyBluetoothService getService() {
            return MyBluetoothService.this;
        }
    }
}
