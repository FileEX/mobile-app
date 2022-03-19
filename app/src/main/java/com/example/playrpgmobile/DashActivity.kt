package com.example.playrpgmobile

import android.annotation.SuppressLint
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

class DashActivity : AppCompatActivity() {
    private var userdata = JSONObject();

    @SuppressLint("SimpleDateFormat")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dash);
        setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);

        val userdataString = intent.getStringExtra("UserData").toString();
        userdata = JSONObject(userdataString);

        val login = userdata.getString("login");

        val loginText = findViewById<TextView>(R.id.loginText);
        loginText.text = login;

        val premiumdate = userdata.getString("premium_date");
        val dateFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        val endDate = dateFormat.parse(premiumdate);
        val currDate = dateFormat.parse(dateFormat.format(Calendar.getInstance().time).toString());

        if (premiumdate == "0000-00-00 00:00:00" || (currDate != null && currDate.after(endDate))) {
            loginText.setTextColor(Color.parseColor("#ffffffff"));
        }
    }
}