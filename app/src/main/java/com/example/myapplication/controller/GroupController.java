package com.example.myapplication.controller;

import com.example.myapplication.model.Group;
import com.example.myapplication.model.GroupMember;
import com.example.myapplication.model.Users;
import com.example.myapplication.view.CreateGroupActivity;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class GroupController {

    private CreateGroupActivity createGroupActivity;
    private FirebaseDatabase database;

    // Constructor nhận vào View (CreateGroupActivity)
    public GroupController(CreateGroupActivity createGroupActivity) {
        this.createGroupActivity = createGroupActivity;
        this.database = FirebaseDatabase.getInstance();
    }

    // Tạo nhóm mới
    public void createGroup(String groupName, List<Users> selectedUsers, String adminId) {
        // Kiểm tra thông tin nhóm trước khi tạo
        if (groupName == null || groupName.isEmpty()) {
            createGroupActivity.showErrorMessage("Tên nhóm không được để trống");
            return;
        }

        // Tạo Group object
        Group group = new Group();
        group.setGroupName(groupName);
        group.setCreatedAt(System.currentTimeMillis());
        group.setGroupId("group_" + System.currentTimeMillis());  // Tạo ID nhóm (ví dụ: dùng timestamp)
        group.setAdminId(adminId);  // Thiết lập ID quản trị viên nhóm

        // Gửi dữ liệu nhóm và thành viên vào database
        saveGroupToDatabase(group, selectedUsers);
    }

    // Lưu nhóm vào cơ sở dữ liệu (Firebase hoặc các cơ sở dữ liệu khác)
    private void saveGroupToDatabase(Group group, List<Users> selectedUsers) {
        DatabaseReference groupReference = database.getReference().child("groups").child(group.getGroupId());

        // Lưu nhóm vào Firebase
        groupReference.setValue(group).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Sau khi lưu nhóm vào DB, thêm các thành viên vào nhóm
                for (Users user : selectedUsers) {
                    GroupMember groupMember = new GroupMember(group.getGroupId(), user.getUserId(), System.currentTimeMillis());
                    saveGroupMemberToDatabase(groupMember);
                }

                // Cập nhật UI sau khi nhóm đã được tạo thành công
                createGroupActivity.onGroupCreated(group); // Gọi phương thức này với đối tượng group vừa tạo
            } else {
                createGroupActivity.showErrorMessage("Lỗi khi tạo nhóm. Vui lòng thử lại.");
            }
        });
    }

    // Lưu thành viên vào cơ sở dữ liệu (Firebase hoặc các cơ sở dữ liệu khác)
    private void saveGroupMemberToDatabase(GroupMember groupMember) {
        DatabaseReference groupMemberReference = database.getReference().child("group_members")
                .child(groupMember.getGroupId()).child(groupMember.getUserId());

        // Lưu thành viên vào Firebase
        groupMemberReference.setValue(groupMember).addOnCompleteListener(task -> {
            if (!task.isSuccessful()) {
                createGroupActivity.showErrorMessage("Lỗi khi thêm thành viên vào nhóm.");
            }
        });
    }
}
