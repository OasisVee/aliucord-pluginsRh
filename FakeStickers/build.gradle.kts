version = "1.2.0"
description = "Posts a sticker as an image if the sticker is unavailable normally (Usually when you don't have Nitro). Lottie stickers are unsupported. (Stickers Animate client side only in this fork, no need for AnimateApngs plugin)"

aliucord {
    changelogMedia.set("https://cdn.discordapp.com/stickers/883809297216192573.png")
    changelog.set(
        """
            # 1.2.0
            * Implemented AnimateApngs into code

            # 1.1.1
            * Support Discord 105.12

            # 1.1.0
            * Make sticker picker automatically close after selecting a sticker
            * Do not mark stickers as unusable (monochrome filter)
        """.trimIndent()
    )
    author("Vendicated", 343383572805058560L)
}
