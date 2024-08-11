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
            holder.b.mediaCont.visibility = View.VISIBLE
            if (mod.msgType == MsgMediaType.GIF) Glide.with(context).load(mod.bytes).apply(options).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(holder.b.mediaImg)
            else Glide.with(context).asBitmap().load(mod.bytes).apply(options).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(holder.b.mediaImg)

            holder.b.msgTv.visibility = View.GONE
            holder.b.emojiTv.visibility = View.GONE

            if (mod.isUploaded) setScale0(holder.b.btRet)
            else setScale1(holder.b.btRet)

            if (mod.isProg) setScale1(holder.b.rvProg)
            else setScale0(holder.b.rvProg)

            if (mod.isDownloaded) setScale0(holder.b.btDRet)
            else setScale1(holder.b.btDRet)
        }

        holder.b.msgTv.text = mod.msg
        holder.b.emojiTv.text = mod.msg
        holder.b.RepTv.text = mod.rep

    }

    private fun setScale1(view: View) {
        view.scaleX = 1f
        view.scaleY = 1f
        view.isClickable = true
    }

    private fun setScale0(view: View) {
        view.scaleX = 0f
        view.scaleY = 0f
        view.isClickable = false
    }

    override fun getItemCount(): Int = chatItems.size

    inner class ChatViewHolder(val b: MsgItemBinding, private val callBack: ChatViewEvents) : RecyclerView.ViewHolder(b.root) {
        private val onCli = View.OnClickListener {
            when (it.id) {
                b.btRet.id -> callBack.onChatItemUploadClicked(absoluteAdapterPosition)
                b.btDRet.id -> callBack.onChatItemDownloadClicked(absoluteAdapterPosition)
                b.root.id -> callBack.onChatItemClicked(absoluteAdapterPosition)
            }
        }

        init {
            b.root.setOnClickListener(onCli)
            b.btDRet.setOnClickListener(onCli)
            b.btRet.setOnClickListener(onCli)
            b.root.setOnLongClickListener {
                callBack.onChatItemClicked(absoluteAdapterPosition)
                true
            }
        }


    }

    //region COMMU

    fun addItem(msg: UiMsgModal) {
        chatItems.add(0, msg)
        notifyItemInserted(0)
    }

    //endregion COMMU


    interface ChatViewEvents {
        fun onChatItemClicked(pos: Int)
        fun onChatItemLongClicked(pos: Int)

        fun onChatItemDownloadClicked(pos: Int)
        fun onChatItemUploadClicked(pos: Int)
    }
}