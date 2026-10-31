<!-- Layout Version: 23.0 - WiFili Messenger Android & Web Edition -->
# 📶 WiFili Messenger • 100% Offline P2P LAN Chat & File Sharing (Android & Web)

[![Android](https://img.shields.io/badge/📱_ANDROID-3776AB?style=for-the-badge&logo=android&logoColor=white)](#-android-app-guide--source-and-binaries)
[![Web Edition](https://img.shields.io/badge/🌐_WEB_EDITION-4285F4?style=for-the-badge&logo=googlechrome&logoColor=white)](#-web-browser-edition-guide--source-and-binaries)
[![Downloads](https://img.shields.io/badge/📥_DOWNLOADS-1E65F3?style=for-the-badge&logo=github)](#-direct-downloads--releases)

**WiFili Messenger** is a 100% offline, peer-to-peer (P2P) local area network (LAN) messaging, high-speed file sharing, and audio/video calling application. It connects **Android smartphones, tablets, and Web Browsers** directly over local Wi-Fi routers or Mobile Hotspots without needing an internet connection, cloud databases, or cellular towers.

---

## 📥 Direct Downloads & Releases (Android APK & Web Edition)

| Edition / Platform | Direct Download Link | Description & Included Files | File Format | File Size |
| :--- | :--- | :--- | :--- | :--- |
| **📱 Android Smartphone** | [📥 Download Android App (.apk)](https://github.com/MohsinTahir4/WiFili-Messenger/raw/main/releases/WiFili-Messenger-Android.apk) | **Android Binary**: Ready-to-install APK for all Android smartphones and tablets. | `.apk` | ~6.0 MB |
| **🌐 Web Browser Edition** | [📥 Download Web Edition Complete (.zip)](https://github.com/MohsinTahir4/WiFili-Messenger/raw/main/releases/WiFili-Messenger-Complete-Source-And-Binaries.zip) | **Web Source & Binaries Complete**: Contains full project source code, Gradle build system, HTTP Web Server binary (`WiFili-WebServer.jar`), HTML5 UI (`web/index.html`), and all offline dependencies. | `.zip` | **~45.4 MB** |

---

## 🏕️ Setup Your Local Wifi Network where Internet Unavailable Can Communicate Offline

Because WiFili Messenger uses **local Wi-Fi radio waves** rather than internet infrastructure, you can set up a fully functioning communication network anywhere on Earth:

```mermaid
graph TD
    A["Phone A (User 1)"] -->|"Wi-Fi Radio P2P"| B["Phone B (User 2)"]
    A -->|"Wi-Fi Radio P2P"| C["Portable Wi-Fi Travel Router"]
    B -->|"Wi-Fi Radio P2P"| C
    D["Laptop / Tablet (Web Edition)"] -->|"Wi-Fi Radio P2P"| C
```

### Setup Instructions for Off-Grid Environments:
1. **Primary Hotspot Phone / Portable Router**:
   - Take any Android smartphone or a battery-powered portable Wi-Fi travel router (e.g., GL.iNet, TP-Link MR3040).
   - Turn on **Mobile Hotspot** (or turn on the travel router). *Note: You do NOT need cellular data or a SIM card with internet; just the Wi-Fi hotspot feature.*
2. **Connect All Devices**:
   - Connect all other smartphones, tablets, and laptops to that same Wi-Fi Hotspot / Router network.
3. **Launch WiFili Messenger**:
   - Open the Android app or open `http://<router-ip>:8080` in any web browser.
   - Instantly chat, send high-speed files (photos, videos, documents), make audio calls, and video call locally with zero internet!

---

## 📡 How to Extend Wi-Fi Range up to 5km or More (Long-Range Outdoor CPE Setup)

Standard mobile hotspots or home routers have a range of about 15–30 meters. To extend your Wi-Fi range up to **5km, 10km, or even farther** across remote areas, use **Long-Range Outdoor CPE Wireless Bridges (Point-to-Point / PTP)**:

```mermaid
graph LR
    Base["Base Camp Router"] -->|"Ethernet Cable"| CPE1["Outdoor CPE Router A"]
    CPE1 ==>|"Directional Beam (Up to 5km)"| CPE2["Outdoor CPE Router B"]
    CPE2 -->|"Ethernet Cable"| AP["Remote Access Point"]
    AP --> RemoteUsers["Remote Phones & Laptops"]
```

### Hardware Equipment Needed:
- **2x Outdoor CPE Units** (e.g., *Ubiquiti NanoStation Loco M5*, *TP-Link CPE510*, or *TP-Link CPE210*). These cost around $40–$60 each and operate on 24V Power-over-Ethernet (PoE).
- **Portable Battery Pack / Solar Generator** (for off-grid power).
- **Ethernet Cables**.

### Step-by-Step 5km+ Range Extension Guide:
1. **At Base Camp (Transmitter)**:
   - Connect your Wi-Fi router or hotspot to **Outdoor CPE Unit A** via an Ethernet cable.
   - Mount CPE Unit A on a pole or mast pointing directly toward the remote location.
2. **At Remote Camp (Receiver)**:
   - Mount **Outdoor CPE Unit B** pointing back toward Base Camp (requires clear **Line of Sight / LOS** with no major blocking mountains or thick obstructions).
   - Connect CPE Unit B via Ethernet to a local Wi-Fi Access Point or travel router.
3. **Result**:
   - The two CPE units form an invisible wireless bridge over the air. Any device connecting to the remote Wi-Fi access point is instantly bridged directly into your local Wi-Fi network, allowing users **5km+ away** to open WiFili Messenger and communicate in real-time!

---

## 🌐 Web Browser Edition Guide (Java Required)

### 📦 Setup & Run Instructions
The Web Edition includes a pre-compiled **Web Server Binary** (`WiFili-WebServer.jar`) which serves the modern HTML5 web client directly from your computer to any browser.

**Prerequisites**: You must have **Java 17 or higher** installed on your computer (Windows, macOS, or Linux).

1. Download the **Web Edition Complete (.zip)** from the downloads section above.
2. Extract the ZIP folder on your computer.
3. Open a Terminal (macOS/Linux) or Command Prompt (Windows) inside the extracted folder.
4. Run the server using Java:
   ```bash
   java -jar web/build/libs/WiFili-WebServer.jar
   ```
5. Once running, open your favorite web browser (Chrome, Edge, Safari, Firefox) and navigate to **`http://localhost:8080`**.
   *Note: Other devices connected to your Wi-Fi or Hotspot can also access this Web Edition by entering your computer's local IP address (e.g., `http://192.168.1.100:8080`).*

### 🛠️ Source Code
- **Full Web Server Source**: [`web/src/main/java/com/example/lanchat/web/WebServer.java`](web/src/main/java/com/example/lanchat/web/WebServer.java)
- **Web UI HTML5 Source**: [`web/src/main/resources/web/index.html`](web/src/main/resources/web/index.html)

---

## 📱 Android App Guide (Source & Binary Complete)

### 📦 Binary (`.apk`)
- Download [`WiFili-Messenger-Android.apk`](https://github.com/MohsinTahir4/WiFili-Messenger/raw/main/releases/WiFili-Messenger-Android.apk) and install directly on Android devices.

### 🛠️ Source Code
- **Source Location**: [`app/src/main/java/com/example/lanchat/`](app/src/main/java/com/example/lanchat/)
- **Build APK via Gradle**:
  ```bash
  ./gradlew :app:assembleDebug
  ```

---

## 🏗️ Technical Architecture & Network Ports

| Protocol | Port | Purpose |
| :--- | :--- | :--- |
| **UDP Broadcast** | `45454` | LAN Subnet Peer Discovery (`LANCHAT\|id\|name`) |
| **TCP Sockets** | `45455` | Text Messages & File Transfer Streams |
| **UDP Stream** | `45457` | Non-blocking 16kHz PCM Audio Calling |
| **UDP Stream** | `45458` | YUV JPEG Video Frame Calling |
| **HTTP Server** | `8080` | Embedded Web Server for Browser access |

---

## ☕ Support & Donations (Binance)

If you find **WiFili Messenger** helpful for off-grid or local communications, you can support this open-source project by donating via Binance!

### Donate via Binance App

You can send your support directly using my Binance Pay ID or by scanning the QR code below:

<img src="assets/binance_qr.jpg" alt="Binance QR Code" width="300" />

**Binance Pay ID**: `Mohsin Tahir12`

*(Open your Binance App -> Go to Pay -> Send -> Select Pay ID and enter `Mohsin Tahir12`)*

---

## 📄 License
This project is released under the **MIT License**.
