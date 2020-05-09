package jade.sfu.walkinggroup.dataobjects;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import jade.sfu.walkinggroup.R;

// Recycler view adapter for message from user activity
public class MessagesFromUserListAdapter extends RecyclerView.Adapter {

    public static final String FROM_USER_ID = "fromUserId";
    private Context context;
    private List<Message> messageList;
    private Boolean isEmergency;

    public MessagesFromUserListAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_from_that_user_layout,
                parent, false);
        return new MessageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = (Message) messageList.get(position);
        ((MessageHolder) holder).bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }


    private class MessageHolder extends RecyclerView.ViewHolder {
        //TextView nameOfSender;
        TextView messageText;
        TextView timeStamp;
        ImageView emergencyIcon;
        ImageView unreadIcon;

        MessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.message_I_received);
            timeStamp = (TextView) itemView.findViewById(R.id.message_timestamp);
            emergencyIcon = (ImageView) itemView.findViewById(R.id.emergencyIcon);
            unreadIcon = (ImageView) itemView.findViewById(R.id.unreadIcon);
        }

        void bind(Message message) {
            DateFormat dateFormat = new SimpleDateFormat("\"yyyy-MM-dd E hh:mm a\"");
            messageText.setText(message.getText());
            timeStamp.setText(dateFormat.format(message.getTimestamp()));
            if (message.isEmergency()) {
                emergencyIcon.setVisibility(View.VISIBLE);
            } else {
                emergencyIcon.setVisibility(View.INVISIBLE);
            }
            if (!message.isRead()) {
                unreadIcon.setVisibility(View.VISIBLE);
            } else {
                unreadIcon.setVisibility(View.INVISIBLE);
            }
        }
    }
}
