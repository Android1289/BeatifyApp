package com.example.beatify;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> users = new ArrayList<>();
    private OnUserClickListener listener;

    public interface OnUserClickListener {
        void onUserClick(User user);
    }

    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }

    public void updateList(List<User> newList) {
        this.users = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false);
        return new UserViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        User user = users.get(position);
        holder.name.setText(user.getName());
        holder.followers.setText(user.getFollowersCount() + " Followers");

        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.ic_user_placeholder)
                    .into(holder.icon);
        } else {
            holder.icon.setImageResource(R.drawable.ic_user_placeholder);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onUserClick(user);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView name, followers;
        ImageView icon;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            name = itemView.findViewById(R.id.userNameItem);
            followers = itemView.findViewById(R.id.userFollowersItem);
            icon = itemView.findViewById(R.id.userAvatarItem);

            if(icon == null) {
                if(itemView instanceof ViewGroup) {
                    ViewGroup vg = (ViewGroup) itemView;
                    for(int i=0; i<vg.getChildCount(); i++) {
                        if(vg.getChildAt(i) instanceof ImageView) {
                            icon = (ImageView) vg.getChildAt(i);
                            break;
                        }
                    }
                }
            }
        }
    }
}