package com.blueshift.blueshiftinboxdemo.fragments

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import com.blueshift.blueshiftinboxdemo.R
import com.blueshift.inbox.BlueshiftInboxAdapter
import com.blueshift.inbox.BlueshiftInboxAdapterExtension
import com.blueshift.inbox.BlueshiftInboxFragment
import com.blueshift.inbox.BlueshiftInboxMessage

class InboxWithCustomListItemStyle : BlueshiftInboxFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val adapterExtension = object : BlueshiftInboxAdapterExtension<Any> {
            override fun getViewType(message: BlueshiftInboxMessage): Int {
                return 0
            }

            override fun getLayoutIdForViewType(viewType: Int): Int {
                return R.layout.custom_bsft_inbox_list_item
            }

            override fun onCreateViewHolder(
                viewHolder: BlueshiftInboxAdapter.ViewHolder, viewType: Int
            ) {
                val colors = intArrayOf(
                    Color.parseColor("#141e30"),
                    Color.parseColor("#243b55")
                )
                val drawable = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, colors)

                viewHolder.apply {
                    // set color for the texts
                    setTitleTextViewColor(Color.parseColor("#FFFFFF"))
                    setDetailsTextViewColor(Color.parseColor("#FFFFFF"))
                    setDateTextViewColor(Color.parseColor("#FFFFFF"))

                    // set color for the background of the cell
                    setBackgroundDrawable(drawable)

                    // change font/typeface of text views
                    // setTitleTextViewTypeface(Typeface.DEFAULT)
                    // setDetailsTextViewTypeface(Typeface.DEFAULT)
                    // setDateTextViewTypeface(Typeface.DEFAULT)

                    // set color for the unread indicator
                    setUnreadIndicatorColor(Color.parseColor("#00FF00"))
                }
            }

            override fun onCreateViewHolderExtension(itemView: View, viewType: Int): Any? {
                return null
            }

            override fun onBindViewHolder(
                holder: BlueshiftInboxAdapter.ViewHolder,
                viewHolderExtension: Any?,
                message: BlueshiftInboxMessage?
            ) {

            }
        }

        setInboxAdapterExtension(adapterExtension)
    }
}