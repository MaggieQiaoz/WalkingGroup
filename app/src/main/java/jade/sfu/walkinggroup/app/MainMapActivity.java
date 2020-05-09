package jade.sfu.walkinggroup.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
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
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.GpsLocation;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Message;
import jade.sfu.walkinggroup.dataobjects.PlaceAutoCompleteAdapter;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

import static jade.sfu.walkinggroup.app.CurrentGroupsActivity.DESTINATION_LAT;
import static jade.sfu.walkinggroup.app.CurrentGroupsActivity.DESTINATION_LNG;
import static jade.sfu.walkinggroup.app.CurrentGroupsActivity.RUNNABLE_STATUS_ACTIVE;
import static jade.sfu.walkinggroup.app.CurrentGroupsActivity.RUNNABLE_STATUS_INACTIVE;


// Initial map main page where user can see all other groups on screen
public class MainMapActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener {

    public static final String USER_ID = "userId";
    private GpsLocation gpsLocation;
    private GoogleMap myMap;
    private AutoCompleteTextView editSearch;
    private PlaceAutoCompleteAdapter autocompleteAdapter;
    protected GeoDataClient geoDataClient;
    private Boolean locationPermission;
    private FusedLocationProviderClient clientLocation;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final int REQUEST_CODE_CREATE_NEW_GROUP = 11;
    private static final int REQUEST_CODE_CURRENT_GROUP = 12;
    private int runnableStatus;// 0: uninitialized, 1: inactive, 2: active

    private DrawerLayout drawerLayout;

    private Server server;
    private User user;
    private Context context;
    private List<Group> groups = new ArrayList<>();
    private List<User> users = new ArrayList<>();
    private List<User> monitoredUsers = new ArrayList<>();


    private Group clickedGroup = new Group();
    private Group createdGroup;

    private static final String TAG = "MainMapActivity";
    private static final String SERVER_TOKEN = "ServerToken";
    private SharedPreferences userPrefs;
    private static final String USER_PREFERENCES = "UserPrefs";
    private static final String USER_EMAIL = "userEmail";
    private final String EMERGENCY = "emergency";

    private LatLng currentLatLng;
    private LatLng destinationLatLng;

    private GpsLocation myGpsLocation;

    private ArrayAdapter<String> adapter;

    private MyRunnable runnableCode; // = new MyRunnable();
    private Handler handler = new Handler();
    boolean isRewarded = false;
    private Integer rewardedPoints;

    private final String DEFAULT_PANIC_MESSAGE = "PANICKING";


    // Inspired by: https://stackoverflow.com/questions/5844308/removecallbacks-not-stopping-runnable/51098789#51098789
    public class MyRunnable implements Runnable {


        private boolean stopTask = false;

        public void run() {
            if (stopTask) {
                return;
            }

            sendCurrentLocationToServer();

            handler.postDelayed(runnableCode, 30000); //30000
        }

        public void killRunnable() {
            stopTask = true;
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (runnableCode != null) {
            runnableCode.killRunnable();
        }

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    @Override
    public void onMapReady(GoogleMap map) {
        myMap = map;
        getAllGroupsOnServer();
        getCurrentUsers();
        getMonitorsUsers(server.getUser().getId());

        if (locationPermission) {
            getCurrentLocation();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            myMap.setMyLocationEnabled(true);
        }
        myMap.getUiSettings().setMyLocationButtonEnabled(false);
//        displayNewCreatedGroup();
        onGroupClick();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        server = Server.getInstance(this, TAG);
        user = server.getUser();
        if (user.getRewards().getHasColorScheme1() && user.getRewards().getHasColorScheme2()) {
            setTheme(R.style.GoldTheme);
        }
        else if (user.getRewards().getHasColorScheme1() && !user.getRewards().getHasColorScheme2()) {
            setTheme(R.style.GreenTheme);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_map);

        editSearch = (AutoCompleteTextView) findViewById(R.id.etxtSearch);
        gpsLocation = GpsLocation.getInstance();

        myGpsLocation = GpsLocation.getInstance();


        getLocationPermission();
        setUpNavDrawer();
        setUpToolBar();
        searchAddress();
        setUpAddNewGroupButton();
        setupPanicButton();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        getAllGroupsOnServer();
        getCurrentUsers();
        getMonitorsUsers(user.getId());
    }

    private void setupPanicButton() {
        Button panicButton = (Button) findViewById(R.id.panicButton);
        panicButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText messageToParentsET = new EditText(MainMapActivity.this);
                messageToParentsET.setHint(R.string.message_parents_and_leaders);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainMapActivity.this);
                builder.setView(messageToParentsET)
                        .setTitle(R.string.panic);

                builder.setPositiveButton(R.string.panic, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Message messageToParents = new Message();
                        if (!messageToParentsET.getText().toString().isEmpty()) {
                            messageToParents.setText(messageToParentsET.getText().toString());
                            messageToParents.setEmergency(true);
                        } else {
                            messageToParents.setText(DEFAULT_PANIC_MESSAGE);
                            messageToParents.setEmergency(true);
                        }
                        Call<List<Message>> caller = server.getProxy().newMessageToParentsOf(server.getUser().getId(), messageToParents);
                        ProxyBuilder.callProxy(MainMapActivity.this, caller, returnedMessages -> responseMessages(returnedMessages));
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    private void responseMessages(List<Message> returnedMessages) {
        Toast.makeText(this, "Message sent!", Toast.LENGTH_LONG).show();
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

    private void setUpNavDrawer() {
        drawerLayout = findViewById(R.id.nav_drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (user.getRewards().getHasColorScheme1() && user.getRewards().getHasColorScheme2()) {
            navigationView.setBackgroundResource(R.color.darkGold);
        }
        else if (user.getRewards().getHasColorScheme1() && !user.getRewards().getHasColorScheme2()) {
            navigationView.setBackgroundResource(R.color.green);
        }


        //Updates the Navigation header to display user's name and email
//        updateNavHeader(navigationView);

        navigationView.setNavigationItemSelectedListener(
                new NavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        //set item as selected to persist highlight
                        item.setChecked(true);

                        //Close drawer when item tapped
                        drawerLayout.closeDrawers();

                        //Go to the activity corresponding to the selected item
                        int id = item.getItemId();
                        Intent intent;

                        switch (id) {
                            case R.id.my_profile:
                                intent = ViewProfileActivity.makeIntent(MainMapActivity.this);
                                intent.putExtra(USER_ID, server.getUser().getId());
                                startActivity(intent);
                                break;
                            case R.id.points_store:
                                intent = PointsStoreActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.my_groups:
                                intent = CurrentGroupsActivity.makeIntent(MainMapActivity.this);
                                startActivityForResult(intent, REQUEST_CODE_CURRENT_GROUP);
                                break;
                            case R.id.messages:
                                intent = MessagesActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.permission_requests:
                                intent = PermissionRequestsActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.parent_dashboard:
                                intent = ParentDashboardActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.users_monitoring_me:
                                intent = MonitoringMeActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.users_I_monitor:
                                intent = MonitoringOthersActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.leaderboard:
                                intent = LeaderboardActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                break;
                            case R.id.logout:
                                userPrefs = MainMapActivity.this.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
                                SharedPreferences.Editor editor = userPrefs.edit();
                                editor.putString(SERVER_TOKEN, null);
                                editor.putString(USER_EMAIL, null);
                                editor.apply();
                                intent = LoginActivity.makeIntent(MainMapActivity.this);
                                startActivity(intent);
                                finish();
                                break;
                        }
                        return true;
                    }
                }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //called when activity finishes
        if (resultCode == Activity.RESULT_CANCELED) {
            return;
        }
        switch (requestCode) {
            case REQUEST_CODE_CREATE_NEW_GROUP:
                // update the local list of groups
                myMap.clear();
                createdGroup = CreateNewGroupActivity.getGroupFromIntent(data);
                displayGroupsOnMap();
                break;
            case REQUEST_CODE_CURRENT_GROUP:
                // Start or Stop Runnable, and set active group destination
                double destinationLat = data.getDoubleExtra(DESTINATION_LAT, 0);
                double destinationLng = data.getDoubleExtra(DESTINATION_LNG, 0);
                destinationLatLng = new LatLng(destinationLat, destinationLng);
                runnableStatus = CurrentGroupsActivity.getRunnableStatusFromIntent(data);

                if (runnableStatus == RUNNABLE_STATUS_ACTIVE) {
                    runnableCode = new MyRunnable();
                    handler.post(runnableCode);
                    isRewarded = false;
                } else if (runnableStatus == RUNNABLE_STATUS_INACTIVE) {
                    runnableCode.killRunnable();
                }
                break;
        }
    }

    private void setUpAddNewGroupButton() {
        FloatingActionButton btnAdd = findViewById(R.id.btnAdd);
        btnAdd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = CreateNewGroupActivity.makeLaunchIntent(MainMapActivity.this);
                startActivityForResult(intent, REQUEST_CODE_CREATE_NEW_GROUP);
            }
        });
    }

    private void sendCurrentLocationToServer() {
        clientLocation = LocationServices.getFusedLocationProviderClient(this);
        try {

            final Task Location = clientLocation.getLastLocation();
            Location.addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        android.location.Location currLocation = (android.location.Location) task.getResult();
                        currentLatLng = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());

                        double distanceToDestination = checkDistanceToDestination(currentLatLng, destinationLatLng);

                        //user within 100 meters of destination
                        if (distanceToDestination < 0.1) {
                            // reward the user with points
                            // in the future do a check on the original distance to destination to determine how many points to reward the user with
                            if (!isRewarded) {
                                rewardedPoints = 150;
                                Integer currentPoints = user.getCurrentPoints() + rewardedPoints;
                                user.setCurrentPoints(currentPoints);
                                Integer totalPointsEarned = user.getTotalPointsEarned() + rewardedPoints;
                                user.setTotalPointsEarned(totalPointsEarned);
                                Call<User> caller = server.getProxy().editUser(user.getId(), user);
                                ProxyBuilder.callProxy(MainMapActivity.this, caller, returnedUser -> responseEditedUser(returnedUser));
                                isRewarded = true;
                            }

                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    runnableCode.killRunnable();
//                                    Toast.makeText(getApplicationContext(), "You've reached your destination, location tracking stopped", Toast.LENGTH_SHORT).show();
                                }
                            }, 600000); //600000


                        }

//                        Toast.makeText(getApplicationContext(), "Get location called", Toast.LENGTH_SHORT).show();

                        // Test sample (will need to upload to server every 30 seconds)
                        Date currentDateTime = getCurrentDateTimeDateObject();
                        myGpsLocation.setTimestamp(currentDateTime);
                        myGpsLocation.setLat(currLocation.getLatitude());
                        myGpsLocation.setLng(currLocation.getLongitude());

                        //send current gps location to the server
                        server.setLastGpsLocation(server.getUser().getId(), myGpsLocation);
//

                    } else {
                        Toast.makeText(getApplicationContext(), "Get and Send Location Failed", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } catch (SecurityException e) {
            Log.e(TAG, "Security Exception: " + e.getMessage());
        }
    }

    private void responseEditedUser(User returnedUser) {
        server.setUser(returnedUser);

        Toast.makeText(this, "edited user: " + returnedUser.toString(), Toast.LENGTH_SHORT).show();
        Log.w(TAG, "Server replied with user: " + returnedUser.toString());
    }

    private Date getCurrentDateTimeDateObject() {

        Date currentDateTime = null;
        Calendar calendar = Calendar.getInstance();
        @SuppressLint("SimpleDateFormat") DateFormat simpleDateFormat = new SimpleDateFormat(getString(R.string.Date_Time_Format));
        String dateTime = simpleDateFormat.format(calendar.getTime());
        try {
            currentDateTime = simpleDateFormat.parse(dateTime);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return currentDateTime;
    }

    // taken from : https://stackoverflow.com/questions/14394366/find-distance-between-two-points-on-map-using-google-map-api-v2
    private double checkDistanceToDestination(LatLng currentLatLng, LatLng destinationLatLng) {
        int Radius = 6371;// radius of earth in Km
        double lat1 = currentLatLng.latitude;
        double lat2 = destinationLatLng.latitude;
        double lon1 = currentLatLng.longitude;
        double lon2 = destinationLatLng.longitude;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1))
                * Math.cos(Math.toRadians(lat2)) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        double c = 2 * Math.asin(Math.sqrt(a));
        double valueResult = Radius * c;
        double km = valueResult / 1;
        DecimalFormat newFormat = new DecimalFormat("####");
        int kmInDec = Integer.valueOf(newFormat.format(km));
        double meter = valueResult % 1000;
        int meterInDec = Integer.valueOf(newFormat.format(meter));
        Log.i("Radius Value", "" + valueResult + "   KM  " + kmInDec
                + " Meter   " + meterInDec);

        return Radius * c;
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Add menu icon to toolbar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setHomeAsUpIndicator(R.drawable.ic_menu);
    }

    private void initialMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    private void getAllGroupsOnServer() {
        Call<List<Group>> caller = server.getProxy().getGroups();
        ProxyBuilder.callProxy(context, caller, returnedGroups -> responseGroups(returnedGroups));
    }

    private void responseGroups(List<Group> returnedGroups) {
        Log.w(TAG, "Recieved all Groups");
        groups.clear();

        for(int i = 0; i < returnedGroups.size(); i ++){
            if(returnedGroups.get(i).getLeader() != null){
                groups.add(returnedGroups.get(i));
            }
        }
        displayGroupsOnMap();
    }

    private void displayGroupsOnMap() {
        double groupLat;
        double groupLng;
        String groupTitle;
        Log.d(TAG, "server has the following number of groups:" + groups.size());
        for (int i = 0; i < groups.size(); i++) {
            groupLat = groups.get(i).getRouteLatArray()[0];
            groupLng = groups.get(i).getRouteLngArray()[0];
            groupTitle = groups.get(i).getGroupDescription();

            LatLng groupLatlng = new LatLng(groupLat, groupLng);

            if(user.getRewards().getHasMapIcon()) {
                myMap.addMarker(new MarkerOptions().position(groupLatlng).title(groupTitle).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)));
            }else{
                myMap.addMarker(new MarkerOptions().position(groupLatlng).title(groupTitle).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
            }
        }
    }

    private void getCurrentUsers() {
        Call<List<User>> caller = server.getProxy().getUsers();
        ProxyBuilder.callProxy(context, caller, returnedUsers -> responseUsers(returnedUsers));
    }

    private void responseUsers(List<User> returnedUsers) {
        Log.w(TAG, "All Users:");
        for (User user : returnedUsers) {
            Log.w(TAG, "User: " + user.toString());
            users.add(user);
        }
    }

    private void getMonitorsUsers(long userId) {
        Call<List<User>> caller = server.getProxy().getMonitorsUsers(userId);
        ProxyBuilder.callProxy(context, caller, returnedMonitoredUsers -> responseMonitoredUsersList(returnedMonitoredUsers));
    }

    private void responseMonitoredUsersList(List<User> returnedMonitoredUsers) {
        Log.w(TAG, "Monitored Users:");
        for (User user : returnedMonitoredUsers) {
            monitoredUsers.add(user);
            Log.w(TAG, "User: " + user.toString());
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        locationPermission = false;
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            locationPermission = false;
                            return;
                        }
                    }
                    locationPermission = true;
                    initialMap();
                }
            }
        }
    }

    public void getCurrentLocation() {
        clientLocation = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (locationPermission) {
                final Task Location = clientLocation.getLastLocation();
                Location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            android.location.Location currLocation = (Location) task.getResult();
                            LatLng currLatlong = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());
                            moveCamera(currLatlong, 15f);
                        } else {
                            Toast.makeText(getApplicationContext(), "location failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            } else {
                Toast.makeText(getApplicationContext(), "no permission", Toast.LENGTH_SHORT).show();
            }
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
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER
                        || keyEvent.getAction() == KeyEvent.ACTION_MULTIPLE) {
                    geoLocate();
                }
                return false;
            }
        });
    }

    private void geoLocate() {

        String searchAddress = editSearch.getText().toString();
        Geocoder geocoder = new Geocoder(MainMapActivity.this);
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
            //Marker groupMarker = myMap.addMarker(new MarkerOptions().position(addressLL));
        }
    }


    private void onGroupClick() {

        myMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {

                double desLat = 0;
                double desLng = 0;

                double groupLat;
                double groupLng;
                // Determine the group that's clicked by comparing it's lat and lng with the marker's lat and lng.
                for (int i = 0; i < groups.size(); i++) {

                    groupLat = groups.get(i).getRouteLatArray()[0];
                    groupLng = groups.get(i).getRouteLngArray()[0];

                    if ((groupLat == marker.getPosition().latitude) && (groupLng == marker.getPosition().longitude)) {

                        clickedGroup = groups.get(i);
                        desLat = groups.get(i).getRouteLatArray()[1];
                        desLng = groups.get(i).getRouteLngArray()[1];
                        break;
                    }
                }
                String addressDes = setAddress(desLat, desLng);

                final long userId = user.getId();


                if (userId != clickedGroup.getLeader().getId()) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainMapActivity.this);
                    builder.setTitle(clickedGroup.getGroupDescription())
                            .setMessage(getString(R.string.group_going_to) + addressDes + getString(R.string.join_or_leave))
                            .setPositiveButton(R.string.join, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    joinGroup(clickedGroup, user);
                                }
                            })
                            .setNegativeButton("Leave", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                    leaveGroup(clickedGroup, user);

                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();

                } else {
                    final EditText email = new EditText(MainMapActivity.this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.MATCH_PARENT);
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainMapActivity.this);
                    builder.setTitle(clickedGroup.getGroupDescription())
                            .setView(email)
                            .setMessage(getString(R.string.you_are_group_leader) + addressDes +
                                    getString(R.string.join_or_leave_child))
                            .setPositiveButton(R.string.join_your_child, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    User joinUser = new User();
                                    int j;
                                    for (j = 0; j < monitoredUsers.size(); j++) {
                                        if (monitoredUsers.get(j).getEmail().equals(email.getText().toString())) {
                                            joinUser = monitoredUsers.get(j);
                                            break;
                                        }
                                    }
                                    if (j < monitoredUsers.size()) {
                                        int b;
                                        for (b = 0; b < clickedGroup.getMemberUsers().size(); b++) {
                                            if (joinUser.getId() == clickedGroup.getMemberUsers().get(b).getId()) {
                                                break;
                                            }
                                        }
                                        //Toast.makeText(getApplicationContext(),"b: "+b,Toast.LENGTH_SHORT).show();
                                        if (b < clickedGroup.getMemberUsers().size()) {
                                            Toast.makeText(getApplicationContext(), R.string.child_already_in_group, Toast.LENGTH_SHORT).show();
                                        } else {


                                            Call<List<User>> caller = server.getProxy().addGroupMember(clickedGroup.getId(), joinUser, true);
                                            ProxyBuilder.callProxy(context, caller, returnedGroupMembers -> responseGroupMembers(returnedGroupMembers));


//                                            server.callAddGroupMember(clickedGroup.getId(), joinUser);
//                                            clickedGroup.addMember(joinUser);
//                                            Toast.makeText(getApplicationContext(), getString(R.string.joined_user) + joinUser.getId(), Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.did_not_monitor_user, Toast.LENGTH_SHORT).show();
                                    }

                                }
                            })
                            .setNegativeButton("Leave your child", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    User leaveUser = new User();
                                    int j;
                                    for (j = 0; j < monitoredUsers.size(); j++) {

                                        if (monitoredUsers.get(j).getEmail().equals(email.getText().toString())) {
                                            leaveUser = monitoredUsers.get(j);
                                            break;
                                        }
                                    }
                                    if (j < monitoredUsers.size()) {
                                        int b;
                                        for (b = 0; b < clickedGroup.getMemberUsers().size(); b++) {
                                            if (leaveUser.getId() == clickedGroup.getMemberUsers().get(b).getId()) {
                                                break;
                                            }
                                        }
                                        if (b < clickedGroup.getMemberUsers().size()) {

                                            Call<Void> caller = server.getProxy().removeGroupMember(clickedGroup.getId(), leaveUser.getId());
                                            ProxyBuilder.callProxy(context, caller, returnedNothing -> responseRemovedGroupMember(returnedNothing));
//                                            server.callRemoveGroupMember(clickedGroup.getId(), leaveUser.getId());
//                                            clickedGroup.deleteMember(leaveUser);
//                                            Toast.makeText(getApplicationContext(), getString(R.string.leave_user) + leaveUser.getId(), Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(getApplicationContext(), R.string.child_not_in_group, Toast.LENGTH_SHORT).show();
                                        }

                                    } else {
                                        Toast.makeText(getApplicationContext(), R.string.joined_user_null, Toast.LENGTH_SHORT).show();
                                    }

                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                }
                return false;
            }
        });
    }

    private void joinGroup(Group group, User user) {

        final EditText joinEmail = new EditText(MainMapActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainMapActivity.this);
        builder.setTitle(group.getGroupDescription())
                .setMessage(R.string.join_options)
                .setPositiveButton("Join yourself", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int a;
                        for (a = 0; a < group.getMemberUsers().size(); a++) {
                            if (user.getId() == group.getMemberUsers().get(a).getId()) break;
                        }
                        if (a < group.getMemberUsers().size()) {
                            Toast.makeText(getApplicationContext(), R.string.already_in_group, Toast.LENGTH_SHORT).show();
                        } else {

                            Call<List<User>> caller = server.getProxy().addGroupMember(group.getId(), user, true);
                            ProxyBuilder.callProxy(context, caller, returnedGroupMembers -> responseGroupMembers(returnedGroupMembers));

//                            clickedGroup.addMember(user);
                        }
                    }
                })
                .setView(joinEmail)
                .setNegativeButton("Join your child", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        User joinUser = new User();
                        int j;
                        for (j = 0; j < monitoredUsers.size(); j++) {
                            if (monitoredUsers.get(j).getEmail().equals(joinEmail.getText().toString())) {
                                joinUser = monitoredUsers.get(j);
                                break;
                            }
                        }
                        if (j < monitoredUsers.size()) {
                            int b;
                            for (b = 0; b < group.getMemberUsers().size(); b++) {
                                if (joinUser.getId() == group.getMemberUsers().get(b).getId()) {
                                    break;
                                }
                            }
                            if (b < group.getMemberUsers().size()) {
                                Toast.makeText(getApplicationContext(), R.string.child_already_in_group, Toast.LENGTH_SHORT).show();
                            } else {
                                Call<List<User>> caller = server.getProxy().addGroupMember(group.getId(), joinUser, true);
                                ProxyBuilder.callProxy(context, caller, returnedGroupMembers -> responseGroupMembers(returnedGroupMembers));
//                                group.addMember(joinUser);
//                                Toast.makeText(getApplicationContext(), "" + R.string.joined_user + joinUser.getId(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.did_not_monitor_user, Toast.LENGTH_SHORT).show();
                        }
                    }

                });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void responseGroupMembers(List<User> returnedGroupMembers) {
        Toast.makeText(this, "request sent to group leader", Toast.LENGTH_SHORT).show();
    }

    private void leaveGroup(Group group, User user) {
        final EditText leaveEmail = new EditText(MainMapActivity.this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        AlertDialog.Builder builder = new AlertDialog.Builder(MainMapActivity.this);
        builder.setTitle(group.getGroupDescription())
                .setMessage(R.string.leave_options)
                .setPositiveButton("Leave yourself", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int a;
                        for (a = 0; a < group.getMemberUsers().size(); a++) {
                            if (user.getId() == group.getMemberUsers().get(a).getId()) break;
                        }
                        if (a < group.getMemberUsers().size()) {
                            Call<Void> caller = server.getProxy().removeGroupMember(group.getId(), user.getId(), true);
                            ProxyBuilder.callProxy(context, caller, returnedNothing -> responseRemovedGroupMember(returnedNothing));

                            clickedGroup.deleteMember(user);
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.you_are_not_in_the_group, Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setView(leaveEmail)
                .setNegativeButton("Leave my child", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        User leaveUser = new User();
                        int j;
                        for (j = 0; j < monitoredUsers.size(); j++) {

                            if (monitoredUsers.get(j).getEmail().equals(leaveEmail.getText().toString())) {
                                leaveUser = monitoredUsers.get(j);
                                break;
                            }
                        }
                        if (j < monitoredUsers.size()) {
                            int b;
                            for (b = 0; b < group.getMemberUsers().size(); b++) {
                                if (leaveUser.getId() == group.getMemberUsers().get(b).getId()) {
                                    break;
                                }
                            }
                            if (b < group.getMemberUsers().size()) {


                                Call<Void> caller = server.getProxy().removeGroupMember(group.getId(), user.getId(), true);
                                ProxyBuilder.callProxy(context, caller, returnedNothing -> responseRemovedGroupMember(returnedNothing));

//                                server.callRemoveGroupMember(group.getId(), leaveUser.getId());
//                                group.deleteMember(leaveUser);
//                                Toast.makeText(getApplicationContext(), "" + R.string.leave_user + leaveUser.getId(), Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(getApplicationContext(), "" + R.string.child_not_in_group, Toast.LENGTH_SHORT).show();
                            }

                        } else {
                            Toast.makeText(getApplicationContext(), "" + R.string.joined_user_null, Toast.LENGTH_SHORT).show();
                        }

                    }
                });

        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void responseRemovedGroupMember(Void returnedNothing) {
    }


    private String setAddress(double lat, double lng) {
        // Converts lat and lng to an Address string
        Geocoder geocoder;
        List<Address> addresses = new ArrayList<Address>();
        geocoder = new Geocoder(this, Locale.getDefault());

        try {
            addresses = geocoder.getFromLocation(lat, lng, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }

        String address = addresses.get(0).getAddressLine(0);

        return address;
    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, MainMapActivity.class);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //Opens the navigation drawer when menu icon tapped
        switch (item.getItemId()) {
            case android.R.id.home:
                drawerLayout.openDrawer(GravityCompat.START);
                return true;

            case R.id.message_parents:
                EditText messageToParentsET = new EditText(MainMapActivity.this);
                messageToParentsET.setHint(R.string.message_parents_and_leaders);
                AlertDialog.Builder builder = new AlertDialog.Builder(MainMapActivity.this);
                builder.setView(messageToParentsET)
                        .setTitle(R.string.message_parents_and_leaders);

                builder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Message messageToParents = new Message();

                        messageToParents.setText(messageToParentsET.getText().toString());
                        messageToParents.setEmergency(false);

                        Call<List<Message>> caller = server.getProxy().newMessageToParentsOf(server.getUser().getId(), messageToParents);
                        ProxyBuilder.callProxy(MainMapActivity.this, caller, returnedMessages -> responseMessages(returnedMessages));
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.show();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


}