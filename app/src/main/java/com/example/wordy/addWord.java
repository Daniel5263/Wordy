package com.example.wordy;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Locale;

public class addWord extends AppCompatActivity {

    Button cancelButton;
    Button addButton;
    Button clearButton;
    EditText enterWord;
    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addword);

        cancelButton = findViewById(R.id.button_cancel);
        addButton = findViewById(R.id.button_add);
        clearButton = findViewById(R.id.button_dbc);
        enterWord = findViewById(R.id.enter_word);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("wordBank");

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(addWord.this, MainActivity.class);
                startActivity(intent);
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                addWordToFirebase();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                databaseReference.removeValue();

                Toast.makeText(addWord.this, "Word Bank cleared", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addWordToFirebase() {
        String enteredWord = enterWord.getText().toString().trim();

        if (TextUtils.isEmpty(enteredWord)) {
            showToast("Please enter a word");
            enterWord.setBackgroundResource(android.R.color.holo_purple);
            return;
        }

        if (enteredWord.length() != 5) {
            showToast("Word must be exactly 5 characters long");
            enterWord.setBackgroundResource(android.R.color.holo_purple);
            return;
        }

        if (!isAlphabetical(enteredWord)) {
            showToast("Word must contain only alphabetical characters");
            enterWord.setBackgroundResource(android.R.color.holo_purple);
            return;
        }

        isWordAlreadyExists(enteredWord.toLowerCase(Locale.getDefault()));
    }

    private void showToast(String message) {
        Toast.makeText(addWord.this, message, Toast.LENGTH_SHORT).show();
    }

    private boolean isAlphabetical(String word) {
        return word.matches("[a-zA-Z]+");
    }

    private void isWordAlreadyExists(final String word) {
        final String lowercaseWord = word.toLowerCase();

        databaseReference.orderByValue().equalTo(lowercaseWord).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    showToast("Word already exists in the word bank");
                } else {
                    storeWordInDatabase(lowercaseWord);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private void storeWordInDatabase(String word) {
        databaseReference.push().setValue(word);
        enterWord.setBackgroundResource(android.R.color.transparent);
        showToast("Word added to the word bank!");
        enterWord.setText("");
    }
}
