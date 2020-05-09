package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

// Edits the user profile
public class EditProfileActivity extends AppCompatActivity {

    public static final String USER_ID = "userId";
    public static final String TAG = "EditProfileActivity";
    private static final String USER_PREFERENCES = "UserPrefs";
    private static final String SERVER_TOKEN = "ServerToken";
    private static final String USER_EMAIL = "userEmail";
    private Long userId;
    private User user;
    private Server server;
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText birthYearEditText;
    private EditText birthMonthEditText;
    private EditText addressEditText;
    private EditText cellPhoneEditText;
    private EditText homePhoneEditText;
    private EditText gradeEditText;
    private EditText teacherNameEditText;
    private EditText emergencyContactInfoEditText;
    private String oldEmail;
    private SharedPreferences userPrefs;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        nameEditText = (EditText) findViewById(R.id.editName);
        emailEditText = (EditText) findViewById(R.id.editEmail);
        birthYearEditText = (EditText) findViewById(R.id.editBirthYear);
        birthMonthEditText = (EditText) findViewById(R.id.editBirthMonth);
        addressEditText = (EditText) findViewById(R.id.editAddress);
        cellPhoneEditText = (EditText) findViewById(R.id.editCellPhone);
        homePhoneEditText = (EditText) findViewById(R.id.editHomePhone);
        gradeEditText = (EditText) findViewById(R.id.editGrade);
        teacherNameEditText = (EditText) findViewById(R.id.editTeacherName);
        emergencyContactInfoEditText = (EditText) findViewById(R.id.editEmergencyContactInfo);

        userId = getIntent().getLongExtra(USER_ID, 0);
        server = Server.getInstance(EditProfileActivity.this, TAG);

        setUpToolBar();
        getAndDisplayCurrentUserInfo();
        setupSaveButton();
    }

    private void getAndDisplayCurrentUserInfo() {
        Call<User> caller = server.getProxy().getUserById(userId);
        ProxyBuilder.callProxy(EditProfileActivity.this, caller, returnedUser -> responseCurrentUserInfo(returnedUser));
    }

    private void responseCurrentUserInfo(User user) {
        this.user = user;
        oldEmail = user.getEmail();
        //Prefill editText forms with current user info so user doesn't overwrite current info with empty strings
        nameEditText.setText(user.getName());
        emailEditText.setText(user.getEmail());

        if (user.getBirthYear() != null) {
            birthYearEditText.setText(user.getBirthYear());
        }

        if (user.getBirthMonth() != null) {
            birthMonthEditText.setText(user.getBirthMonth());
        }

        if (user.getAddress() != null && !user.getAddress().equals(" ")) {
            addressEditText.setText(user.getAddress());
        }

        if (user.getCellPhone() != null && !user.getCellPhone().equals(" ")) {
            cellPhoneEditText.setText(user.getCellPhone());
        }

        if (user.getHomePhone() != null && !user.getHomePhone().equals(" ")) {
            homePhoneEditText.setText(user.getHomePhone());
        }

        if (user.getGrade() != null && !user.getGrade().equals(" ")) {
            gradeEditText.setText(user.getGrade());
        }

        if (user.getTeacherName() != null && !user.getTeacherName().equals(" ")) {
            teacherNameEditText.setText(user.getTeacherName());
        }

        if (user.getEmergencyContactInfo() != null && !user.getEmergencyContactInfo().equals(" ")) {
            emergencyContactInfoEditText.setText(user.getEmergencyContactInfo());
        }
    }

    private void setupSaveButton() {
        Button saveButton = (Button) findViewById(R.id.saveButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Edit the user and send back edited user object to server.
                editUserInfo(EditProfileActivity.this.user);
                if (!oldEmail.equals(EditProfileActivity.this.user.getEmail())) {
                    buildEmailConfirmationDialog();
                } else {
                    Call<User> caller = server.getProxy().editUser(userId, EditProfileActivity.this.user);
                    ProxyBuilder.callProxy(EditProfileActivity.this, caller, returnedUser -> responseEditUser(returnedUser));
                }
            }
        });
    }

    private void buildEmailConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(EditProfileActivity.this);
        builder.setMessage(R.string.change_email_dialog)
                .setTitle(R.string.change_email_dialog_title);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
                Call<User> caller = server.getProxy().editUser(userId, EditProfileActivity.this.user);
                ProxyBuilder.callProxy(EditProfileActivity.this, caller, returnedUser -> responseEmailChanged(returnedUser));
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User cancelled the dialog
                dialog.cancel();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void responseEditUser(User returnedUser) {
        this.user = returnedUser;
        Toast.makeText(this, R.string.edit_info_successful, Toast.LENGTH_LONG).show();
        server.setUser(returnedUser);
    }

    private void responseEmailChanged(User returnedUser) {
        this.user = returnedUser;
        server.setToken(null);

        userPrefs = EditProfileActivity.this.getSharedPreferences(USER_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = userPrefs.edit();
        editor.putString(SERVER_TOKEN, null);
        editor.putString(USER_EMAIL, null);
        editor.apply();

        server.setUser(returnedUser);
        Intent intent = LoginActivity.makeIntent(this);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void editUserInfo(User user) {

        String name = nameEditText.getText().toString();
        String email = emailEditText.getText().toString();
        String birthYear = birthYearEditText.getText().toString();
        String birthMonth = birthMonthEditText.getText().toString();
        String address = addressEditText.getText().toString();
        String cellPhone = cellPhoneEditText.getText().toString();
        String homePhone = homePhoneEditText.getText().toString();
        String grade = gradeEditText.getText().toString();
        String teacherName = teacherNameEditText.getText().toString();
        String emergencyContactInfo = emergencyContactInfoEditText.getText().toString();

        user.setName(name);
        user.setEmail(email);
        user.setBirthYear(birthYear);
        user.setBirthMonth(birthMonth);
        user.setAddress(address);
        user.setCellPhone(cellPhone);
        user.setHomePhone(homePhone);
        user.setGrade(grade);
        user.setTeacherName(teacherName);
        user.setEmergencyContactInfo(emergencyContactInfo);
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.edit_profile_toolbar);
        toolbar.setTitle(R.string.edit_profile_title);
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
        return new Intent(context, EditProfileActivity.class);
    }
}
