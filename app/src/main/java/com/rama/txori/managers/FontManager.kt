package com.rama.txori.managers

import android.content.Context
import android.graphics.Typeface
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import com.rama.txori.R

object FontManager {

    private var cached: Typeface? = null

    /** Returns the app typeface, loading it once and caching it forever. */
    fun getTypeface(context: Context): Typeface {
        cached?.let { return it }
        val tf = try {
            ResourcesCompat.getFont(context.applicationContext, R.font.jersey25_regular)
        } catch (e: Exception) {
            null
        } ?: Typeface.createFromAsset(context.assets, "fonts/jersey25_regular.otf")
        cached = tf
        return tf
    }

    /**
     * Walk [root] and set the typeface on every TextView found in the tree.
     * Safe to call multiple times — just resets the same typeface.
     */
    fun applyToView(context: Context, root: View) {
        applyRecursively(root, getTypeface(context))
    }

    /**
     * Re-apply the font to every currently visible child of a ListView.
     * Call this from the adapter's notifyDataSetChanged path (or after
     * the list has been populated) so recycled rows get the font too.
     */
    fun applyToListView(context: Context, listView: ListView) {
        val tf = getTypeface(context)
        for (i in 0 until listView.childCount) {
            applyRecursively(listView.getChildAt(i), tf)
        }
    }

    private fun applyRecursively(view: View, typeface: Typeface) {
        if (view is TextView) {
            view.typeface = typeface
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyRecursively(view.getChildAt(i), typeface)
            }
        }
    }
}
