package edu.northwestern.langlearn;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Environment;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.speech.tts.TextToSpeech.OnInitListener;
import android.widget.Toast;

import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

import static android.os.Environment.getExternalStorageDirectory;

//TODO: Clean stuff here. This was made by coping our encoding activity to there's stuff we're not using.
public class wordTest extends AppCompatActivity implements OnInitListener{
    private int MY_DATA_CHECK_CODE = 0;
    private TextToSpeech myTTS;
    int INITIAL_WORDS=40; //how many words were originally learned
    int NEW_WORDS=5; //how many new words to add with each test session

    String currentWord=""; //current word is used to allow other functions to accsess the current German word in updateTrans function;
    String currentGerman="";
    String[] translations;
    int tPointer=0;
    int myPointer=0; //scratch space to iterate through the test
    int toRange=0; //if this is a mini test, at what place will we leave off
    boolean minitest=true; //is this a mini test embedded in learning? If so this will be true
    boolean feedbackState=false; //what to do when button clicked, if this is false it will give feedback if true will go onto next word.
    int[] correct; //stores whether each word has been translated succsesfully
    int seen=0; //number of words seen
    int NUM_REQUIRED=1; //number of correct translations required for each word
    boolean ttsWarmedUp=false; //goes true when the TTS is fully initialized
    MediaPlayer correctsound;
    MediaPlayer incorrectsound;
    SharedPreferences prefs;
    FileWriter logWriter;
    //functions for writing the log data. TODO: consolidate these into a single class that can be used across activities
    public boolean isExternalStorageWritable() { //check to make sure we can store data on the SD card.
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    void fileError() {
        Toast.makeText(wordTest.this,
                "Logfile error. Unplug the phone and try again. If problem persists, contact nathanww@u.northwestern.edu", Toast.LENGTH_LONG).show();
        Intent myIntent = new Intent(wordTest.this, participantMode.class);
        wordTest.this.startActivity(myIntent);
    }
    void logTimestamp(String message) { //write something to the log file with a timestamp.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
        String current = sdf.format(new Date());
        try {
            logWriter.write(current + ":" + message + "\n");
            logWriter.flush();
        }
        catch (Exception e) { //fail silently if we can't write

        }

    }

    public int getNextWord(int index) { //gets the index for the next word that has not been succsessfully translated at the criterion. If none is present, returns -1;
        for (int i = index + 1; i < translations.length; i++) {
            if (correct[i] < NUM_REQUIRED) {
                return i;
            }
        }
        //we didn't find any going forward, so loop around
        for (int i = 0; i < index + 1; i++) {
            if (correct[i] < NUM_REQUIRED) {
                return i;
            }
        }
        return -1;
    }


    public void onInit(int initStatus) {
        if (initStatus == TextToSpeech.SUCCESS) {
            myTTS.setLanguage(Locale.GERMAN);
            updateTrans();
        }
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == MY_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, this);
            }
            else {
                Log.e("TTS","Not set up");
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);

            }
        }
    }
    public void updateTrans() { //update the translation displayed on the screen
        View subject = (View) findViewById(R.id.response);
        subject.setVisibility(View.VISIBLE);
        TextView english = (TextView) findViewById(R.id.englishword);
        english.setVisibility(View.GONE);
        EditText rField   = (EditText)findViewById(R.id.response);
        rField.setText("", TextView.BufferType.EDITABLE);
        String[] splitup=translations[myPointer].split(" ");
        TextView germanword = (TextView)findViewById(R.id.germanword);
        myTTS.speak(splitup[0], TextToSpeech.QUEUE_FLUSH, null);
        while (!ttsWarmedUp && myTTS.isSpeaking()==false) { //wait until the TTS is ready

        }
        ttsWarmedUp=true;
        germanword.setText(splitup[0].replace("_"," "));
        TextView englishword = (TextView)findViewById(R.id.englishword);
        englishword.setText(splitup[1].replace("_"," "));
        currentWord=splitup[1].replace("_"," ").toLowerCase(); //update the current word to be accessed from other functions
        currentGerman=splitup[1].replace("_"," ").toLowerCase();
        AudioManager am=(AudioManager) getSystemService(AUDIO_SERVICE);
        int volumeLevel = am.getStreamVolume(AudioManager.STREAM_MUSIC); //get the system volume for logging
        int maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float volume = (float)volumeLevel/maxVolume;
        logTimestamp("Presented word="+currentWord+",system volume="+volume);
    }

    public boolean equalsLenient(String response) { /// checks a response to see if it matches including or excluding spaces
        return (response.equals(currentWord)|| response.equals(currentWord.replace(" ","")) || response.replace(" ","").equals(currentWord));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_test);
       if (!isExternalStorageWritable()) { //if we can't write to the log file, abort
            fileError();
        }
        try { //set up the logging, if it doesn't work then go back to the participant screen
            logWriter = new FileWriter(getExternalStorageDirectory()+"/experimentlog.txt",true);
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        logTimestamp("Recall test started");

        correctsound = new MediaPlayer();
        correctsound = MediaPlayer.create(getApplicationContext(), R.raw.correct);
        incorrectsound = new MediaPlayer();
        incorrectsound = MediaPlayer.create(getApplicationContext(), R.raw.incorrect);
        Intent intent = getIntent();
        tPointer = intent.getIntExtra("tPointer",-1);

        if (tPointer == -1 ) {
            minitest=false;
            myPointer=0;
            logTimestamp("test type=2");
        }
        else {
            myPointer=tPointer-4;
            logTimestamp("test type=1");
        }
         prefs = this.getSharedPreferences(
                "edu.northwestern.langlearn", Context.MODE_PRIVATE);
        int stage = prefs.getInt("learningstage", 0);
        int estage = prefs.getInt("experimentstage", 0);
        int today=(int)((((System.currentTimeMillis()/1000)/60)/60)/24);
        prefs.edit().putInt("lastTestTime", today).apply(); // update the "latest test" to show it was today.





        int endIndex=INITIAL_WORDS+(stage*NEW_WORDS);
        String[] tempTranslations=getResources().getStringArray(R.array.translations);
        if (endIndex >= tempTranslations.length) {
            endIndex=tempTranslations.length-1;
        }
        translations= Arrays.copyOfRange(tempTranslations,0,endIndex);
        if (!minitest) { //if this is a real test, let the person know that there will be new words and also increment the number of tests that have been done
            if (estage > 0) { //we're not introducing new words in initial training
                TextView newwords = (TextView) findViewById(R.id.newwords);
                newwords.setVisibility(View.VISIBLE);
            }
            prefs.edit().putInt("learningstage", stage+1).apply();
            Collections.shuffle(Arrays.asList(translations)); //shuffle the translations array
        }

        correct=new int[translations.length]; //set up our array to say if this owrd has been gotten correctly
        for (int i=0; i< correct.length; i++) {
            correct[i]=0;
        }
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE);
        Button submitButton = (Button) findViewById(R.id.nextbutton); //button to speak again
        submitButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                EditText rField = (EditText) findViewById(R.id.response);
                String response = rField.getText().toString().toLowerCase();
                //check to see if they've entered anything
                if (response.length() >= 3) {
                    if (!feedbackState) { //this is the first feedback

                            if (equalsLenient(response)) { //correct
                                correctsound.start();
                                correct[myPointer]++;
                            } else {
                                incorrectsound.start();
                            }

                            logTimestamp("Entered word=" + currentWord + ",response=" + response + ", judgement=" + response.equals(currentWord));
                            TextView english = (TextView) findViewById(R.id.englishword);
                            english.setVisibility(View.VISIBLE);
                            View subject = (View) findViewById(R.id.response);
                            subject.setVisibility(View.GONE);

                    } else {

                        if (minitest) {
                            myPointer = myPointer + 1;
                            if (myPointer <= tPointer && myPointer < translations.length) {
                                updateTrans();
                            } else {
                                Intent myIntent = new Intent(wordTest.this, encoding.class);
                                myIntent.putExtra("tPointer", tPointer + 1);
                                wordTest.this.startActivity(myIntent);
                            }
                        } else { //regular standalone test
                            myPointer = getNextWord(myPointer);
                            if (myPointer == -1) {

                                prefs.edit().putInt("experimentstage", 2).apply(); //we've completed the test, tell the system the next step is the sleep.

                                Intent myIntent = new Intent(wordTest.this, participantMode.class);
                                myIntent.putExtra("status", "Learning Complete");
                                wordTest.this.startActivity(myIntent);
                            } else {
                                updateTrans();
                            }

                        }

                    }
                    feedbackState = !feedbackState;

                }
             else {//they haven't entered anything, just clear the textbox
                // EditText rField   = (EditText)findViewById(R.id.response);
               updateTrans();
                Toast.makeText(wordTest.this,
                        "If you don't know this word, take a guess!", Toast.LENGTH_LONG).show();

            }

            }
        });
    }
    @Override
    protected void onStop() { //close the file when the activity stops.
        super.onStop();
        try {
            logWriter.close();
        }
        catch (Exception e) {
            fileError();
        }

    }
}
