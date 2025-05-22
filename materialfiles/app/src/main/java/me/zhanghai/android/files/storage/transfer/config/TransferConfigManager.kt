/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.config

import android.content.Context
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.app.application
import me.zhanghai.android.files.provider.common.createDirectories
import me.zhanghai.android.files.provider.common.exists
import me.zhanghai.android.files.provider.common.isDirectory
import java.io.IOException

/**
 * 定向存储传输配置管理器
 * 负责配置的加载、保存和管理
 */
class TransferConfigManager private constructor() {
    
    private val parser = TransferConfigParser()
    private val preferences: SharedPreferences = PreferenceManager.getDefaultSharedPreferences(application)
    
    // 当前配置
    var currentConfig: TransferConfig = TransferConfig.createDefault()
        private set
    
    // 配置文件路径
    val configFilePath: Path
        get() = Paths.get(
            preferences.getString(KEY_CONFIG_PATH, DEFAULT_CONFIG_PATH) ?: DEFAULT_CONFIG_PATH
        )
    
    // 是否启用定向存储传输
    var isTransferEnabled: Boolean
        get() = preferences.getBoolean(KEY_TRANSFER_ENABLED, false)
        set(value) {
            preferences.edit().putBoolean(KEY_TRANSFER_ENABLED, value).apply()
        }
    
    // 是否启用后台监听
    var isBackgroundMonitorEnabled: Boolean
        get() = preferences.getBoolean(KEY_BACKGROUND_MONITOR, true)
        set(value) {
            preferences.edit().putBoolean(KEY_BACKGROUND_MONITOR, value).apply()
        }
    
    // 是否在应用启动时自动整理
    var isAutoOrganizeOnStartup: Boolean
        get() = preferences.getBoolean(KEY_AUTO_ORGANIZE_ON_STARTUP, false)
        set(value) {
            preferences.edit().putBoolean(KEY_AUTO_ORGANIZE_ON_STARTUP, value).apply()
        }
    
    /**
     * 加载配置
     */
    @Throws(IOException::class)
    fun loadConfig() {
        val path = configFilePath
        if (path.exists()) {
            try {
                currentConfig = parser.readFromFile(path)
            } catch (e: Exception) {
                throw IOException("Failed to load config: ${e.message}", e)
            }
        } else {
            // 如果配置文件不存在，使用默认配置并保存
            currentConfig = TransferConfig.createDefault()
            saveConfig()
        }
    }
    
    /**
     * 保存配置
     */
    @Throws(IOException::class)
    fun saveConfig() {
        val path = configFilePath
        // 确保父目录存在
        path.parent?.createDirectories()
        try {
            parser.writeToFile(currentConfig, path)
        } catch (e: Exception) {
            throw IOException("Failed to save config: ${e.message}", e)
        }
    }
    
    /**
     * 更新配置
     */
    @Throws(IOException::class)
    fun updateConfig(config: TransferConfig) {
        currentConfig = config
        saveConfig()
    }
    
    /**
     * 重置为默认配置
     */
    @Throws(IOException::class)
    fun resetToDefault() {
        currentConfig = TransferConfig.createDefault()
        saveConfig()
    }
    
    /**
     * 导入配置
     */
    @Throws(IOException::class)
    fun importConfig(path: Path) {
        currentConfig = parser.readFromFile(path)
        saveConfig()
    }
    
    /**
     * 导出配置
     */
    @Throws(IOException::class)
    fun exportConfig(path: Path) {
        parser.writeToFile(currentConfig, path)
    }
    
    /**
     * 确保分类目录存在
     */
    @Throws(IOException::class)
    fun ensureClassifyDirectoryExists() {
        val classifyDir = currentConfig.classifyDirectory
        if (!classifyDir.exists()) {
            classifyDir.createDirectories()
        } else if (!classifyDir.isDirectory()) {
            throw IOException("Classify path exists but is not a directory: $classifyDir")
        }
    }
    
    companion object {
        private const val KEY_CONFIG_PATH = "storage_transfer_config_path"
        private const val KEY_TRANSFER_ENABLED = "storage_transfer_enabled"
        private const val KEY_BACKGROUND_MONITOR = "storage_transfer_background_monitor"
        private const val KEY_AUTO_ORGANIZE_ON_STARTUP = "storage_transfer_auto_organize_on_startup"
        
        private const val DEFAULT_CONFIG_PATH = "/storage/emulated/0/Android/data/me.zhanghai.android.files/files/transfer_config.fvv"
        
        @Volatile
        private var instance: TransferConfigManager? = null
        
        fun getInstance(): TransferConfigManager {
            return instance ?: synchronized(this) {
                instance ?: TransferConfigManager().also { instance = it }
            }
        }
    }
}
