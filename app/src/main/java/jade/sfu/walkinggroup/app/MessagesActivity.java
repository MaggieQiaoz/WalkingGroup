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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.dataobjects.Message;
import jade.sfu.walkinggroup.dataobjects.MessagesListAdapter;
import jade.sfu.walkinggroup.dataobjects.Server;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import retrofit2.Call;

// displays all the messages you have received
public class MessagesActivity extends AppCompatActivity {

    private Server server = Server.getInstance(MessagesActivity.this, "MessageActivity");
    private MessagesListAdapter messageAdapter;
    private RecyclerView listOfMessagesRecyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        listOfMessagesRecyclerView = (RecyclerView) findViewById(R.id.allMessagesList);

        setUpToolBar();
        setupMessagesView();
    }

    // Code inspired by: https://blog.sendbird.com/android-chat-tutorial-building-a-messaging-ui

    @Override
    protected void onPostResume() {
        super.onPostResume();
        setupMessagesView();
    }

    private void setupMessagesView() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Call<List<Message>> caller = server.getProxy().getMessages(server.getUser().getId(),
                        Long.valueOf(1));
                ProxyBuilder.callProxy(MessagesActivity.this, caller, returnedMessages -> responseMessages(returnedMessages));
                handler.postDelayed(this, 60000);
            }
        }, 0);
    }

    private void responseMessages(List<Message> returnedMessages) {
        List<Long> userIdList = new ArrayList<>();
        List<Message> messagesListToShow = new ArrayList<>();

        for (Message message : returnedMessages) {
            if (!userIdList.contains(message.getFromUser().getId())) {
                userIdList.add(message.getFromUser().getId());
            }
        }
        Collections.sort(userIdList);
        for (int i = 0; i < userIdList.size(); i++) {
            for (Message message : returnedMessages) {
                if (messagesListToShow.size() <= i && message.getFromUser().getId().equals(userIdList.get(i))) {
                    messagesListToShow.add(message);
                } else if (message.getFromUser().getId().equals(userIdList.get(i)) && message.getTimestamp().after(messagesListToShow.get(i).getTimestamp())) {
                    messagesListToShow.remove(i);
                    messagesListToShow.add(message);
                }
            }
        }

        messageAdapter = new MessagesListAdapter(MessagesActivity.this, messagesListToShow);
        listOfMessagesRecyclerView.setAdapter(messageAdapter);
        listOfMessagesRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        listOfMessagesRecyclerView.setLayoutManager(new LinearLayoutManager(MessagesActivity.this));
    }

    private void setUpToolBar() {
        Toolbar toolbar = findViewById(R.id.messages_toolbar);
        toolbar.setTitle(R.string.messages);
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
        return new Intent(context, MessagesActivity.class);
    }
}