package com.example.gridviewapplication;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;


import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.gridviewapplication.databinding.ActivitySetRatesBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class SetRates extends AppCompatActivity {

    /**
     *
     */

    private AppBarConfiguration appBarConfiguration;
    private ActivitySetRatesBinding binding;

    private int user_id, pump_id;
    private static final String TAG = SetRates.class.getSimpleName();
    private boolean update_rate;

    private String url_local_fuelcam;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySetRatesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // URLs
        url_local_fuelcam = getResources().getString(R.string.url_local_fuelcam);

        getBundleFromLogin(savedInstanceState);

        binding.bSetrates.setOnClickListener(v -> sanitizeRates());
    }

    private void getBundleFromLogin(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras != null) {
                user_id = extras.getInt("user_id");
                pump_id = extras.getInt("pump_id");
                update_rate = extras.getBoolean("update_rate");
            }
        } else {
            user_id = (int) savedInstanceState.getSerializable("user_id");
            pump_id = (int) savedInstanceState.getSerializable("pump_id");
            update_rate = (boolean) savedInstanceState.getSerializable("update_rate");
        }

        Log.e(TAG, "user_id: " + user_id + " pump_id: " + pump_id);
    }

    private void sanitizeRates() {
        // Sanitize input
        if (binding.etDiesel.getText().toString().equals("") || binding.etPetrol.getText().toString().equals("")) {
            Snackbar.make(binding.getRoot(), "Empty Rates!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // parse to double
        double p_rate = 0;
        double d_rate = 0;
        boolean check = false;
        try {
            p_rate = Double.parseDouble(binding.etPetrol.getText().toString());
            d_rate = Double.parseDouble(binding.etDiesel.getText().toString());
            check = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        // check if number is too large
        if (check) {
            if ((p_rate < 1 || p_rate > 140) || (d_rate < 1 || d_rate > 140)) {
                Snackbar.make(binding.getRoot(), "Invalid Rate", Snackbar.LENGTH_SHORT).show();
                return;
            }

            // rate is valid
            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("petrol", p_rate);
                jsonObj.put("diesel", d_rate);
                jsonObj.put("pump_id", pump_id);
                jsonObj.put("user_id", user_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e(TAG, jsonObj.toString());
            postRates(jsonObj);
        }
    }

    private void postRates(JSONObject jsonObj) {
        //String url = "http://192.168.1.100/fuelcam/rates";
        String url = url_local_fuelcam + "/rates";

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {
                    Log.e("set rates response", response.toString());

                    try {
                        if (response.getBoolean("success")) {
                            Log.e(TAG, "bool success true");

                            PrefUtils.saveToPrefs(SetRates.this, PrefKeys.USER_NAME, response.getString("user_name"));
                            PrefUtils.saveToPrefs(SetRates.this, PrefKeys.USER_ID, response.getInt("user_id"));
                            PrefUtils.saveToPrefs(SetRates.this, PrefKeys.PUMP_ID, response.getInt("pump_id"));

                            PrefUtils.saveToPrefs(SetRates.this, PrefKeys.PETROL_RATE, response.getString("petrol_rate"));
                            PrefUtils.saveToPrefs(SetRates.this, PrefKeys.DIESEL_RATE, response.getString("diesel_rate"));
                            PrefUtils.saveToPrefs(SetRates.this, PrefKeys.DATE, response.getString("date"));

                            // if rate is updated, user is already logged in
                            // use shift from shared prefs
                            if (update_rate) {
                                // home doesnt finish itself before coming here
                                // user can press back button
                                finish();
                            } else {
                                // login has finished before coming here
                                chooseShiftDialog();
                            }


                        } else {
                            Log.e("result", "fail");
                            String msg = response.getString("msg");
                            Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 409) {
                // HTTP Status Code: 409 Client error
                try {
                    // 409 is not possible from android, but still included to catch random error
                    String jsonString = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                    JSONObject obj = new JSONObject(jsonString);
                    String message = obj.getString("message");

                    // hide the keyboard
                    AppUtils.hideSoftKeyboard(SetRates.this);

                    // Log.e("NetworkResponse", message);
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }
            }
        }) {

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("charset", "utf-8");
                return headers;
            }
        };
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);
    }


    private void chooseShiftDialog() {
        Log.e(TAG, "show shift dialog");

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(SetRates.this);
        alertDialogBuilder.setTitle("Select Shift");
        alertDialogBuilder.setPositiveButton("Shift B", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                goToHomeActivity("b");
            }
        });
        alertDialogBuilder.setNegativeButton("Shift A", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                goToHomeActivity("a");
            }
        });
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

    private void goToHomeActivity(String shift) {
        PrefUtils.saveToPrefs(SetRates.this, PrefKeys.USER_SHIFT, shift);
        Intent i = new Intent(SetRates.this, Home.class);
        startActivity(i);
        finish();
    }
}