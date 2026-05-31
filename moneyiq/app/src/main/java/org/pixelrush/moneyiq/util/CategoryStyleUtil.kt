package org.pixelrush.moneyiq.util

import org.pixelrush.moneyiq.data.db.entities.TransactionType

/**
 * Returns (iconKey, colorHex) for a category by matching name keywords.
 * Icon keys correspond to CATEGORY_ICONS_LIST in CategorySheets.kt.
 * Colors are from CATEGORY_FORM_COLORS.
 */
fun suggestCategoryStyle(name: String, type: TransactionType): Pair<String, String> {
    val n = name.lowercase().trim()

    // Rules checked top-to-bottom; more specific rules must come first.
    val rules: List<Pair<String, List<String>>> = listOf(
        // AI services
        "ai"         to listOf("ai ", " ai", "chatgpt", "openai", "claude", "gemini",
                               "midjourney", "copilot", "gpt", "штучн", "нейромереж",
                               "artificial intelligence", "llm"),

        // Aliexpress / Asian marketplaces
        "aliexpress" to listOf("aliexpress", "ali ", "алиекспресс", "аліекспрес",
                               "temu", "shein", "wish", "banggood", "gearbest"),

        // Cloud storage / subscriptions
        "cloud"      to listOf("cloud", "хмар", "icloud", "google drive", "dropbox",
                               "onedrive", "mega ", "backblaze", "хостинг", "vps", "сервер"),

        // Transfers & money moves
        "transfer"   to listOf("переказ", "перевод", "перевід", "transfer", "відправк", "розрахун"),

        // Courier / delivery
        "delivery"   to listOf("кур'єр", "курьер", "доставк", "delivery", "courier",
                               "посилк", "нова пошта", "укрпошта", "meest", "justin"),

        // Electronics / gadgets
        "devices"    to listOf("електрон", "электрон", "техніка", "техника", "гаджет",
                               "devices", "laptop", "ноутбук", "телевізор", "телевизор",
                               "пристрій", "hardware", "software"),

        // Internet (before phone to avoid overlap on "інтернет")
        "wifi"       to listOf("інтернет", "інтернету", "internet", "wifi", "wi-fi",
                               "broadband", "оптик", "провайдер"),

        // Phone / mobile
        "phone"      to listOf("зв'язок", "зв'язку", "связь", "мобільн",
                               "phone", "mobile", "sim", "телефон", "поповнення",
                               "lifecell", "kyivstar", "vodafone", "укртелеком"),

        // Beauty / spa
        "beauty"     to listOf("краса", "beauty", "салон", "перукарн", "косметик",
                               "манікюр", "педикюр", "спа", "spa", "spa-"),

        // Clothing (before shopping to avoid overlap)
        "clothes"    to listOf("одяг", "взуття", "fashion", "одежд"),

        // Family
        "family"     to listOf("сім'я", "сімей", "family", "дітям", "дитяч",
                               "дитин", "дитяча"),

        // Bills / utilities (before home to be more specific)
        "receipt"    to listOf("рахунки", "рахунок", "bills", "utilities",
                               "счет", "оплат", "платіж"),

        // Cafe (more specific than restaurant — must be before it)
        "coffee"     to listOf("кафе", "кав'ярн", "кофе", "кава", "coffee", "cafe",
                               "tea", "чай", "снэк", "снек", "snack"),

        // Restaurants (includes "ресторація" — One Money's broad category name)
        "restaurant" to listOf("ресторан", "ресторацій", "ресторація", "їдальн",
                               "обід", "ужин", "завтрак", "food", "піца", "pizza",
                               "суші", "sushi", "питан", "харч", "кухн", "restaurant"),

        // Groceries (before general shopping)
        "grocery"    to listOf("продукти", "продукт", "продовольч",
                               "атб", "сільпо", "фора", "новус"),

        // Parties / celebrations (more specific than theater — must come before it)
        "celebration" to listOf("розваг", "свят", "party", "праздн", "вечірк", "банкет"),

        // Broad leisure/entertainment (дозвілля, театр)
        "theater"    to listOf("дозвілл", "театр", "концерт", "шоу",
                               "entertainment", "festival", "відпочин"),

        // Cinema / movies specifically
        "movie"      to listOf("кіно", "cinema", "фільм", "кінотеатр", "netflix"),

        // Video games
        "gaming"     to listOf("gaming", "гейм", "ігри", "відеоігр",
                               "playstation", "xbox", "nintendo", "steam"),

        // Messaging apps
        "telegram"   to listOf("telegram", "телеграм", "viber", "вайбер", "messenger"),

        // Dating apps
        "dating"     to listOf("dating", "тіндер", "tinder", "bumble", "hinge", "знайомств"),

        // Event tickets
        "ticket"     to listOf("квиток", "квитки", "concert ticket"),

        // Music specifically
        "music"      to listOf("музик", "music", "spotify"),

        // Shopping (general)
        "shopping"   to listOf("покупки", "магазин", "market", "shopping", "ринок"),

        // Taxi (before car — more specific)
        "taxi"        to listOf("таксі", "taxi", "uklon", "bolt", "uber"),

        // Gas station (before car — more specific)
        "gas_station" to listOf("азс", "азц", "заправк", "wog", "okko", "socar", "brsm", "нафтан"),

        // Transport (general — bus/metro/public)
        "bus"        to listOf("транспорт", "автобус", "метро", "маршрутк",
                               "bus", "transit", "електричк", "трамвай", "тролейбус"),

        // Car / auto (personal vehicle)
        "car"        to listOf("авто", "машин", "автомоб", "car",
                               "паркінг", "бензин", "пальне"),

        // Home / utilities
        "home"       to listOf("комунальн", "комунал", "квартир", "аренд", "оренд",
                               "home", "rent", "house", "ремонт", "будинок",
                               "електроенергія", "газ ", "вода ", "опален"),

        // Work / income
        "work"       to listOf("зарплат", "заробіт", "офіс", "бізнес", "фриланс",
                               "work", "salary", "доход від", "дохід"),

        // Education
        "school"     to listOf("освіт", "навчан", "школ", "курс", "study",
                               "education", "університет", "репетитор"),

        // Wellbeing (includes "здоров'я" — One Money's broad category name)
        "volunteer"  to listOf("здоров", "самопочутт"),

        // Pharmacy / drugs — more specific, must come before doctor
        "pharmacy"   to listOf("аптек", "ліки", "ліків", "pharmacy", "medication",
                               "таблетк", "пігулк", "препарат"),

        // Medical / hospital / clinic
        "doctor"     to listOf("медицин", "лікар", "клінік", "стоматолог",
                               "health", "doctor", "лікарн", "hospital"),

        // Travel / flights
        "flight"     to listOf("відпочин", "туризм", "відпустк", "перельот",
                               "flight", "travel", "готель", "hotel", "booking"),

        // Finance / savings
        "money"      to listOf("фінанс", "інвестиц", "інвестицій", "банк", "крипто",
                               "invest", "crypto", "накопич", "депозит", "вклад"),

        // Pets
        "pets"       to listOf("тварин", "домашн", "кіт", "собак", "pet", "ветеринар"),

        // Gifts
        "gift"       to listOf("подарун", "подарок", "gift", "present",
                               "свят", "день народ", "birthday"),

        // Sports
        "sports"     to listOf("спорт", "фітнес", "спортзал", "gym", "тренув", "йога", "yoga"),

        // Fines / penalties
        "gavel"      to listOf("штраф", "пеня", "санкц", "fine", "penalty", "стягнен"),

        // Interest / taxes / percentages
        "percent"    to listOf("процент", "відсоток", "податок", "податки", "пдв", "акциз",
                               "interest", "tax", "taxes", "ндфл")
    )

    val iconColorMap = mapOf(
        "ai"         to "#6200EA",  // deep-purple   — AI сервіси
        "aliexpress" to "#FF6D00",  // orange        — Aliexpress
        "cloud"      to "#0288D1",  // sky-blue      — Cloud/хостинг
        "transfer"   to "#00897B",  // teal          — Переказ
        "delivery"   to "#FF6F00",  // amber         — Кур'єр
        "devices"    to "#607D8B",  // blue-grey     — Електроніка
        "wifi"       to "#00BCD4",  // cyan          — Інтернет
        "phone"      to "#3F51B5",  // indigo        — Зв'язок
        "coffee"     to "#795548",  // brown         — Кафе
        "restaurant" to "#4659BE",  // blue          — Ресторація
        "shopping"   to "#7B5947",  // brown         — Покупки
        "taxi"        to "#FDD835",  // yellow        — Таксі
        "gas_station" to "#FF8F00",  // amber         — АЗС
        "theater"    to "#F73579",  // pink          — Дозвілля
        "celebration" to "#FF6D00", // orange        — Свято
        "spa"        to "#26A69A",  // teal-green    — Спа/Велнес
        "bus"        to "#FFA834",  // orange        — Транспорт (публічний)
        "car"        to "#FF7043",  // deep-orange   — Авто (особисте)
        "home"       to "#546E7A",  // blue-grey     — Комунальні
        "work"       to "#1565C0",  // dark-blue     — Зарплата/Робота
        "school"     to "#FF9800",  // orange        — Освіта
        "volunteer"  to "#48B456",  // green         — Здоров'я (серце)
        "pharmacy"   to "#43A047",  // green-dark    — Аптека (таблетки)
        "doctor"     to "#D81B60",  // pink-red      — Лікар/Медицина
        "health"     to "#C62828",  // red           — (legacy key)
        "parking"    to "#78909C",  // blue-grey     — Паркінг
        "key"        to "#9C27B0",  // purple        — Оренда
        "laptop"     to "#26A69A",  // teal-green    — Фриланс
        "percent"    to "#F9A825",  // amber         — Проценти/Податки
        "gavel"      to "#BF360C",  // deep-orange   — Штрафи
        "flight"     to "#03A9F4",  // light-blue    — Подорожі
        "movie"      to "#9C27B0",  // purple        — Кіно
        "gaming"     to "#607D8B",  // blue-grey     — Gaming
        "telegram"   to "#2196F3",  // blue          — Telegram
        "dating"     to "#E91E63",  // pink          — Dating
        "ticket"     to "#AD1457",  // dark-pink     — Концерт/театр
        "music"      to "#AB47BC",  // purple        — Музика
        "grocery"    to "#4AAFE8",  // light-blue    — Продукти
        "money"      to "#F9A825",  // amber-dark    — Інвестиції
        "pets"       to "#8D6E63",  // brown-light   — Тварини
        "gift"       to "#F34B4D",  // red           — Подарунки
        "sports"     to "#F44336",  // red           — Спорт
        "beauty"     to "#AD1457",  // dark-pink     — Краса
        "clothes"    to "#00838F",  // dark-cyan     — Одяг
        "family"     to "#7A48F2",  // purple        — Сім'я
        "receipt"    to "#546E7A"   // blue-grey     — Рахунки
    )

    for ((icon, keywords) in rules) {
        if (keywords.any { n.contains(it) }) {
            return icon to (iconColorMap[icon] ?: "#FF5722")
        }
    }

    // Unrecognised / "Інше" — neutral grey
    val fallbackColor = if (type == TransactionType.INCOME) "#4CAF50" else "#78909C"
    return "category" to fallbackColor
}
