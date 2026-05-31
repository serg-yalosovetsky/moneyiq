# UI Contracts — Category Icon Auto-Suggest

`suggestCategoryStyle(name, type)` in `CategoryStyleUtil.kt` — 55 rules, checked top-to-bottom.

## Icon Rules Table

| Key | Color | Top matching keywords |
|---|---|---|
| `ai` | `#6200EA` deep-purple | ai, chatgpt, openai, claude, gemini, gpt |
| `aliexpress` | `#FF6D00` orange | aliexpress, ali, temu, shein |
| `server` | `#37474F` dark-grey | хостинг, хостінг, hosting, vps, сервер |
| `cloud` | `#0288D1` sky-blue | cloud, хмар, icloud, dropbox |
| `refund` | `#00897B` teal | повернення, refund, cashback, кешбек, компенсація |
| `transfer` | `#00897B` teal | переказ, transfer, відправк |
| `delivery` | `#FF6F00` amber | кур'єр, доставка, нова пошта |
| `devices` | `#607D8B` blue-grey | електрон, техніка, ноутбук, гаджет |
| `wifi` | `#00BCD4` cyan | інтернет, wifi, провайдер |
| `phone` | `#3F51B5` indigo | зв'язок, мобільн, lifecell, kyivstar |
| `beauty` | `#AD1457` dark-pink | краса, салон, манікюр, спа |
| `shoes` | `#5D4037` dark-brown | взуття, shoes, boots |
| `clothes` | `#00838F` dark-cyan | одяг, fashion |
| `toys` | `#FF6D00` orange | іграшки, toys, ляльки, конструктор, lego |
| `family` | `#7A48F2` purple | сім'я, дітям, дитяч |
| `receipt` | `#546E7A` blue-grey | рахунки, bills, платіж, оплат |
| `coffee` | `#795548` brown | кафе, кав'ярня, кава, coffee |
| `restaurant` | `#4659BE` blue | ресторан, ресторація, їдальня, food, pizza |
| `grocery` | `#4AAFE8` light-blue | продукти, атб, сільпо, фора |
| `flower` | `#E91E63` pink | квіти, цвіти, flower, флорист, букет |
| `souvenir` | `#7B1FA2` purple | сувенір, souvenir |
| `celebration` | `#FF6D00` orange | розваг, свят, party, вечірк, банкет |
| `theater` | `#F73579` pink | дозвілл, театр, концерт, шоу, entertainment |
| `movie` | `#9C27B0` purple | кіно, cinema, фільм, netflix |
| `book` | `#5E35B1` deep-purple | книги, книга, book, бібліотек |
| `gaming` | `#607D8B` blue-grey | gaming, ігри, playstation, xbox, steam |
| `telegram` | `#2196F3` blue | telegram, телеграм, viber, messenger |
| `dating` | `#E91E63` pink | dating, tinder, bumble, знайомств |
| `ticket` | `#AD1457` dark-pink | квиток, квитки |
| `music` | `#AB47BC` purple | музик, spotify |
| `store` | `#1E88E5` blue | rozetka, ebay, маркетплейс, prom.ua, hotline |
| `fitness` | `#D32F2F` dark-red | спортивні товари, спорттовар, decathlon |
| `shopping` | `#7B5947` brown | покупки, магазин, shopping |
| `taxi` | `#FDD835` yellow | таксі, taxi, uklon, bolt, uber |
| `gas_station` | `#FF8F00` amber | азс, азц, заправк, wog, okko, socar |
| `train` | `#1565C0` dark-blue | залізниця, потяг, поїзд, train, укрзалізниц |
| `bus` | `#FFA834` orange | **транспорт**, громадськ, автобус, метро, маршрутк |
| `auto_parts` | `#E64A19` deep-orange | запчастин, автозапч, шиномонтаж, ремонт авто |
| `car` | `#FF7043` deep-orange | авто, машин, автомоб, паркінг, бензин, пальне |
| `tools` | `#546E7A` blue-grey | інструмент, дриль, пилк, шуруповерт |
| `hardware` | `#BF360C` deep-orange | будматеріал, будівельн, цегла, ламінат |
| `home` | `#546E7A` blue-grey | комунальн, квартир, оренда, ремонт |
| `work` | `#1565C0` dark-blue | зарплат, офіс, фриланс, дохід |
| `school` | `#FF9800` orange | освіт, навчан, школа, курс |
| `volunteer` | `#48B456` green | здоров, самопочутт |
| `pharmacy` | `#43A047` dark-green | аптека, ліки, medication, таблетк |
| `dental` | `#0097A7` teal | стоматолог, дантист, dental, зубн |
| `doctor` | `#D81B60` pink-red | медицин, лікар, клінік, hospital |
| `hotel` | `#4527A0` deep-purple | готель, hotel, hostel, airbnb |
| `flight` | `#03A9F4` light-blue | відпочин, туризм, перельот, travel, booking |
| `money` | `#F9A825` amber-dark | **фінанс**, інвестиц, банк, крипто, депозит |
| `pets` | `#8D6E63` brown-light | тварин, кіт, собак, ветеринар |
| `gift` | `#F34B4D` red | подарун, birthday |
| `sports` | `#F44336` red | спорт, фітнес, gym, тренув |
| `gavel` | `#BF360C` deep-orange | штраф, пеня, санкц, fine, penalty |
| `percent` | `#F9A825` amber | процент, відсоток, податок, пдв, interest, tax |

**Fallback:** unrecognised name → `category` key, color `#4CAF50` for INCOME or `#78909C` for EXPENSE.

## Critical Rule Orderings (do not reorder)

- `server` before `cloud` — хостинг is more specific
- `wifi` before `phone` — both match "інтернет"-related terms
- `shoes` before `clothes` — взуття is shoes (specific)
- `toys` before `family` — іграшки more specific
- `coffee` before `restaurant` — кафе more specific
- `grocery` before `shopping` — продукти more specific
- `flower`/`souvenir` before `celebration`/`gift` — distinct purchase types
- `receipt` before `home` — "рахунки/оплат" more specific
- `celebration` before `theater` — "розваг" and "свят" go to celebration
- `book` before `movie` — book more specific
- `volunteer` → `pharmacy` → `dental` → `doctor` — wellness specificity chain
- `store` → `fitness` → `shopping` — marketplace → sport goods → generic
- `train` before `bus` — залізниця more specific
- `auto_parts` before `car` — repair/parts more specific
- `tools` → `hardware` → `home` — power tools → building materials → generic
- `hotel` before `flight` — accommodation more specific
- `theater` → `movie` → `gaming`/`telegram`/`dating` → `ticket` — leisure chain
- `taxi` → `gas_station` → `train` → `bus` → `auto_parts` → `car` — transport chain
- `gavel` and `percent` after `sports` — narrow keyword sets

## Auto-Suggest Trigger

When creating a new category (`existing == null`), `CategoryFormSheet` runs a `LaunchedEffect(name)` that calls `suggestCategoryStyle(name, type)` if name ≥ 3 chars AND user hasn't manually picked an icon (`iconKey == "category"`). Auto-suggest stops once the user touches the icon picker.

`CATEGORY_ICONS_LIST` in `CategoryIcons.kt` is the canonical set of valid icon keys.

**Rule:** When adding a new icon key: add to `CATEGORY_ICONS_LIST`, to the `validKeys` set in `repairIconKeys()`, and to `iconColorMap` in `CategoryStyleUtil.kt`. All three must stay in sync.
