const jwt = require('jsonwebtoken');
const JWT_SECRET = process.env.JWT_SECRET;

// Middleware để xác thực token
const isAuthenticated = (req, res, next) => {
    // Lấy token từ header Authorization, có định dạng "Bearer <token>"
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (token == null) {
        // Không có token, trả về lỗi 401 Unauthorized
        return res.status(401).json({ status: "error", message: "Unauthorized: No token provided" });
    }

    // Xác thực token
    jwt.verify(token, JWT_SECRET, (err, payload) => {
        if (err) {
            // Token không hợp lệ hoặc hết hạn, trả về lỗi 403 Forbidden
            return res.status(403).json({ status: "error", message: "Forbidden: Invalid or expired token" });
        }
        // Gán thông tin payload (chứa user id, email, role) vào đối tượng req
        req.user = payload.user;
        next(); // Chuyển sang xử lý tiếp theo
    });
};

// Middleware để kiểm tra vai trò Admin
const isAdmin = (req, res, next) => {
    // Middleware này phải được gọi SAU isAuthenticated
    if (req.user && req.user.role === 'admin') {
        next(); // Nếu là admin, cho qua
    } else {
        // Nếu không phải admin, trả về lỗi 403 Forbidden
        return res.status(403).json({ status: "error", message: "Forbidden: Admin access required" });
    }
};

module.exports = { isAuthenticated, isAdmin };