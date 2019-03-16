package com.example.whatsappclone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import android.support.v7.widget.Toolbar;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    private Button updateAccountSettings;
    private EditText username,userStatus;
    private CircleImageView userProfileImage;
    private Toolbar settingsToolbar;


    private ProgressDialog loadingBar;

    private String currentUserId;
    private FirebaseAuth mAuth;
    private DatabaseReference userRef;
    private StorageReference userProfileImageRef;



    private static final int GALLERY_PICK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);


        mAuth = FirebaseAuth.getInstance();
        currentUserId = mAuth.getCurrentUser().getUid();
        userRef = FirebaseDatabase.getInstance().getReference();
        userProfileImageRef = FirebaseStorage.getInstance().getReference().child("Profile Images");

        username = (EditText)findViewById(R.id.set_username);
        userStatus = (EditText)findViewById(R.id.set_profile_status);
        updateAccountSettings = (Button)findViewById(R.id.update_settings_button);
        userProfileImage = (CircleImageView)findViewById(R.id.set_profile_image);

        settingsToolbar = (Toolbar)findViewById(R.id.setting_toolbar);
        setSupportActionBar(settingsToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowCustomEnabled(true);
        getSupportActionBar().setTitle("Account Settings");


        loadingBar = new ProgressDialog(this);

        updateAccountSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                saveSettingsToDatabase();
            }
        });

        retrieveUserInfo();

        userProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent,GALLERY_PICK);
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_PICK && resultCode == RESULT_OK && data != null){
            Uri imageUri = data.getData();

            CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(this);
        }
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){

                loadingBar.setTitle("Set profile image");
                loadingBar.setMessage("Please Wait!!");
                loadingBar.show();
                loadingBar.setCanceledOnTouchOutside(false);

                Uri resultUri = result.getUri();

                final StorageReference filePath = userProfileImageRef.child(currentUserId+".jpg");



                filePath.putFile(resultUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                final String downloadUrl = uri.toString();

                                userRef.child("Users").child(currentUserId).child("image")
                                        .setValue(downloadUrl).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()){
                                            Toast.makeText(SettingsActivity.this,"Image saved successfully",Toast.LENGTH_SHORT).show();
                                            loadingBar.dismiss();
                                        }else {

                                            String message = task.getException().getMessage();
                                            Toast.makeText(SettingsActivity.this,"Error " +message,Toast.LENGTH_SHORT).show();
                                            loadingBar.dismiss();
                                        }
                                    }
                                });
                            }
                        });
                    }
                });

            }
        }

    }

    private void retrieveUserInfo() {
        userRef.child("Users").child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name")) && dataSnapshot.hasChild("image")){

                    String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                    String retrieveUserStatus = dataSnapshot.child("status").getValue().toString();
                    String retrieveUserProfileImage = dataSnapshot.child("image").getValue().toString();

                    username.setText(retrieveUsername);
                    userStatus.setText(retrieveUserStatus);

                    Picasso.get().load(retrieveUserProfileImage).into(userProfileImage);

                }else if ((dataSnapshot.exists()) && (dataSnapshot.hasChild("name"))) {


                    String retrieveUsername = dataSnapshot.child("name").getValue().toString();
                    String retrieveUserStatus = dataSnapshot.child("status").getValue().toString();

                    username.setText(retrieveUsername);
                    userStatus.setText(retrieveUserStatus);

                }else {
                    Toast.makeText(SettingsActivity.this,"Please set and update your profile information",Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {

            }
        });

    }

    private void saveSettingsToDatabase() {
        String setUsername = username.getText().toString();
        String setUserStatus = userStatus.getText().toString();

        if (TextUtils.isEmpty(setUsername)){
            Toast.makeText(this,"Please enter your email",Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(setUserStatus)){
            Toast.makeText(this,"Please enter your password",Toast.LENGTH_SHORT).show();
        }else {

            HashMap<String,Object>profileMap = new HashMap<>();
            profileMap.put("uid",currentUserId);
            profileMap.put("name",setUsername);
            profileMap.put("status",setUserStatus);

            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please Wait!!");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(false);

            userRef.child("Users").child(currentUserId).updateChildren(profileMap)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            sendUserToMainActivity();
                            Toast.makeText(SettingsActivity.this,"Profile Updated Successfully",Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }else {

                            String message = task.getException().getMessage();
                            Toast.makeText(SettingsActivity.this,"Error " +message,Toast.LENGTH_SHORT).show();
                            loadingBar.dismiss();
                        }
                    }
                });

        }

    }
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(SettingsActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
