package com.example.wordy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private DatabaseReference databaseReference;
    private List<LinearLayout> rows;
    private int currentRow = 1;
    private int attempts = 0;
    private TextView targetWord;
    private List<Character> targetWordLetters;
    private TextView txtWinner;
    private TextView txtLoser;
    private boolean gameWon = false;

    Button addButton;
    Button submitButton;
    Button clearButton;
    Button restartButton;
    Drawable defaultBG;
    Drawable greenBG;
    Drawable yellowBG;
    Drawable greyBG;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();
        databaseReference = FirebaseDatabase.getInstance().getReference().child("wordBank");

        targetWordLetters = new ArrayList<>();

        addButton = findViewById(R.id.button_a);
        submitButton = findViewById(R.id.button_s);
        clearButton = findViewById(R.id.button_c);
        restartButton = findViewById(R.id.button_r);
        targetWord = findViewById(R.id.target_word);
        txtWinner = findViewById(R.id.txt_winner);
        txtLoser = findViewById(R.id.txt_loser);
        defaultBG = getResources().getDrawable(R.drawable.bg_edit);
        greenBG = getResources().getDrawable(R.drawable.bg_green);
        yellowBG = getResources().getDrawable(R.drawable.bg_yellow);
        greyBG = getResources().getDrawable(R.drawable.bg_gray);

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, addWord.class);
                startActivity(intent);
            }
        });

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkEnteredWord();
                handleSubmission();
            }
        });

        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                clearGame();
            }
        });

        restartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resetGame();
            }
        });

        rows = new ArrayList<>();
        rows.add(findViewById(R.id.row1));
        rows.add(findViewById(R.id.row2));
        rows.add(findViewById(R.id.row3));
        rows.add(findViewById(R.id.row4));
        rows.add(findViewById(R.id.row5));
        rows.add(findViewById(R.id.row6));

        loadRandomWord();
    }

    private void checkEnteredWord() {
        if (targetWordLetters.isEmpty()) {
            showToast("Add a word");
            return;
        }

        if (!gameWon) {
            LinearLayout currentRowLayout = rows.get(currentRow - 1);

            boolean[] isCorrectLetter = new boolean[targetWordLetters.size()];
            boolean[] isCorrectPosition = new boolean[targetWordLetters.size()];

            for (int j = 0; j < currentRowLayout.getChildCount(); j++) {
                EditText editText = (EditText) currentRowLayout.getChildAt(j);
                String enteredCharacter = editText.getText().toString();

                for (int i = 0; i < targetWordLetters.size(); i++) {
                    char targetCharacter = targetWordLetters.get(i);

                    if (enteredCharacter.equalsIgnoreCase(String.valueOf(targetCharacter))) {
                        if (j == i && !isCorrectPosition[i]) {
                            isCorrectPosition[i] = true;
                            editText.setBackground(greenBG);
                            break;
                        } else if (!isCorrectLetter[i]) {
                            isCorrectLetter[i] = true;
                            editText.setBackground(yellowBG);
                        } else {
                            editText.setBackground(greyBG);
                        }
                    }
                }
            }

            if (isCorrectPosition.length == targetWordLetters.size() && allElementsTrue(isCorrectPosition)) {
                txtWinner.setVisibility(View.VISIBLE);
                gameWon = true;
            }
        }
    }

    private boolean allElementsTrue(boolean[] array) {
        for (boolean b : array) {
            if (!b) {
                return false;
            }
        }
        return true;
    }

    private void resetGame() {
        enableAllInput();

        for (int i = 1; i < rows.size(); i++) {
            rows.get(i).setVisibility(View.GONE);
        }

        currentRow = 1;
        attempts = 0;
        gameWon = false;

        clearAllEditTexts();
        loadRandomWord();

        txtWinner.setVisibility(View.GONE);
        txtLoser.setVisibility(View.GONE);

        showToast("Game reset");
    }

    private void clearGame() {
        enableAllInput();

        for (int i = 1; i < rows.size(); i++) {
            rows.get(i).setVisibility(View.GONE);
        }

        currentRow = 1;
        attempts = 0;
        gameWon = false;

        clearAllEditTexts();

        txtWinner.setVisibility(View.GONE);
        txtLoser.setVisibility(View.GONE);

        showToast("Cleared guesses");
    }

    private void enableAllInput() {
        for (LinearLayout row : rows) {
            for (int j = 0; j < row.getChildCount(); j++) {
                EditText editText = (EditText) row.getChildAt(j);
                editText.setEnabled(true);
            }
        }
    }

    private void clearAllEditTexts() {
        for (LinearLayout row : rows) {
            for (int j = 0; j < row.getChildCount(); j++) {
                EditText editText = (EditText) row.getChildAt(j);
                editText.setText("");
                editText.setBackground(defaultBG);
            }
        }
    }

    private void loadRandomWord() {
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    List<String> wordList = new ArrayList<>();
                    for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                        String word = snapshot.getValue(String.class);
                        wordList.add(word);
                    }

                    if (!wordList.isEmpty()) {
                        String randomWord = getRandomWord(wordList);
                        targetWord.setText(randomWord);
                        targetWordLetters.clear();

                        for (int i = 0; i < randomWord.length(); i++) {
                            char letter = randomWord.charAt(i);
                            targetWordLetters.add(letter);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
            }
        });
    }

    private String getRandomWord(List<String> wordList) {
        Random random = new Random();
        int randomIndex = random.nextInt(wordList.size());
        return wordList.get(randomIndex);
    }

    private void handleSubmission() {
        if (!gameWon) {
            boolean isRowValid = checkCurrentRow();

            if (isRowValid) {
                disableInputForCurrentRow();
                currentRow++;
                if (currentRow <= rows.size()) {
                    rows.get(currentRow - 1).setVisibility(View.VISIBLE);
                } else {
                    txtLoser.setVisibility(View.VISIBLE);
                }
            } else {
                showToast("Please enter 5 characters");
            }
        }
    }

    private void disableInputForCurrentRow() {
        LinearLayout currentRowLayout = rows.get(currentRow - 1);
        for (int j = 0; j < currentRowLayout.getChildCount(); j++) {
            EditText editText = (EditText) currentRowLayout.getChildAt(j);
            editText.setEnabled(false);
        }
    }

    private boolean checkCurrentRow() {
        LinearLayout currentRowLayout = rows.get(currentRow - 1);
        for (int i = 0; i < currentRowLayout.getChildCount(); i++) {
            View childView = currentRowLayout.getChildAt(i);
            if (childView instanceof EditText) {
                EditText editText = (EditText) childView;
                if (TextUtils.isEmpty(editText.getText().toString().trim())) {
                    return false;
                }
            }
        }
        return true;
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}