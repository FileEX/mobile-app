package com.example.playrpgmobile

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.TextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.edit
import at.favre.lib.crypto.bcrypt.BCrypt
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.toxicbakery.bcrypt.Bcrypt
import org.json.JSONArray
import org.json.JSONException
import java.io.IOException
import java.lang.StringBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*

class LoginActivity : AppCompatActivity() {
    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("CommitPrefEdits", "SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main);
        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_YES);

        val copyright = findViewById<TextView>(R.id.textView2);
        val year = Calendar.getInstance().get(Calendar.YEAR);

        copyright.text = "Play RPG © $year";

        val SharedPref = this.getPreferences(Context.MODE_PRIVATE);

        val login_Saved = SharedPref.getString("login", "").toString();
        val pass_Saved = SharedPref.getString("pass", "").toString();
        val pin_Saved = SharedPref.getString("pin", "").toString();

        if (login_Saved.isNotEmpty()) {
            val queue = Volley.newRequestQueue(this);
            val url = "http://14iu.web.svpj.pl/index.php/user/isUserExists?login=" + login_Saved;

            if (!isOnline(this)) {
                Toast.makeText(this, "Nie można nawiązać połączenia z internetem." , Toast.LENGTH_LONG).show();

                val layout = findViewById<ConstraintLayout>(R.id.loginLayout);
                layout.visibility = View.GONE;

                return;
            }

            val stringRequest = StringRequest(Request.Method.GET,
                url,
                { response ->
                    try {
                        tryLogin(response, login_Saved, pass_Saved, pin_Saved, true);
                    } catch (e: JSONException) {

                    }
                },
                Response.ErrorListener { error ->
                    Toast.makeText(
                        this,
                        "Error ${error.message}",
                        Toast.LENGTH_LONG
                    ).show();
                });

            queue.add(stringRequest);
        }

        val btnLogin = findViewById<Button>(R.id.loginBtn);
        val notif = findViewById<TextView>(R.id.notifText);

        notif.visibility = View.GONE;

        btnLogin.setOnClickListener {
            val loginEdit = findViewById<EditText>(R.id.loginEdit);
            val passEdit = findViewById<EditText>(R.id.passEdit);
            val pinEdit = findViewById<EditText>(R.id.pinEdit);

            val login = loginEdit.getText().toString().trim();
            val pass = passEdit.getText().toString().trim();
            val pin = pinEdit.getText().toString().trim();

            if ((login.isNotEmpty() && login.length >= 3 && login.length <= 11) && (pass.isNotEmpty() && pass.length >= 6 && pass.length <= 20) && (pin.length in 1..4)) {
                if (!isOnline(this)) {
                    Toast.makeText(this, "Nie udało się nawiązać połączenia z internetem." , Toast.LENGTH_LONG).show();
                    return@setOnClickListener;
                }

                val queue = Volley.newRequestQueue(this);
                val url = "http://14iu.web.svpj.pl/index.php/user/isUserExists?login=" + login;

                val stringRequest = StringRequest(Request.Method.GET,
                    url,
                    { response ->
                        tryLogin(response, login, pass, pin, false);
                    },
                    Response.ErrorListener { error ->
                        Toast.makeText(
                            this,
                            "Error ${error.message}",
                            Toast.LENGTH_LONG
                        ).show();
                    });
                queue.add(stringRequest);
            } else {
                createNotification("Poniższe pola zostały niepoprawnie wypełnione.");
            }
        }
    }

    private fun tryLogin(response: String, login: String, pass: String, pin: String, withoutInfo: Boolean) {
        try {
            val jsonData = response.toString();
            val convJSON = JSONArray(jsonData);

            //Toast.makeText(this, "response,"+response.toString()+"" , Toast.LENGTH_LONG).show();

            val SharedPref = this.getPreferences(Context.MODE_PRIVATE);
            val editor = SharedPref.edit();

            if (convJSON.length() > 0) {
                val jObj = convJSON.getJSONObject(0);

                val loginValue = jObj.getString("login");
                val password = jObj.getString("password");
                val pinValue = jObj.getString("pin");

                if (Bcrypt.verify(pass, password.toByteArray())) {
                    if (md5(pin)?.uppercase() == pinValue) {
                        val intent = Intent(this, DashActivity::class.java);
                        intent.putExtra("UserData", jObj.toString());

                        editor.putString("login", login);
                        editor.putString("pass", pass);
                        editor.putString("pin", pin);
                        editor.apply();

                        startActivity(intent);
                        finish();
                    } else {
                        if (!withoutInfo) {
                            createNotification("Wprowadzone hasło lub PIN są niepoprawne.");
                        } else {
                            editor.remove("login");
                            editor.remove("pass");
                            editor.remove("pin");
                        }
                    }
                } else {
                    if (!withoutInfo) {
                        createNotification("Wprowadzone hasło lub PIN są niepoprawne.");
                    } else {
                        editor.remove("login");
                        editor.remove("pass");
                        editor.remove("pin");
                    }
                }
            } else {
                if (!withoutInfo) {
                    createNotification("Konto o takim loginie nie istnieje.");
                } else {
                    editor.remove("login");
                    editor.remove("pass");
                    editor.remove("pin");
                }
            }
        } catch (e: JSONException) {
            //Toast.makeText(this, "response,"+response.toString()+"" , Toast.LENGTH_LONG).show();
        }
    }

    private val hideNotif = Runnable() {
        val notif = findViewById<TextView>(R.id.notifText);

        notif.animate().alpha(0f).setDuration(1000).withEndAction {
            notif.visibility = View.GONE;
        }
    }

    private fun createNotification(title: String) {
        val notif = findViewById<TextView>(R.id.notifText);

        //if (notif.visibility == View.GONE) {
            notif.alpha = 0f;
            notif.visibility = View.VISIBLE;
            notif.text = title;

            notif.animate().alpha(1f).setDuration(1000).setListener(null);

            val h = Handler();
            h.removeCallbacks(hideNotif);
            h.postDelayed(hideNotif, 5000);
        //}
    }

    private fun md5(s: String): String? {
        val MD5 = "MD5"
        try {
            // Create MD5 Hash
            val digest = MessageDigest
                .getInstance(MD5)
            digest.update(s.toByteArray())
            val messageDigest = digest.digest()

            // Create Hex String
            val hexString = StringBuilder()
            for (aMessageDigest in messageDigest) {
                var h = Integer.toHexString(0xFF and aMessageDigest.toInt())
                while (h.length < 2) h = "0$h"
                hexString.append(h)
            }
            return hexString.toString()
        } catch (e: NoSuchAlgorithmException) {
            e.printStackTrace()
        }
        return ""
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("ServiceCast")
    fun isOnline(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities =
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }
}