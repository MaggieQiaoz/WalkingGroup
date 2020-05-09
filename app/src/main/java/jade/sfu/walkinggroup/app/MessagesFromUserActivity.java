package jade.sfu.walkinggroup.app;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.CustomComparator;
import jade.sfu.walkinggroup.dataobjects.Message;
import jade.sfu.walkinggroup.dataobjects.MessagesFromUserListAdapter;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.dataobjects.User;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

// displays all the messages from the selected user
public class MessagesFromUserActivity extends AppCompatActivity {

    public static final String FROM_USER_ID = "fromUserId";
    private Server server = Server.getInstance(MessagesFromUserActivity.this, "MessageFromUserActivity");
    private MessagesFromUserListAdapter messageFromUserListAdapter;
    private RecyclerView listOfMessagesRecyclerView;
    private Long fromUserId;
    private List<Message> messagesFromUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages_from_user);

        fromUserId = getIntent().getLongExtra(FROM_USER_ID, 0);

        listOfMessagesRecyclerView = (RecyclerView) findViewById(R.id.messagesFromUser);

        messagesFromUser = new ArrayList<>();

        setUpToolBar();
        setupMessagesView();
    }

    private void setupMessagesView() {
        Call<List<Message>> caller = server.getProxy().getMessages(server.getUser().getId(),
                Long.valueOf(1));
        ProxyBuilder.callProxy(this, caller, returnedMessages -> responseMessages(returnedMessages));
    }

    private void responseMessages(List<Message> returnedMessages) {
        for (Message message : returnedMessages) {
            if (message.getFromUser().getId().equals(fromUserId)) {
                Call<Message> caller = server.getProxy().markMessageAsRead(message.getId(), true);
                ProxyBuilder.callProxy(this, caller, returnedMessage -> responseMessage(returnedMessage));
            }
        }
    }

    private void responseMessage(Message returnedMessage) {

        messagesFromUser.add(returnedMessage);

        Collections.sort(messagesFromUser, new CustomComparator());
        Collections.reverse(messagesFromUser);
        messageFromUserListAdapter = new MessagesFromUserListAdapter(MessagesFromUserActivity.this, messagesFromUser);
//        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
//        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
//        listOfMessagesRecyclerView.addItemDecoration(new DividerItemDecoration(this));
        listOfMessagesRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        listOfMessagesRecyclerView.setAdapter(messageFromUserListAdapter);
        listOfMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(MessagesFromUserActivity.this));
    }

    private void setUpToolBar() {
        Call<User> caller = server.getProxy().getUserById(fromUserId);
        ProxyBuilder.callProxy(this, caller, returnedUser -> responseUser(returnedUser));
    }

    private void responseUser(User returnedUser) {
        Toolbar toolbar = findViewById(R.id.messages_from_user_toolbar);
        toolbar.setTitle("Messages from " + returnedUser.getName());
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
        return new Intent(context, MessagesFromUserActivity.class);
    }
}