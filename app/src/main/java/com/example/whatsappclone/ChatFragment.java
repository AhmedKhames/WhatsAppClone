package com.example.whatsappclone;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import de.hdodenhof.circleimageview.CircleImageView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ChatFragment extends Fragment {

    private View privateChatView;
    private RecyclerView chatList;

    private DatabaseReference chatsRef,usersRef;


    private FirebaseAuth mAuth;
    private String currentUserId;




    public ChatFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        privateChatView = inflater.inflate(R.layout.fragment_chat, container, false);

        chatList = (RecyclerView)privateChatView.findViewById(R.id.chat_list);
        chatList.setLayoutManager(new LinearLayoutManager(getContext()));


        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        chatsRef = FirebaseDatabase.getInstance().getReference().child("Contacts").child(currentUserId);
        usersRef = FirebaseDatabase.getInstance().getReference().child("Users");

        return privateChatView;
    }

    @Override
    public void onStart() {
        super.onStart();
        FirebaseRecyclerOptions options =
                new FirebaseRecyclerOptions.Builder<Contacts>()
                        .setQuery(chatsRef, Contacts.class)
                        .build();

        FirebaseRecyclerAdapter<Contacts, ChatsViewHolder> adapter = new FirebaseRecyclerAdapter<Contacts, ChatsViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull final ChatsViewHolder holder, int position, @NonNull Contacts model) {
                final String userId = getRef(position).getKey();
                final String[] friendPic = {"default image"};

                usersRef.child(userId).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.hasChild("image")) {
                                friendPic[0] = dataSnapshot.child("image").getValue().toString();
                                Picasso.get().load(friendPic[0]).placeholder(R.drawable.profile_image).into(holder.profileImage);
                            }

                            final String profileName = dataSnapshot.child("name").getValue().toString();
                            final String profileStatus = dataSnapshot.child("status").getValue().toString();

                            holder.username.setText(profileName);
                            holder.userStatus.setText("Last Seen at\nTime: \nDate");


                            if (dataSnapshot.child("userState").hasChild("state")){
                                String state = dataSnapshot.child("userState").child("state").getValue().toString();
                                String date = dataSnapshot.child("userState").child("date").getValue().toString();
                                String time = dataSnapshot.child("userState").child("time").getValue().toString();

                                if (state.equals("Online")){
                                    holder.userStatus.setText("Online");
                                }else if (state.equals("Offline")){
                                    holder.userStatus.setText("Last Seen: " + date + " " + time);
                                }

                            }else {
                                holder.userStatus.setText("offline");
                            }



                            holder.itemView.setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    Intent privateChatIntent = new Intent(getContext(),PrivateChatActivity.class);
                                    privateChatIntent.putExtra("visitedUserId",userId);
                                    privateChatIntent.putExtra("visitedUsername",profileName);
                                    privateChatIntent.putExtra("visitedUserImage", friendPic[0]);
                                    startActivity(privateChatIntent);


                                }
                            });
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
            }

            @NonNull
            @Override
            public ChatsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
                View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.users_display_layout,viewGroup,false);
                ChatsViewHolder viewHolder = new ChatsViewHolder(view);
                return viewHolder;
            }
        };
        chatList.setAdapter(adapter);
        adapter.startListening();

    }


    public static class ChatsViewHolder extends RecyclerView.ViewHolder{

        TextView username,userStatus;
        CircleImageView profileImage;

        public ChatsViewHolder(@NonNull View itemView) {
            super(itemView);

            username = itemView.findViewById(R.id.public_users_profile_name);
            userStatus = itemView.findViewById(R.id.public_users_profile_status);
            profileImage = itemView.findViewById(R.id.public_users_profile_image);
        }
    }

}
