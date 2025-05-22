#!/bin/bash

# 构建脚本：打包MaterialFiles与定向存储传输集成版本

# 设置环境变量
export JAVA_HOME=/usr/lib/jvm/java-11-openjdk-amd64
export ANDROID_HOME=/opt/android-sdk

# 创建输出目录
mkdir -p /home/ubuntu/workspace/output

# 进入MaterialFiles目录
cd /home/ubuntu/workspace/materialfiles

# 清理旧的构建文件
./gradlew clean

# 构建APK
./gradlew assembleDebug

# 复制APK到输出目录
cp app/build/outputs/apk/debug/app-debug.apk /home/ubuntu/workspace/output/MaterialFiles-StorageTransfer.apk

# 创建发布包
cd /home/ubuntu/workspace
zip -r output/MaterialFiles-StorageTransfer-Release.zip output/MaterialFiles-StorageTransfer.apk user_manual.md integration_design.md

echo "构建完成！"
echo "APK路径: /home/ubuntu/workspace/output/MaterialFiles-StorageTransfer.apk"
echo "发布包路径: /home/ubuntu/workspace/output/MaterialFiles-StorageTransfer-Release.zip"
