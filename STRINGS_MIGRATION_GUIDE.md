# String Resources Migration Guide

## Overview
All hardcoded text strings have been added to the string resource files (values/strings.xml, values-fr/strings.xml, values-ar/strings.xml). Now, all XML layout files need to be updated to use @string references instead of hardcoded text.

## Completed Tasks ✅

### String Resource Files (ALL 3 LANGUAGES COMPLETE)
- ✅ **values/strings.xml** - English: 400+ string entries
- ✅ **values-fr/strings.xml** - French: All translations complete
- ✅ **values-ar/strings.xml** - Arabic: All translations with RTL support

### Layout Files Partially Updated
- ✅ **dialog_create_shop.xml** - 100% updated with @string references
- ✅ **working_hours_basic.xml** - Partially updated (Monday field done)
- ✅ **fragment_shop_home.xml** - Shop banner updated
- ✅ **MainActivity.java** - Language restoration on startup added

## Remaining Layout Files to Update

### Working Hours Files (3 files)
Update these files with the following pattern:
- Replace `android:text="Working Hours"` → `android:text="@string/working_hours"`
- Replace day names (Monday-Sunday) → `@string/monday` through `@string/sunday`
- Replace `android:hint="From"` → `android:hint="@string/from"`
- Replace `android:hint="To"` → `android:hint="@string/to"`
- Replace `android:text=" - "` → `android:text="@string/time_separator"`

Files:
1. **working_hours_simple.xml**
2. **working_hours_selector.xml**
3. **working_hours_minimal.xml**

### Fragment Files (Main UI Components)
Files to update and their key strings:

1. **fragment_search.xml**
   - "Leather Crafts" → @string/leather_crafts
   - "Promotion" → @string/promotion_label
   - "Delivery" → @string/delivery_label
   - "Élément introuvable" → @string/item_not_found_fr

2. **fragment_product_detail.xml**
   - "Edit product" → @string/edit_product
   - "Delete product" → @string/delete_product
   - "No Image" → @string/no_image
   - "Product Name" → @string/product_name_placeholder
   - "Type" → Already in strings
   - "0" (for likes/counts) → @string/zero
   - "0.00" (prices) → @string/zero_decimal
   - "Weight" → @string/weight_label
   - "Length" → @string/length_label
   - "Width" → @string/width_label
   - "Height" → @string/height_label
   - "Color" → @string/color_label
   - "Material" → @string/material_label
   - "Contact Seller" → @string/contact_seller
   - "Call seller" → @string/call_seller
   - "Email seller" → @string/email_seller
   - Various descriptions/placeholders → Appropriate @string references

3. **fragment_shop_home.xml** (partially done)
   - "Shop Name" → @string/shop_name
   - "Shop description goes here..." → @string/shop_description_placeholder
   - "Location" → @string/location_text
   - "Contact Information" → @string/contact_information
   - "Loading contact info..." → @string/loading_contact_info
   - "Shop Details" → @string/shop_details
   - "Created: Loading..." → @string/created_label
   - "Working Hours" → @string/working_hours
   - "Follow Us" → @string/follow_us
   - "Products" → @string/products
   - "Likes" → Already in strings
   - "Rating" → Already in strings
   - "No products yet" → @string/no_products_yet
   - "Add your first product to get started" → @string/add_first_product
   - All action buttons (Like, Favorite, Share, Chat)

### Item Layout Files (List Items)
Files:
1. **item_product_clean.xml**
   - "Like" → @string/like_action (contentDescription)
   - "Favorite" → @string/favorite_action (contentDescription)
   - "Visit Shop" → @string/visit_shop (contentDescription)
   - "Product Name" → @string/product_name_placeholder
   - "Type" → @string/Type
   - "0" → @string/zero (for counts)
   - "0.00" → @string/zero_decimal (prices)
   - "MAD" → @string/currency_mad

2. **item_shop.xml**
   - "Like" → @string/like_action (contentDescription)
   - "Favorite" → @string/favorite_action (contentDescription)
   - "Share" → @string/share_action (contentDescription)
   - "Chat" → @string/chat_action (contentDescription)
   - "0" → @string/zero
   - "0.0" → @string/rating_format
   - "+212 600-000-000" → @string/phone_placeholder_format
   - "email@shop.com" → @string/email_placeholder

### Other Files
- **fragment_settings.xml** - Check for email placeholders
- **item_setting.xml** - Check for any hardcoded strings
- **item_message_sent.xml** & **item_message_received.xml** - If any
- **fragment_privacy.xml**, **fragment_privacy_policy.xml** - Already handled via Java code

## String Resource Reference

### Common Replacements
```xml
<!-- Days -->
@string/monday, @string/tuesday, @string/wednesday, @string/thursday,
@string/friday, @string/saturday, @string/sunday

<!-- Time -->
@string/from, @string/to, @string/time_separator

<!-- Shop -->
@string/shop_name, @string/shop_banner_text, @string/description
@string/shop_details, @string/shop_options, @string/shop_description_placeholder

<!-- Product -->
@string/product_name_placeholder, @string/zero, @string/zero_decimal
@string/like_action, @string/favorite_action, @string/edit_product

<!-- Actions -->
@string/enable_promotion, @string/enable_delivery
@string/contact_seller, @string/call_seller, @string/email_seller

<!-- Numbers/Formats -->
@string/zero, @string/zero_decimal, @string/rating_format
@string/currency_mad, @string/phone_placeholder_format

<!-- Locations -->
@string/location_text, @string/region, @string/city, @string/address
```

## Benefits After Completion
✅ **Full Multilingual Support** - All text automatically translates based on device/user language selection
✅ **Centralized Text Management** - All strings in one place per language
✅ **Easy Maintenance** - Update text globally from strings.xml files
✅ **RTL Support** - Arabic translations display correctly with RTL layout
✅ **Professional Localization** - Meets Google Play Store localization requirements

## How to Use String Resources in XML

### For android:text attribute:
```xml
<!-- Before -->
android:text="Product Name"

<!-- After -->
android:text="@string/product_name_placeholder"
```

### For android:hint attribute (on EditText):
```xml
<!-- Before -->
android:hint="Enter name"

<!-- After -->
android:hint="@string/shop_name"
```

### For android:contentDescription attribute (for accessibility):
```xml
<!-- Before -->
android:contentDescription="Like"

<!-- After -->
android:contentDescription="@string/like_action"
```

## Verification Checklist
- [ ] No more hardcoded English text visible in layout XML files
- [ ] All @string references resolve (no red squiggles in Android Studio)
- [ ] App switches languages in Language & Currency settings
- [ ] App remembers language choice after restart
- [ ] All three languages display correctly (EN, FR, AR)
- [ ] Arabic text displays right-to-left
- [ ] All buttons and labels show translated text

## Notes
- Numeric placeholders like "0", "0.00", "0.0" should use string resources too for consistency
- Always use @string/ references, never hardcode text in production XML
- Test language switching thoroughly after completing migrations
