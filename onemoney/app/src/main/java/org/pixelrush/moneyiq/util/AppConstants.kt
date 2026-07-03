package org.syalosovetskyi.onemoney.util

/** Зовнішні HTTP-ендпоінти. */
object ApiEndpoints {
    /** Відкритий API курсів валют НБУ (без ключа). */
    const val NBU_RATES = "https://bank.gov.ua/NBUStatService/v1/statdirectory/exchange?json"
}

/**
 * Таймаути мережевих запитів (мс). CONNECT спільний; READ різний за призначенням:
 * короткий — дрібний JSON (курси), довгий — повний бекап/синхронізація.
 */
object NetworkTimeouts {
    const val CONNECT_MS   = 15_000
    const val READ_SHORT_MS = 30_000
    const val READ_LONG_MS  = 60_000
}
