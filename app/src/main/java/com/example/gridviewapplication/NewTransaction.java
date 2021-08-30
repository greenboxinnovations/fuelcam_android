package com.example.gridviewapplication;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.gridviewapplication.databinding.ActivityNewTransactionBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NewTransaction extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityNewTransactionBinding binding;

    private POJO_Transaction curTransPOJO;


    private MyGlobals myGlobals;
    private boolean isWiFiEnabled;

    boolean keyLock = false;
    private boolean click = false;

    private static final String TAG = NewTransaction.class.getName();

    private String url_hosted;
    private String url_local_fuelcam;
    private String url_local_middleware;

    private ProgressBar progressBar;
    private Button bp0, bp1, bp2;
    private int reprint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityNewTransactionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        myGlobals = new MyGlobals(NewTransaction.this);

        // get from intent
        curTransPOJO = (POJO_Transaction) getIntent().getSerializableExtra("curTransPOJO");


        setPetrolDieselUI();

        setTextWatchers();

        // URLs
        url_hosted = getResources().getString(R.string.url_hosted);
        url_local_fuelcam = getResources().getString(R.string.url_local_fuelcam);
        url_local_middleware = getResources().getString(R.string.url_local_middleware);

        progressBar = binding.progressBarNewTransaction;

        bp0 = binding.bEndPrint0;
        bp1 = binding.bEndPrint1;
        bp2 = binding.bEndPrint2;


        bp0.setOnClickListener((View.OnClickListener) v -> {
            reprint = 0;
            commonButtonFunc();
        });

        bp1.setOnClickListener((View.OnClickListener) v -> {
            reprint = 1;
            commonButtonFunc();
        });

        bp2.setOnClickListener((View.OnClickListener) v -> {
            reprint = 2;
            commonButtonFunc();
        });
    }


    private void commonButtonFunc() {
        isWiFiEnabled = myGlobals.isWiFiEnabled();
        if (isWiFiEnabled) {

            if (!click) {
                click = true;
                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                if (vibe != null) {
                    vibe.vibrate(50);
                }
                Log.e("new transaction", "new_transactions");
                validateFuelValues();
            }

        } else {
            myGlobals.promptWiFiConnection(NewTransaction.this);
        }
    }

    private void setPetrolDieselUI() {
        if (curTransPOJO != null) {
            if (curTransPOJO.getFuel_type().equals("petrol")) {
                binding.rlBack.setBackgroundColor(Color.parseColor("#0D9F56"));
                binding.tvFuelType.setText("Petrol");
            } else {
                binding.rlBack.setBackgroundColor(Color.parseColor("#00AAE8"));
                binding.tvFuelType.setText("Diesel");
            }
            binding.tvFuelRate.setText(String.valueOf(curTransPOJO.getFuel_rate()));

            binding.tvCustName.setText(curTransPOJO.getCust_name());
            binding.tvCarNoPlate.setText(curTransPOJO.getCar_plate_no().toUpperCase());
        }
    }

    private void setTextWatchers() {
        binding.etRs.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(6, 2)});
        //binding.etLit.setFilters(new InputFilter[]{new DecimalDigitsInputFilter(7, 2)});

        // text change listener
        binding.etRs.addTextChangedListener(new GenericTextWatcher(binding.etRs));
        //binding.etLit.addTextChangedListener(new GenericTextWatcher(et_fuel_litres));

    }

    private void validateFuelValues() {

        String fuel_rs = binding.etRs.getText().toString();
        String fuel_lit = binding.etLit.getText().toString();

        if ((fuel_lit.equals("")) || (fuel_rs.equals(""))) {
            Snackbar.make(binding.getRoot(), "Empty Values Not Allowed", Snackbar.LENGTH_SHORT).show();
            click = false;
            return;
        }

        try {
            double maxValueAmount = Double.parseDouble(fuel_rs);
            double maxValueLit = Double.parseDouble(fuel_lit);
            if ((maxValueAmount > 99999.99) || (maxValueLit > 999.99)) {
                Snackbar.make(binding.getRoot(), "Amount has to be less than 99999.99 or lit less than 999.99", Snackbar.LENGTH_SHORT).show();
                click = false;
            } else {
                curTransPOJO.setAmount(maxValueAmount);
                curTransPOJO.setLitres(maxValueLit);
                postTransaction(fuel_rs, fuel_lit);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Snackbar.make(binding.getRoot(), "Invalid Amount", Snackbar.LENGTH_SHORT).show();
            click = false;
        }
    }

    private void postTransaction(String fuel_rs, String fuel_lit) {

        // xml for slow transactions
        progressBar.setVisibility(View.VISIBLE);
        bp0.setEnabled(false);
        bp1.setEnabled(false);
        bp2.setEnabled(false);


        String url = url_local_fuelcam + "/transactions";

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("car_fuel", curTransPOJO.getFuel_type());
            jsonObj.put("fuel_rate", curTransPOJO.getFuel_rate());
            jsonObj.put("car_id", curTransPOJO.getCar_id());
            jsonObj.put("amount", curTransPOJO.getAmount());
            jsonObj.put("liters", curTransPOJO.getLitres());
            jsonObj.put("cust_id", curTransPOJO.getCust_id());
            jsonObj.put("user_id", curTransPOJO.getOperator_id());
            jsonObj.put("receipt_no", curTransPOJO.getReceipt_number());
            jsonObj.put("nozzle_qr", curTransPOJO.getNozzle_qr());
            jsonObj.put("shift", curTransPOJO.getShift());
            jsonObj.put("pump_id", curTransPOJO.getPump_id());
            jsonObj.put("print_num", reprint);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e("t", jsonObj.toString());

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {

                    // xml for slow transactions
                    progressBar.setVisibility(View.INVISIBLE);
                    bp0.setEnabled(true);
                    bp1.setEnabled(true);
                    bp2.setEnabled(true);

                    Log.e("newTransaction response", response.toString());
                    try {
                        if (response.getBoolean("success")) {
                            clickStopPhoto();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Snackbar.make(binding.getRoot(), "Unknown Error", Snackbar.LENGTH_SHORT).show();
                        click = false;
                    }
                }, error -> {

            // xml for slow transactions
            progressBar.setVisibility(View.INVISIBLE);
            bp0.setEnabled(true);
            bp1.setEnabled(true);
            bp2.setEnabled(true);

            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 409) {
                // HTTP Status Code: 409 Client error
                try {
                    String jsonString = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                    JSONObject obj = new JSONObject(jsonString);
                    String message = obj.getString("message");

                    if (message.equals("Duplicate Transaction")) {
                        final AlertDialog.Builder builder =
                                new AlertDialog.Builder(NewTransaction.this).
                                        setMessage("This Is A Duplicate Transaction").
                                        setCancelable(false).
                                        setPositiveButton("Finish", (dialog, which) -> {
                                            Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            if (vibe != null) {
                                                vibe.vibrate(50);
                                            }
                                            myGlobals.removeTransPojoListByCarPlate(curTransPOJO.getCar_plate_no());
                                            finish();
                                        });
                        builder.create().show();
                    } else {
                        Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                    }
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }
            }
            click = false;
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


    private void clickStopPhoto() {
//        String url = "http://192.168.1.100/fuelcam/scan_pump";
        String url = url_local_fuelcam + "/scan_pump";


        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("photo_type", "stop");
            jsonObj.put("nozzle_qr", curTransPOJO.getNozzle_qr());
            jsonObj.put("no_plate", curTransPOJO.getCar_plate_no());

        } catch (JSONException e) {
            e.printStackTrace();
        }

        Log.e(TAG, jsonObj.toString());

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {
                    Log.e("new transaction resp", response.toString());
                    try {
                        if (response.getBoolean("success")) {

                            //get photo url as response and display here
                            String photo_url = response.getString("photo_url");
                            //String url_photo = "http://192.168.1.100/middleware/" + photo_url;
                            String url_photo = url_local_middleware + "/" + photo_url;

                            Log.e(TAG, url_photo);

                            // PICASSO
                            /*
                            ImageView image = new ImageView(NewTransaction.this);
                            //Picasso.get().load(url_photo).into(image);
                            Picasso picasso = Picasso.get();
                            picasso.setLoggingEnabled(true);
                            picasso.load(url_photo).into(image);
                             */

                            // GLIDE
                            ImageView image = new ImageView(NewTransaction.this);
                            int width = 320;
                            int height = 240;
                            LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
                            image.setLayoutParams(parms);
                            Glide.with(NewTransaction.this)
                                    .load(url_photo)
                                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                                    .skipMemoryCache(true)
                                    .listener(new RequestListener() {
                                        @Override
                                        public boolean onLoadFailed(GlideException e, Object model, Target target, boolean isFirstResource) {
                                            // Log the GlideException here (locally or with a remote logging framework):
                                            Log.e(TAG, "Load failed", e);

                                            // You can also log the individual causes:
                                            assert e != null;
                                            for (Throwable t : e.getRootCauses()) {
                                                Log.e(TAG, "Caused by", t);
                                            }
                                            // Or, to log all root causes locally, you can use the built in helper method:
                                            e.logRootCauses(TAG);
                                            return false; // Allow calling onLoadFailed on the Target.
                                        }

                                        @Override
                                        public boolean onResourceReady(Object resource, Object model, Target target, DataSource dataSource, boolean isFirstResource) {
                                            // Log successes here or use DataSource to keep track of cache hits and misses.
                                            return false; // Allow calling onResourceReady on the Target.
                                        }
                                    })
                                    .into(image);


                            final AlertDialog.Builder builder =
                                    new AlertDialog.Builder(NewTransaction.this).
                                            setMessage("Final Photo").
                                            setPositiveButton("OK", (dialog, which) -> {
                                                Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                if (vibe != null) {
                                                    vibe.vibrate(50);
                                                }
                                                myGlobals.removeTransPojoListByCarPlate(curTransPOJO.getCar_plate_no());
                                                finish();
                                            }).setCancelable(false).
                                            setView(image);
                            builder.create().show();


                        } else {

                            Snackbar.make(binding.getRoot(), response.getString("message"), Snackbar.LENGTH_SHORT).show();
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


    private class GenericTextWatcher implements TextWatcher {

        private GenericTextWatcher(View view) {
        }

        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        }

        public void afterTextChanged(Editable editable) {

            String text = editable.toString();

            if (!keyLock) {
                keyLock = true;
                if (!text.equals("")) {
                    try {
                        double rsVal = Double.parseDouble(text);
                        double pre_litVal;

                        pre_litVal = rsVal / curTransPOJO.getFuel_rate();
                        double litVal = AppUtils.round(pre_litVal, 2);
                        binding.etLit.setText(String.valueOf(litVal));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else {
                    binding.etLit.setText("");
                }
                keyLock = false;
            }
        }
    }
}