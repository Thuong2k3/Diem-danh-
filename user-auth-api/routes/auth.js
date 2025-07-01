require("dotenv").config();
const express = require("express");
const router = express.Router();
const Otp = require("../models/Otp");
const User = require("../models/User");
const bcrypt = require("bcryptjs");
const jwt = require("jsonwebtoken");
const nodemailer = require("nodemailer");
const passwordValidator = require("password-validator");
const { isAuthenticated, isAdmin } = require('../middleware/authMiddleware'); // Đảm bảo middleware đã được triển khai đúng
const JWT_SECRET = process.env.JWT_SECRET;

// --- Cấu hình dùng chung ---
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS,
    },
});

const passwordSchema = new passwordValidator();
passwordSchema
    .is().min(8)
    .is().max(100)
    .has().uppercase()
    .has().lowercase()
    .has().digits(1)
    .has().symbols(1)
    .has().not().spaces();

// --- Routes không cần xác thực ---

// Gửi OTP để đăng ký
router.post("/request-otp", async (req, res) => {
    const { email, password } = req.body;
    if (!email || !password) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp email và mật khẩu." });
    }
    if (!passwordSchema.validate(password)) {
        return res.status(400).json({ status: "error", message: "Mật khẩu không đủ mạnh. Phải có ít nhất 8 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt." });
    }
    try {
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(409).json({ status: "error", message: "Email này đã được đăng ký." });
        }
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: email,
            subject: 'Mã xác thực đăng ký QLNV',
            text: `Mã OTP của bạn là: ${otpCode}. Mã này sẽ hết hạn sau 5 phút.`
        };
        await transporter.sendMail(mailOptions);
        await Otp.findOneAndUpdate({ email }, { otp: otpCode }, { upsert: true, new: true, setDefaultsOnInsert: true });
        res.json({ status: "success", message: "Mã OTP đã được gửi đến email của bạn." });
    } catch (e) {
        console.error("Request OTP error:", e);
        res.status(500).json({ status: "error", message: "Không thể gửi mã OTP. Vui lòng thử lại." });
    }
});

// Hoàn tất đăng ký
router.post("/register", async (req, res) => {
    const { email, password, role, otp } = req.body;
    if (!email || !password || !otp) {
        return res.status(400).json({ status: "error", message: "Vui lòng điền đầy đủ thông tin." });
    }
    if (!passwordSchema.validate(password)) {
        return res.status(400).json({ status: "error", message: "Mật khẩu không đủ mạnh." });
    }
    try {
        const otpRecord = await Otp.findOne({ email, otp });
        if (!otpRecord) {
            return res.status(400).json({ status: "error", message: "Mã OTP không hợp lệ hoặc đã hết hạn." });
        }
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);
        const newUser = new User({
            email,
            password: hashedPassword,
            role: role || 'employee',
            profileCompleted: false
        });
        await newUser.save();
        await Otp.deleteOne({ email, otp });
        res.status(201).json({ status: "success", message: "Đăng ký tài khoản thành công!" });
    } catch (e) {
        if (e.code === 11000) {
            return res.status(409).json({ status: "error", message: "Email đã tồn tại." });
        }
        console.error("Registration final step error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi đăng ký." });
    }
});

// Đăng nhập
router.post("/login", async (req, res) => {
    const { email, password } = req.body;
    if (!email || !password) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp email và mật khẩu." });
    }
    try {
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(401).json({ status: "error", message: "Email hoặc mật khẩu không đúng." });
        }
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(401).json({ status: "error", message: "Email hoặc mật khẩu không đúng." });
        }
        const payload = {
            user: { id: user._id, email: user.email, role: user.role }
        };
        jwt.sign(payload, JWT_SECRET, { expiresIn: '1h' }, (err, token) => {
            if (err) {
                console.error("Lỗi khi tạo JWT:", err);
                return res.status(500).json({ status: "error", message: "Lỗi server: Không thể tạo token." });
            }
            res.json({
                status: "success",
                message: "Đăng nhập thành công",
                token: token,
                role: user.role,
                profileCompleted: user.profileCompleted,
                user: { id: user._id, email: user.email }
            });
        });
    } catch (e) {
        console.error("Lỗi server khi đăng nhập:", e.message || e);
        res.status(500).json({ status: "error", message: "Lỗi server trong quá trình đăng nhập." });
    }
});

// Quên mật khẩu - Gửi OTP
router.post("/forgot-password", async (req, res) => {
    const { email } = req.body;
    if (!email) {
        return res.status(400).json({ status: "error", message: "Vui lòng nhập email." });
    }
    try {
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(404).json({ status: "error", message: "Email này chưa được đăng ký." });
        }
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: email,
            subject: 'QLNV - Yêu cầu đặt lại mật khẩu',
            text: `Mã OTP để đặt lại mật khẩu của bạn là: ${otpCode}. Mã này sẽ hết hạn sau 5 phút.`
        };
        await transporter.sendMail(mailOptions);
        await Otp.findOneAndUpdate({ email }, { otp: otpCode }, { upsert: true, new: true, setDefaultsOnInsert: true });
        res.json({ status: "success", message: "Mã OTP đã được gửi đến email của bạn." });
    } catch (e) {
        console.error("Forgot Password error:", e);
        res.status(500).json({ status: "error", message: "Không thể xử lý yêu cầu. Vui lòng thử lại." });
    }
});

// Đặt lại mật khẩu bằng OTP
router.post("/reset-password", async (req, res) => {
    const { email, otp, newPassword } = req.body;
    if (!email || !otp || !newPassword) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp đầy đủ thông tin." });
    }
    if (!passwordSchema.validate(newPassword)) {
        return res.status(400).json({ status: "error", message: "Mật khẩu mới không đủ mạnh." });
    }
    try {
        const otpRecord = await Otp.findOne({ email, otp });
        if (!otpRecord) {
            return res.status(400).json({ status: "error", message: "Mã OTP không hợp lệ hoặc đã hết hạn." });
        }
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(newPassword, salt);
        await User.updateOne({ email: email }, { password: hashedPassword });
        await Otp.deleteOne({ email, otp });
        res.json({ status: "success", message: "Mật khẩu của bạn đã được đặt lại thành công!" });
    } catch (e) {
        console.error("Reset Password error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi đặt lại mật khẩu." });
    }
});


// --- Routes cần xác thực ---

// SỬA: Lấy thông tin cá nhân của người dùng đang đăng nhập
// Middleware `isAuthenticated` sẽ giải mã token và gắn thông tin user vào `req.user`
router.get("/profile", isAuthenticated, async (req, res) => {
    try {
        // Luôn lấy ID từ token đã được xác thực để đảm bảo an toàn
        const user = await User.findById(req.user.id).select("-password");
        if (!user) {
            return res.status(404).json({ status: "error", message: "Không tìm thấy hồ sơ người dùng." });
        }
        // SỬA: Trả về trực tiếp đối tượng user để khớp với logic của Android
        res.json(user);
    } catch (e) {
        console.error("Error fetching profile:", e.message || e);
        res.status(500).json({ status: "error", message: "Lỗi server khi lấy hồ sơ." });
    }
});

// SỬA: Cập nhật thông tin cá nhân của người dùng đang đăng nhập
// Sửa đường dẫn từ "/profile/update" thành "/profile" để nhất quán
router.put("/profile", isAuthenticated, async (req, res) => {
    // SỬA: Lấy ID người dùng từ token, KHÔNG BAO GIỜ tin tưởng ID từ body request
    const idToUpdate = req.user.id;

    const { fullName, gender, dateOfBirth, hometown, phoneNumber } = req.body;

    if (!fullName || !gender || !dateOfBirth || !hometown || !phoneNumber) {
        return res.status(400).json({ status: "error", message: "Vui lòng điền đầy đủ tất cả các trường thông tin." });
    }

    const updateData = {
        fullName,
        gender,
        dateOfBirth,
        hometown,
        phoneNumber,
        profileCompleted: true
    };

    try {
        const updatedUser = await User.findByIdAndUpdate(
            idToUpdate,
            { $set: updateData },
            { new: true, runValidators: true }
        ).select("-password");

        if (!updatedUser) {
            return res.status(404).json({ status: "error", message: "Không tìm thấy người dùng để cập nhật." });
        }
        res.json({ status: "success", message: "Cập nhật hồ sơ thành công!", user: updatedUser });
    } catch (e) {
        console.error("Error updating profile:", e.message || e);
        res.status(500).json({ status: "error", message: "Lỗi server khi cập nhật hồ sơ." });
    }
});

// SỬA: Đổi mật khẩu cho người dùng đang đăng nhập
// Xóa route trùng lặp và sửa lại logic bảo mật
router.post("/change-password", isAuthenticated, async (req, res) => {
    // SỬA: Lấy userId từ token đã xác thực, không lấy từ body
    const userId = req.user.id;
    const { currentPassword, newPassword } = req.body;

    if (!currentPassword || !newPassword) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp đầy đủ thông tin." });
    }

    // Sử dụng passwordSchema để đảm bảo mật khẩu mới đủ mạnh
    if (!passwordSchema.validate(newPassword)) {
        return res.status(400).json({ status: "error", message: "Mật khẩu mới không đủ mạnh. Phải có ít nhất 8 ký tự, gồm chữ hoa, thường, số và ký tự đặc biệt." });
    }

    try {
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ status: "error", message: "Không tìm thấy người dùng." });
        }

        const isMatch = await bcrypt.compare(currentPassword, user.password);
        if (!isMatch) {
            return res.status(401).json({ status: "error", message: "Mật khẩu hiện tại không đúng." });
        }

        const salt = await bcrypt.genSalt(10);
        user.password = await bcrypt.hash(newPassword, salt);
        await user.save();

        res.json({ status: "success", message: "Đổi mật khẩu thành công!" });
    } catch (e) {
        console.error("Change password error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi đổi mật khẩu." });
    }
});


// --- Routes chỉ dành cho Admin ---

// Lấy danh sách tất cả người dùng
router.get("/users", isAuthenticated, isAdmin, async (req, res) => {
    try {
        const users = await User.find().select("-password");
        res.json({ status: "success", users: users });
    } catch (e) {
        console.error("Error fetching users:", e.message || e);
        res.status(500).json({ status: "error", message: "Lỗi server khi lấy danh sách người dùng" });
    }
});

module.exports = router;