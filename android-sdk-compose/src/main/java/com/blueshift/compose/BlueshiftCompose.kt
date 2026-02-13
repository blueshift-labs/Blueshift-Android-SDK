package com.blueshift.compose

import android.app.Activity
import androidx.compose.material3.Surface
import androidx.compose.ui.platform.ComposeView
import com.blueshift.BlueshiftInAppListener
import com.blueshift.inappmessage.InAppManager
import com.blueshift.inappmessage.InAppMessage

object BlueshiftCompose {

    fun addInAppRenderer() {
        InAppManager.inAppRenderListener(object : BlueshiftInAppListener {
            override fun onInAppDelivered(attributes: MutableMap<String, Any>?) {}

            override fun onInAppOpened(attributes: MutableMap<String, Any>?) {}

            override fun onInAppClicked(attributes: MutableMap<String, Any>?) {}

            override fun renderInApp(inAppMessage: InAppMessage?, activity: Activity?): Boolean {
                activity?.let {
                    activity.setContentView(
                        ComposeView(activity).apply {
                            setContent {
                                Surface {
                                    InAppBanner()
                                }
                            }
                        }
                    )
                    return true
                }
                return false
            }
        })
    }

}