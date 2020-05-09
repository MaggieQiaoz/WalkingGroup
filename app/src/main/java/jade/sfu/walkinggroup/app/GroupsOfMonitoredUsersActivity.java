package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;


// Displays the groups that your child is in
public class GroupsOfMonitoredUsersActivity extends AppCompatActivity {

    private static final String USER_ID = "userId";
    private static final String TAG = "MonitoredUsersGroups";
    public static final String GROUP_ID = "groupId";
    private List<Group> currentMonitoredUserGroupList;
    private List<User> monitoredUsersList;
    private Long userId;
    private Server server;
    private ListView listOfGroupsListView;
    private ArrayAdapter<String> adapter;
    List<String> listOfGroupNames;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_groups_of_monitored_users);

        userId = getIntent().getLongExtra(USER_ID, 0);
        server = Server.getInstance(GroupsOfMonitoredUsersActivity.this, TAG);
        listOfGroupsListView = findViewById(R.id.groupsListView);
        listOfGroupNames = new ArrayList<>();

        Call<List<User>> caller = server.getProxy().getMonitorsUsers(userId, Long.valueOf(1));
        ProxyBuilder.callProxy(this, caller, returnedMonitoredUsers -> responseUserList(returnedMonitoredUsers));
    }

    private void responseUserList(List<User> returnedMonitoredUsers) {
        monitoredUsersList = returnedMonitoredUsers;
        Spinner childListSpinner = (Spinner) findViewById(R.id.childListSpinner);

        List<String> listOfChildrenNames = new ArrayList<>();

        for (User child : monitoredUsersList) {
            listOfChildrenNames.add(child.getName());
        }

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, listOfChildrenNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        childListSpinner.setAdapter(adapter);

        childListSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(final AdapterView<?> parent, final View view,
                                       int position, long id) {
                listOfGroupNames.clear();
                currentMonitoredUserGroupList = new ArrayList<>();
                currentMonitoredUserGroupList = monitoredUsersList.get(position).getMemberOfGroups();

                for (Group group : currentMonitoredUserGroupList) {
                    listOfGroupNames.add(group.getGroupDescription());
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<String>(GroupsOfMonitoredUsersActivity.this,
                        R.layout.group_layout, //layout to use
                        listOfGroupNames);//Item to be display
                listOfGroupsListView.setAdapter(adapter);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        listOfGroupsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Long groupId = currentMonitoredUserGroupList.get(position).getId();

                Intent intent = DisplayMembersOfGroupActivity.makeIntent(GroupsOfMonitoredUsersActivity.this);
                intent.putExtra(GROUP_ID, groupId);
                startActivity(intent);
            }
        });
    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, GroupsOfMonitoredUsersActivity.class);
    }
}
