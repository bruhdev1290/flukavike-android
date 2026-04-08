package com.fluxer.client.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fluxer.client.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageScreen(
    onBack: () -> Unit
) {
    var selectedLanguage by remember { mutableStateOf(Language.ENGLISH_US) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Language",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VelvetDark,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = VelvetBlack
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            
            // Suggested Languages
            SettingsSection(title = "Suggested") {
                LanguageItem(
                    language = Language.ENGLISH_US,
                    isSelected = selectedLanguage == Language.ENGLISH_US,
                    onClick = { selectedLanguage = Language.ENGLISH_US }
                )
                SettingsDivider()
                LanguageItem(
                    language = Language.ENGLISH_UK,
                    isSelected = selectedLanguage == Language.ENGLISH_UK,
                    onClick = { selectedLanguage = Language.ENGLISH_UK }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // All Languages
            SettingsSection(title = "All Languages") {
                Language.entries.forEachIndexed { index, language ->
                    LanguageItem(
                        language = language,
                        isSelected = selectedLanguage == language,
                        onClick = { selectedLanguage = language }
                    )
                    if (index < Language.entries.size - 1) {
                        SettingsDivider()
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

enum class Language(
    val displayName: String,
    val nativeName: String
) {
    ENGLISH_US("English (US)", "English"),
    ENGLISH_UK("English (UK)", "English"),
    SPANISH("Spanish", "Español"),
    FRENCH("French", "Français"),
    GERMAN("German", "Deutsch"),
    ITALIAN("Italian", "Italiano"),
    PORTUGUESE("Portuguese", "Português"),
    RUSSIAN("Russian", "Русский"),
    JAPANESE("Japanese", "日本語"),
    KOREAN("Korean", "한국어"),
    CHINESE_SIMPLIFIED("Chinese (Simplified)", "简体中文"),
    CHINESE_TRADITIONAL("Chinese (Traditional)", "繁體中文"),
    ARABIC("Arabic", "العربية"),
    HINDI("Hindi", "हिन्दी"),
    TURKISH("Turkish", "Türkçe"),
    POLISH("Polish", "Polski"),
    DUTCH("Dutch", "Nederlands"),
    SWEDISH("Swedish", "Svenska"),
    NORWEGIAN("Norwegian", "Norsk"),
    DANISH("Danish", "Dansk"),
    FINNISH("Finnish", "Suomi"),
    CZECH("Czech", "Čeština"),
    UKRAINIAN("Ukrainian", "Українська"),
    VIETNAMESE("Vietnamese", "Tiếng Việt"),
    THAI("Thai", "ไทย"),
    INDONESIAN("Indonesian", "Bahasa Indonesia"),
    MALAY("Malay", "Bahasa Melayu"),
    FILIPINO("Filipino", "Filipino"),
    ROMANIAN("Romanian", "Română"),
    GREEK("Greek", "Ελληνικά"),
    HEBREW("Hebrew", "עברית"),
    PERSIAN("Persian", "فارسی"),
    BENGALI("Bengali", "বাংলা"),
    TAMIL("Tamil", "தமிழ்"),
    TELUGU("Telugu", "తెలుగు"),
    MARATHI("Marathi", "मराठी"),
    URDU("Urdu", "اردو"),
    PUNJABI("Punjabi", "ਪੰਜਾਬੀ"),
    GUJARATI("Gujarati", "ગુજરાતી"),
    KANNADA("Kannada", "ಕನ್ನಡ"),
    MALAYALAM("Malayalam", "മലയാളം"),
    SINHALA("Sinhala", "සිංහල"),
    NEPALI("Nepali", "नेपाली"),
    BURMESE("Burmese", "မြန်မာဘာသာ"),
    KHMER("Khmer", "ខ្មែរ"),
    LAO("Lao", "ລາວ"),
    MONGOLIAN("Mongolian", "Монгол"),
    KAZAKH("Kazakh", "Қазақша"),
    UZBEK("Uzbek", "O'zbek"),
    AZERBAIJANI("Azerbaijani", "Azərbaycan"),
    GEORGIAN("Georgian", "ქართული"),
    ARMENIAN("Armenian", "Հայերեն"),
    ALBANIAN("Albanian", "Shqip"),
    SERBIAN("Serbian", "Српски"),
    CROATIAN("Croatian", "Hrvatski"),
    BOSNIAN("Bosnian", "Bosanski"),
    MACEDONIAN("Macedonian", "Македонски"),
    BULGARIAN("Bulgarian", "Български"),
    SLOVENIAN("Slovenian", "Slovenščina"),
    SLOVAK("Slovak", "Slovenčina"),
    HUNGARIAN("Hungarian", "Magyar"),
    LITHUANIAN("Lithuanian", "Lietuvių"),
    LATVIAN("Latvian", "Latviešu"),
    ESTONIAN("Estonian", "Eesti"),
    CATALAN("Catalan", "Català"),
    BASQUE("Basque", "Euskara"),
    GALICIAN("Galician", "Galego"),
    WELSH("Welsh", "Cymraeg"),
    IRISH("Irish", "Gaeilge"),
    SCOTTISH_GAELIC("Scottish Gaelic", "Gàidhlig"),
    BRETON("Breton", "Brezhoneg"),
    CORSICAN("Corsican", "Corsu"),
    AFRIKAANS("Afrikaans", "Afrikaans"),
    SWAHILI("Swahili", "Kiswahili"),
    HAUSA("Hausa", "Hausa"),
    YORUBA("Yoruba", "Yorùbá"),
    IGBO("Igbo", "Igbo"),
    ZULU("Zulu", "isiZulu"),
    XHOSA("Xhosa", "isiXhosa"),
    AMHARIC("Amharic", "አማርኛ"),
    TIGRINYA("Tigrinya", "ትግርኛ"),
    SOMALI("Somali", "Soomaali"),
    OROMO("Oromo", "Oromoo"),
    MALAGASY("Malagasy", "Malagasy"),
    SESOTHO("Sesotho", "Sesotho"),
    SETSWANA("Setswana", "Setswana"),
    CHICHEWA("Chichewa", "Chichewa"),
    KINYARWANDA("Kinyarwanda", "Kinyarwanda"),
    KIRUNDI("Kirundi", "Kirundi"),
    WOLOF("Wolof", "Wolof"),
    LUGANDA("Luganda", "Luganda"),
    SHONA("Shona", "Shona")
}

@Composable
private fun LanguageItem(
    language: Language,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        color = Color.Transparent
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.displayName,
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            }
            
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = PhantomRed,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
