package com.example.aistudymentor

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Trỏ thẳng đến giao diện Đăng nhập (XML)
        setContentView(R.layout.activity_login)
    }
}