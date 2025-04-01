# AutoXPlugin for IntelliJ IDEA

A powerful plugin for IntelliJ IDEA that allows for easy file transfer to devices on your local network via WebSocket.
这是一个IntelliJ IDEA插件，用于开发AutoX.js的配套插件,如果你还没有AutoX.js程序,请访问[AutoX.js](https://github.com/aiselp/AutoX)
## Features

- Simple WebSocket server that runs directly in your IDE
- QR code generation for easy connection from mobile devices
- Right-click on any file to send it to connected devices
- Real-time file transfer over local network

## Installation

1. Download the latest release from the Releases page
2. In IntelliJ IDEA, go to `Settings` > `Plugins` > `Install Plugin from Disk...`
3. Select the downloaded .zip file
4. Restart IntelliJ IDEA

## Usage

1. Open the AutoXPlugin tool window from the right sidebar
2. Click "Start Server" to start the WebSocket server
3. Scan the QR code with your mobile device
4. Connect to the server from your device
5. Right-click on any file in your project and select "Send to Device"

## Building from Source

1. Clone this repository
2. Import the project into IntelliJ IDEA
3. Build the project using Gradle:

```bash
./gradlew buildPlugin
```

The plugin ZIP will be available in `build/distributions/`

## Requirements

- IntelliJ IDEA 2024.1 or newer
- Java 17 or newer

## License

This project is licensed under the MIT License - see the LICENSE file for details. 