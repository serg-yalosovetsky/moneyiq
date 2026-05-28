# Tech Stack

## Platform

- Android native app.
- Minimum SDK: 26.
- Target SDK: 36.
- Compile SDK: 36.
- Java/Kotlin JVM target: 17.

## Language And Build

- Kotlin `2.1.0`.
- Android Gradle Plugin `8.7.3`.
- KSP `2.1.0-1.0.29`.
- Gradle version catalog: `moneyiq/gradle/libs.versions.toml`.

## UI

- Jetpack Compose with Material 3.
- Material icons extended are used heavily.
- Navigation Compose powers app navigation.
- Glance powers app widgets.

## Data And DI

- Room `2.7.0` for local persistence.
- Hilt `2.54` for dependency injection.
- DataStore Preferences for settings.
- Kotlin coroutines and Flow for reactive reads.

## Background Work And Extras

- WorkManager for background work.
- MPAndroidChart is available for charts.
- AndroidX Biometric dependency is present.

## Conventions

- Prefer Compose UI changes in the owning screen package.
- Keep repository methods as the only place that mutates cross-entity state such as account balances.
- Add Room migrations for schema changes; do not silently rely on destructive migration for user data.
- Use existing icon keys and `CategoryStyleUtil` before inventing new category styling logic.
