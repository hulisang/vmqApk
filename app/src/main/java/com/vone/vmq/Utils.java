package com.vone.vmq;

import static android.content.Context.NOTIFICATION_SERVICE;
import static android.content.Context.POWER_SERVICE;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import com.vone.qrcode.R;
import com.vone.vmq.util.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

class Utils {
    public final static String GET_MESSAGE_KEY = "get_message_key";
    public static final String GET_SHOW_ACTIVITY_TYPE = "get_show_activity_type";

    private final static String dayType = "yyyy-MM-dd HH:mm:ss";
    private final static String hourType = "HH:mm:ss";
    private static int notifyDay = -1;
    private static OkHttpClient okHttpClient;

    public static OkHttpClient getOkHttpClient() {
        if (okHttpClient == null) {
            synchronized (Utils.class) {
                if (okHttpClient == null) {
                    okHttpClient = new OkHttpClient.Builder()
                            .connectTimeout(15, TimeUnit.SECONDS)
                            .readTimeout(30, TimeUnit.SECONDS)
                            .writeTimeout(30, TimeUnit.SECONDS)
                            .callTimeout(60, TimeUnit.SECONDS)
                            .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                            .retryOnConnectionFailure(true)
                            .addInterceptor(new RetryInterceptor())
                            .addInterceptor(new LoggingInterceptor())
                            .build();
                }
            }
        }
        return okHttpClient;
    }

    static void putStr(Context context, String value) {
        if (context == null) {
            return;
        }
        File notifycationFilePath = context.getExternalFilesDir("log");
        if (notifycationFilePath == null || !canWrite(notifycationFilePath)) return;

        String notifycationFileName = "notifycation_file.txt";
        File file = new File(notifycationFilePath, notifycationFileName);
        // 为了防止文件无限增大，只保留当天的数据
        if (Calendar.getInstance().get(Calendar.DAY_OF_MONTH) != notifyDay) {
            if (file.exists() && file.canWrite()) {
                // 设置最大容量 1M 大小
                if (file.length() > 1024 * 1024) {
                    FileUtils fileUtils = new FileUtils();
                    if (fileUtils.deleteFileSafely(file)) {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            file.createNewFile();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            notifyDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        }

        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file, true), 1024);
            out.write(value);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static boolean canWrite(File notifycationFilePath) {
        return notifycationFilePath.canWrite();
    }


    static boolean checkBatteryWhiteList(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(POWER_SERVICE);
            if (powerManager == null) return true;
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return true;
    }

    static void gotoBatterySetting(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            context.startActivity(intent);
        }
    }

    static String formatTime(Date time) {
        DateFormat dataFormat = new SimpleDateFormat(dayType, Locale.getDefault());
        return dataFormat.format(time);
    }

    static String formatTimeSimple(Date time) {
        DateFormat dataFormat = new SimpleDateFormat(hourType, Locale.getDefault());
        return dataFormat.format(time);
    }

    static void createNotificationChannel(Context context, String channelId, CharSequence channelName, int importance) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelId, channelName, importance);

            channel.enableLights(true);
            channel.setShowBadge(true);
            channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
            NotificationManager notificationManager = (NotificationManager) context
                    .getSystemService(NOTIFICATION_SERVICE);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    static void sendNotifyMessage(Context context, String title, String text, int type) {
        if (type == 1) {
            String channelId = "MessageNotify";
            int channelLevel = -1;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channelLevel = NotificationManager.IMPORTANCE_HIGH;
            }
            int id = (channelId + System.currentTimeMillis()).hashCode();
            String showTvText = String.format("%s\n%s", title, text);
            Intent intent = new Intent(context, MainActivity.class);
            intent.putExtra(GET_MESSAGE_KEY, showTvText);
            intent.putExtra(GET_SHOW_ACTIVITY_TYPE, type);

            sendNotify(context, channelId, "remote message notify",
                    channelLevel, Notification.PRIORITY_HIGH, id,
                    title, text, intent);
        }
    }

    static void sendBatteryNotify(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            String channelId = "BatteryNotify";
            int channelLevel = -1;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channelLevel = NotificationManager.IMPORTANCE_MAX;
            }
            @SuppressLint("BatteryLife")
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            int id = (channelId + System.currentTimeMillis()).hashCode();
            sendNotify(context, channelId, "battery white list notify",
                    channelLevel, Notification.PRIORITY_MAX, id,
                    context.getString(R.string.app_name), context.getString(R.string.click_add_to_battery_white_list),
                    intent);
        }
    }

    private static void sendNotify(Context context, String channelId, String channelName,
                                   int channelLevel, int priority, int id,
                                   String title, String text, Intent intent) {
        NotificationManager manager = (NotificationManager) context.
                getSystemService(NOTIFICATION_SERVICE);
        if (manager == null) return;
        Notification.Builder notificationBuild;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (channelLevel < 0) {
                channelLevel = NotificationManager.IMPORTANCE_HIGH;
            }
            Utils.createNotificationChannel(context, channelId
                    , channelName
                    , channelLevel);
            notificationBuild = new Notification.Builder(context, channelId);
        } else {
            notificationBuild = new Notification.Builder(context);
        }
        notificationBuild.setContentTitle(title)
                .setContentText(text)
                .setStyle(new Notification.BigTextStyle()
                        .setBigContentTitle(title)
                        .bigText(text))
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(priority)
                .setAutoCancel(true);
        int flags = PendingIntent.FLAG_CANCEL_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, id, intent, flags);
        notificationBuild.setContentIntent(pendingIntent);
        // 正式发出通知
        manager.notify(id, notificationBuild.build());
    }

    /**
     * 发送日志广播
     * 用于将日志消息发送到MainActivity显示
     */
    public static void sendLogBroadcast(Context context, String message) {
        if (context == null || message == null) {
            return;
        }
        
        try {
            Intent intent = new Intent(NeNotificationService2.ACTION_LOG_UPDATE);
            intent.putExtra("log_message", message);
            context.sendBroadcast(intent);
        } catch (Exception e) {
            android.util.Log.w("Utils", "发送日志广播失败: " + e.getMessage());
        }
    }

    /**
     * 重试拦截器 - 适配Android 14+的网络请求重试机制
     */
    private static class RetryInterceptor implements Interceptor {
        private static final int MAX_RETRY_COUNT = 3;
        private static final String TAG = "RetryInterceptor";

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            Response response = null;
            IOException lastException = null;
            
            for (int i = 0; i <= MAX_RETRY_COUNT; i++) {
                try {
                    response = chain.proceed(request);
                    
                    // 如果响应成功，直接返回
                    if (response.isSuccessful()) {
                        return response;
                    }
                    
                    // 对于服务器错误（5xx）进行重试
                    if (response.code() >= 500 && i < MAX_RETRY_COUNT) {
                        android.util.Log.w(TAG, "服务器错误 " + response.code() + "，第 " + (i + 1) + " 次重试");
                        response.close();
                        
                        // 指数退避策略
                        try {
                            Thread.sleep((long) Math.pow(2, i) * 1000);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new IOException("重试被中断", e);
                        }
                        continue;
                    }
                    
                    return response;
                    
                } catch (IOException e) {
                    lastException = e;
                    android.util.Log.w(TAG, "网络请求失败，第 " + (i + 1) + " 次重试: " + e.getMessage());
                    
                    if (response != null) {
                        response.close();
                    }
                    
                    // 最后一次重试失败，抛出异常
                    if (i == MAX_RETRY_COUNT) {
                        throw e;
                    }
                    
                    // 指数退避策略
                    try {
                        Thread.sleep((long) Math.pow(2, i) * 1000);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("重试被中断", ie);
                    }
                }
            }
            
            if (lastException != null) {
                throw lastException;
            }
            
            return response;
        }
    }

    /**
     * 日志拦截器 - 记录网络请求详情，便于调试
     */
    private static class LoggingInterceptor implements Interceptor {
        private static final String TAG = "NetworkRequest";

        @Override
        public Response intercept(Chain chain) throws IOException {
            Request request = chain.request();
            long startTime = System.currentTimeMillis();
            
            android.util.Log.d(TAG, "发送请求: " + request.url());
            android.util.Log.d(TAG, "请求方法: " + request.method());
            
            Response response;
            boolean success = false;
            try {
                response = chain.proceed(request);
                success = response.isSuccessful();
            } catch (Exception e) {
                android.util.Log.e(TAG, "请求失败: " + request.url() + " - " + e.getMessage());
                throw e;
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            android.util.Log.d(TAG, "收到响应: " + response.code() + " " + response.message() + 
                    " (" + duration + "ms)");
            
            // 集成性能监控 - 记录网络请求性能
            // 注意：这里需要Context，但在静态拦截器中无法直接获取
            // 在实际使用中，可以通过其他方式传递Context或使用Application Context
            
            return response;
        }
    }
}
