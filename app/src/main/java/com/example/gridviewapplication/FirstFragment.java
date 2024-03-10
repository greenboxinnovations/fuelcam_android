package com.example.gridviewapplication;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.RetryPolicy;
import com.android.volley.TimeoutError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.gridviewapplication.databinding.ActivityLoginBinding;
import com.example.gridviewapplication.databinding.FragmentLoginFormBinding;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FirstFragment extends Fragment {

    /**
     * Fragment for the login form
     * logUser
     * goToSetRates
     * chooseShiftDialog
     * goToHomeActivity
     */

    // private static final String TAG = FirstFragment.class.getName();
    private static final String TAG = FirstFragment.class.getSimpleName();

    private FragmentLoginFormBinding binding;
    private ProgressBar progressBar;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        binding = FragmentLoginFormBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        progressBar = binding.progressBarLogin;
        binding.buttonLogin.setOnClickListener(v -> {
            // Log.e(TAG, "works");
            String name = binding.loginName.getText().toString();
            String pass = binding.loginPassword.getText().toString();
            Log.e(TAG, name + pass);

            logUser(name, pass);

            // testVolley();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void logUser(String name, String pass) {

        // Empty values not allowed
        if ((name.length() == 0) || (pass.length() == 0)) {
            Snackbar.make(binding.getRoot(), "Empty Fields!", Snackbar.LENGTH_SHORT).show();
            return;
        }

        // Snackbar.make(binding.getRoot(), "all good", Snackbar.LENGTH_SHORT).show();

        //String url = "http://192.168.1.100/fuelcam/login";
        String url = getResources().getString(R.string.url_local_fuelcam) + "/login";


        // check if date mismatch
        // and redirect to rates if mismatch
        Date cDate = new Date();
        final String date = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cDate);
        // Log.e("date", date);


        JSONObject jsonObj = new JSONObject();
        try {
            jsonObj.put("name", name);
            jsonObj.put("pass", pass);
            jsonObj.put("version", "1.5");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // network activity
        progressBar.setVisibility(View.VISIBLE);
        binding.buttonLogin.setEnabled(false);

        JsonObjectRequest jsonObjReq = new JsonObjectRequest(Request.Method.POST,
                url, jsonObj,
                response -> {
                    Log.e("login response", response.toString());


                    // xml for slow networks
                    progressBar.setVisibility(View.INVISIBLE);
                    binding.buttonLogin.setEnabled(true);

                    try {
                        if (response.getBoolean("success")) {
                            Log.e("result", "success");

                            // dont allow rate set

//                            int user_id = response.getInt("user_id");
//                            int pump_id = response.getInt("pump_id");
//                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.DATE, response.getString("date"));
//                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ROLE, response.getString("role"));
//                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ID, user_id);
//                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.PUMP_ID, pump_id);
//                            chooseShiftDialog();


                            // allow rate set
                            if (!response.getBoolean("rate_set")) {
                                int user_id = response.getInt("user_id");
                                int pump_id = response.getInt("pump_id");
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ID, user_id);
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.PUMP_ID, pump_id);
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ROLE, response.getString("role"));
                                goToSetRates(user_id, pump_id);
                            } else if ((response.getString("date").equals(date)) && (response.getBoolean("rate_set"))) {

                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_NAME, response.getString("user_name"));
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ID, response.getInt("user_id"));
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.PUMP_ID, response.getInt("pump_id"));

                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.PETROL_RATE, response.getString("petrol_rate"));
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.DIESEL_RATE, response.getString("diesel_rate"));
                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.DATE, response.getString("date"));

                                PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ROLE, response.getString("role"));

                                chooseShiftDialog();
                            }

                        } else if (!response.getBoolean("success")) {
                            // Password Username error

                            // logout user from shared prefs
                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ID, -1);
                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.PUMP_ID, -1);
                            PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ROLE, "");

                            // hide the keyboard
                            AppUtils.hideSoftKeyboard(requireActivity());

                            // show error message, changeable from server
                            String message = response.getString("msg");
                            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> {

            // xml for slow networks
            progressBar.setVisibility(View.INVISIBLE);
            binding.buttonLogin.setEnabled(true);

            // possible timeout
            AppUtils.hideSoftKeyboard(requireActivity());
            Snackbar.make(binding.getRoot(), "Network Timeout", Snackbar.LENGTH_SHORT).show();

            NetworkResponse networkResponse = error.networkResponse;
            if (networkResponse != null && networkResponse.statusCode == 409) {
                // HTTP Status Code: 409 Client error
                try {

                    // 409 is not possible from android, but still included to catch random error
                    String jsonString = new String(networkResponse.data, HttpHeaderParser.parseCharset(networkResponse.headers));
                    JSONObject obj = new JSONObject(jsonString);
                    String message = obj.getString("message");

                    // logout user from shared prefs
                    PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ID, -1);
                    PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.PUMP_ID, -1);
                    PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_ROLE, "");

                    // hide the keyboard
                    AppUtils.hideSoftKeyboard(requireActivity());

                    // Log.e("NetworkResponse", message);
                    Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
                } catch (UnsupportedEncodingException | JSONException e) {
                    e.printStackTrace();
                }
            }
            // error.printStackTrace();
        }) {

            @Override
            public Map<String, String> getHeaders() {
                HashMap<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("charset", "utf-8");
                return headers;
            }
        };
        // MySingleton.getInstance(this.getApplicationContext()).addToRequestQueue(jsonObjReq);

        jsonObjReq.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                0,
                2));

        MySingleton.getInstance(requireActivity().getApplicationContext()).addToRequestQueue(jsonObjReq);
    }

    private void goToSetRates(int user_id, int pump_id) {
        Log.e(TAG, "goToSetRates");
        Intent i = new Intent(requireActivity().getApplicationContext(), SetRates.class);
        i.putExtra("user_id", user_id);
        i.putExtra("pump_id", pump_id);
        i.putExtra("update_rate", false);
        startActivity(i);
        requireActivity().finish();
    }

    private void chooseShiftDialog() {

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(requireActivity());
        alertDialogBuilder.setTitle("Select Shift");
        alertDialogBuilder.setPositiveButton("Shift B", (dialogInterface, i) -> goToHomeActivity("b"));
        alertDialogBuilder.setNegativeButton("Shift A", (dialogInterface, i) -> goToHomeActivity("a"));
        alertDialogBuilder.setNeutralButton("Shift C", (dialogInterface, i) -> goToHomeActivity("c"));
        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

    private void goToHomeActivity(String shift) {
        PrefUtils.saveToPrefs(requireActivity().getApplicationContext(), PrefKeys.USER_SHIFT, shift);
        Intent i = new Intent(requireActivity().getApplicationContext(), Home.class);
        startActivity(i);
        requireActivity().finish();
    }

    private void testVolley() {

        String url = "http://192.168.0.100/fuelcam/test";

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                Log.e("login response", response.toString());
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        MySingleton.getInstance(requireActivity().getApplicationContext()).addToRequestQueue(jsonObjectRequest);
    }
}