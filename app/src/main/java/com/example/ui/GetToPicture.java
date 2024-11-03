package com.example.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.InputStream;

public class GetToPicture extends AppCompatActivity {

    public static Context context;
    public static final int REQUEST_CODE = 0;
    public Bitmap img;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        GetToPictures();

    }
    public void GetToPictures() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                try {
                    InputStream in = getContentResolver().openInputStream(data.getData());

                    img = BitmapFactory.decodeStream(in);
                    in.close();

                    Intent intent = new Intent(GetToPicture.this, Recognition.class);
                    startActivity(intent);

                    finish();

                } catch (Exception e) {

                }
            } else if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, "사진선택취소", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(GetToPicture.this, Main.class);
                startActivity(intent);
                finish();
            }
        }
    }
}
