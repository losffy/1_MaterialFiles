/*
 * Copyright (c) 2025 Manus AI <support@manus.ai>
 * All Rights Reserved.
 */

package me.zhanghai.android.files.storage.transfer.monitor

import android.os.FileObserver
import java8.nio.file.Path
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * 文件变化观察者
 * 监控指定目录的文件变化
 */
class FileChangeObserver(
    private val path: Path,
    private val recursive: Boolean = false,
    private val onEvent: (event: Int, path: String?) -> Unit
) {
    private var observer: RecursiveFileObserver? = null
    private val observedPaths = ConcurrentHashMap<String, Boolean>()
    
    /**
     * 开始监听文件变化
     */
    fun startWatching() {
        val file = File(path.toString())
        if (!file.exists()) {
            return
        }
        
        observer = RecursiveFileObserver(
            file.absolutePath,
            FileObserver.CREATE or FileObserver.MOVED_TO,
            recursive
        ) { event, path ->
            if (path != null && !observedPaths.containsKey(path)) {
                observedPaths[path] = true
                onEvent(event, path)
                // 一段时间后移除路径，避免重复处理
                Thread {
                    Thread.sleep(5000)
                    observedPaths.remove(path)
                }.start()
            }
        }
        observer?.startWatching()
    }
    
    /**
     * 停止监听
     */
    fun stopWatching() {
        observer?.stopWatching()
        observer = null
        observedPaths.clear()
    }
    
    /**
     * 递归文件观察者
     * 支持递归监控目录
     */
    private class RecursiveFileObserver(
        private val path: String,
        mask: Int,
        private val recursive: Boolean,
        private val onEvent: (event: Int, path: String?) -> Unit
    ) {
        private val observer: FileObserver
        private val observerMap = mutableMapOf<String, FileObserver>()
        
        init {
            observer = object : FileObserver(path, mask) {
                override fun onEvent(event: Int, path: String?) {
                    val fullPath = if (path != null) "$this.path/$path" else this@RecursiveFileObserver.path
                    onEvent(event, fullPath)
                    
                    // 如果是创建目录且需要递归监控，则为新目录创建观察者
                    if (recursive && path != null && event == FileObserver.CREATE) {
                        val newPath = File("$this@RecursiveFileObserver.path/$path")
                        if (newPath.isDirectory) {
                            addDirectoryObserver(newPath.absolutePath)
                        }
                    }
                }
            }
            
            // 如果需要递归监控，为所有子目录创建观察者
            if (recursive) {
                val rootDir = File(path)
                if (rootDir.isDirectory) {
                    addDirectoryObserversRecursively(rootDir)
                }
            }
        }
        
        /**
         * 开始监听
         */
        fun startWatching() {
            observer.startWatching()
            observerMap.values.forEach { it.startWatching() }
        }
        
        /**
         * 停止监听
         */
        fun stopWatching() {
            observer.stopWatching()
            observerMap.values.forEach { it.stopWatching() }
            observerMap.clear()
        }
        
        /**
         * 递归添加目录观察者
         */
        private fun addDirectoryObserversRecursively(directory: File) {
            val files = directory.listFiles() ?: return
            for (file in files) {
                if (file.isDirectory) {
                    addDirectoryObserver(file.absolutePath)
                    addDirectoryObserversRecursively(file)
                }
            }
        }
        
        /**
         * 添加目录观察者
         */
        private fun addDirectoryObserver(dirPath: String) {
            if (observerMap.containsKey(dirPath)) {
                return
            }
            
            val observer = object : FileObserver(dirPath, observer.mask) {
                override fun onEvent(event: Int, path: String?) {
                    val fullPath = if (path != null) "$dirPath/$path" else dirPath
                    onEvent(event, fullPath)
                    
                    // 如果是创建目录，则为新目录创建观察者
                    if (path != null && event == FileObserver.CREATE) {
                        val newPath = File("$dirPath/$path")
                        if (newPath.isDirectory) {
                            addDirectoryObserver(newPath.absolutePath)
                        }
                    }
                }
            }
            
            observerMap[dirPath] = observer
            observer.startWatching()
        }
    }
}
