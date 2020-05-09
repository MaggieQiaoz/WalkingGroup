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
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

/*
    Create a separate Read Only Monitoring Me Activity so group leaders and members can see the
    users who monitor a selected user, but are not able to add or remove to the list of users who
    monitor the selected user.
 */
public class ReadOnlyMonitoringMeActivity extends AppCompatActivity {

    public static final String TAG = "ReadOnlyMonitoredBy";
    public static final String HYPHEN = " ------ ";
    private Server server;
    public static final String USER_ID = "userId";
    private ListView usersMonitoredByListView;
    ArrayAdapter<String> adapter;
    private List<String> monitoredByUsersStringList;
    private List<User> monitoredByUsers;
    private Long userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_read_only_monitoring_me);

        userId = getIntent().getLongExtra(USER_ID, 0);
        usersMonitoredByListView = (ListView) findViewById(R.id.usersMonitoringMeList);
        server = Server.getInstance(this, TAG);

        setUpToolBar();
        populateListView();
    }

    private void populateListView() {
        Call<List<User>> caller = server.getProxy().getMonitoredByUsers(userId);
        ProxyBuilder.callProxy(this, caller, returnedMonitoredByUsers -> responseUserList(returnedMonitoredByUsers));

    }

    private void responseUserList(List<User> returnedMonitoredByUsers) {

        monitoredByUsers = returnedMonitoredByUsers;
        monitoredByUsersStringList = new ArrayList<>();

        for (User user : returnedMonitoredByUsers) {
            monitoredByUsersStringList.add(user.getName() + HYPHEN + user.getEmail());
        }

        adapter = new ArrayAdapter<String>(ReadOnlyMonitoringMeActivity.this,
                R.layout.monitor_layout, //layout to use
                monitoredByUsersStringList);//Item to be display
        usersMonitoredByListView.setAdapter(adapter);

        usersMonitoredByListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                long parentId = monitoredByUsers.get(position).getId();
                Intent intent = ViewOnlyProfileActivity.makeIntent(ReadOnlyMonitoringMeActivity.this);
                intent.putExtra(USER_ID, parentId);
                startActivity(intent);
            }
        });
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.read_only_monitoring_me_toolbar);
        toolbar.setTitle(R.string.monitored_by);
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
        return new Intent(context, ReadOnlyMonitoringMeActivity.class);
    }
}
