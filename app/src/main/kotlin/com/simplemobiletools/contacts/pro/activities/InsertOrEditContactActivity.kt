package com.simplemobiletools.contacts.pro.activities

import android.app.Activity
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Email
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.SearchView
import androidx.core.view.MenuItemCompat
import androidx.viewpager.widget.ViewPager
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.contacts.pro.R
import com.simplemobiletools.contacts.pro.adapters.ViewPagerAdapter
import com.simplemobiletools.contacts.pro.dialogs.ChangeSortingDialog
import com.simplemobiletools.contacts.pro.dialogs.FilterContactSourcesDialog
import com.simplemobiletools.contacts.pro.extensions.config
import com.simplemobiletools.contacts.pro.extensions.getContactPublicUri
import com.simplemobiletools.contacts.pro.fragments.MyViewPagerFragment
import com.simplemobiletools.contacts.pro.helpers.*
import com.simplemobiletools.contacts.pro.interfaces.RefreshContactsListener
import com.simplemobiletools.contacts.pro.models.Contact
import kotlinx.android.synthetic.main.activity_insert_edit_contact.*
import kotlinx.android.synthetic.main.fragment_contacts.*
import kotlinx.android.synthetic.main.fragment_favorites.*

class InsertOrEditContactActivity : SimpleActivity(), RefreshContactsListener {
    private val START_INSERT_ACTIVITY = 1
    private val START_EDIT_ACTIVITY = 2

    private var isSearchOpen = false
    private var searchMenuItem: MenuItem? = null
    private var isSelectContactIntent = false
    private var specialMimeType: String? = null

    private val contactsFavoritesList = arrayListOf(
        TAB_CONTACTS,
        TAB_FAVORITES
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_insert_edit_contact)
        isSelectContactIntent = intent.action == Intent.ACTION_PICK

        if (isSelectContactIntent) {
            specialMimeType = when (intent.data) {
                Email.CONTENT_URI -> Email.CONTENT_ITEM_TYPE
                Phone.CONTENT_URI -> Phone.CONTENT_ITEM_TYPE
                else -> null
            }
        }

        new_contact_holder.beGoneIf(isSelectContactIntent)
        select_contact_label.beGoneIf(isSelectContactIntent)

        if (checkAppSideloading()) {
            return
        }

        setupTabColors()

        // we do not really care about the permission request result. Even if it was denied, load private contacts
        handlePermission(PERMISSION_READ_CONTACTS) {
            if (it) {
                handlePermission(PERMISSION_WRITE_CONTACTS) {
                    handlePermission(PERMISSION_GET_ACCOUNTS) {
                        initFragments()
                    }
                }
            } else {
                initFragments()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        searchMenuItem?.collapseActionView()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_insert_or_edit, menu)
        setupSearch(menu)
        updateMenuItemColors(menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.sort -> showSortingDialog()
            R.id.filter -> showFilterDialog()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)
        if (resultCode == Activity.RESULT_OK) {
            hideKeyboard()
            finish()
        }
    }

    private fun initFragments() {
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                if (isSearchOpen) {
                    getCurrentFragment()?.onSearchQueryChanged("")
                    searchMenuItem?.collapseActionView()
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                insert_or_edit_tabs_holder.getTabAt(position)?.select()
                invalidateOptionsMenu()
            }
        })

        viewpager.onGlobalLayout {
            refreshContacts(getTabsMask())
        }

        insert_or_edit_tabs_holder.onTabSelectionChanged(
            tabUnselectedAction = {
                it.icon?.applyColorFilter(config.textColor)
            },
            tabSelectedAction = {
                if (isSearchOpen) {
                    getCurrentFragment()?.onSearchQueryChanged("")
                    searchMenuItem?.collapseActionView()
                }
                viewpager.currentItem = it.position
                it.icon?.applyColorFilter(getAdjustedPrimaryColor())
            }
        )

        insert_or_edit_tabs_holder.removeAllTabs()
        var skippedTabs = 0
        contactsFavoritesList.forEachIndexed { index, value ->
            if (config.showTabs and value == 0 && value == TAB_FAVORITES) {
                skippedTabs++
            } else {
                val tab = insert_or_edit_tabs_holder.newTab().setIcon(getTabIcon(index))
                insert_or_edit_tabs_holder.addTab(tab, index - skippedTabs, index == 0)
            }
        }

        insert_or_edit_tabs_holder.beVisibleIf(skippedTabs == 0)

        select_contact_label?.setTextColor(getAdjustedPrimaryColor())
        new_contact_tmb?.setImageDrawable(resources.getColoredDrawableWithColor(R.drawable.ic_add_person_vector, config.textColor))
        new_contact_name.setTextColor(config.textColor)
        new_contact_holder?.setOnClickListener {
            createNewContact()
        }
    }

    private fun setupSearch(menu: Menu) {
        val searchManager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        searchMenuItem = menu.findItem(R.id.search)
        (searchMenuItem!!.actionView as SearchView).apply {
            setSearchableInfo(searchManager.getSearchableInfo(componentName))
            isSubmitButtonEnabled = false
            queryHint = getString(getSearchString())
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String) = false

                override fun onQueryTextChange(newText: String): Boolean {
                    if (isSearchOpen) {
                        getCurrentFragment()?.onSearchQueryChanged(newText)
                    }
                    return true
                }
            })
        }

        MenuItemCompat.setOnActionExpandListener(searchMenuItem, object : MenuItemCompat.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem?): Boolean {
                getCurrentFragment()?.onSearchOpened()
                isSearchOpen = true
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem?): Boolean {
                getCurrentFragment()?.onSearchClosed()
                isSearchOpen = false
                return true
            }
        })
    }

    private fun getSearchString(): Int {
        return when (getCurrentFragment()) {
            favorites_fragment -> R.string.search_favorites
            else -> R.string.search_contacts
        }
    }

    private fun getCurrentFragment(): MyViewPagerFragment? {
        return if (viewpager.currentItem == 0) {
            contacts_fragment
        } else {
            favorites_fragment
        }
    }

    private fun setupTabColors() {
        insert_or_edit_tabs_holder.apply {
            background = ColorDrawable(config.backgroundColor)
            setSelectedTabIndicatorColor(getAdjustedPrimaryColor())
        }
    }

    override fun refreshContacts(refreshTabsMask: Int) {
        if (isDestroyed || isFinishing) {
            return
        }

        if (viewpager.adapter == null) {
            viewpager.adapter = ViewPagerAdapter(this, contactsFavoritesList, getTabsMask())
        }

        ContactsHelper(this).getContacts {
            if (isDestroyed || isFinishing) {
                return@getContacts
            }

            val contacts = it.filter {
                if (specialMimeType != null) {
                    val hasRequiredValues = when (specialMimeType) {
                        Email.CONTENT_ITEM_TYPE -> it.emails.isNotEmpty()
                        Phone.CONTENT_ITEM_TYPE -> it.phoneNumbers.isNotEmpty()
                        else -> true
                    }
                    !it.isPrivate() && hasRequiredValues
                } else {
                    true
                }
            } as ArrayList<Contact>

            val placeholderText = when (specialMimeType) {
                Email.CONTENT_ITEM_TYPE -> getString(R.string.no_contacts_with_emails)
                Phone.CONTENT_ITEM_TYPE -> getString(R.string.no_contacts_with_phone_numbers)
                else -> null
            }

            if (refreshTabsMask and TAB_CONTACTS != 0) {
                contacts_fragment?.refreshContacts(contacts, placeholderText)
            }

            if (refreshTabsMask and TAB_FAVORITES != 0) {
                favorites_fragment?.refreshContacts(contacts, placeholderText)
            }
        }
    }

    override fun contactClicked(contact: Contact) {
        hideKeyboard()
        if (isSelectContactIntent) {
            Intent().apply {
                data = getResultUri(contact)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setResult(RESULT_OK, this)
            }
            finish()
        } else {
            val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
            val email = getEmailFromIntent(intent) ?: ""
            Intent(applicationContext, EditContactActivity::class.java).apply {
                data = getContactPublicUri(contact)
                action = ADD_NEW_CONTACT_NUMBER

                if (phoneNumber.isNotEmpty()) {
                    putExtra(KEY_PHONE, phoneNumber)
                }

                if (email.isNotEmpty()) {
                    putExtra(KEY_EMAIL, email)
                }

                putExtra(IS_PRIVATE, contact.isPrivate())
                startActivityForResult(this, START_EDIT_ACTIVITY)
            }
        }
    }

    private fun getResultUri(contact: Contact): Uri {
        return when {
            specialMimeType != null -> {
                val contactId = ContactsHelper(this).getContactMimeTypeId(contact.id.toString(), specialMimeType!!)
                Uri.withAppendedPath(ContactsContract.Data.CONTENT_URI, contactId)
            }
            else -> getContactPublicUri(contact)
        }
    }

    private fun createNewContact() {
        val name = intent.getStringExtra(KEY_NAME) ?: ""
        val phoneNumber = getPhoneNumberFromIntent(intent) ?: ""
        val email = getEmailFromIntent(intent) ?: ""

        Intent().apply {
            action = Intent.ACTION_INSERT
            data = ContactsContract.Contacts.CONTENT_URI

            if (phoneNumber.isNotEmpty()) {
                putExtra(KEY_PHONE, phoneNumber)
            }

            if (name.isNotEmpty()) {
                putExtra(KEY_NAME, name)
            }

            if (email.isNotEmpty()) {
                putExtra(KEY_EMAIL, email)
            }

            try {
                startActivityForResult(this, START_INSERT_ACTIVITY)
            } catch (e: ActivityNotFoundException) {
                toast(R.string.no_app_found)
            } catch (e: Exception) {
                showErrorToast(e)
            }
        }
    }

    fun fabClicked() {
        createNewContact()
    }

    private fun showSortingDialog() {
        ChangeSortingDialog(this) {
            refreshContacts(getTabsMask())
        }
    }

    fun showFilterDialog() {
        FilterContactSourcesDialog(this) {
            contacts_fragment?.forceListRedraw = true
            refreshContacts(getTabsMask())
        }
    }

    private fun getTabsMask(): Int {
        var mask = TAB_CONTACTS
        if (config.showTabs and TAB_FAVORITES != 0) {
            mask += TAB_FAVORITES
        }
        return mask
    }
}
