package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.GpsLocation;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Message;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;


// Displays all groups user is a member of and also leads as well as all the members of that group
public class CurrentGroupsActivity extends AppCompatActivity {
    public static final String DESTINATION_LAT = "destinationLat";
    public static final String DESTINATION_LNG = "destinationLng";
    public static final int RUNNABLE_STATUS_ACTIVE = 2;
    public static final int RUNNABLE_STATUS_INACTIVE = 1;
    public static final String HYPHEN_LEADER = " ------ Leader";
    public static final String HYPHEN = " ------ ";
    public static final String HYPHEN_MEMBER = " ------ Member";
    private static final String USER_ID = "userId";
    private final String TAG = "CurrentGroupsActivity";
    private Server server;
    private Spinner groupListSpinner;
    private ArrayAdapter<String> adapter;
    private Group currentGroupSelected;

    private ListView groupMemberListView;

    private List<Group> leadsGroups = new ArrayList<>();
    private List<String> membersInGroupStringList = new ArrayList<>();
    private List<Group> memberOfGroups = new ArrayList<>();
    private List<String> allGroupsMemberOrLeaderStringList = new ArrayList<>();
    private List<User> membersInGroupList = new ArrayList<>();
    private List<Group> allGroupsMemberOrLeader = new ArrayList<>();
    private String currentGroupName;
    private User currentUser;
    private static int runnableStatus; // 0: uninitialized, 1: inactive, 2: active

    private CheckBox removeGroupMemberCheckBox;
    private TextView mRemoveGroupMemberTextView;
    private boolean mRemoveGroupMemberIsChecked = false;
    private int position;
    private Intent intent;
    Switch locationSwitch;

    //Location Tracking
    private Button startLocationTrackingButton;
    private Button stopLocationTrackingButton;
    private FusedLocationProviderClient clientLocation;
    private GpsLocation myGpsLocation;
    private int testIndex;


    //    private EditText messageToGroupEditText;
    private Button updateLeaderButton;
    private Button messageGroupButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_current_group);

        setUpToolBar();

        server = Server.getInstance(this, TAG);
        myGpsLocation = GpsLocation.getInstance();

        intent = getIntent();
        groupMemberListView = (ListView) findViewById(R.id.groupsListView);
        removeGroupMemberCheckBox = (CheckBox) findViewById(R.id.remove_group_member_checkBox);
        mRemoveGroupMemberTextView = (TextView) findViewById(R.id.remove_group_member_instructions);
        mRemoveGroupMemberTextView.setVisibility(View.INVISIBLE);
        updateLeaderButton = (Button) findViewById(R.id.updateLeaderButton);
        messageGroupButton = (Button) findViewById(R.id.messageGroupBtn);

        startLocationTrackingButton = (Button) findViewById(R.id.start_location_tracking_button);
        stopLocationTrackingButton = (Button) findViewById(R.id.stop_location_tracking_button);

        Call<User> caller = server.getProxy().getUserByEmail(server.getUser().getEmail(),
                Long.valueOf(2));
        ProxyBuilder.callProxy(this, caller, returnedUser -> responseUser(returnedUser));

    }

    private void responseUser(User returnedUser) {
        currentUser = returnedUser;
        setupSpinner();
        setupRemoveButton();
        setupLocationTrackingButtons();
    }

    private void setupLocationTrackingButtons() {
        startLocationTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //might need null check to prevent repeated presses
                double destinationLat = currentGroupSelected.getRouteLatArray()[1];
                double destinationLng = currentGroupSelected.getRouteLngArray()[1];

                intent.putExtra(DESTINATION_LAT, destinationLat);
                intent.putExtra(DESTINATION_LNG, destinationLng);
                runnableStatus = RUNNABLE_STATUS_ACTIVE;
//                Toast.makeText(getApplicationContext(), "group: "+walkingGroup.getGroupDescription(), Toast.LENGTH_LONG).show();
                setResult(AppCompatActivity.RESULT_OK, intent);
                finish();
            }
        });
        stopLocationTrackingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                runnableStatus = RUNNABLE_STATUS_INACTIVE;
                setResult(AppCompatActivity.RESULT_OK, intent);
                finish();
            }
        });
    }
    private void responseNewLeader(User returnedUser, Group currentGroup) {

        currentGroup.setLeader(returnedUser);
        Call<Group> caller = server.getProxy().updateGroup(currentGroup.getId(), currentGroup,
                true);
        ProxyBuilder.callProxy(CurrentGroupsActivity.this, caller, returnedGroup -> responseGroup(returnedGroup));
    }

    private void responseGroup(Group returnedGroup) {
        Toast.makeText(CurrentGroupsActivity.this, "Request sent!", Toast.LENGTH_SHORT).show();
    }

    private void setupGroupMessageBtn(Long groupId) {
        messageGroupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(CurrentGroupsActivity.this);

                View dialogLayout = getLayoutInflater().inflate(R.layout.send_message_to_group_layout, null);
                builder.setView(dialogLayout)
                        .setTitle(R.string.send_message_to_group);

                builder.setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        CheckBox emergencyCheckBox = (CheckBox) dialogLayout.findViewById(R.id.emergency_checkBox);
                        EditText messageToGroupET = (EditText) dialogLayout.findViewById(R.id.messageToGroup);

                        Message groupMessage = new Message();
                        groupMessage.setText(messageToGroupET.getText().toString());
                        if (!emergencyCheckBox.isChecked()) {
                            groupMessage.setEmergency(false);
                        } else {
                            groupMessage.setEmergency(true);
                        }
                        Call<List<Message>> caller = server.getProxy().newMessageToGroup(groupId, groupMessage);
                        ProxyBuilder.callProxy(CurrentGroupsActivity.this, caller, returnedMessages -> responseMessages(returnedMessages));
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

//    private MyRunnable runnableCode;

    public static int getRunnableStatusFromIntent(Intent data) {
        return runnableStatus;
    }

    private void setupSpinner() {
        groupListSpinner = (Spinner) findViewById(R.id.groupListSpinner);

        leadsGroups = currentUser.getLeadsGroups();
        memberOfGroups = currentUser.getMemberOfGroups();

        for (Group group : leadsGroups) {
            allGroupsMemberOrLeaderStringList.add(group.getGroupDescription() + HYPHEN_LEADER);
            allGroupsMemberOrLeader.add(group);
        }
        for (Group group : memberOfGroups) {
            allGroupsMemberOrLeaderStringList.add(group.getGroupDescription() + HYPHEN_MEMBER);
            allGroupsMemberOrLeader.add(group);
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item,
                allGroupsMemberOrLeaderStringList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        groupListSpinner.setAdapter(adapter);

        groupListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view,
                                       int pos, long id) {
                membersInGroupStringList.clear();
                membersInGroupList.clear();

                boolean isLeader;

                currentGroupSelected = allGroupsMemberOrLeader.get(pos);
                currentGroupName = allGroupsMemberOrLeaderStringList.get(pos);

                //Display remove member and group message features only to group leaders
                if (currentGroupName.contains(HYPHEN_LEADER)) {
                    removeGroupMemberCheckBox.setVisibility(View.VISIBLE);
                    messageGroupButton.setVisibility(View.VISIBLE);
                    isLeader = true;

                } else {
                    mRemoveGroupMemberTextView.setVisibility(View.INVISIBLE);
                    removeGroupMemberCheckBox.setVisibility(View.INVISIBLE);
                    mRemoveGroupMemberIsChecked = false;
                    messageGroupButton.setVisibility(View.INVISIBLE);
                    isLeader = false;
                }
                if (currentGroupSelected.getMemberUsers().size() == 0) {
                    mRemoveGroupMemberTextView.setVisibility(View.INVISIBLE);
                    removeGroupMemberCheckBox.setVisibility(View.INVISIBLE);
                    mRemoveGroupMemberIsChecked = false;
                }

                for (User user : currentGroupSelected.getMemberUsers()) {
                    membersInGroupList.add(user);
                    membersInGroupStringList.add(user.getName() + HYPHEN + user.getEmail());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(CurrentGroupsActivity.this,
                        R.layout.group_layout, //layout to use
                        membersInGroupStringList);//Item to be display
                groupMemberListView.setAdapter(adapter);

                setupGroupMessageBtn(currentGroupSelected.getId());
                setupUpdateLeaderButton(currentGroupSelected, isLeader);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        groupMemberListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {

                if (mRemoveGroupMemberIsChecked) {
                    CurrentGroupsActivity.this.position = position;
                    Call<Void> caller = server.getProxy().removeGroupMember(currentGroupSelected.getId(), membersInGroupList.get(position).getId(),true);
                    ProxyBuilder.callProxy(CurrentGroupsActivity.this, caller, returnedNothing -> responseRemovedGroupMember(returnedNothing));

                } else {
                    Intent intent = ReadOnlyMonitoringMeActivity.makeIntent(CurrentGroupsActivity.this);
                    intent.putExtra(USER_ID, membersInGroupList.get(position).getId());
                    startActivity(intent);
                }
            }
        });
    }

    private void setupUpdateLeaderButton(Group currentGroupSelected, boolean isLeader) {
        if (isLeader) {
            updateLeaderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    EditText userEmailET = new EditText(CurrentGroupsActivity.this);
                    userEmailET.setHint(R.string.email_of_new_leader_hint);
                    AlertDialog.Builder builder = new AlertDialog.Builder(CurrentGroupsActivity.this);
                    builder.setView(userEmailET)
                            .setTitle(R.string.change_leader);

                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (!userEmailET.getText().toString().isEmpty()) {
                                Call<User> caller = server.getProxy().getUserByEmail(userEmailET.getText().toString());
                                ProxyBuilder.callProxy(CurrentGroupsActivity.this, caller, returnedUser ->
                                        responseNewLeader(returnedUser, currentGroupSelected));
                            }
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
        } else {
            updateLeaderButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    currentGroupSelected.setLeader(server.getUser());
                    Call<Group> caller = server.getProxy().updateGroup(currentGroupSelected.getId(), currentGroupSelected,
                            true);
                    ProxyBuilder.callProxy(CurrentGroupsActivity.this, caller, returnedGroup -> responseGroup(returnedGroup));
                }
            });
        }
    }

    private void setupRemoveButton() {
        removeGroupMemberCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Delete Button is checked
                //show or hide instructions
                mRemoveGroupMemberIsChecked = isChecked;
                if (isChecked) {
                    mRemoveGroupMemberTextView.setVisibility(View.VISIBLE);
                } else {
                    mRemoveGroupMemberTextView.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

    private void responseRemovedGroupMember(Void returnedNothing) {
//        membersInGroupStringList.remove(position);
//        adapter = new ArrayAdapter<String>(CurrentGroupsActivity.this,
//                R.layout.monitor_layout, //layout to use
//                membersInGroupStringList);//Item to be display
//        groupMemberListView.setAdapter(adapter);
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.current_groups_toolbar);
        toolbar.setTitle(R.string.my_groups);
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
        return new Intent(context, CurrentGroupsActivity.class);
    }
}