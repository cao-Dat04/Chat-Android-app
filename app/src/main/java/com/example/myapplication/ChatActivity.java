package com.example.myapplication;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.model.msgModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;

public class ChatActivity extends AppCompatActivity {

    String reciverUID, reciverName, SenderUID;
    TextView reciverNameAc;
    CardView sendbtn;
    EditText textmsg;
    FirebaseAuth firebaseAuth;
    FirebaseDatabase database;
    String senderRoom, reciverRoom;
    RecyclerView msgAdapter;
    ArrayList<msgModel> messagesArrayList;
    messagesAdapter messagesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        firebaseAuth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance();

        reciverName = getIntent().getStringExtra("name");
        reciverUID = getIntent().getStringExtra("uid");

        messagesArrayList = new ArrayList<>();

        // Setup RecyclerView
        msgAdapter = findViewById(R.id.msgadapter);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        msgAdapter.setLayoutManager(linearLayoutManager);
        messagesAdapter = new messagesAdapter(ChatActivity.this, messagesArrayList);
        msgAdapter.setAdapter(messagesAdapter);

        sendbtn = findViewById(R.id.sendbtn);
        textmsg = findViewById(R.id.textmsg);
        reciverNameAc = findViewById(R.id.recivername);

        reciverNameAc.setText(reciverName);

        SenderUID = firebaseAuth.getUid();
        senderRoom = SenderUID + reciverUID;
        reciverRoom = reciverUID + SenderUID;

        DatabaseReference chatreference = database.getReference().child("chats").child(senderRoom).child("messages");

        // Lắng nghe và cập nhật tin nhắn
        chatreference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                messagesArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    msgModel messages = dataSnapshot.getValue(msgModel.class);
                    messagesArrayList.add(messages);
                }
                messagesAdapter.notifyDataSetChanged();

                if (messagesArrayList.size() > 0) {
                    msgAdapter.scrollToPosition(messagesArrayList.size() - 1);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ChatActivity.this, "Error loading messages", Toast.LENGTH_SHORT).show();
            }
        });

        // Gửi tin nhắn văn bản
        sendbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String message = textmsg.getText().toString();
                if (message.isEmpty()) {
                    Toast.makeText(ChatActivity.this, "Enter The Message First", Toast.LENGTH_SHORT).show();
                    return;
                }

                textmsg.setText("");
                Date date = new Date();
                msgModel messagess = new msgModel(message, SenderUID, date.getTime(), "text", null);

                // Lưu tin nhắn vào Firebase
                database.getReference().child("chats").child(senderRoom)
                        .child("messages").push().setValue(messagess)
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if (task.isSuccessful()) {
                                    database.getReference().child("chats").child(reciverRoom)
                                            .child("messages").push().setValue(messagess);

                                    msgAdapter.scrollToPosition(messagesArrayList.size() - 1);
                                }
                            }
                        });
            }
        });

        ImageButton sendImage = findViewById(R.id.sendImage);
        ImageButton sendFile = findViewById(R.id.sendFile);

        // Chọn ảnh
        sendImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectImage();
            }
        });

        // Chọn file
        sendFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectFile();
            }
        });

        // Kiểm tra quyền truy cập bộ nhớ
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 100);
        }
    }

    // Chọn ảnh từ bộ nhớ
    private void selectImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    // Chọn file từ bộ nhớ
    private void selectFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(intent, 2);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @NonNull Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri fileUri = data.getData();
            if (requestCode == 1) {  // Xử lý ảnh
                uploadToFirebaseStorage(fileUri, "image");
            } else if (requestCode == 2) {  // Xử lý file
                uploadToFirebaseStorage(fileUri, "file");
            }
        }
    }

    // Upload ảnh hoặc file lên Firebase Storage
    private void uploadToFirebaseStorage(Uri fileUri, String fileType) {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        String fileName = getFileName(fileUri); // Lấy tên file
        StorageReference storageReference = storage.getReference().child("uploads").child(System.currentTimeMillis() + "." + getFileExtension(fileUri));

        storageReference.putFile(fileUri).addOnSuccessListener(taskSnapshot ->
                storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                    String fileUrl = uri.toString();
                    Date date = new Date();
                    msgModel messagess = new msgModel(fileUrl, SenderUID, date.getTime(), fileType, fileName); // Thêm tên file vào đây

                    database.getReference().child("chats").child(senderRoom)
                            .child("messages").push().setValue(messagess)
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    database.getReference().child("chats").child(reciverRoom)
                                            .child("messages").push().setValue(messagess);
                                }
                            });
                })
        ).addOnFailureListener(e -> {
            Toast.makeText(ChatActivity.this, "Failed to upload file", Toast.LENGTH_SHORT).show();
        });
    }

    // Lấy tên file từ URI
    private String getFileName(Uri uri) {
        String fileName = "";
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                fileName = cursor.getString(nameIndex);
                cursor.close();
            }
        } else {
            fileName = uri.getLastPathSegment();
        }
        return fileName != null ? fileName : "unknown_file"; // Nếu không có tên, trả về "unknown_file"
    }

    // Lấy phần mở rộng của file
    private String getFileExtension(Uri uri) {
        String extension = "";
        String[] split = uri.toString().split("\\.");
        if (split.length > 0) {
            extension = split[split.length - 1];
        }
        return extension;
    }
}
