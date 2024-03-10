package com.example.gridviewapplication;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.ui.AppBarConfiguration;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideContext;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;
import com.example.gridviewapplication.databinding.ActivityHomeBinding;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.snackbar.Snackbar;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Home extends AppCompatActivity {

    /**
     * setUI
     */
    private static final String TAG = Home.class.getName();

    private AppBarConfiguration appBarConfiguration;
    private ActivityHomeBinding binding;

    private MyGlobals myGlobals;
    private boolean isWiFiEnabled;
    private POJO_Transaction curTransPojo;
    String d_rate;
    String p_rate;

    private String url_hosted;
    private String url_local_fuelcam;
    private String url_local_middleware;

    private int user_id;
    private int pump_id;
    private String shift;

    private ProgressBar progressBar; // search for car number
    private ArrayList<POJO_Transaction> transList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        myGlobals = new MyGlobals(Home.this);
        transList = new ArrayList<>();

        // URLs
        url_hosted = getResources().getString(R.string.url_hosted);
        url_local_fuelcam = getResources().getString(R.string.url_local_fuelcam);
        url_local_middleware = getResources().getString(R.string.url_local_middleware);

        shift = (String) PrefUtils.getFromPrefs(Home.this, PrefKeys.USER_SHIFT, "");
        user_id = (int) PrefUtils.getFromPrefs(Home.this, PrefKeys.USER_ID, 0);
        pump_id = (int) PrefUtils.getFromPrefs(Home.this, PrefKeys.PUMP_ID, 0);

        Log.e(TAG, "pump_id=" + pump_id);

        binding.fab.setOnClickListener(view -> {
            isWiFiEnabled = myGlobals.isWiFiEnabled();

            if (myGlobals.getTransListSize() >= 2) {
                Snackbar.make(binding.getRoot(), "Only 2 transactions allowed", Snackbar.LENGTH_LONG).show();
            } else {
                if (isWiFiEnabled) {
                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibe != null) {
                        vibe.vibrate(50);
                    }

                    Intent i = new Intent(Home.this, Scan.class);
                    i.putExtra("title", "Scan Car");

                    activityResultLauncher.launch(i);
                } else {
                    myGlobals.promptWiFiConnection(Home.this);
                }
            }
        });

        //setUI();

        // search for car number
        binding.homeContainer.buttonSearchCarNo.setOnClickListener(v -> {

            if (isWiFiEnabled) {
                String no_plate = binding.homeContainer.etNoPlate.getText().toString().toLowerCase().trim();


                if (!no_plate.equals("")) {
                    if (myGlobals.getTransListSize() >= 2) {
                        AppUtils.hideSoftKeyboard(this);
                        Snackbar.make(binding.getRoot(), "Only 2 transactions allowed", Snackbar.LENGTH_LONG).show();
                    } else {
                        // xml for slow network
                        progressBar.setVisibility(View.VISIBLE);
                        binding.homeContainer.buttonSearchCarNo.setEnabled(false);
                        binding.fab.setEnabled(false);
                        searchNoPlate(no_plate);
                    }
                }
            } else {
                Snackbar.make(binding.getRoot(), "Wifi Not connected to fuelcam", Snackbar.LENGTH_LONG).show();
            }

        });

        // allow to reenter new-transaction
        binding.homeContainer.bEnterTrans1.setOnClickListener(v -> {
            POJO_Transaction transaction = transList.get(0);
            Intent i = new Intent(getApplicationContext(), NewTransaction.class);
            i.putExtra("curTransPOJO", transaction);
            startActivity(i);
        });

        binding.homeContainer.bEnterTrans2.setOnClickListener(v -> {
            POJO_Transaction transaction = transList.get(1);
            Intent i = new Intent(getApplicationContext(), NewTransaction.class);
            i.putExtra("curTransPOJO", transaction);
            startActivity(i);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.add_car_qr_new) {

            String user_role = (String) PrefUtils.getFromPrefs(this, PrefKeys.USER_ROLE, "");

            if (user_role.equals("admin") || user_role.equals("manager")) {
                Intent i = new Intent(getApplicationContext(), AddQRCode.class);
                startActivity(i);
                Log.e(TAG, "assign qr");
            } else {
                Snackbar.make(binding.getRoot(), "Manager access only", Snackbar.LENGTH_SHORT).show();
            }


        }
        if (item.getItemId() == R.id.enter_receipt) {
            Log.e(TAG, "Receipt");
            showReceiptDialog();
        }
        if (item.getItemId() == R.id.logout) {
            PrefUtils.clearAllPrefs(Home.this);
            Intent i = new Intent(getApplicationContext(), Login.class);
            startActivity(i);
            finish();
        }

//        if (item.getItemId() == R.id.add_new_rate) {
//            showRatesDialog();
//        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();

        transList = myGlobals.getTransPojoList();
        Log.e("myGlobals", "transList: " + transList.size());
        for (POJO_Transaction transaction : transList) {
            Log.e("myGlobals", "transList: " + transaction.getCar_plate_no());
        }

        isWiFiEnabled = myGlobals.isWiFiEnabled();

        getLatestRate();

        setUI();

        // Wifi


        // mobile network
        if (myGlobals.networkInfo()) {
            Log.e(TAG, "isActiveNetworkMetered");
            myGlobals.promptWiFiConnection(Home.this);
        } else {

            // wifi network
            Log.e(TAG, "not Metered");
            Log.e(TAG, "useOnlyWifi");
            //myGlobals.useOnlyWifi();
        }

        // date mismatch, logout user
        //logoutYesterdaySignIn();
        // sign out at 6am
    }

    private void getLatestRate() {

        if (isWiFiEnabled) {
            String url = url_local_fuelcam + "/latest_rate";

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                    url, null, response -> {
                try {
                    if (response.getBoolean("success")) {
                        PrefUtils.saveToPrefs(Home.this, PrefKeys.PETROL_RATE, response.getString("petrol_rate"));
                        PrefUtils.saveToPrefs(Home.this, PrefKeys.DIESEL_RATE, response.getString("diesel_rate"));
                        setUI();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }, error -> {
                Log.e(TAG, error.getMessage());

            });
            MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);
        }
    }

    private void logoutYesterdaySignIn() {
        String date = myGlobals.getDateString();
        String pref_date = (String) PrefUtils.getFromPrefs(this, PrefKeys.DATE, "");
        if (!date.equals(pref_date)) {
            Intent i = new Intent(Home.this, Login.class);
            startActivity(i);
            finish();
        }
    }

    private void logout6amSignIn() throws ParseException {
        String date = myGlobals.getDateString();
        String pref_date = (String) PrefUtils.getFromPrefs(this, PrefKeys.DATE, "");
        if (!date.equals(pref_date)) {
            // date has changed

            Calendar curTime = Calendar.getInstance();


            String string2 = "06:00:00";
            Date time2 = new SimpleDateFormat("HH:mm:ss").parse(string2);
            Calendar calendar6am = Calendar.getInstance();
            calendar6am.setTime(time2);
            calendar6am.add(Calendar.DATE, 1);

            if (curTime.after(calendar6am.getTime())) {
                //checkes whether the current time is between 14:49:00 and 20:11:13.
                System.out.println(true);
            }


            Intent i = new Intent(Home.this, Login.class);
            startActivity(i);
            finish();
        }
    }

    private void setUI() {
        // set UI
        d_rate = (String) PrefUtils.getFromPrefs(this, PrefKeys.DIESEL_RATE, "0");
        p_rate = (String) PrefUtils.getFromPrefs(this, PrefKeys.PETROL_RATE, "0");
        String user_name = (String) PrefUtils.getFromPrefs(this, PrefKeys.USER_NAME, "user");

        binding.homeContainer.tvDieselRate.setText(d_rate);
        binding.homeContainer.tvPetrolRate.setText(p_rate);
        binding.homeContainer.tvUserName.setText(user_name);

        progressBar = binding.homeContainer.progressBarCarNoSearch;


        // TRANSACTION LIST STUFF
        // default
        binding.homeContainer.bEnterTrans1.setVisibility(View.INVISIBLE);
        binding.homeContainer.bEnterTrans2.setVisibility(View.INVISIBLE);

        for (int i = 0; i < transList.size(); i++) {

            POJO_Transaction transaction = transList.get(i);

            if (i == 0) {
                binding.homeContainer.bEnterTrans1.setText(transaction.getCar_plate_no());
                binding.homeContainer.bEnterTrans1.setVisibility(View.VISIBLE);
            }

            if (i == 1) {
                binding.homeContainer.bEnterTrans2.setText(transaction.getCar_plate_no());
                binding.homeContainer.bEnterTrans2.setVisibility(View.VISIBLE);
            }
        }
    }

    ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    assert data != null;
                    final Barcode barcode = data.getParcelableExtra("barcode");
                    assert barcode != null;
                    String car_qr = barcode.displayValue;
                    Log.e(TAG, "scan car result: " + car_qr);
                    checkCarQR(car_qr);
                }
            }
    );

    ActivityResultLauncher<Intent> scanPumpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    assert data != null;
                    final Barcode barcode = data.getParcelableExtra("barcode");
                    assert barcode != null;
                    String nozzle_qr = barcode.displayValue;

                    Log.e(TAG, "scan nozzle_qr result: " + nozzle_qr);

                    // check if duplicate is found
                    if (!myGlobals.isNozzleBusy(nozzle_qr)) {
                        checkNozzleQR(nozzle_qr, curTransPojo.getCar_plate_no());
                    } else {
                        Snackbar.make(binding.getRoot(), "Nozzle Busy", Snackbar.LENGTH_LONG).show();
                    }
                }
            }
    );


    private void showReceiptDialog() {

        final EditText input = new EditText(this);
        input.setWidth(60);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter receipt no")
                .setView(input)
                .setPositiveButton("Ok", (dialog1, which) -> {

                    String rNum = input.getText().toString();
                    if (!rNum.equals("")) {
                        isReceiptInLocalDB(rNum);
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        dialog.show();
    }

    private void checkCarQR(final String car_qr) {
        if (isWiFiEnabled) {
            //String url = "http://192.168.1.100/fuelcam/scan_car";
            String url = url_local_fuelcam + "/scan_car";


            // xml for slow network
            progressBar.setVisibility(View.VISIBLE);
            binding.homeContainer.buttonSearchCarNo.setEnabled(false);
            binding.fab.setEnabled(false);

            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("qr_code", car_qr);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    url, jsonObj,
                    response -> {
                        Log.e("login response", response.toString());
                        try {
                            if (response.getBoolean("success")) {
                                curTransPojo = new POJO_Transaction();
                                curTransPojo.setCust_id(response.getInt("cust_id"));
                                curTransPojo.setCust_name(response.getString("cust_name"));

                                curTransPojo.setCar_id(response.getInt("car_id"));
                                curTransPojo.setCar_plate_no(response.getString("car_no").toLowerCase());
                                curTransPojo.setFuel_type(response.getString("car_fuel"));

                                curTransPojo.setReceipt_number("0");

                                curTransPojo.setOperator_id(user_id);
                                curTransPojo.setShift(shift);
                                curTransPojo.setPump_id(pump_id);

                                if (response.getString("car_fuel").equals("petrol")) {
                                    curTransPojo.setFuel_rate(Double.parseDouble(p_rate));
                                } else if (response.getString("car_fuel").equals("diesel")) {
                                    curTransPojo.setFuel_rate(Double.parseDouble(d_rate));
                                }

                                // prevent same scan for active car
                                if (myGlobals.isPlateNoBusy(response.getString("car_no"))) {
                                    AppUtils.hideSoftKeyboard(Home.this);
                                    // xml for slow network
                                    progressBar.setVisibility(View.INVISIBLE);
                                    binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                                    binding.fab.setEnabled(true);
                                    Snackbar.make(binding.getRoot(), "Car already Started", Snackbar.LENGTH_SHORT).show();

                                } else {
                                    String dialog_string = response.getString("cust_name") + "\n" + response.getString("car_no").toUpperCase();
                                    final AlertDialog.Builder builder =
                                            new AlertDialog.Builder(Home.this).
                                                    setTitle("Scan Pump Now").
                                                    setMessage(dialog_string).
                                                    setCancelable(false).
                                                    setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).
                                                    setPositiveButton("OK", (dialog, which) -> {
                                                        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                        if (vibe != null) {
                                                            vibe.vibrate(50);
                                                        }
                                                        Intent scan = new Intent(getApplicationContext(), Scan.class);
                                                        scan.putExtra("title", "Scan Pump");
                                                        scanPumpLauncher.launch(scan);
                                                    }).setOnDismissListener(dialog -> {
                                                // xml for slow network
                                                progressBar.setVisibility(View.INVISIBLE);
                                                binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                                                binding.fab.setEnabled(true);
                                            });
                                    builder.create().show();
                                }


                            } else if (!response.getBoolean("success")) {

                                // xml for slow network
//                                progressBar.setVisibility(View.VISIBLE);
//                                binding.homeContainer.buttonSearchCarNo.setEnabled(false);
//                                binding.fab.setEnabled(false);
                                progressBar.setVisibility(View.INVISIBLE);
                                binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                                binding.fab.setEnabled(true);

                                String msg = response.getString("msg");
                                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> {

                // xml for slow network
                progressBar.setVisibility(View.INVISIBLE);
                binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                binding.fab.setEnabled(true);


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
                } else {
                    // possible timeout
                    AppUtils.hideSoftKeyboard(Home.this);
                    Snackbar.make(binding.getRoot(), "Network Timeout", Snackbar.LENGTH_SHORT).show();
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
            jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    0,
                    2));
            MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);

        } else {
            Snackbar.make(binding.getRoot(), "Please Enable Wifi", Snackbar.LENGTH_LONG).show();

            // xml for slow network
            progressBar.setVisibility(View.VISIBLE);
            binding.homeContainer.buttonSearchCarNo.setEnabled(false);
            binding.fab.setEnabled(false);
        }
    }

    private void searchNoPlate(String no_plate) {
        if (isWiFiEnabled) {
            String url = url_local_fuelcam + "/no_plate/" + no_plate;

            // check if in transactions already
            if (myGlobals.isPlateNoBusy(no_plate)) {

                AppUtils.hideSoftKeyboard(Home.this);

                // xml for slow network
                progressBar.setVisibility(View.INVISIBLE);
                binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                binding.fab.setEnabled(true);
                Snackbar.make(binding.getRoot(), "Car already Started", Snackbar.LENGTH_SHORT).show();

            } else {
                JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                        url, null, response -> {
                    Log.e(TAG, response.toString());

                    // xml for slow network
                    progressBar.setVisibility(View.INVISIBLE);
                    binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                    binding.fab.setEnabled(true);

                    try {

                        curTransPojo = new POJO_Transaction();
                        curTransPojo.setCust_id(response.getInt("cust_id"));
                        curTransPojo.setCust_name(response.getString("cust_disp_name"));

                        curTransPojo.setCar_id(response.getInt("car_id"));
                        curTransPojo.setCar_plate_no(no_plate);
                        curTransPojo.setFuel_type(response.getString("car_fuel"));

                        curTransPojo.setReceipt_number("0");

                        curTransPojo.setOperator_id(user_id);
                        curTransPojo.setShift(shift);
                        curTransPojo.setPump_id(pump_id);

                        if (response.getString("car_fuel").equals("petrol")) {
                            curTransPojo.setFuel_rate(Double.parseDouble(p_rate));
                        } else if (response.getString("car_fuel").equals("diesel")) {
                            curTransPojo.setFuel_rate(Double.parseDouble(d_rate));
                        }

                        String dialog_string = response.getString("cust_disp_name") + "\n" + no_plate.toUpperCase();

                        final AlertDialog.Builder builder =
                                new AlertDialog.Builder(Home.this).
                                        setTitle("Scan Pump Now").
                                        setMessage(dialog_string).
                                        setCancelable(false).
                                        setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()).
                                        setPositiveButton("OK", (dialog, which) -> {
                                            Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                            if (vibe != null) {
                                                vibe.vibrate(50);
                                            }
                                            binding.homeContainer.etNoPlate.setText(""); // clear input
                                            Intent scan = new Intent(getApplicationContext(), Scan.class);
                                            scan.putExtra("title", "Scan Pump");
                                            scanPumpLauncher.launch(scan);
                                        });
                        builder.create().show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }, error -> {

                    Log.e("Volley Error", error.toString());

                    // xml for slow network
                    progressBar.setVisibility(View.INVISIBLE);
                    binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                    binding.fab.setEnabled(true);


                    NetworkResponse networkResponse = error.networkResponse;
                    if (networkResponse != null && networkResponse.statusCode == 409) {
                        // HTTP Status Code: 409 Client error
                        try {

                            AppUtils.hideSoftKeyboard(this);

                            String jsonString = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                            JSONObject obj = new JSONObject(jsonString);
                            String message = obj.getString("message");


                            // Log.e("NetworkResponse", message);
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        } catch (UnsupportedEncodingException | JSONException e) {
                            e.printStackTrace();
                        }
                    } else {
                        // possible timeout
                        AppUtils.hideSoftKeyboard(Home.this);
                        Snackbar.make(binding.getRoot(), "Network Timeout", Snackbar.LENGTH_SHORT).show();
                    }
                });

                jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                        10000,
                        0,
                        2));
                MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);
            }


        } else {
            Snackbar.make(binding.getRoot(), "Please Enable Wifi", Snackbar.LENGTH_LONG).show();

            // xml for slow network
            progressBar.setVisibility(View.INVISIBLE);
            binding.homeContainer.buttonSearchCarNo.setEnabled(true);
            binding.fab.setEnabled(true);
        }
    }

    private void checkNozzleQR(final String nozzle_qr, String no_plate) {
        if (isWiFiEnabled) {
            //String url = "http://192.168.1.100/fuelcam/scan_pump";
            String url = url_local_fuelcam + "/scan_pump";

            // xml for slow network
            progressBar.setVisibility(View.VISIBLE);
            binding.homeContainer.buttonSearchCarNo.setEnabled(false);
            binding.fab.setEnabled(false);

            JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("photo_type", "start");
                jsonObj.put("nozzle_qr", nozzle_qr);
                jsonObj.put("no_plate", no_plate);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    url, jsonObj,
                    response -> {
                        Log.e("login response", response.toString());

                        // xml for slow network
                        progressBar.setVisibility(View.INVISIBLE);
                        binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                        binding.fab.setEnabled(true);

                        try {
                            if (response.getBoolean("success")) {

                                //get photo url as response and display here
                                String photo_url = response.getString("photo_url");
                                //String url_photo = "http://192.168.1.100/middleware/" + photo_url;
                                String url_photo = url_local_middleware + "/" + photo_url;
//                                String url_photo = response.getString("photo_url");
//                                String url_photo = "https://picsum.photos/id/237/200/300";
                                Log.e(TAG, url_photo);


                                curTransPojo.setNozzle_qr(nozzle_qr);

                                // IMAGE LOADING
                                /*
                                ImageView image = new ImageView(Home.this);
                                Picasso picasso = Picasso.get();
                                picasso.setLoggingEnabled(true);
                                picasso.load(url_photo).into(image, new Callback() {
                                    @Override
                                    public void onSuccess() {
                                        Log.e(TAG, "picasso success");
                                    }

                                    @Override
                                    public void onError(Exception e) {
                                        Log.e(TAG, "picasso ERROR " + e.getMessage());
                                        picasso.load(url_photo).into(image);
                                    }
                                });
                                */


                                // GLIDE
                                ImageView image = new ImageView(Home.this);
                                int width = 320;
                                int height = 240;
                                LinearLayout.LayoutParams parms = new LinearLayout.LayoutParams(width, height);
                                image.setLayoutParams(parms);
                                Glide.with(Home.this)
                                        .setDefaultRequestOptions(new RequestOptions()
                                                .timeout(60000))
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

                                                // Retry
                                                Glide.with(Home.this).load(url_photo);

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
                                        new AlertDialog.Builder(Home.this).
                                                setMessage("Zero Photo").
                                                setCancelable(false).
                                                setPositiveButton("Start", (dialog, which) -> {
                                                    Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                    if (vibe != null) {
                                                        vibe.vibrate(50);
                                                    }

                                                    // insert for multi transaction
                                                    myGlobals.insertTransPojoList(curTransPojo);

                                                    Intent i = new Intent(getApplicationContext(), NewTransaction.class);
                                                    i.putExtra("curTransPOJO", curTransPojo);
                                                    curTransPojo = null;
                                                    startActivity(i);
                                                }).
                                                setNegativeButton("Cancel", (dialog, which) -> {
                                                    cancelTransaction(nozzle_qr, no_plate);
                                                }).
                                                setView(image);
                                builder.create().show();

                            } else if (!response.getBoolean("success")) {
                                String msg = response.getString("msg");
                                Snackbar.make(binding.getRoot(), msg, Snackbar.LENGTH_SHORT).show();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }


                    }, error -> {

                // xml for slow network
                progressBar.setVisibility(View.INVISIBLE);
                binding.homeContainer.buttonSearchCarNo.setEnabled(true);
                binding.fab.setEnabled(true);

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
                } else {
                    // possible timeout
                    AppUtils.hideSoftKeyboard(Home.this);
                    Snackbar.make(binding.getRoot(), "Network Timeout", Snackbar.LENGTH_SHORT).show();
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
            jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                    10000,
                    0,
                    2));
            MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);
        } else {
            Snackbar.make(binding.getRoot(), "Please Enable Wifi", Snackbar.LENGTH_LONG).show();
            // xml for slow network
            progressBar.setVisibility(View.INVISIBLE);
            binding.homeContainer.buttonSearchCarNo.setEnabled(true);
            binding.fab.setEnabled(true);
        }
    }

    private void isReceiptInLocalDB(String rNum) {

        //String url = "http://192.168.1.100/fuelcam/receipts/" + rNum;
        String url = url_local_fuelcam + "/receipts/" + rNum;

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.GET,
                url, null, response -> {
            try {
                if (response.getBoolean("success")) {
                    Log.e(TAG, "can use receipt");
                    isReceiptValidHosted(rNum);
                } else if (!response.getBoolean("success")) {
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


                    // Log.e("NetworkResponse", message);
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }
            }
        });
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);
    }

    private void isReceiptValidHosted(String rNum) {
        if (isWiFiEnabled) {

            String url = url_hosted + "/exe/check_receipt.php";
            //int pump_id = (int) PrefUtils.getFromPrefs(Home.this, PrefKeys.PUMP_ID, 0);


            Log.e("isReceiptValidHosted", url);

            final JSONObject jsonObj = new JSONObject();
            try {
                jsonObj.put("rnum", rNum);
                jsonObj.put("pump_id", pump_id);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Log.e(TAG, jsonObj.toString());

            JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                    url, jsonObj,
                    response -> {
                        Log.e("login response", response.toString());

                        try {
                            if (response.getBoolean("success")) {

                                curTransPojo = new POJO_Transaction();
                                curTransPojo.setCust_id(response.getInt("cust_id"));
                                curTransPojo.setCust_name(response.getString("cust_name"));

                                curTransPojo.setCar_id(response.getInt("car_id"));
                                curTransPojo.setCar_plate_no(response.getString("car_no"));
                                //curTransPojo.setFuel_type(response.getString("car_fuel"));

                                curTransPojo.setReceipt_number(rNum);

                                curTransPojo.setOperator_id(user_id);
                                curTransPojo.setShift(shift);
                                curTransPojo.setPump_id(pump_id);

//                                if (response.getString("car_fuel").equals("petrol")) {
//                                    curTransPojo.setFuel_rate(Double.parseDouble(p_rate));
//                                } else if (response.getString("car_fuel").equals("diesel")) {
//                                    curTransPojo.setFuel_rate(Double.parseDouble(d_rate));
//                                }

                                final AlertDialog.Builder builder =
                                        new AlertDialog.Builder(Home.this).
                                                setMessage(response.getString("cust_name")).
                                                setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        Vibrator vibe = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                                                        if (vibe != null) {
                                                            vibe.vibrate(50);
                                                        }


//                                                        Intent i = new Intent(getApplicationContext(), AddNewCar.class);
//                                                        i.putExtra("isReceipt", true);
//                                                        i.putExtra("cust_id", cust_id);
//                                                        i.putExtra("cust_name", cust_name);
//                                                        i.putExtra("curTransPOJO", curTransPojo);
//                                                        startActivityForResult(i, ADD_CAR);
                                                    }
                                                }).
                                                setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        dialog.dismiss();
                                                    }
                                                }).setCancelable(false);
                                builder.create().show();

                            } else {
                                Log.e("result", "fail");
                                Snackbar.make(binding.getRoot(), response.getString("msg"), Snackbar.LENGTH_SHORT).show();
                            }

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }, error -> {
                Log.e("Volley Error", "Error: " + error.getMessage());
                Snackbar.make(binding.getRoot(), "Network Error", Snackbar.LENGTH_LONG).show();
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

        } else {
            Snackbar.make(binding.getRoot(), "Please Enable Wifi", Snackbar.LENGTH_LONG).show();
        }
    }


    private void cancelTransaction(String nozzle_qr, String no_plate) {

        String url = url_local_fuelcam + "/scan_pump";

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("photo_type", "cancel");
            jsonObj.put("nozzle_qr", nozzle_qr);
            jsonObj.put("no_plate", no_plate);

        } catch (JSONException e) {
            e.printStackTrace();
        }


        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {
                    Log.e("login response", response.toString());

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


    private void showRatesDialog() {
        final EditText input = new EditText(this);

        input.setWidth(60);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Enter Password")
                .setView(input)
                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int val = Integer.parseInt(String.valueOf(input.getText()));
                        if (val == 123456) {

                            Intent i = new Intent(Home.this, SetRates.class);
                            i.putExtra("user_id", user_id);
                            i.putExtra("pump_id", pump_id);
                            i.putExtra("update_rate", true);
                            startActivity(i);
                        } else {
                            Snackbar.make(binding.getRoot(), "Access Denied", Snackbar.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .create();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);
        dialog.show();
    }
}