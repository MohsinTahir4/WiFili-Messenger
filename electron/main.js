const { app, BrowserWindow, ipcMain } = require('electron');
const path = require('path');
const dgram = require('dgram');
const net = require('net');
const http = require('http');

const DISCOVERY_PORT = 45454;
const CHAT_PORT = 45455;
const WEB_PORT = 8080;

let mainWindow;
let deviceId = Math.random().toString(36).substring(2, 10);
let osName = process.platform === 'win32' ? 'Win-User-' : process.platform === 'linux' ? 'Linux-User-' : 'Desktop-User-';
let deviceName = osName + deviceId;
let peers = new Map();
let messageHistory = [];

function createWindow() {
    mainWindow = new BrowserWindow({
        width: 1080,
        height: 720,
        webPreferences: {
            nodeIntegration: true,
            contextIsolation: false
        },
        autoHideMenuBar: true
    });

    // Load Web Edition UI
    const indexPath = path.join(__dirname, '../web/index.html');
    mainWindow.loadFile(indexPath);
}

// Start Embedded HTTP Server for Web Edition API compatibility
function startWebServer() {
    const server = http.createServer((req, res) => {
        res.setHeader('Access-Control-Allow-Origin', '*');
        res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
        res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

        if (req.url === '/api/peers') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            // Prune peers not seen in 15 seconds
            const now = Date.now();
            for (let [id, p] of peers.entries()) {
                if (now - p.lastSeen > 15000) peers.delete(id);
            }
            res.end(JSON.stringify(Array.from(peers.values())));
        } else if (req.url === '/api/messages') {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify(messageHistory));
        } else if (req.url === '/api/send' && req.method === 'POST') {
            let body = '';
            req.on('data', chunk => { body += chunk; });
            req.on('end', () => {
                try {
                    const data = JSON.parse(body);
                    if (data.ip && data.text) {
                        sendTcpMessage(data.ip, data.text);
                        messageHistory.push({ sender: 'You', body: data.text, type: 'sent', time: 'Now' });
                    }
                    res.writeHead(200, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ status: 'ok' }));
                } catch (e) {
                    res.writeHead(400, { 'Content-Type': 'application/json' });
                    res.end(JSON.stringify({ error: e.message }));
                }
            });
        } else {
            res.writeHead(404);
            res.end('Not Found');
        }
    });

    server.listen(WEB_PORT, '0.0.0.0', () => {
        console.log(`🚀 Electron Web Server running at http://localhost:${WEB_PORT}`);
    });
}

// Start UDP Discovery
function startDiscovery() {
    const socket = dgram.createSocket({ type: 'udp4', reuseAddr: true });

    socket.on('error', (err) => {
        socket.close();
    });

    socket.on('message', (msg, rinfo) => {
        const message = msg.toString();
        const parts = message.split('|');
        if (parts.length === 3 && parts[1] !== deviceId) {
            peers.set(parts[1], {
                id: parts[1],
                name: parts[2],
                ip: rinfo.address,
                status: 'Online',
                lastSeen: Date.now()
            });
        }
    });

    socket.bind(DISCOVERY_PORT, () => {
        socket.setBroadcast(true);
    });

    // Announce every 2 seconds
    setInterval(() => {
        const message = Buffer.from(`LANCHAT|${deviceId}|${deviceName}`);
        socket.send(message, 0, message.length, DISCOVERY_PORT, '255.255.255.255', (err) => {});
    }, 2000);
}

// Start TCP Server for Messages & Files
function startTcpServer() {
    const server = net.createServer((socket) => {
        let chunks = [];
        socket.on('data', (chunk) => {
            chunks.push(chunk);
        });
        socket.on('end', () => {
            const buffer = Buffer.concat(chunks);
            if (buffer.length > 4) {
                const type = buffer.readInt32BE(0);
                const text = buffer.toString('utf8', 4);
                messageHistory.push({ sender: 'Peer', body: text, type: 'received', time: 'Now' });
            }
        });
    });

    server.listen(CHAT_PORT, () => {
        console.log(`💬 TCP Chat Server running on port ${CHAT_PORT}`);
    });
}

function sendTcpMessage(destIp, text) {
    const client = new net.Socket();
    client.connect(CHAT_PORT, destIp, () => {
        const buf = Buffer.alloc(4 + text.length + 10);
        buf.writeInt32BE(1, 0); // type 1 = text
        buf.write(text, 4, 'utf8');
        client.write(buf);
        client.end();
    });
    client.on('error', (err) => {
        console.error('TCP Send Error:', err);
    });
}

app.whenReady().then(() => {
    createWindow();
    startWebServer();
    startDiscovery();
    startTcpServer();

    app.on('activate', () => {
        if (BrowserWindow.getAllWindows().length === 0) createWindow();
    });
});

app.on('window-all-closed', () => {
    if (process.platform !== 'darwin') app.quit();
});
