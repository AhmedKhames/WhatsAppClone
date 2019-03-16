package com.example.whatsappclone;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

public class PrivateChatActivity extends AppCompatActivity {

    private String messageReceiverName,messageReceiverId,messageReceiverImage,messageSenderId;

    private Toolbar mToolbar;
    private FirebaseAuth mAuth;
    private DatabaseReference rootRef;


    private TextView chatUsername,userLastSeen;
    private CircleImageView userImage;
    private ImageButton sendMessageButton;
    private EditText messageInputText;
    private RecyclerView userMessagesList;

    private final List<Messages> messagesList = new ArrayList<>();
    private LinearLayoutManager linearLayoutManager ;
    private MessageAdapter messageAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_private_chat);

        mAuth = FirebaseAuth.getInstance();
        messageSenderId = mAuth.getCurrentUser().getUid();
        rootRef = FirebaseDatabase.getInstance().getReference();

        messageReceiverId = getIntent().getExtras().get("visitedUserId").toString();
        messageReceiverName = getIntent().getExtras().get("visitedUsername").toString();
        messageReceiverImage = getIntent().getExtras().get("visitedUserImage").toString();


        mToolbar = (Toolbar)findViewById(R.id.private_chat_toolbar);
        setSupportActionBar(mToolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowCustomEnabled(true);

        LayoutInflater layoutInflater = (LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.custom_chat_bar,null);
        actionBar.setCustomView(view);

        chatUsername = (TextView)findViewById(R.id.custom_profile_name);
        userLastSeen = (TextView)findViewById(R.id.custom_profile_last_seen);
        userImage = (CircleImageView) findViewById(R.id.custom_profile_image);
        sendMessageButton = (ImageButton)findViewById(R.id.private_send_message_button);
        messageInputText = (EditText)findViewById(R.id.input_private_message);


        messageAdapter = new MessageAdapter(messagesList);
        userMessagesList = (RecyclerView)findViewById(R.id.private_messages_list);
        linearLayoutManager = new LinearLayoutManager(this);
        userMessagesList.setLayoutManager(linearLayoutManager);
        userMessagesList.setAdapter(messageAdapter);

        chatUsername.setText(messageReceiverName);
        Picasso.get().load(messageReceiverImage).placeholder(R.drawable.profile_image).into(userImage);


        sendMessageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendMessage();

            }
        });

    }


    private void displayLastSeen(){
        rootRef.child("Users").child(messageSenderId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {

                if (dataSnapshot.child("userState").hasChild("state")){
                    String state = dataSnapshot.child("userState").child("state").getValue().toString();
                    String date = dataSnapshot.child("userState").child("date").getValue().toString();
                    String time = dataSnapshot.child("userState").child("time").getValue().toString();

                    if (state.equals("Online")){
                        userLastSeen.setText("Online");
                    }else if (state.equals("Offline")){
                        userLastSeen.setText("Last Seen: " + date + " " + time);
                    }

                }else {
                    userLastSeen.setText("offline");
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        rootRef.child("Messages").child(messageSenderId).child(messageReceiverId).addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Messages messages = dataSnapshot.getValue(Messages.class);
                messagesList.add(messages);
                messageAdapter.notifyDataSetChanged();
                userMessagesList.smoothScrollToPosition(userMessagesList.getAdapter().getItemCount());
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });
    }

    private void sendMessage() {

        String textMessage = messageInputText.getText().toString();

        if (TextUtils.isEmpty(textMessage)){
            Toast.makeText(PrivateChatActivity.this,"Enter your message",Toast.LENGTH_SHORT).show();
        }else {

            String messageSenderRef = "Messages/"+messageSenderId + "/"+ messageReceiverId;
            String messageReceiverRef = "Messages/"+messageReceiverId + "/"+ messageSenderId;

            DatabaseReference userMessageKeyRef = rootRef.child("Messages")
                    .child(messageSenderId).child(messageReceiverId).push();

            String messagePushId = userMessageKeyRef.getKey();

            Map messageTextBody = new HashMap();
            messageTextBody.put("message",textMessage);
            messageTextBody.put("type","text");
            messageTextBody.put("from",messageSenderId);

            Map messageBodyDetails = new HashMap();
            messageBodyDetails.put(messageSenderRef+"/"+messagePushId,messageTextBody);
            messageBodyDetails.put(messageReceiverRef+"/"+messagePushId,messageTextBody);

            rootRef.updateChildren(messageBodyDetails).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()){
                        Toast.makeText(PrivateChatActivity.this,"Message Sent",Toast.LENGTH_SHORT).show();
                    }else {
                        Toast.makeText(PrivateChatActivity.this,"Error",Toast.LENGTH_SHORT).show();

                    }
                    messageInputText.setText("");
                }
            });

        }


    }
}
