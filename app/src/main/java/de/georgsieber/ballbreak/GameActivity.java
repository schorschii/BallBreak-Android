package de.georgsieber.ballbreak;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;

import java.io.File;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;


public class GameActivity extends AppCompatActivity {
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private GameView mGameView;
    private View mOptionsView;
    private int lastPoints;
    Date date;
    DateFormat dateFormat;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        //mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);
        mOptionsView = findViewById(R.id.grid_options);
        mGameView = findViewById(R.id.view_game);
        mGameView.setGameStateListener(new GameView.GameStateListener() {
            @Override
            public void onGameOver(int points) {
                lastPoints = points;
                date = new Date();
                dateFormat = android.text.format.DateFormat.getDateFormat(getApplicationContext());
                mOptionsView.setVisibility(View.VISIBLE);

                int maxScore = 0;
                String maxScoreName = "";
                ArrayList<Highscore> scores = readScores();
                for(Highscore score : scores) {
                    maxScore = score.points;
                    maxScoreName = score.name;
                    break;
                }

                if(points > maxScore && points > 1) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(mOptionsView.getContext(), android.R.style.Theme_Holo));
                    builder.setTitle("["+Integer.toString(points)+"]  New Highscore!");
                    final EditText input = new EditText(new ContextThemeWrapper(mOptionsView.getContext(), android.R.style.Theme_Holo));
                    input.setHint("Enter your name...");
                    input.setText(maxScoreName);
                    input.setInputType(InputType.TYPE_CLASS_TEXT /*| InputType.TYPE_TEXT_VARIATION_PASSWORD*/);
                    builder.setView(input);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            insertScore(dateFormat.format(date), input.getText().toString(),lastPoints);
                            delayedHide(100);
                        }
                    });
                    /*builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                            delayedHide(100);
                        }
                    });*/
                    builder.setCancelable(false);
                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
                    dialog.show();
                }
            }
            @Override
            public void onGameStart() {
                mOptionsView.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        initDatabase();

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    protected void onResume() {
        super.onResume();
        delayedHide(100);
    }

    public void onClickWeb(View v) {
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://georg-sieber.de"));
        startActivity(browserIntent);
    }

    public void onClickHighscores(View v) {
        String scoresString = "";
        ArrayList<Highscore> scores = readScores();
        int count = 0;
        for(Highscore score : scores) {
            if(count == 6) {
                scoresString += "...";
                break;
            }
            scoresString += "[" + score.points + "]  " + score.name + " (" + score.date + ")\n";
            count ++;
        }

        AlertDialog.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder = new AlertDialog.Builder(mGameView.getContext(), android.R.style.Theme_Material_Dialog_Alert);
        } else {
            builder = new AlertDialog.Builder(mGameView.getContext());
        }
        builder.setTitle("Highscores")
                .setMessage(scoresString)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        delayedHide(100);
                    }
                })
                /*.setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // do nothing
                    }
                })*/
                .show();
    }

    SQLiteDatabase db = null;
    private File getStorage() {
        return new File(getExternalFilesDir(null), "score.sqlite");
    }
    public void scanFile(File f) {
        Uri uri = Uri.fromFile(f);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }
    private void initDatabase() {
        db = openOrCreateDatabase(getStorage().getPath(), MODE_PRIVATE, null);
        db.execSQL("CREATE TABLE IF NOT EXISTS highscore (id INTEGER PRIMARY KEY AUTOINCREMENT, name VARCHAR, date VARCHAR, points INTEGER);");
        scanFile(getStorage());
    }
    private ArrayList<Highscore> readScores() {
        Cursor cursor = db.rawQuery("SELECT * FROM highscore ORDER BY points DESC", null);
        ArrayList<Highscore> scores = new ArrayList<>();
        try {
            if (cursor.moveToFirst()) {
                do {
                    Highscore newHighscore = new Highscore(cursor.getString(1),cursor.getString(2),cursor.getInt(3));
                    scores.add(newHighscore);
                } while (cursor.moveToNext());
            }
        } catch (SQLiteException e) {
            Log.e("SELECT", "SQLite Error: " + e.getMessage());
            return null;
        } finally {
            cursor.close();
            //db.close();
        }
        return scores;
    }
    private void insertScore(String date, String name, int points) {
        SQLiteStatement stmt = db.compileStatement("INSERT INTO highscore (date, name, points) VALUES (?,?,?)");
        stmt.bindString(1, date);
        stmt.bindString(2, name);
        stmt.bindLong(3, points);
        stmt.execute();
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }
}
