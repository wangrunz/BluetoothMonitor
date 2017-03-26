package wangrunz.bluetoothmonitor;

import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothDevice;
import android.os.ParcelUuid;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by wrz19 on 3/23/2017.
 */

public class MyRecyclerAdapter extends RecyclerView.Adapter {
    private List<BluetoothDevice> mBluetoothDevices = new ArrayList<>();
    private Set<BluetoothDevice> mConnectedDevices = new HashSet<>();

    public void updateConnectedDevice(Set<BluetoothDevice> connectedDevices) {
        mConnectedDevices = connectedDevices;
    }

    public void setmBluetoothDevices(Set<BluetoothDevice> bluetoothDevices) {
        mBluetoothDevices = new ArrayList<>(bluetoothDevices);
        notifyDataSetChanged();
    }

    public void addmBluetoothDevices(BluetoothDevice bluetoothDevice) {
        if (!mBluetoothDevices.contains(bluetoothDevice)) {
            String TAG2 = "Found device";
/*            if (bluetoothDevice.getName()==null){
                Log.d(TAG2,"Unknown");
            }
            else {
                Log.d(TAG2,bluetoothDevice.getName());
            }
            Log.d(TAG2,bluetoothDevice.getAddress());
            Log.d(TAG2,"DeviceClass: "+bluetoothDevice.getBluetoothClass().getDeviceClass()+" major: "+bluetoothDevice.getBluetoothClass().getMajorDeviceClass()+" type: "+bluetoothDevice.getType());
            if (bluetoothDevice.getUuids()!=null){
                for (ParcelUuid uuid : bluetoothDevice.getUuids()){
                    Log.d(TAG2,Long.toHexString(uuid.getUuid().getMostSignificantBits()).substring(0,4));
                }
            }*/
            mBluetoothDevices.add(bluetoothDevice);
            notifyItemInserted(getItemCount() - 1);
        }
    }

    public void clearmBluetoothDevices() {
        mBluetoothDevices.clear();
        notifyDataSetChanged();
    }

    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.bluetooth_device_view_holder, parent, false);
        BluetoothDeviceViewHolder bluetoothDeviceViewHolder = new BluetoothDeviceViewHolder(v);
        bluetoothDeviceViewHolder.textViewName = (TextView) v.findViewById(R.id.bluetooth_name);
        bluetoothDeviceViewHolder.textViewAddress = (TextView) v.findViewById(R.id.bluetooth_address);
        bluetoothDeviceViewHolder.textViewType = (TextView) v.findViewById(R.id.bluetooth_type);
        bluetoothDeviceViewHolder.imageViewIcon = (ImageView) v.findViewById(R.id.bluetooth_icon);
        bluetoothDeviceViewHolder.imageViewStatusIcon = (ImageView) v.findViewById(R.id.bluetooth_status_icon);
        return bluetoothDeviceViewHolder;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
        BluetoothDevice device = mBluetoothDevices.get(position);
        int bondState = device.getBondState();
        int majorDeviceClass = device.getBluetoothClass().getMajorDeviceClass();
        int deviceClass = device.getBluetoothClass().getDeviceClass();


        BluetoothDeviceViewHolder bluetoothDeviceViewHolder = (BluetoothDeviceViewHolder) holder;

        //Device name
        if (device.getName() != null) {
            bluetoothDeviceViewHolder.textViewName.setText(device.getName());
        } else {
            bluetoothDeviceViewHolder.textViewName.setText(R.string.unknown_device);
        }

        //Device address
        bluetoothDeviceViewHolder.textViewAddress.setText(device.getAddress());

        //Device status
        if (mConnectedDevices.contains(device)) {
            bluetoothDeviceViewHolder.imageViewStatusIcon.setImageResource(R.drawable.ic_bluetooth_connect_black_24dp);
        } else if (bondState == BluetoothDevice.BOND_BONDED) {
            bluetoothDeviceViewHolder.imageViewStatusIcon.setImageResource(R.drawable.ic_bluetooth_grey600_24dp);
        } else {
            bluetoothDeviceViewHolder.imageViewStatusIcon.setImageResource(android.R.color.transparent);
        }

        //Device Type
        switch (device.getType()) {
            case BluetoothDevice.DEVICE_TYPE_CLASSIC:
                bluetoothDeviceViewHolder.textViewType.setText(R.string.bluetooth_device_type_classic);
                break;
            case BluetoothDevice.DEVICE_TYPE_DUAL:
                bluetoothDeviceViewHolder.textViewType.setText(R.string.bluetooth_device_type_dual);
                break;
            case BluetoothDevice.DEVICE_TYPE_LE:
                bluetoothDeviceViewHolder.textViewType.setText(R.string.bluetooth_device_type_le);
                break;
            case BluetoothDevice.DEVICE_TYPE_UNKNOWN:
                bluetoothDeviceViewHolder.textViewType.setText(R.string.bluetooth_device_type_unknown);
                break;
        }

        //Device class
        if (device.getUuids() != null) {
            Set<String> uuids = new HashSet<>();
            for (ParcelUuid uuid : device.getUuids()) {
                uuids.add(Long.toHexString(uuid.getUuid().getMostSignificantBits()).substring(0, 4));
            }
            if (uuids.contains("1400")) {
                bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_local_hospital_black_24dp);
            } else if (uuids.contains("111e")) {
                bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_headset_mic_black_24dp);
            } else if (uuids.contains("1108")) {
                bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_headset_black_24dp);
            }
        }
        if (bluetoothDeviceViewHolder.imageViewIcon.getDrawable().getConstantState() == bluetoothDeviceViewHolder.itemView.getResources().getDrawable(R.drawable.ic_devices_other_black_24dp, null).getConstantState()) {
            switch (majorDeviceClass) {
                case BluetoothClass.Device.Major.AUDIO_VIDEO:
                    if (deviceClass == BluetoothClass.Device.AUDIO_VIDEO_HANDSFREE) {
                        bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_headset_mic_black_24dp);
                    } else {
                        bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_headset_black_24dp);
                    }
                    break;
                case BluetoothClass.Device.Major.COMPUTER:
                    bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_computer_black_24dp);
                    break;
                case BluetoothClass.Device.Major.HEALTH:
                    bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_local_hospital_black_24dp);
                    break;
                case BluetoothClass.Device.Major.IMAGING:

                    break;
                case BluetoothClass.Device.Major.MISC:

                    break;
                case BluetoothClass.Device.Major.NETWORKING:

                    break;
                case BluetoothClass.Device.Major.PERIPHERAL:

                    break;
                case BluetoothClass.Device.Major.PHONE:

                    break;
                case BluetoothClass.Device.Major.TOY:

                    break;
                case BluetoothClass.Device.Major.WEARABLE:
                    bluetoothDeviceViewHolder.imageViewIcon.setImageResource(R.drawable.ic_watch_black_24dp);
                    break;
                case BluetoothClass.Device.Major.UNCATEGORIZED:
                    break;
            }
        }

    }

    @Override
    public int getItemCount() {
        return mBluetoothDevices.size();
    }

    public class BluetoothDeviceViewHolder extends RecyclerView.ViewHolder {
        TextView textViewName;
        TextView textViewAddress;
        TextView textViewType;
        ImageView imageViewIcon;
        ImageView imageViewStatusIcon;

        public BluetoothDeviceViewHolder(View itemView) {
            super(itemView);
        }
    }
}
