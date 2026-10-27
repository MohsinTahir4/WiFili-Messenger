# 📶 WiFili Messenger (LanChat)

![Android](https://img.shields.io/badge/Android-3776AB?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/Java_17-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)
![Windows](https://img.shields.io/badge/Windows-0078D6?style=for-the-badge&logo=windows&logoColor=white)
![Linux](https://img.shields.io/badge/Linux-FCC624?style=for-the-badge&logo=linux&logoColor=black)
![Chrome](https://img.shields.io/badge/Chrome_Web-4285F4?style=for-the-badge&logo=googlechrome&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-green.style=for-the-badge)

**WiFili Messenger** is a powerful, 100% offline, peer-to-peer messaging, file sharing, and real-time audio/video calling system designed to work seamlessly across **Android, Windows, Linux, and Web Browsers** over local Wi-Fi or Mobile Hotspot networks without needing an internet connection.

---

## 🌟 Key Features

- **💬 Real-Time Messaging & Emojis**: Instant peer-to-peer chat with built-in emoji selection.
- **📄 High-Speed File Transfer**: Share photos, videos, audio notes, and documents (PDF, DOCX, ZIP) with real-time percentage progress bars (`0% - 100%`).
- **📞 Audio Calling**: Low-latency, crystal-clear voice calls over UDP with Loudspeaker / Earpiece toggle and ringing alerts.
- **🎥 Video Calling**: Real-time video calling with upright portrait orientation, front/back camera switching, and local picture-in-picture preview.
- **💻 Desktop App (Windows & Linux)**: Native Java Swing desktop application (`:desktop` module) for PC & laptop users.
- **🌐 Web Edition (Chrome / Edge / Firefox)**: Built-in offline Web App ([web/index.html](web/index.html)) powered by an embedded HTTP Web Server.
- **🔧 Network Diagnostics & Help**: Live IP status detection, active LAN discovery scan, and step-by-step connection troubleshooting guides.

---

## 🏗️ Technical Architecture & Network Ports

| Feature | Protocol | Port | Description |
| :--- | :--- | :--- | :--- |
| **Peer Discovery** | `UDP Broadcast` | `45454` | LAN subnet broadcast announcement (`LANCHAT\|id\|name`) |
| **Messaging & Files** | `TCP Sockets` | `45455` | Reliable TCP sockets for chat messages and media files |
| **Audio Calling** | `UDP Stream` | `45457` | 16kHz PCM audio streaming with non-blocking packets |
| **Video Calling** | `UDP Stream` | `45458` | Compressed YUV JPEG video frame streaming |
| **Web Server** | `HTTP` | `8080` | Embedded HTTP web server for web browser access |

---

## 🚀 How to Run & Build

### 1. Android App (`:app`)
Open the project in **Android Studio** and click **Run**, or build via terminal:
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

## 📤 How to Upload to Your GitHub Account

Follow these steps to upload this repository to your GitHub account:

### Step 1: Create a Repository on GitHub
1. Go to [GitHub New Repository](https://github.com/new).
2. Enter Repository Name: `WiFili-Messenger` (or `LanChat`).
3. Set visibility to **Public** or **Private**.
4. Leave "Add a README file" **unchecked** (since we already have a README).
5. Click **Create repository**.

### Step 2: Push Local Code to GitHub
Open Command Prompt or Terminal in the project root folder and run:

```bash
# 1. Add all project files
git add .

# 2. Create initial commit
git commit -m "Initial commit of WiFili Messenger - Android, Desktop & Web"

# 3. Rename branch to main
git branch -M main

# 4. Link your GitHub repository (replace URL with your GitHub repo URL)
git remote add origin https://github.com/YOUR_USERNAME/WiFili-Messenger.git

# 5. Push code to GitHub
git push -u origin main
```

---

## 🔄 How to Push Future Updates to GitHub

Whenever you make new changes or updates to the project, run these 3 simple commands to update GitHub:

```bash
# 1. Stage all new and modified files
git add .

# 2. Commit changes with a description
git commit -m "Updated features and UI fixes"

# 3. Push updates to GitHub
git push
```

---

## 📄 License
This project is open-source under the **MIT License**.
