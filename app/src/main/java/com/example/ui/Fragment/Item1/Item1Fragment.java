package com.example.ui.Fragment.Item1;

import static com.example.ui.Database.db;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import androidx.annotation.NonNull;

import androidx.fragment.app.Fragment;

import com.example.ui.Database;
import com.example.ui.Database.DBHelper;
import com.example.ui.R;
import com.example.ui.databinding.FragmentItem1Binding;

public class Item1Fragment extends Fragment {

    private FragmentItem1Binding binding;
    private ListView listView;
    private SimpleCursorAdapter adapter;
    private DBHelper helper;
    int CheckValue = 1;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentItem1Binding.inflate(inflater, container, false);
        View root = binding.getRoot();

        listView = root.findViewById(R.id.item1_list);

        // Database 초기화
        helper = new DBHelper(getContext());
        SQLiteDatabase db = helper.getWritableDatabase();

        // 데이터베이스에서 햄버거에 대한 데이터들을 가져오기
        String query = "SELECT * FROM contacts WHERE kindValue = ? ORDER BY DATE ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(CheckValue)});
        String[] from = {"name", "date", "use"};
        int[] to = {R.id.text1, R.id.text2, R.id.text3};

        // 어댑터 설정 및 리스트뷰에 연결
        adapter = new SimpleCursorAdapter(getContext(), R.layout.list_item_custom, cursor, from, to, 0);
        listView.setAdapter(adapter);

        // 리스트뷰 항목 클릭 이벤트
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) adapter.getItem(position);
                byte[] barcodeBytes = cursor.getBlob(cursor.getColumnIndexOrThrow("barcode"));
                showPopup(barcodeBytes);
            }
        });

        // 리스트뷰 항목 길게 누르기 이벤트
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = (Cursor) adapter.getItem(position);
                String exchange = cursor.getString(cursor.getColumnIndexOrThrow("exchange"));
                showDeleteConfirmationDialog(exchange);
                return true;
            }
        });
        return root;
    }

    // 팝업창 이벤트
    private void showPopup(byte[] barcodeBytes) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        LayoutInflater inflater = getLayoutInflater();
        View dialogLayout = inflater.inflate(R.layout.dialog_barcode, null);
        ImageView barcodeimageView = dialogLayout.findViewById(R.id.barcode_image_view);

        if (barcodeBytes != null) {
            Bitmap bitmap = BitmapFactory.decodeByteArray(barcodeBytes, 0, barcodeBytes.length);
            barcodeimageView.setImageBitmap(bitmap);
        }

        builder.setView(dialogLayout);
        builder.setPositiveButton("확인", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 길게 눌렀을 때 이벤트
    private void showDeleteConfirmationDialog(String exchange) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("삭제 확인");
        builder.setMessage("정말 삭제 하시겠습니까?");
        builder.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                helper.deleteItem(exchange);
                updateListView();
            }
        });
        builder.setNegativeButton("취소", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 리스트 뷰 갱신
    private void updateListView() {
        if (db == null) {
            db = helper.getWritableDatabase();
        }

        String query = "SELECT * FROM contacts WHERE kindValue = ? ORDER BY DATE ASC";
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(CheckValue)});
        adapter.changeCursor(cursor);
        adapter.notifyDataSetChanged();
    }



    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}