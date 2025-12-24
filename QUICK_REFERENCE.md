# Quick Reference: Multilingual & User Settings Implementation

## File Locations

### String Resources
```
📁 app/src/main/res/
├── values/strings.xml              # English (Default)
├── values-fr/strings.xml           # French
└── values-ar/strings.xml           # Arabic
```

### Database Files
```
No local database files - uses Firebase Firestore directly
```

### Updated UI Files
```
📁 app/src/main/java/com/example/soukify/ui/settings/
└── LanguageCurrencyFragment.java   # Enhanced with DB/Firebase integration

📁 app/src/main/java/com/example/soukify/ui/settings/
└── NotificationsFragment.java      # Updated to use string resources

📁 app/src/main/java/com/example/soukify/ui/shop/
└── ProductDetailFragment.java      # Updated to use string resources
```

### Existing Repository
```
📁 app/src/main/java/com/example/soukify/data/repositories/
└── UserSettingRepository.java      # Firebase MVVM Repository (Existing)
```

### Documentation
```
📁 Project Root/
├── MULTILINGUAL_SETUP.md           # Detailed Technical Documentation
└── IMPLEMENTATION_SUMMARY.md       # Summary of Changes
```

## Common Tasks

### Add a New String Resource

**1. Add to English** (`values/strings.xml`):
```xml
<string name="my_new_string">English Text</string>
```

**2. Add to French** (`values-fr/strings.xml`):
```xml
<string name="my_new_string">Texte Français</string>
```

**3. Add to Arabic** (`values-ar/strings.xml`):
```xml
<string name="my_new_string">النص العربي</string>
```

**4. Use in Code**:
```java
String text = getString(R.string.my_new_string);
Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
```

### Get User Settings in Code

```java
// Initialize repository
UserSettingRepository repo = new UserSettingRepository(getApplication());

// Load settings
repo.loadUserSettings(userId);

// Observe changes
repo.getCurrentUserSettings().observe(getViewLifecycleOwner(), settings -> {
    if (settings != null) {
        String language = settings.getLanguage();
        String currency = settings.getCurrency();
        // Use the settings
    }
});
```

### Save User Settings

```java
UserSettingModel settings = new UserSettingModel(userId);
settings.setLanguage("en");
settings.setCurrency("MAD");
settings.setTheme("light");
settings.setNotifications(true);

userSettingRepository.updateUserSettings(settings);
```

### Local Database Operations

All settings are stored in Firebase Firestore:

```java
// Get database instance - NOT APPLICABLE
// Firebase is used directly

// Insert/Save
userSettingRepository.saveUserSettings(userSettingModel);

// Get with LiveData
userSettingRepository.getCurrentUserSettings().observe(owner, settings -> {});

// Update specific field
userSettingRepository.updateLanguage(userId, "fr");

// Delete
userSettingRepository.deleteUserSettings(userId);
```

## Database Schema

```
Firestore Collection: user_settings
Document ID: {userId}

Fields:
  - user_id: String
  - theme: String (DEFAULT: 'system')
  - language: String (DEFAULT: 'en')
  - currency: String (DEFAULT: 'MAD')
  - notifications: Boolean (DEFAULT: TRUE)
```

## Supported Languages

| Language | Code | Default |
|----------|------|---------|
| English | en | ✓ |
| French | fr | |
| Arabic | ar | |

## String Resource Categories

### Common Messages
```java
getString(R.string.please_enter_email_and_password)
getString(R.string.login_successful)
getString(R.string.error_updating_user_profile)
```

### Settings
```java
getString(R.string.language)
getString(R.string.currency)
getString(R.string.use_device_language)
getString(R.string.error_unable_to_save_settings)
```

### Notifications
```java
getString(R.string.notification_preferences_saved)
getString(R.string.unable_to_open_system_settings)
```

### Products
```java
getString(R.string.deleting_product)
getString(R.string.product_deleted_successfully)
```

### Common Actions
```java
getString(R.string.save)
getString(R.string.delete)
getString(R.string.edit)
getString(R.string.cancel)
```

## Architecture Overview

```
┌─────────────────────────────────────┐
│     UI Layer (Fragments)            │
│  - LanguageCurrencyFragment         │
│  - NotificationsFragment            │
│  - ProductDetailFragment            │
└──────────────┬──────────────────────┘
               │
     ┌─────────▼──────────┐
     │  Repository Layer  │
     │ UserSettingRepo    │
     └─────────┬──────────┘
               │
    ┌──────────▼──────────┐
    │     Firebase        │
    │  Firestore Cloud    │
    │  Persistence        │
    └─────────────────────┘

String Resources (3 Languages)
├── values/strings.xml (English)
├── values-fr/strings.xml (French)
└── values-ar/strings.xml (Arabic)
```

## Key Classes

### UserSettingModel (Firebase)
```java
- userId: String
- theme: String
- language: String
- currency: String
- notifications: boolean
```

### UserSettingRepository
```java
// Query methods
getCurrentUserSettings(): LiveData<UserSettingModel>
getErrorMessage(): LiveData<String>
getIsLoading(): LiveData<Boolean>

// CRUD methods
saveUserSettings(UserSettingModel)
updateUserSettings(UserSettingModel)
deleteUserSettings(String userId)
loadUserSettings(String userId)

// Specific update methods
updateTheme(String userId, String theme)
updateLanguage(String userId, String language)
updateCurrency(String userId, String currency)
updateNotifications(String userId, boolean enabled)
```

## Error Handling

All errors use localized string resources:

```java
// Bad ❌
Toast.makeText(context, "Error occurred", Toast.LENGTH_SHORT).show();

// Good ✅
Toast.makeText(context, getString(R.string.error_unable_to_save_settings), 
    Toast.LENGTH_SHORT).show();
```

## Testing

### Unit Test Template
```java
@Test
public void testLanguageUpdate() {
    UserSettingModel settings = new UserSettingModel("user123");
    settings.setLanguage("fr");
    assertEquals("fr", settings.getLanguage());
}
```

## Troubleshooting

### Settings Not Persisting
1. Check Firebase connection
2. Verify database transaction completed
3. Check user permissions in Firestore

### Language Not Changing
1. Verify string resource file exists
2. Check language code is correct (en, fr, ar)
3. Ensure app is restarted (for full locale change)

### Database Errors
1. Check database version matches
2. Verify migration scripts if updating
3. Check primary key constraints

## Useful Gradle Dependencies

```gradle
// Firebase
implementation "com.google.firebase:firebase-firestore:24.0.0"
implementation "com.google.firebase:firebase-auth:21.0.0"

// LiveData & ViewModel
implementation "androidx.lifecycle:lifecycle-viewmodel:2.5.0"
implementation "androidx.lifecycle:lifecycle-livedata:2.5.0"
```

## Deployment Checklist

- [ ] All three language files validated (en, fr, ar)
- [ ] Database migration script tested
- [ ] Firebase rules updated for user_settings collection
- [ ] LanguageCurrencyFragment tested in all languages
- [ ] Offline mode tested
- [ ] Real-time sync tested
- [ ] Error messages displayed correctly
- [ ] Documentation updated
- [ ] Team trained on conventions

## Additional Resources

- [Android Localization](https://developer.android.com/guide/topics/resources/localization)
- [Room Persistence Library](https://developer.android.com/training/data-storage/room)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [LiveData & ViewModel](https://developer.android.com/topic/libraries/architecture)
- [Material Design](https://material.io/design)

---

**Last Updated**: December 2025
**Version**: 1.0 (Complete Implementation)
