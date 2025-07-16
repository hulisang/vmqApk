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
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import android.text.TextUtils;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.activity.CaptureActivity;

import com.vone.qrcode.BuildConfig;
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

public class MainActivity extends AppCompatActivity implements ThemeChangeListener {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView txthost;
    private TextView txtkey;
    private TextView logTextView;
    private ScrollView logScrollView;
    private com.google.android.material.button.MaterialButton btnClearLogsInline;
    private LogBroadcastReceiver logBroadcastReceiver;

    private boolean isOk = false;
    private static String TAG = "MainActivity";
    private PerformanceMonitor performanceMonitor;
    private CompatibilityChecker compatibilityChecker;
    private UserGuideHelper userGuideHelper;

    private static String host;
    private static String key;
    int id = 0;

    // Activity Result Launchers - 替换过时的startActivityForResult
    private ActivityResultLauncher<Intent> qrCodeLauncher;
    private ActivityResultLauncher<Intent> manageStoragePermissionLauncher;
    
    // 启动画面诊断相关
    private SplashScreenDiagnostic splashScreenDiagnostic;
    private SplashScreenErrorHandler splashScreenErrorHandler;
    private LegacySplashScreenHelper legacySplashScreenHelper;
    private SplashScreenThemeAdapter splashScreenThemeAdapter;
    private SplashScreenPerformanceOptimizer performanceOptimizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 初始化性能监控
        performanceMonitor = PerformanceMonitor.getInstance(this);
        performanceMonitor.recordApplicationStart();
        
        // 初始化兼容性检查器和用户引导助手
        compatibilityChecker = new CompatibilityChecker(this);
        userGuideHelper = new UserGuideHelper(this);
        
        // 检查设备兼容性
        CompatibilityChecker.CompatibilityResult compatibilityResult = compatibilityChecker.checkCompatibility();
        if (!compatibilityResult.isAndroidVersionSupported) {
            userGuideHelper.showFeatureUnavailableGuide(
                "设备兼容性",
                "您的Android版本过低（当前：" + compatibilityResult.androidVersion + "，最低要求：23）",
                "请升级到Android 6.0或更高版本"
            );
        }
        
        // 应用Material You主题到Activity
        ThemeUtils.applyThemeToActivity(this);
        
        // 启动画面诊断和错误处理
        initializeSplashScreenDiagnostics();
        
        setContentView(R.layout.activity_main);

        txthost = (TextView) findViewById(R.id.txt_host);
        txtkey = (TextView) findViewById(R.id.txt_key);
        logTextView = (TextView) findViewById(R.id.log_text_view);
        logScrollView = (ScrollView) findViewById(R.id.log_scroll_view);
        btnClearLogsInline = (com.google.android.material.button.MaterialButton) findViewById(R.id.btn_clear_logs_inline);
        
        // 初始化清除按钮状态
        updateClearButtonState();

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


        // 检查并请求必要的权限
        checkAndRequestPermissions();

        // 注册广播接收器
        logBroadcastReceiver = new LogBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(NeNotificationService2.ACTION_LOG_UPDATE);
        
        // 适配Android 13+的BroadcastReceiver注册要求
        // 使用RECEIVER_EXPORTED因为我们需要接收来自Service的广播
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(logBroadcastReceiver, intentFilter, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(logBroadcastReceiver, intentFilter);
        }
        
        // 记录应用启动完成
        performanceMonitor.recordApplicationReady();
        
        // 开始内存监控
        performanceMonitor.monitorMemoryUsage();
        
        // 初始化Activity Result Launchers
        initializeActivityResultLaunchers();
        
        // 检查是否首次启动，显示欢迎引导
        if (userGuideHelper.isFirstLaunch()) {
            userGuideHelper.showWelcomeGuide();
        }
        
        // 执行启动画面诊断后处理
        performPostSplashScreenDiagnostics();
        
        // 记录MainActivity准备完成
        if (performanceOptimizer != null) {
            performanceOptimizer.recordMainActivityReady();
        }
    }

    // 在Activity销毁时取消注册
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (logBroadcastReceiver != null) {
            unregisterReceiver(logBroadcastReceiver);
        }
        
        // 清理启动画面相关资源
        try {
            if (legacySplashScreenHelper != null) {
                legacySplashScreenHelper.cleanup();
            }
            if (performanceOptimizer != null) {
                performanceOptimizer.cleanup();
            }
        } catch (Exception e) {
            Log.w(TAG, "清理启动画面资源失败: " + e.getMessage());
        }
    }

    /**
     * 初始化Activity Result Launchers
     */
    private void initializeActivityResultLaunchers() {
        // 二维码扫描结果处理
        qrCodeLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bundle bundle = result.getData().getExtras();
                        if (bundle != null) {
                            String scanResult = bundle.getString(Constant.INTENT_EXTRA_KEY_QR_SCAN);
                            handleQrCodeResult(scanResult);
                        }
                    }
                }
            }
        );

        // 管理存储权限设置结果处理
        manageStoragePermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        if (Environment.isExternalStorageManager()) {
                            Toast.makeText(MainActivity.this, "文件管理权限已授予", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(MainActivity.this, "文件管理权限未授予，扫码功能可能无法正常使用", Toast.LENGTH_LONG).show();
                        }
                    }
                }
            }
        );
    }

    /**
     * 处理二维码扫描结果
     */
    private void handleQrCodeResult(String scanResult) {
        if (scanResult == null || scanResult.isEmpty()) {
            Toast.makeText(this, "扫码结果为空", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] tmp = scanResult.split("/");
        if (tmp.length != 2) {
            Toast.makeText(MainActivity.this, "二维码错误，请您扫描网站上显示的二维码!", Toast.LENGTH_SHORT).show();
            return;
        }

        String t = String.valueOf(new Date().getTime());
        String sign = md5(t + tmp[1]);

        Request request = new Request.Builder()
            .url("http://" + tmp[0] + "/api/monitor/heart?t=" + t + "&sign=" + sign)
            .method("GET", null)
            .build();
        
        Call call = Utils.getOkHttpClient().newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                handler.post(() -> {
                    Toast.makeText(MainActivity.this, "配置验证失败，请检查网络连接", Toast.LENGTH_SHORT).show();
                    appendLog("扫码配置验证失败: " + e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    String responseBody = response.body().string();
                    Log.d(TAG, "扫码配置心跳返回: " + responseBody);
                    Log.d(TAG, "扫码配置HTTP状态码: " + response.code());
                    
                    handler.post(() -> {
                        if (tmp[0].contains("localhost")) {
                            Toast.makeText(MainActivity.this, 
                                "配置信息错误，本机调试请访问 本机局域网IP:8080(如192.168.1.101:8080) 获取配置信息进行配置!", 
                                Toast.LENGTH_LONG).show();
                            return;
                        }
                        
                        // 将扫描出的信息显示出来
                        txthost.setText(" 通知地址：" + tmp[0]);
                        txtkey.setText(" 通讯密钥：" + tmp[1]);
                        host = tmp[0];
                        key = tmp[1];

                        SharedPreferences.Editor editor = getSharedPreferences("vone", MODE_PRIVATE).edit();
                        editor.putString("host", host);
                        editor.putString("key", key);
                        editor.apply(); // 使用apply()替代commit()
                        
                        isOk = true;
                        Toast.makeText(MainActivity.this, "配置成功！", Toast.LENGTH_SHORT).show();
                        appendLog("扫码配置成功: " + tmp[0]);
                    });
                } catch (Exception e) {
                    Log.e(TAG, "扫码配置心跳异常: " + e.getMessage(), e);
                    handler.post(() -> {
                        Toast.makeText(MainActivity.this, "配置处理异常", Toast.LENGTH_SHORT).show();
                        appendLog("扫码配置处理异常: " + e.getMessage());
                    });
                }
            }
        });
    }

    private void appendLog(final String message) {
        // 在release版本中过滤启动画面相关日志
        if (!BuildConfig.DEBUG && shouldFilterLogMessage(message)) {
            return;
        }
        
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
                // 更新清除按钮状态
                updateClearButtonState();
            }
        });
    }
    
    /**
     * 判断是否应该过滤日志消息（仅在release版本中使用）
     * @param message 日志消息
     * @return true表示应该过滤掉，false表示应该显示
     */
    private boolean shouldFilterLogMessage(String message) {
        if (message == null) {
            return false;
        }
        
        // 启动画面相关的关键词，这些日志在release版本中不显示
        String[] splashScreenKeywords = {
            "启动画面",
            "诊断",
            "性能优化",
            "主题适配",
            "内存优化",
            "资源预加载",
            "SplashScreen",
            "启动画面测试",
            "配置检查",
            "兼容性",
            "Material You",
            "动态颜色"
        };
        
        String lowerMessage = message.toLowerCase();
        for (String keyword : splashScreenKeywords) {
            if (message.contains(keyword) || lowerMessage.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        
        // 保留的核心业务日志关键词
        String[] coreBusinessKeywords = {
            "心跳",
            "推送",
            "通知",
            "监听",
            "配置成功",
            "配置失败",
            "扫码",
            "权限",
            "电池",
            "服务",
            "网络",
            "连接",
            "支付宝",
            "微信",
            "到账",
            "匹配成功",
            "应用检测",
            "白名单",
            "兼容性检查",
            "Android版本"
        };
        
        // 如果包含核心业务关键词，则不过滤
        for (String keyword : coreBusinessKeywords) {
            if (message.contains(keyword) || lowerMessage.contains(keyword.toLowerCase())) {
                return false;
            }
        }
        
        // 默认情况下，在release版本中过滤掉大部分技术性日志
        return true;
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
        // 添加触觉反馈
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        // 检查相机权限
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            showPermissionExplanationDialog(
                "相机权限",
                "应用需要相机权限来扫描二维码。",
                new String[]{Manifest.permission.CAMERA},
                Constant.REQ_PERM_CAMERA
            );
            return;
        }
        
        // 检查存储权限
        if (!hasStoragePermission()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                showManageStoragePermissionDialog();
            } else {
                showPermissionExplanationDialog(
                    "存储权限",
                    "应用需要存储权限来保存扫码结果。",
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    Constant.REQ_PERM_EXTERNAL_STORAGE
                );
            }
            return;
        }
        
        try {
            // 启动二维码扫描
            Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
            // 添加启动标志，防止白屏问题
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            qrCodeLauncher.launch(intent);
        } catch (Exception e) {
            Log.e(TAG, "启动扫码界面失败: " + e.getMessage(), e);
            Toast.makeText(this, "启动扫码界面失败，请重试", Toast.LENGTH_SHORT).show();
        }
    }
 
   //手动配置
    public void doInput(View v) {
        // 添加触觉反馈
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        showConfigurationInputDialog();
    }
    
    /**
     * 显示配置输入对话框 - 使用Material 3设计
     */
    private void showConfigurationInputDialog() {
        // 创建Material 3风格的输入布局
        com.google.android.material.textfield.TextInputLayout inputLayout = 
            new com.google.android.material.textfield.TextInputLayout(this);
        inputLayout.setBoxBackgroundMode(com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE);
        inputLayout.setHint("请输入配置数据");
        inputLayout.setHelperText("格式：服务器地址/通讯密钥");
        inputLayout.setCounterEnabled(true);
        inputLayout.setCounterMaxLength(200);
        
        // 创建EditText
        com.google.android.material.textfield.TextInputEditText inputServer = 
            new com.google.android.material.textfield.TextInputEditText(this);
        inputServer.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        inputServer.setSingleLine(true);
        
        // 设置布局参数
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 
            LinearLayout.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(48, 24, 48, 24);
        inputLayout.setLayoutParams(layoutParams);
        
        // 将EditText添加到TextInputLayout
        inputLayout.addView(inputServer);
        
        // 创建Material 3风格的对话框
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("手动配置")
               .setMessage("请输入从网站获取的配置数据")
               .setView(inputLayout)
               .setNegativeButton("取消", null);
        
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                String scanResult = inputServer.getText().toString().trim();
                
                // 输入验证
                if (scanResult.isEmpty()) {
                    inputLayout.setError("配置数据不能为空");
                    return;
                }
                
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
                editor.apply(); // 使用apply()替代commit()

            }
        });
        builder.show();

    }

    //检测心跳
    public void doStart(View view) {
        // 添加触觉反馈
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
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
        // 添加触觉反馈
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        logTextView.setText("");
        // 更新清除按钮状态
        updateClearButtonState();
    }

    /**
     * 更新清除按钮的状态
     */
    private void updateClearButtonState() {
        if (btnClearLogsInline != null && logTextView != null) {
            // 检查日志内容是否为空
            boolean hasContent = logTextView.getText().toString().trim().length() > 0;
            // 根据日志内容调整按钮的透明度，但保持可点击
            btnClearLogsInline.setAlpha(hasContent ? 1.0f : 0.6f);
        }
    }

    /**
     * 主题切换功能
     */
    public void toggleTheme(View view) {
        // 显示主题选择对话框
        showThemeSelectionDialog();
    }

    /**
     * 显示主题选择对话框
     */
    private void showThemeSelectionDialog() {
        String[] themeOptions = {"跟随系统", "浅色模式", "深色模式"};
        int currentThemeMode = ThemeUtils.getSavedThemeMode(this);
        
        new AlertDialog.Builder(this)
            .setTitle("选择主题模式")
            .setSingleChoiceItems(themeOptions, currentThemeMode, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (which != currentThemeMode) {
                        // 应用新的主题模式
                        ThemeUtils.setThemeMode(MainActivity.this, which);
                        
                        // 显示提示信息
                        String themeName = themeOptions[which];
                        Toast.makeText(MainActivity.this, "已切换到" + themeName, Toast.LENGTH_SHORT).show();
                        
                        // 重新创建Activity以应用主题变化
                        recreate();
                    }
                    dialog.dismiss();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    /**
     * 显示动态颜色设置对话框
     */
    public void toggleDynamicColor(View view) {
        if (!ThemeUtils.isDynamicColorSupported()) {
            Toast.makeText(this, "您的设备不支持动态颜色功能（需要Android 12+）", Toast.LENGTH_LONG).show();
            return;
        }
        
        boolean currentEnabled = ThemeUtils.isDynamicColorEnabled(this);
        String message = currentEnabled ? 
            "当前已启用动态颜色，是否关闭？\n\n动态颜色会根据您的壁纸自动调整应用颜色主题。" :
            "当前已关闭动态颜色，是否启用？\n\n动态颜色会根据您的壁纸自动调整应用颜色主题。";
        
        new AlertDialog.Builder(this)
            .setTitle("动态颜色设置")
            .setMessage(message)
            .setPositiveButton(currentEnabled ? "关闭" : "启用", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ThemeUtils.setDynamicColorEnabled(MainActivity.this, !currentEnabled);
                    String statusText = currentEnabled ? "已关闭" : "已启用";
                    Toast.makeText(MainActivity.this, statusText + "动态颜色", Toast.LENGTH_SHORT).show();
                    
                    // 重新创建Activity以应用变化
                    recreate();
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    public void checkPush(View v) {
        // 添加触觉反馈
        v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
        
        // 添加日志记录
        appendLog("发送测试通知，检测监听权限...");
        
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

        mNotificationManager.notify(id++, mNotification);
    }

    /**
     * 检查并请求必要的权限
     */
    private void checkAndRequestPermissions() {
        // 检查通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                showPermissionExplanationDialog(
                    "通知权限",
                    "应用需要通知权限来显示重要信息和状态更新。",
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    Constant.REQ_PERM_POST_NOTIFICATIONS
                );
                return;
            }
        }

        // 检查文件访问权限
        checkStoragePermissions();
    }

    /**
     * 检查存储权限
     */
    private void checkStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 检查所有文件访问权限
            if (!Environment.isExternalStorageManager()) {
                showManageStoragePermissionDialog();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // Android 6-10 检查传统存储权限
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != PackageManager.PERMISSION_GRANTED) {
                showPermissionExplanationDialog(
                    "存储权限",
                    "应用需要存储权限来保存和读取配置文件。",
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                    Constant.REQ_PERM_EXTERNAL_STORAGE
                );
            }
        }
    }

    /**
     * 显示权限说明对话框 - 使用改进的用户引导
     */
    private void showPermissionExplanationDialog(String title, String message, String[] permissions, int requestCode) {
        // 使用Material 3风格的对话框和更好的用户引导
        com.google.android.material.dialog.MaterialAlertDialogBuilder builder = 
            new com.google.android.material.dialog.MaterialAlertDialogBuilder(this);
        builder.setTitle("需要" + title)
               .setMessage(message + "\n\n这个权限对应用的正常运行非常重要。")
               .setPositiveButton("授权", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       ActivityCompat.requestPermissions(MainActivity.this, permissions, requestCode);
                   }
               })
               .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       // 使用用户引导助手显示权限被拒绝的处理
                       userGuideHelper.showPermissionDeniedGuide(title, message);
                   }
               })
               .setNeutralButton("了解更多", new DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(DialogInterface dialog, int which) {
                       userGuideHelper.showDetailedPermissionGuide();
                   }
               })
               .setCancelable(false)
               .show();
    }

    /**
     * 显示管理所有文件权限对话框 (Android 11+)
     */
    private void showManageStoragePermissionDialog() {
        new AlertDialog.Builder(this)
            .setTitle("需要文件管理权限")
            .setMessage("为了正常使用扫码功能，应用需要访问设备存储。请在设置中允许访问所有文件。")
            .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    try {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        manageStoragePermissionLauncher.launch(intent);
                    } catch (Exception e) {
                        // 如果无法打开特定设置页面，打开通用的应用设置页面
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        Toast.makeText(MainActivity.this, "请在权限设置中允许访问所有文件", Toast.LENGTH_LONG).show();
                    }
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "文件权限被拒绝，扫码功能可能无法正常使用", Toast.LENGTH_LONG).show();
                }
            })
            .setCancelable(false)
            .show();
    }

    /**
     * 检查是否有存储权限
     */
    private boolean hasStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return Environment.isExternalStorageManager();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    //各种权限的判断
    private void toggleNotificationListenerService(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(context, NeNotificationService2.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

        pm.setComponentEnabledSetting(new ComponentName(context, NeNotificationService2.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
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
        
        // 处理管理所有文件权限设置返回
        if (requestCode == Constant.REQ_PERM_MANAGE_EXTERNAL_STORAGE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Toast.makeText(this, "文件管理权限已授予", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "文件管理权限未授予，扫码功能可能无法正常使用", Toast.LENGTH_LONG).show();
                }
            }
            return;
        }
        
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
            editor.apply(); // 使用apply()替代commit()
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case Constant.REQ_PERM_CAMERA:
                // 摄像头权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "相机权限已授予", Toast.LENGTH_SHORT).show();
                    // 重新检查扫码所需的所有权限
                    startQrCode(null);
                } else {
                    Toast.makeText(this, "相机权限被拒绝，无法使用扫码功能", Toast.LENGTH_LONG).show();
                }
                break;
                
            case Constant.REQ_PERM_EXTERNAL_STORAGE:
                // 文件读写权限申请
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "存储权限已授予", Toast.LENGTH_SHORT).show();
                    // 重新检查扫码所需的所有权限
                    startQrCode(null);
                } else {
                    Toast.makeText(this, "存储权限被拒绝，扫码功能可能无法正常使用", Toast.LENGTH_LONG).show();
                }
                break;
                
            case Constant.REQ_PERM_POST_NOTIFICATIONS:
                // 通知权限申请 (Android 13+)
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "通知权限已授予", Toast.LENGTH_SHORT).show();
                    // 继续检查其他权限
                    checkStoragePermissions();
                } else {
                    Toast.makeText(this, "通知权限被拒绝，可能无法正常显示通知", Toast.LENGTH_LONG).show();
                    // 即使通知权限被拒绝，也继续检查存储权限
                    checkStoragePermissions();
                }
                break;
        }
    }

    // 实现ThemeChangeListener接口
    @Override
    public void onThemeChanged(android.content.res.Configuration newConfig) {
        // 当系统主题发生变化时，重新应用主题
        ThemeUtils.applyThemeToActivity(this);
        
        // 可以在这里添加其他主题变化时的处理逻辑
        Log.d(TAG, "主题已变化，重新应用主题设置");
    }

    @Override
    public void onDynamicColorChanged(boolean enabled) {
        // 当动态颜色设置发生变化时的处理
        Log.d(TAG, "动态颜色设置已变化: " + enabled);
        
        // 重新应用主题
        ThemeUtils.applyThemeToActivity(this);
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        
        // 检查是否是主题相关的配置变化
        if ((newConfig.uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) != 
            (getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK)) {
            // 主题模式发生了变化
            onThemeChanged(newConfig);
        }
    }
    
    /**
     * 初始化启动画面诊断功能
     */
    private void initializeSplashScreenDiagnostics() {
        try {
            Log.d(TAG, "初始化启动画面诊断功能");
            
            // 记录启动画面开始
            SplashScreenLogger.logStartup("启动画面诊断初始化开始");
            
            // 创建错误处理器（release版本也需要）
            splashScreenErrorHandler = new SplashScreenErrorHandler(this);
            
            // 创建主题适配器
            splashScreenThemeAdapter = new SplashScreenThemeAdapter(this);
            splashScreenThemeAdapter.applySplashScreenThemeAdaptation(this);
            
            // 创建性能优化器并开始优化
            performanceOptimizer = new SplashScreenPerformanceOptimizer(this);
            performanceOptimizer.startOptimization(this);
            
            // 初始化传统启动画面辅助类
            if (LegacySplashScreenHelper.needsLegacyHandling()) {
                legacySplashScreenHelper = new LegacySplashScreenHelper(this);
                legacySplashScreenHelper.setupLegacySplashScreen();
                SplashScreenLogger.logStartup("传统启动画面辅助类初始化完成");
            }
            
            // 详细诊断功能仅在debug版本中启用
            if (BuildConfig.DEBUG) {
                // 创建诊断工具
                splashScreenDiagnostic = new SplashScreenDiagnosticImpl(this);
                
                // 记录设备信息
                SplashScreenSupport support = splashScreenDiagnostic.getSplashScreenSupport();
                SplashScreenLogger.logDeviceInfo(support);
                
                // 记录主题适配信息
                SplashScreenLogger.logInfo(splashScreenThemeAdapter.getThemeAdaptationSummary());
                
                // 执行快速诊断检查
                performQuickDiagnosticCheck();
            }
            
            SplashScreenLogger.logStartup("启动画面诊断功能初始化完成");
            Log.d(TAG, "启动画面诊断功能初始化完成");
            
        } catch (Exception e) {
            Log.e(TAG, "启动画面诊断初始化失败: " + e.getMessage(), e);
            SplashScreenLogger.logError("启动画面诊断初始化失败", e);
            
            // 即使诊断失败，也不应该影响应用正常启动
            if (splashScreenErrorHandler != null) {
                splashScreenErrorHandler.handleGeneralError(e, "诊断初始化");
            }
        }
    }
    
    /**
     * 执行快速诊断检查
     */
    private void performQuickDiagnosticCheck() {
        try {
            if (splashScreenDiagnostic == null) return;
            
            // 验证资源完整性
            boolean resourcesValid = splashScreenDiagnostic.validateSplashScreenResources();
            if (!resourcesValid) {
                Log.w(TAG, "启动画面资源验证失败");
                appendLog("启动画面资源验证失败，可能影响启动体验");
                
                if (splashScreenErrorHandler != null) {
                    splashScreenErrorHandler.handleResourceError(
                        new android.content.res.Resources.NotFoundException("启动画面资源验证失败")
                    );
                }
            }
            
            // 获取支持信息
            SplashScreenSupport support = splashScreenDiagnostic.getSplashScreenSupport();
            Log.d(TAG, "启动画面支持: " + support.getSupportedType().getDescription());
            
        } catch (Exception e) {
            Log.w(TAG, "快速诊断检查失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行启动画面诊断后处理
     */
    private void performPostSplashScreenDiagnostics() {
        try {
            if (legacySplashScreenHelper != null) {
                // 对于传统启动画面，执行过渡动画
                int duration = LegacySplashScreenHelper.getRecommendedSplashDuration(this);
                legacySplashScreenHelper.performSplashTransition(duration, () -> {
                    Log.d(TAG, "传统启动画面过渡完成");
                });
            }
            
            // 在后台执行完整的诊断检查
            new Thread(() -> {
                try {
                    performCompleteDiagnosticCheck();
                } catch (Exception e) {
                    Log.w(TAG, "后台诊断检查失败: " + e.getMessage());
                }
            }).start();
            
        } catch (Exception e) {
            Log.w(TAG, "启动画面诊断后处理失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行完整的诊断检查（后台执行）
     */
    private void performCompleteDiagnosticCheck() {
        try {
            if (splashScreenDiagnostic == null) return;
            
            Log.d(TAG, "开始完整启动画面诊断检查");
            
            // 执行完整配置检查
            DiagnosticResult result = splashScreenDiagnostic.checkSplashScreenConfiguration();
            
            // 记录诊断结果
            Log.d(TAG, "诊断结果: " + result.toString());
            
            if (result.hasIssues()) {
                // 在主线程中更新UI
                handler.post(() -> {
                    String message = "发现启动画面配置问题 " + result.getIssueCount() + " 个";
                    appendLog(message);
                    
                    // 显示问题详情
                    for (String issue : result.getIssues()) {
                        appendLog("问题: " + issue);
                    }
                    for (String recommendation : result.getRecommendations()) {
                        appendLog("建议: " + recommendation);
                    }
                });
                
                // 尝试自动修复一些问题
                attemptAutoFix(result);
            } else {
                handler.post(() -> {
                    appendLog("启动画面配置检查通过");
                });
            }
            
            Log.d(TAG, "完整启动画面诊断检查完成");
            
        } catch (Exception e) {
            Log.e(TAG, "完整诊断检查异常: " + e.getMessage(), e);
            handler.post(() -> {
                appendLog("启动画面诊断检查异常: " + e.getMessage());
            });
        }
    }
    
    /**
     * 尝试自动修复启动画面问题
     */
    private void attemptAutoFix(DiagnosticResult result) {
        try {
            if (splashScreenErrorHandler == null) return;
            
            Log.d(TAG, "尝试自动修复启动画面问题");
            
            for (String issue : result.getIssues()) {
                if (issue.contains("资源")) {
                    splashScreenErrorHandler.handleResourceError(
                        new android.content.res.Resources.NotFoundException(issue)
                    );
                } else if (issue.contains("主题") || issue.contains("配置")) {
                    splashScreenErrorHandler.handleThemeError(
                        new RuntimeException(issue)
                    );
                } else if (issue.contains("兼容性")) {
                    splashScreenErrorHandler.handleCompatibilityError(
                        new UnsupportedOperationException(issue)
                    );
                }
            }
            
            // 记录修复尝试
            handler.post(() -> {
                appendLog("已尝试自动修复启动画面问题");
            });
            
        } catch (Exception e) {
            Log.w(TAG, "自动修复失败: " + e.getMessage());
        }
    }
    
    /**
     * 手动触发启动画面诊断
     * 可以通过UI按钮调用（详细诊断仅在debug版本中可用）
     */
    public void triggerSplashScreenDiagnostic(View view) {
        try {
            // 添加触觉反馈
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            
            if (!BuildConfig.DEBUG) {
                appendLog("详细诊断功能仅在debug版本中可用");
                appendLog("Release版本仅提供基础启动画面优化");
                return;
            }
            
            appendLog("开始手动启动画面诊断...");
            
            new Thread(() -> {
                try {
                    if (splashScreenDiagnostic != null) {
                        DiagnosticResult result = splashScreenDiagnostic.checkSplashScreenConfiguration();
                        SplashScreenSupport support = splashScreenDiagnostic.getSplashScreenSupport();
                        
                        handler.post(() -> {
                            // 显示诊断结果
                            appendLog("=== 启动画面诊断结果 ===");
                            appendLog("设备支持: " + support.getSupportedType().getDescription());
                            appendLog("配置有效: " + (result.isConfigurationValid() ? "是" : "否"));
                            appendLog("资源有效: " + (result.areResourcesValid() ? "是" : "否"));
                            appendLog("问题数量: " + result.getIssueCount());
                            
                            if (result.hasIssues()) {
                                appendLog("--- 发现的问题 ---");
                                for (String issue : result.getIssues()) {
                                    appendLog("• " + issue);
                                }
                                
                                appendLog("--- 修复建议 ---");
                                for (String recommendation : result.getRecommendations()) {
                                    appendLog("• " + recommendation);
                                }
                            }
                            
                            appendLog("=== 诊断完成 ===");
                        });
                    }
                } catch (Exception e) {
                    handler.post(() -> {
                        appendLog("手动诊断失败: " + e.getMessage());
                    });
                }
            }).start();
            
        } catch (Exception e) {
            Log.e(TAG, "触发手动诊断失败: " + e.getMessage(), e);
            appendLog("触发诊断失败: " + e.getMessage());
        }
    }
    
    /**
     * 启动启动画面测试工具
     * 可以通过UI按钮调用（仅在debug版本中可用）
     */
    public void launchSplashScreenTestTool(View view) {
        if (!BuildConfig.DEBUG) {
            appendLog("测试工具仅在debug版本中可用");
            return;
        }
        
        try {
            // 添加触觉反馈
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            
            appendLog("启动启动画面测试工具...");
            
            Intent intent = new Intent(this, SplashScreenTestActivity.class);
            startActivity(intent);
            
        } catch (Exception e) {
            Log.e(TAG, "启动测试工具失败: " + e.getMessage(), e);
            appendLog("启动测试工具失败: " + e.getMessage());
        }
    }
    
}