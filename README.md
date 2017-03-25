# BluetoothMonitor
This is a simple Bluetooth Monitor app which shows the status, type and MAC of nearby devices.

Support both Bluetooth and Bluetooth LE devices.

Functions did not complete yet, any help will be pleasured.

## Need to implement
### Battery level for bluetooth headsets support HandsFree profile

Based on [Bluetooth Accessory Design Guidelines for Apple Products](https://developer.apple.com/hardwaredrivers/BluetoothDesignGuidelines.pdf), we could get battery level by listening Apple-specific AT Command.

>#### Bluetooth Headset Battery Level Indication
>Any Hands-Free Bluetooth headset accessory can show its battery level to the user as an indicator icon in the iOS device status bar. This feature is supported on all iOS devices that support the Hands-Free Profile
>##### Apple-specific HFP Command AT+IPHONEACCEV
>Description: Reports a headset state change.
>Initiator: Headset accessory
>Format: AT+IPHONEACCEV=Number of key/value pairs,key1 ,val1 ,key2 ,val2 ,...
>Parameters:
>- Number of key/value pairs: The number of parameters coming next.
>- key : the type of change being reported:
>- 1 = Battery Level
>- 2 = Dock State
>- val : the value of the change:
>- Battery Level: string value between '0' and '9'
>- Dock State: 0 = undocked, 1 = docked
>##### Example: AT+IPHONEACCEV=1,1,3
And based on [Bluetooth | Android Developer](https://developer.android.com/guide/topics/connectivity/bluetooth.html), to handle vendor-specific AT commands for headset, we need to create a broadcast receiver for the [ACTION_VENDOR_SPECIFIC_HEADSET_EVENT](https://developer.android.com/reference/android/bluetooth/BluetoothHeadset.html#ACTION_VENDOR_SPECIFIC_HEADSET_EVENT).
    
>#### Vendor-specific AT commands
>Starting in Android 3.0 (API level 11), applications can register to receive system broadcasts of pre-defined vendor-specific AT commands sent by headsets (such as a Plantronics +XEVENT command). For example, an application could receive broadcasts that indicate a connected device's battery level and could notify the user or take other action as needed. Create a broadcast receiver for the ACTION_VENDOR_SPECIFIC_HEADSET_EVENT intent to handle vendor-specific AT commands for the headset.

##### Issue
Broadcast Receiver never be fired by ACTION_VENDOR_SPECIFIC_HEADSET_EVENT

Service class:
```Java
mMyBluetoothBackgroundReceiver = new MyBluetoothBackgroundReceiver();
IntentFilter intentFilter = new IntentFilter();
intentFilter.addAction(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT);
intentFilter.addCategory(BluetoothHeadset.VENDOR_SPECIFIC_HEADSET_EVENT_COMPANY_ID_CATEGORY+'.'+BluetoothAssignedNumbers.APPLE);
registerReceiver(mMyBluetoothBackgroundReceiver,intentFilter);
```
BroadcastReceiver class:
```Java
public void onReceive(Context context, Intent intent) {
  String action = intent.getAction();
  Log.d(TAG,"onReceive: "+action);
  if (action.equals(BluetoothHeadset.ACTION_VENDOR_SPECIFIC_HEADSET_EVENT)){
    //not working
    String command = intent.getStringExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD);
    int type = intent.getIntExtra(BluetoothHeadset.EXTRA_VENDOR_SPECIFIC_HEADSET_EVENT_CMD_TYPE,-1);
    Log.d(TAG, "onReceive: "+command+" type: "+type);
  }
}
```
    


### Battery level for bluetooth LE device ([GATT battery service](https://www.bluetooth.com/specifications/gatt/viewer?attributeXmlFile=org.bluetooth.service.battery_service.xml)) profile

Based on [Bluetooth Low Energy | Android Developer](https://developer.android.com/guide/topics/connectivity/bluetooth-le.html), to read the characteristics from GATT server (remote bluetooth LE device) should follow steps:
1. Request Bluetooth Permissions
2. Set Up BluetoothAdapter
3. Find BLE Devices
4. Connect to a GATT Server
5. Read BLE Attributes
6. Close the Client App

##### Issue
At step 4 `BluetoothGattCallBack.onServicesDiscovered()` never fired. 
```Java
BluetoothGattCallback mGattCallback = new BluetoothGattCallback(){
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_CONNECTED){
            Log.d("GATT","services Connected");
            Log.d("GATT","do discoverServices:"+mBluetoothGatt.discoverServices());
        }
    }
    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        Log.d("GATT","services discovered"+status);
    }
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        UUID uuid = characteristic.getUuid();
        Log.d("GATT characteristic",uuid.toString());
    }
};
public void connectGatt(Context context,BluetoothDevice device){
    mBluetoothGatt = device.connectGatt(context,false,mGattCallback);
}
```
`Log.d("GATT","services Connected")` and `Log.d("GATT","do discoverServices:"+mBluetoothGatt.discoverServices())` are hitted correctly and return value of `mBluetoothGatt.discoverServices()` is `true`
