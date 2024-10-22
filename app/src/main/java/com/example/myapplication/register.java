package com.example.myapplication;
import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.text.TextUtils;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;// là một lớp trong AndroidX, một thư viện hỗ trợ cho các ứng dụng Android. Nó là một phần của thư viện androidx.appcompat, được thiết kế để cung cấp khả năng tương thích ngược với các phiên bản Android cũ hơn và hỗ trợ các tính năng mới trong giao diện người dùng.

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

public class register extends AppCompatActivity {
    TextView loginbut;
    EditText rg_fullname, rg_email, rg_password, rg_repassword;
    Button rg_signup;
    FirebaseAuth auth;
    FirebaseDatabase database;
    FirebaseStorage storage;
    String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();

        loginbut = findViewById(R.id.textViewLogin);
        rg_fullname = findViewById(R.id.editTextFullName);
        rg_email = findViewById(R.id.editTextEmailAddress);
        rg_password = findViewById(R.id.editTextPassword);
        rg_repassword = findViewById(R.id.editReEnterTextPassword);
        rg_signup = findViewById(R.id.signupbutton);


        loginbut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(register.this, login.class);
                startActivity(intent);
                finish();
            }
        });
        rg_signup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String fullname = rg_fullname.getText().toString();
                String email = rg_email.getText().toString();
                String Password = rg_password.getText().toString();
                String cPassword = rg_repassword.getText().toString();
                String status = "Xin Chào Các Con Dợ Xinh Yêu.";

                if (TextUtils.isEmpty(fullname) || TextUtils.isEmpty(email) || TextUtils.isEmpty(Password) || TextUtils.isEmpty(cPassword)){
                    Toast.makeText(register.this, "Xin hãy điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
                }else if(!email.matches(emailPattern)){
                    rg_email.setError("Type A valid Email Here");
                }else if(Password.length()<8){
                    rg_password.setError("Mật khẩu phải hơn 8 kí tự.");
                }else if(!Password.equals(cPassword)){
                    rg_password.setError("Mật khẩu không giống nhau!");
                }else{
                    auth.createUserWithEmailAndPassword(email, Password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                        @Override
                        public void onComplete(@NonNull Task<AuthResult> task) {
                            if(task.isSuccessful()){
                                String id = task.getResult().getUser().getUid();
                                DatabaseReference reference = database.getReference().child("user").child(id);
                                StorageReference storageReference = storage.getReference().child("Upload").child(id);

                                // Không cần kiểm tra imageURI nữa vì không còn ảnh
                                String status = "Xin Chào Các Con Dợ Xinh Yêu.";
                                Users users = new Users(id, fullname, email, Password, status);

                                // Lưu người dùng vào Firebase Database mà không cần URL ảnh
                                reference.setValue(users).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if (task.isSuccessful()) {
                                            // Chuyển đến MainActivity khi đăng ký thành công
                                            Intent intent = new Intent(register.this, MainActivity.class);
                                            startActivity(intent);
                                            finish();
                                        } else {
                                            // Thông báo lỗi nếu không tạo được tài khoản
                                            Toast.makeText(register.this, "Error in creating the user", Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });
                            }else {
                                Toast.makeText(register.this, task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }
            }
        });
    }
}
