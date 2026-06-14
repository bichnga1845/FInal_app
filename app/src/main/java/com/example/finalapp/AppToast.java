package com.example.finalapp;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

// Popup thong bao theo giao dien app (thay cho Toast mac dinh mau xam cua he thong).
// Nen kem #FFF8EC, chu olive, bo goc - tu an sau vai giay, khong can bam OK.
public final class AppToast {

    private AppToast() {}

    public static void show(Context context, CharSequence message) {
        make(context, message, Toast.LENGTH_SHORT).show();
    }

    public static void showLong(Context context, CharSequence message) {
        make(context, message, Toast.LENGTH_LONG).show();
    }

    // Tien ich goi truc tiep bang string resource id
    public static void show(Context context, int messageResId) {
        show(context, context.getString(messageResId));
    }

    private static Toast make(Context context, CharSequence message, int duration) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.view_app_toast, null);
        TextView tv = view.findViewById(R.id.tv_app_toast);
        tv.setText(message);

        Toast toast = new Toast(context.getApplicationContext());
        toast.setDuration(duration);
        toast.setView(view);
        toast.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, 160);
        return toast;
    }
}
