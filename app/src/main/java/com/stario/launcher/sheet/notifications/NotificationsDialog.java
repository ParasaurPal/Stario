/*
 * Copyright (C) 2025 Răzvan Albu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>
 */

package com.stario.launcher.sheet.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.StatusBarNotification;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.stario.launcher.R;
import com.stario.launcher.services.NotificationService;
import com.stario.launcher.sheet.SheetDialogFragment;
import com.stario.launcher.sheet.SheetType;
import com.stario.launcher.themes.ThemedActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class NotificationsDialog extends SheetDialogFragment {
    private ThemedActivity activity;
    private LinearLayout list;
    private TextView status;

    public NotificationsDialog() {
        super();
    }

    public NotificationsDialog(@NonNull SheetType type) {
        super(type);
    }

    public static String getName() {
        return "Notification feed";
    }

    @Override
    public boolean requiresEagerInitialization() {
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        activity = (ThemedActivity) requireActivity();

        int surface = activity.getAttributeData(com.google.android.material.R.attr.colorSurface);
        int onSurface = activity.getAttributeData(com.google.android.material.R.attr.colorOnSurface);
        int secondary = activity.getAttributeData(com.google.android.material.R.attr.colorSecondary);

        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(20), dp(22), dp(20));
        root.setBackgroundColor(surface);

        TextView title = new TextView(activity);
        title.setText("Notifications");
        title.setTextColor(onSurface);
        title.setTextSize(26);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        status = new TextView(activity);
        status.setTextColor(secondary);
        status.setTextSize(14);
        status.setPadding(0, dp(5), 0, dp(12));
        root.addView(status);

        LinearLayout actions = new LinearLayout(activity);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        Button refresh = new Button(activity);
        refresh.setText("Refresh");
        refresh.setOnClickListener(v -> refreshNotifications());
        actions.addView(refresh, new LinearLayout.LayoutParams(0, dp(48), 1));
        Button clear = new Button(activity);
        clear.setText("Clear all");
        clear.setOnClickListener(v -> {
            NotificationService service = NotificationService.getInstance();
            if (service != null) {
                service.cancelAllNotifications();
                refreshNotifications();
            }
        });
        actions.addView(clear, new LinearLayout.LayoutParams(0, dp(48), 1));
        root.addView(actions);

        ScrollView scroll = new ScrollView(activity);
        scroll.setFillViewport(true);
        list = new LinearLayout(activity);
        list.setOrientation(LinearLayout.VERTICAL);
        list.setPadding(0, dp(12), 0, dp(20));
        scroll.addView(list, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        refreshNotifications();
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (list != null) {
            refreshNotifications();
        }
    }

    private void refreshNotifications() {
        if (list == null || activity == null) {
            return;
        }

        list.removeAllViews();
        if (!hasNotificationAccess()) {
            status.setText("Notification access is required to show your notifications.");
            TextView enable = makeText("Grant notification access", 16, true);
            enable.setGravity(Gravity.CENTER);
            enable.setPadding(dp(16), dp(18), dp(16), dp(18));
            enable.setOnClickListener(v -> activity.startActivity(
                    new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
            list.addView(enable);
            return;
        }

        NotificationService service = NotificationService.getInstance();
        if (service == null) {
            status.setText("Connecting to Android notifications…");
            return;
        }

        StatusBarNotification[] active;
        try {
            active = service.getActiveNotifications();
        } catch (SecurityException exception) {
            status.setText("Notification access is unavailable. Re-enable it in Android settings.");
            return;
        }

        List<StatusBarNotification> notifications = new ArrayList<>();
        if (active != null) {
            for (StatusBarNotification item : active) {
                if (item != null && (item.getNotification().flags & Notification.FLAG_GROUP_SUMMARY) == 0) {
                    notifications.add(item);
                }
            }
        }
        Collections.sort(notifications, Comparator.comparingLong(
                StatusBarNotification::getPostTime).reversed());

        status.setText(notifications.size() + (notifications.size() == 1
                ? " active notification" : " active notifications"));

        if (notifications.isEmpty()) {
            TextView empty = makeText("You're all caught up.", 16, false);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(dp(12), dp(36), dp(12), dp(36));
            list.addView(empty);
            return;
        }

        for (StatusBarNotification item : notifications) {
            addNotification(item);
        }
    }

    private void addNotification(StatusBarNotification item) {
        Notification notification = item.getNotification();
        Bundle extras = notification.extras;
        String title = extras != null ? extras.getString(Notification.EXTRA_TITLE) : null;
        CharSequence text = extras != null ? extras.getCharSequence(Notification.EXTRA_BIG_TEXT) : null;
        if (text == null && extras != null) {
            text = extras.getCharSequence(Notification.EXTRA_TEXT);
        }
        String appName = item.getPackageName();
        try {
            ApplicationInfo info = activity.getPackageManager().getApplicationInfo(item.getPackageName(), 0);
            appName = activity.getPackageManager().getApplicationLabel(info).toString();
        } catch (Exception ignored) {
            // Keep the package name when the app is no longer installed.
        }

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(dp(12), dp(12), dp(12), dp(12));
        card.setBackgroundResource(android.R.drawable.dialog_holo_light_frame);

        ImageView icon = new ImageView(activity);
        try {
            icon.setImageDrawable(activity.getPackageManager().getApplicationIcon(item.getPackageName()));
        } catch (Exception ignored) {
            icon.setImageResource(R.drawable.ic_notifications);
        }
        card.addView(icon, new LinearLayout.LayoutParams(dp(42), dp(42)));

        LinearLayout content = new LinearLayout(activity);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), 0, 0, 0);
        TextView app = makeText(appName + " · " +
                DateFormat.getTimeFormat(activity).format(item.getPostTime()), 12, false);
        TextView heading = makeText(title == null || title.trim().isEmpty()
                ? appName : title, 16, true);
        TextView body = makeText(text == null ? "" : text.toString(), 14, false);
        content.addView(app);
        content.addView(heading);
        if (body.getText().length() > 0) {
            content.addView(body);
        }
        card.addView(content, new LinearLayout.LayoutParams(0,
                ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        card.setOnClickListener(v -> {
            PendingIntent intent = notification.contentIntent;
            if (intent != null) {
                try {
                    intent.send();
                    hide(true);
                } catch (PendingIntent.CanceledException ignored) {
                    refreshNotifications();
                }
            }
        });
        card.setOnLongClickListener(v -> {
            NotificationService service = NotificationService.getInstance();
            if (service != null) {
                service.cancelNotification(item.getKey());
                refreshNotifications();
            }
            return true;
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = dp(8);
        list.addView(card, params);
    }

    private boolean hasNotificationAccess() {
        String enabled = Settings.Secure.getString(activity.getContentResolver(),
                "enabled_notification_listeners");
        ComponentName component = new ComponentName(activity, NotificationService.class);
        return enabled != null && enabled.contains(component.flattenToString());
    }

    private TextView makeText(String value, int size, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(activity.getAttributeData(
                com.google.android.material.R.attr.colorOnSurface));
        view.setTextSize(size);
        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }
        view.setPadding(0, dp(2), 0, dp(2));
        return view;
    }

    private int dp(float value) {
        return (int) (value * activity.getResources().getDisplayMetrics().density + 0.5f);
    }
}
