package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
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
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;


// displays all the users I monitor and add or remove from the list
public class MonitoringOthersActivity extends AppCompatActivity {
    public static final String TAG = "MonitoringOthersActivity";
    public static final String HYPHEN = " ------ ";
    List<String> usersMonitoredStringList = new ArrayList<>();
    private Server server;
    public static final String USER_ID = "userId";
    private ListView usersMonitoredListView;
    ArrayAdapter<String> adapter;
    private EditText inputEmail;
    private List<String> newStringList;
    private int position;
    private CheckBox removeMonitoredUserCheckbox;
    private TextView mRemoveMonitoredUserTextView;
    private boolean mRemoveMonitoredUserIsChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring_others);
        usersMonitoredListView = (ListView) findViewById(R.id.usersMonitoredList);
        inputEmail = (EditText) findViewById(R.id.addUserEmailEditText);


        server = Server.getInstance(MonitoringOthersActivity.this, TAG);

        populateListView();

        setupRemoveButton();
        setupAddButton();
        setUpToolBar();
    }

    private void populateListView() {

        Call<List<User>> caller = server.getProxy().getMonitorsUsers(server.getUser().getId());
        ProxyBuilder.callProxy(this, caller, returnedMonitoredUsers -> responseUserList(returnedMonitoredUsers));

    }


    private void setupRemoveButton() {

        removeMonitoredUserCheckbox = (CheckBox) findViewById(R.id.remove_monitored_user_checkBox);
        mRemoveMonitoredUserTextView = (TextView) findViewById(R.id.remove_monitored_user_instructions);
        mRemoveMonitoredUserTextView.setVisibility(View.INVISIBLE);
        removeMonitoredUserCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Delete Button is checked
                //show or hide instructions
                mRemoveMonitoredUserIsChecked = isChecked;
                if (isChecked) {
                    mRemoveMonitoredUserTextView.setVisibility(View.VISIBLE);
                } else {
                    mRemoveMonitoredUserTextView.setVisibility(View.INVISIBLE);
                }
            }
        });

        usersMonitoredListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {

                if (mRemoveMonitoredUserIsChecked) {
                    MonitoringOthersActivity.this.position = position;
                    long childId = server.getUser().getMonitorsUsers().get(position).getId();
                    Call<Void> caller = server.getProxy().removeFromMonitorsUsers(server.getUser().getId(), childId);
                    ProxyBuilder.callProxy(MonitoringOthersActivity.this, caller, returnedNothing -> responseRemovedMonitoredUser(returnedNothing));
                } else {
                    long childId = server.getUser().getMonitorsUsers().get(position).getId();
                    Intent intent = ViewProfileActivity.makeIntent(MonitoringOthersActivity.this);
                    intent.putExtra(USER_ID, childId);
                    startActivity(intent);
                }
            }
        });
    }

    private void responseRemovedMonitoredUser(Void returnedNothing) {
//        notifyUserViaLogAndToast("Removed monitored user");
        newStringList.remove(position);
        adapter = new ArrayAdapter<String>(MonitoringOthersActivity.this,
                R.layout.monitor_layout, //layout to use
                newStringList);//Item to be display
        usersMonitoredListView.setAdapter(adapter);


    }


    private void setupAddButton() {

        Button addButton = (Button) findViewById(R.id.AddButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = inputEmail.getText().toString();

                // verify user with useremail exists on server
                // Make call
                Call<User> caller = server.getProxy().getUserByEmail(email);
                ProxyBuilder.callProxy(MonitoringOthersActivity.this, caller, returnedUser -> responseUser(returnedUser));
            }
        });
    }

    private void responseUser(User user) {
        //user exists on server
        Call<List<User>> caller = server.getProxy().addToMonitorsUsers(server.getUser().getId(), user,true);
        ProxyBuilder.callProxy(this, caller, returnedMonitoredUsers -> responseUserList(returnedMonitoredUsers));
//        Toast.makeText(this,"Server replied with user: " + user.toString(), Toast.LENGTH_LONG).show();

    }

    private void responseUserList(List<User> returnedMonitoredUsers) {


        newStringList = new ArrayList<>();
//        notifyUserViaLogAndToast("Got list of " + returnedMonitoredUsers.size() + " Users! See logcat.");
        for (User user : returnedMonitoredUsers) {
            newStringList.add(user.getName() + HYPHEN + user.getEmail());
        }
        adapter = new ArrayAdapter<String>(MonitoringOthersActivity.this,
                R.layout.monitor_layout, //layout to use
                newStringList);//Item to be display
        usersMonitoredListView.setAdapter(adapter);

        // call server to get updated returned monitored Users with increased depth
        Call<List<User>> caller = server.getProxy().getMonitorsUsers(server.getUser().getId(), Long.valueOf(3));
        ProxyBuilder.callProxy(this, caller, returnedUpdatedMonitoredUsers -> updateServerMonitoredUsers(returnedUpdatedMonitoredUsers));

    }

    private void updateServerMonitoredUsers(List<User> returnedMonitoredUsers) {
        // update current Monitored Users locally
        server.getUser().setMonitorsUsers(returnedMonitoredUsers);
    }


    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.monitoring_others_toolbar);
        toolbar.setTitle(R.string.monitoring_others);
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
        return new Intent(context, MonitoringOthersActivity.class);
    }
}
