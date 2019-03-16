package com.example.whatsappclone;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class ProfileActivity extends AppCompatActivity {

    private String receiverUserId,currentState,senderUserId;

    private CircleImageView userProfileImage;
    private TextView userProfileName,userProfileStatus;
    private Button sendMessageButton,declineMessageRequest;

    private DatabaseReference userRef,chatRequestRef,contactRef,notificationRef;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);


        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference().child("Users");
        chatRequestRef = FirebaseDatabase.getInstance().getReference().child("Chat Request");
        contactRef = FirebaseDatabase.getInstance().getReference().child("Contacts");
        notificationRef = FirebaseDatabase.getInstance().getReference().child("Notifications");

        Intent intent = getIntent();

        receiverUserId = intent.getStringExtra("visitUserId");
        senderUserId = mAuth.getCurrentUser().getUid();

        //Toast.makeText(this,receiverUserId,Toast.LENGTH_SHORT).show();

        userProfileImage = (CircleImageView)findViewById(R.id.visit_profile_image);
        userProfileName = (TextView)findViewById(R.id.visit_profile_username);
        userProfileStatus = (TextView)findViewById(R.id.visit_profile_user_status);
        sendMessageButton = (Button)findViewById(R.id.visit_send_message_button);
        declineMessageRequest = (Button)findViewById(R.id.decline_message_request);

        currentState = "new";

        receiverUserInfo();






    }

    private void receiverUserInfo() {

        userRef.child(receiverUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")) && dataSnapshot.hasChild("image")) {

                    String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                    String retrieveUserStatus = dataSnapshot.child("status").getValue().toString();
                    String retrieveUserProfileImage = dataSnapshot.child("image").getValue().toString();


                    userProfileName.setText(retrieveUsername);
                    userProfileStatus.setText(retrieveUserStatus);

                    Picasso.get().load(retrieveUserProfileImage).placeholder(R.drawable.profile_image).into(userProfileImage);

                    manageChatRequest();

                }else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))){
                    String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                    String retrieveUserStatus = dataSnapshot.child("status").getValue().toString();



                    userProfileName.setText(retrieveUsername);
                    userProfileStatus.setText(retrieveUserStatus);


                    manageChatRequest();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void manageChatRequest() {

        chatRequestRef.child(senderUserId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.hasChild(receiverUserId)){

                            String requestType =  dataSnapshot.child(receiverUserId).child("request type").getValue().toString();

                            if (requestType.equals("send")){
                                currentState = "request sent";
                                sendMessageButton.setText("Cancel Chat Request");

                            }else if (requestType.equals("received")){
                                currentState = "request received";
                                sendMessageButton.setText("Accept Chat Request");

                                declineMessageRequest.setVisibility(View.VISIBLE);
                                declineMessageRequest.setEnabled(true);

                                declineMessageRequest.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View view) {
                                        cancelChatRequest();
                                    }
                                });
                            }
                        }else {
                            contactRef.child(senderUserId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                            if (dataSnapshot.hasChild(receiverUserId)){
                                                currentState = "friends";
                                                sendMessageButton.setText("Remove this contact");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {

                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });

        if (!senderUserId.equals(receiverUserId)){
            sendMessageButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    sendMessageButton.setEnabled(false);

                    if (currentState.equals("new")){
                        sendMessageRequest();
                    }
                    if (currentState.equals("request sent")){
                        cancelChatRequest();
                    }
                    if (currentState.equals("request received")){
                        acceptChatRequest();
                    }
                    if (currentState.equals("friends")){
                        removeFriendFromContacts();
                    }
                }
            });


        }else {
            sendMessageButton.setVisibility(View.INVISIBLE);
        }

    }

    private void removeFriendFromContacts() {

        contactRef.child(senderUserId).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            contactRef.child(receiverUserId).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){
                                                sendMessageButton.setEnabled(false);
                                                currentState = "new";
                                                sendMessageButton.setText("Send Message");
                                                declineMessageRequest.setVisibility(View.INVISIBLE);
                                                declineMessageRequest.setEnabled(false);
                                            }

                                        }
                                    });
                        }
                    }
                });
    }


    private void acceptChatRequest() {

        contactRef.child(senderUserId).child(receiverUserId)
                .child("Contacts").setValue("Saved")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()){
                            contactRef.child(receiverUserId).child(senderUserId)
                                    .child("Contacts").setValue("Saved")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){

                                                chatRequestRef.child(senderUserId).child(receiverUserId)
                                                        .removeValue()
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {

                                                                if (task.isSuccessful()){

                                                                    chatRequestRef.child(receiverUserId).child(senderUserId)
                                                                            .removeValue()
                                                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                                                @Override
                                                                                public void onComplete(@NonNull Task<Void> task) {
                                                                                    sendMessageButton.setEnabled(true);
                                                                                    currentState = "friends";
                                                                                    sendMessageButton.setText("Remove this contact");
                                                                                    declineMessageRequest.setVisibility(View.INVISIBLE);
                                                                                    declineMessageRequest.setEnabled(false);



                                                                                }
                                                                            });
                                                                }

                                                            }
                                                        });
                                            }

                                        }
                                    });
                    }
                    }
                });
    }



    private void sendMessageRequest() {

        chatRequestRef.child(senderUserId).child(receiverUserId)
                .child("request type").setValue("send")
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                                    .child("request type").setValue("received")
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()){

                                                HashMap<String,String>chatNotificationMap = new HashMap<>();
                                                chatNotificationMap.put("from",senderUserId);
                                                chatNotificationMap.put("type","request");
                                                notificationRef.child(receiverUserId).push()
                                                        .setValue(chatNotificationMap)
                                                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                            @Override
                                                            public void onComplete(@NonNull Task<Void> task) {
                                                                if (task.isSuccessful()){
                                                                    sendMessageButton.setEnabled(true);
                                                                    currentState = "request sent";
                                                                    sendMessageButton.setText("Cancel Request");
                                                                }
                                                            }
                                                        });

                                            }
                                        }
                                    });
                        }
                    }
                });

    }

    private void cancelChatRequest() {

        chatRequestRef.child(senderUserId).child(receiverUserId)
                .removeValue()
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            chatRequestRef.child(receiverUserId).child(senderUserId)
                                    .removeValue()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {

                                            if (task.isSuccessful()){
                                                sendMessageButton.setEnabled(false);
                                                currentState = "new";
                                                sendMessageButton.setText("Send Message");
                                                declineMessageRequest.setVisibility(View.INVISIBLE);
                                                declineMessageRequest.setEnabled(false);
                                            }

                                        }
                                    });
                        }
                    }
                });
    }

}
