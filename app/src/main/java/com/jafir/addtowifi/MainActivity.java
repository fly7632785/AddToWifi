package com.jafir.addtowifi;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_WIFI = 1;
    private SwipeRefreshLayout mRefreshLayout;
    private ListView mLisview;
    private WifiAdmin wifiAdmin;
    private WifiAdapter mAdapter;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                getData();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initWifi();
    }

    private void initView() {
        mLisview = (ListView) findViewById(R.id.listview);
        mRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe);
        mRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                wifiAdmin.startScan();
                mRefreshLayout.setRefreshing(false);
            }
        });
    }

    private void initWifi() {
        wifiAdmin = new WifiAdmin(this);
        wifiAdmin.openWifi();
        addAccount();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            if (checkPermissions()) {
                wifiAdmin.startScan();
            }
        } else {
            wifiAdmin.startScan();
        }
        registerReceiver(receiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
    }

    private void addAccount() {
        wifiAdmin.addNetwork(wifiAdmin.CreateEnterpriseWifiInfo("van-staff_5G-EAP", "i@xloli.com", "12345678"));
        Toast.makeText(this, "success", Toast.LENGTH_SHORT).show();
    }

    private boolean checkPermissions() {
        List<String> permissionsList = new ArrayList<String>();
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }

        if (permissionsList.size() > 0) {
            ActivityCompat.requestPermissions(this, permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_WIFI);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_WIFI) {
            if (permissions.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED ||
                    (permissions.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                            grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
                wifiAdmin.startScan();
            } else {
                Toast.makeText(this, "没有权限", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void getData() {
        List<ScanResult> data = getConfigData();
        mAdapter = new WifiAdapter(this, android.R.layout.simple_list_item_1);
        mAdapter.addAll(data);
        mLisview.setAdapter(mAdapter);
    }

    /**
     * 显示已经配置好的wifi列表
     *
     * @return
     */
    private List<ScanResult> getConfigData() {
        List<ScanResult> newData = new ArrayList<>();
        List<WifiConfiguration> configData = wifiAdmin.getConfiguration();
        List<ScanResult> data = wifiAdmin.getWifiList();
        for (ScanResult scanResult : data) {
            for (WifiConfiguration wifiConfiguration : configData) {
                //如果扫描出来的里面也已经在配置里面就显示
                String ssid = wifiConfiguration.SSID.substring(1, wifiConfiguration.SSID.length() - 1);
                if (scanResult.SSID.equals(ssid)) {
                    newData.add(scanResult);
                }
            }
        }
        return newData;
    }

    class WifiAdapter extends ArrayAdapter<ScanResult> {
        public WifiAdapter(@NonNull Context context, @LayoutRes int resource) {
            super(context, resource);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ScanResult result = getItem(position);
            TextView text = new TextView(parent.getContext());
            StringBuilder builder = new StringBuilder();
            builder.append(result.SSID + "\n" + Math.abs(result.level));
            text.setText(builder);
            return text;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(receiver);
    }
}
