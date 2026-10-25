package com.openvideo.app.ui.local

import android.app.Application
import android.content.res.Configuration
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.openvideo.app.R
import com.openvideo.app.core.prefs.LocalContinuePlaybackPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class LocalContinuePlaybackLayoutTest {
    @Test
    fun lastFolderCanScrollFullyAboveBottomCardAcrossScreenSizesFontsAndColumns() {
        for ((width, height) in listOf(720 to 1280, 1280 to 720)) {
            for (fontScale in listOf(1f, 1.8f)) {
                for (columns in listOf(1, 2)) {
                    val root = createLayout(fontScale, columns)
                    val recycler = root.findViewById<RecyclerView>(R.id.recycler_folders)
                    val card = root.findViewById<View>(R.id.row_continue_playback)
                    LocalContinuePlaybackLayout.apply(root, LocalContinuePlaybackPosition.BOTTOM)
                    layout(root, width, height)
                    recycler.scrollToPosition(30)
                    layout(root, width, height)
                    recycler.scrollBy(0, Int.MAX_VALUE)
                    layout(root, width, height)

                    val last = recycler.findViewHolderForAdapterPosition(30)?.itemView
                    assertNotNull("The last folder must be reachable", last)
                    assertTrue("The card must reserve real space", card.height > 0)
                    assertTrue("The list must end above the card", recycler.bottom <= card.top)
                    assertTrue("The whole last folder must clear the card", recycler.top + last!!.bottom <= card.top)
                    assertTrue("The last folder must fit in the visible list", last.top >= 0)
                    assertTrue("The list must reach its end", !recycler.canScrollVertically(1))
                }
            }
        }
    }

    @Test
    fun changingBottomBackToTopRestoresTheOriginalLayout() {
        val root = createLayout()
        val recycler = root.findViewById<RecyclerView>(R.id.recycler_folders)
        val card = root.findViewById<View>(R.id.row_continue_playback)
        repeat(2) {
            LocalContinuePlaybackLayout.apply(root, LocalContinuePlaybackPosition.BOTTOM)
            layout(root, 720, 1280)
            assertTrue(recycler.bottom <= card.top)
            LocalContinuePlaybackLayout.apply(root, LocalContinuePlaybackPosition.TOP)
            layout(root, 720, 1280)
            assertTrue(card.bottom <= recycler.top)
            assertEquals(root.height, recycler.bottom)
        }
    }

    @Test
    fun hidingBottomCardReturnsItsSpaceToTheListAndShowingItReservesSpaceAgain() {
        val root = createLayout()
        val recycler = root.findViewById<RecyclerView>(R.id.recycler_folders)
        val card = root.findViewById<View>(R.id.row_continue_playback)
        LocalContinuePlaybackLayout.apply(root, LocalContinuePlaybackPosition.BOTTOM)
        card.visibility = View.GONE
        layout(root, 720, 1280)
        assertEquals(root.height, recycler.bottom)
        card.visibility = View.VISIBLE
        layout(root, 720, 1280)
        assertTrue(recycler.bottom <= card.top)
    }

    private fun createLayout(fontScale: Float = 1f, columns: Int = 1): ConstraintLayout {
        val app = RuntimeEnvironment.getApplication()
        val configuration = Configuration(app.resources.configuration).apply { this.fontScale = fontScale }
        val context = ContextThemeWrapper(app.createConfigurationContext(configuration), R.style.Theme_OpenVideo)
        val root = LayoutInflater.from(context).inflate(R.layout.fragment_local_folders, null) as ConstraintLayout
        root.findViewById<View>(R.id.row_continue_playback).visibility = View.VISIBLE
        root.findViewById<TextView>(R.id.tv_continue_title).text = "Continue a previously watched video"
        root.findViewById<RecyclerView>(R.id.recycler_folders).apply {
            layoutManager = GridLayoutManager(context, columns)
            itemAnimator = null
            adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                override fun getItemCount(): Int = 31
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                    val item = TextView(parent.context).apply {
                        layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 88)
                    }
                    return object : RecyclerView.ViewHolder(item) {}
                }
                override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                    (holder.itemView as TextView).text = "Folder $position"
                }
            }
        }
        return root
    }

    private fun layout(root: View, width: Int, height: Int) {
        root.forceLayout()
        root.measure(
            View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        )
        root.layout(0, 0, width, height)
    }
}
