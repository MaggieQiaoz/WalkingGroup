package jade.sfu.walkinggroup.app;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import jade.sfu.walkinggroup.proxy.WGServerProxy;
import retrofit2.Call;

//Creates new group with group description, start and end locations
public class CreateNewGroupActivity extends AppCompatActivity {

    public static final String GROUP_DESCRIPTION = "groupDescription";
    public static final String GROUP_LAT = "groupLat";
    public static final String GROUP_LNG = "groupLng";
    public static final String DES_LAT = "desLat";
    public static final String DES_LNG = "desLng";
    public static final String DES_ADDRESS = "desAddress";
    private Button createNewGroupButton;
    private Button cancelNewGroupButton;
    private EditText groupNameEditText;
    private EditText walkingToEditText;
    private EditText meetAtEditText;
    private String addressDes;
    private double latStart;
    private double lngStart;
    private double latEnd;
    private double lngEnd;
    private static Group newGroup;
    private String groupDescription;

    private WGServerProxy proxy;
    private Server server;
    private static String TAG = "create new group";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_new_group);

        setUpGroupNameEditText();
        setUpMeetAtEditText();
        setUpWalkingToEditText();

        setUpToolBar();
        setUpCreateNewGroupButton();
        setUpCancelNewGroupButton();
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.create_new_group_toolbar);
        toolbar.setTitle(R.string.createNewWalkingGroup);
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


    private void setUpGroupNameEditText() {
        groupNameEditText = (EditText) findViewById(R.id.groupNameEditText);
    }

    private void setUpMeetAtEditText() {
        meetAtEditText = (EditText) findViewById(R.id.meetingPlaceEditText);
        meetAtEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    Intent intent = SelectMapLocationActivity.makeIntent(CreateNewGroupActivity.this);
                    startActivityForResult(intent, 1);
                }
            }
        });
    }

    private void setUpWalkingToEditText() {
        walkingToEditText = (EditText) findViewById(R.id.locationEditText);
        walkingToEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    Intent intent = SelectMapLocationActivity.makeIntent(CreateNewGroupActivity.this);
                    startActivityForResult(intent, 2);
                }
            }
        });
    }

    private void setUpCreateNewGroupButton() {
        createNewGroupButton = (Button) findViewById(R.id.createNewGroupButton);
        createNewGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                groupDescription = groupNameEditText.getText().toString();
                server = Server.getInstance(CreateNewGroupActivity.this, TAG);
                callCreateGroup(groupDescription, latStart, lngStart, latEnd, lngEnd);

            }
        });
    }

    public void callCreateGroup(String groupDescription, double latStart, double lngStart, double latEnd, double lngEnd) {

        Group group = new Group();
        group.setGroupDescription(groupDescription);
        group.setRouteLatArray(latStart, latEnd);
        group.setRouteLngArray(lngStart, lngEnd);
        group.setLeader(server.getUser());
        Call<Group> caller = server.getProxy().createGroup(group,true);
        ProxyBuilder.callProxy(this, caller, returnedGroup -> responseGroup(returnedGroup));

    }

    private void responseGroup(Group returnedGroup) {
        newGroup = returnedGroup;
        Intent intent = new Intent();

        intent.putExtra(GROUP_DESCRIPTION, groupDescription);
        intent.putExtra(GROUP_LAT, latStart);
        intent.putExtra(GROUP_LNG, lngStart);
        intent.putExtra(DES_LAT, latEnd);
        intent.putExtra(DES_LNG, lngEnd);
        intent.putExtra(DES_ADDRESS, addressDes);

        setResult(AppCompatActivity.RESULT_OK, intent);

        //startActivity();
        finish();
    }

    public static Intent makeLaunchIntent(Context context) {
        Intent intent = new Intent(context, CreateNewGroupActivity.class);
        return intent;
    }

    public static Group getGroupFromIntent(Intent data) {
        return newGroup;
    }


    // Login actually completes by calling this; nothing to do as it was all done
    // when we got the token.

    private void setUpCancelNewGroupButton() {
        cancelNewGroupButton = (Button) findViewById(R.id.cancelNewGroupButton);
        cancelNewGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                latStart = data.getDoubleExtra("lat", 0);
                lngStart = data.getDoubleExtra("lng", 0);

                String addressGroup = setAddress(latStart, lngStart);
                meetAtEditText.setText(addressGroup);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
        if (requestCode == 2) {
            if (resultCode == Activity.RESULT_OK) {
                latEnd = data.getDoubleExtra("lat", 0);
                lngEnd = data.getDoubleExtra("lng", 0);

                addressDes = setAddress(latEnd, lngEnd);
                walkingToEditText.setText(addressDes);
            }
            if (resultCode == Activity.RESULT_CANCELED) {
                //Write your code if there's no result
            }
        }
    }

    private String setAddress(double lat, double lng) {

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


}
