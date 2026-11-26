/* Copyright (C) 2026 ParasaurPal. Licensed under GPL-3.0-or-later. */
package com.stario.launcher.sheet.drawer.niagara;

import android.content.pm.ApplicationInfo;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.stario.launcher.R;
import com.stario.launcher.apps.LauncherApplication;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.sheet.drawer.list.ListAdapter;
import com.stario.launcher.themes.ThemedActivity;

public class NiagaraListAdapter extends ListAdapter {
    public NiagaraListAdapter(ThemedActivity activity, ProfileApplicationManager applications) {
        super(activity, applications);
    }

    @Override
    public void onBind(@NonNull ApplicationViewHolder holder, int position) {
        super.onBind(holder, position);
        LauncherApplication app = getApplication(position);
        int category = app == null ? ApplicationInfo.CATEGORY_UNDEFINED : app.getInfo().category;
        holder.setIcon(AppCompatResources.getDrawable(
                holder.itemView.getContext(), getCategoryIcon(category)));
    }

    private int getCategoryIcon(int category) {
        switch (category) {
            case ApplicationInfo.CATEGORY_GAME:
                return R.drawable.ic_niagara_category_games;
            case ApplicationInfo.CATEGORY_AUDIO:
                return R.drawable.ic_niagara_category_audio;
            case ApplicationInfo.CATEGORY_VIDEO:
                return R.drawable.ic_niagara_category_video;
            case ApplicationInfo.CATEGORY_IMAGE:
                return R.drawable.ic_niagara_category_photo;
            case ApplicationInfo.CATEGORY_SOCIAL:
                return R.drawable.ic_niagara_category_social;
            case ApplicationInfo.CATEGORY_NEWS:
                return R.drawable.ic_niagara_category_news;
            case ApplicationInfo.CATEGORY_MAPS:
                return R.drawable.ic_niagara_category_maps;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY:
                return R.drawable.ic_niagara_category_productivity;
            case ApplicationInfo.CATEGORY_ACCESSIBILITY:
                return R.drawable.ic_niagara_category_accessibility;
            default:
                return R.drawable.ic_niagara_category_apps;
        }
    }

    @Override
    protected int getLayout(int viewType) { return R.layout.recycler_niagara_application_item; }

    @Override
    protected int getLabelLineCount() { return 1; }
}
