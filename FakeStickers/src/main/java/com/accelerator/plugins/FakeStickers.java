package com.accelerator.plugins;

import android.content.Context;
import android.widget.ImageView;

import com.aliucord.Http;
import com.aliucord.Utils;
import com.aliucord.annotations.AliucordPlugin;
import com.aliucord.entities.Plugin;
import com.aliucord.patcher.InsteadHook;
import com.aliucord.patcher.PreHook;
import com.aliucord.utils.ReflectUtils;

import com.discord.widgets.chat.input.WidgetChatInputAttachments;
import com.discord.widgets.chat.input.sticker.*;
import com.discord.utilities.stickers.StickerUtils;
import com.discord.utilities.rest.RestAPI;
import com.discord.restapi.RestAPIParams;
import com.discord.models.domain.NonceGenerator;
import com.discord.utilities.time.ClockFactory;
import com.aliucord.utils.RxUtils;
import java.util.Collections;
import com.discord.stores.StoreStream;
import java.lang.reflect.Method;

@SuppressWarnings("unused")
@AliucordPlugin
public class FakeStickers extends Plugin {

    private void initApngSticker(ImageView view, String stickerUrl, Integer w, Integer h) {
        final var url = stickerUrl
                // Replace media domain with cdn to ensure APNG support
                .replace("media.discordapp.net", "cdn.discordapp.com");

        Utils.threadPool.execute(() -> {
            try (var is = new Http.Request(url).execute().stream()) {
                var drawable = b.l.a.a.a(is, w != null ? w : 160, h != null ? h : 160);
                if (view != null)
                    Utils.mainThread.post(() -> {
                        view.setImageDrawable(drawable);
                        drawable.start();
                    });
            } catch (Throwable ignored) { }
        });
    }

    @Override
    public void start(Context context) throws Throwable {
        // Do not mark stickers as unsendable (grey overlay)
        patcher.patch(StickerItem.class.getDeclaredMethod("getSendability"), InsteadHook.returnConstant(StickerUtils.StickerSendability.SENDABLE));

        // Patch StickerViewHolder to support animated stickers
        patcher.patch(StickerViewHolder.class.getDeclaredMethod("configureSticker", Object.class), new PreHook(param -> {
            try {
                var stickerItem = (StickerItem) param.args[0];
                var sticker = stickerItem.getSticker();
                
                // Get the binding and StickerView via reflection
                Object binding = ReflectUtils.getField(param.thisObject, "binding");
                Object stickerView = ReflectUtils.getField(binding, "b");

                // Use reflection to set alpha
                Method setAlphaMethod = stickerView.getClass().getMethod("setAlpha", float.class);
                setAlphaMethod.invoke(stickerView, 1.0f);
                
                // Add animation support for PNG/APNG stickers
                if (sticker != null) {
                    var stickerUrl = "https://media.discordapp.net/stickers/" + sticker.d() + sticker.b() + "?size=160";
                    initApngSticker((ImageView) stickerView, stickerUrl, 160, 160);
                }
            } catch (Throwable ignored) {}
        }));

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

                Utils.threadPool.execute(() -> {
                    RxUtils.subscribe(
                            RestAPI.getApi().sendMessage(StoreStream.getChannelsSelected().getId(), message),
                            RxUtils.createActionSubscriber(zz -> {})
                    );
                });

                // Skip original method
                param.setResult(null);

                // Use different approach to dismiss sticker picker
                Object flexInputFragment = ReflectUtils.getField(param.thisObject, "flexInputFragment");
                Object flexInputViewModel = ReflectUtils.getField(flexInputFragment, "r");
                Method hideMethod = flexInputViewModel.getClass().getMethod("hideExpressionTray");
                hideMethod.invoke(flexInputViewModel);
            } catch (Throwable ignored) {
            }
        }));
    }

    @Override
    public void stop(Context context) {
        // Remove all patches
        patcher.unpatchAll();
    }
}
