/* Copyright (C) 2026 ParasaurPal. Licensed under GPL-3.0-or-later. */
package com.stario.launcher.sheet.drawer.niagara;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.stario.launcher.R;
import com.stario.launcher.apps.ProfileApplicationManager;
import com.stario.launcher.apps.ProfileManager;
import com.stario.launcher.sheet.drawer.DrawerPage;
import com.stario.launcher.ui.Measurements;
import com.stario.launcher.ui.recyclers.FastScroller;

public class NiagaraList extends DrawerPage {
    private FastScroller fastScroller;
    private ProfileApplicationManager applications;

    public NiagaraList() { super(); }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = super.onCreateView(inflater, container, savedInstanceState);
        fastScroller = root.findViewById(R.id.fast_scroller);
        drawer.setLayoutManager(new LinearLayoutManager(activity));
        drawer.setItemAnimator(null);
        Measurements.addStatusBarListener(value -> fastScroller.setTopOffset(drawer.getPaddingTop()));
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        title.setText(R.string.apps);
        applications = ProfileManager.getInstance().getProfile(0);
        if (applications == null) return;

        drawer.setAdapter(new NiagaraListAdapter(activity, applications));
        applications.addOnReadyListener(manager -> showLayout());
        drawer.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                applyWave();
            }
        });
        drawer.post(this::applyWave);

        View searchContainer = (View) search.getParent();
        search.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                fastScroller.setBottomOffset(searchContainer.getPaddingBottom() + (bottom - top)
                        + Measurements.spToPx(32) + Measurements.dpToPx(20)));
    }

    private void applyWave() {
        if (drawer == null) return;
        float center = drawer.getHeight() / 2f;
        float wavelength = Measurements.dpToPx(78);
        float amplitude = Measurements.dpToPx(17);
        for (int i = 0; i < drawer.getChildCount(); i++) {
            View child = drawer.getChildAt(i);
            float distance = child.getY() + child.getHeight() / 2f - center;
            float phase = distance / wavelength;
            child.setTranslationX((float) Math.sin(phase) * amplitude);
            child.setScaleX(1f + 0.025f * (float) Math.cos(phase));
        }
    }

    @Override
    public void setSelected(boolean selected) {
        super.setSelected(selected);
        if (fastScroller != null) fastScroller.animateVisibility(selected);
    }

    @Override
    public void onResume() {
        if (title != null) title.setText(R.string.apps);
        super.onResume();
    }

    @Override
    protected int getLayoutResID() { return R.layout.drawer_list; }
}
