package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.PermissionRequest;
import jade.sfu.walkinggroup.dataobjects.RequestsAdapter;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

public class PermissionRequestsActivity extends AppCompatActivity {

    private Server server = Server.getInstance(PermissionRequestsActivity.this, "MessageActivity");
    private RequestsAdapter requestsAdapter;
    private RecyclerView listOfPermissionsRequests;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_requests);

        listOfPermissionsRequests = (RecyclerView) findViewById(R.id.listOfAllPermissionRequests);

        setupPermissionsRequests();
        setUpToolBar();
    }

    private void setupPermissionsRequests() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {

                Call<List<PermissionRequest>> caller = server.getProxy().getPermissions(server.getUser().getId(),
                        Long.valueOf(1));
                ProxyBuilder.callProxy(PermissionRequestsActivity.this, caller, returnedRequests -> responseRequests(returnedRequests));


//                Call<User> caller = server.getProxy().getUserByEmail(server.getUser().getEmail(), Long.valueOf(1));
//                ProxyBuilder.callProxy(PermissionRequestsActivity.this, caller, returnedUser -> responseUser(returnedUser));
                handler.postDelayed(this, 60000);
            }
        }, 0);
    }


    private void responseUser(User returnedUser) {
        List<PermissionRequest> returnedRequests = returnedUser.getPendingPermissionRequests();
        requestsAdapter = new RequestsAdapter(PermissionRequestsActivity.this, returnedRequests);
        listOfPermissionsRequests.setAdapter(requestsAdapter);
        listOfPermissionsRequests.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        listOfPermissionsRequests.setLayoutManager(new LinearLayoutManager(PermissionRequestsActivity.this));
    }

    private void responseRequests(List<PermissionRequest> returnedRequests) {

        requestsAdapter = new RequestsAdapter(PermissionRequestsActivity.this, returnedRequests);
        listOfPermissionsRequests.setAdapter(requestsAdapter);
        listOfPermissionsRequests.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        listOfPermissionsRequests.setLayoutManager(new LinearLayoutManager(PermissionRequestsActivity.this));

    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.permission_requests_toolbar);
        toolbar.setTitle(R.string.permission_requests);
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
        return new Intent(context, PermissionRequestsActivity.class);
    }
}
