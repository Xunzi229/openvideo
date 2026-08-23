package com.example.openvideo.core.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.openvideo.R

object LibraryNavigator {
    fun push(from: Fragment, target: Fragment, name: String? = null) {
        from.libraryFragmentManager().beginTransaction()
            .setReorderingAllowed(true)
            .setCustomAnimations(
                R.anim.ov_slide_in_right,
                R.anim.ov_slide_out_left,
                R.anim.ov_slide_in_left,
                R.anim.ov_slide_out_right
            )
            .hide(from)
            .add(from.libraryContainerId(), target)
            .addToBackStack(name)
            .commit()
    }

    fun pop(from: Fragment): Boolean {
        val manager = from.libraryFragmentManager()
        if (manager.backStackEntryCount == 0) return false
        manager.popBackStack()
        return true
    }

    fun findTabHost(from: Fragment): LibraryTabHostFragment? {
        var parent = from.parentFragment
        while (parent != null) {
            if (parent is LibraryTabHostFragment) return parent
            parent = parent.parentFragment
        }
        return null
    }
}

fun Fragment.libraryFragmentManager(): FragmentManager =
    LibraryNavigator.findTabHost(this)?.childFragmentManager ?: parentFragmentManager

fun Fragment.libraryContainerId(): Int =
    if (LibraryNavigator.findTabHost(this) != null) {
        R.id.tab_child_container
    } else {
        R.id.fragment_container
    }
