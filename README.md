# CCTV直播 (mytv)

把央视官网 [`tv.cctv.com/live`](https://tv.cctv.com/live/) 与央视频 [`yangshipin.cn`](https://www.yangshipin.cn)
的直播网页**套壳**进一个 Android App，做成可侧载到 **Android TV 盒子 / 平板**的直播应用：
打开即用遥控器选台，进频道**自动播放并全屏**，无需任何网页交互。

- **41 个频道**：20 个央视频道 + 21 个地方卫视，每个频道一个独立 URL，直接加载、无需模拟点击。
- **遥控器友好**：方向键选台、上下切台、返回键逐级退出；焦点高亮。
- **自动全屏**：注入脚本自动点全屏按钮、开声音、保活播放，并把画面铺满任意分辨率的屏幕。
- **可替换浏览器引擎**：
  - **系统 WebView** —— 体积最小、最稳；
  - **GeckoView**（默认）—— Firefox 引擎打包进 APK，跨设备表现一致、自带 Widevine DRM。
  - 切换只改一行（`web/WebPlayer.kt` 的 `ACTIVE_ENGINE`）。

## 构建 & 安装

```bash
./gradlew :app:assembleDebug
# 产物（按 ABI 分包）：app/build/outputs/apk/debug/app-<abi>-debug.apk
#   armeabi-v7a → 32 位盒子    arm64-v8a → 64 位平板/盒子
adb install -r app/build/outputs/apk/debug/app-arm64-v8a-debug.apk
```

需 Android SDK（`local.properties` 里配 `sdk.dir`）、JDK 17。技术细节、各模块逻辑、踩过的坑见
[`doc/架构说明.md`](doc/架构说明.md)（详尽到可据此重建）。

## 致谢

自动化选台与注入全屏的思路移植自 [**CCTV_Viewer**](https://github.com/eanyatonic/CCTV_Viewer)
（by eanyatonic）——频道清单、"逐 URL 选台 + 轮询点全屏按钮"的核心做法都来自该项目，特此感谢。
本项目在其基础上换成 Jetpack Compose + 单 Activity 架构，并增加了可替换的 GeckoView 引擎。

## 说明

仅供自家盒子上观看学习，请勿分发或商用。频道版权归央视所有；体育等加密频道及海外地理封锁与本项目无关。
