package com.blueshift.blueshiftinboxdemo

import android.os.Bundle
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import com.blueshift.Blueshift
import com.blueshift.inbox.BlueshiftInboxCallback
import com.blueshift.inbox.BlueshiftInboxFragment
import com.blueshift.inbox.BlueshiftInboxManager
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    private var mBottomNavView: BottomNavigationView? = null
    private var mFragmentContainerView: FrameLayout? = null
    private var mInboxFragment = BlueshiftInboxFragment.newInstance()
    private var mHomeFragment = HomeFragment.newInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mBottomNavView = findViewById(R.id.bottomNavigationView)
        mFragmentContainerView = findViewById(R.id.fragmentContainerView)

        showHomeFragment()

        mBottomNavView!!.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.menuHome -> showHomeFragment()
                R.id.menuInbox -> showInboxFragment()
                else -> {
                    showHomeFragment()
                }
            }
        }

        Blueshift.getInstance(this).registerForInAppMessages(this)
    }

    private fun showHomeFragment(): Boolean {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragmentContainerView, mHomeFragment)
            .commit()

        return true
    }

    private fun showInboxFragment(): Boolean {
        mInboxFragment?.let {
            supportFragmentManager
                .beginTransaction()
                .replace(R.id.fragmentContainerView, it)
                .commit()
        }

        return true
    }
}