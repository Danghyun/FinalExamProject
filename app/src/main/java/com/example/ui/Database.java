package com.example.ui;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.SimpleCursorAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;

import com.example.ui.Fragment.Entire.EntireFragment;

import java.io.ByteArrayOutputStream;
import java.util.Objects;

public class Database extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    public static Database instance;
    DBHelper helper;
    public static SQLiteDatabase db;
    EditText edit_name, edit_number, edit_use, edit_date, edit_exchange, edit_kind;
    String Number, Use, Valid, Exchange;
    Bitmap Barcode;

    public String name, number, use, date, exchange, kind;

    int kindValue;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_database);

        instance = this;

        edit_name = findViewById(R.id.name);
        edit_number = findViewById(R.id.number);
        edit_use = findViewById(R.id.use);
        edit_date = findViewById(R.id.date);
        edit_exchange = findViewById(R.id.exchange);
        edit_kind = findViewById(R.id.kind);

        Number = ((Recognition)Recognition.context).NumberString;  // 교환번호 가져오기
        Use = ((Recognition)Recognition.context).UseString;  // 사용처 가져오기
        Valid = ((Recognition)Recognition.context).ValidString;  // 유효기간 가져오기
        Exchange = ((Recognition)Recognition.context).ExchangeString;  // 교환처 가져오기
        Barcode = ((Recognition)Recognition.context).BarcodeBitmap;  // 바코드 가져오기

        Log.d("사진 인식 테스트", "상품코드 : " + Number);
        Log.d("사진 인식 테스트", "사용처 : " + Use);
        Log.d("사진 인식 테스트", "유효기간 : " + Valid);
        Log.d("사진 인식 테스트", "교환번호 : " + Exchange);

        // 글자 인식 오류 수정
        if (Objects.equals(Use, "6525")) {
            edit_use.setText("GS25");
        } else {
            edit_use.setText(Use);
        }

        edit_number.setText(Number);
        edit_date.setText(Valid);
        edit_exchange.setText(Exchange);

        helper = new DBHelper(this);
        db = helper.getWritableDatabase();

        // Loader 초기화
        LoaderManager.getInstance(this).initLoader(0, null, this);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.DataBase), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    public void onClick(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenuInflater().inflate(R.menu.popup, popup.getMenu());
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                edit_kind.setText(item.getTitle());
                return true;
            }
        });
        popup.show();
    }

    public void setting(View target){
        name = edit_name.getText().toString();
        number = edit_number.getText().toString();
        use = edit_use.getText().toString();
        date = edit_date.getText().toString();
        exchange = edit_exchange.getText().toString();
        kind = edit_kind.getText().toString();

        kindValue = 0;

        if (kind.equals("햄버거")) {
            kindValue = 1;
        } else if (kind.equals("치킨")) {
            kindValue = 2;
        } else if (kind.equals("피자")) {
            kindValue = 3;
        } else if (kind.equals("아이스크림")) {
            kindValue = 4;
        } else if (kind.equals("과자")) {
            kindValue = 5;
        } else if (kind.equals("기타")) {
            kindValue = 6;
        }

        // 상품명, 교환번호, 사용처, 유효기간, 교환처, 종류가 비어있지 않다면 데이터베이스에 등록
        if (name != null && number != null && use != null && date != null && exchange != null && kindValue != 0) {

            // 바코드 비트맵을 바이트 배열로 변환
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Barcode.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
            byte[] barcodeBytes = outputStream.toByteArray();

            Cursor cursorCheck = db.rawQuery("SELECT 1 FROM contacts WHERE exchange = ?", new String[]{exchange});
            if (cursorCheck.moveToFirst()) {
                //중복된 경우
                Toast.makeText(getApplicationContext(), "중복된 사진입니다.", Toast.LENGTH_LONG).show();
            } else {
                // 중복되지 않은 경우
                db.execSQL("INSERT INTO contacts (name, number, use, date, exchange, kindValue) VALUES ('" + name + "','" + number + "', '" + use + "','" + date + "','" + exchange + "'," + kindValue + ");");

                // 바코드 이미지를 ContentValues를 사용하여 업데이트
                ContentValues values = new ContentValues();
                values.put("barcode", barcodeBytes);
                db.update("contacts", values, "exchange = ?", new String[]{exchange});

                Toast.makeText(getApplicationContext(), "추가 완료!", Toast.LENGTH_SHORT).show();

                edit_name.setText("");
                edit_number.setText("");
                edit_use.setText("");
                edit_date.setText("");
                edit_exchange.setText("");
            }
            Intent intent = new Intent(Database.this, Main.class);
            startActivity(intent);
            finish();
            cursorCheck.close();

        } else if (TextUtils.isEmpty(name)) {  // 상품명이 비어있을 경우
            Toast.makeText(getApplication(), "상품명을 입력해주세요.", Toast.LENGTH_SHORT).show();
        } else if (number == null) {  // 교환번호가 비어있을 경우
            Toast.makeText(getApplication(), "인식실패. 기프티콘을 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
        } else if (use == null) {
            Toast.makeText(getApplication(), "인식실패. 기프티콘을 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
        } else if (date == null) {
            Toast.makeText(getApplication(), "인식실패. 기프티콘을 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
        } else if (exchange == null) {
            Toast.makeText(getApplication(), "인식실패. 기프티콘을 다시 선택해주세요.", Toast.LENGTH_SHORT).show();
        } else if (kindValue == 0){
            Toast.makeText(getApplication(), "상품의 종류를 선택해주세요", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // CursorLoader 생성
        return new CursorLoader(this, MyContentProvider.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    public static class DBHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "mycontacts.db";
        private static final int DATABASE_VERSION = 1;

        public DBHelper(Context context){
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE contacts (_id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT, number TEXT, use TEXT, date DATE, exchange TEXT UNIQUE, kindValue INTEGER, barcode BLOB);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS contacts");
            onCreate(db);
        }

        public void deleteItem(String exchange) {
            SQLiteDatabase db = this.getWritableDatabase();
            db.delete("contacts", "exchange = ?", new String[]{exchange});
        }
    }

    public static class MyContentProvider extends ContentProvider {
        private static final String AUTHORITY = "com.example.myapp.mycontentprovider";
        private static final String BASE_PATH = "contacts";
        public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/" + BASE_PATH);
        private static final int CONTACTS = 1;
        private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
        private SQLiteDatabase database;

        static {
            uriMatcher.addURI(AUTHORITY, BASE_PATH, CONTACTS);
        }

        @Override
        public boolean onCreate() {
            DBHelper dbHelper = new DBHelper(getContext());
            database = dbHelper.getWritableDatabase();
            return true;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
            return database.query("contacts", projection, selection, selectionArgs, null, null, sortOrder);
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            long id = database.insert("contacts", null, values);
            if (id > 0) {
                return Uri.parse(BASE_PATH + "/" + id);
            }
            throw new SQLException("Failed to insert row into " + uri);
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return database.delete("contacts", selection, selectionArgs);
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return database.update("contacts", values, selection, selectionArgs);
        }
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(Database.this, Main.class);
        startActivity(intent);
        finish();
    }
}