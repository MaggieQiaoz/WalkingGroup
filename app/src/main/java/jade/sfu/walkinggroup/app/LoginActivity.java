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
import android.widget.ProgressBar;
import android.widget.TextView;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import jade.sfu.walkinggroup.proxy.WGServerProxy;
import retrofit2.Call;


// handles logic for user login
public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private String userEmail;
    private String userPassword;
    private String token = null;
    SharedPreferences userPrefs;
    private static final String USER_PREFERENCES = "UserPrefs";
    private static final String SERVER_TOKEN = "ServerToken";
    private static final String USER_EMAIL = "userEmail";
    private Server server;
    private User user;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        progressBar = findViewById(R.id.progressBar);
        getUserPrefs();
        startMapActivityIfUserLoggedIn();
        server = Server.getInstance(this, token, TAG);

        setupLoginButton();
        setupSignupTextView();
    }

    public static Intent makeIntent(Context context) {

        return new Intent(context, LoginActivity.class);
    }

    private void startMapActivityIfUserLoggedIn() {
        if (token != null) {
            server = server.getInstance(this, token, TAG);
            Call<User> caller = server.getProxy().getUserByEmail(userEmail, Long.valueOf(3));
            ProxyBuilder.callProxy(this, caller, returnedUser -> responseUser(returnedUser));
        }
    }

    private void getUserPrefs() {
        userPrefs = this.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        userEmail = userPrefs.getString(USER_EMAIL, null);
        token = userPrefs.getString(SERVER_TOKEN, null);
    }

    private void setupSignupTextView() {
        TextView textView = (TextView) findViewById(R.id.activity_login_tv_sign_up);
        textView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = SignupActivity.makeIntent(LoginActivity.this);
                startActivity(intent);
            }
        });
    }

    private void setupLoginButton() {
        Button button = (Button) findViewById(R.id.activity_login_btn_login);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText editTextEmail = (EditText) findViewById(R.id.activity_login_et_email);
                EditText editTextPassword = (EditText) findViewById(R.id.activity_login_et_password);

                userEmail = editTextEmail.getText().toString();
                userPassword = editTextPassword.getText().toString();
                user = new User();
                user.setEmail(userEmail);
                user.setPassword(userPassword);
                callLogin(user);
            }
        });
    }

    private void callLogin(User user) {
        // Register for token received:
        ProxyBuilder.setOnTokenReceiveCallback(token -> onReceiveToken(token));
        token = ProxyBuilder.getServerToken().toString();
        server.setToken(token);

        // Make call
        Call<Void> caller = server.getProxy().login(user);
        ProxyBuilder.callProxy(this, caller, returnedNothing -> response(returnedNothing));
    }

    // Handle the token by generating a new Proxy which is encoded with it.
    private void onReceiveToken(String token) {
        // Replace the current proxy with one that uses the token!
        Log.w(TAG, "   --> NOW HAVE TOKEN: " + token);
        WGServerProxy proxy = ProxyBuilder.getProxy(this.getString(R.string.apikey), token);
        server.setProxy(proxy);
    }

    // Login actually completes by calling this; nothing to do as it was all done
    // when we got the token.
    private void response(Void returnedNothing) {
        //login is successful at this point
        progressBar.setVisibility(View.VISIBLE);
        Log.w(TAG, "Server replied to login request (no content was expected).");
        Call<User> caller = server.getProxy().getUserByEmail(userEmail, Long.valueOf(3));
        ProxyBuilder.callProxy(this, caller, returnedUser -> responseUser(returnedUser));
    }

    private void responseUser(User user) {
        server.setUser(user);
        Log.w(TAG, "Server replied with user: " + user.toString());
        startMapActivity();
    }

    private void startMapActivity() {
        storeUserPreferences();
        Intent intent = MainMapActivity.makeIntent(LoginActivity.this);
        startActivity(intent);
        finish();
    }

    private void storeUserPreferences() {
        userPrefs = this.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString(SERVER_TOKEN, token);
        editor.putString(USER_EMAIL, userEmail);
        editor.apply();
    }
}
