package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.EarnedRewards;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import jade.sfu.walkinggroup.proxy.WGServerProxy;
import retrofit2.Call;

// handles logic for user sign up
public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";
    private long userId = 0;
    private String userName;
    private String userEmail;
    private String userPassword;
    private WGServerProxy proxy;
    private String token = null;
    SharedPreferences userPrefs;
    private static final String USER_PREFERENCES = "UserPrefs";
    private static final String SERVER_TOKEN = "ServerToken";
    private static final String USER_EMAIL = "userEmail";
    private Server server;
    private User user;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        server = Server.getInstance(this, token, TAG);
        setupSignUpButton();
    }

    private void setupSignUpButton() {
        Button btn = (Button) findViewById(R.id.activity_signup_btn_signup);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextName = (EditText) findViewById(R.id.activity_signup_et_name);
                EditText editTextEmail = (EditText) findViewById(R.id.activity_signup_et_email);
                EditText editTextPassword = (EditText) findViewById(R.id.activity_signup_et_password);

                userName = editTextName.getText().toString();
                userEmail = editTextEmail.getText().toString();
                userPassword = editTextPassword.getText().toString();
                user = new User();
                user.setName(userName);
                user.setEmail(userEmail);
                user.setPassword(userPassword);
                user.setRewards(new EarnedRewards());
                callCreateUser(user);
            }
        });
    }

    private void callCreateUser(User user) {
        Call<User> caller = server.getProxy().createUser(user);
        ProxyBuilder.callProxy(this, caller, returnedUser -> responseCreatedUser(returnedUser));
    }

    private void responseCreatedUser(User user) {
        server.setUser(user);
        ProxyBuilder.setOnTokenReceiveCallback(token -> onReceiveToken(token));
        Call<Void> caller = server.getProxy().login(this.user);
        ProxyBuilder.callProxy(SignupActivity.this, caller, returnedNothing -> response(returnedNothing));
    }

    private void response(Void returnedNothing) {
        //login is successful at this point
        Log.w(TAG, "Server replied to login request (no content was expected).");
        startMapActivity();
    }

    // Handle the token by generating a new Proxy which is encoded with it.
    private void onReceiveToken(String token) {
        WGServerProxy proxy = ProxyBuilder.getProxy(this.getString(R.string.apikey), token);
        this.token = ProxyBuilder.getServerToken().toString();
        server.setToken(token);
        server.setProxy(proxy);
    }

    public static Intent makeIntent(Context context) {
        return new Intent(context, SignupActivity.class);
    }

    private void startMapActivity() {
        userPrefs = this.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString(SERVER_TOKEN, token);
        editor.putString(USER_EMAIL, userEmail);
        editor.apply();
        //Add intent flags in order to clear the previous loginActivity: Back on MapInit will now close app.
        Intent intent = new Intent(this, MainMapActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
