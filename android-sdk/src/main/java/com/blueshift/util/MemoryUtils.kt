package com.blueshift.util

import android.content.Context
import com.blueshift.BlueshiftLogger

/**
 * Utility class for 16KB page size detection and debugging.
 * Provides basic page size information for troubleshooting compatibility issues.
 */
internal object MemoryUtils {
    private const val TAG = "MemoryUtils"
    
    // Page size constants
    private const val PAGE_SIZE_4KB = 4096
    private const val PAGE_SIZE_16KB = 16384
    
    // Cache the page size to avoid repeated system calls
    @Volatile
    private var cachedPageSize: Int? = null
    
    /**
     * Get the system page size.
     * @return Page size in bytes (typically 4096 or 16384)
     */
    @JvmStatic
    private fun getPageSize(): Int {
        cachedPageSize?.let { return it }
        
        return synchronized(this) {
            cachedPageSize?.let { return it }
            
            try {
                // Try to get page size from system property
                val pageSizeStr = System.getProperty("ro.product.page_size")
                if (!pageSizeStr.isNullOrEmpty()) {
                    val pageSize = pageSizeStr.toInt()
                    cachedPageSize = pageSize
                    return pageSize
                }
            } catch (e: Exception) {
                BlueshiftLogger.d(TAG, "Could not get page size from system property: ${e.message}")
            }
            
            // Default to 4KB if unable to determine
            cachedPageSize = PAGE_SIZE_4KB
            BlueshiftLogger.d(TAG, "Using default page size: $cachedPageSize bytes")
            return cachedPageSize!!
        }
    }
    
    /**
     * Check if the device is using 16KB page size.
     * @return true if using 16KB pages, false otherwise
     */
    @JvmStatic
    private fun is16KBPageSize(): Boolean = getPageSize() == PAGE_SIZE_16KB
    
    /**
     * Log page size information for debugging purposes.
     * This helps with troubleshooting 16KB page size compatibility issues.
     * 
     * @param context Application context
     */
    @JvmStatic
    fun logPageSizeInfo(context: Context) {
        val pageSize = getPageSize()
        BlueshiftLogger.d(TAG, "Device page size: $pageSize bytes")
        
        when {
            is16KBPageSize() -> {
                BlueshiftLogger.i(TAG, "Running on 16KB page size device")
            }
            pageSize == PAGE_SIZE_4KB -> {
                BlueshiftLogger.d(TAG, "Running on 4KB page size device")
            }
            else -> {
                BlueshiftLogger.w(TAG, "Unknown page size detected: $pageSize bytes")
            }
        }
        
        // Log basic memory info for debugging
        val runtime = Runtime.getRuntime()
        val maxMemoryMB = runtime.maxMemory() / (1024 * 1024)
        BlueshiftLogger.d(TAG, "Max memory available: ${maxMemoryMB} MB")
    }
}