package jade.sfu.walkinggroup.app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.GpsLocation;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Message;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

// Gives you the map view that shows where your child and it's leaders are located
public class ParentDashboardActivity extends AppCompatActivity implements OnMapReadyCallback {

    public static final String USER_ID = "userId";
    private Server server;
    private User user;
    private Boolean locationPermission;
    private FusedLocationProviderClient clientLocation;
    private GoogleMap myMap;
    private GpsLocation myGpsLocation;
    private List<User> monitorsUsers = new ArrayList<>();
    private static final String TAG = "ParentDashboardActivity";
    private Context context;
    private TextView numberOfUnreadMessages;
    private List<Group> currentMonitoredUserGroupList = new ArrayList<>();
    private User currentMonitoredUserGroupLeader;
    private MyRunnable runnableCode; // = new MyRunnable();
    private Handler handler = new Handler();
    private String monitoredUserName;
    private String groupLeaderName;
    private String groupDescription;
    String newLine = System.getProperty("line.separator");

    private TextView unreadMessagesNumber;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parent_dashboard);

        getLocationPermission();

        server = Server.getInstance(this, TAG);
        user = server.getUser();


        // start getting location of monitored users every 30 seconds
        runnableCode = new MyRunnable();
        handler.post(runnableCode);

        numberOfUnreadMessages = (TextView) findViewById(R.id.unread_messages_number);

        myGpsLocation = GpsLocation.getInstance();

        setupViewMonitoredUsersGroupsButton();
        setUpToolBar();
    }

    //Code inspired by: https://stackoverflow.com/questions/43194243/notification-badge-on-action-item-android

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.parent_dashboard_menu, menu);

        final MenuItem menuItem = menu.findItem(R.id.unread_messages);

        View actionView = MenuItemCompat.getActionView(menuItem);
        unreadMessagesNumber = (TextView) actionView.findViewById(R.id.unread_messages_number);
        unreadMessagesNumber.setVisibility(View.GONE);

        setupBadge();

        actionView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new MessagesActivity().makeIntent(ParentDashboardActivity.this);
                intent.putExtra(USER_ID, user.getId());
                startActivity(intent);
            }
        });

        return true;
    }


    private void setupBadge() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Call<List<Message>> caller = server.getProxy().getUnreadMessages(server.getUser().getId(),
                        null);

                ProxyBuilder.callProxy(ParentDashboardActivity.this, caller, returnedMessages -> responseMessages(returnedMessages));
                handler.postDelayed(this, 60000);
            }
        }, 0);
    }

    private void responseMessages(List<Message> returnedMessages) {
        if (returnedMessages.size() == 0) {
            if (unreadMessagesNumber.getVisibility() != View.GONE)
                unreadMessagesNumber.setVisibility(View.GONE);
        } else {
            if (unreadMessagesNumber.getVisibility() != View.VISIBLE)
                unreadMessagesNumber.setVisibility(View.VISIBLE);
            unreadMessagesNumber.setText("" + returnedMessages.size());
        }
    }


    // Inspired by: https://stackoverflow.com/questions/5844308/removecallbacks-not-stopping-runnable/51098789#51098789
    public class MyRunnable implements Runnable {

        private boolean stopTask = false;

        public void run() {
            if (stopTask) {
                return;
            }
            getMonitoredUsersLocationFromServer();
            Toast.makeText(ParentDashboardActivity.this, "Updated location of monitored users", Toast.LENGTH_SHORT).show();
            handler.postDelayed(runnableCode, 30000);
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
    protected void onPostResume() {
        super.onPostResume();
        setupBadge();
    }


    private void setupViewMonitoredUsersGroupsButton() {
        Button button = (Button) findViewById(R.id.btnViewMonitoredUsersGroups);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = GroupsOfMonitoredUsersActivity.makeIntent(ParentDashboardActivity.this);
                intent.putExtra(USER_ID, user.getId());
                startActivity(intent);
            }
        });
    }


    private void getMonitoredUsersLocationFromServer() {
        //test if able to be gotten from local data, if not, more calls to server need to be made to get the leader
        myMap.clear();
        monitorsUsers = user.getMonitorsUsers();
        for (User monitoredUser : monitorsUsers) {

            monitoredUserName = monitoredUser.getName();
            // retrieve monitored user gps location from server
            Call<GpsLocation> monitoredUserCaller = server.getProxy().getLastGpsLocation(monitoredUser.getId());
            ProxyBuilder.callProxy(this, monitoredUserCaller, returnedGPSLocation -> responseMonitoredUserGPSLocation(returnedGPSLocation));

        }
    }


    private void responseMonitoredUserGPSLocation(GpsLocation returnedGPSLocation) {


        // if the user has not uploaded their location, don't show it.
        if (returnedGPSLocation.getTimestamp() != null) {
            double monitoredUserLat = returnedGPSLocation.getLat();
            double monitoredUserLng = returnedGPSLocation.getLng();

            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_to_string_format));
            Date gpsLocationDate = returnedGPSLocation.getTimestamp();
            String gpsLocationDateString = dateFormat.format(gpsLocationDate);

            LatLng monitoredUserLatLng = new LatLng(monitoredUserLat, monitoredUserLng);
            // update the location marker on the map
            myMap.addMarker(new MarkerOptions().position(monitoredUserLatLng).title(monitoredUserName + "-----" + gpsLocationDateString).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)));//.showInfoWindow(); need to show for all markers not just latest one
        }

        //possibly need to subtract current time with timestamp back from the server if time in minutes since last active time is needed

//        Toast.makeText(this, returnedGPSLocation.toString(), Toast.LENGTH_LONG).show();

        for (User monitoredUser : monitorsUsers) {
            // retrieve monitored user's group Leader gps location from server
            // get groups monitored user is member of
            currentMonitoredUserGroupList = monitoredUser.getMemberOfGroups();
            for (Group currentMonitoredUserGroup : currentMonitoredUserGroupList) {
                // get leader location of each group
                currentMonitoredUserGroupLeader = currentMonitoredUserGroup.getLeader();
                groupLeaderName = currentMonitoredUserGroupLeader.getName();
//                groupDescription = currentMonitoredUserGroup.getGroupDescription();
                Call<GpsLocation> monitoredUserGroupLeaderCaller = server.getProxy().getLastGpsLocation(currentMonitoredUserGroupLeader.getId());
                ProxyBuilder.callProxy(this, monitoredUserGroupLeaderCaller, returnedLeaderGPSLocation -> responseGroupLeaderGPSLocation(returnedLeaderGPSLocation));

            }
        }
    }


    private void responseGroupLeaderGPSLocation(GpsLocation returnedLeaderGPSLocation) {

        // if the user has not uploaded their location, don't show it.
        if (returnedLeaderGPSLocation.getTimestamp() != null) {
            double monitoredUserLat = returnedLeaderGPSLocation.getLat();
            double monitoredUserLng = returnedLeaderGPSLocation.getLng();

            @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat(getString(R.string.date_to_string_format));
            Date gpsLocationDate = returnedLeaderGPSLocation.getTimestamp();
            String gpsLocationDateString = dateFormat.format(gpsLocationDate);

            LatLng monitoredUserLatLng = new LatLng(monitoredUserLat, monitoredUserLng);
            // update the location marker on the map
            myMap.addMarker(new MarkerOptions().position(monitoredUserLatLng).title(groupLeaderName + "-----" + gpsLocationDateString).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));//.showInfoWindow(); need to show for all markers not just latest one
        }

//        Toast.makeText(this, returnedLeaderGPSLocation.toString(), Toast.LENGTH_LONG).show();

    }

    @Override
    public void onMapReady(GoogleMap map) {
        myMap = map;
        if (locationPermission) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            myMap.setMyLocationEnabled(true);
            LatLng initialLocation = new LatLng(49.2827, -123.1207);
            moveCamera(initialLocation, 10f);
        }
        myMap.getUiSettings().setMyLocationButtonEnabled(false);
    }


//
//    private void responseGPSLocation(GpsLocation returnedGPSLocation) {
//
//        Toast.makeText(this, returnedGPSLocation.toString(), Toast.LENGTH_LONG).show();
//
//    }


    private void moveCamera(LatLng latLng, float zoom) {
        myMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
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

    private void initialMap() {
        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapParentDashboard);
        mapFragment.getMapAsync(this);
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.parent_dashboard_toolbar);
        toolbar.setTitle(R.string.parent_dashboard);
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

    public static Intent makeIntent(Context context) {
        return new Intent(context, ParentDashboardActivity.class);
    }
}
