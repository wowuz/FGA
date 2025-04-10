# Fate/Grand Automata

Forked project to add a simple translation based on Gemini-2.0-flash.
By giving proper prompt, the quality of the translation is almost the official translation quality.
(Only based on my experience on CN and TW server and my learned-from-anime japanese.)

Features:
- Uses ML ocr provided by google
- Uses Gemini-2.0-flash, and configurable api-key and prompt
- Display an overlay to show the translation

Known problem:
- The ML ocr japanese accuracy is not good enough.
- Be careful, the api-key is saved as plain text, not safe, too lazy to make it secure.
- Also not my fault if you find your api token drains in light speed, I myself uses only free account.

To-be improved:
- Didn't find a way to get screenshot without catching the subtitles, and if the subtitles will jam the ocr text recognizing,
so the position and size of the subtitle window is fixed to avoid the ocr region.
- The ocr region are defined in a very untidy way, should defined it along side other locations. But I'm to lazy to do the math.

Unknown:
- Almost no testing, anyway I like it and am using it

Will be good if possible:
- Since the ML ocr japanese accuracy is not good enough, it's also an option to send the screen shot directly to LLM api.
I'm too poor to buy some token to test this.

Any comment or contribution is welcome, if you are patient enough to read the shits I wrote. (They are actually mostly written by Gemini though)

[![CI](https://github.com/Fate-Grand-Automata/FGA/workflows/CI/badge.svg?branch=master&event=push)](https://github.com/Fate-Grand-Automata/FGA/actions)

[![Discord Banner 2](https://discordapp.com/api/guilds/1117873862500163684/widget.png?style=banner2)](https://discord.gg/H99eUMYuH7)

Auto-battle app for FGO (Android 7 or later, no need for root on phones).

Download from our [website](https://fate-grand-automata.github.io)

This is a **Kotlin** port of [FGO-Lua][FGOLua] as an Android app with UI for configuration and without a time-limit on use.  
It doesn't tamper with the game in anyway and works by looking at the screen and tapping things just like a normal user would do.  
It's not made to do the story for you, but to automate the mundane farming.

Having Trouble? See the [Troubleshooting Guide](https://github.com/Fate-Grand-Automata/FGA/wiki/Troubleshooting) first.

## Video Guide by @reconman

[![Watch the video guide](https://img.youtube.com/vi/JOwupZ4W8AQ/sddefault.jpg)](https://youtu.be/JOwupZ4W8AQ)

## How to Use?

1. Install from the link given above and launch the app.
2. Click on `Start Service` and give all the permissions it asks for.
3. Open FGO. Now, you can see a button with play icon on it floating on screen.
4. Go to the node you want to farm.
5. Press Play to start. The same button can be pressed to pause/stop later.

Check the [Troubleshooting Guide](https://github.com/Fate-Grand-Automata/FGA/wiki/Troubleshooting) first if you face any problems.

## How to make/use images of Servant/CE/Friend?

See the wiki page for [Support Image Maker](https://github.com/Fate-Grand-Automata/FGA/wiki/Support-Image-Maker).

## What about other scripts like Lottery and Friend Gacha?

When you click on the PLAY button, the app detects which script can be run on the current screen and presents it to you.

## How does it work?

This is a native Android app written in Kotlin.
We use [OpenCV](https://opencv.org/) for image recognition,
[Media Projection](https://developer.android.com/reference/android/media/projection/MediaProjection) for taking screenshots
and [Accessibility Service](https://developer.android.com/guide/topics/ui/accessibility) for clicking/swiping.

## Contributing

If you want to contribute, read the [Contribution Guide](CONTRIBUTING.md).

## Acknowledgements

- [FGO-Lua][FGOLua] developers are the real deal. Without them this app won't exist.
- The icons are from https://materialdesignicons.com/
- Drag-sort logic on Card Priority screen is thanks to https://medium.com/@ipaulpro/drag-and-swipe-with-recyclerview-6a6f0c422efd

[FGOLua]: https://github.com/29988122/Fate-Grand-Order_Lua

## Like the project? Want to support us?

<a href='https://ko-fi.com/W7W0F7D9T' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://storage.ko-fi.com/cdn/kofi2.png?v=3' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

Code/doc contributions are surely welcome!

Translations should be edited via https://poeditor.com/join/project/67PXOyBGI0
