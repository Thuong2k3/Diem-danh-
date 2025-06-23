package com.example.qlnv;

public class User {
    private String _id;
    private String email;
    private String role;

    // --- THÊM CÁC TRƯỜNG MỚI CHO THÔNG TIN CÁ NHÂN ---
    private String fullName;
    private String gender;
    private String dateOfBirth; // Dùng String để khớp với JSON từ server và dễ xử lý
    private String hometown;
    private String phoneNumber;
    private boolean profileCompleted;

    // Constructors (nếu cần)
    public User() {
    }

    // --- THÊM GETTERS VÀ SETTERS CHO TẤT CẢ CÁC TRƯỜNG ---

    public String getId() {
        return _id;
    }

    public void setId(String id) {
        this._id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    // --- GETTER VÀ SETTER CHO CÁC TRƯỜNG MỚI ---

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getHometown() {
        return hometown;
    }

    public void setHometown(String hometown) {
        this.hometown = hometown;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public boolean isProfileCompleted() {
        return profileCompleted;
    }

    public void setProfileCompleted(boolean profileCompleted) {
        this.profileCompleted = profileCompleted;
    }
}