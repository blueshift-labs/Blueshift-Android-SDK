package com.blueshift.blueshiftinboxdemo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.blueshift.Blueshift
import com.blueshift.BlueshiftConstants
import com.blueshift.inbox.BlueshiftInboxActivity
import com.blueshift.inbox.BlueshiftInboxFragment

/**
 * A simple [Fragment] subclass.
 * Use the [HomeFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class HomeFragment : Fragment() {
    private var mSpinner: Spinner? = null
    private var mSendEventButton: Button? = null
    private var mOpenInboxButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view: View? = inflater.inflate(R.layout.fragment_home, container, false)
        if (view != null) {
            mSpinner = view.findViewById(R.id.spinner)
            mSendEventButton = view.findViewById(R.id.buttonSendEvent)
            mOpenInboxButton = view.findViewById(R.id.buttonOpenInbox)


            mSendEventButton!!.setOnClickListener {
                if (mSpinner != null) {
                    val selectedItem: String = mSpinner!!.selectedItem as String
                    Blueshift.getInstance(context).trackEvent(selectedItem, null, false)
                }
            }

            mOpenInboxButton!!.setOnClickListener {
                val intent = Intent(context, BlueshiftInboxActivity::class.java)

//                intent.putExtra(
//                    BlueshiftConstants.INBOX_EMPTY_MESSAGE,
//                    "The inbox is empty!"
//                )
//                intent.putExtra(
//                    BlueshiftConstants.INBOX_ITEM_LAYOUT,
//                    R.layout.custom_bsft_inbox_list_item
//                )
//                intent.putExtra(
//                    BlueshiftConstants.INBOX_UNREAD_INDICATOR_COLOR,
//                    Color.MAGENTA
//                )
//                intent.putExtra(
//                    BlueshiftConstants.INBOX_REFRESH_INDICATOR_COLORS,
//                    intArrayOf(Color.CYAN, Color.BLUE, Color.GREEN)
//                )

                startActivity(intent)
            }

            val btnCellUi: Button = view.findViewById(R.id.buttonCustomInboxCellUi)
            val btnCellElements: Button = view.findViewById(R.id.buttonCustomInboxCellElements)
            val btnDateFormat: Button = view.findViewById(R.id.buttonCustomInboxDateFormat)
            val btnSort: Button = view.findViewById(R.id.buttonCustomInboxSort)
            val btnFilter: Button = view.findViewById(R.id.buttonCustomInboxFilter)

            btnCellUi.setOnClickListener { launchCustomInbox(R.string.custom_inbox_ui) }
            btnCellElements.setOnClickListener { launchCustomInbox(R.string.custom_inbox_elements) }
            btnDateFormat.setOnClickListener { launchCustomInbox(R.string.custom_inbox_date_format) }
            btnSort.setOnClickListener { launchCustomInbox(R.string.custom_inbox_sort) }
            btnFilter.setOnClickListener { launchCustomInbox(R.string.custom_inbox_filter) }
        }

        return view
    }

    private fun launchCustomInbox(inboxType: Int) {
        val intent = Intent(context, CustomInboxActivity::class.java)
        intent.putExtra("custom_inbox_type", inboxType)
        startActivity(intent)
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}