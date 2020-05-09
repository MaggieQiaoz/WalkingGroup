package jade.sfu.walkinggroup.dataobjects;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import jade.sfu.walkinggroup.proxy.WGServerProxy;
import retrofit2.Call;

// Singleton for proxy and user
public class Server {
    private static WGServerProxy proxy;
    private static String token = null;
    private static Server instance;
    private static String TAG;
    private Context context;
    private User user;

    //Make private to prevent anyone from instantiating
    private Server(Context context, String TAG) {
        this.TAG = TAG;
        proxy = ProxyBuilder.getProxy(context.getString(R.string.apikey), token);
    }

    private Server(Context context, String token, String TAG) {
        this.TAG = TAG;
        this.token = token;
        proxy = ProxyBuilder.getProxy(context.getString(R.string.apikey), token);
    }

    public static Server getInstance(Context context, String TAG) {
        if (instance == null) {
            instance = new Server(context, TAG);
        }
        instance.setContext(context);
        return instance;
    }

    public static Server getInstance(Context context, String token, String TAG) {
        if (instance == null) {
            instance = new Server(context, token, TAG);
        }
        instance.setContext(context);
        return instance;
    }

    public Context getContext() {
        return context;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public WGServerProxy getProxy() {
        return proxy;
    }

    public void setProxy(WGServerProxy proxy) {
        this.proxy = proxy;
    }

    // Put message up in toast and logcat
    // -----------------------------------------------------------------------------------------
    private void notifyUserViaLogAndToast(String message) {
        Log.w(TAG, message);
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void removeGroupMember(long groupId, long userId) {
        Call<Void> caller = instance.getProxy().removeGroupMember(groupId, userId);
        ProxyBuilder.callProxy(context, caller, returnedNothing -> responseRemovedGroupMember(returnedNothing));
    }

    public void callRemoveGroupMember(long groupId, long userId) {
        removeGroupMember(groupId, userId);
    }

    private void responseRemovedGroupMember(Void returnedNothing) {
        notifyUserViaLogAndToast("Removed group member");
    }

    private void responseGPSLocation(GpsLocation returnedGPSLocation) {
    }

    public void setLastGpsLocation(long userId, GpsLocation location) {
        Call<GpsLocation> caller = instance.getProxy().setLastGpsLocation(userId, location);
        ProxyBuilder.callProxy(context, caller, returnedGPSLocation -> responseGPSLocation(returnedGPSLocation));
    }
}
