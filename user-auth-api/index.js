require("dotenv").config();
const express = require('express');
const mongoose = require('mongoose');
const authRoutes = require('./routes/auth'); // Giả sử authRoutes.js đã được sửa với bcryptjs
const attendanceRoutes = require('./routes/attendanceRoutes');
const cors = require('cors');

const app = express();

// Middlewares
app.use(cors()); // Cho phép Cross-Origin Resource Sharing

// THAY ĐỔI Ở ĐÂY: Middleware để parse URL-encoded bodies (từ form data)
// Đặt trước express.json() để nó có thể xử lý cả hai loại
app.use(express.urlencoded({ extended: true })); // << THÊM DÒNG NÀY HOẶC ĐẢM BẢO NÓ CÓ

// Middleware để parse JSON request bodies
app.use(express.json());

// Định tuyến cho API
app.use("/api", authRoutes);
app.use("/api/attendance", attendanceRoutes);
// Chuỗi kết nối MongoDB Atlas
// vi du no thay duoc link nay cua em la cung~ bay luon database no thich thi no pha' =))),the co phai ma hoa het ko
// ngta hay tao ra file .env
const uri = process.env.MONGO_URI;
// Kết nối đến MongoDB
mongoose.connect(uri, { useNewUrlParser: true, useUnifiedTopology: true }) // Cảnh báo về các option này là bình thường, có thể bỏ đi
    .then(() => console.log("✅ MongoDB connected"))
    .catch(err => {
        console.error("❌ MongoDB connection error:", err.message || err);
    });

// Middleware xử lý lỗi chung
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