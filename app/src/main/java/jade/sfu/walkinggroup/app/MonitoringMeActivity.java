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

// displays all the users monitoring me and add or remove from the list
public class MonitoringMeActivity extends AppCompatActivity {

    public static final String TAG = "MonitoringMeActivity";
    public static final String HYPHEN = " ------ ";
    List<String> usersMonitoredStringList = new ArrayList<>();
    private Server server;
    private ListView usersMonitoringMeListView;
    private ArrayAdapter<String> adapter;
    private EditText inputEmail;
    private List<String> newStringList;
    private int position;
    private CheckBox removeUserMonitoringMeCheckbox;
    private TextView mRemoveUserMonitoringMeTextView;
    private boolean mRemoveUserMonitoringMeIsChecked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_monitoring_me);
        usersMonitoringMeListView = (ListView) findViewById(R.id.usersMonitoringMeList);
        inputEmail = (EditText) findViewById(R.id.addUserEmailMonitoringMeEditText);

        server = Server.getInstance(MonitoringMeActivity.this, TAG);

        populateListView();

        setupRemoveButton();
        setupAddButton();
        setUpToolBar();
    }

    private void populateListView() {

        Call<List<User>> caller = server.getProxy().getMonitoredByUsers(server.getUser().getId());
        ProxyBuilder.callProxy(this, caller, returnedMonitoredByUsers -> responseUserList(returnedMonitoredByUsers));

    }


    private void setupRemoveButton() {

        removeUserMonitoringMeCheckbox = (CheckBox) findViewById(R.id.remove_user_monitoring_me_checkBox);
        mRemoveUserMonitoringMeTextView = (TextView) findViewById(R.id.remove_user_monitoring_me_instructions);
        mRemoveUserMonitoringMeTextView.setVisibility(View.INVISIBLE);
        removeUserMonitoringMeCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                //Delete Button is checked
                //show or hide instructions
                mRemoveUserMonitoringMeIsChecked = isChecked;
                if (isChecked) {
                    mRemoveUserMonitoringMeTextView.setVisibility(View.VISIBLE);
                } else {
                    mRemoveUserMonitoringMeTextView.setVisibility(View.INVISIBLE);
                }
            }
        });

        usersMonitoringMeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {

                if (mRemoveUserMonitoringMeIsChecked) {
                    MonitoringMeActivity.this.position = position;
                    long childId = server.getUser().getMonitoredByUsers().get(position).getId();
                    Call<Void> caller = server.getProxy().removeFromMonitoredByUsers(server.getUser().getId(), childId,true);
                    ProxyBuilder.callProxy(MonitoringMeActivity.this, caller, returnedNothing -> responseRemovedMonitoredByUser(returnedNothing));
                }
            }
        });
    }

    private void responseRemovedMonitoredByUser(Void returnedNothing) {
//        notifyUserViaLogAndToast("Removed monitored user");
        newStringList.remove(position);
        adapter = new ArrayAdapter<String>(MonitoringMeActivity.this,
                R.layout.monitor_layout, //layout to use
                newStringList);//Item to be display
        usersMonitoringMeListView.setAdapter(adapter);


    }


    private void setupAddButton() {

        Button addButton = (Button) findViewById(R.id.AddUserMonitoringMeButton);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                String email = inputEmail.getText().toString();

                // verify user with useremail exists on server
                // Make call
                Call<User> caller = server.getProxy().getUserByEmail(email);
                ProxyBuilder.callProxy(MonitoringMeActivity.this, caller, returnedUser -> responseUser(returnedUser));


            }
        });
    }

    private void responseUser(User user) {
        //user exists on server
        Call<List<User>> caller = server.getProxy().addToMonitoredByUsers(server.getUser().getId(), user,true);
        ProxyBuilder.callProxy(this, caller, returnedMonitoredByUsers -> responseUserList(returnedMonitoredByUsers));
//        Toast.makeText(this,"Server replied with user: " + user.toString(), Toast.LENGTH_LONG).show();

    }

    private void responseUserList(List<User> returnedMonitoredByUsers) {

        server.getUser().setMonitoredByUsers(returnedMonitoredByUsers);

        newStringList = new ArrayList<>();
//        notifyUserViaLogAndToast("Got list of " + returnedMonitoredUsers.size() + " Users! See logcat.");
        for (User user : returnedMonitoredByUsers) {
            newStringList.add(user.getName() + HYPHEN + user.getEmail());
        }
        adapter = new ArrayAdapter<String>(MonitoringMeActivity.this,
                R.layout.monitor_layout, //layout to use
                newStringList);//Item to be display
        usersMonitoringMeListView.setAdapter(adapter);
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.monitoring_me_toolbar);
        toolbar.setTitle(R.string.users_monitoring_me);
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
        return new Intent(context, MonitoringMeActivity.class);
    }
}
