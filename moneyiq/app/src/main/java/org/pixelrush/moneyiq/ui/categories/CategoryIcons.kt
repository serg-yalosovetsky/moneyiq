package org.pixelrush.moneyiq.ui.categories

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

internal val CATEGORY_FORM_COLORS = listOf(
    "#4AAFE8", "#4659BE", "#F73579", "#FFA834",
    "#48B456", "#F34B4D", "#7A48F2", "#7B5947",
    "#FF5722", "#E91E63", "#9C27B0", "#3F51B5",
    "#009688", "#FFEB3B", "#6200EA", "#607D8B"
)

internal val CATEGORY_ICONS_LIST: List<Pair<String, ImageVector>> = listOf(
    "category"   to Icons.Outlined.Category,
    "shopping"   to Icons.Outlined.ShoppingCart,
    "restaurant" to Icons.Outlined.Restaurant,
    "car"        to Icons.Outlined.DirectionsCar,
    "taxi"       to Icons.Outlined.LocalTaxi,
    "gas_station" to Icons.Outlined.LocalGasStation,
    "home"       to Icons.Outlined.Home,
    "work"       to Icons.Outlined.Work,
    "school"     to Icons.Outlined.School,
    "health"     to Icons.Outlined.LocalHospital,
    "flight"     to Icons.Outlined.Flight,
    "music"      to Icons.Outlined.MusicNote,
    "movie"      to Icons.Outlined.Movie,
    "gaming"     to Icons.Outlined.SportsEsports,
    "telegram"   to Icons.Outlined.Send,
    "dating"     to Icons.Outlined.Favorite,
    "money"      to Icons.Outlined.AttachMoney,
    "coffee"     to Icons.Outlined.LocalCafe,
    "pets"       to Icons.Outlined.Pets,
    "gift"       to Icons.Outlined.CardGiftcard,
    "phone"      to Icons.Outlined.PhoneAndroid,
    "sports"     to Icons.Outlined.FitnessCenter,
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
    "volunteer"    to Icons.Outlined.VolunteerActivism,
    "theater"      to Icons.Outlined.TheaterComedy,
    "celebration"  to Icons.Outlined.Celebration,
    "spa"          to Icons.Outlined.Spa,
)

internal fun categoryIconFor(iconName: String): ImageVector =
    CATEGORY_ICONS_LIST.firstOrNull { it.first == iconName }?.second ?: Icons.Outlined.Category
