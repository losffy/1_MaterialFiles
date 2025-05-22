/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.config

import java8.nio.file.Path
import java8.nio.file.Paths

/**
 * 定向存储传输配置数据模型
 * 兼容SuperUserUtils的config.fvv格式
 */
data class TransferConfig(
    val name: String = "默认配置",
    val author: String = "MaterialFiles",
    val version: String = "1.0.0",
    val classifyDirectory: Path = Paths.get("/storage/emulated/0/文件分类"),
    val delay: Int = 60,
    val multiUser: Boolean = false,
    val subApp: Boolean = true,
    val subTime: Boolean = false,
    val defaultType: String = "其他",
    val ignoreNoSuffix: Boolean = true,
    val listenDirectories: Map<String, List<String>> = emptyMap(),
    val recList: List<String> = emptyList(),
    val suffixRules: Map<String, List<String>> = emptyMap(),
    val ignoreList: List<String> = listOf("bak", "aria", "tmp", "cache", "log")
) {
    companion object {
        /**
         * 创建默认配置
         */
        fun createDefault(): TransferConfig {
            val defaultListenDirs = mapOf(
                "下载" to listOf("{storage}/Download"),
                "QQ" to listOf("{androidData}/com.tencent.mobileqq/Tencent/QQfile_recv"),
                "微信" to listOf("{androidData}/com.tencent.mm/MicroMsg/Download")
            )
            
            val defaultSuffixRules = mapOf(
                "压缩包" to listOf("zip", "rar", "7z", "tar", "gz"),
                "程序" to listOf("exe", "msi", "bat", "sh", "cmd", "jar"),
                "视频" to listOf("mp4", "mkv", "avi", "mov", "wmv", "flv"),
                "文档" to listOf("doc", "docx", "pdf", "txt", "xls", "xlsx", "ppt", "pptx"),
                "安装包" to listOf("apk", "apks", "xapk", "ipa"),
                "音频" to listOf("mp3", "wav", "flac", "aac", "ogg"),
                "图片" to listOf("jpg", "jpeg", "png", "gif", "bmp", "webp"),
                "代码" to listOf("java", "py", "js", "html", "css", "json", "xml", "kt"),
                "其他" to listOf("bin", "dat")
            )
            
            return TransferConfig(
                listenDirectories = defaultListenDirs,
                suffixRules = defaultSuffixRules
            )
        }
    }
}
