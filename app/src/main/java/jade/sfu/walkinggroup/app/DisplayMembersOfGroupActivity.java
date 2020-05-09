package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

// Displays member of groups
public class DisplayMembersOfGroupActivity extends AppCompatActivity {

    private static final String GROUP_ID = "groupId";
    private static final String TAG = "DisplayMembersOfGroup";
    public static final String HYPHEN = " ------ ";
    public static final String USER_ID = "userId";
    private Long groupId;
    private Server server;
    private List<User> membersInGroupList;
    private List<String> membersInGroupStringList;
    private ListView groupMembersListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_members_of_group);

        groupId = getIntent().getLongExtra(GROUP_ID, 0);
        server = Server.getInstance(DisplayMembersOfGroupActivity.this, TAG);

        groupMembersListView = (ListView) findViewById(R.id.groupMembersListView);

        Call<Group> caller = server.getProxy().getGroupById(groupId);
        ProxyBuilder.callProxy(this, caller, returnedGroup -> responseGroup(returnedGroup));

        membersInGroupList = new ArrayList<>();
        membersInGroupStringList = new ArrayList<>();
    }

    private void responseGroup(Group returnedGroup) {
        for (User user : returnedGroup.getMemberUsers()) {
            Call<User> caller = server.getProxy().getUserById(user.getId());
            ProxyBuilder.callProxy(DisplayMembersOfGroupActivity.this, caller, returnedUser -> responseMemberUser(returnedUser));
        }
    }

    private void responseMemberUser(User returnedUser) {
        membersInGroupList.add(returnedUser);
        membersInGroupStringList.add(returnedUser.getName() + HYPHEN + returnedUser.getEmail());

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(DisplayMembersOfGroupActivity.this,
                R.layout.group_layout, //layout to use
                membersInGroupStringList);//Item to be display
        groupMembersListView.setAdapter(adapter);

        groupMembersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {
                Intent intent = ReadOnlyMonitoringMeActivity.makeIntent(DisplayMembersOfGroupActivity.this);
                intent.putExtra(USER_ID, membersInGroupList.get(position).getId());
                startActivity(intent);
            }
        });
    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, DisplayMembersOfGroupActivity.class);
    }
}
