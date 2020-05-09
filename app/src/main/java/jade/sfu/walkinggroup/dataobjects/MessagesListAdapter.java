package jade.sfu.walkinggroup.dataobjects;

import android.content.Context;
import android.content.Intent;
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
import jade.sfu.walkinggroup.app.MessagesActivity;
import jade.sfu.walkinggroup.app.MessagesFromUserActivity;

// For message activity
public class MessagesListAdapter extends RecyclerView.Adapter {

    public static final String FROM_USER_ID = "fromUserId";
    private Context context;
    private List<Message> messageList;
    private Boolean isEmergency;

    public MessagesListAdapter(Context context, List<Message> messageList) {
        this.context = context;
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.messages_received_from_all_users,
                parent, false);
        return new MessageHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = (Message) messageList.get(position);
        ((MessageHolder) holder).bind(message);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = MessagesFromUserActivity.makeIntent(context);
                intent.putExtra(FROM_USER_ID, message.getFromUser().getId());
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }


    private class MessageHolder extends RecyclerView.ViewHolder {
        TextView nameOfSender;
        TextView messageText;
        TextView timeStamp;
        ImageView emergencyIcon;
        ImageView unreadIcon;

        MessageHolder(View itemView) {
            super(itemView);
            messageText = (TextView) itemView.findViewById(R.id.message_I_received);
            timeStamp = (TextView) itemView.findViewById(R.id.message_timestamp);
            nameOfSender = (TextView) itemView.findViewById(R.id.name_of_message_Sender);
            emergencyIcon = (ImageView) itemView.findViewById(R.id.emergencyIcon);
            unreadIcon = (ImageView) itemView.findViewById(R.id.unreadIcon);
        }

        void bind(Message message) {
            DateFormat dateFormat = new SimpleDateFormat("\"yyyy-MM-dd E hh:mm a\"");
            nameOfSender.setText(message.getFromUser().getName());
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
