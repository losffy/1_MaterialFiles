/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.monitor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import java8.nio.file.Path
import java8.nio.file.Paths
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.zhanghai.android.files.R
import me.zhanghai.android.files.app.NotificationIds
import me.zhanghai.android.files.filejob.FileJobService
import me.zhanghai.android.files.storage.transfer.config.TransferConfigManager
import me.zhanghai.android.files.storage.transfer.job.AutoClassifyJob
import me.zhanghai.android.files.util.startForegroundService
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 文件监听服务
 * 监控配置的目录并触发自动分类任务
 */
class FileMonitorService : Service() {
    
    private val observers = ConcurrentHashMap<String, FileChangeObserver>()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    
    private val configManager = TransferConfigManager.getInstance()
    
    override fun onCreate() {
        super.onCreate()
        
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        // 获取唤醒锁以保持服务运行
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "MaterialFiles:FileMonitorWakeLock"
        ).apply {
            acquire(WAKE_LOCK_TIMEOUT)
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_MONITORING -> startMonitoring()
            ACTION_STOP_MONITORING -> stopMonitoring()
            ACTION_MANUAL_ORGANIZE -> manualOrganize()
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        stopMonitoring()
        serviceScope.cancel()
        wakeLock?.release()
        super.onDestroy()
    }
    
    /**
     * 开始监控
     */
    private fun startMonitoring() {
        try {
            // 加载配置
            configManager.loadConfig()
            
            // 停止现有观察者
            stopObservers()
            
            // 设置新的观察者
            setupObservers()
            
            // 更新通知
            updateNotification("正在监控文件变化")
        } catch (e: Exception) {
            updateNotification("监控启动失败: ${e.message}")
            stopSelf()
        }
    }
    
    /**
     * 停止监控
     */
    private fun stopMonitoring() {
        stopObservers()
        stopForeground(true)
        stopSelf()
    }
    
    /**
     * 手动整理
     */
    private fun manualOrganize() {
        serviceScope.launch {
            try {
                // 加载配置
                configManager.loadConfig()
                
                // 确保分类目录存在
                configManager.ensureClassifyDirectoryExists()
                
                // 更新通知
                updateNotification("正在整理文件...")
                
                // 处理所有监听目录中的文件
                val config = configManager.currentConfig
                config.listenDirectories.forEach { (category, paths) ->
                    paths.forEach { pathStr ->
                        val expandedPath = expandPathVariables(pathStr)
                        val path = Paths.get(expandedPath)
                        val file = File(path.toString())
                        
                        if (file.exists() && file.isDirectory) {
                            file.listFiles()?.forEach { childFile ->
                                if (childFile.isFile) {
                                    // 为每个文件创建自动分类任务
                                    val childPath = Paths.get(childFile.absolutePath)
                                    val job = AutoClassifyJob(childPath, config)
                                    FileJobService.run(job)
                                    
                                    // 避免过快处理导致系统负担
                                    delay(100)
                                }
                            }
                        }
                    }
                }
                
                // 更新通知
                updateNotification("文件整理完成")
                
                // 延迟后恢复监控状态通知
                delay(3000)
                updateNotification("正在监控文件变化")
            } catch (e: Exception) {
                updateNotification("文件整理失败: ${e.message}")
            }
        }
    }
    
    /**
     * 设置文件观察者
     */
    private fun setupObservers() {
        val config = configManager.currentConfig
        
        // 为每个监听目录创建观察者
        config.listenDirectories.forEach { (category, paths) ->
            paths.forEach { pathStr ->
                val expandedPath = expandPathVariables(pathStr)
                val path = Paths.get(expandedPath)
                val file = File(path.toString())
                
                if (file.exists() && file.isDirectory) {
                    val observer = FileChangeObserver(
                        path,
                        config.recList.contains(pathStr)
                    ) { event, filePath ->
                        if (filePath != null) {
                            handleFileChange(Paths.get(filePath))
                        }
                    }
                    
                    observers[path.toString()] = observer
                    observer.startWatching()
                }
            }
        }
    }
    
    /**
     * 停止所有观察者
     */
    private fun stopObservers() {
        observers.values.forEach { it.stopWatching() }
        observers.clear()
    }
    
    /**
     * 处理文件变化
     */
    private fun handleFileChange(path: Path) {
        val file = File(path.toString())
        if (!file.isFile) {
            return
        }
        
        // 延迟处理，避免文件尚未完全写入
        serviceScope.launch {
            val config = configManager.currentConfig
            delay(config.delay * 1000L)
            
            // 创建自动分类任务
            val job = AutoClassifyJob(path, config)
            FileJobService.run(job)
        }
    }
    
    /**
     * 展开路径变量
     */
    private fun expandPathVariables(path: String): String {
        return path
            .replace("{storage}", "/storage/emulated/0")
            .replace("{androidData}", "/storage/emulated/0/Android/data")
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.file_job_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.file_job_notification_channel_description)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * 创建通知
     */
    private fun createNotification(): Notification {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("定向存储传输服务正在运行")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    companion object {
        private const val CHANNEL_ID = "file_monitor_service"
        private const val NOTIFICATION_ID = NotificationIds.FILE_MONITOR_SERVICE
        private const val WAKE_LOCK_TIMEOUT = 10 * 60 * 1000L // 10分钟
        
        private const val ACTION_START_MONITORING = "me.zhanghai.android.files.action.START_MONITORING"
        private const val ACTION_STOP_MONITORING = "me.zhanghai.android.files.action.STOP_MONITORING"
        private const val ACTION_MANUAL_ORGANIZE = "me.zhanghai.android.files.action.MANUAL_ORGANIZE"
        
        /**
         * 启动监控服务
         */
        fun startMonitoring(context: Context) {
            val intent = Intent(context, FileMonitorService::class.java).apply {
                action = ACTION_START_MONITORING
            }
            context.startForegroundService(intent)
        }
        
        /**
         * 停止监控服务
         */
        fun stopMonitoring(context: Context) {
            val intent = Intent(context, FileMonitorService::class.java).apply {
                action = ACTION_STOP_MONITORING
            }
            context.startService(intent)
        }
        
        /**
         * 手动触发整理
         */
        fun manualOrganize(context: Context) {
            val intent = Intent(context, FileMonitorService::class.java).apply {
                action = ACTION_MANUAL_ORGANIZE
            }
            context.startForegroundService(intent)
        }
    }
}
