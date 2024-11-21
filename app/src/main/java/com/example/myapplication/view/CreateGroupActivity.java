package com.example.myapplication.view;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.controller.GroupController;
import com.example.myapplication.model.Group;
import com.example.myapplication.model.Users;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class CreateGroupActivity extends AppCompatActivity {

    private GroupController groupController;
    private UserSearchAdapter userAdapter;
    private EditText groupNameEditText, searchUsersEditText;
    private RecyclerView userRecyclerView;
    private List<Users> userList; // Danh sách người dùng
    private List<Users> filteredUserList; // Danh sách người dùng lọc từ tìm kiếm
    private List<Users> selectedUsers; // Danh sách người dùng đã chọn

    private FirebaseDatabase database;
    private DatabaseReference reference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_group);

        // Khởi tạo các thành phần giao diện
        groupNameEditText = findViewById(R.id.groupName);
        searchUsersEditText = findViewById(R.id.searchUsers);
        userRecyclerView = findViewById(R.id.userRecyclerView);

        // Khởi tạo danh sách người dùng và danh sách đã chọn
        userList = new ArrayList<>();
        filteredUserList = new ArrayList<>();
        selectedUsers = new ArrayList<>();

        // Khởi tạo UserSearchAdapter
        userAdapter = new UserSearchAdapter(this, filteredUserList, new UserSearchAdapter.OnUserSelectedListener() {
            @Override
            public void onUserSelected(Users user) {
                // Thêm hoặc loại bỏ người dùng vào/ra danh sách đã chọn
                if (selectedUsers.contains(user)) {
                    selectedUsers.remove(user);
                } else {
                    selectedUsers.add(user);
                }
            }
        });

        // Cài đặt RecyclerView để hiển thị danh sách người dùng
        userRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userRecyclerView.setAdapter(userAdapter);

        // Khởi tạo Firebase
        database = FirebaseDatabase.getInstance();
        reference = database.getReference().child("user");

        // Lấy dữ liệu người dùng từ Firebase và cập nhật adapter
        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                userList.clear();
                for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                    Users user = dataSnapshot.getValue(Users.class);
                    userList.add(user);
                }
                filterUserList(""); // Lọc lại danh sách khi dữ liệu thay đổi
            }

            @Override
            public void onCancelled(DatabaseError error) {
                showErrorMessage("Lỗi khi tải dữ liệu người dùng.");
            }
        });

        // Khởi tạo GroupController với Context hiện tại (CreateGroupActivity)
        groupController = new GroupController(this);

        // Xử lý sự kiện khi người dùng nhấn nút tạo nhóm
        findViewById(R.id.createButton).setOnClickListener(v -> {
            String groupName = groupNameEditText.getText().toString();

            // Kiểm tra xem có người dùng được chọn hay không
            if (selectedUsers.isEmpty()) {
                showErrorMessage("Bạn chưa chọn thành viên cho nhóm.");
                return;
            }

            // Tạo tên nhóm tự động nếu không có
            if (groupName.isEmpty()) {
                groupName = generateGroupName(selectedUsers);
            }

            // Giả sử adminId là ID của người dùng hiện tại
            String adminId = "admin_user_id"; // Thay thế bằng ID của người tạo nhóm (có thể lấy từ Firebase Auth)

            // Gọi GroupController để tạo nhóm
            groupController.createGroup(groupName, selectedUsers, adminId);
        });

        // Thêm TextWatcher để theo dõi thay đổi trong trường tìm kiếm người dùng
        searchUsersEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                // Lọc danh sách người dùng khi có thay đổi trong ô tìm kiếm
                filterUserList(charSequence.toString());
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void filterUserList(String query) {
        filteredUserList.clear();
        if (query.isEmpty()) {
            filteredUserList.addAll(userList);
        } else {
            for (Users user : userList) {
                if (user.getFullname().toLowerCase().contains(query.toLowerCase())) {
                    filteredUserList.add(user);
                }
            }
        }
        userAdapter.notifyDataSetChanged();
    }

    private String generateGroupName(List<Users> selectedUsers) {
        StringBuilder groupName = new StringBuilder();
        for (int i = 0; i < Math.min(3, selectedUsers.size()); i++) {
            groupName.append(selectedUsers.get(i).getFullname());
            if (i < 2 && i < selectedUsers.size() - 1) {
                groupName.append(", ");
            }
        }
        if (selectedUsers.size() > 3) {
            groupName.append(",...");
        }
        return groupName.toString();
    }

    public void showErrorMessage(String message) {
        // Hiển thị thông báo lỗi
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    // Phương thức này sẽ trả về danh sách người dùng đã chọn
    public List<Users> getSelectedUsers() {
        return selectedUsers;
    }

    public void onGroupCreated(Group group) {
        // Hiển thị thông báo thành công khi nhóm được tạo
        Toast.makeText(this, "Nhóm '" + group.getGroupName() + "' đã được tạo thành công!", Toast.LENGTH_SHORT).show();

        // Nếu muốn chuyển sang màn hình khác (ví dụ: danh sách nhóm)
        Intent intent = new Intent(CreateGroupActivity.this, Group.class);
        startActivity(intent);

        // Hoặc nếu chỉ muốn quay lại màn hình trước
        finish();
    }
}
