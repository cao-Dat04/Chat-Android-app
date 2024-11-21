package com.example.myapplication.view;

import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Group;
import com.example.myapplication.model.Users;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

public class MainActivityGroup extends AppCompatActivity {

    FirebaseDatabase database;
    DatabaseReference reference;
    RecyclerView recyclerView;
    GroupAdapter adapter;
    ArrayList<Group> groupArrayList;
    ImageView imglogout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_group); // Đảm bảo layout đã được thiết lập

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance();
        reference = database.getReference().child("groups");

        // Khởi tạo RecyclerView và Adapter
        recyclerView = findViewById(R.id.groupChatText); // Đảm bảo ID đúng
        if (recyclerView != null) {
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        } else {
            Log.e("MainActivityGroup", "RecyclerView is null");
        }

        groupArrayList = new ArrayList<>();
        adapter = new GroupAdapter(this, groupArrayList);
        recyclerView.setAdapter(adapter);

        // Lấy dữ liệu người dùng từ Firebase và cập nhật Adapter
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                groupArrayList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Group group = dataSnapshot.getValue(Group.class);
                    if (group != null) {
                        groupArrayList.add(group);
                    }
                }
                adapter.notifyDataSetChanged(); // Cập nhật dữ liệu trong Adapter
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Log.e("FirebaseError", "Database error: " + error.getMessage());
            }
        });

        // Xử lý sự kiện đăng xuất
        imglogout = findViewById(R.id.logoutimg);
        imglogout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLogoutDialog();
            }
        });

        // Xử lý sự kiện khi nhấn vào chatGroup
        ImageView chatOneOne = findViewById(R.id.chatOneOne);
        chatOneOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chuyển sang activity nhóm chat
                Intent intent = new Intent(MainActivityGroup.this, MainActivity.class);
                startActivity(intent);
            }
        });

        // Xử lý sự kiện khi nhấn vào createGroup
        ImageView createGroup = findViewById(R.id.createGroup);
        createGroup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Chuyển sang activity tạo nhóm
                Intent intent = new Intent(MainActivityGroup.this, CreateGroupActivity.class);
                startActivity(intent);
            }
        });

        // Cập nhật sự kiện nhấn vào nhóm chat trong RecyclerView
        adapter.setOnItemClickListener(new GroupAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Group group) {
                // Chuyển sang GroupChatActivity khi nhấn vào một nhóm
                Intent intent = new Intent(MainActivityGroup.this, GroupChatActivity.class);
                intent.putExtra("groupId", group.getGroupId()); // Truyền groupId
                intent.putExtra("groupName", group.getGroupName()); // Truyền tên nhóm
                startActivity(intent);
            }
        });
    }

    // Phương thức hiển thị dialog đăng xuất
    private void showLogoutDialog() {
        Dialog dialog = new Dialog(MainActivityGroup.this, R.style.dialoge);
        dialog.setContentView(R.layout.dialog_layout);
        Button no = dialog.findViewById(R.id.nobnt);
        Button yes = dialog.findViewById(R.id.yesbnt);

        yes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Đăng xuất người dùng
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(MainActivityGroup.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Đóng MainActivityGroup sau khi đăng xuất
            }
        });

        no.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dialog.dismiss(); // Đóng dialog nếu chọn "Không"
            }
        });

        dialog.show();
    }
}
