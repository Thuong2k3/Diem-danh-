// middleware/authMiddleware.js

// Middleware giả định để kiểm tra đăng nhập (sẽ thay bằng JWT sau)
const isAuthenticated = (req, res, next) => {
    // Giả sử sau khi đăng nhập, thông tin user được lưu vào req.user (ví dụ)
    // Hoặc bạn có thể kiểm tra session nếu dùng session-based auth
    // TẠM THỜI: Cho qua để test, SẼ THAY THẾ BẰNG JWT
    console.warn("isAuthenticated middleware is currently a placeholder!");
    // if (req.session && req.session.userId) { // Ví dụ nếu dùng session
    //    return next();
    // }
    // return res.status(401).json({ status: "error", message: "Unauthorized: No active session" });
    return next(); // Tạm thời cho qua
};

// Middleware giả định để kiểm tra vai trò Admin (sẽ thay bằng JWT sau)
const isAdmin = (req, res, next) => {
    // Giả sử req.user chứa thông tin user đã đăng nhập, bao gồm role
    // TẠM THỜI: Cho qua để test, SẼ THAY THẾ BẰNG JWT
    console.warn("isAdmin middleware is currently a placeholder! Assuming admin for testing.");
    // if (req.user && req.user.role === 'admin') { // Ví dụ nếu req.user có role
    //     return next();
    // }
    // return res.status(403).json({ status: "error", message: "Forbidden: Admin access required" });
    return next(); // Tạm thời cho qua
};

module.exports = { isAuthenticated, isAdmin };