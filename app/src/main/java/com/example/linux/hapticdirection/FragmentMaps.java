package com.example.linux.hapticdirection;

import android.Manifest;
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


public class FragmentMaps extends Fragment {

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

        return rootView;
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
