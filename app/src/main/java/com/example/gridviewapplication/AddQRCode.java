package com.example.gridviewapplication;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.gridviewapplication.databinding.ActivityAddQrcodeBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class AddQRCode extends AppCompatActivity implements AdapterCustomerList.gridListener {

    private ActivityAddQrcodeBinding binding;

    private static final String TAG = AddQRCode.class.getSimpleName();

    private AdapterCustomerList mAdapter;
    private ArrayList<POJO_id_string> customerList = new ArrayList<>();

    private int pump_id;
    private ProgressBar progressBar;
    private Button b_retry;

    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAddQrcodeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        init();

        pump_id = (int) PrefUtils.getFromPrefs(AddQRCode.this, PrefKeys.PUMP_ID, 0);

    }

    private void init() {

        AdapterCustomerList.gridListener mListener = this;

        recyclerView = binding.contentAddQrcode.rvCustomerList;
        mAdapter = new AdapterCustomerList(customerList, this, mListener);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);

        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(mAdapter);

        b_retry = binding.contentAddQrcode.bCustListRetry;
        progressBar = binding.contentAddQrcode.progressBarCustList;

        b_retry.setOnClickListener(v -> {
            b_retry.setVisibility(View.INVISIBLE);
            getCustomerList();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        getCustomerList();
    }

    private void getCustomerList() {

        recyclerView.setVisibility(View.INVISIBLE);
        progressBar.setVisibility(View.VISIBLE);


        String url = getResources().getString(R.string.url_hosted) + "/api/customers/" + pump_id;
        Log.d(TAG, url);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(
                Request.Method.GET,
                url,
                null,
                response -> {

                    recyclerView.setVisibility(View.VISIBLE);
                    progressBar.setVisibility(View.INVISIBLE);

                    customerList.clear();

                    POJO_id_string pojo1 = new POJO_id_string();
                    pojo1.setCust_id(-99);
                    pojo1.setDisplay_name("ADD NEW CUSTOMER");
                    customerList.add(pojo1);
                    mAdapter.notifyDataSetChanged();

                    try {
                        for (int i = 0; i < response.length(); i++) {
                            // Get current json object
                            JSONObject customer = response.getJSONObject(i);

                            // Get the current student (json object) data
                            String cust_company = customer.getString("cust_company");
                            String cust_f_name = customer.getString("cust_f_name");
                            String cust_l_name = customer.getString("cust_l_name");
                            int cust_id = customer.getInt("cust_id");

                            String display_name;
                            if (cust_company.equals("")) {
                                display_name = cust_f_name + " " + cust_l_name;
                            } else {
                                display_name = cust_company;
                            }


                            POJO_id_string pojo = new POJO_id_string();
                            pojo.setCust_id(cust_id);
                            pojo.setDisplay_name(display_name);
                            customerList.add(pojo);

                        }
                        mAdapter.notifyDataSetChanged();
                        Log.e(TAG, "customerList size" + customerList.size());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    // Do something when error occurred
                    progressBar.setVisibility(View.INVISIBLE);
                    b_retry.setVisibility(View.VISIBLE);
                    Log.e(TAG, error.toString());
                    Snackbar.make(
                            binding.getRoot(),
                            "Error fetching JSON",
                            Snackbar.LENGTH_LONG
                    ).show();
                }
        );
        jsonArrayRequest.setRetryPolicy(new DefaultRetryPolicy(
                20000,
                5,
                2));
        MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonArrayRequest);
    }


    @Override
    public void listClick(int position) {
        int cust_id = customerList.get(position).getCust_id();
        Log.e(TAG, "id: " + cust_id);

        if (cust_id == -99) {
            Intent i = new Intent(getApplicationContext(), AddNewCustomer.class);
            startActivity(i);
        } else {
            Intent i = new Intent(getApplicationContext(), CarList.class);
            String cust_name = customerList.get(position).getDisplay_name();
            i.putExtra("cust_id", cust_id);
            i.putExtra("cust_name", cust_name);
            startActivity(i);
        }
    }
}