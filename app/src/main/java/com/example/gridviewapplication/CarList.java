package com.example.gridviewapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.gridviewapplication.databinding.ActivityCarListBinding;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CarList extends AppCompatActivity implements AdapterCustomerList.gridListener {

    private ActivityCarListBinding binding;

    private static final String TAG = CarList.class.getSimpleName();

    //private MyGlobals myGlobals;

    private AdapterCustomerList mAdapter;

    private final ArrayList<POJO_id_string> carList = new ArrayList<>();
    private int cust_id = -1;
    private int car_id = -1;
    private String cust_name = "";
    private int pump_id;
    private boolean inProcess = false;

    private ProgressBar progressBar;
    private Button b_retry;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityCarListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        pump_id = (int) PrefUtils.getFromPrefs(CarList.this, PrefKeys.PUMP_ID, 0);

        //myGlobals = new MyGlobals(CarList.this);

        getBundle(savedInstanceState);
        init();

        b_retry.setOnClickListener(v -> {
            b_retry.setVisibility(View.INVISIBLE);
            getCarList();
        });
    }

    private void getBundle(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            Bundle extras = getIntent().getExtras();
            if (extras == null) {
                cust_id = -1;
            } else {
                cust_id = extras.getInt("cust_id");
                cust_name = extras.getString("cust_name");
            }
        } else {
            cust_id = (int) savedInstanceState.getSerializable("cust_id");
            cust_name = (String) savedInstanceState.getSerializable("cust_name");
        }
        Log.e(TAG, " " + cust_id);
    }

    private void init() {


        AdapterCustomerList.gridListener mListener = this;

        recyclerView = binding.contentCarList.rvCarList;
        mAdapter = new AdapterCustomerList(carList, this, mListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(mAdapter);
        b_retry = binding.contentCarList.bCarListRetry;
        progressBar = binding.contentCarList.progressBarCarList;
    }

    private void getCarList() {

        recyclerView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);

        String url = getResources().getString(R.string.url_hosted) + "/api/cars/" + pump_id + "/" + cust_id;

        Log.e(TAG, "url" + url);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);

                    carList.clear();

                    POJO_id_string pojo1 = new POJO_id_string();
                    pojo1.setCust_id(-99);
                    pojo1.setDisplay_name("ADD NEW CAR");
                    carList.add(pojo1);
                    mAdapter.notifyDataSetChanged();

                    Log.e("resp", response.toString());


                    try {
                        for (int i = 0; i < response.length(); i++) {
                            // Get current json object
                            JSONObject car = response.getJSONObject(i);
                            String car_no_plate = car.getString("car_no_plate");
                            int car_id = car.getInt("car_id");

                            POJO_id_string pojo = new POJO_id_string();
                            pojo.setCust_id(car_id);
                            pojo.setDisplay_name(car_no_plate);
                            carList.add(pojo);

                        }
                        //mAdapter.updateReceiptsList(customerList);
                        mAdapter.notifyDataSetChanged();
                        Log.e("size", "" + carList.size());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {

                    progressBar.setVisibility(View.INVISIBLE);
                    b_retry.setVisibility(View.VISIBLE);

                    // Do something when error occurred
                    Snackbar.make(
                            binding.getRoot(),
                            "Error fetching JSON",
                            Snackbar.LENGTH_LONG
                    ).show();
                }
        );
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                0,
                2));
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonArrayRequest);
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCarList();
    }

    @Override
    public void listClick(int position) {
        car_id = carList.get(position).getCust_id();
        Log.e(TAG, "car_id: " + car_id);

        if (car_id == -99) {
            Intent i = new Intent(getApplicationContext(), AddNewCar.class);
            i.putExtra("cust_id", cust_id);
            i.putExtra("cust_name", cust_name);
            startActivity(i);
        } else {
            Intent i = new Intent(CarList.this, Scan.class);
            i.putExtra("title", "Scan QR Code");
            scanCarLauncher.launch(i);
        }

    }

    ActivityResultLauncher<Intent> scanCarLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();
                    assert data != null;
                    final Barcode barcode = data.getParcelableExtra("barcode");
                    assert barcode != null;
                    String unassigned_qr = barcode.displayValue;
                    Log.e(TAG, "unassigned_qr result: " + unassigned_qr);

                    if (!inProcess) {
                        postUnassignedCode(unassigned_qr);
                    }
                }
            }
    );

    private void showSuccessDialog(String msg) {

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(msg)
                .setPositiveButton("OK", (dialog1, which) -> finish())
                .create();
        dialog.show();
    }

    private void postUnassignedCode(String unassigned_qr) {

        inProcess = true;

        //progressBar.setVisibility(View.VISIBLE);
        //String url = getResources().getString(R.string.url_hosted);
        //url = url + "/exe/post_qr_code.php";
        //String url = "http://fuelmaster.greenboxinnovations.in/exe/post_qr_code.php";
        String url = getResources().getString(R.string.url_hosted) + "/exe/post_qr_code.php";


        //String date = myGlobals.getDateString();

        progressBar.setVisibility(View.VISIBLE);

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("cust_id", cust_id);
            jsonObj.put("car_id", car_id);
            jsonObj.put("qr_code", unassigned_qr);
            jsonObj.put("pump_id", pump_id);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("post qr details", jsonObj.toString());


        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {
                    Log.e("post code response", response.toString());

                    progressBar.setVisibility(View.INVISIBLE);

                    try {
                        if (response.getBoolean("success")) {
                            Log.e("result", "success");

                            showSuccessDialog(response.getString("msg"));

                        } else {
                            Snackbar.make(binding.getRoot(), response.getString("msg"), Snackbar.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    inProcess = false;
                }, error -> {
            Log.e("Volley Error", "Error: " + error.getMessage());
            Snackbar.make(binding.getRoot(), "Network Error", Snackbar.LENGTH_LONG).show();
            progressBar.setVisibility(View.INVISIBLE);
            inProcess = false;
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
}