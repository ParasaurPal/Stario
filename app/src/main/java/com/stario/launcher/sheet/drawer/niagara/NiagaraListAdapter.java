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
import com.stario.launcher.ui.popup.PopupMenu;

public class NiagaraListAdapter extends ListAdapter {
    private final ThemedActivity activity;

    public NiagaraListAdapter(ThemedActivity activity, ProfileApplicationManager applications) {
        super(activity, applications);
        this.activity = activity;
    }

    @Override
    public void onBind(@NonNull ApplicationViewHolder holder, int position) {
        super.onBind(holder, position);
        LauncherApplication app = getApplication(position);
        if (app == null) return;
        int category = getAutomaticCategory(app.getInfo().category);
        holder.setIcon(NiagaraIconPicker.icon(holder.itemView.getContext(), app, category));
    }

    private int getAutomaticCategory(int category) {
        switch (category) {
            case ApplicationInfo.CATEGORY_GAME: return 33;
            case ApplicationInfo.CATEGORY_AUDIO: return 20;
            case ApplicationInfo.CATEGORY_VIDEO: return 31;
            case ApplicationInfo.CATEGORY_IMAGE: return 24;
            case ApplicationInfo.CATEGORY_SOCIAL: return 27;
            case ApplicationInfo.CATEGORY_NEWS: return 21;
            case ApplicationInfo.CATEGORY_MAPS: return 18;
            case ApplicationInfo.CATEGORY_PRODUCTIVITY: return 25;
            case ApplicationInfo.CATEGORY_ACCESSIBILITY: return 29;
            default: return 0;
        }
    }

    @Override
    protected void addCustomPopupItems(PopupMenu menu, LauncherApplication application) {
        menu.add(new PopupMenu.Item("Change Niagara icon",
                AppCompatResources.getDrawable(activity, R.drawable.ic_edit),
                view -> NiagaraIconPicker.show(activity, application,
                        getAutomaticCategory(application.getInfo().category),
                        this::notifyDataSetChanged)));
    }

    @Override
    protected int getLayout(int viewType) { return R.layout.recycler_niagara_application_item; }

    @Override
    protected int getLabelLineCount() { return 1; }
}
