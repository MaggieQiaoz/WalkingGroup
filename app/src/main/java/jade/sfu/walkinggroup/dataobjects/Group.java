package jade.sfu.walkinggroup.dataobjects;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Store information about the walking groups.
 * <p>
 * WARNING: INCOMPLETE! Server returns more information than this.
 * This is just to be a placeholder and inspire you how to do it.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Group extends IdItemBase {

    private String groupDescription; //descriptive name
    private double[] routeLatArray = new double[2]; // holds lat for meetup and destination
    private double[] routeLngArray = new double[2]; // holds lat for meetup and destination
    private User leader;
    private List<User> memberUsers = new ArrayList<>(); // members in the group

    public double[] getRouteLatArray() {
        return routeLatArray;
    }

    public void setRouteLatArray(double latStart, double latEnd) {
        this.routeLatArray[0] = latStart;
        this.routeLatArray[1] = latEnd;
    }

    public double[] getRouteLngArray() {
        return routeLngArray;
    }

    public void setRouteLngArray(double lngStart, double lngEnd) {
        this.routeLngArray[0] = lngStart;
        this.routeLngArray[1] = lngEnd;
    }

    public User getLeader() {
        return leader;
    }

    public void setLeader(User leader) {
        this.leader = leader;
    }

    public List<User> getMemberUsers() {
        return memberUsers;
    }

    public void addMember(User user) {
        this.memberUsers.add(user);
    }

    public void deleteMember(User user) {
        this.memberUsers.remove(user);
    }

    public void setMemberUsers(List<User> memberUsers) {
        this.memberUsers = memberUsers;
    }

    public String getCustomJson() {
        return customJson;
    }

    public void setCustomJson(String customJson) {
        this.customJson = customJson;
    }

    //    private List<Message> messages = new ArrayList<>(); // <-- TO BE IMPLEMENTED (will be a list of messages)
    private String customJson; // later for gamification


    // Utility Functions (must be changed for group)
    // -------------------------------------------------------------------------------------------


    @Override
    public String toString() {
        return "Group{" +
                "id=" + getId() +
                "groupDescription='" + groupDescription + '\'' +
                ", routeLatArray=" + Arrays.toString(routeLatArray) +
                ", routeLngArray=" + Arrays.toString(routeLngArray) +
                ", leader=" + leader +
                ", memberUsers=" + memberUsers +
                ", customJson='" + customJson + '\'' +
                ", hasFullData=" + hasFullData() +
                ", href='" + getHref() + '\'' +
                '}';
    }

    public String getGroupDescription() {
        return groupDescription;
    }

    public void setGroupDescription(String groupDescription) {
        this.groupDescription = groupDescription;
    }
}
