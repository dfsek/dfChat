package com.dfsek.dfchat.ui.settings

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.dfsek.dfchat.AppState
import com.dfsek.dfchat.AppState.Preferences.updatePrefs
import com.dfsek.dfchat.util.GENERAL_PREFS
import kotlin.math.roundToInt

class GeneralActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val preferences = remember { getSharedPreferences(GENERAL_PREFS, MODE_PRIVATE) }

            MaterialTheme(colors = AppState.themeColors) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column {
                        SettingsTopBar("General")
                        SettingsItem("Avatar Size in DM List") {
                            SliderValue(
                                min = 16,
                                max = 128,
                                default = 48,
                                preference = AVATAR_SIZE_DMS_KEY,
                                descriptor = " pixels",
                                preferences = preferences,
                                modifier = it
                            )
                        }
                        SettingsItem("Avatar Size in Space List") {
                            SliderValue(
                                min = 16,
                                max = 64,
                                default = 36,
                                preference = AVATAR_SIZE_SPACES_KEY,
                                descriptor = " pixels",
                                preferences = preferences,
                                modifier = it
                            )
                        }
                        SettingsItem("Avatar Size in Chat Rooms") {
                            SliderValue(
                                min = 16,
                                max = 128,
                                default = 48,
                                preference = AVATAR_SIZE_ROOMS_KEY,
                                descriptor = " pixels",
                                preferences = preferences,
                                modifier = it
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun SettingsItem(text: String, content: @Composable (Modifier) -> Unit) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text(text, fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                    content(Modifier.padding(6.dp))
                }
            }
            Divider()
        }
    }

    @Composable
    fun SliderValue(min: Int, max: Int, default: Int = (max + min) / 2, preference: String, descriptor: String = "", preferences: SharedPreferences, modifier: Modifier) {
        var savedValue by remember { mutableStateOf(preferences.getInt(preference, default)) }
        Column {
            Text("$savedValue$descriptor", modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(modifier = modifier) {
                Text(min.toString(), modifier = Modifier.align(Alignment.CenterVertically))
                Slider(value = savedValue.toFloat(), onValueChange = {
                    val round = it.roundToInt()
                    preferences.edit {
                        putInt(preference, round)
                    }
                    updatePrefs()
                    savedValue = round
                }, valueRange = min.toFloat()..max.toFloat(), modifier = Modifier.weight(1f).align(Alignment.CenterVertically))
                Text(max.toString(), modifier = Modifier.align(Alignment.CenterVertically))
            }
        }
    }


    companion object {
        internal const val AVATAR_SIZE_SPACES_KEY = "avatarSizeSpaces"
        internal const val AVATAR_SIZE_DMS_KEY = "avatarSizeDMs"
        internal const val AVATAR_SIZE_ROOMS_KEY = "avatarSizeRooms"
    }
}