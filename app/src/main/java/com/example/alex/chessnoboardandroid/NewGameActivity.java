package com.example.alex.chessnoboardandroid;

import android.app.Activity;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;

public class NewGameActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    private static final String TAG = MainApp.MainTag + NewGameActivity.class.getSimpleName();

    private TextView strengthLabel;
    private String origLabel;
    //private Spinner spinnerAllowBoard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_game);


        final SeekBar seekBar = findViewById(R.id.seekBar);
        seekBar.setOnSeekBarChangeListener(this);
        strengthLabel = findViewById(R.id.strengthLabel);
        origLabel = strengthLabel.getText().toString();

        String jsonObj = getIntent().getStringExtra("prevParm");
        NewGameParams prevParm = (new Gson()).fromJson(jsonObj, NewGameParams.class);

        //spinnerAllowBoard = findViewById(R.id.spinnerAllowBoard);
        //spinnerAllowBoard.setSelection(prevParm.getAllowViewBoardMode().getIndex());

        Log.d(TAG, String.format("onCreate NewGameActivity. strengthLabel=%s", Boolean.toString(strengthLabel != null)));

        seekBar.setProgress(prevParm.getCompStrength());
        updateLabel(seekBar.getProgress());
    }

    public void onStart(View view) {

        NewGameParams newGameParm = new NewGameParams();

        //int pos = spinnerAllowBoard.getSelectedItemPosition();
        //newGameParm.setAllowViewBoardMode(AllowViewBoardMode.fromId(pos));

        newGameParm.setCompStrength(((SeekBar)findViewById(R.id.seekBar)).getProgress());
        Intent returnIntent = new Intent();
        returnIntent.putExtra("newGameParm", (new Gson()).toJson(newGameParm));
        setResult(Activity.RESULT_OK, returnIntent);
        finish();
    }

    void updateLabel(int progress) {
        strengthLabel.setText(origLabel + " " + Integer.toString(progress));
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        updateLabel(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }
}
