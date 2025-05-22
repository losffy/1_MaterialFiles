/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.config

import java8.nio.file.Path
import java8.nio.file.Paths
import me.zhanghai.android.files.provider.common.ByteString
import me.zhanghai.android.files.provider.common.newBufferedReader
import me.zhanghai.android.files.provider.common.newBufferedWriter
import me.zhanghai.android.files.provider.common.newByteChannel
import me.zhanghai.android.files.provider.common.toByteString
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

/**
 * 定向存储传输配置文件解析器
 * 支持FVV格式（兼容SuperUserUtils）和JSON格式
 */
class TransferConfigParser {

    /**
     * 从FVV格式字符串解析配置
     */
    @Throws(IOException::class)
    fun parseFromFVV(content: String): TransferConfig {
        val name = extractValue(content, "Name") ?: "默认配置"
        val author = extractValue(content, "Author") ?: "MaterialFiles"
        val version = extractValue(content, "Version") ?: "1.0.0"
        val classifyDir = extractValue(content, "Classify") ?: "/storage/emulated/0/文件分类"
        val delay = extractValue(content, "Delay")?.toIntOrNull() ?: 60
        val multiUser = extractValue(content, "MultiUser")?.toBoolean() ?: false
        val subApp = extractValue(content, "SubApp")?.toBoolean() ?: true
        val subTime = extractValue(content, "SubTime")?.toBoolean() ?: false
        val defaultType = extractValue(content, "DefaultType") ?: "其他"
        val ignoreNoSuffix = extractValue(content, "IgnoreNoSuffix")?.toBoolean() ?: true
        
        // 解析监听目录列表
        val listenDirectories = extractMapList(content, "ListenList")
        
        // 解析递归目录列表
        val recList = extractList(content, "RecList")
        
        // 解析后缀规则
        val suffixRules = extractMapList(content, "SuffixList")
        
        // 解析忽略后缀列表
        val ignoreSuffixList = extractList(content, "IgnoreSuffixList")
        
        // 解析忽略名称列表
        val ignoreNameList = extractList(content, "IgnoreNameList")
        
        // 合并忽略列表
        val ignoreList = (ignoreSuffixList + ignoreNameList).distinct()
        
        return TransferConfig(
            name = name,
            author = author,
            version = version,
            classifyDirectory = Paths.get(classifyDir),
            delay = delay,
            multiUser = multiUser,
            subApp = subApp,
            subTime = subTime,
            defaultType = defaultType,
            ignoreNoSuffix = ignoreNoSuffix,
            listenDirectories = listenDirectories,
            recList = recList,
            suffixRules = suffixRules,
            ignoreList = ignoreList
        )
    }

    /**
     * 将配置导出为FVV格式字符串
     */
    fun exportToFVV(config: TransferConfig): String {
        val builder = StringBuilder()
        
        // 基本属性
        builder.appendLine("Name = \"${config.name}\"")
        builder.appendLine("Author = \"${config.author}\"")
        builder.appendLine("Version = \"${config.version}\"")
        builder.appendLine("Classify = \"${config.classifyDirectory}\"")
        builder.appendLine("Delay = ${config.delay}")
        builder.appendLine("MultiUser = ${config.multiUser}")
        builder.appendLine("SubApp = ${config.subApp}")
        builder.appendLine("SubTime = ${config.subTime}")
        builder.appendLine("DefaultType = \"${config.defaultType}\"")
        builder.appendLine("IgnoreNoSuffix = ${config.ignoreNoSuffix}")
        
        // 监听目录列表
        builder.appendLine("ListenList = {")
        config.listenDirectories.forEach { (key, paths) ->
            builder.appendLine("  $key = [")
            paths.forEach { path ->
                builder.appendLine("    \"$path\"")
            }
            builder.appendLine("  ]")
        }
        builder.appendLine("}")
        
        // 递归目录列表
        builder.appendLine("RecList = [")
        config.recList.forEach { path ->
            builder.appendLine("  \"$path\"")
        }
        builder.appendLine("]")
        
        // 后缀规则
        builder.appendLine("SuffixList = {")
        config.suffixRules.forEach { (key, suffixes) ->
            builder.appendLine("  $key = [${suffixes.joinToString(", ") { "\"$it\"" }}]")
        }
        builder.appendLine("}")
        
        // 忽略列表
        builder.appendLine("IgnoreSuffixList = [${config.ignoreList.joinToString(", ") { "\"$it\"" }}]")
        builder.appendLine("IgnoreNameList = []")
        
        return builder.toString()
    }

    /**
     * 从JSON格式字符串解析配置
     */
    fun parseFromJson(json: String): TransferConfig {
        val jsonObject = JSONObject(json)
        
        val name = jsonObject.optString("name", "默认配置")
        val author = jsonObject.optString("author", "MaterialFiles")
        val version = jsonObject.optString("version", "1.0.0")
        val classifyDir = jsonObject.optString("classifyDirectory", "/storage/emulated/0/文件分类")
        val delay = jsonObject.optInt("delay", 60)
        val multiUser = jsonObject.optBoolean("multiUser", false)
        val subApp = jsonObject.optBoolean("subApp", true)
        val subTime = jsonObject.optBoolean("subTime", false)
        val defaultType = jsonObject.optString("defaultType", "其他")
        val ignoreNoSuffix = jsonObject.optBoolean("ignoreNoSuffix", true)
        
        // 解析监听目录列表
        val listenDirectories = mutableMapOf<String, List<String>>()
        val listenDirsJson = jsonObject.optJSONObject("listenDirectories")
        if (listenDirsJson != null) {
            val keys = listenDirsJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val pathsArray = listenDirsJson.getJSONArray(key)
                val paths = mutableListOf<String>()
                for (i in 0 until pathsArray.length()) {
                    paths.add(pathsArray.getString(i))
                }
                listenDirectories[key] = paths
            }
        }
        
        // 解析递归目录列表
        val recList = mutableListOf<String>()
        val recListJson = jsonObject.optJSONArray("recList")
        if (recListJson != null) {
            for (i in 0 until recListJson.length()) {
                recList.add(recListJson.getString(i))
            }
        }
        
        // 解析后缀规则
        val suffixRules = mutableMapOf<String, List<String>>()
        val suffixRulesJson = jsonObject.optJSONObject("suffixRules")
        if (suffixRulesJson != null) {
            val keys = suffixRulesJson.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                val suffixesArray = suffixRulesJson.getJSONArray(key)
                val suffixes = mutableListOf<String>()
                for (i in 0 until suffixesArray.length()) {
                    suffixes.add(suffixesArray.getString(i))
                }
                suffixRules[key] = suffixes
            }
        }
        
        // 解析忽略列表
        val ignoreList = mutableListOf<String>()
        val ignoreListJson = jsonObject.optJSONArray("ignoreList")
        if (ignoreListJson != null) {
            for (i in 0 until ignoreListJson.length()) {
                ignoreList.add(ignoreListJson.getString(i))
            }
        }
        
        return TransferConfig(
            name = name,
            author = author,
            version = version,
            classifyDirectory = Paths.get(classifyDir),
            delay = delay,
            multiUser = multiUser,
            subApp = subApp,
            subTime = subTime,
            defaultType = defaultType,
            ignoreNoSuffix = ignoreNoSuffix,
            listenDirectories = listenDirectories,
            recList = recList,
            suffixRules = suffixRules,
            ignoreList = ignoreList
        )
    }

    /**
     * 将配置导出为JSON格式字符串
     */
    fun exportToJson(config: TransferConfig): String {
        val jsonObject = JSONObject()
        
        // 基本属性
        jsonObject.put("name", config.name)
        jsonObject.put("author", config.author)
        jsonObject.put("version", config.version)
        jsonObject.put("classifyDirectory", config.classifyDirectory.toString())
        jsonObject.put("delay", config.delay)
        jsonObject.put("multiUser", config.multiUser)
        jsonObject.put("subApp", config.subApp)
        jsonObject.put("subTime", config.subTime)
        jsonObject.put("defaultType", config.defaultType)
        jsonObject.put("ignoreNoSuffix", config.ignoreNoSuffix)
        
        // 监听目录列表
        val listenDirsJson = JSONObject()
        config.listenDirectories.forEach { (key, paths) ->
            val pathsArray = JSONArray()
            paths.forEach { path ->
                pathsArray.put(path)
            }
            listenDirsJson.put(key, pathsArray)
        }
        jsonObject.put("listenDirectories", listenDirsJson)
        
        // 递归目录列表
        val recListJson = JSONArray()
        config.recList.forEach { path ->
            recListJson.put(path)
        }
        jsonObject.put("recList", recListJson)
        
        // 后缀规则
        val suffixRulesJson = JSONObject()
        config.suffixRules.forEach { (key, suffixes) ->
            val suffixesArray = JSONArray()
            suffixes.forEach { suffix ->
                suffixesArray.put(suffix)
            }
            suffixRulesJson.put(key, suffixesArray)
        }
        jsonObject.put("suffixRules", suffixRulesJson)
        
        // 忽略列表
        val ignoreListJson = JSONArray()
        config.ignoreList.forEach { item ->
            ignoreListJson.put(item)
        }
        jsonObject.put("ignoreList", ignoreListJson)
        
        return jsonObject.toString(2)
    }

    /**
     * 从文件读取配置
     */
    @Throws(IOException::class)
    fun readFromFile(path: Path): TransferConfig {
        val content = path.newBufferedReader(StandardCharsets.UTF_8).use { reader ->
            reader.readText()
        }
        
        return when {
            path.toString().endsWith(".fvv", ignoreCase = true) -> parseFromFVV(content)
            path.toString().endsWith(".json", ignoreCase = true) -> parseFromJson(content)
            else -> throw IOException("Unsupported file format")
        }
    }

    /**
     * 将配置写入文件
     */
    @Throws(IOException::class)
    fun writeToFile(config: TransferConfig, path: Path) {
        val content = when {
            path.toString().endsWith(".fvv", ignoreCase = true) -> exportToFVV(config)
            path.toString().endsWith(".json", ignoreCase = true) -> exportToJson(config)
            else -> throw IOException("Unsupported file format")
        }
        
        path.newBufferedWriter(StandardCharsets.UTF_8).use { writer ->
            writer.write(content)
        }
    }

    // 辅助方法：从FVV内容中提取单个值
    private fun extractValue(content: String, key: String): String? {
        val pattern = Pattern.compile("$key\\s*=\\s*\"([^\"]*)\"", Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        return if (matcher.find()) {
            matcher.group(1)
        } else {
            val patternNoQuotes = Pattern.compile("$key\\s*=\\s*([^\\s,\\}\\]]+)", Pattern.DOTALL)
            val matcherNoQuotes = patternNoQuotes.matcher(content)
            if (matcherNoQuotes.find()) {
                matcherNoQuotes.group(1)
            } else {
                null
            }
        }
    }

    // 辅助方法：从FVV内容中提取列表
    private fun extractList(content: String, key: String): List<String> {
        val result = mutableListOf<String>()
        val pattern = Pattern.compile("$key\\s*=\\s*\\[(.*?)\\]", Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        
        if (matcher.find()) {
            val listContent = matcher.group(1)
            val itemPattern = Pattern.compile("\"([^\"]*)\"")
            val itemMatcher = itemPattern.matcher(listContent)
            
            while (itemMatcher.find()) {
                result.add(itemMatcher.group(1))
            }
        }
        
        return result
    }

    // 辅助方法：从FVV内容中提取映射列表
    private fun extractMapList(content: String, key: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val pattern = Pattern.compile("$key\\s*=\\s*\\{(.*?)\\}", Pattern.DOTALL)
        val matcher = pattern.matcher(content)
        
        if (matcher.find()) {
            val mapContent = matcher.group(1)
            val entryPattern = Pattern.compile("(\\S+)\\s*=\\s*\\[(.*?)\\]", Pattern.DOTALL)
            val entryMatcher = entryPattern.matcher(mapContent)
            
            while (entryMatcher.find()) {
                val entryKey = entryMatcher.group(1)
                val entryValues = mutableListOf<String>()
                
                val valueContent = entryMatcher.group(2)
                val valuePattern = Pattern.compile("\"([^\"]*)\"")
                val valueMatcher = valuePattern.matcher(valueContent)
                
                while (valueMatcher.find()) {
                    entryValues.add(valueMatcher.group(1))
                }
                
                result[entryKey] = entryValues
            }
        }
        
        return result
    }
}
