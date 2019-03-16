package com.example.whatsappclone;

import android.app.ProgressDialog;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.iid.FirebaseInstanceId;

public class RegisterActivity extends AppCompatActivity {

    private Button registerButton;
    private EditText registerEmailField,registerPasswordField;
    private TextView haveAccountLink;
    private ProgressDialog loadingBar;


    private FirebaseAuth mAuth;
    private DatabaseReference userRef;

    String currentUserId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);


        mAuth = FirebaseAuth.getInstance();
        userRef = FirebaseDatabase.getInstance().getReference();

        registerButton = (Button)findViewById(R.id.register_button);
        haveAccountLink = (TextView) findViewById(R.id.have_account);
        registerPasswordField = (EditText)findViewById(R.id.register_password);
        registerEmailField = (EditText)findViewById(R.id.register_email);

        loadingBar = new ProgressDialog(this);

        haveAccountLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                sendUserToLoginActivity();
            }
        });

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createNewAccount();
            }
        });
    }



    private void createNewAccount() {
        String email = registerEmailField.getText().toString();
        String password = registerPasswordField.getText().toString();

        if (TextUtils.isEmpty(email)){
            Toast.makeText(this,"Please enter your email",Toast.LENGTH_SHORT).show();
        }else if (TextUtils.isEmpty(password)){
            Toast.makeText(this,"Please enter your password",Toast.LENGTH_SHORT).show();
        }else {
            loadingBar.setTitle("Creating New Account");
            loadingBar.setMessage("Please Wait!!");
            loadingBar.show();
            loadingBar.setCanceledOnTouchOutside(false);

            mAuth.createUserWithEmailAndPassword(email,password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    if (task.isSuccessful()){

                        String deviceToken = FirebaseInstanceId.getInstance().getToken();

                        currentUserId = mAuth.getCurrentUser().getUid();
                        userRef.child("Users").child(currentUserId).setValue("");

                        userRef.child("Users").child(currentUserId).child("device_token")
                                .setValue(deviceToken);

                        sendUserToMainActivity();
                        Toast.makeText(RegisterActivity.this,"Your account is created successfully",Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }else {

                        String message = task.getException().getMessage();
                        Toast.makeText(RegisterActivity.this,"Error " +message,Toast.LENGTH_SHORT).show();
                        loadingBar.dismiss();
                    }
                }
            });

        }
    }

    private void sendUserToLoginActivity() {
            Intent loginIntent = new Intent(RegisterActivity.this,LoginActivity.class);
            startActivity(loginIntent);
    }
    private void sendUserToMainActivity() {
        Intent mainIntent = new Intent(RegisterActivity.this,MainActivity.class);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(mainIntent);
        finish();
    }
}
