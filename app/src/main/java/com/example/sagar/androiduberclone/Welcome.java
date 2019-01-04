package com.example.sagar.androiduberclone;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;

import com.example.sagar.androiduberclone.common.Common;
import com.example.sagar.androiduberclone.remote.IGoogleApi;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationListener;

import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.geofire.GeoFire;
import com.github.glomadrian.materialanimatedswitch.MaterialAnimatedSwitch;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class Welcome extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private GoogleMap mMap;

    //play services
    private static final int MY_PERMISSION_REQUEST_CODE = 7000;
    private static final int PLAY_SERVICES_RES_REQUEST = 7001;


    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;


    private static int UPDATE_INTERVAL = 5000;
    private static int FASTEST_INTERVAL = 3000;
    private static int DISPLACEMENT = 10;

    DatabaseReference drivers;
    GeoFire geoFire;

    Marker mCurrent;
    MaterialAnimatedSwitch location_switch;
    SupportMapFragment mapFragment;

    //car animation
    private List<LatLng> polyLineList;
    private Marker pickupLocationMarker;
    private float v;
    private double lat,lng;
    private Handler handler;
    private LatLng startPosition,endPosition,currentPosition;
    private int index,next;
    private Button btnGo;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions polylineOptions,blackPolylineOptions;
    private Polyline blackPolyline,greyPolyline;

    private IGoogleApi mService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        location_switch = findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new MaterialAnimatedSwitch.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(boolean isOnline) {
                if (isOnline) {
                    startLocationUpdates();
                    displayLocation();
                    Snackbar.make(mapFragment.getView(), "You are online", Snackbar.LENGTH_SHORT).show();
                } else {
                    stopLocationUpdates();
                    mCurrent.remove();
                    Snackbar.make(mapFragment.getView(), "You are offline", Snackbar.LENGTH_SHORT).show();
                }
            }
        });

        polyLineList=new ArrayList<>();
        btnGo=findViewById(R.id.btnGo);
        edtPlace=findViewById(R.id.edtPlace);

        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                destination=edtPlace.getText().toString();
                destination=destination.replace(" ","+");


                getDirection();
            }
        });

        //Geo fire
        drivers = FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire = new GeoFire(drivers);

        setUpLocation();

        mService= Common.getGoogleApi();
    }

    private void getDirection() {

        currentPosition=new LatLng(mLastLocation.getLatitude(),mLastLocation.getLongitude());

        String requestApi=null;

        try {
            requestApi= "https://maps.googleapis.com/maps/api/directions/json?" +
                    "mode=driving&"
                    + "transit_routing_preference=less_driving&"
                    + "origin=" + currentPosition.latitude + "," + currentPosition.longitude + "&"
                    + "destination=" + destination + "&"
                    + "key=" + getResources().getString(R.string.google_direction_api);


            Log.d("requestApi", requestApi);
            mService.getPath(requestApi)
                     .enqueue(new Callback<String>() {
                         @Override
                         public void onResponse(Call<String> call, Response<String> response) {

                             try {
                                 JSONObject jsonObject=new JSONObject(response.body().toString());

                             } catch (JSONException e) {
                                 e.printStackTrace();
                             }
                         }

                         @Override
                         public void onFailure(Call<String> call, Throwable t) {

                             Toast.makeText(getApplicationContext(),""+t.getMessage(),Toast.LENGTH_SHORT).show();
                         }
                     });


        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {

            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (checkPlayServices()) {

                        buildGoogleApiClient();
                        createLocationRequet();

                        if (location_switch.isChecked()) {
                            displayLocation();
                        }
                    }
                }
        }
    }

    private void setUpLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            //request runtime permission
            ActivityCompat.requestPermissions(Welcome.this, new String[]
                            {Manifest.permission.ACCESS_COARSE_LOCATION,
                                    Manifest.permission.ACCESS_FINE_LOCATION},
                    MY_PERMISSION_REQUEST_CODE);

        } else {

            if (checkPlayServices()) {

                buildGoogleApiClient();
                createLocationRequet();

                if (location_switch.isChecked()) {
                    displayLocation();
                }
            }
        }
    }

    private void createLocationRequet() {
        //mLocationRequest=LocationRequest.create();
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();

        mGoogleApiClient.connect();

    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {

            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RES_REQUEST).show();

            } else {
                Toast.makeText(this, "This device is not supported", Toast.LENGTH_LONG).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        // to retrieve the user’s location in your Android application you can take advantage of Google’s FusedLocationApi.
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdates() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    private void displayLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }

        //getLastLocation(mGoogleApiClient) Returns the best most recent location currently available.
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

        if (mLastLocation != null) {

            if (location_switch.isChecked()) {

                final double latitude = mLastLocation.getLatitude();
                final double longitude = mLastLocation.getLongitude();


                //update to firebase
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latitude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        //Add marker
                        if (mCurrent != null)
                            mCurrent.remove();  //remove already marker

                        mCurrent = mMap.addMarker(new MarkerOptions()
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                .position(new LatLng(latitude, longitude))
                                .title("You"));

                        //move camera to this position
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 15.0f));

                    }
                });

            }
        } else {
            Log.d("Error", "cant get your location");
        }
    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {

        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        final float startRotation = mCurrent.getRotation();
        final long duration = 1500;

        final Interpolator interpolator = new LinearInterpolator();

        handler.post(new Runnable() {
            @Override
            public void run() {

                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                float rot = t * i + (1 - t) * startRotation;

                mCurrent.setRotation(-rot > 180 ? rot / 2 : rot);
                if (t < 1.0) {
                    handler.postDelayed(this, 16);
                }
            }
        });
    }



    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setBuildingsEnabled(false);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);
    }


    @Override
    public void onConnected(@Nullable Bundle bundle) {

/*
        After calling connect(), this method will be invoked asynchronously when the connect request has successfully completed.
        After this callback, the application can make requests on other methods provided by the client and expect that no user
        intervention is required to call methods
        that use account and scopes provided to the client constructor.
*/
        displayLocation();
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
     //   Called when the client is temporarily in a disconnected state.
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        //no connection ,error
    }

    @Override
    public void onLocationChanged(Location location) {
        //Called when the location has changed.
        mLastLocation = location;
        displayLocation();
    }
}
