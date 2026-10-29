#!/bin/bash
echo "===================================================="
echo "📶 Installing WiFili Messenger on Linux..."
echo "===================================================="

INSTALL_DIR="$HOME/.local/share/wifili-messenger"
mkdir -p "$INSTALL_DIR"

if [ -f "WiFili-Messenger-Desktop.jar" ]; then
    cp WiFili-Messenger-Desktop.jar "$INSTALL_DIR/"
fi
if [ -f "run-desktop.sh" ]; then
    cp run-desktop.sh "$INSTALL_DIR/"
    chmod +x "$INSTALL_DIR/run-desktop.sh"
fi
if [ -d "web" ]; then
    cp -r web "$INSTALL_DIR/"
fi

DESKTOP_DIR="$HOME/.local/share/applications"
mkdir -p "$DESKTOP_DIR"

cat << EOF > "$DESKTOP_DIR/wifili-messenger.desktop"
[Desktop Entry]
Name=WiFili Messenger
Comment=100% Offline P2P LAN Chat & Calling
Exec=$INSTALL_DIR/run-desktop.sh
Terminal=false
Type=Application
Categories=Network;Chat;
EOF

chmod +x "$DESKTOP_DIR/wifili-messenger.desktop"

echo "✅ Installation Complete!"
echo "WiFili Messenger has been added to your Linux application menu and desktop."
