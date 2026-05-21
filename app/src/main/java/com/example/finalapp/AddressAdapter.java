package com.example.finalapp;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.models.Address;

import java.util.List;

// Adapter cho RecyclerView danh sach dia chi
public class AddressAdapter extends RecyclerView.Adapter<AddressAdapter.AddressViewHolder> {

    public interface OnAddressActionListener {
        void onSelect(Address address);
        void onSetDefault(Address address);
        void onDelete(Address address);
    }

    private final List<Address> addresses;
    private final OnAddressActionListener listener;
    private String selectedAddressId;

    public AddressAdapter(List<Address> addresses, OnAddressActionListener listener) {
        this.addresses = addresses;
        this.listener = listener;
    }

    public void setSelectedAddressId(String addressId) {
        this.selectedAddressId = addressId;
        notifyDataSetChanged();
    }

    public String getSelectedAddressId() {
        return selectedAddressId;
    }

    @NonNull
    @Override
    public AddressViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_address, parent, false);
        return new AddressViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AddressViewHolder holder, int position) {
        Address address = addresses.get(position);

        holder.tvReceiver.setText(address.receiverName == null ? "" : address.receiverName);
        holder.tvPhone.setText(address.phone == null ? "" : address.phone);
        holder.tvDetail.setText(address.detail == null ? "" : address.detail);
        holder.tvDefaultBadge.setVisibility(address.isDefault ? View.VISIBLE : View.GONE);

        // Highlight neu duoc chon
        boolean selected = address.addressId != null && address.addressId.equals(selectedAddressId);
        holder.card.setBackgroundResource(selected
                ? R.drawable.bg_address_item_selected
                : R.drawable.bg_address_item);

        // An nut "Dat mac dinh" neu da la mac dinh
        holder.btnSetDefault.setVisibility(address.isDefault ? View.GONE : View.VISIBLE);

        // Click ca card -> chon dia chi
        holder.card.setOnClickListener(v -> {
            if (listener != null) listener.onSelect(address);
        });
        holder.btnSetDefault.setOnClickListener(v -> {
            if (listener != null) listener.onSetDefault(address);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(address);
        });
    }

    @Override
    public int getItemCount() {
        return addresses.size();
    }

    static class AddressViewHolder extends RecyclerView.ViewHolder {
        LinearLayout card;
        TextView tvReceiver;
        TextView tvPhone;
        TextView tvDetail;
        TextView tvDefaultBadge;
        TextView btnSetDefault;
        TextView btnDelete;

        AddressViewHolder(@NonNull View itemView) {
            super(itemView);
            card = (LinearLayout) itemView;
            tvReceiver = itemView.findViewById(R.id.tv_address_receiver);
            tvPhone = itemView.findViewById(R.id.tv_address_phone);
            tvDetail = itemView.findViewById(R.id.tv_address_detail);
            tvDefaultBadge = itemView.findViewById(R.id.tv_default_badge);
            btnSetDefault = itemView.findViewById(R.id.btn_set_default);
            btnDelete = itemView.findViewById(R.id.btn_delete_address);
        }
    }
}
