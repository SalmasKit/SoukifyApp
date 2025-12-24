# Soukify Multilingual Support & User Settings Documentation

## Overview
This documentation describes the complete implementation of multilingual support and user settings persistence in the Soukify application.

## Architecture

### 1. String Resources (Localization)
All user-facing strings are now externalized into XML resource files, supporting three languages:

#### English (Default)
- **File**: `app/src/main/res/values/strings.xml`
- **Language Code**: `en`
- **Includes**: 200+ string resources covering all UI text

#### French
- **File**: `app/src/main/res/values-fr/strings.xml`
- **Language Code**: `fr`
- **Includes**: Complete French translations

#### Arabic
- **File**: `app/src/main/res/values-ar/strings.xml`
- **Language Code**: `ar`
- **Includes**: Complete Arabic translations with RTL support

### 2. User Settings Storage

#### Firebase Backend (UserSettingRepository)
```java
- Location: com.example.soukify.data.repositories.UserSettingRepository
- Purpose: Cloud synchronization and backup
- Collection: user_settings
- Fields stored:
  - user_id: String (Primary Key)
  - theme: String (light|dark|system)
  - language: String (en|fr|ar)
  - currency: String (MAD|USD|EUR|...)
  - notifications: Boolean
```

Settings are stored in Firebase Firestore for cloud-based persistence and multi-device synchronization.

## Implementation Details

### Database Schema
```
Firestore Collection: user_settings
Document ID: {userId}
Fields:
  - user_id: String
  - theme: String DEFAULT 'system'
  - language: String DEFAULT 'en'
  - currency: String DEFAULT 'MAD'
  - notifications: Boolean DEFAULT TRUE
```

### Key Classes

#### 2. UserSettingModel (Firebase POJO)
```java
Location: com.example.soukify.data.models.UserSettingModel
- userId: String
- theme: String
- language: String
- currency: String
- notifications: boolean
```

#### 3. UserSettingRepository (MVVM Repository Pattern)
```java
Location: com.example.soukify.data.repositories.UserSettingRepository
Methods:
- saveUserSettings(UserSettingModel)
- updateUserSettings(UserSettingModel)
- deleteUserSettings(String userId)
- loadUserSettings(String userId)
- initializeUserSettings(String userId)
- updateTheme(String userId, String theme)
- updateLanguage(String userId, String language)
- updateCurrency(String userId, String currency)
- updateNotifications(String userId, boolean enabled)
```

#### 4. LanguageCurrencyFragment (UI Implementation)
```java
Location: com.example.soukify.ui.settings.LanguageCurrencyFragment
Features:
- Language selection dropdown (en|fr|ar)
- Currency selection dropdown
- Device language toggle switch
- Real-time settings loading from Firebase
- Persistent storage to database
- All strings externalized to resources
```

## String Resources Organization

### Common UI Strings
```xml
<!-- Settings Screen -->
<string name="language">Language</string>
<string name="currency">Currency</string>
<string name="use_device_language">Use device language</string>

<!-- Common Actions -->
<string name="save">Save</string>
<string name="delete">Delete</string>
<string name="edit">Edit</string>
<string name="cancel">Cancel</string>

<!-- Error Messages -->
<string name="error_unable_to_save_settings">Error: Unable to save settings</string>
<string name="please_select_both_language_and_currency">Please select both language and currency</string>
```

### Supported Languages Array
```xml
<!-- In arrays.xml -->
<string-array name="supported_languages">
    <item>English</item>
    <item>Français</item>
    <item>العربية</item>
</string-array>

<string-array name="supported_currencies">
    <item>MAD</item>
    <item>USD</item>
    <item>EUR</item>
    <item>GBP</item>
</string-array>
```

## Usage Examples

### 1. Loading User Settings in Fragment
```java
// In onViewCreated()
userSettingRepository.getCurrentUserSettings().observe(getViewLifecycleOwner(), userSettings -> {
    if (userSettings != null) {
        String language = userSettings.getLanguage();
        String currency = userSettings.getCurrency();
        // Update UI with these values
    }
});

// Load from repository
userSettingRepository.loadUserSettings(currentUserId);
```

### 2. Saving User Settings
```java
UserSettingModel settings = new UserSettingModel(userId);
settings.setLanguage("en");
settings.setCurrency("MAD");
settings.setTheme("light");
settings.setNotifications(true);

userSettingRepository.updateUserSettings(settings);
```

### 3. Accessing String Resources
```java
// In Activity/Fragment
String message = getString(R.string.language_and_currency);

// With formatting
String formatted = getString(R.string.saved_format, language, currency);

// From Repository/ViewModel
String errorMsg = getString(R.string.error_unable_to_save_settings);
```

### 4. Local Database Operations
```java
AppDatabase db = AppDatabase.getInstance(context);
UserSettingsDao dao = db.userSettingsDao();

// Get live data
LiveData<UserSettingsEntity> settings = dao.getUserSettingsLive(userId);

// Update specific field
dao.updateLanguage(userId, "ar");

// Get synchronously
UserSettingsEntity entity = dao.getUserSettings(userId);
```

## Deployment Checklist

- [ ] All three language files validated (en, fr, ar)
- [ ] Firebase Firestore collection initialized
- [ ] Firebase rules configured for user_settings
- [ ] LanguageCurrencyFragment tested in all languages
- [ ] Real-time sync tested
- [ ] Error messages displayed correctly
- [ ] Documentation updated
- [ ] Team trained on conventions

## Best Practices

### 1. Always Use String Resources
❌ Bad:
```java
Toast.makeText(context, "Settings saved successfully", Toast.LENGTH_SHORT).show();
```

✅ Good:
```java
Toast.makeText(context, getString(R.string.notification_preferences_saved), Toast.LENGTH_SHORT).show();
```

### 2. Handle Null Values Safely
```java
String language = userSettings.getLanguage();
if (language != null && !language.isEmpty()) {
    // Use language
} else {
    // Use default
    language = "en";
}
```

### 3. Sync Firebase with Settings
```java
// Save to Firebase
userSettingRepository.updateUserSettings(userSettingModel);

// Load and observe changes
userSettingRepository.loadUserSettings(userId);
userSettingRepository.getCurrentUserSettings().observe(owner, settings -> {
    // Update UI with new settings
});
```

## Testing

### Unit Tests
```java
// Test language update
@Test
public void testUpdateLanguage() {
    UserSettingModel settings = new UserSettingModel("user123");
    settings.setLanguage("fr");
    
    assertEquals("fr", settings.getLanguage());
}
```

### Integration Tests
```java
@Test
public void testFirebaseLanguageUpdate() {
    UserSettingModel settings = new UserSettingModel("user123");
    settings.setLanguage("ar");
    
    repo.updateUserSettings(settings);
    
    // Verify settings saved to Firebase
    repo.loadUserSettings("user123");
    assertEquals("ar", repo.getCurrentUserSettings().getValue().getLanguage());
}
```

## Future Enhancements

1. **Offline Caching**: Cache Firebase data locally for offline access
2. **Update Queue**: Queue settings changes when offline, sync when online
3. **Analytics**: Track language and currency preferences for UX optimization
4. **Device Configuration**: Apply language to entire app (not just UI strings)
5. **Theme Application**: Actually apply selected theme to app resources
6. **Multi-user Support**: Handle settings switching when user logs out/in

## Files Added/Modified

### New Files Created:
- `app/src/main/java/com/example/soukify/data/database/AppDatabase.java`
- `app/src/main/java/com/example/soukify/data/database/UserSettingsEntity.java`
- `app/src/main/java/com/example/soukify/data/database/UserSettingsDao.java`
- `app/src/main/res/values-fr/strings.xml`
- `app/src/main/res/values-ar/strings.xml`

### Files Modified:
- `app/src/main/res/values/strings.xml` (Added new strings)
- `app/src/main/java/com/example/soukify/ui/settings/LanguageCurrencyFragment.java`
- `app/src/main/java/com/example/soukify/ui/settings/NotificationsFragment.java`
- `app/src/main/java/com/example/soukify/ui/shop/ProductDetailFragment.java`

## Support

For questions or issues with the multilingual implementation:
1. Check string resources first
2. Verify database entries are correct
3. Monitor Firebase console for sync issues
4. Check Logcat for error messages

## References

- [Android Localization Guide](https://developer.android.com/guide/topics/resources/localization)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Firebase Firestore](https://firebase.google.com/docs/firestore)
- [MVVM Pattern](https://developer.android.com/jetpack/guide)
