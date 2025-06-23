const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const otpSchema = new Schema({
    email: {
        type: String,
        required: true,
    },
    otp: {
        type: String,
        required: true,
    },
    createdAt: {
        type: Date,
        default: Date.now,
        expires: 300, // Mã OTP sẽ tự động bị xóa sau 300 giây (5 phút)
    },
});

const Otp = mongoose.model('Otp', otpSchema);

module.exports = Otp;
