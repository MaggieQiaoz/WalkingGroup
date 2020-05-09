package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.EarnedRewards;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

public class PointsStoreActivity extends AppCompatActivity {

    public static final String TAG = "PointsStoreActivity";
    public static final String HYPHEN = " ------ ";
    public static final int COLOR_SCHEME_1 = 0;
    public static final int COLOR_SCHEME_2 = 1;
    public static final int MAP_ICON = 2;
    private ListView pointsStoreListView;
    private TextView currentPointsTextView;
    private ArrayAdapter<String> adapter;
    private Server server;
    private User user;
    private List<String> purchasableItemsList = new ArrayList<>();
    private List<Integer> purchasableItemsPricesList = new ArrayList<>();
    private EarnedRewards earnedRewards;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_points_store);

        pointsStoreListView = (ListView) findViewById(R.id.pointsStoreListView);
        server = Server.getInstance(PointsStoreActivity.this, TAG);
        user = server.getUser();
        earnedRewards = user.getRewards();
        displayCurrentPoints();
        populateListView();
        setupListViewClickListener();
        setUpToolBar();
    }

    @Override
    protected void onDestroy() {
        user.setRewards(earnedRewards);

        Call<User> caller = server.getProxy().editUser(user.getId(), PointsStoreActivity.this.user);
        ProxyBuilder.callProxy(PointsStoreActivity.this, caller, returnedUser -> responseUpdateRewards(returnedUser));

        super.onDestroy();
    }

    private void responseUpdateRewards(User returnedUser) {
        this.user = returnedUser;
        server.setUser(returnedUser);
    }

    private void setupListViewClickListener() {
        pointsStoreListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View viewClicked, int position, long id) {

                Integer priceOfItemSelected = purchasableItemsPricesList.get(position);

                switch (position) {
                    case COLOR_SCHEME_1: {
                        if (user.getCurrentPoints() >= priceOfItemSelected && !earnedRewards.getHasColorScheme1()) {
                            earnedRewards.setHasColorScheme1(true);
                            Toast.makeText(PointsStoreActivity.this, "Color Scheme 1 Purchased", Toast.LENGTH_SHORT).show();
                            user.setCurrentPoints(user.getCurrentPoints() - priceOfItemSelected);

                            //update UI
                            viewClicked.setAlpha((float) 0.4);
                            viewClicked.setEnabled(false);
                            viewClicked.setOnClickListener(null);
                            currentPointsTextView.setText("Current Points: " + user.getCurrentPoints());

                        } else if (user.getCurrentPoints() < priceOfItemSelected) {
                            Toast.makeText(PointsStoreActivity.this, "Insufficient points", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PointsStoreActivity.this, "Already Purchased", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                    case COLOR_SCHEME_2: {
                        if (user.getCurrentPoints() >= priceOfItemSelected && !earnedRewards.getHasColorScheme2()) {
                            earnedRewards.setHasColorScheme2(true);
                            Toast.makeText(PointsStoreActivity.this, "Color Scheme 2 Purchased", Toast.LENGTH_SHORT).show();
                            user.setCurrentPoints(user.getCurrentPoints() - priceOfItemSelected);
                            //update UI
                            viewClicked.setAlpha((float) 0.4);
                            viewClicked.setEnabled(false);
                            viewClicked.setOnClickListener(null);
                            currentPointsTextView.setText("Current Points: " + user.getCurrentPoints());

                        } else if (user.getCurrentPoints() < priceOfItemSelected) {
                            Toast.makeText(PointsStoreActivity.this, "Insufficient points", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PointsStoreActivity.this, "Already Purchased", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;
                    case MAP_ICON: {
                        if (user.getCurrentPoints() >= priceOfItemSelected && !earnedRewards.getHasMapIcon()) {
                            earnedRewards.setHasMapIcon(true);
                            Toast.makeText(PointsStoreActivity.this, "Map Icon Purchased", Toast.LENGTH_SHORT).show();
                            user.setCurrentPoints(user.getCurrentPoints() - priceOfItemSelected);
                            //update UI
                            viewClicked.setAlpha((float) 0.4);
                            viewClicked.setEnabled(false);
                            viewClicked.setOnClickListener(null);
                            currentPointsTextView.setText("Current Points: " + user.getCurrentPoints());

                        } else if (user.getCurrentPoints() < priceOfItemSelected) {
                            Toast.makeText(PointsStoreActivity.this, "Insufficient points", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(PointsStoreActivity.this, "Already Purchased", Toast.LENGTH_SHORT).show();
                        }
                    }
                    break;

                    default:
                        break;
                }

                user.setRewards(earnedRewards);

            }
        });
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.points_store_toolbar);
        toolbar.setTitle(R.string.points_store);
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

    private void populateListView() {


        if (earnedRewards.getHasColorScheme1()) {
            purchasableItemsList.add(getString(R.string.colorScheme1Bought));
        } else {
            purchasableItemsList.add(getString(R.string.colorScheme1));
        }
        if (earnedRewards.getHasColorScheme2()) {
            purchasableItemsList.add(getString(R.string.colorScheme2Bought));
        } else {
            purchasableItemsList.add(getString(R.string.colorScheme2));
        }
        if (earnedRewards.getHasMapIcon()) {
            purchasableItemsList.add(getString(R.string.mapIconBought));
        } else {
            purchasableItemsList.add(getString(R.string.mapIcon));
        }


        purchasableItemsPricesList.add(50);
        purchasableItemsPricesList.add(100);
        purchasableItemsPricesList.add(150);


        adapter = new ArrayAdapter<String>(PointsStoreActivity.this,
                R.layout.points_store_layout, //layout to use
                purchasableItemsList);//Item to be display
        pointsStoreListView.setAdapter(adapter);


    }

    private void displayCurrentPoints() {

        currentPointsTextView = (TextView) findViewById(R.id.currentPointsTextView);
        currentPointsTextView.setText("Current Points: " + user.getCurrentPoints());


    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, PointsStoreActivity.class);
    }
}
