package com.example.linux.hapticdirection;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Random;
import static java.util.Arrays.asList;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    /***************************************************
     * Variables for Bluetooth Service
     ***************************************************/
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "nRFUART";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;;
    private Button btnConnectDisconnect;

    /**********************************************************
     * Variables for Location Updates
     *********************************************************/

    //Desired Interval for Location Updates
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 2000;

    // The fastest interval for Location Updates
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    protected GoogleApiClient mGoogleApiClient;

    //stores parameters for requests to the FusedLocationProviderApi
    protected LocationRequest mLocationRequest;

    //tracks whether location updates are currently requested. Changes when Start or Stop Updates is pressed
    protected Boolean mRequestingLocationUpdates;

    //last time the location was updated
    protected String mLastUpdateTime;

    //store current and destination location of both routes
    protected Location mCurrentLocation;
    protected Location destLocation = new Location("");
    protected final LatLng route1Pos = new LatLng(50.978915, 11.309729);
    protected final LatLng route2Pos = new LatLng(50.978032, 11.319835);
    protected String direction = "No direction set yet";
    protected double bearingToDest;

    // stores whether push- or pull based mode is selected
    protected String selectedMode;

    /* List of all 8 directions, each repeated 3 times
    needed for testing by playing 24 pattern in randomized order*/
    protected List<String> directions = asList("l","al","a","ar","r","br","b","bl",
            "l","al","a","ar","r","br","b","bl",
            "l","al","a","ar","r","br","b","bl");
    protected int indexDirections;

    // UI Widgets.
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected TextView mLastUpdateTimeTextView;
    protected TextView mLatitudeTextView;
    protected TextView mLongitudeTextView;
    protected TextView mCurrentHeadingTextView;
    protected Switch mActivePassiveSwitch;
    protected Switch mRouteSwitch;

    // Labels.
    protected String mLatitudeLabel;
    protected String mLongitudeLabel;
    protected String mLastUpdateTimeLabel;
    protected String mCurrentHeadingLabel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // display map when app is started
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.place_holder, new FragmentMaps());
        fragmentTransaction.commit();

        //initialize Bluetooth Service
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnectDisconnect = (Button) findViewById(R.id.btn_select);
        service_init();

        // Handle Disconnect & Connect button
        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {

                        //Connect button pressed, open DeviceListActivity class, with popup windows that scan for devices
                        Intent newIntent = new Intent(MainActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);
                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                            mRequestingLocationUpdates=false;
                            stopLocationUpdates();
                        }
                    }
                }
            }
        });

        /*******************************************************
         * Set up location Updates
         *******************************************************/

        // Locate the UI widgets.
        mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
        mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);
        mLatitudeTextView = (TextView) findViewById(R.id.latitude_text);
        mLongitudeTextView = (TextView) findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView = (TextView) findViewById(R.id.last_update_time_text);
        mCurrentHeadingTextView = (TextView) findViewById(R.id.current_heading);

        // Set labels.
        mLatitudeLabel = "Latitude";
        mLongitudeLabel = "Longitude";
        mLastUpdateTimeLabel = "LastUpdate";
        mCurrentHeadingLabel = "CurrHeading";

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";

        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
        mStartUpdatesButton.setEnabled(false);
        mStopUpdatesButton.setEnabled(false);

        // Kick off the process of building a GoogleApiClient and requesting the LocationServices API
        buildGoogleApiClient();

        // get the selected Mode and attach a change listener to the switch
        mActivePassiveSwitch = (Switch) findViewById(R.id.active_passive_switch);
        if(mActivePassiveSwitch.isChecked()){
            selectedMode="Active";
        }
        else{
            selectedMode="Passive";
        }
        mActivePassiveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    selectedMode="Active";
                }
                else {
                    selectedMode="Passive";
                }
            }
        });

        // get current route and attach change listener
        // set destination location according to current route
        mRouteSwitch = (Switch) findViewById(R.id.route_switch);
        if(mRouteSwitch.isChecked()){
            destLocation.setLatitude(route2Pos.latitude);
            destLocation.setLongitude(route2Pos.longitude);
        }else{
            destLocation.setLatitude(route1Pos.latitude);
            destLocation.setLongitude(route1Pos.longitude);
        }
        mRouteSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if(isChecked){
                    destLocation.setLatitude(route2Pos.latitude);
                    destLocation.setLongitude(route2Pos.longitude);
                }else{
                    destLocation.setLatitude(route1Pos.latitude);
                    destLocation.setLongitude(route1Pos.longitude);
                }
            }
        });
    }

    /***********************************************************************************
     * Methods related to Bluetooth Service
     ***********************************************************************************/

    //UART service connected/disconnected
    public ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        //Handler events that received from UART service
        public void handleMessage(Message msg) {

        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        setButtonsEnabledState();
                        ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - ready");
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        mStartUpdatesButton.setEnabled(false);
                        mStopUpdatesButton.setEnabled(false);
                        ((TextView) findViewById(R.id.deviceName)).setText("Not Connected");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {

                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = new String(txValue, "UTF-8");
                            String currentDateTimeString = DateFormat.getTimeInstance().format(new Date());
                            String lastLocation = String.valueOf(mCurrentLocation.getLatitude()) + "," + String.valueOf(mCurrentLocation.getLongitude());
                            Log.i("VibrationInformation", currentDateTimeString+" "+ lastLocation +" " + text);
                            appendLog(currentDateTimeString+" "+ lastLocation +" " + text);
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });
            }
            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                //When the DeviceListActivity return, with the selected device address
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    ((TextView) findViewById(R.id.deviceName)).setText(mDevice.getName() + " - connecting");
                    mService.connect(deviceAddress);

                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }
    /**********************************************************************************
     * Methods related to Google Maps API and Location Services API
     **********************************************************************************/

    //builds GoogleApiClient and requests LocationServices API
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    //sets parameters for Location Request
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // if location was never requested before, use getLastLocation() to get it
        if (mCurrentLocation == null) {
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
            updateUI();
        }

        // check if User pressed button before the client was ready and start locationUpdates if so
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        // connection was lost, try to reconnect.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());
    }

    //gets saved currentlocation and lastupdatetime from bundle and updates UI
    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    //saves current location and last update time if App is paused
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }

    // starts location updates when start button is pressed and not already running
    public void startUpdatesButtonHandler(View view) {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;
            setButtonsEnabledState();
            startLocationUpdates();
        }
    }

    //location requests started
    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);

    }

    // stop location updates when stop button is pressed and running
    public void stopUpdatesButtonHandler(View view) {
        if (mRequestingLocationUpdates) {
            mRequestingLocationUpdates = false;
            setButtonsEnabledState();
            stopLocationUpdates();
        }
    }

    //location requests stopped
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    // if locationUpdates are currently running, set button to stop
    //otherwise set button to start
    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }

    //Callback that fires when the location changes.
    // updates LastUpdateTime and calculates the heading
    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation=location;
        Calendar c = Calendar.getInstance();
        mLastUpdateTime = c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
        updateUI();
        try {
            calculateHeading();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        Log.i(TAG, "Location is updated");
    }

    //calculates heading by comparing bearing to destination with current bearing
    // converts heading to direction
    private void calculateHeading() throws UnsupportedEncodingException {
        bearingToDest = mCurrentLocation.bearingTo(destLocation) - mCurrentLocation.getBearing();
        bearingToDest=mod((int)bearingToDest,360);
        if (bearingToDest<=22.5 || bearingToDest>337.5) {
            direction = "a";
        }else if(bearingToDest>22.5 && bearingToDest<=67.5){
            direction ="ar";
        }else if(bearingToDest>67.5 && bearingToDest<=112.5){
            direction ="r";
        }else if(bearingToDest>112.5 && bearingToDest<=157.5){
            direction ="br";
        }else if(bearingToDest>157.5 && bearingToDest<=202.5){
            direction = "b";
        }else if(bearingToDest>202.5 && bearingToDest<=247.5){
            direction = "bl";
        }else if(bearingToDest>247.5 && bearingToDest<=292.5){
            direction = "l";
        }else if(bearingToDest>292.5 && bearingToDest<=337.5){
            direction = "al";
        } else {
            direction = String.valueOf(bearingToDest);
        }
        String modeDirection = selectedMode+" "+direction;
        byte [] value = modeDirection.getBytes("UTF-8");
        //sends direction to Arduino
        mService.writeRXCharacteristic(value);

    }

    private int mod(int x, int y)
    {
        int result = x % y;
        return result < 0? result + y : result;
    }

    /**************************************************************************************
     * Methods related to UI
     ***************************************************************************************/

    // enables to switch between Test and Map Fragment
    public void selectFrag(View v) {
        Fragment fragment;
        if (v == findViewById(R.id.test)) {
            fragment = new FragmentTest();
        } else {
            fragment = new FragmentMaps();
        }

        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.place_holder, fragment);
        fragmentTransaction.commit();

    }

    //updates TextViewa of Latitude, Loongitude, LastUpdateTime and Heading
    private void updateUI() {
        mLatitudeTextView.setText(String.format("%s: %f", mLatitudeLabel,
                mCurrentLocation.getLatitude()));
        mLongitudeTextView.setText(String.format("%s: %f", mLongitudeLabel,
                mCurrentLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(String.format("%s: %s", mLastUpdateTimeLabel,
                mLastUpdateTime));
        mCurrentHeadingTextView.setText(String.format("%s: %f", mCurrentHeadingLabel,
                bearingToDest));
    }

    // if Button of Testfragment is pressed, send the respective direction to Arduino
    public void ButtonPressed(View v) {
        String s = v.getTag().toString();
        s = "Test"+" "+s;
        byte[] value;
        try {
            value = s.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    // list of directions is randomly shuffled by taking the System Time as a seed
    // after shuffling the direction, the start Button for the Test is enabled
    public void shuffleDirections(View v) {
        long seed = System.nanoTime();
        Collections.shuffle(directions,new Random(seed));
        indexDirections = 0;
        v.setEnabled(false);
        Button startNextButton = (Button) findViewById(R.id.start_next_button);
        startNextButton.setEnabled(true);
        TextView currentDirection = (TextView) findViewById(R.id.current_direction);
        currentDirection.setText("");
        TextView progressTest = (TextView) findViewById(R.id.progress_test);
        progressTest.setText("");
    }

    //after pressing the start button, text turns into next
    // go through the list of shuffled direction
    // display the current direction and the progress
    public void startNext (View v){
        Button b = (Button) v;
        if(b.getText().equals("Start")){
            b.setText("Next");
        }
        String s = "Test "+ directions.get(indexDirections);
        byte[] value;
        try {
            value = s.getBytes("UTF-8");
            mService.writeRXCharacteristic(value);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        TextView currentDirection = (TextView) findViewById(R.id.current_direction);
        currentDirection.setText(directions.get(indexDirections));
        TextView progressTest = (TextView) findViewById(R.id.progress_test);
        progressTest.setText(indexDirections+1 +"/"+directions.size());
        indexDirections +=1;
        if(indexDirections >= directions.size()){
            b.setEnabled(false);
            b.setText("Start");
            Button shuffleButton = (Button) findViewById(R.id.shuffle_button);
            shuffleButton.setEnabled(true);
        }

    }

    /*******************************************************************************************
     * General methods like onStart
     *******************************************************************************************/

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService = null;

    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
        if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }

    @Override
    public void onBackPressed() {
        if (mState == UART_PROFILE_CONNECTED) {
            Intent startMain = new Intent(Intent.ACTION_MAIN);
            startMain.addCategory(Intent.CATEGORY_HOME);
            startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startMain);
            showMessage("nRFUART's running in background.\n             Disconnect to exit");
        } else {
            new AlertDialog.Builder(this)
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setTitle(R.string.popup_title)
                    .setMessage(R.string.popup_message)
                    .setPositiveButton(R.string.popup_yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    })
                    .setNegativeButton(R.string.popup_no, null)
                    .show();
        }
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    /*************************************************************************************************
     * Method for Logging
     ************************************************************************************************/
    public void appendLog(String text)
    {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
            String fileName = Environment.getExternalStorageDirectory().getAbsolutePath()+"/log.txt";
            File logFile = new File(fileName);
            //if no log file exist, create a new file
            if (!logFile.exists())
            {
                try
                {
                    logFile.createNewFile();
                }
                catch (IOException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            try
            {
                //append the vibrationinformation to the existing logfile
                BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
                buf.append(text);
                Log.i("VibrationInformation", text + " appended to" + fileName);
                buf.newLine();
                buf.close();
            }
            catch (IOException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
}


