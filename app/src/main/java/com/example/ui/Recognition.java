package com.example.ui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Recognition extends AppCompatActivity {

    public static Context context;
    int width, height;
    Bitmap bitmap;  // 사용할 이미지

    Bitmap NumberBitmap, UseBitmap, ValidityBitmap, ExchangeBitmap, BarcodeBitmap;  // 각 텍스트 영역에 대한 비트맵

    TessBaseAPI tessBaseAPI;
    String datapath = "";

    public String NumberString, UseString, ValidString, ExchangeString;

    @SuppressLint("MissingInflatedId")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        context = this;

        datapath = getFilesDir() + "/tesseract/";

        bitmap = ((GetToPicture) GetToPicture.context).img;

        width = bitmap.getWidth();
        height = bitmap.getHeight();

        Log.d("BitmapSize", "가로길이 : " + width + ", 세로길이 : " + height);

        // 트레이닝 데이터 확인
        checkFile(new File(datapath + "tessdata/"), "kor");
        checkFile(new File(datapath + "tessdata/"), "eng");

        tessBaseAPI = new TessBaseAPI();

        // ROI 설정
        Rect Number = new Rect((int) (0.1439 * width), (int) (0.6597 * height), (int) (0.705 * width), (int) (0.0694 * height));  // 상품 코드
        Rect Use = new Rect((int) (0.6475 * width), (int) (0.6944 * height), (int) (0.259 * width), (int) (0.0694 * height));  // 교환처
        Rect Validity = new Rect((int) (0.4964 * width), (int) (0.7638 * height), (int) (0.4100 * width), (int) (0.0694 * height));  // 유효기간
        Rect Exchange = new Rect((int) (0.431 * width), (int) (0.833 * height), (int) (0.474 * width), (int) (0.0694 * height));  // 교환번호
//        Rect Barcode = new Rect((int) (0.1366 * width), (int) (0.6597 * height), (int) (0.7266 * width), (int) (0.1041 * height));  // 바코드
        Rect Barcode = new Rect((int) (0.1151 * width), (int) (0.5486 * height), (int) (0.7769 * width), (int) (0.1180 * height));

        bitmap = increaseContrast(bitmap);

        // 비트맵 생성
        NumberBitmap = Bitmap.createBitmap(bitmap, Number.left, Number.top, Number.right, Number.bottom);  // 상품 코드 인식
        UseBitmap = Bitmap.createBitmap(bitmap, Use.left, Use.top, Use.right, Use.bottom);  // 교환처 인식
        ValidityBitmap = Bitmap.createBitmap(bitmap, Validity.left, Validity.top, Validity.right, Validity.bottom);  // 유효기간
        ExchangeBitmap = Bitmap.createBitmap(bitmap, Exchange.left, Exchange.top, Exchange.right, Exchange.bottom);  // 교환번호
        BarcodeBitmap = Bitmap.createBitmap(bitmap, Barcode.left, Barcode.top, Barcode.right, Barcode.bottom); // 바코드

        // 이미지에서 텍스트 인식
        processImage();
    }

    private Bitmap increaseContrast(Bitmap original) {
        Bitmap bmp = original.copy(Bitmap.Config.ARGB_8888, true);
        Paint paint = new Paint();
        Color color = new Color();
        int A, R, G, B;
        int pixelColor;
        double contrastLevel = 1.5;

        for (int y = 0; y < bmp.getHeight(); y++) {
            for (int x = 0; x < bmp.getWidth(); x++) {
                pixelColor = bmp.getPixel(x, y);
                A = color.alpha(pixelColor);
                R = Math.min(255, (int)(color.red(pixelColor) * contrastLevel));
                G = Math.min(255, (int)(color.green(pixelColor) * contrastLevel));
                B = Math.min(255, (int)(color.blue(pixelColor) * contrastLevel));
                bmp.setPixel(x, y, color.argb(A, R, G, B));
            }
        }
        return bmp;
    }

    // 텍스트 인식 처리
    public void processImage() {

        tessBaseAPI.init(datapath, "kor");
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);

        // 교환번호
        tessBaseAPI.setImage(NumberBitmap);
        NumberString = tessBaseAPI.getUTF8Text().replaceAll("[^0-9]", "").trim();

        // 유효기간
        tessBaseAPI.setImage(ValidityBitmap);
        ValidString = tessBaseAPI.getUTF8Text().trim();

        // 주문번호
        tessBaseAPI.setImage(ExchangeBitmap);
        ExchangeString = tessBaseAPI.getUTF8Text().replaceAll("[^0-9]", "").trim();

        tessBaseAPI.init(datapath, "kor+eng");
        tessBaseAPI.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK);

        // 사용처
        tessBaseAPI.setImage(UseBitmap);
        UseString = tessBaseAPI.getUTF8Text().trim();

        Intent intent = new Intent(Recognition.this, Database.class);
        startActivity(intent);
        finish();
    }

    // 파일 체크 및 복사
    private void checkFile(File dir, String lang) {
        if (!dir.exists() && dir.mkdirs()) {
            copyFiles(lang);
        }
        if (dir.exists()) {
            String datafilepath = datapath + "/tessdata/" + lang + ".traineddata";
            File datafile = new File(datafilepath);
            if (!datafile.exists()) {
                copyFiles(lang);
            }
        }
    }

    // 파일 복사
    private void copyFiles(String lang) {
        try {
            String filepath = datapath + "/tessdata/" + lang + ".traineddata";
            AssetManager assetManager = getAssets();
            InputStream instream = assetManager.open("tessdata/" + lang + ".traineddata");
            OutputStream outstream = new FileOutputStream(filepath);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, read);
            }
            outstream.flush();
            outstream.close();
            instream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
