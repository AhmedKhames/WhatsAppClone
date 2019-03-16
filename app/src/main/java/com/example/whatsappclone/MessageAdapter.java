package com.example.whatsappclone;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessagesViewHolder> {
    private List<Messages> usersMessagesList;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    public MessageAdapter(List<Messages> usersMessagesList) {

        this.usersMessagesList = usersMessagesList;
    }

    public class MessagesViewHolder extends RecyclerView.ViewHolder {
        public TextView senderMessageText,receiverMessageText;
        public CircleImageView receiverProfileImage;

        public MessagesViewHolder(@NonNull View itemView) {
            super(itemView);

            senderMessageText = (TextView)itemView.findViewById(R.id.sender_message_id);
            receiverMessageText = (TextView)itemView.findViewById(R.id.receiver_message_id);
            receiverProfileImage = (CircleImageView)itemView.findViewById(R.id.message_profile_image);
        }
    }


    @NonNull
    @Override
    public MessagesViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.custom_messages_layout,viewGroup,false);


        mAuth = FirebaseAuth.getInstance();



        return new MessagesViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MessagesViewHolder messagesViewHolder, int i) {

        String senderId = mAuth.getCurrentUser().getUid();
        Messages messages = usersMessagesList.get(i);

        String fromUserId = messages.getFrom();
        String fromMessageType = messages.getType();

        userRef = FirebaseDatabase.getInstance().getReference().child("Users").child(fromUserId);
        userRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    if (dataSnapshot.hasChild("image")){
                        String receiverImage = dataSnapshot.child("image").getValue().toString();
                        Picasso.get().load(receiverImage).placeholder(R.drawable.profile_image).into(messagesViewHolder.receiverProfileImage);


                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

        if (fromMessageType.equals("text")){
            messagesViewHolder.receiverMessageText.setVisibility(View.INVISIBLE);
            messagesViewHolder.receiverProfileImage.setVisibility(View.INVISIBLE);
            messagesViewHolder.senderMessageText.setVisibility(View.INVISIBLE);

            if (fromUserId.equals(senderId)){
                messagesViewHolder.senderMessageText.setVisibility(View.VISIBLE);

                messagesViewHolder.senderMessageText.setBackgroundResource(R.drawable.sender_messages_layout);
                messagesViewHolder.senderMessageText.setText(messages.getMessage());
            }else {


                messagesViewHolder.receiverProfileImage.setVisibility(View.VISIBLE);
                messagesViewHolder.receiverMessageText.setVisibility(View.VISIBLE);

                messagesViewHolder.receiverMessageText.setBackgroundResource(R.drawable.receiver_messages_layout);
                messagesViewHolder.receiverMessageText.setText(messages.getMessage());

            }
        }

    }

    @Override
    public int getItemCount() {
        return usersMessagesList.size();
    }


}