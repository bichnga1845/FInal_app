package com.example.finalapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

import com.example.finalapp.utils.VietnamAddressApi;
import com.example.finalapp.utils.VietnamAddressApi.Item;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalapp.models.Address;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Man chon dia chi giao hang - hien thi RecyclerView tu /addresses/{uid}, them/sua/xoa
public class SelectAddressActivity extends AppCompatActivity
        implements AddressAdapter.OnAddressActionListener {

    public static final String EXTRA_UID = "extra_uid";
    public static final String RESULT_SELECTED_ADDRESS_ID = "selected_address_id";

    private static final String DEFAULT_UID = "69a9a035fde9b32594ffb37e";

    private RecyclerView rvAddresses;
    private View emptyState;
    private MaterialButton btnAdd;
    private MaterialButton btnConfirm;
    private ImageView btnBack;

    private AddressAdapter adapter;
    private final List<Address> addressList = new ArrayList<>();

    private String currentUid;
    private DatabaseReference addressesRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_select_address);

        View root = findViewById(R.id.select_address_root);
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

        bindViews();
        resolveUid();
        setupRecyclerView();
        setupButtons();
        loadAddresses();
    }

    private void bindViews() {
        rvAddresses = findViewById(R.id.rv_addresses);
        emptyState = findViewById(R.id.empty_state);
        btnAdd = findViewById(R.id.btn_add_address);
        btnConfirm = findViewById(R.id.btn_confirm_address);
        btnBack = findViewById(R.id.btn_back);
    }

    private void resolveUid() {
        String uidFromIntent = getIntent().getStringExtra(EXTRA_UID);
        currentUid = !TextUtils.isEmpty(uidFromIntent) ? uidFromIntent : DEFAULT_UID;
        addressesRef = FirebaseDatabase.getInstance()
                .getReference("addresses").child(currentUid);
    }

    private void setupRecyclerView() {
        adapter = new AddressAdapter(addressList, this);
        rvAddresses.setLayoutManager(new LinearLayoutManager(this));
        rvAddresses.setAdapter(adapter);
    }

    private void setupButtons() {
        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showAddAddressDialog());
        btnConfirm.setOnClickListener(v -> confirmSelection());
    }

    private void loadAddresses() {
        addressesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                addressList.clear();
                String defaultId = null;
                for (DataSnapshot child : snapshot.getChildren()) {
                    Address addr = child.getValue(Address.class);
                    if (addr == null) continue;
                    addr.addressId = child.getKey();
                    addressList.add(addr);
                    if (addr.isDefault) defaultId = addr.addressId;
                }
                // Neu chua chon, uu tien mac dinh, neu khong co mac dinh thi item dau tien
                if (adapter.getSelectedAddressId() == null && !addressList.isEmpty()) {
                    adapter.setSelectedAddressId(defaultId != null ? defaultId : addressList.get(0).addressId);
                } else {
                    adapter.notifyDataSetChanged();
                }
                updateEmptyState();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                AppToast.showLong(SelectAddressActivity.this,
                        getString(R.string.str_addr_load_error, error.getMessage()));
            }
        });
    }

    private void updateEmptyState() {
        boolean empty = addressList.isEmpty();
        emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        rvAddresses.setVisibility(empty ? View.GONE : View.VISIBLE);
        btnConfirm.setEnabled(!empty);
    }

    private void showAddAddressDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_add_address, null, false);

        EditText etReceiver = dialogView.findViewById(R.id.et_dlg_receiver);
        EditText etPhone    = dialogView.findViewById(R.id.et_dlg_phone);
        EditText etDetail   = dialogView.findViewById(R.id.et_dlg_detail);
        MaterialCheckBox cbDefault = dialogView.findViewById(R.id.cb_set_default);
        Spinner spProvince  = dialogView.findViewById(R.id.spinner_province);
        Spinner spDistrict  = dialogView.findViewById(R.id.spinner_district);
        Spinner spWard      = dialogView.findViewById(R.id.spinner_ward);

        if (addressList.isEmpty()) cbDefault.setChecked(true);

        // State
        final Item[] selectedProvince = {null};
        final Item[] selectedDistrict = {null};
        final Item[] selectedWard     = {null};

        // Placeholder list ban đầu
        java.util.List<Item> districtHint  = new java.util.ArrayList<>();
        java.util.List<Item> wardHint      = new java.util.ArrayList<>();
        districtHint.add(new Item(-1, getString(R.string.addr_hint_select_province_first)));
        wardHint.add(new Item(-1, getString(R.string.addr_hint_select_district_first)));

        java.util.List<Item> loadingHint = new java.util.ArrayList<>();
        loadingHint.add(new Item(-1, getString(R.string.loading_hint)));
        spProvince.setAdapter(spinnerAdapter(loadingHint));
        spDistrict.setAdapter(spinnerAdapter(districtHint));
        spWard.setAdapter(spinnerAdapter(wardHint));

        // Load tỉnh thành
        VietnamAddressApi.getProvinces(provinces -> {
            java.util.List<Item> list = new java.util.ArrayList<>();
            list.add(new Item(-1, getString(R.string.addr_hint_province)));
            list.addAll(provinces);
            spProvince.setAdapter(spinnerAdapter(list));
        });

        spProvince.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) {}
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Item item = (Item) p.getItemAtPosition(pos);
                if (item.code == -1) { selectedProvince[0] = null; return; }
                selectedProvince[0] = item;
                selectedDistrict[0] = null;
                selectedWard[0]     = null;
                spDistrict.setEnabled(false);
                spWard.setEnabled(false);
                java.util.List<Item> loading = new java.util.ArrayList<>();
                loading.add(new Item(-1, getString(R.string.loading_hint)));
                spDistrict.setAdapter(spinnerAdapter(loading));
                spWard.setAdapter(spinnerAdapter(wardHint));
                VietnamAddressApi.getDistricts(item.code, districts -> {
                    java.util.List<Item> list = new java.util.ArrayList<>();
                    list.add(new Item(-1, getString(R.string.addr_hint_district)));
                    list.addAll(districts);
                    spDistrict.setAdapter(spinnerAdapter(list));
                    spDistrict.setEnabled(true);
                });
            }
        });

        spDistrict.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) {}
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Item item = (Item) p.getItemAtPosition(pos);
                if (item.code == -1) { selectedDistrict[0] = null; return; }
                selectedDistrict[0] = item;
                selectedWard[0]     = null;
                spWard.setEnabled(false);
                java.util.List<Item> loading = new java.util.ArrayList<>();
                loading.add(new Item(-1, getString(R.string.loading_hint)));
                spWard.setAdapter(spinnerAdapter(loading));
                VietnamAddressApi.getWards(item.code, wards -> {
                    java.util.List<Item> list = new java.util.ArrayList<>();
                    list.add(new Item(-1, getString(R.string.addr_hint_ward)));
                    list.addAll(wards);
                    spWard.setAdapter(spinnerAdapter(list));
                    spWard.setEnabled(true);
                });
            }
        });

        spWard.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> p) {}
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                Item item = (Item) p.getItemAtPosition(pos);
                selectedWard[0] = item.code == -1 ? null : item;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton(R.string.str_save, null)
                .setNegativeButton(R.string.str_cancel, (d, w) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
                String receiver = etReceiver.getText().toString().trim();
                String phone    = etPhone.getText().toString().trim();
                String specific = etDetail.getText().toString().trim();
                boolean setDefault = cbDefault.isChecked();

                if (TextUtils.isEmpty(receiver)) {
                    etReceiver.setError(getString(R.string.str_addr_err_receiver)); return;
                }
                if (!phone.matches("^0\\d{9,10}$")) {
                    etPhone.setError(getString(R.string.str_addr_err_phone)); return;
                }
                if (selectedProvince[0] == null) {
                    android.widget.Toast.makeText(this, getString(R.string.addr_select_province_required), android.widget.Toast.LENGTH_SHORT).show(); return;
                }
                if (selectedDistrict[0] == null) {
                    android.widget.Toast.makeText(this, getString(R.string.addr_select_district_required), android.widget.Toast.LENGTH_SHORT).show(); return;
                }
                if (selectedWard[0] == null) {
                    android.widget.Toast.makeText(this, getString(R.string.addr_select_ward_required), android.widget.Toast.LENGTH_SHORT).show(); return;
                }
                if (TextUtils.isEmpty(specific)) {
                    etDetail.setError(getString(R.string.addr_err_detail_required)); return;
                }

                // Ghép địa chỉ đầy đủ
                String fullDetail = specific + ", " + selectedWard[0].name
                        + ", " + selectedDistrict[0].name
                        + ", " + selectedProvince[0].name;

                saveNewAddress(new Address(receiver, phone, fullDetail, setDefault));
                dialog.dismiss();
            });
        });

        dialog.show();
    }

    private ArrayAdapter<Item> spinnerAdapter(java.util.List<Item> items) {
        ArrayAdapter<Item> a = new ArrayAdapter<>(this, R.layout.item_spinner, items);
        a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        return a;
    }

    private void saveNewAddress(Address address) {
        String newId = addressesRef.push().getKey();
        if (newId == null) {
            AppToast.show(this, R.string.str_addr_no_id);
            return;
        }
        address.addressId = newId;

        // Neu set mac dinh -> phai unset cac dia chi khac
        if (address.isDefault) {
            unsetAllDefaults(() -> writeAddress(newId, address));
        } else {
            writeAddress(newId, address);
        }
    }

    private void writeAddress(String id, Address address) {
        addressesRef.child(id).setValue(address.toMap(), (error, ref) -> {
            if (error == null) {
                AppToast.show(this, R.string.str_addr_added);
                // Dia chi mac dinh -> dong bo sang /users/{uid}/address
                // de man dat hang (doc dia chi cua user) lay dung dia chi moi.
                if (address.isDefault) {
                    mirrorAddressToUser(address.detail);
                }
            } else {
                AppToast.showLong(this, getString(R.string.str_addr_save_error, error.getMessage()));
            }
        });
    }

    // Dong bo dia chi giao hang dang dung sang node user de cac man khac (dat hang) khop du lieu
    private void mirrorAddressToUser(String detail) {
        if (TextUtils.isEmpty(detail)) return;
        FirebaseDatabase.getInstance().getReference("users")
                .child(currentUid).child("address").setValue(detail);
    }

    private void unsetAllDefaults(Runnable onDone) {
        Map<String, Object> updates = new HashMap<>();
        for (Address a : addressList) {
            if (a.isDefault && a.addressId != null) {
                updates.put(a.addressId + "/isDefault", false);
            }
        }
        if (updates.isEmpty()) {
            onDone.run();
            return;
        }
        addressesRef.updateChildren(updates, (error, ref) -> {
            if (onDone != null) onDone.run();
        });
    }

    // ===== AddressAdapter.OnAddressActionListener =====

    @Override
    public void onSelect(Address address) {
        adapter.setSelectedAddressId(address.addressId);
    }

    @Override
    public void onSetDefault(Address address) {
        if (address.isDefault) return;
        unsetAllDefaults(() -> {
            addressesRef.child(address.addressId).child("isDefault").setValue(true,
                    (error, ref) -> {
                        if (error == null) {
                            AppToast.show(this, R.string.str_addr_set_default_done);
                            // Dong bo dia chi mac dinh sang node user
                            mirrorAddressToUser(address.detail);
                        }
                    });
        });
    }

    @Override
    public void onDelete(Address address) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.str_addr_delete_confirm_title)
                .setMessage(R.string.str_addr_delete_confirm_msg)
                .setPositiveButton(R.string.str_delete, (d, w) -> {
                    addressesRef.child(address.addressId).removeValue((error, ref) -> {
                        if (error == null) {
                            AppToast.show(this, R.string.str_addr_deleted);
                            if (address.addressId.equals(adapter.getSelectedAddressId())) {
                                adapter.setSelectedAddressId(null);
                            }
                        }
                    });
                })
                .setNegativeButton(R.string.str_cancel, null)
                .show();
    }

    private void confirmSelection() {
        String selectedId = adapter.getSelectedAddressId();
        if (TextUtils.isEmpty(selectedId)) {
            AppToast.show(this, R.string.str_addr_select_required);
            return;
        }
        // Dong bo dia chi vua chon sang /users/{uid}/address de man dat hang dung dia chi nay
        for (Address a : addressList) {
            if (selectedId.equals(a.addressId)) {
                mirrorAddressToUser(a.detail);
                break;
            }
        }
        Intent result = new Intent();
        result.putExtra(RESULT_SELECTED_ADDRESS_ID, selectedId);
        setResult(RESULT_OK, result);
        finish();
    }
}
