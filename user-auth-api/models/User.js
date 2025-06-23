const mongoose = require('mongoose');
const bcrypt = require('bcryptjs'); // Giữ lại nếu bạn dùng pre-save hook ở đây

const userSchema = new mongoose.Schema({
    email: {
        type: String,
        required: true,
        unique: true,
        lowercase: true,
        trim: true
    },
    password: {
        type: String,
        required: true
    },
    role: {
        type: String,
        enum: ['admin', 'employee'],
        default: 'employee'
    },
    // --- THÊM CÁC TRƯỜNG MỚI ---
    fullName: {
        type: String,
        default: null // Hoặc có thể để là "" nếu muốn chuỗi rỗng
    },
    gender: { // Giới tính
        type: String,
        enum: ['male', 'female', 'other', null], // 'nam', 'nữ', 'khác'
        default: null
    },
    dateOfBirth: { // Ngày sinh
        type: Date,
        default: null
    },
    hometown: { // Quê quán
        type: String,
        default: null
    },
    phoneNumber: {
        type: String,
        default: null
    },
    profileCompleted: { // Cờ để đánh dấu đã hoàn thành cập nhật thông tin
        type: Boolean,
        default: false
    }
    // --- KẾT THÚC CÁC TRƯỜNG MỚI ---
}, { timestamps: true });

const User = mongoose.model('User', userSchema);

module.exports = User;