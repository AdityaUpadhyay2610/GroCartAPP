
<h1 align="center">🛒 GroCart — Fresh Groceries in 10 Minutes</h1>

<p align="center">
  <b>A modern, full-featured grocery shopping Android app built with Jetpack Compose, Firebase Auth, and Firebase Realtime Database.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-green?style=for-the-badge&logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue?style=for-the-badge&logo=jetpackcompose" />
  <img src="https://img.shields.io/badge/Backend-Firebase-orange?style=for-the-badge&logo=firebase" />
  <img src="https://img.shields.io/badge/API%20SDK-36-brightgreen?style=for-the-badge" />
</p>

---
## ✨ Features

### 🔐 Authentication
- **Firebase Email/Password Auth** — Secure registration & login using Firebase Authentication SDK
- **Guest Mode** — Browse products without an account (restricted from orders/profile)
- **Password Visibility Toggle** — Eye icon for show/hide password in login form
- **Auth Error Feedback** — Real-time error toasts for invalid credentials

### 🏠 Home Screen
- **Promotional Banner** — Animated "Mega Sale" hero card with gradient background
- **Recommended Items** — Horizontally scrollable carousel of shuffled product recommendations
- **Shop By Category** — Compact category grid with pastel-colored icon cards
- **GPS Location Header** — Real-time geocoded address displayed in the top header

### 🔍 Search
- **Predictive Search** — Live filtering as-you-type across all product names and categories
- **Search-to-Category Navigation** — Clicking a search result navigates directly to its category page

### 📂 Categories
- **13 Product Categories** — Fresh Fruits, Bread & Biscuits, Sweet Tooth, Bath & Body, Beverages, Kitchen Essentials, Munchies, Packed Food, Fresh Vegetables, Cleaning Essentials, Stationery, Pet Food, and more
- **Blinkit-Style Category Cards** — Distinct color-coded cards with "Shop →" accent chips
- **Gradient Header** — Purple-to-teal gradient with category title overlay

### 🛒 Cart System
- **Real-time Firebase Sync** — Cart items are stored per-user in Firebase Realtime Database
- **Quantity Controls** — Increment/decrement with animated quantity selector
- **Detailed Bill Breakdown** — Line items, item totals, handling charges (1%), delivery fee (₹30), and grand total
- **25% Discount Logic** — All items displayed at 75% of original price (hardcoded discount)

### 💳 Checkout
- **Simulated Payment** — Countdown-based payment screen with animated transitions
- **Order Placement** — Places order to Firebase and clears cart atomically
- **Payment Cancellation** — Users can cancel during the countdown

### 📦 Order History
- **Order Cards** — Each order displayed with timestamp, order number, item images, and total
- **PDF Invoice Generation** — Professional receipt-style PDF invoices saved to Downloads
- **Auto-Open PDF** — Automatically launches PDF viewer after download

### 👤 Profile
- **Editable Profile** — Full name, email, phone, and delivery address fields
- **Gradient Avatar Header** — Purple-to-teal gradient with circular avatar
- **Save Profile** — Toast confirmation on successful profile update

### 🎨 UI / UX
- **Custom Curved Bottom Navigation** — Animated floating circle indicator with cutout shape
- **Material 3 Design System** — Modern color scheme with violet primary and teal secondary
- **Light & Dark Theme Support** — Automatic theme switching based on system preference
- **Micro-Animations** — Flying item animation on add-to-cart, spring-animated navigation cutout
- **Coil Image Loading** — Async remote image loading for product images

---

## 🏗️ Architecture & Tech Stack

```
┌──────────────────────────────────────────────────────────┐
│                    PRESENTATION LAYER                     │
│  Jetpack Compose UI • Material 3 • Navigation-Compose    │
├──────────────────────────────────────────────────────────┤
│                    VIEWMODEL LAYER                        │
│  GroViewModel • StateFlow • Kotlin Coroutines            │
├──────────────────────────────────────────────────────────┤
│                    DATA LAYER                             │
│  Retrofit (REST) • Kotlinx Serialization • DataStore     │
├──────────────────────────────────────────────────────────┤
│                    BACKEND (BaaS)                         │
│  Firebase Auth • Firebase Realtime Database               │
└──────────────────────────────────────────────────────────┘
```

| Category | Technology |
|---|---|
| **Language** | Kotlin |
| **UI Framework** | Jetpack Compose (Material 3) |
| **Architecture** | MVVM (Model-View-ViewModel) |
| **State Management** | `StateFlow`, `MutableStateFlow`, `mutableStateOf` |
| **Navigation** | Navigation-Compose |
| **Authentication** | Firebase Authentication (Email/Password) |
| **Database** | Firebase Realtime Database (REST API via Retrofit) |
| **Networking** | Retrofit 2 + Kotlinx Serialization Converter |
| **Image Loading** | Coil Compose |
| **Local Storage** | SharedPreferences (`SessionManager`) + DataStore |
| **PDF Generation** | Android `PdfDocument` API |
| **Async Operations** | Kotlin Coroutines (`viewModelScope`) |
| **Min SDK** | 28 (Android 9.0 Pie) |
| **Target SDK** | 35 |
| **Compile SDK** | 36 |

---

## 📂 Project Structure

```
grocart_frontend/
├── app/
│   ├── src/main/
│   │   ├── java/com/grocart/first/
│   │   │   ├── MainActivity.kt              # Single Activity entry point
│   │   │   ├── data/
│   │   │   │   ├── Categories.kt            # Category data class (StringRes + DrawableRes)
│   │   │   │   ├── DataSource.kt            # Static category list with 13 categories
│   │   │   │   ├── InternetItem.kt          # Product model + CartItemResponse + Firestore models
│   │   │   │   ├── InternetItemWithQuantity.kt # Wrapper for grouped order display
│   │   │   │   ├── Item.kt                  # Base item model
│   │   │   │   ├── Order.kt                 # Order model (items + timestamp)
│   │   │   │   ├── OrderItemWithQuantity.kt # Order item grouping model
│   │   │   │   ├── SessionManager.kt        # SharedPreferences-based session handling
│   │   │   │   └── UserResponse.kt          # User model (id, username, email)
│   │   │   ├── network/
│   │   │   │   ├── FirstApiService.kt       # Retrofit API interface for Firebase REST
│   │   │   │   └── UserSignupRequest.kt     # Signup request DTO
│   │   │   ├── ui/
│   │   │   │   ├── FirstApp.kt              # Main composable + Navigation graph + Bottom bar
│   │   │   │   ├── GroViewModel.kt          # Central ViewModel (auth, cart, orders, items)
│   │   │   │   ├── GroUiState.kt            # UI state data class
│   │   │   │   ├── StartScreen.kt           # Home screen with banner, recommendations, categories
│   │   │   │   ├── ItemScreen.kt            # Product grid with category filter
│   │   │   │   ├── CartScreen.kt            # Cart with bill details + payment flow
│   │   │   │   ├── CategoryScreen.kt        # Blinkit-style category grid
│   │   │   │   ├── LoginUi.kt              # Login/Signup UI with Firebase Auth
│   │   │   │   ├── MyOrdersScreen.kt        # Order history with PDF invoices
│   │   │   │   ├── ProfileScreen.kt         # Editable user profile
│   │   │   │   └── theme/
│   │   │   │       ├── Color.kt             # Color palette (Modern, Aesthetic, Dark)
│   │   │   │       ├── Shape.kt             # Shape definitions
│   │   │   │       ├── Theme.kt             # Material 3 theme configuration
│   │   │   │       └── Type.kt              # Typography definitions
│   │   │   └── utils/
│   │   │       └── PdfGenerator.kt          # Receipt-style PDF invoice generator
│   │   ├── res/
│   │   │   ├── drawable/                    # 33 image assets (categories, products, UI states)
│   │   │   ├── values/
│   │   │   │   ├── strings.xml              # Category names & app strings
│   │   │   │   ├── colors.xml               # XML color definitions
│   │   │   │   └── themes.xml               # XML theme overrides
│   │   │   └── xml/                         # Backup rules & file provider paths
│   │   └── AndroidManifest.xml              # Permissions: INTERNET, Location, Storage
│   ├── build.gradle.kts                     # Module-level dependencies
│   └── google-services.json                 # ⚠️ EXCLUDED via .gitignore
├── build.gradle.kts                         # Project-level Gradle config
├── settings.gradle.kts                      # Module includes
├── gradle.properties                        # JVM args & Android flags
├── screenshots/                             # App UI screenshots for README
└── .gitignore                               # Security rules (credentials excluded)
```

---

## 🐛 Issues Faced & Solutions

### Issue 1 — Firebase Data Format Mismatch
> **Problem:** Firebase Realtime Database returns items as either a JSON **Object** (map) or **Array** depending on how data is indexed. The app would crash when the format didn't match the expected deserialization type.
>
> **Solution:** Used `JsonElement` as the raw response type in Retrofit and wrote a custom `parseItems()` function that handles both `JsonObject` (extracting values) and `JsonArray` (filtering nulls) dynamically.

```kotlin
private fun parseItems(element: JsonElement?): List<JsonElement> {
    if (element == null || element is JsonNull) return emptyList()
    return when (element) {
        is JsonObject -> element.values.toList()
        is JsonArray -> element.filter { it !is JsonNull }
        else -> emptyList()
    }
}
```

---

### Issue 2 — Cart Items Overwriting Instead of Aggregating
> **Problem:** When the same item was added to the cart multiple times, it would create separate entries instead of incrementing the quantity. This resulted in duplicate items in the cart view.
>
> **Solution:** Before adding to cart, the code now checks for an existing item with the same `id`. If found, it increments the quantity; otherwise, it creates a new entry. The cart is stored in Firebase using the item's `id` as the key (`PUT carts/{userId}/{itemId}`), which naturally ensures upsert behavior.

```kotlin
fun addToCart(item: InternetItem) {
    val existing = _cartItems.value.find { it.id == item.id }
    val cartItem = CartItemResponse(
        id = item.id,
        quantity = (existing?.quantity ?: 0) + 1
        // ...
    )
    FirstApi.retrofitService.addCartItem(userId, item.id, cartItem, token)
}
```

---

### Issue 3 — Migrating from Mock Auth to Firebase Authentication
> **Problem:** The original app used a hardcoded mock login system. Migrating to Firebase Authentication required significant refactoring of the auth flow, token management, and per-user data isolation.
>
> **Solution:**
> - Replaced mock login with `FirebaseAuth.signInWithEmailAndPassword()` and `createUserWithEmailAndPassword()`
> - Added ID Token retrieval (`getIdToken()`) for authenticated API calls to Firebase REST endpoints
> - Implemented `UserProfileChangeRequest` to set display names on registration
> - All user data (cart, orders) is now scoped under the Firebase UID

---

### Issue 4 — Securing Firebase Configuration Files
> **Problem:** The `google-services.json` file contains Firebase API keys and project identifiers. Accidentally committing these to a public repository is a **serious security risk**.
>
> **Solution:**
> - Added `google-services.json`, `serviceAccountKey.json`, `*.keystore`, `*.jks`, and `.env` to `.gitignore`
> - Created a template file (`google-services.json.template`) for collaborators to fill in their own credentials
> - **Verified:** The `.gitignore` pattern `google-services.json` (without path prefix) matches the file at any depth — including `app/google-services.json` ✅

---

### Issue 5 — Remote Images Not Loading (Coil + Cleartext Traffic)
> **Problem:** Product images from Firebase were not rendering. The app silently failed to load HTTP URLs due to Android's cleartext traffic policy (HTTPS required by default).
>
> **Solution:**
> - Added `android:usesCleartextTraffic="true"` to `AndroidManifest.xml` to allow HTTP image URLs
> - Switched to **Coil's `AsyncImage`** composable for efficient, coroutine-based image loading with built-in caching

---

### Issue 6 — Bottom Navigation Bar Design (Curved Cutout)
> **Problem:** Standard Material 3 `NavigationBar` doesn't support curved cutout designs. The goal was to create a premium Blinkit-style floating indicator.
>
> **Solution:** Created a fully custom `CurvedBottomBarShape` using Bézier curves:
> - Custom `Shape` implementation with `cubicTo()` and `quadraticBezierTo()` for the smooth cutout
> - Spring-animated `cutPosition` that smoothly transitions the cutout position between tabs
> - Floating white circle overlay that positions itself using `IntOffset` above the cutout
> - Badge support for cart item count indicator

---

### Issue 7 — Pricing Inconsistency Across Screens
> **Problem:** The discounted price (75% of MRP) was calculated differently across the item screen, cart screen, and order history — leading to incorrect totals.
>
> **Solution:** Standardized the discount formula `itemPrice * 75 / 100` across all screens (StartScreen, ItemScreen, CartScreen, MyOrdersScreen) and ensured the bill breakdown in CartScreen and PdfGenerator use the same calculation.

---

### Issue 8 — PDF Invoice Not Opening After Download
> **Problem:** The generated PDF was saved to Downloads but the user had no way to view it immediately.
>
> **Solution:**
> - After saving, the app creates an `ACTION_VIEW` Intent with the PDF URI
> - For API 29+, uses `MediaStore.Downloads` with `ContentResolver`
> - For older APIs, uses `FileProvider` to generate a shareable URI
> - Added `<provider>` in AndroidManifest with `file_provider_paths` configuration

---

### Issue 9 — UI Color Palette Overhaul
> **Problem:** The original UI used pinkish/lavender colors that looked dated and inconsistent across screens.
>
> **Solution:** Implemented a complete color system overhaul:
> - **Primary:** Violet 600 (`#7C3AED`) — used for buttons, accents, and profile header
> - **Secondary:** Teal 600 (`#0D9488`) — used for category header gradient
> - **Background:** Clean white-to-slate gradient (`#FFFFFF` → `#F1F5F9`)
> - **Navigation:** Green (`#43A047`) for the bottom bar
> - Applied consistently across all 6+ screens with dark theme support

---

## 🔒 Security — `.gitignore` Audit

The following files are **excluded from version control** to protect sensitive credentials:

| File / Pattern | Status | Purpose |
|---|---|---|
| `google-services.json` | ✅ Ignored | Firebase API keys & project config |
| `serviceAccountKey.json` | ✅ Ignored | Firebase Admin SDK credentials |
| `*.keystore` / `*.jks` | ✅ Ignored | APK signing keys |
| `.env` | ✅ Ignored | Environment variables |
| `local.properties` | ✅ Ignored | SDK path (machine-specific) |
| `/build` | ✅ Ignored | Build outputs |
| `/app/build` | ✅ Ignored | Module build outputs |
| `/app/release` | ✅ Ignored | Release APK artifacts |
| `.gradle` / `.kotlin` | ✅ Ignored | Build cache directories |
| `*.apk` / `*.aab` | ✅ Ignored | Compiled app bundles |

> [!IMPORTANT]
> **For collaborators:** After cloning, you must create your own `google-services.json` by setting up a Firebase project with package name `com.example.first` and placing the file in `app/`.

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio** Ladybug | 2024.2.1 or newer
- **JDK 11+**
- A **Firebase** project with Authentication and Realtime Database enabled

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/GroCart.git
   cd GroCart
   ```

2. **Firebase Setup**
   - Create a project at [Firebase Console](https://console.firebase.google.com/)
   - Add an Android app with package name: `com.example.first`
   - Download `google-services.json` and place it in `app/`
   - Enable **Email/Password** sign-in under Authentication → Sign-in method
   - Create a Realtime Database and add your product items under `items/`

3. **Firebase Realtime Database Rules** (recommended)
   ```json
   {
     "rules": {
       "items": { ".read": true, ".write": false },
       "carts": {
         "$uid": { ".read": "$uid === auth.uid", ".write": "$uid === auth.uid" }
       },
       "orders": {
         "$uid": { ".read": "$uid === auth.uid", ".write": "$uid === auth.uid" }
       }
     }
   }
   ```

4. **Open in Android Studio**
   - Open the project and let Gradle sync
   - Run the `app` configuration on an emulator or device (min API 28)

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  <b>⭐ If you found this project helpful, consider giving it a star!</b>
</p>

<p align="center">
  Made with ❤️ using Kotlin & Jetpack Compose
</p>
