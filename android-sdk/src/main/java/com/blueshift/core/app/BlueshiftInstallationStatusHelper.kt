package com.blueshift.core.app

import com.blueshift.BlueshiftConstants
import com.blueshift.core.common.BlueshiftLogger
import com.blueshift.util.CommonUtils
import java.io.File

class BlueshiftInstallationStatusHelper {

    fun getInstallationStatus(
        appVersion: String, previousAppVersion: String?, database: File
    ): BlueshiftInstallationStatus {
        var status = BlueshiftInstallationStatus.NO_CHANGE

        if (previousAppVersion == null) {
            // When stored value for previousAppVersion is absent. It could be because..
            // 1 - The app is freshly installed
            // 2 - The app is updated, but the old version did not have blueshift SDK
            // 3 - The app is updated, but the old version had blueshift SDK's old version.
            //
            // Case 1 & 2 will be treated as app_install.
            // Case 3 will be treated as app_update. We do this by checking the availability
            // of the database file created by the older version of blueshift SDK. If present,
            // it is app update, else it is app install.

            if (database.exists()) {
                // case 3
                status = BlueshiftInstallationStatus.APP_UPDATE
                BlueshiftLogger.d("App updated. Previous app had Blueshift SDK version older than v3.4.6.")
            } else {
                // case 1 OR 2
                status = BlueshiftInstallationStatus.APP_INSTALL
                BlueshiftLogger.d("App installation detected.")
            }
        } else {
            // When a stored value for previousAppVersion is found, we compare it with the existing
            // app version value. If there is a change, we consider it as app_update.
            //
            // PS: Android will not let you downgrade the app version without installing the old
            // version, so it will always be an app upgrade event.
            if (appVersion != previousAppVersion) {
                status = BlueshiftInstallationStatus.APP_UPDATE
                BlueshiftLogger.d("App updated from $previousAppVersion to $appVersion.")
            }
        }

        return status
    }

    fun getEventAttributes(
        status: BlueshiftInstallationStatus, previousAppVersion: String?
    ): HashMap<String, Any> {
        return when (status) {
            BlueshiftInstallationStatus.APP_INSTALL -> hashMapOf(
                BlueshiftConstants.KEY_APP_INSTALLED_AT to CommonUtils.getCurrentUtcTimestamp()
            )

            BlueshiftInstallationStatus.APP_UPDATE -> {
                val updatedMap: HashMap<String, Any> = hashMapOf(
                    BlueshiftConstants.KEY_APP_UPDATED_AT to CommonUtils.getCurrentUtcTimestamp()
                )
                previousAppVersion?.let {
                    updatedMap[BlueshiftConstants.KEY_PREVIOUS_APP_VERSION] = it
                }
                updatedMap
            }

            BlueshiftInstallationStatus.NO_CHANGE -> hashMapOf()
        }
    }
}