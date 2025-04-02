# AutoXPlugin pour IntelliJ IDEA

[![official JetBrains project](https://jb.gg/badges/official-flat-square.svg)][jb:github]
[![JetBrains IntelliJ Platform SDK Docs](https://jb.gg/badges/docs.svg?style=flat-square)][jb:docs]

C'est un plugin IntelliJ IDEA pour développer des plugins complémentaires à AutoX.js. Si vous n'avez pas encore le programme AutoX.js, veuillez visiter [AutoX.js](https://github.com/aiselp/AutoX)

[🌟 English](README_en.md) | [🌏 中文](README.md) | [🌏 日本語](README_JP.md) | [🌏 한국인](README_ko.md) | [🌏 Русский](README_ru.md) | [🌏 Français](README_fr.md)
> [!TIP]
> Le débogage USB n'est pas encore pris en charge, seul le débogage LAN est supporté.

## Fonctionnalités

- Exécution d'un serveur WebSocket simple directement dans l'IDE
- Génération de codes QR pour faciliter la connexion des appareils mobiles
- Envoi de fichiers aux appareils connectés par clic droit
- Transfert de fichiers en temps réel via le réseau local
- Support multilingue, le plugin change automatiquement de langue après modification de la langue de l'IDE

## Captures d'écran
<div align="center">
<table>
<tr>

<td align="center">
<b>Barre d'outils</b><br>
<img src="/img/%E6%88%AA%E5%B1%8F2025-04-03%2000.43.22.png" width="500" alt="Barre d'outils"><br>
<small>Après avoir cliqué sur démarrer, le téléphone mobile peut scanner le code QR pour se connecter</small>
</td>
<td align="center">
<b>Clic droit sur dossier</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.57.png" width="500" alt="Clic droit sur dossier"><br>
<small>Cliquez avec le bouton droit sur un dossier pour choisir d'envoyer ou d'exécuter sur les appareils connectés</small>
</td>
<td align="center">
<b>Clic droit sur fichier JS</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.40.39.png" width="500" alt="Clic droit sur fichier JS"><br>
<em>Cliquez avec le bouton droit sur un script autox.js valide pour enregistrer, exécuter, relancer ou arrêter sur les appareils connectés</em>
</td>
<td align="center">
<b>Clic droit sur fichier JSON</b><br>
<img src="img/%E6%88%AA%E5%B1%8F2025-04-02%2017.41.36.png" width="500" alt="Clic droit sur fichier JSON"><br>
<em>Cliquez avec le bouton droit sur un fichier JSON valide pour enregistrer son répertoire sur l'appareil</em>
</td>
</tr>
</table>
</div>

## Explication détaillée des fonctionnalités

Comme un projet autox.js valide contient un fichier project.json, vous pouvez effectuer l'opération "Exécuter sur l'appareil" sur les dossiers contenant un fichier project.json.

- Enregistrer le projet sur l'appareil : Envoie le dossier sélectionné par clic droit à l'appareil connecté. Après une opération réussie, vous pouvez voir le fichier sur l'appareil mobile.
- Exécuter le projet sur l'appareil : Envoie le dossier sélectionné par clic droit à l'appareil connecté. Après une opération réussie, il peut être exécuté sur l'appareil mobile, mais le fichier ne sera pas enregistré sur l'appareil.
- Enregistrer le script sur l'appareil : Envoie le fichier unique sélectionné par clic droit à l'appareil connecté. Après une opération réussie, vous pouvez voir le fichier sur l'appareil mobile.
- Exécuter le script sur l'appareil : Envoie le fichier unique sélectionné par clic droit à l'appareil connecté. Après une opération réussie, il peut être exécuté sur l'appareil mobile, mais le fichier ne sera pas enregistré sur l'appareil.
- Relancer le script sur l'appareil : Envoie le fichier unique sélectionné par clic droit à l'appareil connecté. Après une opération réussie, il arrêtera d'abord le programme précédemment en cours d'exécution, puis s'exécutera sur l'appareil mobile.
- Arrêter le script sur l'appareil : Après une opération réussie, il arrêtera le programme du même nom en cours d'exécution sur l'appareil mobile.

## Installation

1. Téléchargez la dernière version depuis la page des Releases
2. Dans IntelliJ IDEA, allez dans `Paramètres` > `Plugins` > `Installer le plugin depuis le disque...`
3. Sélectionnez le fichier `.zip` téléchargé
4. Redémarrez IntelliJ IDEA

## Utilisation

1. Ouvrez la fenêtre d'outil **AutoXPlugin** dans la barre latérale droite
2. Cliquez sur **"Démarrer le serveur"** pour lancer le serveur WebSocket
3. Scannez le code QR avec votre appareil mobile
4. Connectez-vous au serveur sur votre appareil
5. Cliquez avec le bouton droit sur n'importe quel fichier de votre projet et sélectionnez **"Envoyer à l'appareil"**

## Compilation depuis les sources

1. Clonez ce dépôt
2. Importez le projet dans IntelliJ IDEA
3. Compilez le projet avec Gradle :

   ```bash
   ./gradlew buildPlugin
   ```

## Licence

Ce projet est sous licence MIT - voir le fichier LICENSE pour plus de détails.

## Remerciements

Merci à
[jetbrains sample](https://github.com/JetBrains/intellij-sdk-code-samples) dépôt d'exemples de code
[https://github.com/wilinz/Auto.js-VSCode-Extension](https://github.com/wilinz/Auto.js-VSCode-Extension) plugin VSCode avec des fonctionnalités similaires 