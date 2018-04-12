package com.likpia.recenthostget;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.likpia.recenthostget.utils.DownloadTool;
import com.stericson.RootTools.RootTools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, View.OnClickListener {

    private TextView onKeyTv;
    private ProgressBar pb;
    private boolean isOneKeyEnable = true;
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            if (msg.what == 1) {
                Snackbar.make(rootView, "替换成功", BaseTransientBottomBar.LENGTH_SHORT).show();
                refreshLocalUpdateDate();
            } else if (msg.what == -1) {
                Snackbar.make(rootView, "替换失败", BaseTransientBottomBar.LENGTH_SHORT).show();
            } else if (msg.what == 2) {
                Snackbar.make(rootView, "文件不通过", BaseTransientBottomBar.LENGTH_SHORT).show();
            } else if (msg.what == 3) {
                updateTimeTv.setText(msg.getData().getString("time"));
            }
            onKeyTv.setText("一键获取最新\nHOST并替换");
            pb.setVisibility(View.GONE);
            return false;
        }
    });
    private TextView updateTimeTv;
    private DownloadTool tool;
    private View rootView;

    private void refreshLocalUpdateDate() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                if (RootTools.isAccessGiven()) {
                    handle();
                }
            }

            private void handle() {
                try {
                    File file = File.createTempFile("hosts" + System.currentTimeMillis(), "");

                    boolean isSuccess = RootTools.copyFile("/system/etc/hosts", file.getAbsolutePath(), false, false);
                    if (isSuccess) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));
                        while (true) {
                            String line = reader.readLine();
                            if (line == null) {
                                break;
                            }
                            if (line.toLowerCase().contains("last updated:")) {
                                String lowLine = line.toLowerCase();
                                int startIndex = lowLine.indexOf("last updated:");
                                Message msg = new Message();
                                msg.what = 3;
                                Bundle bundle = new Bundle();
                                bundle.putString("time", lowLine.substring(startIndex + "last updated:".length(), lowLine.length()));
                                msg.setData(bundle);
                                handler.sendMessage(msg);
                                break;
                            }


                        }
                    }


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }).start();


    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        pb = findViewById(R.id.pb);
        rootView = findViewById(R.id.root);
        onKeyTv = findViewById(R.id.tv_one_host);
        updateTimeTv = findViewById(R.id.update_time_tv);
        refreshLocalUpdateDate();
        findViewById(R.id.one_key_get_host).setOnClickListener(this);
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }


    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        switch (id) {
            case R.id.nav_setting:
                startActivity(new Intent(this, SettingActivity.class));
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.one_key_get_host:
                if (!isOneKeyEnable) {
                    new AlertDialog.Builder(this).setTitle("提示").setMessage("确定取消下载?").setNegativeButton("取消", null).setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            tool.cancel();
                        }
                    }).show();
                    return;
                }

                String hostUrl = PreferenceManager.getDefaultSharedPreferences(this).getString("host_url", "");
                try {
                    tool = new DownloadTool.Builder(this).setUrl(!hostUrl.isEmpty() ? hostUrl : MyConfig.defaultHostUrl).setFile(File.createTempFile("host", "")).setLoadListener(new DownloadTool.DownLoadListener() {
                        @Override
                        public void onSuccess(final File file) {

                            onKeyTv.setText("下载完成");
                            Toast.makeText(MainActivity.this, "处理中...", Toast.LENGTH_SHORT).show();
                            new Thread(new Runnable() {
                                private void replace(File file) {
                                    try {
                                        boolean result = RootTools.copyFile(file.getAbsolutePath(), "/system/etc/hosts", true, false);
                                        if (result) {
                                            handler.sendEmptyMessage(1);
                                            return;
                                        }
                                    } catch (Exception ignored) {
                                    }
                                    handler.sendEmptyMessage(-1);
                                }

                                private boolean checkFile() {
                                    try {
                                        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "utf-8"));

                                        while (true) {
                                            String line = reader.readLine();
                                            if (line == null) {
                                                break;
                                            }
                                        }
                                        reader.close();
                                        return true;
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    return false;
                                }

                                @Override
                                public void run() {
                                    if (!checkFile()) {
                                        handler.sendEmptyMessage(2);
                                        return;
                                    }
                                    if (RootTools.isAccessGiven()) {
                                        replace(file);
                                    }
                                }
                            }).start();


                        }

                        @Override
                        public void onFinished() {
                            isOneKeyEnable = true;
                        }

                        @Override
                        public void onCanceled() {
                            Snackbar.make(rootView, "已取消", BaseTransientBottomBar.LENGTH_SHORT).show();
                            pb.setVisibility(View.GONE);
                            onKeyTv.setText("一键获取最新\nHOST并替换");
                        }

                        @Override
                        public void onProgress(int percentage) {
                            Log.i("SUHAODONG", percentage + "");
                            pb.setProgress(percentage);
                        }

                        @Override
                        public void onStart() {
                            isOneKeyEnable = false;
                            pb.setProgress(0);
                            onKeyTv.setText(String.valueOf("下载中..."));
                            pb.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(String e) {
                            onKeyTv.setText("下载失败");
                        }
                    }).build();
                    tool.writeToFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
        }
    }
}
