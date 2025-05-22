/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import me.zhanghai.android.files.R
import me.zhanghai.android.files.databinding.TransferSettingsFragmentBinding
import me.zhanghai.android.files.storage.transfer.TransferManager
import me.zhanghai.android.files.storage.transfer.config.TransferConfigManager
import me.zhanghai.android.files.util.createViewIntent
import me.zhanghai.android.files.util.showToast
import java.io.IOException

/**
 * 定向存储传输设置界面
 */
class TransferSettingsFragment : Fragment() {

    private lateinit var binding: TransferSettingsFragmentBinding
    private val transferManager = TransferManager.getInstance()
    private val configManager = TransferConfigManager.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = TransferSettingsFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).setSupportActionBar(binding.toolbar)

        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        // 初始化开关状态
        binding.transferEnabledSwitch.isChecked = configManager.isTransferEnabled
        binding.backgroundMonitorSwitch.isChecked = configManager.isBackgroundMonitorEnabled
        binding.autoOrganizeSwitch.isChecked = configManager.isAutoOrganizeOnStartup

        // 设置分类目录路径
        binding.classifyDirectoryText.text = configManager.currentConfig.classifyDirectory.toString()

        // 设置监听目录数量
        val listenDirsCount = configManager.currentConfig.listenDirectories.values
            .flatten().size
        binding.listenDirectoriesCount.text = listenDirsCount.toString()

        // 设置规则数量
        val rulesCount = configManager.currentConfig.suffixRules.size
        binding.rulesCount.text = rulesCount.toString()
    }

    private fun setupListeners() {
        // 定向存储传输开关
        binding.transferEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
            transferManager.enableTransfer(isChecked)
            updateViewState()
        }

        // 后台监听开关
        binding.backgroundMonitorSwitch.setOnCheckedChangeListener { _, isChecked ->
            transferManager.enableBackgroundMonitor(isChecked)
        }

        // 自动整理开关
        binding.autoOrganizeSwitch.setOnCheckedChangeListener { _, isChecked ->
            transferManager.enableAutoOrganizeOnStartup(isChecked)
        }

        // 立即整理按钮
        binding.organizeNowButton.setOnClickListener {
            transferManager.organizeFiles()
            requireContext().showToast(R.string.storage_transfer_organizing_started)
        }

        // 编辑规则按钮
        binding.editRulesButton.setOnClickListener {
            startActivity(RuleEditorActivity.newIntent(requireContext()))
        }

        // 导入配置按钮
        binding.importConfigButton.setOnClickListener {
            // 实现导入配置逻辑
            // 可以使用SAF选择文件
        }

        // 导出配置按钮
        binding.exportConfigButton.setOnClickListener {
            // 实现导出配置逻辑
            // 可以使用SAF选择保存位置
        }

        // 重置配置按钮
        binding.resetConfigButton.setOnClickListener {
            resetConfig()
        }

        // 打开分类目录按钮
        binding.openClassifyDirButton.setOnClickListener {
            openClassifyDirectory()
        }
    }

    private fun updateViewState() {
        val isEnabled = configManager.isTransferEnabled
        
        // 更新控件状态
        binding.backgroundMonitorSwitch.isEnabled = isEnabled
        binding.autoOrganizeSwitch.isEnabled = isEnabled
        binding.organizeNowButton.isEnabled = isEnabled
        binding.editRulesButton.isEnabled = isEnabled
        binding.importConfigButton.isEnabled = isEnabled
        binding.exportConfigButton.isEnabled = isEnabled
        binding.resetConfigButton.isEnabled = isEnabled
        binding.openClassifyDirButton.isEnabled = isEnabled
    }

    private fun resetConfig() {
        lifecycleScope.launch {
            try {
                configManager.resetToDefault()
                requireContext().showToast(R.string.storage_transfer_config_reset_success)
                
                // 重新加载UI
                initializeViews()
            } catch (e: IOException) {
                requireContext().showToast(
                    getString(R.string.storage_transfer_config_reset_error, e.message)
                )
            }
        }
    }

    private fun openClassifyDirectory() {
        val path = configManager.currentConfig.classifyDirectory
        try {
            startActivity(path.createViewIntent())
        } catch (e: Exception) {
            requireContext().showToast(
                getString(R.string.storage_transfer_open_directory_error, e.message)
            )
        }
    }

    companion object {
        fun newInstance(): TransferSettingsFragment {
            return TransferSettingsFragment()
        }
    }
}
