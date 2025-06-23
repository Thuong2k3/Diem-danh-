const mongoose = require('mongoose');
const Schema = mongoose.Schema;

const attendanceSchema = new Schema({
    userId: {
        type: Schema.Types.ObjectId,
        ref: 'User', // Tham chiếu đến model User
        required: true
    },
    date: {
        type: String, // Lưu dưới dạng chuỗi "YYYY-MM-DD" để dễ dàng truy vấn
        required: true
    },
    checkInTime: {
        type: Date,
        default: null
    },
    checkOutTime: {
        type: Date,
        default: null
    },
    status: { // Ví dụ: 'present', 'late', 'absent'
        type: String,
        default: 'present'
    }
}, { timestamps: true });

// Tạo một index kết hợp để đảm bảo mỗi user chỉ có một bản ghi điểm danh mỗi ngày
attendanceSchema.index({ userId: 1, date: 1 }, { unique: true });

const Attendance = mongoose.model('Attendance', attendanceSchema);

module.exports = Attendance;