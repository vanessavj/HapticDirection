package com.example.linux.hapticdirection;

import android.Manifest;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.TextView;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;

import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Calendar;


public class FragmentMaps extends Fragment {

    protected static final String TAG = "location-updates-sample";

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    /**
     * The fastest rate for active location updates. Exact. Updates will never be more frequent
     * than this value.
     */
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";

    /**
     * Stores parameters for requests to the FusedLocationProviderApi.
     */
    protected LocationRequest mLocationRequest;
    /**
     * Represents a geographical location.
     */
    protected Location mCurrentLocation;
    protected GoogleApiClient mGoogleApiClient;

    protected Location mLastLocation;

    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;
    protected TextView mLastUpdateTimeTextView;
    protected String mLatitudeLabel = "Lat";
    protected String mLongitudeLabel = "Long";
    protected String mLastUpdateTimeLabel = "Last Update";
    protected String mBearingToDestLabel = "BearToDest";
    protected String mHeadingLabel = "Heading";
    protected String mDistanceToDestLabel = "DistToDest";
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;
    protected TextView mBearingToDestText;
    protected TextView mHeadingText;
    protected TextView mDistanceToDestText;
    protected String direction ="";


    /**
     * Tracks the status of the location updates request. Value changes when the user presses the
     * Start Updates and Stop Updates buttons.
     */
    protected Boolean mRequestingLocationUpdates;
    /**
     * Time when the location was updated represented as a String.
     */
    protected String mLastUpdateTime;


    MapView mMapView;
    private GoogleMap mGoogleMap;
    private final String bla = "bla";



    //TODO: get destLocation by searching
    Location destLocation = new Location("");

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);
       /* mStartUpdatesButton = (Button) rootView.findViewById(R.id.start_updates_button);
        mStartUpdatesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if (!mRequestingLocationUpdates) {
                    mRequestingLocationUpdates = true;
                    setButtonsEnabledState();
                    startLocationUpdates();
                }
            }
        });
        mStopUpdatesButton = (Button) rootView.findViewById(R.id.stop_updates_button);
        mStopUpdatesButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if (mRequestingLocationUpdates) {
                    mRequestingLocationUpdates = false;
                    setButtonsEnabledState();
                    stopLocationUpdates();

                }
            }
        });
        mLatitudeText = (TextView) rootView.findViewById(R.id.latitude_text);
        mLongitudeText = (TextView) rootView.findViewById(R.id.longitude_text);
        mLastUpdateTimeTextView = (TextView) rootView.findViewById(R.id.last_update_time_text);
        mBearingToDestText = (TextView) rootView.findViewById(R.id.bearing_to_dest_text);
        mDistanceToDestText = (TextView) rootView.findViewById(R.id.distance_to_dest_text);
        mHeadingText = (TextView) rootView.findViewById(R.id.heading_text);

        mRequestingLocationUpdates = false;
        mLastUpdateTime = "";
        // Update values using data stored in the Bundle.
        updateValuesFromBundle(savedInstanceState);
        */

        mMapView = (MapView) rootView.findViewById(R.id.map);
        //Log.i(bla, "irgendwas");
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume(); // needed to get the map to display immediately

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {

        public void onMapReady(GoogleMap map) {
            mGoogleMap = map;
            if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(true);
                LocationManager locman = (LocationManager) getActivity().getSystemService(getContext().LOCATION_SERVICE);
                Location lastKnown = locman.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                //Log.i(bla, Double.toString(lastKnown.getLongitude())+"  "+Double.toString((lastKnown.getLatitude())));
                LatLng pos = new LatLng(lastKnown.getLatitude(), lastKnown.getLongitude());
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.976429, 11.316403)).title("Start R1").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.980347, 11.313355)).title("Start R2").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.978915, 11.309729)).title("Dest R1").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.977828, 11.319856)).title("Dest R2").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.990320, 11.333630)).title("Dest R1").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.986775, 11.329777)).title("Dest R2").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                Log.i(bla, Double.toString(lastKnown.bearingTo(destLocation)));
                Log.i(bla, Double.toString(lastKnown.distanceTo(destLocation)));
                CameraUpdate camUp = CameraUpdateFactory.newLatLngZoom(pos, 16);
                mGoogleMap.moveCamera(camUp);
            }
            else{
                Log.i(bla, "Permissions nicht gesetzt");
            }
        }

        });
        /*
        buildGoogleApiClient();

        mLatitudeText = (TextView) rootView.findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) rootView.findViewById((R.id.longitude_text));*/
        return rootView;
    }

    /*private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);
                setButtonsEnabledState();
            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            updateUI();
        }
    }

    private void updateUI() {

        mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel,
                mCurrentLocation.getLatitude()));
        mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel,
                mCurrentLocation.getLongitude()));
        mLastUpdateTimeTextView.setText(String.format("%s: %s", mLastUpdateTimeLabel,
                mLastUpdateTime));
        mBearingToDestText.setText(String.format("%s: %s", mBearingToDestLabel,
               mCurrentLocation.bearingTo(destLocation)));
        mHeadingText.setText(String.format("%s: %s", mHeadingLabel, mCurrentLocation.getBearing()));
        mDistanceToDestText.setText(String.format("%s: %s", mDistanceToDestLabel,
                mCurrentLocation.distanceTo(destLocation)));
        CameraUpdate camUp = CameraUpdateFactory.newLatLngZoom(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()), 16);
        mGoogleMap.moveCamera(camUp);
        


    }

    private void setButtonsEnabledState() {
        if (mRequestingLocationUpdates) {
            mStartUpdatesButton.setEnabled(false);
            mStopUpdatesButton.setEnabled(true);
        } else {
            mStartUpdatesButton.setEnabled(true);
            mStopUpdatesButton.setEnabled(false);
        }
    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();

        // Sets the desired interval for active location updates. This interval is
        // inexact. You may not receive updates at all if no location sources are available, or
        // you may receive them slower than requested. You may also receive updates faster than
        // requested if other applications are requesting location at a faster interval.
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);

        // Sets the fastest rate for active location updates. This interval is exact, and your
        // application will never receive updates faster than this value.
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);

        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }



    

    private void stopLocationUpdates() {
        // It is a good practice to remove location requests when the activity is in a paused or
        // stopped state. Doing so helps battery performance and is especially
        // recommended in applications that request frequent location updates.

        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, (LocationListener) this);
    }

    private void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(
                    mGoogleApiClient, mLocationRequest, this);
        }

    }*/

    @Override
    public void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
       /* if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }*/
    }

    /*public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        // If the initial location was never previously requested, we use
        // FusedLocationApi.getLastLocation() to get it. If it was previously requested, we store
        // its value in the Bundle and check for it in onCreate(). We
        // do not request it again unless the user specifically requests location updates by pressing
        // the Start Updates button.
        //
        // Because we cache the value of the initial location in the Bundle, it means that if the
        // user launches the activity,
        // moves to a new location, and then changes the device orientation, the original location
        // is displayed as the activity is re-created.
        if (mCurrentLocation == null) {
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                Calendar c = Calendar.getInstance();
                mLastUpdateTime = c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
                //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
                updateUI();
                calculateHeading();
            }

        }
        // If the user presses the Start Updates button before GoogleApiClient connects, we set
        // mRequestingLocationUpdates to true (see startUpdatesButtonHandler()). Here, we check
        // the value of mRequestingLocationUpdates and if it is true, we start location updates.
        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }

    }



    @Override
    public void onLocationChanged(Location location) {
        mCurrentLocation = location;
        Calendar c = Calendar.getInstance();
        mLastUpdateTime = c.get(Calendar.HOUR)+":"+c.get(Calendar.MINUTE)+":"+c.get(Calendar.SECOND);
        //mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
        updateUI();
        calculateHeading();
        Log.i(bla, "Location is updated");

    }

    private void calculateHeading() {
        double bearingToDest = (mCurrentLocation.bearingTo(destLocation) - mCurrentLocation.getBearing()) % 360;
        if (bearingToDest<=22.5 && bearingToDest>337.5) {
            direction = "a";
        }else if(bearingToDest>22.5 && bearingToDest<=67.5){
            direction ="al";
        }else if(bearingToDest>67.5 && bearingToDest<=112.5){
            direction ="l";
        }else if(bearingToDest>112.5 && bearingToDest<=157.5){
            direction ="bl";
        }else if(bearingToDest>157.5 && bearingToDest<=202.5){
            direction = "b";
        }else if(bearingToDest>202.5 && bearingToDest<=247.5){
            direction = "br";
        }else if(bearingToDest>247.5 && bearingToDest<=292.5){
            direction = "r";
        }else if(bearingToDest>292.5 && bearingToDest<=337.5){
            direction = "ar";
        }
        //sendToArduino(direction);

    }*/

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
        /*if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }*/
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
       /* if (mGoogleApiClient.isConnected()) {
            stopLocationUpdates();
        }*/
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }


  /*  @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.i(bla, "Connection failed: ConnectionResult.getErrorCode() = " + connectionResult.getErrorCode());

    }

    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(bla, "Connection suspended");
        mGoogleApiClient.connect();
    }

    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }*/

    //TODO: Hier haben wir ein Problem ... cool was ... NullpointerObject
    /*public void sendToArduino(String direction){
        byte[] value;
        try {
            value = direction.getBytes("UTF-8");
            ((MainActivity)getActivity()).getmService().writeRXCharacteristic(value);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }*/
}

