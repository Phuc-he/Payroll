import express from 'express';
import cors from 'cors';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import qrcodeTerminal from 'qrcode-terminal';
import { Zalo, ThreadType, LoginQRCallbackEventType } from 'zca-js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = process.env.PORT || 5001;
const SESSION_FILE = path.join(__dirname, 'session.json');

const app = express();
app.use(cors());
app.use(express.json());

let apiInstance = null;
let currentQRImage = null;
let currentQRRawCode = null;
let botUserInfo = null;
let isLoggingIn = false;
let friendsCache = [];

function cleanPhoneNumber(phone) {
    if (!phone) return '';
    let p = phone.trim().replace(/\s+/g, '').replace(/[-().]/g, '');
    if (p.startsWith('+84')) {
        p = '0' + p.substring(3);
    } else if (p.startsWith('84') && p.length > 9) {
        p = '0' + p.substring(2);
    }
    return p;
}

async function refreshFriends() {
    if (!apiInstance) return;
    try {
        friendsCache = await apiInstance.getAllFriends(1000, 1);
        console.log(`[Zalo Gateway] Đã đồng bộ ${friendsCache.length} bạn bè từ tài khoản Zalo.`);
    } catch (err) {
        console.warn('[Zalo Gateway] Không thể lấy danh sách bạn bè:', err.message);
    }
}

async function initZalo() {
    if (isLoggingIn) return;
    isLoggingIn = true;
    const zalo = new Zalo();

    // 1. Thử login bằng session đã lưu
    if (fs.existsSync(SESSION_FILE)) {
        try {
            console.log('[Zalo Gateway] Đang khôi phục phiên đăng nhập từ session.json...');
            const sessionData = JSON.parse(fs.readFileSync(SESSION_FILE, 'utf-8'));
            apiInstance = await zalo.login(sessionData);
            botUserInfo = await apiInstance.fetchAccountInfo();
            console.log(`[Zalo Gateway] Đăng nhập thành công với tài khoản: ${botUserInfo?.name || 'Admin'}`);
            currentQRImage = null;
            currentQRRawCode = null;
            isLoggingIn = false;
            await refreshFriends();
            return;
        } catch (err) {
            console.warn('[Zalo Gateway] Session cũ hết hạn hoặc không hợp lệ. Đang chuyển sang quét mã QR:', err.message);
            try { fs.unlinkSync(SESSION_FILE); } catch (e) {}
        }
    }

    // 2. Nếu chưa có session hoặc session hỏng -> Khởi tạo Login QR
    try {
        console.log('[Zalo Gateway] Đang tạo mã QR đăng nhập Zalo...');
        apiInstance = await zalo.loginQR({}, (event) => {
            if (event.type === LoginQRCallbackEventType.QRCodeGenerated) {
                const rawImg = event.data.image || '';
                currentQRImage = rawImg.startsWith('data:') ? rawImg : `data:image/png;base64,${rawImg}`;
                currentQRRawCode = event.data.code;
                console.log('\n======================================================');
                console.log('👉 HÃY MỞ ZALO TRÊN ĐIỆN THOẠI VÀ QUÉT MÃ QR DƯỚI ĐÂY:');
                console.log(`Hoặc xem trên trình duyệt tại: http://localhost:${PORT}/qr`);
                console.log('======================================================\n');
                try {
                    qrcodeTerminal.generate(event.data.code, { small: true });
                } catch (qrErr) {
                    console.log('(Không thể in QR ASCII ra console, vui lòng mở link /qr trên trình duyệt)');
                }
            } else if (event.type === LoginQRCallbackEventType.QRCodeScanned) {
                console.log(`[Zalo Gateway] Đã quét QR bởi: ${event.data.display_name}. Vui lòng nhấn "Đăng nhập" trên điện thoại...`);
            } else if (event.type === LoginQRCallbackEventType.QRCodeExpired) {
                console.log('[Zalo Gateway] Mã QR đã hết hạn, đang tạo mã mới...');
                event.actions?.retry();
            } else if (event.type === LoginQRCallbackEventType.GotLoginInfo) {
                console.log('[Zalo Gateway] Đã nhận thông tin đăng nhập! Đang lưu session...');
                try {
                    fs.writeFileSync(SESSION_FILE, JSON.stringify(event.data, null, 2), 'utf-8');
                } catch (fsErr) {
                    console.error('[Zalo Gateway] Lỗi ghi session.json:', fsErr.message);
                }
            }
        });

        botUserInfo = await apiInstance.fetchAccountInfo();
        console.log(`\n🎉 [Zalo Gateway] ĐĂNG NHẬP THÀNH CÔNG VỚI TÀI KHOẢN: ${botUserInfo?.name || 'Admin'}!\n`);
        currentQRImage = null;
        currentQRRawCode = null;
        await refreshFriends();
    } catch (err) {
        console.error('[Zalo Gateway] Lỗi khi đăng nhập QR:', err.message);
    } finally {
        isLoggingIn = false;
    }
}

// -------------------------------------------------------------
// REST API Endpoints
// -------------------------------------------------------------

// Kiểm tra trạng thái Bot
app.get('/api/status', (req, res) => {
    res.json({
        loggedIn: !!apiInstance,
        account: botUserInfo ? {
            name: botUserInfo.name,
            avatar: botUserInfo.avatar,
            userId: botUserInfo.userId
        } : null,
        pendingQR: !!currentQRImage
    });
});

// Xem mã QR trực tiếp trên trình duyệt
app.get('/qr', (req, res) => {
    if (apiInstance) {
        return res.send(`
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>Zalo Bot Gateway</title></head>
            <body style="font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f0fdf4;">
                <h1 style="color: #16a34a; font-size: 2rem; margin-bottom: 0.5rem;">✅ Zalo Bot Đã Đăng Nhập Thành Công!</h1>
                <p style="font-size: 1.15rem;">Tài khoản bot: <strong style="color: #0284c7;">${botUserInfo?.name || 'Admin'}</strong></p>
                <p style="color: #64748b; margin-top: 1rem;">Hệ thống đã sẵn sàng tự động bắn tin nhắn Zalo khi bạn duyệt ứng lương hoặc thanh toán tiền!</p>
            </body>
            </html>
        `);
    }

    if (!currentQRImage) {
        return res.send(`
            <!DOCTYPE html>
            <html>
            <head><meta charset="utf-8"><title>Đang tạo mã QR...</title><meta http-equiv="refresh" content="3"></head>
            <body style="font-family: Arial, sans-serif; text-align: center; padding: 50px;">
                <h2>Đang tạo mã QR đăng nhập Zalo...</h2>
                <p>Trang sẽ tự tải lại sau 3 giây.</p>
            </body>
            </html>
        `);
    }

    res.send(`
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8">
            <title>Quét Mã QR Đăng Nhập Zalo Bot</title>
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 100vh; margin: 0; background: #f8fafc; }
                .card { background: white; padding: 2.5rem; border-radius: 16px; box-shadow: 0 10px 25px -5px rgba(0,0,0,0.1); text-align: center; max-width: 440px; }
                h2 { color: #0f172a; margin-top: 0; margin-bottom: 0.5rem; font-size: 1.35rem; }
                p { color: #64748b; font-size: 0.95rem; margin-bottom: 1.5rem; }
                img { border: 2px solid #e2e8f0; border-radius: 12px; padding: 8px; width: 260px; height: 260px; display: block; margin: 0 auto; background: white; }
                .hint { margin-top: 1.5rem; font-size: 0.85rem; color: #0284c7; background: #e0f2fe; padding: 0.75rem 1rem; border-radius: 8px; }
            </style>
        </head>
        <body>
            <div class="card">
                <h2>📲 Quét Mã QR Zalo Bot</h2>
                <p>Mở ứng dụng <strong>Zalo trên điện thoại</strong> và quét mã dưới đây để đăng nhập bot Phúc Quân HR:</p>
                <img src="${currentQRImage}" alt="Zalo Login QR" />
                <div class="hint">💡 Trang này sẽ tự động chuyển trang ngay sau khi bạn bấm <strong>Đăng nhập</strong> trên điện thoại.</div>
            </div>
            <script>
                setInterval(async () => {
                    try {
                        const res = await fetch('/api/status');
                        const data = await res.json();
                        if (data.loggedIn) {
                            window.location.reload();
                        }
                    } catch (e) {}
                }, 2000);
            </script>
        </body>
        </html>
    `);
});

// API gửi tin nhắn thông báo Zalo
app.post('/api/send-zalo', async (req, res) => {
    const { phoneNumber, message, employeeId, employeeName } = req.body;

    if (!apiInstance) {
        return res.status(503).json({
            success: false,
            message: 'Zalo Bot chưa đăng nhập. Vui lòng quét mã QR tại http://localhost:' + PORT + '/qr'
        });
    }

    if (!phoneNumber || !message) {
        return res.status(400).json({
            success: false,
            message: 'Thiếu số điện thoại (phoneNumber) hoặc nội dung tin nhắn (message)'
        });
    }

    const cleanPhone = cleanPhoneNumber(phoneNumber);
    if (!cleanPhone) {
        return res.status(400).json({
            success: false,
            message: 'Số điện thoại không hợp lệ'
        });
    }

    try {
        let targetUid = null;
        let targetName = employeeName || '';

        // 1. Tìm trong danh bạ bạn bè trước
        const matchedFriend = friendsCache.find(f => {
            const fPhone = cleanPhoneNumber(f.phoneNumber);
            return fPhone && fPhone === cleanPhone;
        });

        if (matchedFriend) {
            targetUid = matchedFriend.userId;
            targetName = matchedFriend.displayName || targetName;
        }

        // 2. Nếu chưa thấy trong bạn bè, gọi findUser
        if (!targetUid) {
            console.log(`[Zalo Gateway] Đang tra cứu tài khoản Zalo cho SĐT: ${cleanPhone}...`);
            const user = await apiInstance.findUser(cleanPhone);
            if (user && user.uid) {
                targetUid = user.uid;
                targetName = user.display_name || user.zalo_name || targetName;
            }
        }

        if (!targetUid) {
            return res.status(404).json({
                success: false,
                message: `Không tìm thấy tài khoản Zalo liên kết với số điện thoại ${cleanPhone}`
            });
        }

        // 3. Gửi tin nhắn qua Zalo (ThreadType.User = 0)
        console.log(`[Zalo Gateway] Đang gửi tin nhắn Zalo tới ${targetName} (${cleanPhone} - UID: ${targetUid})...`);
        await apiInstance.sendMessage(message, targetUid, ThreadType.User);
        console.log(`[Zalo Gateway] ✅ Đã gửi tin nhắn Zalo thành công tới ${targetName}!`);

        return res.json({
            success: true,
            targetUid,
            targetName,
            message: 'Đã gửi tin nhắn Zalo thành công'
        });
    } catch (err) {
        console.error(`[Zalo Gateway] ❌ Gửi tin nhắn Zalo thất bại:`, err.message);
        return res.status(500).json({
            success: false,
            error: err.message
        });
    }
});

// Khởi động server
app.listen(PORT, async () => {
    console.log(`\n======================================================`);
    console.log(`🚀 Zalo Bot Gateway đang chạy tại cổng http://localhost:${PORT}`);
    console.log(`📌 Xem mã QR hoặc trạng thái tại: http://localhost:${PORT}/qr`);
    console.log(`======================================================\n`);
    await initZalo();
});
