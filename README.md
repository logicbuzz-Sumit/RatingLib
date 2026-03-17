# IconRatingBar

A customizable Android RatingBar with swipe gesture, half-star support, and fully customizable icons.

## Features

* Swipe to change rating
* Tap to set rating
* Half-star support
* Custom star icons
* Adjustable star size and spacing
* Indicator mode (read-only rating)
* Lightweight and easy to integrate

---

# Installation

Add JitPack repository in your **settings.gradle** or **settings.gradle.kts**

```gradle
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

Then add the dependency in your **app module build.gradle**

```gradle
dependencies {
    implementation 'com.github.logicbuzz-Sumit:RatingLib:1.0.7'
}
```



---

# Usage

## XML Layout

```xml
<com.sumit.iconratingbar.IconRatingBar
    android:id="@+id/ratingBar"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    app:starSize="40dp"
    app:starSpacing="6dp"
    app:emptyStar="@drawable/ic_star_empty"
    app:filledStar="@drawable/ic_star_filled"
    app:halfStar="@drawable/ic_star_half"/>
```

---

## Kotlin Example

```kotlin
val ratingBar = findViewById<IconRatingBar>(R.id.ratingBar)

ratingBar.onRatingChanged = { rating ->
    Log.d("Rating", "Selected rating: $rating")
}
```

---

# Customization

You can also configure stars programmatically.

```kotlin
ratingBar.numStars = 5
ratingBar.stepSize = 0.5f
ratingBar.rating = 3.5f
ratingBar.allowZeroRating = true
```

---

# Indicator Mode (Read-only)

```kotlin
ratingBar.isIndicatorOnly = true
ratingBar.rating = 4.0f
```

---

# License

MIT License
