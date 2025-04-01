package com.accelerator.plugins;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.api.SettingsAPI;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.*;
import com.aliucord.Logger;
import com.aliucord.Utils;
import com.aliucord.utils.DimenUtils;
import com.aliucord.utils.ReflectUtils;
import com.aliucord.views.Button;
import com.aliucord.widgets.BottomSheet;
import com.discord.widgets.chat.input.WidgetChatInputAttachments;
import com.discord.widgets.chat.input.WidgetChatInputAttachments$createAndConfigureExpressionFragment$stickerPickerListener$1;
import com.discord.widgets.chat.input.sticker.*;
import com.discord.utilities.stickers.StickerUtils;
import com.discord.utilities.rest.RestAPI;
import com.discord.restapi.RestAPIParams;
import com.discord.models.domain.NonceGenerator;
import com.discord.utilities.time.ClockFactory;
import com.aliucord.utils.RxUtils;
import com.discord.databinding.WidgetChatListAdapterItemStickerBinding;
import com.discord.widgets.chat.list.adapter.WidgetChatListAdapterItemSticker;
import com.discord.widgets.chat.list.entries.ChatListEntry;
import com.lytefast.flexinput.R;

import java.util.Collections;
import java.lang.reflect.Field;
import com.discord.stores.StoreStream;

// Add the missing import
import com.aliucord.patcher.AfterHook;

@SuppressWarnings("unused")
@AliucordPlugin
public class FakeStickers extends Plugin {

    private static final String DEFAULT_STICKER_SIZE = "240";
    private Field bindingField;
    private Logger logger = new Logger("FakeStickers");

    public FakeStickers() {
        try {
            bindingField = WidgetChatListAdapterItemSticker.class.getDeclaredField("binding");
            bindingField.setAccessible(true);
        } catch (NoSuchFieldException e) {
            logger.error("Failed to get binding field", e);
        }
    }

    @Override
    public void start(Context context) throws Throwable {
        // Register settings tab
        settings.setString("enhanced_stickers", "enabled");
        settingsTab = new SettingsTab(FakeStickersSettings.class, SettingsTab.Type.BOTTOM_SHEET).withArgs(settings);

        // Do not mark stickers as unsendable (grey overlay)
        patcher.patch(StickerItem.class.getDeclaredMethod("getSendability"), InsteadHook.returnConstant(StickerUtils.StickerSendability.SENDABLE));

        // Patch onClick to send sticker
        patcher.patch(WidgetStickerPicker.class.getDeclaredMethod("onStickerItemSelected", StickerItem.class), new PreHook(param -> {
            try {
                // getSendability is patched above to always return SENDABLE so get the real value via reflect
                if (ReflectUtils.getField(param.args[0], "sendability") == StickerUtils.StickerSendability.SENDABLE) return;

                var sticker = ((StickerItem) param.args[0]).getSticker();

                RestAPIParams.Message message = new RestAPIParams.Message(
                    "https://media.discordapp.net/stickers/"+sticker.d()+sticker.b()+"?size=160",
                    Long.toString(NonceGenerator.computeNonce(ClockFactory.get())),
                    null,
                    null,
                    Collections.emptyList(),
                    null,
                    new RestAPIParams.Message.AllowedMentions(
                        Collections.emptyList(),
                        Collections.emptyList(),
                        Collections.emptyList(),
                        false
                    ),
                    null,
                    null
                );
                logger.debug(message.toString());
                Utils.threadPool.execute(() -> {
                    // Subscriptions in Java, because you can't do msg.subscribe() like in Kotlin
                    RxUtils.subscribe(
                        RestAPI.getApi().sendMessage(StoreStream.getChannelsSelected().getId(), message),
                        RxUtils.createActionSubscriber(zz -> {})
                    );
                });

                // Skip original method
                param.setResult(null);

                // Dismiss sticker picker
                var stickerListener = (WidgetChatInputAttachments$createAndConfigureExpressionFragment$stickerPickerListener$1)
                    ReflectUtils.getField(param.thisObject, "stickerPickerListener");
                WidgetChatInputAttachments.access$getFlexInputFragment$p(stickerListener.this$0).s.hideExpressionTray();
            } catch (Throwable ignored) {
                logger.error("Error in sticker selection patch", ignored);
            }
        }));

        // Add configurable sticker size feature
        patcher.patch(WidgetChatListAdapterItemSticker.class.getDeclaredMethod("onConfigure", int.class, ChatListEntry.class), new AfterHook(param -> {
            try {
                if (bindingField != null) {
                    WidgetChatListAdapterItemStickerBinding binding = (WidgetChatListAdapterItemStickerBinding) bindingField.get(param.thisObject);
                    int stickerSize = Integer.parseInt(settings.getString("stickerSize", DEFAULT_STICKER_SIZE));
                    
                    binding.b.getLayoutParams().height = stickerSize;
                    binding.b.getLayoutParams().width = stickerSize;
                }
            } catch (Exception e) {
                logger.error("Error in sticker size patch", e);
            }
        }));
    }

    @Override
    public void stop(Context context) {
        patcher.unpatchAll();
    }

    public static class FakeStickersSettings extends BottomSheet {
        private final SettingsAPI settings;
        private int stickerSize;

        public FakeStickersSettings(SettingsAPI settings) {
            this.settings = settings;
            this.stickerSize = Integer.parseInt(settings.getString("stickerSize", DEFAULT_STICKER_SIZE));
        }

        @Override
        public void onViewCreated(View view, Bundle bundle) {
            super.onViewCreated(view, bundle);
            Context ctx = view.getContext();

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
            slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {}

                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    currentSize.setText((progress + 100) + " px");
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    stickerSize = seekBar.getProgress() + 100;
                    settings.setString("stickerSize", String.valueOf(stickerSize));
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
                stickerSize = 240;
                settings.setString("stickerSize", DEFAULT_STICKER_SIZE);
            });

            TextView title = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label);
            title.setText("Sticker size (pixels)");
            addView(title);

            LinearLayout sliderContainer = new LinearLayout(ctx, null, 0, R.i.UiKit_Settings_Item);
            sliderContainer.addView(currentSize);
            sliderContainer.addView(slider);
            addView(sliderContainer);

            addView(resetButton);

            TextView note = new TextView(ctx, null, 0, R.i.UiKit_Settings_Item_Label);
            note.setText("Changes will apply after reloading the current channel");
            note.setTextSize(DimenUtils.dpToPx(4));
            addView(note);
        }
    }
}
