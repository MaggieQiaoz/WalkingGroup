package jade.sfu.walkinggroup.dataobjects;

import java.util.Comparator;

// Compares timestamps
public class CustomComparator implements Comparator<Message> {
    @Override
    public int compare(Message message1, Message message2) {
        return message1.getTimestamp().compareTo(message2.getTimestamp());
    }
}
