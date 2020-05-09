package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

public class LeaderboardActivity extends AppCompatActivity {

    public static final String TAG = "LeaderboardActivity";
    public static final String HYPHEN = " ------ ";
    private ListView leaderboardListView;
    private ArrayAdapter<String> adapter;
    private Server server;
    private List<String> userNamesList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leader_board);
        leaderboardListView = (ListView) findViewById(R.id.leaderboardListView);

        server = Server.getInstance(LeaderboardActivity.this, TAG);

        populateListView();
        setUpToolBar();

    }

    private void populateListView() {
        Call<List<User>> caller = server.getProxy().getUsers();
        ProxyBuilder.callProxy(this, caller, returnedUsersOnServer -> responseUserList(returnedUsersOnServer));
    }

    private void responseUserList(List<User> returnedUsersOnServer) {

        //sort list by total points earned descending
        // inspired by : https://stackoverflow.com/questions/8432581/how-to-sort-a-listobject-alphabetically-using-object-name-field
        if (!returnedUsersOnServer.isEmpty()) {
            Collections.sort(returnedUsersOnServer, new Comparator<User>() {
                @Override
                public int compare(User user1, User user2) {
                    return user1.getTotalPointsEarned().compareTo(user2.getTotalPointsEarned());
                }
            });
            Collections.reverse(returnedUsersOnServer);
        }


        for (User user : returnedUsersOnServer) {
            String name = user.getName();
            if (name.contains(" ")) {
                String nameParts[] = name.split(" ", 2);
                userNamesList.add(nameParts[0] + " " + nameParts[1].substring(0, 1) + "." + HYPHEN + user.getTotalPointsEarned() + " points earned");
            } else {
                userNamesList.add(user.getName() + HYPHEN + user.getTotalPointsEarned() + " points earned");
            }
        }
        adapter = new ArrayAdapter<String>(LeaderboardActivity.this,
                R.layout.leaderboard_layout, //layout to use
                userNamesList);//Item to be display
        leaderboardListView.setAdapter(adapter);

    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.leaderboard_toolbar);
        toolbar.setTitle(R.string.leaderboard);
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
        return new Intent(context, LeaderboardActivity.class);
    }
}
