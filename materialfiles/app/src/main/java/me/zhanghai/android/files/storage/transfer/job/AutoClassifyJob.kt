/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.job

import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.filejob.FileJob
import me.zhanghai.android.files.provider.common.createDirectories
import me.zhanghai.android.files.provider.common.exists
import me.zhanghai.android.files.provider.common.isDirectory
import me.zhanghai.android.files.provider.common.moveTo
import me.zhanghai.android.files.storage.transfer.config.TransferConfig
import java.io.File
import java.io.IOException

/**
 * 自动分类任务
 * 根据文件后缀自动分类文件
 */
class AutoClassifyJob(
    private val sourcePath: Path,
    private val config: TransferConfig
) : FileJob() {

    @Throws(IOException::class)
    override fun run() {
        // 检查源文件是否存在
        if (!sourcePath.exists()) {
            throw IOException("Source file does not exist: $sourcePath")
        }

        // 获取文件名和后缀
        val fileName = sourcePath.fileName.toString()
        val extension = fileName.substringAfterLast('.', "").lowercase()

        // 检查是否忽略无后缀文件
        if (extension.isEmpty() && config.ignoreNoSuffix) {
            return
        }

        // 检查是否在忽略列表中
        if (config.ignoreList.contains(extension) || 
            config.ignoreList.any { fileName.contains(it, ignoreCase = true) }) {
            return
        }

        // 确定目标分类
        val category = determineCategory(extension)

        // 构建目标路径
        val targetDir = buildTargetPath(category)
        
        // 确保目标目录存在
        if (!targetDir.exists()) {
            targetDir.createDirectories()
        } else if (!targetDir.isDirectory()) {
            throw IOException("Target path exists but is not a directory: $targetDir")
        }

        // 构建完整目标路径
        val targetPath = targetDir.resolve(fileName)

        // 如果目标文件已存在，添加序号
        val finalTargetPath = if (targetPath.exists()) {
            generateUniqueTargetPath(targetPath)
        } else {
            targetPath
        }

        // 移动文件
        sourcePath.moveTo(finalTargetPath)
    }

    /**
     * 确定文件分类
     */
    private fun determineCategory(extension: String): String {
        // 遍历后缀规则
        config.suffixRules.forEach { (category, suffixes) ->
            if (suffixes.contains(extension)) {
                // 处理特殊分类名称（包含[name]标记）
                if (category.contains("[name]")) {
                    return category.replace("[name]", sourcePath.fileName.toString().substringBeforeLast('.'))
                }
                return category
            }
        }

        // 如果没有匹配的规则，使用默认分类
        return config.defaultType
    }

    /**
     * 构建目标路径
     */
    private fun buildTargetPath(category: String): Path {
        val baseDir = config.classifyDirectory
        
        // 如果启用了子应用分类
        if (config.subApp) {
            // 从源路径中提取应用名称
            val appName = extractAppName(sourcePath.toString())
            if (appName != null) {
                return baseDir.resolve(appName).resolve(category)
            }
        }
        
        return baseDir.resolve(category)
    }

    /**
     * 从路径中提取应用名称
     */
    private fun extractAppName(path: String): String? {
        // 遍历监听目录列表
        config.listenDirectories.forEach { (appName, paths) ->
            paths.forEach { listenPath ->
                val expandedPath = expandPathVariables(listenPath)
                if (path.startsWith(expandedPath)) {
                    return appName
                }
            }
        }
        
        return null
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
     * 生成唯一的目标路径
     */
    private fun generateUniqueTargetPath(targetPath: Path): Path {
        val fileName = targetPath.fileName.toString()
        val baseName = fileName.substringBeforeLast('.')
        val extension = fileName.substringAfterLast('.', "")
        
        var counter = 1
        var newPath: Path
        
        do {
            val newName = if (extension.isEmpty()) {
                "${baseName}_$counter"
            } else {
                "${baseName}_$counter.$extension"
            }
            
            newPath = targetPath.parent.resolve(newName)
            counter++
        } while (newPath.exists())
        
        return newPath
    }
}
