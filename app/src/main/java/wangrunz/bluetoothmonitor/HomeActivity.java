package wangrunz.bluetoothmonitor;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import java.util.Set;

public class HomeActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG_HOME = "home";
    private static final String TAG_ABOUT = "about";
    private static final String TAG_FEEDBACK = "feedback";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_CODE = 100;

    //
    private static final long SCAN_PERIOD = 15000;
    public static String CURRENT_TAG = TAG_HOME;
    //fragment tags
    private static int navItemIndex = 0;
    private FloatingActionButton fab;

    private BluetoothAdapter mBluetoothAdapter;

    private BroadcastReceiver mDiscoveryReceiver;
    private MyBluetoothService myBluetoothService;
    private boolean mIsBound = false;

    private Handler mHandler;
    private BluetoothLeScanner mBluetoothLeScanner;
    private boolean mBluetoothStatus;
    private ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();
            addToRecycler(device);
            if (device.getName() == null) {
                Log.d("LE device", "Unknown");
            } else {
                Log.d("LE device", device.getName());
            }
            Log.d("LE address", device.getAddress());
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d("LESCANNER", "fail:" + errorCode);
        }
    };
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            myBluetoothService = ((MyBluetoothService.MyServiceBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            myBluetoothService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        mHandler = new Handler();

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh();
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        if (savedInstanceState == null) {
            navItemIndex = 0;
            CURRENT_TAG = TAG_HOME;
            loadFragment();
        }

        /******************************************************/
        //bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        mDiscoveryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    Toast.makeText(context, "Scan started", Toast.LENGTH_SHORT).show();
                    setFabStatus(false);
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    setFabStatus(true);
                    Toast.makeText(context, "Scan finished", Toast.LENGTH_SHORT).show();
                } else if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    addToRecycler(device);
                } else if (action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                    refresh();
                } else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                    int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (state) {
                        case BluetoothAdapter.STATE_OFF:

                            toggleBluetooth(false);
                            break;
                        case BluetoothAdapter.STATE_ON:

                            toggleBluetooth(true);
                            break;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        registerReceiver(mDiscoveryReceiver, filter);

        if (mBluetoothAdapter == null) {
            //Device does not support Bluetooth
            Toast.makeText(this, R.string.bluetooth_not_support, Toast.LENGTH_SHORT).show();
            toggleBluetooth(false);
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                toggleBluetooth(true);
            } else {
                toggleBluetooth(false);
            }
        }

        startService(new Intent(this, MyBluetoothService.class));
        doBindService();
    }

    private void loadFragment() {
        if (getSupportFragmentManager().findFragmentByTag(CURRENT_TAG) != null) {
            return;
        }

        Runnable mPendingRunnable = new Runnable() {
            @Override
            public void run() {
                Fragment fragment = getFragment();
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out);
                fragmentTransaction.replace(R.id.content_home, fragment, CURRENT_TAG);
                fragmentTransaction.commitNowAllowingStateLoss();
            }
        };

        if (mPendingRunnable != null) {
            mHandler.post(mPendingRunnable);
        }
        toggleFab();
    }

    private void toggleFab() {
        if (CURRENT_TAG != TAG_HOME) {
            fab.hide();
        } else {
            fab.show();
        }
    }

    private Fragment getFragment() {
        if (CURRENT_TAG != TAG_HOME) {
            scanLeDevice(false);
            mBluetoothAdapter.cancelDiscovery();
        }
        switch (CURRENT_TAG) {
            case TAG_HOME:
                return new HomeFragment();
            case TAG_ABOUT:
                return new AboutFragment();
            case TAG_FEEDBACK:
                return new FeedbackFragment();
            default:
                return new HomeFragment();
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (CURRENT_TAG != TAG_HOME) {
                CURRENT_TAG = TAG_HOME;
                loadFragment();
                return;
            }
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.home, menu);
        if (mBluetoothStatus) {
            menu.findItem(R.id.action_settings).setTitle(R.string.bluetooth_on);
        } else {
            menu.findItem(R.id.action_settings).setTitle(R.string.bluetooth_off);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            if (mBluetoothStatus) {
                //Turn Bluetooth Off
                mBluetoothAdapter.disable();
                item.setTitle(R.string.bluetooth_off);
            } else {
                //Turn Bluetooth On
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                item.setTitle(R.string.bluetooth_on);
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            CURRENT_TAG = TAG_HOME;
        } else if (id == R.id.nav_send) {
            CURRENT_TAG = TAG_FEEDBACK;
        } else if (id == R.id.nav_about) {
            CURRENT_TAG = TAG_ABOUT;
        }

        loadFragment();
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //Request enable bluetooth
        if (requestCode == REQUEST_ENABLE_BT) {
            // Check if it was failed
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "Fail to enable bluetooth", Toast.LENGTH_SHORT).show();
                toggleBluetooth(false);
            }
        }
    }

    private void refresh() {
        if (mBluetoothAdapter.isEnabled()) {
            myBluetoothService.closeGatt();
            clearRecycler();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : pairedDevices) {
                addToRecycler(device);
                if (device.getType() == BluetoothDevice.DEVICE_TYPE_LE || device.getType() == BluetoothDevice.DEVICE_TYPE_DUAL) {
                    myBluetoothService.connectGatt(this, device);
                }
            }

            Set<BluetoothDevice> connectedDevices = myBluetoothService.getConnectedDevices();
            updateConnectedDeviceToRecycler(connectedDevices);
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    Log.d("LESCANNER", "stop");
                }
            }, SCAN_PERIOD);

            mBluetoothLeScanner.startScan(mLeScanCallback);
        } else {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
            Log.d("LESCANNER", "stop");
        }
    }

    void doBindService() {
        bindService(new Intent(this, MyBluetoothService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
    }

    void doUnbindService() {
        if (mIsBound) {
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    private void toggleBluetooth(boolean enable) {
        if (enable) {
            setFabStatus(true);
            mBluetoothStatus = true;
        } else {
            clearRecycler();
            setFabStatus(false);
            mBluetoothStatus = false;
        }
    }

    private void setFabStatus(boolean status) {
        if (status) {
            fab.setClickable(true);
            fab.setAlpha(1f);
        } else {
            fab.setAlpha(0.5f);
            fab.setClickable(false);
        }
    }

    private void addToRecycler(BluetoothDevice device) {
        HomeFragment fragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        if (fragment != null) {
            fragment.mRecyclerAdapter.addmBluetoothDevices(device);
        }
    }

    private void clearRecycler() {
        HomeFragment fragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        if (fragment != null) {
            fragment.mRecyclerAdapter.clearmBluetoothDevices();
        }
    }

    private void updateConnectedDeviceToRecycler(Set<BluetoothDevice> devices) {
        HomeFragment fragment = (HomeFragment) getSupportFragmentManager().findFragmentByTag(TAG_HOME);
        if (fragment != null) {
            fragment.mRecyclerAdapter.updateConnectedDevice(devices);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    scanLeDevice(true);
                    mBluetoothAdapter.startDiscovery();
                } else {
                    Toast.makeText(this, "Location Permission is needed for scan!", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    @Override
    public void onDestroy() {
        //stopService(new Intent(this,MyBluetoothService.class));
        unregisterReceiver(mDiscoveryReceiver);
        myBluetoothService.closeGatt();
        doUnbindService();
        super.onDestroy();
    }
}
