package com.example.gridviewapplication;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.gridviewapplication.databinding.ActivityLoginBinding;


import android.view.Menu;
import android.view.MenuItem;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;

public class Login extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityLoginBinding binding;

    private static final int PERMISSION_REQUEST_ALL = 0;
    private View mLayout;

    private static final String TAG = Login.class.getName();

    private MyGlobals myGlobals;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        mLayout = binding.getRoot();

        // init
        myGlobals = new MyGlobals(getApplicationContext());


        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onResume() {
        super.onResume();
        if (AppUtils.hasPermissions(this)) {
            Log.e(TAG, "hasPermissions");


            String pref_date = (String) PrefUtils.getFromPrefs(this, PrefKeys.DATE, "");
            String shift = (String) PrefUtils.getFromPrefs(this, PrefKeys.USER_SHIFT, "");
            // date is today and shift is found
            String date = myGlobals.getDateString();

            if ((date.equals(pref_date)) && (!shift.equals(""))) {

                Intent i = new Intent(Login.this, Home.class);
                startActivity(i);
                finish();
            } else {
                Log.e(TAG, "isActiveNetworkMetered");
                if (myGlobals.networkInfo()) {
                    myGlobals.promptWiFiConnection(Login.this);
                }
            }


//            if (myGlobals.networkInfo()) {
//                // mobile network
//                Log.e(TAG, "isActiveNetworkMetered");
//                myGlobals.promptWiFiConnection(Login.this);
//                //promptWiFiConnection();
//            } else {
//                // wifi
//                Log.e(TAG, "not Metered");
//                Log.e(TAG, "useOnlyWifi");
//                //myGlobals.useOnlyWifi();
//
//                // date is today and shift is found
//                String date = myGlobals.getDateString();
//                String pref_date = (String) PrefUtils.getFromPrefs(this, PrefKeys.DATE, "");
//                String shift = (String) PrefUtils.getFromPrefs(this, PrefKeys.USER_SHIFT, "");
//                if ((date.equals(pref_date)) && (!shift.equals(""))) {
//
//                    Intent i = new Intent(Login.this, Home.class);
//                    startActivity(i);
//                    finish();
//                }
//            }

        } else {
            Log.e(TAG, "myRequestPermission");
            // user can deny twice
            // redirect to settings after 2 denies
            // must select while using app to work
            myRequestPermission();
        }
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.menu_main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up button, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//
//        //noinspection SimplifiableIfStatement
//        if (id == R.id.add_new_rate) {
//            //showRatesDialog();
//        }
//
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void myRequestPermission() {

        // user may deny twice, then permission dialog wont work correctly
        boolean userAskedPermissionBefore = (Boolean) PrefUtils.getFromPrefs(this, PrefKeys.USER_ASKED_STORAGE_PERMISSION_BEFORE, false);

        // Log.e("TAG", "pre requestPermissions");
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            Log.e(TAG, "shouldShowRequestPermissionRationale");

            // In an educational UI, explain to the user why your app requires this
            // permission for a specific feature to behave as expected. In this UI,
            // include a "cancel" or "no thanks" button that allows the user to
            // continue using your app without granting the permission.
            Snackbar.make(mLayout, R.string.camera_access_required,
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    /*
                    ActivityCompat.requestPermissions(Login.this,
                            new String[]{Manifest.permission.CAMERA},
                            PERMISSION_REQUEST_CAMERA);
                     */

                    // Request the permission
                    ActivityCompat.requestPermissions(Login.this,
                            AppUtils.PERMISSIONS,
                            PERMISSION_REQUEST_ALL);
                }
            }).show();
        } else {

            if (userAskedPermissionBefore) {
                // If User was asked permission before and denied
                Log.e(TAG, "If User was asked permission before and denied");

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

                alertDialogBuilder.setTitle("Permission needed");
                alertDialogBuilder.setMessage("Storage permission needed for accessing photos");
                alertDialogBuilder.setPositiveButton("Open Setting", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", Login.this.getPackageName(),
                                null);
                        intent.setData(uri);
                        Login.this.startActivity(intent);
                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Log.d(TAG, "onClick: Cancelling");
                    }
                });

                AlertDialog dialog = alertDialogBuilder.create();
                dialog.show();

            } else {
                // If user is asked permission for first time
                Log.e(TAG, "If user is asked permission for first time");
                /*
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CAMERA},
                        PERMISSION_REQUEST_CAMERA);
                 */
                // You can directly ask for the permission.
                ActivityCompat.requestPermissions(Login.this,
                        AppUtils.PERMISSIONS,
                        PERMISSION_REQUEST_ALL);

                // Update the permission boolean
                PrefUtils.saveToPrefs(this, PrefKeys.USER_ASKED_STORAGE_PERMISSION_BEFORE, true);
            }
        }
    }


//    private void promptWiFiConnection() {
//        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
//
//        alertDialogBuilder.setTitle("WiFi needed");
//        alertDialogBuilder.setMessage("Please connect to pump WiFi network");
//        alertDialogBuilder.setPositiveButton("Open Setting", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_WIFI_SETTINGS);
////                Uri uri = Uri.fromParts("package", Login.this.getPackageName(),
////                        null);
////                intent.setData(uri);
////                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
//                Login.this.startActivity(intent);
//            }
//        });
//        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialogInterface, int i) {
//                Log.d(TAG, "onClick: Cancelling");
//            }
//        });
//
//        AlertDialog dialog = alertDialogBuilder.create();
//        dialog.show();
//    }
}