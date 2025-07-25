package com.vone.vmq;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;
import com.vone.qrcode.R;
import com.vone.vmq.util.Constant;

import org.json.JSONObject;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView txthost;
    private TextView txtkey;
    private TextView logTextView;
    private ScrollView logScrollView;
    private LogBroadcastReceiver logBroadcastReceiver;

    private boolean isOk = false;
    private static String TAG = "MainActivity";

    private static String host;
    private static String key;
    int id = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txthost = (TextView) findViewById(R.id.txt_host);
        txtkey = (TextView) findViewById(R.id.txt_key);
        logTextView = (TextView) findViewById(R.id.log_text_view);
        logScrollView = (ScrollView) findViewById(R.id.log_scroll_view);

        //检测通知使用权是否启用
        if (!isNotificationListenersEnabled()) {
            //跳转到通知使用权页面
            gotoNotificationAccessSetting();
        } else if (!Utils.checkBatteryWhiteList(this)) {
            Utils.gotoBatterySetting(this);
        }
        //重启监听服务
        if (!NeNotificationService2.isRunning) {
            toggleNotificationListenerService(this);
        }
        //读入保存的配置数据并显示
        SharedPreferences read = getSharedPreferences("vone", MODE_PRIVATE);
        host = read.getString("host", "");
        key = read.getString("key", "");

        if (host != null && key != null && host != "" && key != "") {
            txthost.setText(" 通知地址：" + host);
            txtkey.setText(" 通讯密钥：" + key);
            isOk = true;
        }
        Toast.makeText(MainActivity.this, "v免签开源免费免签系统 v2.1", Toast.LENGTH_SHORT).show();

        // 注册广播接收器
        logBroadcastReceiver = new LogBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NeNotificationService2.ACTION_LOG_UPDATE);
        registerReceiver(logBroadcastReceiver, intentFilter);
    }

    // 在Activity销毁时取消注册
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logBroadcastReceiver != null) {
            unregisterReceiver(logBroadcastReceiver);
        }
    }

    private void appendLog(final String message) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                logTextView.append("\n" + message);
                // 自动滚动到底部
                logScrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        logScrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        });
    }

    // 广播接收器，用于接收来自Service的日志
    private class LogBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && NeNotificationService2.ACTION_LOG_UPDATE.equals(intent.getAction())) {
                String logMessage = intent.getStringExtra("log_message");
                if (logMessage != null) {
                    appendLog(logMessage);
                }
            }
        }
    }

    //扫码配置
    public void startQrCode(View v) {
        // 申请相机权限
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, Constant.REQ_PERM_CAMERA);
            return;
        }
        // 申请文件读写权限（部分朋友遇到相册选图需要读写权限的情况，这里一并写一下）
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // 申请权限
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, Constant.REQ_PERM_EXTERNAL_STORAGE);
            return;
        }
        // 二维码扫码
        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
        startActivityForResult(intent, Constant.REQ_QR_CODE);
    }

    //手动配置
    public void doInput(View v) {
        final EditText inputServer = new EditText(this);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("请输入配置数据").setView(inputServer)
                .setNegativeButton("取消", null);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                String scanResult = inputServer.getText().toString();

                String[] tmp = scanResult.split("/");
                if (tmp.length != 2) {
                    Toast.makeText(MainActivity.this, "数据错误，请您输入网站上显示的配置数据!", Toast.LENGTH_SHORT).show();
                    return;
                }

                String t = String.valueOf(new Date().getTime());
                String sign = md5(t + tmp[1]);

                Request request = new Request.Builder().url("http://" + tmp[0] + "/api/monitor/heart?t=" + t + "&sign=" + sign).method("GET", null).build();
                Call call = Utils.getOkHttpClient().newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {

                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        try {
                            String responseBody = response.body().string();
                            Log.d(TAG, "配置测试心跳返回: " + responseBody);
                            Log.d(TAG, "配置测试HTTP状态码: " + response.code());
                        } catch (Exception e) {
                            Log.e(TAG, "配置测试心跳异常: " + e.getMessage(), e);
                            e.printStackTrace();
                        }
                        isOk = true;
                    }
                });
                if (tmp[0].indexOf("localhost") >= 0) {
                    Toast.makeText(MainActivity.this, "配置信息错误，本机调试请访问 本机局域网IP:8080(如192.168.1.101:8080) 获取配置信息进行配置!", Toast.LENGTH_LONG).show();

                    return;
                }
                //将扫描出的信息显示出来
                txthost.setText(" 通知地址：" + tmp[0]);
                txtkey.setText(" 通讯密钥：" + tmp[1]);
                host = tmp[0];
                key = tmp[1];

                SharedPreferences.Editor editor = getSharedPreferences("vone", MODE_PRIVATE).edit();
                editor.putString("host", host);
                editor.putString("key", key);
                editor.commit();

            }
        });
        builder.show();

    }

    //检测心跳
    public void doStart(View view) {
        if (!isOk) {
            Toast.makeText(MainActivity.this, "请您先配置!", Toast.LENGTH_SHORT).show();
            return;
        }

        appendLog("开始检测心跳...");
        String t = String.valueOf(new Date().getTime());
        String sign = md5(t + key);

        Request request = new Request.Builder().url("http://" + host + "/api/monitor/heart?t=" + t + "&sign=" + sign).method("GET", null).build();
        Call call = Utils.getOkHttpClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, "心跳状态错误，请检查配置是否正确!", Toast.LENGTH_SHORT).show();
                        appendLog("心跳请求失败: " + e.getMessage());
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                try {
                    // 在网络线程中读取响应体
                    final String responseBody = response.body().string();
                    final int httpCode = response.code();

                    // 切换到UI线程处理结果
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                appendLog("心跳返回: " + responseBody);
                                Log.d(TAG, "心跳返回原始数据: " + responseBody);
                                Log.d(TAG, "HTTP状态码: " + httpCode);

                                JSONObject jsonObject = new JSONObject(responseBody);
                                Log.d(TAG, "JSON解析成功");

                                // 兼容新旧两种返回格式
                                // 新格式：{"code": 200, "msg": "消息", "data": null}
                                // 旧格式：{"code": 0, "msg": "消息"}
                                int code = jsonObject.getInt("code");
                                String msg = jsonObject.getString("msg");

                                Log.d(TAG, "解析到的code: " + code + ", msg: " + msg);

                                if (code == 200 || code == 0 || code == 1) {
                                    // 成功状态 (兼容code=1的情况)
                                    Toast.makeText(MainActivity.this, "心跳返回：" + msg, Toast.LENGTH_LONG).show();
                                    appendLog("心跳成功: " + msg);
                                    Log.d(TAG, "心跳成功");
                                } else {
                                    // 错误状态
                                    Toast.makeText(MainActivity.this, "心跳错误：" + msg, Toast.LENGTH_LONG).show();
                                    appendLog("心跳失败: " + msg);
                                    Log.d(TAG, "心跳失败，code: " + code);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "心跳数据解析异常: " + e.getMessage(), e);
                                e.printStackTrace();
                                Toast.makeText(MainActivity.this, "心跳返回数据解析失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                appendLog("心跳返回数据解析异常: " + e.getMessage());
                            }
                        }
                    });
                } catch (Exception e) {
                    Log.e(TAG, "网络响应读取异常: " + e.getMessage(), e);
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "网络响应读取失败", Toast.LENGTH_LONG).show();
                            appendLog("心跳请求读取网络响应失败");
                        }
                    });
                }
            }
        });
    }

    public void clearLogs(View view) {
        logTextView.setText("日志输出:");
    }

    public void checkPush(View v) {
        Notification mNotification;
        NotificationManager mNotificationManager;
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("1",
                    "Channel1", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.setShowBadge(true);
            mNotificationManager.createNotificationChannel(channel);

            Notification.Builder builder = new Notification.Builder(this, "1");

            mNotification = builder
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker("这是一条测试推送信息，如果程序正常，则会提示监听权限正常")
                    .setContentTitle("V免签测试推送")
                    .setContentText("这是一条测试推送信息，如果程序正常，则会提示监听权限正常")
                    .build();
        } else {
            mNotification = new Notification.Builder(MainActivity.this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setTicker("这是一条测试推送信息，如果程序正常，则会提示监听权限正常")
                    .setContentTitle("V免签测试推送")
                    .setContentText("这是一条测试推送信息，如果程序正常，则会提示监听权限正常")
                    .build();
        }
        //Toast.makeText(MainActivity.this, "已推送信息，如果权限，那么将会有下一条提示！", Toast.LENGTH_SHORT).show();

        mNotificationManager.notify(id++, mNotification);
    }

    //各种权限的判断
    private void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, NeNotificationService2.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(context, NeNotificationService2.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);

        // 不要每次打开都显示
        // Toast.makeText(MainActivity.this, "监听服务启动中...", Toast.LENGTH_SHORT).show();
    }

    public boolean isNotificationListenersEnabled() {
        String pkgName = getPackageName();
        final String flat = Settings.Secure.getString(getContentResolver(), "enabled_notification_listeners");
        if (!TextUtils.isEmpty(flat)) {
            final String[] names = flat.split(":");
            for (int i = 0; i < names.length; i++) {
                final ComponentName cn = ComponentName.unflattenFromString(names[i]);
                if (cn != null) {
                    if (TextUtils.equals(pkgName, cn.getPackageName())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected boolean gotoNotificationAccessSetting() {
        try {
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {//普通情况下找不到的时候需要再特殊处理找一次
            try {
                Intent intent = new Intent();
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ComponentName cn = new ComponentName("com.android.settings", "com.android.settings.Settings$NotificationAccessSettingsActivity");
                intent.setComponent(cn);
                intent.putExtra(":settings:show_fragment", "NotificationAccessSettings");
                startActivity(intent);
                return true;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
            Toast.makeText(this, "对不起，您的手机暂不支持", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
            return false;
        }
    }

    public static String md5(String string) {
        if (TextUtils.isEmpty(string)) {
            return "";
        }
        MessageDigest md5 = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            byte[] bytes = md5.digest(string.getBytes());
            StringBuilder result = new StringBuilder();
            for (byte b : bytes) {
                String temp = Integer.toHexString(b & 0xff);
                if (temp.length() == 1) {
                    temp = "0" + temp;
                }
                result.append(temp);
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //扫描结果回调
        if (requestCode == Constant.REQ_QR_CODE && resultCode == RESULT_OK) {
            Bundle bundle = data.getExtras();
            String scanResult = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);

            String[] tmp = scanResult.split("/");
            if (tmp.length != 2) {
                Toast.makeText(MainActivity.this, "二维码错误，请您扫描网站上显示的二维码!", Toast.LENGTH_SHORT).show();
                return;
            }

            String t = String.valueOf(new Date().getTime());
            String sign = md5(t + tmp[1]);

            Request request = new Request.Builder().url("http://" + tmp[0] + "/api/monitor/heart?t=" + t + "&sign=" + sign).method("GET", null).build();
            Call call = Utils.getOkHttpClient().newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    try {
                        String responseBody = response.body().string();
                        Log.d(TAG, "扫码配置心跳返回: " + responseBody);
                        Log.d(TAG, "扫码配置HTTP状态码: " + response.code());
                    } catch (Exception e) {
                        Log.e(TAG, "扫码配置心跳异常: " + e.getMessage(), e);
                        e.printStackTrace();
                    }
                    isOk = true;
                }
            });

            //将扫描出的信息显示出来
            txthost.setText(" 通知地址：" + tmp[0]);
            txtkey.setText(" 通讯密钥：" + tmp[1]);
            host = tmp[0];
            key = tmp[1];

            SharedPreferences.Editor editor = getSharedPreferences("vone", MODE_PRIVATE).edit();
            editor.putString("host", host);
            editor.putString("key", key);
            editor.commit();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode(null);
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的相机访问权限", Toast.LENGTH_LONG).show();
                }
                break;
            case Constant.REQ_PERM_EXTERNAL_STORAGE:
                // 文件读写权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获得授权
                    startQrCode(null);
                } else {
                    // 被禁止授权
                    Toast.makeText(MainActivity.this, "请至权限中心打开本应用的文件读写权限", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }
}
