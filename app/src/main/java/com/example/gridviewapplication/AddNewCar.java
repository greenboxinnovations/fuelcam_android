package com.example.gridviewapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.snackbar.Snackbar;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.gridviewapplication.databinding.ActivityAddNewCarBinding;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class AddNewCar extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityAddNewCarBinding binding;

    private int cust_id = 0;

    private TextView tv_cust_name;
    private Button save;
    private EditText et_vehicle_no;
    private ConstraintLayout constraintLayout;
    // private ProgressBar progressBar;
    boolean canClick = true;

    private RadioGroup radioGroup;
    private Boolean isReceipt = false, isPetrol = false;

    private int pump_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddNewCarBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        pump_id = (int) PrefUtils.getFromPrefs(AddNewCar.this, PrefKeys.PUMP_ID, 0);

        setSupportActionBar(binding.toolbar);

        init();
        getBundle();

        save.setOnClickListener(v -> {
            if (canClick) {
                String car_no;
                String fuel_type = "";

                car_no = String.valueOf(et_vehicle_no.getText());

                if (radioGroup.getCheckedRadioButtonId() == R.id.petrol) {
                    fuel_type = "petrol";
                    isPetrol = true;

                } else if (radioGroup.getCheckedRadioButtonId() == R.id.petrol) {
                    fuel_type = "diesel";
                }


                if (!car_no.equals("")) {
                    String clean_car_no = car_no.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
                    Log.e("values", "" + clean_car_no + fuel_type);
                    postCustomerCar(clean_car_no, fuel_type);
                } else {
                    Snackbar.make(binding.getRoot(), "Please Enter Valid Car Number", Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void init() {
        /*
        et_vehicle_no = findViewById(R.id.et_vehicle_no);
        tv_cust_name = findViewById(R.id.tv_cust_name);
        //progressBar = findViewById(R.id.progressBar_cc);
        radioGroup = findViewById(R.id.radio_fuel);
        radioGroup.check(R.id.petrol);
        save = findViewById(R.id.btn_save_new_car);
        constraintLayout = findViewById(R.id.layout_add_car);
         */


        et_vehicle_no = binding.contentAddNewCar.etVehicleNo;
        tv_cust_name = binding.contentAddNewCar.tvCustName;
        //progressBar = findViewById(R.id.progressBar_cc);
        radioGroup = binding.contentAddNewCar.radioFuel;
        radioGroup.check(R.id.petrol);
        save = binding.contentAddNewCar.btnSaveNewCar;
    }

    private void getBundle() {
        //noinspection -ConstantConditions
        if (getIntent().hasExtra("cust_id")) {
            cust_id = Objects.requireNonNull(getIntent().getExtras()).getInt("cust_id", -1);
        }


        if (getIntent().hasExtra("isReceipt")) {
            isReceipt = true;
        }

        if (getIntent().hasExtra("cust_name")) {
            tv_cust_name.setText((Objects.requireNonNull(getIntent().getExtras())).getString("cust_name"));
            tv_cust_name.setVisibility(View.VISIBLE);
        }
    }

    private void postCustomerCar(final String car_no, String fuel_type) {

        //progressBar.setVisibility(View.VISIBLE);
        canClick = false;
        //String url = "http://fuelmaster.greenboxinnovations.in/api/cars";
        String url = getResources().getString(R.string.url_hosted) + "/api/cars";

        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("car_no_plate", car_no);
            jsonObj.put("car_fuel_type", fuel_type);
            jsonObj.put("cust_id", cust_id);
            jsonObj.put("car_brand", "unknown");
            jsonObj.put("car_sub_brand", "unknown");
            jsonObj.put("car_qr_code", "");
            jsonObj.put("pump_id", pump_id);

        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e("credentials", jsonObj.toString());

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {
                    Log.e("Server add car response", response.toString());

                    // xml for slow networks
                    //progressBar.setVisibility(View.INVISIBLE);
                    canClick = true;

                    try {
                        if (response.getBoolean("success")) {
                            Log.e("result", "success");
                            int car_id = response.getInt("car_id");
                            isPetrol = response.getBoolean("isPetrol");
                            if (isReceipt) {
                                Intent intent = new Intent();
                                intent.putExtra("car_id", car_id);
                                intent.putExtra("car_no", car_no);
                                intent.putExtra("isPetrol", isPetrol);
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                finish();
                            }
                            //Snackbar.make(constraintLayout, "Customer Added Successfully", Snackbar.LENGTH_LONG).show();

                        } else {
                            int car_id = response.getInt("car_id");
                            isPetrol = response.getBoolean("isPetrol");
                            if (isReceipt) {
                                Intent intent = new Intent();
                                intent.putExtra("car_id", car_id);
                                intent.putExtra("car_no", car_no);
                                intent.putExtra("isPetrol", isPetrol);
                                setResult(RESULT_OK, intent);
                                finish();
                            } else {
                                Snackbar.make(binding.getRoot(), response.getString("msg"), Snackbar.LENGTH_LONG).show();
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {
            Log.e("Volley Error", "Error: " + error.getMessage());
            Snackbar.make(binding.getRoot(), "Network Error", Snackbar.LENGTH_LONG).show();
            //xml for slow networks
            //progressBar.setVisibility(View.INVISIBLE);
            canClick = true;
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
                20000,
                0,
                2));
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);
    }
}