# 📶 WiFili Messenger (LanChat) • 100% Offline P2P LAN Chat & Calling App

[![Android](https://img.shields.io/badge/📱_ANDROID-3776AB?style=for-the-badge&logo=android&logoColor=white)](#-android-app-guide)
[![Windows](https://img.shields.io/badge/💻_WINDOWS-0078D6?style=for-the-badge&logo=windows&logoColor=white)](#-windows--linux-desktop-app-guide)
[![Linux](https://img.shields.io/badge/🐧_LINUX-FCC624?style=for-the-badge&logo=linux&logoColor=black)](#-windows--linux-desktop-app-guide)
[![Chrome Web](https://img.shields.io/badge/🌐_CHROME_WEB-4285F4?style=for-the-badge&logo=googlechrome&logoColor=white)](#-web-browser-edition-guide)
[![Downloads](https://img.shields.io/badge/📥_DOWNLOADS-1E65F3?style=for-the-badge&logo=github)](#-direct-downloads--releases)

**WiFili Messenger** (also known as **LanChat**) is a 100% offline, cross-platform, peer-to-peer (P2P) local area network (LAN) messaging, high-speed file sharing, and real-time audio/video calling application. It connects **Android smartphones, Windows PCs, Linux desktops, and Web Browsers** directly over local Wi-Fi routers or Mobile Hotspots without needing an internet connection, cloud databases, or third-party servers.

---

## 📥 Direct Downloads & Releases (Cross-Platform Packages)

| Platform / Device | Direct Download Link | Description & Included Installers | File Format | File Size |
| :--- | :--- | :--- | :--- | :--- |
| **📱 Android Smartphone App** | [📥 Download Android App (.apk)](https://github.com/MohsinTahir4/WiFili-Messenger/raw/main/releases/WiFili-Messenger-Android.apk) | **Android App**: Ready-to-install APK for all Android smartphones. | `.apk` | ~6.0 MB |
| **💻 Windows & Linux Desktop** | [📥 Download Desktop Complete Package (.zip)](https://github.com/MohsinTahir4/WiFili-Messenger/raw/main/releases/WiFili-Messenger-Desktop-Complete.zip) | **Desktop Package**: Contains Windows Installer script (`WiFili-Messenger-Windows-Installer.bat`), Linux installer script (`install-linux.sh`), Java app, and guides! | `.zip` | ~45 KB |
| **🌐 Web Browser Edition** | [📥 Download Web Edition Complete Package](https://github.com/MohsinTahir4/WiFili-Messenger/raw/main/releases/WiFili-Messenger-Web-Complete.zip) | **Web Package**: Contains Web Server, HTML5 Web UI, launchers, and Web instructions! | `.zip` | ~49 KB |

---

### 📖 Installation & Setup Guide for Each Platform

#### 💻 1. For Windows PC Users (`WiFili-Messenger-Desktop-Complete.zip`):
1. Download and extract **`WiFili-Messenger-Desktop-Complete.zip`**.
2. Double-click **`WiFili-Messenger-Windows-Installer.bat`** to install the app and create a **Desktop Shortcut** automatically!
3. Double-click the new **"WiFili Messenger"** shortcut on your Desktop whenever you want to chat offline.

#### 🐧 2. For Linux Desktop Users (`WiFili-Messenger-Desktop-Complete.zip`):
1. Extract **`WiFili-Messenger-Desktop-Complete.zip`** in terminal.
2. Run the Linux installer script:
   ```bash
   chmod +x install-linux.sh
   ./install-linux.sh
   ```
3. WiFili Messenger will be added to your Linux application menu and desktop automatically!

#### 🌐 3. For Web Browser Users in Chrome / Edge (`WiFili-Messenger-Web-Complete.zip`):
1. Download and extract **`WiFili-Messenger-Web-Complete.zip`**.
2. **Windows**: Double-click **`1-Click-Start-Web-Browser-App.bat`**.
3. **Linux**: Run `./run-web.sh`.
4. Chrome or Edge will open automatically at **`http://localhost:8080`**!

#### 📱 4. For Android Smartphone Users (`.apk`):
1. Download **`WiFili-Messenger-Android.apk`** on your phone.
2. Open the `.apk` file, allow *"Install unknown apps"*, and tap **Install**.
3. Launch **WiFili Messenger** and tap **`⚡ Scan LAN Users`** (or use **`+ IP`** to connect manually) to start communicating across platforms!

---

## 📱 Android App Guide

### Overview & Interface Design
The **WiFili Android App** features a modern WhatsApp/Telegram-style user interface built for high-speed offline local Wi-Fi communication.

| Feature | Description |
| :--- | :--- |
| **💬 Offline Chat** | Instant peer-to-peer messaging with built-in emoji selection. |
| **📄 File Sharing** | Send photos, videos, audio notes, and documents (PDF, DOCX, ZIP) with **live percentage progress bars (`0%-100%`)**. |
| **📞 Audio Calling** | Low-latency 16kHz PCM audio calls over UDP with Loudspeaker & Earpiece toggle. |
| **🎥 Video Calling** | Upright portrait video calling with front/back camera switching and local picture-in-picture preview. |
| **🎬 Video Player** | In-app video player to preview received videos directly. |

---

## 💻 Windows & Linux Desktop App Guide

### Overview & Interface Design
The **WiFili Desktop App** is a native Java Swing application (`:desktop` module) styled in a sleek dark slate theme that matches the web browser edition.

| Feature | Description |
| :--- | :--- |
| **🔍 Search & Discovery** | Real-time LAN user search sidebar displaying online status badges (`🟢 Online`) and `+ IP` manual connection. |
| **💬 PC Messaging** | Full desktop keyboard chat with custom emoji picker and file attachment dialogs. |
| **📞 Desktop Calls** | Complete audio & video call modal windows with live video preview, mute microphone, and hangup buttons. |

---

## 🌐 Web Browser Edition Guide

### Overview & Interface Design
The **WiFili Web Edition** runs locally in **Chrome, Edge, Firefox, or Safari** via an embedded HTTP Web Server running on port `8080`.

| Feature | Description |
| :--- | :--- |
| **⚡ Zero Installation** | Runs instantly inside any web browser without installing additional software. |
| **🎨 Dark & Light Theme** | Toggle between dark and light modes effortlessly. |
| **🔄 Live Peer Polling** | Automatically polls `/api/peers` every 2 seconds to list online Wi-Fi users. |

---

## 🌐 Offline Mesh Architecture

```mermaid
graph TD
    A["📱 Android Phone A<br>(192.168.1.102)"] <-->|Direct Local Wi-Fi| B["📱 Android Phone B<br>(192.168.1.104)"]
    A <-->|Direct Local Wi-Fi| C["💻 Windows/Linux Desktop<br>(192.168.1.111)"]
    B <-->|Direct Local Wi-Fi| D["🌐 Web Browser (Chrome/Edge)<br>(192.168.1.105:8080)"]
    C <-->|Direct Local Wi-Fi| D
```

---

## ✨ Feature Summary

| Feature | Description |
| :--- | :--- |
| **💬 Direct Messaging & Emojis** | Real-time chat with custom emoji selection. |
| **📄 High-Speed File Transfer** | Share photos, videos, audio notes, and documents (PDF, DOCX, ZIP) with percentage progress bars (`0% - 100%`). |
| **📞 Audio Calling** | Low-latency 16kHz PCM audio calls over UDP with Loudspeaker & Earpiece toggle. |
| **🎥 Video Calling** | Upright portrait video calling with front/back camera switching and local picture-in-picture preview. |
| **💻 Desktop App (Windows & Linux)** | Native Java Swing GUI app (`:desktop` module) matching the web design. |
| **🌐 Web Browser Edition** | HTML5 Web App ([web/index.html](web/index.html)) powered by an embedded HTTP Web Server. |
| **🔧 Connection Diagnostics** | Active IP status detection, network discovery scan, and step-by-step troubleshooting guides. |

---

## 🏗️ Technical Architecture & Network Ports

| Protocol | Port | Purpose |
| :--- | :--- | :--- |
| **UDP Broadcast** | `45454` | LAN Subnet Peer Discovery (`LANCHAT\|id\|name`) |
| **TCP Sockets** | `45455` | Text Messages & File Transfer Streams |
| **UDP Stream** | `45457` | Non-blocking 16kHz PCM Audio Calling |
| **UDP Stream** | `45458` | YUV JPEG Video Frame Calling |
| **HTTP Server** | `8080` | Embedded Web Server for Chrome/Edge access |

---

## 📶 How to Increase Wi-Fi Range with a Good Router

To get maximum range and coverage for WiFili Messenger across large homes, offices, or outdoor areas:

```mermaid
graph LR
    Sub1["📡 Central Router Location<br>(Elevated 2m+)"] --> Coverage1["🟢 2.4 GHz Band<br>(Long Range / Through Walls)"]
    Sub1 --> Coverage2["⚡ 5.0 GHz Band<br>(Ultra High Speed)"]
    Sub1 --> Mesh["📶 Wi-Fi Mesh Node<br>(TP-Link Deco / Netgear Orbi)"]
```

1. **Optimal Router Placement**: Place your Wi-Fi router in a central, elevated position (at least 1.5–2 meters high) away from thick concrete walls and metal objects.
2. **Wi-Fi Band Selection**: Use **2.4 GHz** for long range through walls, and **5.0 GHz** for ultra-fast video calling and large file transfers.
3. **Mesh Systems**: Use Mesh Wi-Fi routers (TP-Link Deco, Netgear Orbi, or ASUS Mesh) for seamless multi-story coverage.
4. **Mobile Hotspot**: Turn on Mobile Hotspot on one phone when outdoors; all other phones and laptops connect directly.

---

## 🚀 How to Build & Run

### 1. Android App
Build and install via Android Studio or Terminal:
```bash
./gradlew :app:assembleDebug
```

### 2. Desktop App (Windows & Linux)
- **Windows**: Double-click `run-desktop.bat` or run:
  ```cmd
  run-desktop.bat
  ```
- **Linux**:
  ```bash
  chmod +x run-desktop.sh
  ./run-desktop.sh
  ```

### 3. Web App Edition (Chrome / Edge / Firefox)
- **Windows**: Double-click `run-web.bat` or run:
  ```cmd
  run-web.bat
  ```
- **Linux**:
  ```bash
  chmod +x run-web.sh
  ./run-web.sh
  ```
Open `http://localhost:8080` or `http://<your-ip>:8080` in Chrome/Edge!

---

## ☕ Support & Donations (Coinbase & Binance)

If you find **WiFili Messenger** helpful and want to support open-source offline development, you can donate via Coinbase, Binance, or Cryptocurrency!

[![Coinbase](https://img.shields.io/badge/Donate-Coinbase-0052FF?style=for-the-badge&logo=coinbase&logoColor=white)](https://www.coinbase.com)
[![Binance](https://img.shields.io/badge/Donate-Binance-F0B90B?style=for-the-badge&logo=binance&logoColor=black)](https://www.binance.com)

- **Coinbase (ETH / USDC / BTC)**: `0xYourCoinbaseWalletAddressHere`
- **Binance (USDT / BNB / SOL)**: `YourBinanceDepositAddressHere`

---

## 📄 License
This project is released under the **MIT License**.
