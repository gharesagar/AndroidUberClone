package com.example.sagar.androiduberclone;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.example.sagar.androiduberclone.model.User;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.rengwuxian.materialedittext.MaterialEditText;

import dmax.dialog.SpotsDialog;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class MainActivity extends AppCompatActivity {

    Button btSignIn, btRegister;

    FirebaseAuth firebaseAuth;
    FirebaseDatabase db;
    DatabaseReference users;
    RelativeLayout rootLayout;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Arkhip_font.ttf")
                .setFontAttrId(R.attr.fontPath)
                .build());
        setContentView(R.layout.activity_main);

        btSignIn = findViewById(R.id.btSignIn);
        btRegister = findViewById(R.id.btRegister);
        rootLayout = findViewById(R.id.rootLayout);

        //ininy firebase
        firebaseAuth = FirebaseAuth.getInstance();
        db = FirebaseDatabase.getInstance();
        users = db.getReference("UberUsers");


        btRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showRegisterDialog();
            }
        });

        btSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLoginDialog();
            }
        });
    }

    private void showLoginDialog() {

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("LOGIN");
        dialog.setMessage("Please use email to sign in");

        View view = LayoutInflater.from(this).inflate(R.layout.layout_login, null);

        final MaterialEditText edtEmail = view.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = view.findViewById(R.id.edtPassword);
        dialog.setView(view);

        //set button
        dialog.setPositiveButton("LOGIN", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();

                //disable button while sign in
                btSignIn.setEnabled(false);

                //validation
                if (TextUtils.isEmpty(edtEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter email address", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (edtPassword.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Password is too short", Snackbar.LENGTH_SHORT).show();
                    return;
                }


                final android.app.AlertDialog dialog = new SpotsDialog.Builder().setContext(MainActivity.this).setMessage("Please wait").build();
                dialog.show();

                //login
                firebaseAuth.signInWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                dialog.dismiss();

                                startActivity(new Intent(MainActivity.this,Welcome.class));
                                finish();
                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        dialog.dismiss();

                        Snackbar.make(rootLayout, "Failed " + e.getMessage(), Snackbar.LENGTH_SHORT).show();

                        btSignIn.setEnabled(true);

                    }
                });
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });

        dialog.show();

    }


    private void showRegisterDialog() {
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("REGISTER");
        dialog.setMessage("Please use email to register");

        View view = LayoutInflater.from(this).inflate(R.layout.layout_register, null);

        final MaterialEditText edtEmail = view.findViewById(R.id.edtEmail);
        final MaterialEditText edtPassword = view.findViewById(R.id.edtPassword);
        final MaterialEditText edtName = view.findViewById(R.id.edtName);
        final MaterialEditText edtPhone = view.findViewById(R.id.edtPhone);

        dialog.setView(view);

        //set button
        dialog.setPositiveButton("REGISTER", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {


                //validation
                if (TextUtils.isEmpty(edtEmail.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter email address", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtPassword.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter password", Snackbar.LENGTH_SHORT).show();
                    return;
                }
                if (edtPassword.getText().toString().length() < 6) {
                    Snackbar.make(rootLayout, "Password is too short", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtName.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter Name", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                if (TextUtils.isEmpty(edtPhone.getText().toString())) {
                    Snackbar.make(rootLayout, "Please enter phone number", Snackbar.LENGTH_SHORT).show();
                    return;
                }

                //register new user
                firebaseAuth.createUserWithEmailAndPassword(edtEmail.getText().toString(), edtPassword.getText().toString())
                        .addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                            @Override
                            public void onSuccess(AuthResult authResult) {
                                //save user to db

                                User user = new User(edtEmail.getText().toString(),
                                        edtPassword.getText().toString(),
                                        edtName.getText().toString(),
                                        edtPhone.getText().toString());

                                //email is key
                                users.child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                                        .setValue(user)
                                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                                            @Override
                                            public void onSuccess(Void aVoid) {

                                                Snackbar.make(rootLayout, "User registered successfully!!", Snackbar.LENGTH_SHORT).show();
                                            }
                                        })
                                        .addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                Snackbar.make(rootLayout, "Failed " + e.getMessage(), Snackbar.LENGTH_SHORT).show();

                                            }
                                        });
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Snackbar.make(rootLayout, "Failed " + e.getMessage(), Snackbar.LENGTH_SHORT).show();

                            }
                        });
            }
        });

        dialog.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                dialogInterface.dismiss();
            }
        });

        dialog.show();

    }
}
