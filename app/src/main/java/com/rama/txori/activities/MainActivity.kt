package com.rama.txori.activities

import android.app.Fragment
import android.app.FragmentManager
import android.os.Bundle
import android.view.View
import com.rama.txori.CsActivity
import com.rama.txori.R
import com.rama.txori.managers.FontManager
import com.rama.txori.widgets.WdNavbar

class MainActivity : CsActivity() {

    private lateinit var navbar: WdNavbar
    private var currentPage: WdNavbar.Page = WdNavbar.Page.HOME

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.root)
        applyEdgeToEdgePadding(root)
        applyFont(root)

        navbar = findViewById(R.id.navbar)
        navbar.onNavigate = { page -> navigateTo(page) }

        if (savedInstanceState == null) {
            navigateTo(WdNavbar.Page.HOME)
            findViewById<View>(R.id.root).post {
                FontManager.applyToView(this, findViewById(R.id.root))
            }
        } else {
            currentPage = savedInstanceState
                .getString(KEY_PAGE)
                ?.let { WdNavbar.Page.valueOf(it) }
                ?: WdNavbar.Page.HOME
            navbar.setActivePage(currentPage)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_PAGE, currentPage.name)
    }
    
    fun navigateTo(page: WdNavbar.Page) {
        if (page == currentPage && fragmentManager.findFragmentById(R.id.content_container) != null) return

        currentPage = page
        navbar.setActivePage(page)

        val fragment: Fragment = when (page) {
            WdNavbar.Page.HOME -> HomeFragment()
            WdNavbar.Page.STOPWATCH -> StopwatchFragment()
            WdNavbar.Page.TIMER -> TimerFragment()
            WdNavbar.Page.ABOUT -> AboutFragment()
        }

        fragmentManager
            .beginTransaction()
            .replace(R.id.content_container, fragment, page.name)
            .commit()

        // Re-apply font after fragment views are attached
        fragmentManager.executePendingTransactions()
        val root = findViewById<View>(R.id.root)
        FontManager.applyToView(this, root)
    }

    /** Let HomeFragment show/hide the navbar during a running workout. */
    fun setNavbarVisible(visible: Boolean) {
        navbar.visibility = if (visible) View.VISIBLE else View.GONE
    }

    companion object {
        private const val KEY_PAGE = "current_page"
    }
}
