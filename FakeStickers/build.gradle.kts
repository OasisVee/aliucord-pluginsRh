version = "1.2.0"
description = "Posts a sticker as an image if the sticker is unavailable normally (Usually when you don't have Nitro). Sticker sizes are now configurable. Lottie stickers are unsupported."

aliucord {
    changelogMedia.set("https://cdn.discordapp.com/stickers/883809297216192573.png")
    changelog.set(
        """
            # 1.2.0
            * Added configurable sticker sizes
            * Improved size handling for sticker images
            
            # 1.1.1
            * Support Discord 105.12
            # 1.1.0
            * Make sticker picker automatically close after selecting a sticker
            * Do not mark stickers as unusable (monochrome filter)
        """.trimIndent()
    )
    author("Vendicated", 343383572805058560L)
}
