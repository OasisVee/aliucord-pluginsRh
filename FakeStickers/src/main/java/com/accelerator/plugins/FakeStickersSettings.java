package com.accelerator.plugins;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;
import android.widget.SeekBar.OnSeekBarChangeListener;

import com.aliucord.api.SettingsAPI;
import com.aliucord.utils.DimenUtils;
import com.aliucord.views.Button;
import com.aliucord.widgets.BottomSheet;
import com.lytefast.flexinput.R;

@SuppressLint("SetTextI18n")
public class FakeStickersSettings extends BottomSheet {
    private static final int DEFAULT_STICKER_SIZE = 240;
    private final SettingsAPI settings;
    
    public FakeStickersSettings(SettingsAPI settings) {
        this.settings = settings;
    }
    
    @Override
    public void onViewCreated(View view, Bundle bundle) {
        super.onViewCreated(view, bundle);
        Context ctx = view.getContext();
        
        int stickerSize = settings.getInt("stickerSize", DEFAULT_STICKER_SIZE);
        
        TextView currentSize = new TextView(ctx, null, 0, R.i.UiKit_TextView);
        currentSize.setText(stickerSize + " px");
        currentSize.setWidth(DimenUtils.dpToPx(45));
        
        SeekBar slider = new SeekBar(ctx, null, 0, R.i.UiKit_SeekBar);
        LinearLayout.LayoutParams sliderParams = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        );
        slider.setLayoutParams(sliderParams);
        slider.setMax(700);
        slider.setProgress(stickerSize - 100);
        slider.setPadding(DimenUtils.dpToPx(12), 0, DimenUtils.dpToPx(12), 0);
        slider.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                currentSize.setText((progress + 100) + " px");
            }
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                settings.setInt("stickerSize", seekBar.getProgress() + 100);
            }
        });
        
        Button resetButton = new Button(ctx);
        resetButton.setText("Reset");
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
            LayoutParams.MATCH_PARENT, 
            LayoutParams.WRAP_CONTENT
        );
        buttonParams.setMargins(DimenUtils.dpToPx(12), 0, DimenUtils.dpToPx(12), 0);
        resetButton.setLayoutParams(buttonParams);
        resetButton.setOnClickListener(v -> {
            currentSize.setText("240 px");
            slider.setProgress(140);
            settings.setInt("stickerSize", 240);
        });
        
        TextView labelView = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label);
        labelView.setText("Sticker size (pixels)");
        addView(labelView);
        
        LinearLayout sliderLayout = new LinearLayout(ctx, null, 0, R.i.UiKit_Settings_Item);
        sliderLayout.addView(currentSize);
        sliderLayout.addView(slider);
        addView(sliderLayout);
        
        addView(resetButton);
        
        TextView noteView = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label);
        noteView.setText("Changes will apply after reloading the current channel");
        noteView.setTextSize(DimenUtils.dpToPx(4));
        addView(noteView);
    }
}
