//splaskcreen
package com.example.answer;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 백그라운드 스레드에서 지연 실행
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(3000); // 3초 대기

                    // UI 스레드에서 액티비티 전환
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Intent intent = new Intent(MainActivity.this, Main_Page.class);
                            startActivity(intent);
                            finish();
                        }
                    });
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
}