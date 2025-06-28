require("dotenv").config();
const express = require('express');
const mongoose = require('mongoose');
const authRoutes = require('./routes/auth'); // Đảm bảo file này tồn tại
const attendanceRoutes = require('./routes/attendanceRoutes'); // Đảm bảo file này tồn tại
const cors = require('cors');

const app = express();

// Middlewares
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// --- SỬA LỖI QUAN TRỌNG NHẤT NẰM Ở ĐÂY ---
//
// QUY TẮC: LUÔN ĐẶT ROUTE CỤ THỂ HƠN LÊN TRƯỚC ROUTE CHUNG CHUNG HƠN
//
// Route cho điểm danh: /api/attendance
app.use("/api/attendance", attendanceRoutes);

// Route cho xác thực: /api/auth
app.use("/api/auth", authRoutes);
// ------------------------------------------

// Chuỗi kết nối MongoDB
const uri = process.env.MONGO_URI;

// Kết nối đến MongoDB
mongoose.connect(uri)
    .then(() => console.log("✅ MongoDB connected successfully"))
    .catch(err => {
        console.error("❌ MongoDB connection error:", err.message || err);
    });

// Middleware xử lý lỗi chung (phải đặt ở cuối cùng)
app.use((err, req, res, next) => {
  console.error("Unhandled Server Error:", err.stack || err.message || err);
  const statusCode = err.statusCode || 500;
  const errorMessage = err.message || "Rất tiếc, server đang gặp sự cố. Vui lòng thử lại sau.";
  res.status(statusCode).json({
    status: "error",
    message: errorMessage
  });
});

// Khởi động server
const PORT = process.env.PORT || 3000;
app.listen(PORT, () => console.log(`🚀 Server is running on http://localhost:${PORT}`));
