package com.example.linux.hapticdirection;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.widget.TextView;

import android.app.Fragment;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import android.view.View;


public class FragmentMaps extends Fragment implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    protected GoogleApiClient mGoogleApiClient;
    protected Location mLastLocation;
    protected String mLatitudeLabel = "Latitude:";
    protected String mLongitudeLabel = "Longitude";
    protected TextView mLatitudeText;
    protected TextView mLongitudeText;

    MapView mMapView;
    private GoogleMap mGoogleMap;
    private final String bla = "bla";
    public double dirToDest;
    private double dist;
    private double dir;

    //TODO: get destLocation by searching
    Location destLocation = new Location("");
    LatLng destLocationPos= new LatLng(50.97871349999999, 11.309648599999946);


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        destLocation.setLatitude(destLocationPos.latitude);
        destLocation.setLongitude(destLocationPos.longitude);

        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

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
            if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mGoogleMap.setMyLocationEnabled(true);
                LocationManager locman = (LocationManager) getActivity().getSystemService(getContext().LOCATION_SERVICE);
                Location lastKnown = locman.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                //Log.i(bla, Double.toString(lastKnown.getLongitude())+"  "+Double.toString((lastKnown.getLatitude())));
                LatLng pos = new LatLng(lastKnown.getLatitude(), lastKnown.getLongitude());
                mGoogleMap.addMarker(new MarkerOptions().position(destLocationPos).title("Position"));
                //TODO: Display the values on phone
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
        buildGoogleApiClient();

        mLatitudeText = (TextView) rootView.findViewById((R.id.latitude_text));
        mLongitudeText = (TextView) rootView.findViewById((R.id.longitude_text));
        return rootView;
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    public void onConnected(Bundle connectionHint) {
        // Provides a simple way of getting a device's location and is well suited for
        // applications that do not require a fine-grained location and that do not need location
        // updates. Gets the best and most recent location currently available, which may be null
        // in rare cases when a location is not available.
        if (ContextCompat.checkSelfPermission(getActivity(), android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            if (mLastLocation != null) {
                mLatitudeText.setText(String.format("%s: %f", mLatitudeLabel, mLastLocation.getLatitude()));
                mLongitudeText.setText(String.format("%s: %f", mLongitudeLabel, mLastLocation.getLongitude()));
            } else {
                Log.i(bla, "Location not detected ");
            }
        }

    }


    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
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


    @Override
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
}

