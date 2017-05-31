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

    MapView mMapView;
    private GoogleMap mGoogleMap;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map, container, false);

        mMapView = (MapView) rootView.findViewById(R.id.map);
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
                LatLng pos = new LatLng(lastKnown.getLatitude(), lastKnown.getLongitude());
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.976429, 11.316403)).title("Start R1").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_CYAN)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.980347, 11.313355)).title("Start R2").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ROSE)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.978915, 11.309729)).title("Dest R1").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.977828, 11.319856)).title("Dest R2").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.990320, 11.333630)).title("Dest R1").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
                mGoogleMap.addMarker(new MarkerOptions().position(new LatLng(50.986775, 11.329777)).title("Dest R2").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_YELLOW)));
                CameraUpdate camUp = CameraUpdateFactory.newLatLngZoom(pos, 16);
                mGoogleMap.moveCamera(camUp);
            }
            else{
                Log.i("Permissions", "Permissions nicht gesetzt");
            }
        }

        });
        return rootView;
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        super.onStop();
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

}

