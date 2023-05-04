package com.blueshift.blueshiftinboxdemo

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.blueshift.blueshiftinboxdemo.fragments.InboxWithCustomDateFormat
import com.blueshift.blueshiftinboxdemo.fragments.InboxWithCustomFilter
import com.blueshift.blueshiftinboxdemo.fragments.InboxWithCustomListItemLayout
import com.blueshift.blueshiftinboxdemo.fragments.InboxWithCustomListItemStyle
import com.blueshift.blueshiftinboxdemo.fragments.InboxWithCustomSorting

class CustomInboxActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_inbox)

        supportFragmentManager.beginTransaction()
            .replace(
                R.id.fragmentContainerView,
                customInboxFragment(intent.getIntExtra("custom_inbox_type", 0))
            ).commit()
    }

    private fun customInboxFragment(resourceId: Int): Fragment {
        return when (resourceId) {
            R.string.custom_inbox_ui -> InboxWithCustomListItemLayout()
            R.string.custom_inbox_elements -> InboxWithCustomListItemStyle()
            R.string.custom_inbox_date_format -> InboxWithCustomDateFormat()
            R.string.custom_inbox_sort -> InboxWithCustomSorting()
            R.string.custom_inbox_filter -> InboxWithCustomFilter()
            else -> InboxWithCustomListItemLayout()
        }
    }
}
