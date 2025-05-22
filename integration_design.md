# MaterialFiles与SuperUserUtils定向存储传输功能集成方案

## 1. 功能概述

本方案旨在将SuperUserUtils（苏柚）的定向存储传输功能集成到MaterialFiles文件管理器中，使用户能够通过直观、简单的方式实现文件的自动分类和整理。

### 1.1 SuperUserUtils定向存储传输核心功能

SuperUserUtils的定向存储传输功能主要通过以下机制实现：
- 基于配置文件（config.fvv）驱动的规则系统
- 监听指定目录的文件变化
- 根据文件后缀自动分类
- 自动化脚本执行文件移动和整理

### 1.2 MaterialFiles核心架构

MaterialFiles是一个基于Material Design的开源文件管理器，具有以下特点：
- 基于现代Android架构组件（ViewModel、LiveData）
- 文件操作通过FileJob系统实现异步处理
- 支持多种文件系统和协议
- 良好的UI设计和用户体验

## 2. 集成架构设计

### 2.1 新增模块结构

在MaterialFiles中新增以下模块：

```
me.zhanghai.android.files.storage
├── transfer/
│   ├── config/
│   │   ├── TransferConfig.kt           // 配置文件数据模型
│   │   ├── TransferConfigParser.kt     // 配置文件解析器
│   │   └── TransferConfigManager.kt    // 配置文件管理
│   ├── monitor/
│   │   ├── FileMonitorService.kt       // 文件监听服务
│   │   └── FileChangeObserver.kt       // 文件变化观察者
│   ├── job/
│   │   ├── TransferFileJob.kt          // 文件传输任务
│   │   └── AutoClassifyJob.kt          // 自动分类任务
│   ├── ui/
│   │   ├── TransferSettingsActivity.kt // 传输设置界面
│   │   ├── TransferSettingsFragment.kt // 传输设置片段
│   │   ├── RuleEditorActivity.kt       // 规则编辑界面
│   │   └── RuleEditorFragment.kt       // 规则编辑片段
│   └── TransferManager.kt              // 传输管理器
```

### 2.2 配置文件解析模块

配置文件将采用与SuperUserUtils兼容的FVV格式，同时提供JSON格式的导入/导出功能：

```kotlin
// TransferConfig.kt
data class TransferConfig(
    val name: String,
    val author: String,
    val version: String,
    val classifyDirectory: Path,
    val delay: Int,
    val listenDirectories: Map<String, List<String>>,
    val suffixRules: Map<String, List<String>>,
    val ignoreList: List<String>
)

// TransferConfigParser.kt
class TransferConfigParser {
    fun parseFromFVV(content: String): TransferConfig
    fun exportToFVV(config: TransferConfig): String
    fun parseFromJson(json: String): TransferConfig
    fun exportToJson(config: TransferConfig): String
}
```

### 2.3 文件监听和自动分类服务

文件监听服务将作为前台服务运行，监控配置的目录并触发自动分类任务：

```kotlin
// FileMonitorService.kt
class FileMonitorService : Service() {
    private val observers = mutableMapOf<Path, FileChangeObserver>()
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        // 初始化文件观察者
        return START_STICKY
    }
    
    fun setupObservers(config: TransferConfig) {
        // 根据配置设置文件观察者
    }
}

// FileChangeObserver.kt
class FileChangeObserver(private val path: Path) {
    fun startWatching() {
        // 开始监听文件变化
    }
    
    fun stopWatching() {
        // 停止监听
    }
}
```

### 2.4 文件传输任务

文件传输任务将继承MaterialFiles的FileJob系统，实现自动分类和移动功能：

```kotlin
// TransferFileJob.kt
class TransferFileJob(
    private val source: Path,
    private val config: TransferConfig
) : FileJob() {
    override fun run() {
        // 根据配置规则确定目标路径
        // 执行文件移动操作
    }
}

// AutoClassifyJob.kt
class AutoClassifyJob(
    private val newFile: Path,
    private val config: TransferConfig
) : FileJob() {
    override fun run() {
        // 根据文件后缀自动分类
        // 创建目标目录（如果不存在）
        // 移动文件到目标目录
    }
}
```

### 2.5 用户界面设计

#### 2.5.1 主界面集成

在MaterialFiles的导航抽屉中添加"定向存储传输"入口：

```xml
<!-- navigation_items.xml 添加 -->
<item
    android:id="@+id/action_storage_transfer"
    android:icon="@drawable/storage_transfer_icon"
    android:title="@string/storage_transfer_title" />
```

#### 2.5.2 传输设置界面

传输设置界面将允许用户：
- 启用/禁用定向存储传输功能
- 查看和编辑现有规则
- 导入/导出配置文件
- 设置全局参数（延迟时间、默认分类目录等）

#### 2.5.3 规则编辑界面

规则编辑界面将允许用户：
- 添加/删除监听目录
- 设置文件类型规则
- 配置忽略规则
- 测试规则效果

## 3. 实现策略

### 3.1 配置文件兼容性

为确保与SuperUserUtils的配置文件兼容，我们将：
- 实现完整的FVV格式解析器
- 支持SuperUserUtils的所有配置参数
- 提供配置文件导入/导出功能

### 3.2 文件监听机制

文件监听将采用以下策略：
- 使用Android的FileObserver API监控文件系统变化
- 实现延迟处理机制，避免频繁触发
- 支持多目录同时监控
- 提供电池优化策略

### 3.3 权限管理

定向存储传输功能需要以下权限：
- 存储读写权限
- 后台运行权限
- 自启动权限（可选）

我们将实现完整的权限请求和检查流程，确保功能正常运行。

### 3.4 用户体验优化

为提供良好的用户体验，我们将：
- 提供直观的配置界面
- 实现操作反馈机制（通知、进度条等）
- 支持规则测试和预览
- 提供详细的使用说明

## 4. 集成路线图

### 4.1 基础架构实现
- 创建新的模块结构
- 实现配置文件解析器
- 实现基本的文件监听服务

### 4.2 核心功能实现
- 实现文件传输任务
- 实现自动分类逻辑
- 集成到MaterialFiles的FileJob系统

### 4.3 用户界面实现
- 设计并实现传输设置界面
- 设计并实现规则编辑界面
- 在主界面添加入口

### 4.4 测试与优化
- 单元测试各个模块
- 集成测试整体功能
- 性能优化和电池使用优化

## 5. 技术挑战与解决方案

### 5.1 文件监听效率

**挑战**：持续监听多个目录可能导致电池消耗过大。

**解决方案**：
- 实现智能监听策略，根据用户活动调整监听频率
- 使用批处理机制，减少文件操作次数
- 提供省电模式选项

### 5.2 权限管理

**挑战**：Android 10+对存储访问有更严格的限制。

**解决方案**：
- 使用SAF（Storage Access Framework）处理文件
- 提供明确的权限请求流程
- 针对不同Android版本优化实现

### 5.3 配置文件复杂性

**挑战**：FVV格式解析和兼容性维护。

**解决方案**：
- 实现健壮的解析器，处理各种边缘情况
- 提供配置验证机制
- 支持配置版本管理

## 6. 总结

本集成方案将SuperUserUtils的定向存储传输功能无缝集成到MaterialFiles中，保持原有功能的灵活性和强大性，同时提供更直观、更易用的界面。通过这种集成，用户将能够在享受MaterialFiles优秀文件管理体验的同时，获得强大的文件自动分类和整理能力。
