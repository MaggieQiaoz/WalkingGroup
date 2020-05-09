package jade.sfu.walkinggroup.app;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.GpsLocation;
import jade.sfu.walkinggroup.dataobjects.PlaceAutoCompleteAdapter;


//This shows how to create a simple activity with a map and a marker on the map.
public class SelectMapLocationActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    private GpsLocation gpsLocation;
    private GoogleMap myMap;
    private PlaceAutoCompleteAdapter autocompleteAdapter;
    protected GeoDataClient geoDataClient;
    private AutoCompleteTextView editSearch;
    private Boolean locationPermission;
    private FusedLocationProviderClient clientLocation;
    private static final String TAG = "MapLocationActivity";


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap map) {
        myMap = map;

        if (locationPermission) {
            getCurrentLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            myMap.setMyLocationEnabled(true);
        }
        onMapClick();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_set_map_location);

        editSearch = (AutoCompleteTextView) findViewById(R.id.etxtSearch);
        gpsLocation = GpsLocation.getInstance();

        setUpToolBar();
        getLocationPermission();
        searchAddress();

    }

    private void initialMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getLocationPermission() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (gpsEnabled) {

            locationPermission = true;
            initialMap();

        } else {
            //display dialog asking to turn on location services
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    private void getCurrentLocation() {
        clientLocation = LocationServices.getFusedLocationProviderClient(this);
        try {
            final Task Location = clientLocation.getLastLocation();
            Location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Location currLocation = (Location) task.getResult();
                        LatLng currLatlong = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());
                        moveCamera(currLatlong, 15f);
                    } else {
                        Toast.makeText(getApplicationContext(), "location failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception: " + e.getMessage());
        }
    }

    private void moveCamera(LatLng latLng, float zoom) {
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
    }

    private void searchAddress() {

        geoDataClient = Places.getGeoDataClient(this, null);
        LatLngBounds latlongBounds = new LatLngBounds(new LatLng(-90, -180), new LatLng(90, 180));

        autocompleteAdapter = new PlaceAutoCompleteAdapter(this, geoDataClient, latlongBounds, null);

        editSearch.setAdapter(autocompleteAdapter);

        editSearch.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_SEARCH
                        || i == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
                    geoLocate();
                }
                return false;
            }
        });
    }

    private void geoLocate() {

        String searchAddress = editSearch.getText().toString();
        Geocoder geocoder = new Geocoder(SelectMapLocationActivity.this);
        List<Address> listAdress = new ArrayList<>();
        try {

            listAdress = geocoder.getFromLocationName(searchAddress, 1);

        } catch (IOException e) {

            Log.e(TAG, "error is " + e.getMessage());

        }

        if (listAdress.size() > 0) {

            Address address = listAdress.get(0);
            LatLng addressLL = new LatLng(address.getLatitude(), address.getLongitude());

            moveCamera(addressLL, 15);
            Marker groupMarker = myMap.addMarker(new MarkerOptions().position(addressLL));
            createGroupDialog(groupMarker);

        }
    }

    private void onMapClick() {

        myMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng point) {
                myMap.clear();

                Marker groupMarker = myMap.addMarker(new MarkerOptions().position(point));
                createGroupDialog(groupMarker);

                gpsLocation.setLat(point.latitude);
                gpsLocation.setLng(point.longitude);

            }
        });

    }

    private void createGroupDialog(Marker marker) {

        AlertDialog.Builder builder = new AlertDialog.Builder(SelectMapLocationActivity.this);
        builder.setMessage("Do you want to create a Walking Group?")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("lat", marker.getPosition().latitude);
                        returnIntent.putExtra("lng", marker.getPosition().longitude);
                        setResult(Activity.RESULT_OK, returnIntent);
                        finish();
                    }
                })
                .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        marker.remove();
                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, SelectMapLocationActivity.class);
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Add back button to toolbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateNavHeader(NavigationView navigationView) {
        TextView navHeaderTitleTextView = (TextView) navigationView.getHeaderView(0)
                .findViewById(R.id.nav_header_title);
        TextView navHeaderSubtitleTextView = (TextView) navigationView.getHeaderView(1)
                .findViewById(R.id.nav_header_subtitle);

        //TODO: NEED TO MERGE AND GET THE SERVER CLASS
//        navHeaderTitleTextView.setText(server.getUser().getName());
//        navHeaderSubtitleTextView.setText(server.getUser().getEmail());
    }
}