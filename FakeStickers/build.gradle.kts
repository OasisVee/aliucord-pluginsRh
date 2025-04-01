version = "1.2.0"
description = "Posts a sticker as an image if the sticker is unavailable normally (Usually when you don't have Nitro) with configurable sticker sizes."

aliucord {
    changelogMedia.set("https://cdn.discordapp.com/stickers/883809297216192573.png")
    changelog.set(
        """
            # 1.2.0
            * Added configurable sticker sizes
            * Settings menu to adjust sticker dimensions
            
            # 1.1.2
            * Support Discord 105.12
            # 1.1.1
            * Make sticker picker automatically close after selecting a sticker
            * Do not mark stickers as unusable (monochrome filter)
        """.trimIndent()
    )
    author("Vendicated & Accelerator", 343383572805058560L)
}
