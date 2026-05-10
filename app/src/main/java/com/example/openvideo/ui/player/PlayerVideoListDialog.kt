package com.example.openvideo.ui.player

import android.app.Dialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager.LayoutParams
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.openvideo.R
import com.example.openvideo.data.model.VideoItem

/**
 * 横屏播放页左侧滑出的「当前列表」面板，视觉与播放器设置面板一致。
 */
class PlayerVideoListDialog(
    context: Context,
    private val videos: List<VideoItem>,
    private val playingVideoId: Long,
    private val onPick: (VideoItem) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_player_video_list)
        setCanceledOnTouchOutside(true)

        window?.apply {
            val width = context.resources.displayMetrics.widthPixels
            val height = context.resources.displayMetrics.heightPixels
            val density = context.resources.displayMetrics.density
            val portrait = height > width
            if (portrait) {
                setLayout(LayoutParams.MATCH_PARENT, (height * 0.56f).toInt())
                setGravity(Gravity.BOTTOM)
                attributes = attributes.apply {
                    x = 0
                    y = 0
                }
            } else {
                val bounds = PlayerSettingsLayoutPolicy.panelBounds(width, height, density)
                setLayout(bounds.width, bounds.height)
                setGravity(Gravity.START or Gravity.CENTER_VERTICAL)
                attributes = attributes.apply {
                    x = PlayerSettingsLayoutPolicy.landscapeMarginPx(width, height, density)
                }
            }
            applyWindowBackdrop()
            setBackgroundDrawableResource(android.R.color.transparent)
            decorView.setPadding(0, 0, 0, 0)
            decorView.elevation = dp(20).toFloat()
        }

        val recycler = findViewById<RecyclerView>(R.id.player_video_list_recycler)
        recycler.layoutManager = LinearLayoutManager(context)
        recycler.adapter = SessionVideoAdapter(videos, playingVideoId) { item ->
            onPick(item)
            dismiss()
        }

        val idx = videos.indexOfFirst { it.id == playingVideoId }.coerceAtLeast(0)
        recycler.scrollToPosition(idx)
    }

    override fun onStart() {
        super.onStart()
        applyWindowBackdrop()
    }

    private fun applyWindowBackdrop() {
        window?.apply {
            setDimAmount(0.12f)
            addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setBackgroundBlurRadius(dp(18))
            }
        }
    }

    private fun dp(value: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            value.toFloat(),
            context.resources.displayMetrics
        ).toInt()

    private class SessionVideoAdapter(
        private val items: List<VideoItem>,
        private val playingId: Long,
        private val onClick: (VideoItem) -> Unit
    ) : RecyclerView.Adapter<SessionVideoAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val text: TextView = itemView.findViewById(R.id.tv_title)
            val thumb: ImageView = itemView.findViewById(R.id.iv_thumbnail)

            init {
                itemView.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) onClick(items[pos])
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_player_session_video, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.text.text = item.title.ifBlank { item.path }
            val playing = item.id == playingId
            val ctx = holder.text.context
            holder.text.setTextColor(
                ContextCompat.getColor(
                    ctx,
                    if (playing) R.color.player_accent else R.color.player_title_normal
                )
            )
            Glide.with(holder.thumb)
                .load(item.thumbnailUri)
                .centerCrop()
                .placeholder(R.drawable.ic_movie)
                .into(holder.thumb)
        }

        override fun getItemCount(): Int = items.size
    }
}
