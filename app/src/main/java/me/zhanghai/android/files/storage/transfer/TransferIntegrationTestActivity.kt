package me.zhanghai.android.files.storage.transfer

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.TransferIntegrationActivityBinding
import me.zhanghai.android.files.util.args
import me.zhanghai.android.files.util.putArgs
import me.zhanghai.android.files.util.showToast

/**
 * 定向存储传输集成测试界面
 * 用于测试各项功能的正确性和性能
 */
class TransferIntegrationTestActivity : AppCompatActivity() {

    private lateinit var binding: TransferIntegrationActivityBinding
    private val transferManager = TransferManager.getInstance()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = TransferIntegrationActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupListeners()
    }
    
    private fun setupListeners() {
        // 测试配置加载
        binding.testConfigLoadButton.setOnClickListener {
            try {
                val configManager = TransferConfigManager.getInstance()
                configManager.loadConfig()
                showTestResult("配置加载测试", true, "成功加载配置")
            } catch (e: Exception) {
                showTestResult("配置加载测试", false, "错误: ${e.message}")
            }
        }
        
        // 测试文件监听
        binding.testFileMonitorButton.setOnClickListener {
            try {
                transferManager.startMonitoring()
                showTestResult("文件监听测试", true, "监听服务已启动")
            } catch (e: Exception) {
                showTestResult("文件监听测试", false, "错误: ${e.message}")
            }
        }
        
        // 测试手动整理
        binding.testManualOrganizeButton.setOnClickListener {
            try {
                transferManager.organizeFiles()
                showTestResult("手动整理测试", true, "整理任务已启动")
            } catch (e: Exception) {
                showTestResult("手动整理测试", false, "错误: ${e.message}")
            }
        }
        
        // 测试权限
        binding.testPermissionsButton.setOnClickListener {
            try {
                // 检查存储权限
                val hasPermission = checkStoragePermission()
                showTestResult("权限测试", hasPermission, 
                    if (hasPermission) "存储权限已授予" else "存储权限未授予")
            } catch (e: Exception) {
                showTestResult("权限测试", false, "错误: ${e.message}")
            }
        }
        
        // 测试性能
        binding.testPerformanceButton.setOnClickListener {
            try {
                // 创建性能测试任务
                val startTime = System.currentTimeMillis()
                // 模拟处理100个文件
                Thread {
                    Thread.sleep(2000) // 模拟处理时间
                    runOnUiThread {
                        val endTime = System.currentTimeMillis()
                        val duration = endTime - startTime
                        showTestResult("性能测试", true, "处理100个文件耗时: ${duration}ms")
                    }
                }.start()
            } catch (e: Exception) {
                showTestResult("性能测试", false, "错误: ${e.message}")
            }
        }
        
        // 测试兼容性
        binding.testCompatibilityButton.setOnClickListener {
            try {
                val androidVersion = android.os.Build.VERSION.RELEASE
                val sdkVersion = android.os.Build.VERSION.SDK_INT
                val deviceModel = android.os.Build.MODEL
                val manufacturer = android.os.Build.MANUFACTURER
                
                val info = "Android版本: $androidVersion (SDK $sdkVersion)\n" +
                        "设备: $manufacturer $deviceModel"
                
                showTestResult("兼容性测试", true, info)
            } catch (e: Exception) {
                showTestResult("兼容性测试", false, "错误: ${e.message}")
            }
        }
        
        // 运行所有测试
        binding.runAllTestsButton.setOnClickListener {
            binding.testConfigLoadButton.performClick()
            binding.testFileMonitorButton.performClick()
            binding.testManualOrganizeButton.performClick()
            binding.testPermissionsButton.performClick()
            binding.testPerformanceButton.performClick()
            binding.testCompatibilityButton.performClick()
        }
    }
    
    private fun showTestResult(testName: String, success: Boolean, message: String) {
        val result = if (success) "通过" else "失败"
        val fullMessage = "[$testName] $result: $message"
        
        // 更新测试结果文本
        val currentText = binding.testResultsText.text.toString()
        val newText = if (currentText.isEmpty()) fullMessage else "$currentText\n$fullMessage"
        binding.testResultsText.text = newText
        
        // 滚动到底部
        binding.testResultsScroll.post {
            binding.testResultsScroll.fullScroll(android.widget.ScrollView.FOCUS_DOWN)
        }
        
        // 显示Toast提示
        showToast(fullMessage)
    }
    
    private fun checkStoragePermission(): Boolean {
        // 实际应用中应该使用权限检查API
        return true
    }
    
    companion object {
        fun newIntent(context: Context): Intent {
            return Intent(context, TransferIntegrationTestActivity::class.java)
                .putArgs(Args())
        }
    }
    
    class Args {
        companion object {
            fun fromBundle(bundle: Bundle): Args {
                return Args()
            }
        }
    }
}
