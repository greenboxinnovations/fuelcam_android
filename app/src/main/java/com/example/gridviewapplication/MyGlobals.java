package com.example.gridviewapplication;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MyGlobals {
    private Context mContext;

    // constructor
    MyGlobals(Context context) {
        this.mContext = context;
    }

    boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert cm != null;
        NetworkInfo ni = cm.getActiveNetworkInfo();

        assert ni != null;
        if (ni.getTypeName().equalsIgnoreCase("MOBILE")) {
            return ni.isConnected();
        }
        return false;
    }

    boolean isWiFiEnabled() {
        ConnectivityManager connManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        assert connManager != null;
        NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        assert mWifi != null;
        return mWifi.isConnected();
    }


    boolean networkInfo() {
        String DEBUG_TAG = "NetworkStatusExample";
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.isActiveNetworkMetered();
    }

    void useOnlyWifi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {

            ConnectivityManager connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);

            for (Network net : connectivityManager.getAllNetworks()) {

                NetworkInfo networkInfo = connectivityManager.getNetworkInfo(net);

                if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI) {
                    connectivityManager.bindProcessToNetwork(net);
                    break;
                }
            }
        }
    }


    private ArrayList<POJO_Transaction> transList = new ArrayList<>();


    public boolean isNozzleBusy(String nozzle_qr) {
        ArrayList<POJO_Transaction> transList = getTransPojoList();

        logList(transList, "isNozzleBusy: ");

        for (POJO_Transaction transaction : transList) {
            if (transaction.getNozzle_qr().equals(nozzle_qr)) {
                return true;
            }
        }
        return false;
    }


    public int getTransListSize() {
        ArrayList<POJO_Transaction> transList = getTransPojoList();
        return transList.size();
    }

    public boolean isPlateNoBusy(String plate_no) {
        ArrayList<POJO_Transaction> transList = getTransPojoList();

        logList(transList, "removeTransPojoList: ");


        for (POJO_Transaction transaction : transList) {
            if (transaction.getCar_plate_no().equals(plate_no)) {
                return true;
            }
        }
        return false;
    }

    public void logList(ArrayList<POJO_Transaction> transList, String action) {
        for (POJO_Transaction trans : transList) {
            Log.e("myGlobals", action + trans.getCar_plate_no());
        }
    }

    public void insertTransPojoList(POJO_Transaction transaction) {

        ArrayList<POJO_Transaction> transList = getTransPojoList();
        transList.add(transaction);

        // store in prefs
        Gson gson = new Gson();
        String json = gson.toJson(transList);
        PrefUtils.saveToPrefs(mContext, PrefKeys.TRANS_LIST, json);

        logList(transList, "insertTransPojoList: ");
    }

    public void removeTransPojoListByCarPlate(String plate_no) {
        ArrayList<POJO_Transaction> transList = getTransPojoList();

        for (int i = 0; i < transList.size(); i++) {
            if (transList.get(i).getCar_plate_no().equals(plate_no)) {
                transList.remove(transList.get(i));
            }
        }

        // store in prefs
        Gson gson = new Gson();
        String json = gson.toJson(transList);
        PrefUtils.saveToPrefs(mContext, PrefKeys.TRANS_LIST, json);

        logList(transList, "removeTransPojoList: ");
    }

    public ArrayList<POJO_Transaction> getTransPojoList() {
        String json = (String) PrefUtils.getFromPrefs(mContext, PrefKeys.TRANS_LIST, "");

        // no shared prefs found
        if (json.equals("")) {
            Log.e("myGlobals", "no list found in shared prefs: " + transList.size());
//            return transList;
            return new ArrayList<>();
        }

        Gson gson = new Gson();
        Type type = new TypeToken<ArrayList<POJO_Transaction>>() {
        }.getType();
        //transList = gson.fromJson(json, type);
        return gson.fromJson(json, type);
    }


    public String getDateString() {
        Date cDate = new Date();
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(cDate);
    }

    public void promptWiFiConnection(Activity activity) {
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(activity);

        alertDialogBuilder.setTitle("WiFi needed");
        alertDialogBuilder.setMessage("Please connect to pump WiFi network");
        alertDialogBuilder.setPositiveButton("Open Setting", (dialogInterface, i) -> {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_WIFI_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            activity.getApplication().startActivity(intent);
        });
        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//                Log.d(TAG, "onClick: Cancelling");
            }
        });

        AlertDialog dialog = alertDialogBuilder.create();
        dialog.show();
    }

}
