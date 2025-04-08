# IntelliJ IDEA用のAutoXPlugin
<!-- Plugin description -->
![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)
![JetBrains IntelliJ Platform SDK Docs](https://jb.gg/badges/docs.svg?style=flat-square)

これはAutoX.jsの連携プラグインを開発するためのIntelliJ IDEAプラグインです。AutoX.jsプログラムをまだお持ちでない場合は、[AutoX.js](https://github.com/aiselp/AutoX)をご覧ください。

[🌟 English](README_en.md) | [🌏 中文](README.md) | [🌏 日本語](README_JP.md) | [🌏 한국인](README_ko.md) | [🌏 Русский](README_ru.md) | [🌏 Français](README_fr.md)
> [!TIP]
> USBデバッグ開発はまだサポートされておらず、LANデバッグのみサポートされています。

## 機能

- IDE内で直接シンプルなWebSocketサーバーを実行
- モバイルデバイスの接続を容易にするQRコードの生成
- 任意のファイルを右クリックして接続されたデバイスに送信
- ローカルネットワークを介したリアルタイムのファイル転送
- 多言語サポート、IDEの言語を変更するとプラグインが自動的に言語を切り替える

<!-- Plugin description end -->
## スクリーンショット
<div align="center">
<table>
<tr>

<td align="center">
<b>ツールバー</b><br>
<img src="/img/%E6%88%AA%E5%B1%8F2025-04-03%2000.43.22.png" width="500" alt="ツールバー"><br>
<small>起動ボタンをクリックした後、モバイルでQRコードをスキャンして接続できます</small>
</td>
<td align="center">
<b>フォルダの右クリック</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.57.png" width="500" alt="フォルダの右クリック"><br>
<small>フォルダを右クリックして、接続されたデバイスに送信または実行するかを選択できます</small>
</td>
<td align="center">
<b>JSファイルの右クリック</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.39.png" width="500" alt="JSファイルの右クリック"><br>
<em>有効なautox.jsスクリプトを右クリックして、接続されたデバイスで保存、実行、再実行、または停止することができます</em>
</td>
<td align="center">
<b>JSONファイルの右クリック</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.41.36.png" width="500" alt="JSONファイルの右クリック"><br>
<em>有効なJSONファイルを右クリックして、そのディレクトリをデバイスに保存できます</em>
</td>
</tr>
</table>
</div>

## 機能の詳細説明

有効なautox.jsプロジェクトファイルにはproject.jsonファイルが含まれているため、project.jsonファイルを含むフォルダに対して「デバイスで実行」操作を実行できます。

- プロジェクトをデバイスに保存: 右クリックしたフォルダを接続されたデバイスに送信します。操作が成功すると、モバイルデバイスでファイルを確認できます。
- プロジェクトをデバイスで実行: 右クリックしたフォルダを接続されたデバイスに送信します。操作が成功すると、モバイルデバイスで実行できますが、ファイルはデバイスに保存されません。
- スクリプトをデバイスに保存: 右クリックした単一ファイルを接続されたデバイスに送信します。操作が成功すると、モバイルデバイスでファイルを確認できます。
- スクリプトをデバイスで実行: 右クリックした単一ファイルを接続されたデバイスに送信します。操作が成功すると、モバイルデバイスで実行できますが、ファイルはデバイスに保存されません。
- スクリプトをデバイスで再実行: 右クリックした単一ファイルを接続されたデバイスに送信します。操作が成功すると、まず以前実行中のプログラムを停止してから、モバイルデバイスで実行します。
- デバイスでスクリプトを停止: 操作が成功すると、モバイルデバイスで実行中の同名のプログラムを停止します。

## インストール

1. Releasesページから最新バージョンをダウンロードします。
2. IntelliJ IDEAで、`設定` > `プラグイン` > `ディスクからプラグインをインストール...`に進みます。
3. ダウンロードした`.zip`ファイルを選択します。
4. IntelliJ IDEAを再起動します。

## 使用方法

1. 右サイドバーで**AutoXPlugin**ツールウィンドウを開きます。
2. WebSocketサーバーを起動するには**「サーバーを起動」**をクリックします。
3. モバイルデバイスでQRコードをスキャンします。
4. デバイスでサーバーに接続します。
5. プロジェクト内の任意のファイルを右クリックし、**「デバイスに送信」**を選択します。

## ソースからのビルド

1. このリポジトリをクローンします。
2. IntelliJ IDEAでプロジェクトをインポートします。
3. Gradleを使用してプロジェクトをビルドします：

   ```bash
   ./gradlew buildPlugin
   ```

## ライセンス

このプロジェクトはMITライセンスの下でライセンスされています。詳細はLICENSEファイルをご覧ください。

## 謝辞

以下に感謝します：
[jetbrains sample](https://github.com/JetBrains/intellij-sdk-code-samples) サンプルコードリポジトリ

[https://github.com/wilinz/Auto.js-VSCode-Extension](https://github.com/wilinz/Auto.js-VSCode-Extension) 同様の機能を持つVSCodeプラグイン 