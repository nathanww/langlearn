package edu.northwestern.langlearn;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    SharedPreferences prefs;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
         prefs = this.getSharedPreferences(
                "edu.northwestern.langlearn", Context.MODE_PRIVATE);

        Button sleepButton = (Button) findViewById(R.id.sleep); //button to start sleep mode
        sleepButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, sleepMode.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        Button trainButton = (Button) findViewById(R.id.startinitial); //button to start training
        trainButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, encodingInstructions.class);
                MainActivity.this.startActivity(myIntent);
            }
        });

        Button testButton = (Button) findViewById(R.id.starttest); //button to start word test
        testButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this,wordTest.class);
                MainActivity.this.startActivity(myIntent);
            }
        });



        Button resetButton = (Button) findViewById(R.id.reset); //button to reset progress
        resetButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                prefs.edit().putInt("experimentstage", 1).apply();
                prefs.edit().putInt("learningstage", 0).apply();
                //prefs.edit().putInt("lastTestTime", -1000).apply();

                prefs.edit().putInt("lastTestTime", (int)(((((System.currentTimeMillis()+21600000)/1000)/60)/60)/24)).apply();
            }
        });

        Button nTest = (Button) findViewById(R.id.ntest); //button to test notifications
        nTest.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this,
                        "Scheduled", Toast.LENGTH_LONG).show();
                AlarmManager alarmMgr = (AlarmManager)getSystemService(ALARM_SERVICE);
                Intent intent = new Intent(MainActivity.this, alarmActivity.class);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(MainActivity.this, 0,  intent, PendingIntent.FLAG_UPDATE_CURRENT);
                //alarmMgr.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),10000, pendingIntent);
                alarmMgr.set(AlarmManager.RTC_WAKEUP, System.currentTimeMillis()+10000, pendingIntent);
                //alarmMgr.setAndAllowWhileIdle();
               // Intent myIntent = new Intent(MainActivity.this, alertSubject.class);
               // MainActivity.this.startActivity(myIntent);
            }
        });

        Button pButton = (Button) findViewById(R.id.pmode); //button to start participant mode
        pButton.setOnClickListener( new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, participantMode.class);
                MainActivity.this.startActivity(myIntent);
            }
        });
    }

    private void scheduleNotification(Notification notification, int delay) {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        long futureInMillis = SystemClock.elapsedRealtime() + delay;
        AlarmManager alarmManager = (AlarmManager)getSystemService(Context.ALARM_SERVICE);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis, pendingIntent);
    }

    private Notification getNotification(String content) {
        Notification.Builder builder = new Notification.Builder(this);
        builder.setContentTitle("Scheduled Notification");
        builder.setContentText(content);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        return builder.build();
    }




}
