package jade.sfu.walkinggroup.dataobjects;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import jade.sfu.walkinggroup.R;
import jade.sfu.walkinggroup.proxy.ProxyBuilder;
import jade.sfu.walkinggroup.proxy.WGServerProxy;
import retrofit2.Call;

public class RequestsAdapter extends RecyclerView.Adapter {
    private static final int PENDING_REQUEST = 1;
    private static final int APPROVED_OR_DENIED_REQUEST = 2;
    private Server server;

    private List<PermissionRequest> requestList;
    private Context context;

    public RequestsAdapter(Context context, List<PermissionRequest> requests) {
        this.context = context;
        this.requestList = requests;
        server = Server.getInstance(context, "RequestsAdapter");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view;

        if (viewType == PENDING_REQUEST) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.pending_request_layout,
                    parent, false);
            return new PendingRequestHolder(view);
        } else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.approved_or_denied_request_layout,
                    parent, false);
            return new ApprovedOrDeniedHolder(view);
        }
    }

    @Override
    public int getItemViewType(int position) {
        PermissionRequest request = requestList.get(position);
        List<PermissionRequest.Authorizor> authorizerList = new ArrayList<>(request.getAuthorizors());
        int positionOfCurrentAuthorizer = 0;

        for (int i = 0; i < authorizerList.size(); i++) {
            List<User> userList = new ArrayList<>(authorizerList.get(i).getUsers());
            for (int j = 0; j < userList.size(); j++) {
                if (userList.get(j).getId().equals(server.getUser().getId())) {
                    positionOfCurrentAuthorizer = i;
                    break;
                }
            }
        }

        if (authorizerList.get(positionOfCurrentAuthorizer).getStatus().equals(
                WGServerProxy.PermissionStatus.PENDING)) {
            return PENDING_REQUEST;
        } else{
            return APPROVED_OR_DENIED_REQUEST;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        PermissionRequest request = (PermissionRequest) requestList.get(position);

        switch (holder.getItemViewType()) {
            case PENDING_REQUEST:
                ((PendingRequestHolder) holder).bind(request);
                break;

            case APPROVED_OR_DENIED_REQUEST:
                ((ApprovedOrDeniedHolder) holder).bind(request);
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<PermissionRequest.Authorizor> authorizerList = new ArrayList<>(request.getAuthorizors());
                String statusMessageTotal="";
                String statusMessage="";
                for(int i=0;i<authorizerList.size();i++){
                    if (authorizerList.get(i).getStatus().equals(WGServerProxy.PermissionStatus.APPROVED)) {
                          statusMessage= authorizerList.get(i).getWhoApprovedOrDenied().getName()+" has approved this request.";
                    }
                    else if(authorizerList.get(i).getStatus().equals(WGServerProxy.PermissionStatus.DENIED)){
                        statusMessage  = authorizerList.get(i).getWhoApprovedOrDenied().getName()+" has denied this request.";
                    }
                    statusMessageTotal=statusMessageTotal+ statusMessage+"\n";
                }

                TextView statusMessageTextView=new TextView(context);
                statusMessageTextView.setText(statusMessageTotal);

                RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(
                        RecyclerView.LayoutParams.WRAP_CONTENT, RecyclerView.LayoutParams.WRAP_CONTENT);
                params.setMargins(12, 0, 0, 0);
                statusMessageTextView.setLayoutParams(params);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);

                builder.setView(statusMessageTextView)
                        .setTitle(R.string.status_detail);

                AlertDialog dialog = builder.create();
                dialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return requestList.size();
    }

    private class PendingRequestHolder extends RecyclerView.ViewHolder {
        TextView nameOfRequestingUser;
        TextView action;
        Button approveRequest;
        Button denyRequest;

        PendingRequestHolder(View itemView) {
            super(itemView);
            nameOfRequestingUser = (TextView) itemView.findViewById(R.id.requesting_user);
            action = (TextView) itemView.findViewById(R.id.request_Action);
            approveRequest = (Button) itemView.findViewById(R.id.approve_request_button);
            denyRequest = (Button) itemView.findViewById(R.id.deny_request_button);
        }

        void bind(PermissionRequest request) {
            nameOfRequestingUser.setText(request.getRequestingUser().getName());
            action.setText(request.getMessage());

            //Set up buttons
            approveRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Call<PermissionRequest> caller = server.getProxy().approveOrDenyPermissionRequest(
                            request.getId(), WGServerProxy.PermissionStatus.APPROVED);
                    ProxyBuilder.callProxy(context, caller, returnedRequest -> responseRequest(returnedRequest));
                }
            });

            denyRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Call<PermissionRequest> caller = server.getProxy().approveOrDenyPermissionRequest(
                            request.getId(), WGServerProxy.PermissionStatus.DENIED);
                    ProxyBuilder.callProxy(context, caller, returnedRequest -> responseRequest(returnedRequest));
                }
            });
        }
    }

    private void responseRequest(PermissionRequest returnedRequest) {

    }

    private class ApprovedOrDeniedHolder extends RecyclerView.ViewHolder {
        TextView nameOfRequestingUser;
        TextView action;
        TextView state;

        ApprovedOrDeniedHolder(View itemView) {
            super(itemView);
            nameOfRequestingUser = (TextView) itemView.findViewById(R.id.requesting_user);
            action = (TextView) itemView.findViewById(R.id.request_Action);
            state = (TextView) itemView.findViewById(R.id.state_of_request);
        }

        void bind(PermissionRequest request) {
            nameOfRequestingUser.setText(request.getRequestingUser().getName());
            action.setText(request.getMessage());
            state.setText(request.getStatus().toString());

            //Format to distinguish from pending requests
        }

    }

}
