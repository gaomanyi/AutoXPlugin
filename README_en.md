# AutoXPlugin for IntelliJ IDEA
<!-- Plugin description -->
![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)
![JetBrains IntelliJ Platform SDK Docs](https://jb.gg/badges/docs.svg?style=flat-square)

This is an IntelliJ IDEA plugin for developing AutoX.js companion plugins. If you don't have the AutoX.js program yet, please visit [AutoX.js](https://github.com/aiselp/AutoX)

[🌟 English](README_en.md) | [🌏 中文](README.md) | [🌏 日本語](README_JP.md) | [🌏 한국인](README_ko.md) | [🌏 Русский](README_ru.md) | [🌏 Français](README_fr.md)
> [!TIP]
> USB debugging development is not supported yet, only LAN debugging is supported

## Features

- Run a simple WebSocket server directly in the IDE
- Generate QR codes for easy connection with mobile devices
- Right-click any file to send it to connected devices
- Real-time file transfer over the local network
- Multi-language support, the plugin will automatically switch languages after changing the IDE language

<!-- Plugin description end -->

## Screenshots
<div align="center">
<table>
<tr>

<td align="center">
<b>Toolbar</b><br>
<img src="/img/%E6%88%AA%E5%B1%8F2025-04-03%2000.43.22.png" width="500" alt="Toolbar"><br>
<small>After clicking start, the mobile phone can scan the QR code to connect</small>
</td>
<td align="center">
<b>Folder Right-Click</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.57.png" width="500" alt="Folder Right-Click"><br>
<small>Right-click on a folder to choose to send or run to connected devices</small>
</td>
<td align="center">
<b>JS File Right-Click</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.39.png" width="500" alt="JS File Right-Click"><br>
<em>Right-click on a valid autox.js script to save, run, rerun, or stop on connected devices</em>
</td>
<td align="center">
<b>JSON File Right-Click</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.41.36.png" width="500" alt="JSON File Right-Click"><br>
<em>Right-click on a valid JSON file to save its directory to the device</em>
</td>
</tr>
</table>
</div>

## Detailed Feature Explanation

Because a valid autox.js project file contains a project.json file, you can perform the "Run to Device" operation on folders containing project.json file.

- Save Project to Device: Send the right-clicked folder to the connected device. After successful operation, you can see the file on the mobile device
- Run Project to Device: Send the right-clicked folder to the connected device. After successful operation, it can run on the mobile device, but the file will not be saved to the device
- Save Script to Device: Send the right-clicked single file to the connected device. After successful operation, you can see the file on the mobile device
- Run Script to Device: Send the right-clicked single file to the connected device. After successful operation, it can run on the mobile device, but the file will not be saved to the device
- Rerun Script to Device: Send the right-clicked single file to the connected device. After successful operation, it will first stop the previously running program and then run on the mobile device
- Stop Script on Device: After successful operation, it will stop the program with the same name running on the mobile device

## Installation

1. Download the latest version from the Releases page
2. In IntelliJ IDEA, go to `Settings` > `Plugins` > `Install Plugin from Disk...`
3. Select the downloaded `.zip` file
4. Restart IntelliJ IDEA

## Usage

1. Open the **AutoXPlugin** tool window on the right sidebar
2. Click **"Start Server"** to start the WebSocket server
3. Scan the QR code with your mobile device
4. Connect to the server on your device
5. Right-click any file in your project and select **"Send to Device"**

## Building from Source

1. Clone this repository
2. Import the project in IntelliJ IDEA
3. Build the project using Gradle:

   ```bash
   ./gradlew buildPlugin
   ```

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

Thanks to
[jetbrains sample](https://github.com/JetBrains/intellij-sdk-code-samples) example code repository

[https://github.com/wilinz/Auto.js-VSCode-Extension](https://github.com/wilinz/Auto.js-VSCode-Extension) a VSCode plugin with similar functionality 