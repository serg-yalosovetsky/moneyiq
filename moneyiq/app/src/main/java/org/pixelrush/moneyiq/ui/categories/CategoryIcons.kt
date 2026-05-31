package org.pixelrush.moneyiq.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

internal val CATEGORY_FORM_COLORS = listOf(
    "#C2185B", "#AD1457", "#D81B60", "#F50057", "#F73579",
    "#BA68C8", "#AB47BC", "#9C27B0", "#8E24AA", "#7B1FA2",
    "#6A1B9A", "#4A148C", "#AA00FF", "#D500F9", "#E040FB",
    "#9575CD", "#7E57C2", "#673AB7", "#5E35B1", "#512DA8",
    "#4527A0", "#311B92", "#6200EA", "#651FFF", "#7C4DFF",
    "#7986CB", "#5C6BC0", "#3F51B5", "#3949AB", "#303F9F",
    "#283593", "#1A237E", "#304FFE", "#3D5AFE", "#536DFE",
    "#64B5F6", "#42A5F5", "#2196F3", "#1E88E5", "#1976D2",
    "#009688", "#00897B", "#00695C", "#48B456", "#2E7D32",
    "#FFA834", "#F9A825", "#FF5722", "#795548", "#607D8B"
)

internal val CATEGORY_ICONS_LIST: List<Pair<String, ImageVector>> = listOf(
    "category"   to Icons.Outlined.Category,
    "shopping"   to Icons.Outlined.ShoppingCart,
    "restaurant" to Icons.Outlined.Restaurant,
    "car"        to Icons.Outlined.DirectionsCar,
    "bus"        to Icons.Outlined.DirectionsBus,
    "taxi"       to Icons.Outlined.LocalTaxi,
    "gas_station" to Icons.Outlined.LocalGasStation,
    "parking"    to Icons.Outlined.LocalParking,
    "home"       to Icons.Outlined.Home,
    "key"        to Icons.Outlined.Key,
    "work"       to Icons.Outlined.Work,
    "laptop"     to Icons.Outlined.Laptop,
    "school"     to Icons.Outlined.School,
    "health"     to Icons.Outlined.LocalHospital,
    "pharmacy"   to Icons.Outlined.Medication,
    "doctor"     to Icons.Outlined.MedicalServices,
    "percent"    to Icons.Outlined.Percent,
    "gavel"      to Icons.Outlined.Gavel,
    "flight"     to Icons.Outlined.Flight,
    "music"      to Icons.Outlined.MusicNote,
    "movie"      to Icons.Outlined.Movie,
    "gaming"     to Icons.Outlined.SportsEsports,
    "telegram"   to Icons.AutoMirrored.Outlined.Send,
    "dating"     to Icons.Outlined.Favorite,
    "money"      to Icons.Outlined.AttachMoney,
    "coffee"     to Icons.Outlined.LocalCafe,
    "pets"       to Icons.Outlined.Pets,
    "gift"       to Icons.Outlined.CardGiftcard,
    "phone"      to Icons.Outlined.PhoneAndroid,
    "sports"     to Icons.Outlined.DirectionsRun,
    "wifi"       to Icons.Outlined.Wifi,
    "delivery"   to Icons.Outlined.LocalShipping,
    "devices"    to Icons.Outlined.Devices,
    "transfer"   to Icons.Outlined.SwapHoriz,
    "family"     to Icons.Outlined.FamilyRestroom,
    "receipt"    to Icons.Outlined.Receipt,
    "beauty"     to Icons.Outlined.SelfImprovement,
    "ai"         to Icons.Outlined.Psychology,
    "aliexpress" to Icons.Outlined.LocalMall,
    "cloud"      to Icons.Outlined.Cloud,
    "clothes"    to Icons.Outlined.Checkroom,
    "grocery"      to Icons.Outlined.ShoppingBasket,
    "ticket"       to Icons.Outlined.ConfirmationNumber,
    "volunteer"    to Icons.Outlined.HealthAndSafety,
    "theater"      to Icons.Outlined.TheaterComedy,
    "celebration"  to Icons.Outlined.Celebration,
    "spa"          to Icons.Outlined.Spa,
)

internal fun categoryIconFor(iconName: String): ImageVector =
    CATEGORY_ICONS_LIST.firstOrNull { it.first == iconName }?.second ?: Icons.Outlined.Category
