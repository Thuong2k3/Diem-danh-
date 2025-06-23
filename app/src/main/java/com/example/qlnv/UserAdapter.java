package com.example.qlnv;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private Context context;
    private List<User> userList;
    private final OnUserClickListener listener; // Giữ nguyên

    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<< SỬA LẠI HÀM KHỞI TẠO NÀY >>>>>>>>>>>>>>>>>>>>>>>>>>>>>
    public UserAdapter(Context context, List<User> userList, OnUserClickListener listener) {
        this.context = context;
        this.userList = userList;
        this.listener = listener; // Giờ đây listener sẽ được gán giá trị từ bên ngoài
    }
    // <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = userList.get(position);
        final String updatingStatus = "Đang cập nhật";

        holder.tvUserEmail.setText(user.getEmail() != null ? user.getEmail() : "N/A");
        holder.tvUserRole.setText("Vai trò: " + (user.getRole() != null ? user.getRole() : "N/A"));

        // Dòng này giờ sẽ hoạt động chính xác vì listener không còn null
        holder.itemView.setOnClickListener(v -> listener.onUserClick(user));

        // --- PHẦN CÒN LẠI GIỮ NGUYÊN ---

        // Họ tên
        String fullName = user.getFullName();
        holder.tvUserFullName.setText("Họ tên: " + (fullName != null && !fullName.isEmpty() ? fullName : updatingStatus));

        // Quê quán
        String hometown = user.getHometown();
        holder.tvUserHometown.setText("Quê quán: " + (hometown != null && !hometown.isEmpty() ? hometown : updatingStatus));

        // Số điện thoại
        String phoneNumber = user.getPhoneNumber();
        holder.tvUserPhoneNumber.setText("SĐT: " + (phoneNumber != null && !phoneNumber.isEmpty() ? phoneNumber : updatingStatus));

        // Giới tính
        String gender = user.getGender();
        if (gender != null && !gender.isEmpty()) {
            String genderDisplay;
            switch (gender.toLowerCase()) {
                case "male":
                    genderDisplay = "Nam";
                    break;
                case "female":
                    genderDisplay = "Nữ";
                    break;
                case "other":
                    genderDisplay = "Khác";
                    break;
                default:
                    genderDisplay = updatingStatus;
                    break;
            }
            holder.tvUserGender.setText("Giới tính: " + genderDisplay);
        } else {
            holder.tvUserGender.setText("Giới tính: " + updatingStatus);
        }

        // Ngày sinh
        String dobString = user.getDateOfBirth();
        if (dobString != null && !dobString.isEmpty() && !dobString.equalsIgnoreCase("null")) {
            SimpleDateFormat serverFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            try {
                Date date = serverFormat.parse(dobString);
                holder.tvUserDob.setText("Ngày sinh: " + displayFormat.format(date));
            } catch (ParseException e) {
                if (dobString.length() >= 10) {
                    holder.tvUserDob.setText("Ngày sinh: " + dobString.substring(0, 10));
                } else {
                    holder.tvUserDob.setText("Ngày sinh: " + updatingStatus);
                }
            }
        } else {
            holder.tvUserDob.setText("Ngày sinh: " + updatingStatus);
        }
    }

    @Override
    public int getItemCount() {
        return userList == null ? 0 : userList.size();
    }

    public void setUserList(List<User> newUserList) {
        this.userList = newUserList;
        notifyDataSetChanged();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserEmail, tvUserRole, tvUserFullName, tvUserGender, tvUserDob, tvUserHometown, tvUserPhoneNumber;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvUserRole = itemView.findViewById(R.id.tvUserRole);
            tvUserFullName = itemView.findViewById(R.id.tvUserFullName);
            tvUserGender = itemView.findViewById(R.id.tvUserGender);
            tvUserDob = itemView.findViewById(R.id.tvUserDob);
            tvUserHometown = itemView.findViewById(R.id.tvUserHometown);
            tvUserPhoneNumber = itemView.findViewById(R.id.tvUserPhoneNumber);
        }
    }
}