package com.example.p4;

import androidx.annotation.ArrayRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";

    private Thread p1, p2;
    private volatile boolean p1Running, p2Running;
    private int countClick = 0;
    private int p1Counter = 0;
    private int p2Counter = 0;
    private int gameIterations = 20;
    private Integer sync = 0;

    private TextView playerOneText, playerTwoText;
    private ListView playerOneListView, playerTwoListView;
    private String[] playerOneNotifications, playerTwoNotifications;
    private List<String> playerOneList, playerTwoList;
    private ArrayAdapter<String> playerOneArrayAdapter, playerTwoArrayAdapter;
    private List<Integer> playerOneRandomNumber, playerTwoRandomNumber;
    public Handler playerOneHandler, playerTwoHandler;
    public Handler UIThreadHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        UIThreadHandler = new Handler();

        playerOneText = findViewById(R.id.player1_number);
        playerTwoText = findViewById(R.id.player2_number);

        playerOneListView = findViewById(R.id.player1_notificationBox);
        playerTwoListView = findViewById(R.id.player2_notificationBox);

        playerOneNotifications = new String[]{};
        playerTwoNotifications = new String[]{};

        playerOneList = new ArrayList<>(Arrays.asList(playerOneNotifications));
        playerTwoList = new ArrayList<>(Arrays.asList(playerTwoNotifications));

        playerOneArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                playerOneList);
        playerTwoArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                playerTwoList);

        playerOneListView.setAdapter(playerOneArrayAdapter);
        playerTwoListView.setAdapter(playerTwoArrayAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    public List<Integer> r() {
        List<Integer> r = new ArrayList<>();
        r.add(1); r.add(2); r.add(3); r.add(4);

        return r;
    }

    public void onClick(View view) {
        if(countClick == 0) {
            p1 = new Thread(new PlayerOneThread());
            playerOneRandomNumber = generateNumber();
            String text1 = "Secret Number: " + playerOneRandomNumber.get(0).toString() +
                    playerOneRandomNumber.get(1).toString() + playerOneRandomNumber.get(2).toString() +
                    playerOneRandomNumber.get(3).toString();
            Log.i("P1", "Secret Number: " + text1);
            playerOneText.setText(text1);
            playerOneArrayAdapter.notifyDataSetChanged();

            p2 = new Thread(new PlayerTwoThread());
            playerTwoRandomNumber = generateNumber();
            String text2 = "Secret Number: " + playerTwoRandomNumber.get(0).toString() +
                    playerTwoRandomNumber.get(1).toString() + playerTwoRandomNumber.get(2).toString() +
                    playerTwoRandomNumber.get(3).toString();
            Log.i("P2", "Secret Number: " + text2);
            playerTwoText.setText(text2);
            playerTwoArrayAdapter.notifyDataSetChanged();

            p1Running = true;
            p2Running = true;

            p1.start();
            p2.start();
            Toast.makeText(this, "Starting new game...", Toast.LENGTH_SHORT).show();
        } else {
            playerOneHandler.getLooper().quit();
            playerTwoHandler.getLooper().quit();

            playerOneListView.setAdapter(null);
            playerTwoListView.setAdapter(null);

            Intent restartIntent = getBaseContext().getPackageManager()
                    .getLaunchIntentForPackage(getBaseContext().getPackageName());
            restartIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(restartIntent);
        }
        countClick++;
    }

    // Generates random 4 digit number
    public List<Integer> generateNumber() {
        List<Integer> numbers = new ArrayList<>();

        for(int i = 0; i < 10; i++) {
            numbers.add(i);
        }

        Collections.shuffle(numbers);

        List<Integer> randomNumber = new ArrayList<>();
        for(int i = 0; i < 4; i++) {
            randomNumber.add(numbers.get(i));
        }

        return randomNumber;
    }

    public List<Integer> playerOneStrategy() {
        List<Integer> numbers = new ArrayList<>();
        for(int i = 0; i < 10; i++)
            numbers.add(i);
        Collections.shuffle(numbers);

        List<Integer> guess = new ArrayList<>();
        for(int i = 0; i < 4; i++)
            guess.add(numbers.get(i));

        return guess;
    }

    public List<Integer> playerTwoStrategy() {
        List<Integer> numbers = new ArrayList<>();
        for(int i = 0; i < 10; i++)
            numbers.add(i);
        Collections.shuffle(numbers);

        List<Integer> numbers2 = new ArrayList<>();
        for(int i = 0; i < 5; i++)
            numbers2.add(numbers.get(i));
        Collections.shuffle(numbers2);

        List<Integer> guess = new ArrayList<>();
        for(int i = 0; i < 4; i++)
            guess.add(numbers2.get(i));

        return generateNumber();
    }

    public Integer[] checkGuess(List<Integer> originalNumber, List<Integer> guess) {
        int correct = 0;
        int correctGuessWrongPos = 0;
        List<Integer> temp = new ArrayList<>();
        temp.addAll(guess);

        for(int i = 0; i < originalNumber.size(); i++) {
            for(int j = 0; j < guess.size(); j++) {
                if((originalNumber.get(i) == guess.get(j)) & (i != j))
                    correctGuessWrongPos++;
            }

            if(originalNumber.get(i) == guess.get(i))
                correct++;
        }

        temp.removeAll(originalNumber);
        if(temp.size() > 0) {
            Collections.shuffle(temp);
            return new Integer[]{correct, correctGuessWrongPos, temp.get(0)};
        } else {
            return new Integer[]{correct, correctGuessWrongPos, null};
        }

    }

    public boolean checkWinState(List<Integer> originalNumber, List<Integer> guess) {
        return originalNumber.equals(guess);
    }

    public class PlayerOneThread implements Runnable {
        @SuppressLint("HandlerLeak")
        synchronized
        public void run() {
            Looper.prepare();

            playerOneHandler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);

                    Object object = msg.obj;
                    Log.i("P1", "Message: " + object.toString());
                    List<Integer> playerTwoGuess = (List<Integer>) object;
                    String text = playerTwoGuess.get(0).toString() + playerTwoGuess.get(1).toString()
                            + playerTwoGuess.get(2).toString() + playerTwoGuess.get(3).toString();

                    if(p1Counter == gameIterations - 1) {
                        playerOneList.add("Game Tied");
                        UIThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                playerOneArrayAdapter.notifyDataSetChanged();
                            }
                        });
                    } else if(checkWinState(playerOneRandomNumber, playerTwoGuess)) {
                        playerOneList.add("Player One Won");
                        UIThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                playerOneArrayAdapter.notifyDataSetChanged();
                            }
                        });
                        Looper.myLooper().quit();
                        playerTwoHandler.getLooper().quit();
                    } else {
                        try {
                            Thread.sleep(3000);

                            Integer[] check = checkGuess(playerOneRandomNumber, playerTwoGuess);
                            if(check[2] == null) {
                                playerOneList.add("Guess: " + text +
                                        "\nCorrect Guesses: " + check[0].toString() +
                                        "\nCorrect Guesses in Wrong Positions: " + check[1].toString() +
                                        "\nMissed Digit: " + "N/A");
                            } else {
                                playerOneList.add("Guess: " + text +
                                        "\nCorrect Guesses: " + check[0].toString() +
                                        "\nCorrect Guesses in Wrong Positions: " + check[1].toString() +
                                        "\nMissed Digit: " + check[2].toString());
                            }

                            UIThreadHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    playerOneArrayAdapter.notifyDataSetChanged();
                                }
                            });
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        p1Counter++;
                    }
                }
            };

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int count = 0;
            while (count < gameIterations) {
                Message msg = Message.obtain();
                synchronized (sync) {
                    Log.i("P1 Missed", sync.toString());
                    msg.obj = playerOneStrategy();
                }
                playerTwoHandler.sendMessage(msg);
                count++;
            }

            Looper.loop();
        }
    }

    public class PlayerTwoThread implements Runnable {
        @SuppressLint("HandlerLeak")
        synchronized
        public void run() {
            Looper.prepare();

            playerTwoHandler = new Handler() {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    super.handleMessage(msg);

                    Object object = msg.obj;
                    Log.i("P2", "Message: " + object.toString());

                    List<Integer> playerOneGuess = (List<Integer>) object;
                    String text = playerOneGuess.get(0).toString() + playerOneGuess.get(1).toString()
                            + playerOneGuess.get(2).toString() + playerOneGuess.get(3).toString();

                    if(p2Counter == gameIterations - 1) {
                        playerTwoList.add("Game Tied");
                        UIThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                playerTwoArrayAdapter.notifyDataSetChanged();
                            }
                        });
                        Looper.myLooper().quit();
                        playerOneHandler.getLooper().quit();
                    } else if (checkWinState(playerTwoRandomNumber, playerOneGuess)) {
                        playerOneHandler.getLooper().quit();
                        UIThreadHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                playerTwoArrayAdapter.notifyDataSetChanged();
                            }
                        });
                        playerTwoList.add("Player Two Won");
                        Looper.myLooper().quit();
                    } else {
                        try {
                            Thread.sleep(3000);

                            Integer[] check = checkGuess(playerTwoRandomNumber, playerOneGuess);
                            if(check[2] == null) {
                                playerTwoList.add("Guess: " + text +
                                        "\nCorrect Guesses: " + check[0].toString() +
                                        "\nCorrect Guesses in Wrong Positions: " + check[1].toString() +
                                        "\nMissed Digit: " + "N/A");

                            } else {
                                playerTwoList.add("Guess: " + text +
                                        "\nCorrect Guesses: " + check[0].toString() +
                                        "\nCorrect Guesses in Wrong Positions: " + check[1].toString() +
                                        "\nMissed Digit: " + check[2].toString());
                            }

                            UIThreadHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    playerTwoArrayAdapter.notifyDataSetChanged();
                                }
                            }, 1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        p2Counter++;
                    }
                }
            };

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int count = 0;
            while (count < gameIterations) {
                Message msg = Message.obtain();
                synchronized (sync) {
                    Log.i("P1 Missed", sync.toString());
                    msg.obj = playerTwoStrategy();
                    sync++;
                }
                playerOneHandler.sendMessage(msg);
                count++;
            }

            Looper.loop();
        }
    }
}