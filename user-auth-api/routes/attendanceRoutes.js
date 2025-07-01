const express = require("express");
const router = express.Router();
const Attendance = require("../models/Attendance");
const moment = require('moment-timezone');
const { isAuthenticated, isAdmin } = require('../middleware/authMiddleware');

// === CÁC ROUTE CHO NHÂN VIÊN ===

// 1. Nhân viên tự điểm danh
router.post("/check-in", isAuthenticated, async (req, res) => {
    // Lấy userId từ token đã xác thực, không lấy từ body
    const userId = req.user.id;
    const today = moment().tz("Asia/Ho_Chi_Minh").format("YYYY-MM-DD");

    try {
        const existingAttendance = await Attendance.findOne({ userId, date: today });
        if (existingAttendance) {
            return res.status(409).json({ status: "error", message: "Bạn đã check-in hôm nay rồi." });
        }
        const newAttendance = new Attendance({ userId, date: today, checkInTime: new Date() });
        await newAttendance.save();
        res.status(201).json({ status: "success", message: "Check-in thành công!", attendance: newAttendance });
    } catch (e) {
        console.error("Check-in error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi check-in." });
    }
});

// 2. Nhân viên xem lịch sử của chính mình
router.get("/my-history", isAuthenticated, async (req, res) => {
    // Lấy userId từ token, không cần client gửi lên
    const userId = req.user.id;
    const { month, year } = req.query; // Chỉ cần tháng và năm từ query

    if (!year || !month) {
        return res.status(400).json({ status: "error", message: "Yêu cầu thiếu năm hoặc tháng." });
    }
    try {
        const searchDate = `${year}-${String(month).padStart(2, '0')}`;
        const dateRegex = new RegExp(`^${searchDate}`);

        const attendanceRecords = await Attendance.find({ userId, date: { $regex: dateRegex } }).select('date status');
        res.json(attendanceRecords);
    } catch (e) {
        console.error("Get my history error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi lấy lịch sử điểm danh." });
    }
});


// === ROUTE CHỈ DÀNH CHO ADMIN ===

// 3. Admin xem lịch sử của một nhân viên cụ thể
// Dùng path parameter /:userId để chỉ định nhân viên cần xem
router.get("/history/:userId", isAuthenticated, isAdmin, async (req, res) => {
    // Lấy userId của nhân viên cần xem từ URL
    const { userId } = req.params;
    const { month, year } = req.query; // Lấy tháng và năm từ query

    if (!userId || !year || !month) {
        return res.status(400).json({ status: "error", message: "Yêu cầu thiếu UserID, năm, hoặc tháng." });
    }
    try {
        const searchDate = `${year}-${String(month).padStart(2, '0')}`;
        const dateRegex = new RegExp(`^${searchDate}`);

        const attendanceRecords = await Attendance.find({ userId, date: { $regex: dateRegex } }).select('date status');
        res.json(attendanceRecords);
    } catch (e) {
        console.error("Admin get attendance history error:", e);
        res.status(500).json({ status: "error", message: "Lỗi server khi lấy lịch sử điểm danh." });
    }
});

module.exports = router;