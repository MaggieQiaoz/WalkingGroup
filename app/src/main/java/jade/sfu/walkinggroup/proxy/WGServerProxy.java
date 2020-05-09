package jade.sfu.walkinggroup.proxy;

import java.util.List;

import jade.sfu.walkinggroup.dataobjects.GpsLocation;
import jade.sfu.walkinggroup.dataobjects.Group;
import jade.sfu.walkinggroup.dataobjects.Message;
import jade.sfu.walkinggroup.dataobjects.PermissionRequest;
import jade.sfu.walkinggroup.dataobjects.User;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;


/**
 * The ProxyBuilder class will handle the apiKey and token being injected as a header to all calls
 * This is a Retrofit interface.
 */
public interface WGServerProxy {
    @GET("getApiKey")
    Call<String> getApiKey(@Query("groupName") String groupName);

    // -----------------------------
    // Users
    // -----------------------------
    @POST("/users/signup")
    Call<User> createUser(@Body User user);

    @POST("/login")
    Call<Void> login(@Body User userWithEmailAndPassword);

    @GET("/users")
    Call<List<User>> getUsers();

    @GET("/users/{id}")
    Call<User> getUserById(@Path("id") Long userId);

    @GET("/users/byEmail")
    Call<User> getUserByEmail(@Query("email") String email);

    @GET("/users/byEmail")
    Call<User> getUserByEmail(@Query("email") String email, @Header("JSON-DEPTH") Long depth);

    @POST("/users/{id}")
    Call<User> editUser(@Path("id") Long userId, @Body User user);

    // -----------------------------
    // GPS Location
    // -----------------------------
    @GET("/users/{id}/lastGpsLocation")
    Call<GpsLocation> getLastGpsLocation(@Path("id") Long userId);

    @POST("/users/{id}/lastGpsLocation")
    Call<GpsLocation> setLastGpsLocation(@Path("id") Long userId, @Body GpsLocation location);


    // -----------------------------
    // User Monitoring
    // -----------------------------
    @GET("/users/{id}/monitoredByUsers")
    Call<List<User>> getMonitoredByUsers(@Path("id") Long userId);



    @GET("/users/{id}/monitoredByUsers")
    Call<List<User>> getMonitoredByUsers(@Path("id") Long userId, @Header("JSON-DEPTH") Long depth);

    @POST("/users/{id}/monitoredByUsers")
    Call<List<User>> addToMonitoredByUsers(@Path("id") Long userId, @Body User user);

    @POST("/users/{id}/monitoredByUsers")
    Call<List<User>> addToMonitoredByUsers(@Path("id") Long userId, @Body User user ,@Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);


    @DELETE("/users/{id}/monitoredByUsers/{childId}")
    Call<Void> removeFromMonitoredByUsers(@Path("id") Long userId, @Path("childId") Long childId);

    @DELETE("/users/{id}/monitoredByUsers/{childId}")
    Call<Void> removeFromMonitoredByUsers(@Path("id") Long userId, @Path("childId") Long childId, @Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);


    @GET("/users/{id}/monitorsUsers")
    Call<List<User>> getMonitorsUsers(@Path("id") Long userId);

    @GET("/users/{id}/monitorsUsers")
    Call<List<User>> getMonitorsUsers(@Path("id") Long userId, @Header("JSON-DEPTH") Long depth);


    @POST("/users/{id}/monitorsUsers")
    Call<List<User>> addToMonitorsUsers(@Path("id") Long userId, @Body User user);

    @POST("/users/{id}/monitorsUsers")
    Call<List<User>> addToMonitorsUsers(@Path("id") Long userId, @Body User user, @Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);

    @DELETE("/users/{id}/monitorsUsers/{childId}")
    Call<Void> removeFromMonitorsUsers(@Path("id") Long userId, @Path("childId") Long childId);


    // -----------------------------
    // Groups
    // -----------------------------
    @GET("/groups")
    Call<List<Group>> getGroups();

    @GET("/groups/{id}")
    Call<Group> getGroupById(@Path("id") Long groupId);

    @POST("/groups")
    Call<Group> createGroup(@Body Group group);

    @POST("/groups")
    Call<Group> createGroup(@Body Group group, @Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);

    @POST("/groups/{id}")
    Call<Group> updateGroup(@Path("id") Long groupId, @Body Group group);

    @POST("/groups/{id}")
    Call<Group> updateGroup(@Path("id") Long groupId, @Body Group group, @Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);

    @DELETE("/groups/{id}")
    Call<Void> deleteGroup(@Path("id") Long groupId);

    @GET("/groups/{id}/memberUsers")
    Call<List<User>> getGroupMembers(@Path("id") Long groupId);

    @POST("/groups/{id}/memberUsers")
    Call<List<User>> addGroupMember(@Path("id") Long groupId, @Body User user);

    @POST("/groups/{id}/memberUsers")
    Call<List<User>> addGroupMember(@Path("id") Long groupId, @Body User user, @Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);

    @DELETE("/groups/{id}/memberUsers/{userId}")
    Call<Void> removeGroupMember(@Path("id") Long groupId, @Path("userId") Long userId);

    @DELETE("/groups/{id}/memberUsers/{userId}")
    Call<Void> removeGroupMember(@Path("id") Long groupId, @Path("userId") Long userId, @Header("PERMISSIONS-ENABLED") Boolean permissionsEnabled);


    // -----------------------------
    // Messages
    // -----------------------------
    @POST("/messages/togroup/{groupId}")
    Call<List<Message>> newMessageToGroup(@Path("groupId") Long groupId, @Body Message message);

    @POST("/messages/toparentsof/{userId}")
    Call<List<Message>> newMessageToParentsOf(@Path("userId") Long userId, @Body Message message);

    @GET("/messages")
    Call<List<Message>> getMessages();

    @GET("/messages")
    Call<List<Message>> getMessages(@Query("touser") Long toUserId);

    @GET("/messages")
    Call<List<Message>> getMessages(@Query("touser") Long toUserId, @Header("JSON-DEPTH") Long depth);

    @GET("/messages")
    Call<List<Message>> getMessages(@Query("touser") Long toUserId, @Query("is-emergency") Boolean isEmergency);

    @GET("/messages?status=unread")
    Call<List<Message>> getUnreadMessages(
            @Query("touser") Long toUserId,
            @Query("is-emergency") Boolean isEmergency);    // null for not filtering

    @GET("/messages?status=read")
    Call<List<Message>> getReadMessages(
            @Query("touser") Long toUserId,
            @Query("is-emergency") Boolean isEmergency);    // null for not filtering

    @GET("/messages/{id}")
    Call<Message> getOneMessage(@Path("id") Long id);

    // if markAsRead is false, then marks it as unread.
    @POST("/messages/{id}/mark-read-or-unread")
    Call<Message> markMessageAsRead(@Path("id") Long id, @Body Boolean markAsRead);

    @DELETE("/messages/{id}")
    Call<Void> deleteMessage(@Path("id") Long id);


    // -----------------------------
    // Permissions
    // -----------------------------
    // TODO: Add query options
    @GET("/permissions")
    Call<List<PermissionRequest>> getPermissions();

    @GET("/permissions")
    Call<List<PermissionRequest>> getPermissions(@Query("userId") long userId);

    @GET("/permissions")
    Call<List<PermissionRequest>> getPermissions(@Query("userId") long userId, @Header("JSON-DEPTH") Long depth);

    @GET("/permissions/{id}")
    Call<PermissionRequest> getPermissionById(@Path("id") long permissionId);

    @POST("/permissions/{id}")
    Call<PermissionRequest> approveOrDenyPermissionRequest(
            @Path("id") long permissionId,
            @Body PermissionStatus status
    );

    // -- Internal --
    @GET("/permissions/actions")
    Call<List<ActionInfo>> getPermissionActionInfo();

    //  -- Internal --
    @POST("/permissions/{id}/magicOverrideDoNotUse")
    Call<PermissionRequest> magicOverridePermissionById(
            @Path("id") long permissionId,
            @Body PermissionRequest request
    );


    // -----------------------------
    // Small data classes required by permissions
    // -----------------------------
    class ActionInfo {
        String value;
        String description;
    }

    enum PermissionStatus {
        PENDING,
        APPROVED,
        DENIED
    }
}
