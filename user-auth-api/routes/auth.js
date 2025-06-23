require("dotenv").config();
const express = require("express");
const router = express.Router();
const Otp = require("../models/Otp");
const User = require("../models/User"); // Đảm bảo tệp User model của bạn định nghĩa schema đúng
const bcrypt = require("bcryptjs");    // Thư viện để hash mật khẩu
const jwt = require("jsonwebtoken");
const nodemailer = require("nodemailer");
const passwordValidator = require("password-validator");
const { isAuthenticated, isAdmin } = require('../middleware/authMiddleware');
const JWT_SECRET = process.env.JWT_SECRET;
// em nen tao 1 file .env 
// vi khi em push code len nhe bi. lo^. key la bay he thong 
// Đăng ký
router.post("/register", async (req, res) => {
    const { email, password, role, otp } = req.body;

    // 1. Kiểm tra các trường bắt buộc
    if (!email || !password || !otp) {
        return res.status(400).json({ status: "error", message: "Vui lòng điền đầy đủ thông tin." });
    }

    // 2. Kiểm tra lại độ mạnh mật khẩu
    if (!passwordSchema.validate(password)) {
        return res.status(400).json({ status: "error", message: "Mật khẩu không đủ mạnh." });
    }

    try {
        // 3. Xác thực mã OTP
        const otpRecord = await Otp.findOne({ email, otp });
        if (!otpRecord) {
            return res.status(400).json({ status: "error", message: "Mã OTP không hợp lệ hoặc đã hết hạn." });
        }

        // 4. Hash mật khẩu và tạo người dùng
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(password, salt);

        const newUser = new User({
            email,
            password: hashedPassword,
            role: role || 'employee',
            profileCompleted: false // Mặc định là chưa hoàn thành profile
        });

        await newUser.save();

        // 5. Xóa OTP đã sử dụng
        await Otp.deleteOne({ email });

        res.status(201).json({ status: "success", message: "Đăng ký tài khoản thành công!" });

    } catch (e) {
        // Xử lý lỗi trùng lặp email (dù đã kiểm tra ở bước gửi OTP, nhưng vẫn nên có để an toàn)
        if (e.code === 11000) {
            return res.status(409).json({ status: "error", message: "Email đã tồn tại." });
        }
        console.error("Registration final step error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi đăng ký." });
    }
});
const transporter = nodemailer.createTransport({
    service: 'gmail',
    auth: {
        user: process.env.EMAIL_USER,
        pass: process.env.EMAIL_PASS,
    },
});
const passwordSchema = new passwordValidator();
passwordSchema
    .is().min(8)                                    // Tối thiểu 8 ký tự
    .is().max(100)                                  // Tối đa 100 ký tự
    .has().uppercase()                              // Phải có chữ hoa
    .has().lowercase()                              // Phải có chữ thường
    .has().digits(1)                                // Phải có ít nhất 1 chữ số
    .has().symbols(1)                               // Phải có ít nhất 1 ký tự đặc biệt
    .has().not().spaces();
router.post("/request-otp", async (req, res) => {
    const { email, password } = req.body;

    // 1. Kiểm tra độ mạnh mật khẩu
    if (!passwordSchema.validate(password)) {
        return res.status(400).json({ status: "error", message: "Mật khẩu không đủ mạnh. Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt." });
    }

    try {
        // 2. Kiểm tra email đã tồn tại chưa
        const existingUser = await User.findOne({ email });
        if (existingUser) {
            return res.status(409).json({ status: "error", message: "Email này đã được đăng ký." });
        }

        // 3. Tạo và gửi OTP
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString(); // Tạo mã 6 số

        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: email,
            subject: 'Mã xác thực đăng ký QLNV',
            text: `Mã OTP của bạn là: ${otpCode}. Mã này sẽ hết hạn sau 5 phút.`
        };

        await transporter.sendMail(mailOptions);

        // 4. Lưu OTP vào database
        await Otp.findOneAndUpdate({ email }, { otp: otpCode }, { upsert: true, new: true, setDefaultsOnInsert: true });

        res.json({ status: "success", message: "Mã OTP đã được gửi đến email của bạn." });

    } catch (e) {
        console.error("Request OTP error:", e);
        res.status(500).json({ status: "error", message: "Không thể gửi mã OTP. Vui lòng thử lại." });
    }
});
// Đăng nhập
// đây kieu post nè
                // async(req, res) la middleware
                // req: request , res: response
                // client sẽ request (yêu cầu)
                // thg middleware có nhiệm vụ kiểm tra xem cái yêu cầu ( req ) có hợp lệ hay ko
                // sau đó thg Server phản hồi ( res ) về cho thằng Client
router.post("/login", async (req, res) => {
    const { email, password } = req.body;

    if (!email || !password) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp email và mật khẩu." });
    }

    try {
        // 1. Tìm người dùng bằng email
        const user = await User.findOne({ email });
        if (!user) {
            return res.status(401).json({ status: "error", message: "Email hoặc mật khẩu không đúng." }); // Thông báo chung
        }

        // 2. So sánh mật khẩu nhập vào với mật khẩu đã hash trong database
        const isMatch = await bcrypt.compare(password, user.password);
        if (!isMatch) {
            return res.status(401).json({ status: "error", message: "Email hoặc mật khẩu không đúng." }); // Thông báo chung
        }

        // 3. Đăng nhập thành công, tạo JWT
        const payload = {
            user: {
                id: user._id,
                email: user.email,
                role: user.role
            }
        };

        // Ký (Sign) và tạo JWT
        jwt.sign(
            payload,
            JWT_SECRET,
            { expiresIn: '1h' }, // Token hết hạn sau 1 giờ (bạn có thể thay đổi)
            (err, token) => {
                if (err) {
                    console.error("Lỗi khi tạo JWT:", err);
                    return res.status(500).json({ status: "error", message: "Lỗi server: Không thể tạo token." });
                }

                // Gửi token và thông tin người dùng về cho client
                // ĐÚNG THEO CẤU TRÚC BẠN YÊU CẦU
                res.json({
                    status: "success",
                    message: "Đăng nhập thành công",
                    role: user.role,
                    token: token, // Token JWT thực sự
                    profileCompleted: user.profileCompleted, // Trạng thái hoàn thành profile
                    user: {
                        id: user._id,
                        email: user.email
                        // Cân nhắc chỉ gửi thông tin user cần thiết,
                        // vì role đã có ở cấp ngoài và trong payload của token
                    }
                });
            }
        );

    } catch (e) {
        console.error("Lỗi server khi đăng nhập:", e.message || e);
        res.status(500).json({ status: "error", message: "Lỗi server trong quá trình đăng nhập." });
    }
});

router.get("/users", isAuthenticated, isAdmin, async (req, res) => {
    try {
        // Lấy tất cả người dùng, không trả về mật khẩu
        const users = await User.find().select("-password"); // "-password" để loại bỏ trường password
        res.json({ status: "success", users: users });
    } catch (e) {
        console.error("Error fetching users:", e.message || e);
        res.status(500).json({ status: "error", message: "Server error fetching users" });
    }
});

router.put("/profile/update", isAuthenticated, async (req, res) => {
    // Phần lấy userId vẫn giữ nguyên logic tạm thời hoặc logic JWT của bạn
    const { userId, fullName, gender, dateOfBirth, hometown, phoneNumber } = req.body;
    let idToUpdate;

    if (userId) {
        idToUpdate = userId;
    } else if (req.body.email) { // Cách tạm thời nếu không có JWT
        try {
            const tempUser = await User.findOne({email: req.body.email});
            if (!tempUser) {
                return res.status(404).json({status: "error", message: "User not found with provided email."});
            }
            idToUpdate = tempUser._id;
        } catch(e) {
            return res.status(500).json({status: "error", message: "Error finding user by email."});
        }
    } else {
        return res.status(400).json({status: "error", message: "User identification failed."});
    }

    // --- KIỂM TRA CÁC TRƯỜNG BẮT BUỘC ---
    if (!fullName || !gender || !dateOfBirth || !hometown || !phoneNumber) {
        return res.status(400).json({ status: "error", message: "Vui lòng điền đầy đủ tất cả các trường thông tin." });
    }
    // --- KẾT THÚC KIỂM TRA ---

    const updateData = {
        fullName,
        gender,
        dateOfBirth,
        hometown,
        phoneNumber,
        profileCompleted: true // Luôn đánh dấu là đã hoàn thành khi gọi API này
    };

    try {
        const updatedUser = await User.findByIdAndUpdate(
            idToUpdate,
            { $set: updateData },
            { new: true, runValidators: true }
        ).select("-password");

        if (!updatedUser) {
            return res.status(404).json({ status: "error", message: "User not found for update." });
        }

        res.json({ status: "success", message: "Profile updated successfully!", user: updatedUser });
    } catch (e) {
        console.error("Error updating profile:", e.message || e);
        res.status(500).json({ status: "error", message: "Server error updating profile." });
    }
});

// MỚI: Route để lấy thông tin cá nhân của người dùng đang đăng nhập (hoặc người dùng cụ thể bởi Admin)
router.get("/profile", isAuthenticated, async (req, res) => {
    // Tương tự như trên, cần cách lấy userId của người đang đăng nhập từ JWT
    // TẠM THỜI: giả sử client gửi email để lấy profile (KHÔNG AN TOÀN CHO GET PROFILE CỦA CHÍNH MÌNH)
    // Hoặc nếu là admin lấy thông tin user khác thì gửi userId
    const { emailForProfile, userIdForProfile } = req.query; // Lấy từ query params ví dụ ?emailForProfile=a@b.com

    let targetUserId;

    if (userIdForProfile) { // Admin có thể lấy theo ID
        targetUserId = userIdForProfile;
    } else if (emailForProfile) { // Người dùng tự lấy profile của mình qua email (tạm thời)
         try {
            const tempUser = await User.findOne({email: emailForProfile});
            if (!tempUser) {
                return res.status(404).json({status: "error", message: "User not found with provided email."});
            }
            targetUserId = tempUser._id;
        } catch(e) {
            return res.status(500).json({status: "error", message: "Error finding user by email."});
        }
    } else {
        // Khi có JWT, sẽ lấy từ req.user.id
        return res.status(400).json({ status: "error", message: "User identification (email/userId) required." });
    }


    try {
        const user = await User.findById(targetUserId).select("-password");
        if (!user) {
            return res.status(404).json({ status: "error", message: "User profile not found." });
        }
        res.json({ status: "success", user: user });
    } catch (e) {
        console.error("Error fetching profile:", e.message || e);
        res.status(500).json({ status: "error", message: "Server error fetching profile." });
    }
});

router.post("/change-password", isAuthenticated, async (req, res) => {
    // Khi có JWT hoàn chỉnh, userId sẽ được lấy từ token: const { id: userId } = req.user;
    // Tạm thời, chúng ta lấy userId từ req.body để test
    const { userId, currentPassword, newPassword } = req.body;

    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp đầy đủ thông tin." });
    }

    if (newPassword.length < 6) { // Thêm validation cho mật khẩu mới
        return res.status(400).json({ status: "error", message: "Mật khẩu mới phải có ít nhất 6 ký tự." });
    }

    try {
        // 1. Tìm người dùng bằng ID
        const user = await User.findById(userId);
        if (!user) {
            return res.status(404).json({ status: "error", message: "Không tìm thấy người dùng." });
        }

        // 2. So sánh mật khẩu hiện tại người dùng nhập với mật khẩu trong DB
        const isMatch = await bcrypt.compare(currentPassword, user.password);
        if (!isMatch) {
            return res.status(401).json({ status: "error", message: "Mật khẩu hiện tại không đúng." });
        }

        // 3. Hash mật khẩu mới và cập nhật
        const salt = await bcrypt.genSalt(10);
        user.password = await bcrypt.hash(newPassword, salt);
        await user.save();

        res.json({ status: "success", message: "Đổi mật khẩu thành công!" });

    } catch (e) {
        console.error("Change password error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi đổi mật khẩu." });
    }
});
router.post("/change-password", async (req, res) => {
    // Khi có JWT, userId sẽ được lấy từ token. Tạm thời lấy từ body.
    const { userId, currentPassword, newPassword } = req.body;

    if (!userId || !currentPassword || !newPassword) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp đầy đủ thông tin." });
    }

    // Sử dụng passwordSchema đã định nghĩa ở trên để kiểm tra mật khẩu mới
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
router.post("/forgot-password", async (req, res) => {
    const { email } = req.body;
    if (!email) {
        return res.status(400).json({ status: "error", message: "Vui lòng nhập email." });
    }

    try {
        // 1. Kiểm tra xem email có tồn tại trong hệ thống không
        const user = await User.findOne({ email });
        if (!user) {
            // Trả về lỗi nếu email không tồn tại
            return res.status(404).json({ status: "error", message: "Email này chưa được đăng ký." });
        }

        // 2. Tạo và gửi OTP
        const otpCode = Math.floor(100000 + Math.random() * 900000).toString();
        const mailOptions = {
            from: process.env.EMAIL_USER,
            to: email,
            subject: 'QLNV - Yêu cầu đặt lại mật khẩu',
            text: `Mã OTP để đặt lại mật khẩu của bạn là: ${otpCode}. Mã này sẽ hết hạn sau 5 phút.`
        };

        await transporter.sendMail(mailOptions);

        // 3. Lưu OTP vào database (cùng collection với OTP đăng ký)
        await Otp.findOneAndUpdate({ email }, { otp: otpCode }, { upsert: true, new: true, setDefaultsOnInsert: true });

        res.json({ status: "success", message: "Mã OTP đã được gửi đến email của bạn." });

    } catch (e) {
        console.error("Forgot Password error:", e);
        res.status(500).json({ status: "error", message: "Không thể xử lý yêu cầu. Vui lòng thử lại." });
    }
});


// API MỚI: Đặt lại mật khẩu bằng OTP
router.post("/reset-password", async (req, res) => {
    const { email, otp, newPassword } = req.body;

    // 1. Kiểm tra các trường bắt buộc
    if (!email || !otp || !newPassword) {
        return res.status(400).json({ status: "error", message: "Vui lòng cung cấp đầy đủ thông tin." });
    }

    // 2. Kiểm tra độ mạnh của mật khẩu mới
    if (!passwordSchema.validate(newPassword)) { // Sử dụng passwordSchema đã định nghĩa ở trên
        return res.status(400).json({ status: "error", message: "Mật khẩu mới không đủ mạnh." });
    }

    try {
        // 3. Xác thực mã OTP
        const otpRecord = await Otp.findOne({ email, otp });
        if (!otpRecord) {
            return res.status(400).json({ status: "error", message: "Mã OTP không hợp lệ hoặc đã hết hạn." });
        }

        // 4. Tìm người dùng và hash mật khẩu mới
        const salt = await bcrypt.genSalt(10);
        const hashedPassword = await bcrypt.hash(newPassword, salt);

        // 5. Cập nhật mật khẩu mới cho người dùng có email tương ứng
        await User.updateOne({ email: email }, { password: hashedPassword });

        // 6. Xóa OTP đã sử dụng
        await Otp.deleteOne({ email });

        res.json({ status: "success", message: "Mật khẩu của bạn đã được đặt lại thành công!" });

    } catch (e) {
        console.error("Reset Password error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi đặt lại mật khẩu." });
    }
});
module.exports = router;