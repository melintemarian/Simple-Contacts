package com.simplemobiletools.contacts.pro.helpers

import android.content.Context
import com.simplemobiletools.commons.helpers.BaseConfig

class Config(context: Context) : BaseConfig(context) {
    companion object {
        fun newInstance(context: Context) = Config(context)
    }

    var ignoredContactSources: HashSet<String>
        get() = prefs.getStringSet(IGNORED_CONTACT_SOURCES, hashSetOf(".")) as HashSet
        set(ignoreContactSources) = prefs.edit().remove(IGNORED_CONTACT_SOURCES).putStringSet(IGNORED_CONTACT_SOURCES, ignoreContactSources).apply()

    var showContactThumbnails: Boolean
        get() = prefs.getBoolean(SHOW_CONTACT_THUMBNAILS, true)
        set(showContactThumbnails) = prefs.edit().putBoolean(SHOW_CONTACT_THUMBNAILS, showContactThumbnails).apply()

    var showPhoneNumbers: Boolean
        get() = prefs.getBoolean(SHOW_PHONE_NUMBERS, false)
        set(showPhoneNumbers) = prefs.edit().putBoolean(SHOW_PHONE_NUMBERS, showPhoneNumbers).apply()

    var showOnlyContactsWithNumbers: Boolean
        get() = prefs.getBoolean(SHOW_ONLY_CONTACTS_WITH_NUMBERS, false)
        set(showOnlyContactsWithNumbers) = prefs.edit().putBoolean(SHOW_ONLY_CONTACTS_WITH_NUMBERS, showOnlyContactsWithNumbers).apply()

    var lastUsedContactSource: String
        get() = prefs.getString(LAST_USED_CONTACT_SOURCE, "")!!
        set(lastUsedContactSource) = prefs.edit().putString(LAST_USED_CONTACT_SOURCE, lastUsedContactSource).apply()

    var onContactClick: Int
        get() = prefs.getInt(ON_CONTACT_CLICK, ON_CLICK_VIEW_CONTACT)
        set(onContactClick) = prefs.edit().putInt(ON_CONTACT_CLICK, onContactClick).apply()

    var showContactFields: Int
        get() = prefs.getInt(
            SHOW_CONTACT_FIELDS, SHOW_FIRST_NAME_FIELD or SHOW_SURNAME_FIELD or SHOW_PHONE_NUMBERS_FIELD or SHOW_EMAILS_FIELD or
                SHOW_ADDRESSES_FIELD or SHOW_EVENTS_FIELD or SHOW_NOTES_FIELD or SHOW_GROUPS_FIELD or SHOW_CONTACT_SOURCE_FIELD
        )
        set(showContactFields) = prefs.edit().putInt(SHOW_CONTACT_FIELDS, showContactFields).apply()

    var showTabs: Int
        get() = prefs.getInt(SHOW_TABS, ALL_TABS_MASK)
        set(showTabs) = prefs.edit().putInt(SHOW_TABS, showTabs).apply()

    var showDialpadButton: Boolean
        get() = prefs.getBoolean(SHOW_DIALPAD_BUTTON, true)
        set(showDialpadButton) = prefs.edit().putBoolean(SHOW_DIALPAD_BUTTON, showDialpadButton).apply()

    var wasLocalAccountInitialized: Boolean
        get() = prefs.getBoolean(WAS_LOCAL_ACCOUNT_INITIALIZED, false)
        set(wasLocalAccountInitialized) = prefs.edit().putBoolean(WAS_LOCAL_ACCOUNT_INITIALIZED, wasLocalAccountInitialized).apply()

    var lastExportPath: String
        get() = prefs.getString(LAST_EXPORT_PATH, "")!!
        set(lastExportPath) = prefs.edit().putString(LAST_EXPORT_PATH, lastExportPath).apply()

    var speedDial: String
        get() = prefs.getString(SPEED_DIAL, "")!!
        set(speedDial) = prefs.edit().putString(SPEED_DIAL, speedDial).apply()

    var showPrivateContacts: Boolean
        get() = prefs.getBoolean(SHOW_PRIVATE_CONTACTS, true)
        set(showPrivateContacts) = prefs.edit().putBoolean(SHOW_PRIVATE_CONTACTS, showPrivateContacts).apply()

    var mergeDuplicateContacts: Boolean
        get() = prefs.getBoolean(MERGE_DUPLICATE_CONTACTS, true)
        set(mergeDuplicateContacts) = prefs.edit().putBoolean(MERGE_DUPLICATE_CONTACTS, mergeDuplicateContacts).apply()
}
