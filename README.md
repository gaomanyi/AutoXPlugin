# AutoXPlugin for IntelliJ IDEA
<!-- Plugin description -->
![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)
![JetBrains IntelliJ Platform SDK Docs](https://jb.gg/badges/docs.svg?style=flat-square)

这是一个IntelliJ IDEA插件，用于开发AutoX.js的配套插件,如果你还没有AutoX.js程序,请访问[AutoX.js](https://github.com/aiselp/AutoX)

[🌟 English](README_en.md) | [🌏 中文](README.md) | [🌏 日本語](README_JP.md) | [🌏 한국인](README_ko.md) | [🌏 Русский](README_ru.md) | [🌏 Français](README_fr.md)
> [!TIP]
> 暂时没有适配USB调试开发,只支持局域网调试

## 功能

- 在 IDE 内直接运行简单的 WebSocket 服务器
- 生成二维码，方便移动设备连接
- 右键单击任意文件，即可发送到已连接的设备
- 通过本地网络进行实时文件传输
- 支持多语言,修改 idea 的语言后,插件会自动切换语言


<!-- Plugin description end -->

## 界面截图
<div align="center">
<table>
<tr>

<td align="center">
<b>工具栏</b><br>
<img src="/img/%E6%88%AA%E5%B1%8F2025-04-03%2000.43.22.png" width="500" alt="微信赞赏码"><br>
<small>点击启动后,手机就可以扫描二维码进行连接</small>
</td>
<td align="center">
<b>文件夹右键</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.57.png" width="500" alt="支付宝赞赏码"><br>
<small>在文件夹上右键,可以选择发送或运行到已连接的设备</small>
</td>
<td align="center">
<b>js文件右键</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.39.png" width="500" alt="Alipay"><br>
<em>在有效的autox.js脚本上右键,可以对已连接的设备执行保存,运行,重新运行,结束</em>
</td>
<td align="center">
<b>有效的json文件上右键</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.41.36.png" width="500" alt="WeChat"><br>
<em>在有效的json文件上右键,可以把所在的目录保存到设备</em>
</td>
</tr>
</table>
</div>

## 功能详细解释

因为一个有效的autox.js工程文件,其中包含一个project.json文件,所以,可以对包含project.json文件的文件夹进行操作"运行到设备"

- 保存项目到设备: 将右键的文件夹发送到连接的设备,操作成功后,可以在手机端看到文件
- 运行项目到设备: 将右键的文件夹发送到连接的设备,操作成功后,可以在手机端运行,但是不会把文件保存到设备
- 保存脚本到设备: 将右键单个文件发送到连接的设备,操作成功后,可以在手机端看到文件
- 运行脚本到设备: 将右键单个文件发送到连接的设备,操作成功后,可以在手机端运行,但是不会把文件保存到设备
- 重新运行脚本到设备: 将右键单个文件发送到连接的设备,操作成功后,可以在手机端运行,并把上次运行的程序先结束
- 结束脚本到设备: 操作成功后,可以在手机端结束运行同名的程序

## 安装

1. 从 Releases 页面下载最新版本
2. 在 IntelliJ IDEA 中，进入 `设置` > `插件` > `从磁盘安装插件...`
3. 选择下载的 `.zip` 文件
4. 重启 IntelliJ IDEA

## 使用方法

1. 在右侧边栏打开 **AutoXPlugin** 工具窗口
2. 点击 **"启动服务器"** 以启动 WebSocket 服务器
3. 使用移动设备扫描二维码
4. 在设备上连接到服务器
5. 右键单击项目中的任意文件，选择 **"发送到设备"**

## 从源码构建

1. 克隆此仓库
2. 在 IntelliJ IDEA 中导入项目
3. 使用 Gradle 构建项目：

   ```bash
   ./gradlew buildPlugin
   ```

## 许可证

该项目是MIT许可证 - 有关详细信息，请参见许可证文件

## 感谢

[jetbrains sample](https://github.com/JetBrains/intellij-sdk-code-samples) 示例代码库
[https://github.com/wilinz/Auto.js-VSCode-Extension](https://github.com/wilinz/Auto.js-VSCode-Extension) 一个同功能的VSCode插件fork
