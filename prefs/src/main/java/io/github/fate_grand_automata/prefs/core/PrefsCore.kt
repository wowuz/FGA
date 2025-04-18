package io.github.fate_grand_automata.prefs.core

import android.content.Context
import com.fredporciuncula.flow.preferences.Serializer
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.fate_grand_automata.scripts.enums.GameServer
import io.github.fate_grand_automata.scripts.enums.ScriptModeEnum
import io.github.lib_automata.Location
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PrefsCore @Inject constructor(
    maker: PrefMaker,
    @ApplicationContext val context: Context
) {
    companion object {
        const val GAME_SERVER_AUTO_DETECT = "auto_detect"

        // increase for each changed Onboarding screen
        const val CURRENT_ONBOARDING_VERSION = 2
    }

    val onboardingCompletedVersion = maker.int("onboarding_completed_version")

    val scriptMode = maker.enum("script_mode", ScriptModeEnum.Battle)

    val gameServerRaw = maker.string(
        "game_server",
        GAME_SERVER_AUTO_DETECT
    )

    val battleConfigList = maker.stringSet("autoskill_list")

    val storySkip = maker.bool("story_skip")
    val withdrawEnabled = maker.bool("withdraw_enabled")

    val stopOnCEGet = maker.bool("stop_on_ce_get")
    val stopOnFirstClearRewards = maker.bool("stop_on_first_clear_rewards")

    val boostItemSelectionMode = maker.stringAsInt("selected_boost_item", -1)

    val useRootForScreenshots = maker.bool("use_root_screenshot")
    val recordScreen = maker.bool("record_screen")
    val screenshotDrops = maker.bool("screenshot_drops")
    val screenshotDropsUnmodified = maker.bool("screenshot_drops_unmodified")
    val screenshotBond = maker.bool("screenshot_bond")
    val hidePlayButton = maker.bool("hide_play_button")
    val debugMode = maker.bool("debug_mode")
    val autoStartService = maker.bool("auto_start_service")

    val hideSQInAPResources = maker.bool("hide_sq_in_ap_resources")

    val shouldLimitFP = maker.bool("should_fp_limit")
    val limitFP = maker.int("fp_limit", 1)
    val receiveEmbersWhenGiftBoxFull = maker.bool("receive_embers_when_gift_box_full")

    val supportSwipesPerUpdate = maker.int("support_swipes_per_update_x", 10)
    val supportMaxUpdates = maker.int("support_max_updates_x", 5)

    val minSimilarity = maker.int("min_similarity", 80)
    val mlbSimilarity = maker.int("mlb_similarity", 70)
    val stageCounterSimilarity = maker.int("stage_counter_similarity", 85)
    val stageCounterNew = maker.bool("stage_counter_new")

    val skillDelay = maker.int("skill_delay", 500)
    val waitMultiplier = maker.int("wait_multiplier", 100)
    val waitBeforeTurn = maker.int("wait_before_turn", 500)
    val waitBeforeCards = maker.int("wait_before_cards", 2000)

    val clickWaitTime = maker.int("click_wait_time", 300)
    val clickDuration = maker.int("click_duration", 50)
    val clickDelay = maker.int("click_delay", 10)

    val swipeWaitTime = maker.int("swipe_wait_time", 700)
    val swipeDuration = maker.int("swipe_duration", 300)
    val swipeMultiplier = maker.int("swipe_multiplier", 100)

    val maxGoldEmberSetSize = maker.int("max_gold_ember_set_size", 1)
    val maxGoldEmberTotalCount = maker.int("max_gold_ember_total_count", 100)

    val ceBombTargetRarity = maker.int("ce_bomb_target_rarity", 1)

    val stopAfterThisRun = maker.bool("stop_after_this_run")
    val skipServantFaceCardCheck = maker.bool("skip_servant_face_card_check")
    val treatSupportLikeOwnServant = maker.bool("treat_support_like_own_servant")

    val playBtnLocation = maker.serialized(
        "play_btn_location",
        serializer = object : Serializer<Location> {
            override fun deserialize(serialized: String) =
                try {
                    val split = serialized.split(',')

                    Location(split[0].toInt(), split[1].toInt())
                } catch (e: Exception) {
                    Location()
                }

            override fun serialize(value: Location) =
                "${value.x},${value.y}"
        },
        default = Location()
    )

    val gameAreaMode = maker.enum("game_area_mode", GameAreaMode.Default)
    val gameOffsetLeft = maker.int("game_offset_left", 0)
    val gameOffsetTop = maker.int("game_offset_top", 0)
    val gameOffsetRight = maker.int("game_offset_right", 0)
    val gameOffsetBottom = maker.int("game_offset_bottom", 0)

    val dirRoot = maker.string("dir_root")

    var showGameServer = maker.serialized(
        key = "show_game_server",
        default = listOf(GameServer.default),
        serializer = object : Serializer<List<GameServer>> {
            private val separator = ","
            override fun deserialize(serialized: String): List<GameServer> {
                val values = serialized.split(separator)
                return values.mapNotNull { GameServer.deserialize(it) }
            }

            override fun serialize(value: List<GameServer>): String = value.joinToString(separator)

        }
    )

    val defaultTranslateInstruction = "You are a highly skilled translation engine." +
            " Your function is to translate OCR texts from Fate Grand Order accurately into target language," +
            " ensuring that the original tone and cultural nuances are preserved." +
            " You will also try to improve the translation quality according to the previous translation." +
            " Please also try to correct the possibly in-accurate OCR recognizing." +
            " You are expert in the Type-moon world, knowing all those terms and characters in type-moon stories." +
            " 選択肢 is only a info implying that the texts inside {} are recognized in the top part of the screen, " +
            " so please ignore the 選択肢 and the curly braces in the output." +
            " Avoid adding any explanations or annotations to the translated text."

    val defaultImageTranslateInstruction = "You are a highly skilled translation engine." +
            " Your function is to translate the texts in the attached screenshot from Fate Grand Order accurately into target language," +
            " ensuring that the original tone and cultural nuances are preserved." +
            " You will also try to improve the translation quality according to the previous translation." +
            " You are expert in the Type-moon world, knowing all those terms and characters in type-moon stories." +
            " Avoid adding any explanations or annotations to the translated text."

    // TODO: this is definitely not safe, right?
    val autoTranslateApiKey = maker.string("auto_translate_api_key", "YOUR_GEMINI_API_KEY")
    val autoTranslateTargetLanguage = maker.string("auto_translate_target_lang", "Traditional Chinese")
    val autoTranslateInstruction = maker.string("auto_translate_target_lang", defaultTranslateInstruction)
    val autoImageTranslateInstruction = maker.string("auto_image_translate_target_lang", defaultImageTranslateInstruction)
    val autoTranslateOcrRegionX = maker.int("auto_translate_ocr_region_x", 100) // Default X
    val autoTranslateOcrRegionY = maker.int("auto_translate_ocr_region_y", 50)  // Default Y
    val autoTranslateOcrRegionWidth = maker.int("auto_translate_ocr_region_width", 2360) // Default Width
    val autoTranslateOcrRegionHeight = maker.int("auto_translate_ocr_region_height", 200) // Default Height

    val autoTranslateImageInputSwitch = maker.bool("auto_translate_image_input_switch", false)
    val autoTranslateModel = maker.string("auto_translate_model", "gemini-2.0-flash")
    val autoTranslateChatMode = maker.bool("auto_translate_chat_mode", false)

    val subtitleOverlayX = maker.int("subtitle_overlay_x", 0) // Default X (center gravity handles initial horizontal)
    val subtitleOverlayY = maker.int("subtitle_overlay_y", 150) // Default Y (offset from bottom)
    val subtitleOverlayLocked = maker.bool("subtitle_overlay_locked", true) // Default unlocked
    val subtitleOverlayWidth = maker.int("subtitle_overlay_width", 28) // Default width in percentage
    val subtitleOverlayHeight = maker.int("subtitle_overlay_height", 50) // Default height in percentage

    private val battleConfigMap = mutableMapOf<String, BattleConfigCore>()

    fun forBattleConfig(id: String): BattleConfigCore =
        battleConfigMap.getOrPut(id) {
            BattleConfigCore(
                id,
                context
            )
        }

    fun removeBattleConfig(id: String) = battleConfigMap.remove(id)

    private val perServerConfigPrefsMap = mutableMapOf<String, PerServerConfigPrefsCore>()

    fun forPerServerConfigPrefs(gameServer: GameServer): PerServerConfigPrefsCore =
        perServerConfigPrefsMap.getOrPut(gameServer.simple) {
            PerServerConfigPrefsCore(
                gameServer,
                context
            )
        }

    val servantEnhancement = ServantEnhancementPrefsCore(maker)

}