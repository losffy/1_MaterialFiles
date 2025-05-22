/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer

import android.content.Context
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.storage.transfer.config.TransferConfigManager
import me.zhanghai.android.files.storage.transfer.monitor.FileMonitorService

/**
 * 定向存储传输管理器
 * 负责协调配置管理、文件监听和任务调度
 */
class TransferManager private constructor() {
    
    private val configManager = TransferConfigManager.getInstance()
    
    /**
     * 初始化传输管理器
     */
    fun initialize() {
        try {
            // 加载配置
            configManager.loadConfig()
            
            // 如果启用了定向存储传输，启动监控服务
            if (configManager.isTransferEnabled && configManager.isBackgroundMonitorEnabled) {
                startMonitoring()
            }
            
            // 如果启用了应用启动时自动整理，触发整理
            if (configManager.isTransferEnabled && configManager.isAutoOrganizeOnStartup) {
                organizeFiles()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 启动文件监控
     */
    fun startMonitoring() {
        if (configManager.isTransferEnabled && configManager.isBackgroundMonitorEnabled) {
            FileMonitorService.startMonitoring(application)
        }
    }
    
    /**
     * 停止文件监控
     */
    fun stopMonitoring() {
        FileMonitorService.stopMonitoring(application)
    }
    
    /**
     * 手动触发文件整理
     */
    fun organizeFiles() {
        if (configManager.isTransferEnabled) {
            FileMonitorService.manualOrganize(application)
        }
    }
    
    /**
     * 启用定向存储传输
     */
    fun enableTransfer(enable: Boolean) {
        configManager.isTransferEnabled = enable
        
        if (enable && configManager.isBackgroundMonitorEnabled) {
            startMonitoring()
        } else if (!enable) {
            stopMonitoring()
        }
    }
    
    /**
     * 启用后台监听
     */
    fun enableBackgroundMonitor(enable: Boolean) {
        configManager.isBackgroundMonitorEnabled = enable
        
        if (configManager.isTransferEnabled) {
            if (enable) {
                startMonitoring()
            } else {
                stopMonitoring()
            }
        }
    }
    
    /**
     * 启用应用启动时自动整理
     */
    fun enableAutoOrganizeOnStartup(enable: Boolean) {
        configManager.isAutoOrganizeOnStartup = enable
    }
    
    companion object {
        @Volatile
        private var instance: TransferManager? = null
        
        fun getInstance(): TransferManager {
            return instance ?: synchronized(this) {
                instance ?: TransferManager().also { instance = it }
            }
        }
        
        /**
         * 在应用启动时初始化
         */
        fun init(context: Context) {
            getInstance().initialize()
        }
    }
}
