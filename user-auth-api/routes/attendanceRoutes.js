const express = require("express");
const router = express.Router();
const Attendance = require("../models/Attendance");
const { isAuthenticated } = require('../middleware/authMiddleware'); // Middleware xác thực JWT (sẽ làm sau)
const moment = require('moment-timezone'); // Thư viện để xử lý múi giờ

// API để Check-in
router.post("/check-in", isAuthenticated, async (req, res) => {
    // const userId = req.user.id; // Lấy userId từ JWT sau khi triển khai
    const { userId } = req.body; // Tạm thời lấy userId từ body để test
    const today = moment().tz("Asia/Ho_Chi_Minh").format("YYYY-MM-DD");

    try {
        // Kiểm tra xem đã check-in trong ngày chưa
        const existingAttendance = await Attendance.findOne({ userId, date: today });
        if (existingAttendance) {
            return res.status(409).json({ status: "error", message: "Bạn đã check-in hôm nay rồi." });
        }

        const newAttendance = new Attendance({
            userId,
            date: today,
            checkInTime: new Date()
        });

        await newAttendance.save();
        res.status(201).json({ status: "success", message: "Check-in thành công!", attendance: newAttendance });

    } catch (e) {
        console.error("Check-in error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi check-in." });
    }
});

// API để Check-out
router.post("/check-out", isAuthenticated, async (req, res) => {
    // const userId = req.user.id; // Lấy userId từ JWT
    const { userId } = req.body; // Tạm thời lấy userId từ body để test
    const today = moment().tz("Asia/Ho_Chi_Minh").format("YYYY-MM-DD");

    try {
        const attendance = await Attendance.findOne({ userId, date: today });
        if (!attendance) {
            return res.status(404).json({ status: "error", message: "Bạn chưa check-in hôm nay." });
        }
        if (attendance.checkOutTime) {
            return res.status(409).json({ status: "error", message: "Bạn đã check-out hôm nay rồi." });
        }

        attendance.checkOutTime = new Date();
        await attendance.save();
        res.json({ status: "success", message: "Check-out thành công!", attendance: attendance });

    } catch (e) {
        console.error("Check-out error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi check-out." });
    }
});

// API để lấy trạng thái điểm danh trong ngày
router.get("/status", isAuthenticated, async (req, res) => {
    // const userId = req.user.id; // Lấy userId từ JWT
    const { userId } = req.query; // Lấy userId từ query params để test (ví dụ: /status?userId=...)
    const today = moment().tz("Asia/Ho_Chi_Minh").format("YYYY-MM-DD");

    try {
        const attendance = await Attendance.findOne({ userId, date: today });
        if (!attendance) {
            res.json({ status: "success", attendanceStatus: "not_checked_in", record: null });
        } else if (!attendance.checkOutTime) {
            res.json({ status: "success", attendanceStatus: "checked_in", record: attendance });
        } else {
            res.json({ status: "success", attendanceStatus: "checked_out", record: attendance });
        }
    } catch (e) {
        console.error("Get attendance status error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi lấy trạng thái điểm danh." });
    }
});

router.get("/history", isAuthenticated, async (req, res) => {
    // const userId = req.user.id; // Lấy userId từ JWT sau khi triển khai
    const { userId, month, year } = req.query; // Lấy từ query params, ví dụ: /history?userId=...&year=2025&month=6

    if (!userId || !year || !month) {
        return res.status(400).json({ status: "error", message: "UserId, year, and month are required." });
    }

    try {
        // Tạo regex để tìm tất cả các ngày trong tháng/năm đã cho
        // Ví dụ: cho tháng 6/2025, regex sẽ là /^2025-06/
        const searchDate = `${year}-${String(month).padStart(2, '0')}`; // Đảm bảo tháng có 2 chữ số, ví dụ: 06
        const dateRegex = new RegExp(`^${searchDate}`);

        const attendanceRecords = await Attendance.find({
            userId: userId,
            date: { $regex: dateRegex }
        }).select('date status'); // Chỉ lấy các trường cần thiết

        res.json({ status: "success", history: attendanceRecords });
    } catch (e) {
        console.error("Get attendance history error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi lấy lịch sử điểm danh." });
    }
});
module.exports = router;