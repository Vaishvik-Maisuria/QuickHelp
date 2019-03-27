package com.example.profile;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.profile.directionhelpers.FetchURL;
import android.location.LocationListener;

import com.example.profile.httpRequestHelpers.httpPostRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;


public class MapFragment extends Fragment implements OnMapReadyCallback, LocationListener {
    GoogleMap mGoogleMap;
    MapView mMapView;
    View mView;
    Polyline currentPolyline;
    private LocationManager locationManager;
    private final int REQUEST_FINE_LOCATION = 1234;
    String endpoint = "https://quick-health.herokuapp.com";
    private Double patientlat = null;
    private Double patientlong = null;

    private Location onlyOneLocation;
    private MarkerOptions place1, place2;

    public MapFragment() {}

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_FINE_LOCATION);
        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);


        mMapView = (MapView) mView.findViewById(R.id.map);
        if (mMapView != null) {
            mMapView.onCreate(null);
            mMapView.onResume();
            mMapView.getMapAsync(this);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mView = inflater.inflate(R.layout.fragment_map, container, false);
        return mView;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        JSONObject json = null;
        String sosActive = null;
        MapsInitializer.initialize(getContext());
        mGoogleMap = googleMap;

        Criteria criteria = new Criteria();
        LocationManager locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        String provider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(provider);
        if (location != null) {
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();

            sosActive = getSOSId();
            if (sosActive != "null") {
                json = getSOSInfo(sosActive);
                try {
                    patientlat = (double) json.get("latitude");
                    patientlong = (double) json.get("longitude");
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                place1 = new MarkerOptions().position(new LatLng(latitude, longitude)).title("You");

                if (patientlong != null && patientlat != null) {
                    place2 = new MarkerOptions().position(new LatLng(patientlat, patientlong)).title("Patient");
                    if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mGoogleMap.setMyLocationEnabled(true);

                    new FetchURL(this).execute(getUrl(place1.getPosition(), place2.getPosition(), "driving"), "driving");

                    googleMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

                    googleMap.addMarker(place1);
                    googleMap.addMarker(place2);
                    CameraPosition Liberty = CameraPosition.builder().target(new LatLng(latitude, longitude)).zoom(18).bearing(0).tilt(45).build();

                    googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(Liberty));
                }
            }
        }
    }

    private String getUrl(LatLng origin, LatLng dest, String directionMode) {
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Mode
        String mode = "mode=" + directionMode;
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + mode;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.google_maps_key);
        return url;
    }


    public void onTaskDone(Object... values) {
        if (currentPolyline != null)
            currentPolyline.remove();
        currentPolyline = mGoogleMap.addPolyline((PolylineOptions) values[0]);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_FINE_LOCATION:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("gps", "Location permission granted");
                    try {
                        locationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
                    } catch (SecurityException ex) {
                        Log.d("gps", "Location permission did not work!");
                    }
                }
                break;
        }
    }

    @Override
    public void onLocationChanged(Location location) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}


    /*
     *   Get current logged in user's active sos id
     */
    public String getSOSId() {
        String sosActive = null;

        try {

            // Get current logged in user's id
            String id = User.getUser().get("_id").toString();

            // Create request body
            Map<String, String> body = new HashMap<>();
            body.put("id", id);

            // Create request to fetch user info
            httpPostRequest userInfoTask = new httpPostRequest(body);

            String result = userInfoTask.execute( endpoint + "/user/specificUserInfo").get();
            JSONObject sosInfo = new JSONObject(result.substring(8, result.length()-1));
            sosActive = sosInfo.get("sosActive").toString();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sosActive;
    }


    /*
     *   Get sos info
     */
    public JSONObject getSOSInfo(String sos) {
        JSONObject json = null;

        // Create request body
        Map<String, String> body = new HashMap<>();
        body.put("id", sos);

        // Create request to fetch an SOS info
        httpPostRequest sosInfoTask = new httpPostRequest(body);

        try {
            String res = sosInfoTask.execute( endpoint + "/sos/specificSOS").get();
            json = new JSONObject(res.substring(7, res.length()-1));
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }
}

