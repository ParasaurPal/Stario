/* Copyright (C) 2026 ParasaurPal. Licensed under GPL-3.0-or-later. */
package com.stario.launcher.sheet.drawer.niagara;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.stario.launcher.R;
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
        Drawable generic = AppCompatResources.getDrawable(
                holder.itemView.getContext(), R.drawable.ic_niagara_generic);
        holder.setIcon(generic);
    }

    @Override
    protected int getLayout(int viewType) { return R.layout.recycler_niagara_application_item; }

    @Override
    protected int getLabelLineCount() { return 1; }
}
