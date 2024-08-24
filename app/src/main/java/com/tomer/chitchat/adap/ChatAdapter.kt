package com.tomer.chitchat.adap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.MsgItemBinding
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.room.MsgMediaType
import java.util.LinkedList


class ChatAdapter(
    private val callBack: ChatViewEvents,
    private val context: Context,
    private val chatItems: LinkedList<UiMsgModal>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {


    @SuppressLint("CheckResult")
    private val options = RequestOptions().apply {
        placeholder(R.drawable.ic_gifs)
        override(400)
        error(R.drawable.ic_search)
        transform(RoundedCorners(12))
    }

    private val statusDrawables: List<Drawable> = listOf(
        ContextCompat.getDrawable(context, R.drawable.ic_sending)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_tick)!!,
        ContextCompat.getDrawable(context, R.drawable.ic_double_tick)!!,
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
        ChatViewHolder(MsgItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), callBack)


    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {

        val params = holder.b.msgLay.layoutParams as ConstraintLayout.LayoutParams
        val mod = chatItems[position]
        if (mod.isSent) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.horizontalBias = 1f
            holder.b.msgLay.setLayoutParams(params)
            holder.b.innerLay.gravity = Gravity.END
            holder.b.msgLay.gravity = Gravity.END
            holder.b.innerLay.background = ContextCompat.getDrawable(context, R.drawable.msg_sent)
            holder.b.RepTv.setGravity(Gravity.END)
            holder.b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.fore))
            holder.b.contTime.gravity = Gravity.END
            holder.b.imgMsgStatus.visibility = View.VISIBLE
        } else {
            params.horizontalBias = 0f
            holder.b.msgLay.setLayoutParams(params)
            holder.b.innerLay.gravity = Gravity.START
            holder.b.msgLay.gravity = Gravity.START
            holder.b.innerLay.background = ContextCompat.getDrawable(context, R.drawable.msg_rec)
            holder.b.RepTv.setGravity(Gravity.START)
            holder.b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.b.contTime.gravity = Gravity.START
            holder.b.imgMsgStatus.visibility = View.GONE
        }

        if (mod.isSelected) holder.b.root.setBackgroundColor(ContextCompat.getColor(context,R.color.selected))
        else holder.b.root.setBackgroundColor(ContextCompat.getColor(context,R.color.trans))

        holder.b.tvTime.text = mod.timeText
        holder.b.imgMsgStatus.setImageDrawable(statusDrawables[mod.status.ordinal])
        holder.b.contTime.visibility = View.VISIBLE
        if (mod.status.ordinal == 2) holder.b.contTime.visibility = View.GONE

        holder.b.RepTv.visibility = if (mod.isReply) View.VISIBLE else View.GONE

        if (mod.isReply && (mod.replyType == MsgMediaType.GIF || mod.replyType == MsgMediaType.IMAGE)) {
            holder.b.repImgRv.setVisibility(View.VISIBLE)
            holder.b.RepTv.visibility = View.GONE
            Glide.with(context)
                .asBitmap()
                .load(mod.repBytes)
                .apply(options)
                .override(80)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.b.repImgRv)
        } else holder.b.repImgRv.setVisibility(View.GONE)


        if (mod.bytes == null) {
            holder.b.mediaCont.visibility = View.GONE
            if (mod.isEmojiOnly) {
                holder.b.innerLay.background = ColorDrawable(ContextCompat.getColor(context, R.color.trans))
                holder.b.msgTv.visibility = View.GONE
                holder.b.emojiTv.visibility = View.VISIBLE
            } else {
                holder.b.msgTv.visibility = View.VISIBLE
                holder.b.emojiTv.visibility = View.GONE
            }
        } else {
            //There is Media File either upload or download
            holder.b.mediaCont.visibility = View.VISIBLE
            if (mod.msgType == MsgMediaType.GIF) Glide.with(context).load(mod.bytes).apply(options).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(holder.b.mediaImg)
            else Glide.with(context).asBitmap().load(mod.bytes).apply(options).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(holder.b.mediaImg)

            holder.b.msgTv.visibility = View.GONE
            holder.b.emojiTv.visibility = View.GONE


            if (mod.isProg) {
                holder.b.rvProg.visibility = View.VISIBLE
                holder.b.layMediaRoot.visibility = View.VISIBLE
                holder.b.layUpload.visibility = View.GONE
                holder.b.layDownload.visibility = View.GONE
            } else {
                holder.b.rvProg.visibility = View.GONE
                holder.b.layMediaRoot.visibility = View.VISIBLE
                if (mod.isSent) {//uploading
                    holder.b.layDownload.visibility = View.GONE
                    if (mod.isUploaded) holder.b.layMediaRoot.visibility = View.GONE
                    else holder.b.layUpload.visibility = View.VISIBLE

                } else {//downloading
                    holder.b.layUpload.visibility = View.GONE
                    if (mod.isDownloaded) holder.b.layMediaRoot.visibility = View.GONE
                    else {
                        holder.b.layDownload.visibility = View.VISIBLE
                        holder.b.tvDownBytes.text = mod.mediaSize
                    }
                }
            }

        }

        holder.b.msgTv.text = mod.msg
        holder.b.emojiTv.text = mod.msg
        holder.b.RepTv.text = mod.rep

    }

    override fun getItemCount(): Int = chatItems.size

    inner class ChatViewHolder(val b: MsgItemBinding, private val callBack: ChatViewEvents) : RecyclerView.ViewHolder(b.root) {
        private val onCli = View.OnClickListener {
            when (it.id) {
                b.root.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.ROOT)
                b.layUpload.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.UPLOAD)
                b.layDownload.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.DOWNLOAD)
                b.repImgRv.id, b.RepTv.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.REPLY)
                b.mediaImg.id -> callBack.onImageClick(absoluteAdapterPosition, b.mediaImg)
            }
        }

        init {
            b.root.setOnClickListener(onCli)
            b.layUpload.setOnClickListener(onCli)
            b.layDownload.setOnClickListener(onCli)
            b.root.setOnLongClickListener {
                callBack.onChatItemLongClicked(absoluteAdapterPosition)
                true
            }
        }
    }

    //region COMMU

    fun addItem(msg: UiMsgModal) {
        chatItems.addFirst(msg)
        notifyItemInserted(0)
    }

    //endregion COMMU


    enum class ClickEvents {
        DOWNLOAD, UPLOAD, REPLY, ROOT
    }

    interface ChatViewEvents {
        fun onChatItemClicked(pos: Int, type: ClickEvents)
        fun onChatItemLongClicked(pos: Int)

        fun onImageClick(pos: Int, imageView: View)
    }
}