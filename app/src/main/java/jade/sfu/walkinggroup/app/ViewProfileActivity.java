package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

// displays a profile that the user can edit (generic)
public class ViewProfileActivity extends AppCompatActivity {


    public static final String USER_ID = "userId";
    public static final String TAG = "ViewProfileActivity";
    private Long userId;
    private Server server;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_profile);

        userId = getIntent().getLongExtra(USER_ID, 0);
        server = Server.getInstance(ViewProfileActivity.this, TAG);

        Call<User> caller = server.getProxy().getUserById(userId);
        ProxyBuilder.callProxy(ViewProfileActivity.this, caller, returnedUser -> responseUser(returnedUser));

        setUpToolBar();
        setupEditButton();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Call<User> caller = server.getProxy().getUserById(userId);
        ProxyBuilder.callProxy(ViewProfileActivity.this, caller, returnedUser -> responseUser(returnedUser));
    }

    private void responseUser(User returnedUser) {
        setupProfileListView(returnedUser);
    }

    private void setupEditButton() {
        Button editButton = (Button) findViewById(R.id.EditButton);
        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = EditProfileActivity.makeIntent(ViewProfileActivity.this);
                intent.putExtra(USER_ID, userId);
                startActivity(intent);
            }
        });
    }

    private void setupProfileListView(User user) {
        ListView profileListView = (ListView) findViewById(R.id.viewOnlyProfileListView);

        List<String> profileStringList = new ArrayList<>();
        profileStringList.add("Name: " + user.getName());
        profileStringList.add("Email: " + user.getEmail());
        if (user.getBirthYear() != null) {
            profileStringList.add("Birth Year: " + user.getBirthYear());
        } else {
            profileStringList.add("Birth Year: ");
        }

        if (user.getBirthMonth() != null)
            profileStringList.add("Birth Month: " + user.getBirthMonth());
        else profileStringList.add("Birth Month: ");

        if (user.getAddress() != null)
            profileStringList.add("Address: " + user.getAddress());
        else
            profileStringList.add("Address: ");

        if (user.getCellPhone() != null)
            profileStringList.add("Cell Phone: " + user.getCellPhone());
        else
            profileStringList.add("Cell Phone: ");

        if (user.getHomePhone() != null)
            profileStringList.add("Home Phone: " + user.getHomePhone());
        else profileStringList.add("Home Phone: ");

        if (user.getGrade() != null)
            profileStringList.add("Grade: " + user.getGrade());
        else profileStringList.add("Grade: ");

        if (user.getTeacherName() != null)
            profileStringList.add("Teacher Name: " + user.getTeacherName());
        else profileStringList.add("Teacher Name: ");

        if (user.getEmergencyContactInfo() != null)
            profileStringList.add("Emergency Contact Info: " + user.getEmergencyContactInfo());
        else profileStringList.add("Emergency Contact Info: ");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(ViewProfileActivity.this,
                R.layout.group_layout, //layout to use
                profileStringList);//Item to be display
        profileListView.setAdapter(adapter);
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.view_profile_toolbar);
        toolbar.setTitle(R.string.profile);
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
        return new Intent(context, ViewProfileActivity.class);
    }
}
