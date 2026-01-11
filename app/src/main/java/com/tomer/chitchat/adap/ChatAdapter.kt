package com.tomer.chitchat.adap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.Drawable
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.text.style.ForegroundColorSpan
import android.text.style.UnderlineSpan
import android.util.Patterns
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorInt
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
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.qrProvider.GradModel
import java.util.LinkedList
import androidx.core.graphics.toColorInt


class ChatAdapter(
    private val callBack: ChatViewEvents,
    private val context: Context,
    private val chatItems: LinkedList<UiMsgModal>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    private var textSize: Float = 18f
    private var corners: Float = 12f
    private var partnerGrad: GradModel? = null

    @ColorInt
    private var partnerColor: Int = 0

    @ColorInt
    private var myColor = ContextCompat.getColor(context, R.color.softBg)

    @SuppressLint("CheckResult")
    private val options = RequestOptions().apply {
        placeholder(R.drawable.ic_gifs)
        override(400)
        error(R.drawable.logo)
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
        val mod = chatItems[position]
        //check for clickable Spanned
        if (mod.spannableString == null) {
            val spanStr = SpannableString(mod.msg)
            val matcher = Patterns.WEB_URL.matcher(mod.msg)
            while (matcher.find()) {
                val start = matcher.start()
                val end = matcher.end()

                val clickableSpan = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        callBack.onOpenLinkInBrowser(mod.msg.substring(start, end))
                    }
                }
                spanStr.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spanStr.setSpan(UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                spanStr.setSpan(ForegroundColorSpan("#ee0979".toColorInt()), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
            mod.spannableString = spanStr
        }
        holder.b.msgTv.text = mod.spannableString
        holder.b.msgTv.movementMethod = LinkMovementMethod.getInstance()

        val params = holder.b.msgLay.layoutParams as ConstraintLayout.LayoutParams
        if (mod.isSent) {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.horizontalBias = 1f
            holder.b.msgLay.setLayoutParams(params)
            holder.b.innerLay.gravity = Gravity.END
            holder.b.msgLay.gravity = Gravity.END
            holder.b.msgBg.foreground = ContextCompat.getDrawable(context, R.drawable.msg_sent)
            holder.b.RepTv.gravity = Gravity.END
            holder.b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.fore))
            holder.b.contTime.gravity = Gravity.END
            holder.b.imgMsgStatus.visibility = View.VISIBLE
            holder.b.msgBg.setData(mod.isSent, corners, myColor)
        } else {
            params.horizontalBias = 0f
            holder.b.msgLay.setLayoutParams(params)
            holder.b.innerLay.gravity = Gravity.START
            holder.b.msgLay.gravity = Gravity.START
            holder.b.msgBg.foreground = ContextCompat.getDrawable(context, R.drawable.msg_rec)
            holder.b.RepTv.gravity = Gravity.START
            holder.b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.white))
            holder.b.contTime.gravity = Gravity.START
            holder.b.imgMsgStatus.visibility = View.GONE
            if (partnerGrad == null)
                holder.b.msgBg.setData(mod.isSent, corners, partnerColor)
            else holder.b.msgBg.setData(mod.isSent, corners, partnerGrad!!)
        }

        if (mod.isSelected) holder.b.root.setBackgroundColor(ContextCompat.getColor(context, R.color.selected))
        else holder.b.root.setBackgroundColor(ContextCompat.getColor(context, R.color.trans))

        holder.b.emojiTv.text = mod.msg
        holder.b.RepTv.text = mod.rep
        holder.b.tvTime.text = mod.timeText
        holder.b.imgMsgStatus.setImageDrawable(statusDrawables[mod.status.ordinal])
        holder.b.contTime.visibility = View.VISIBLE
        if (mod.status.ordinal == 2) holder.b.contTime.visibility = View.GONE

        holder.b.RepTv.visibility = if (mod.isReply) View.VISIBLE else View.GONE

        if (mod.isReply && (mod.replyType == MsgMediaType.GIF || mod.replyType == MsgMediaType.IMAGE)) {
            holder.b.repImgRv.visibility = View.VISIBLE
            holder.b.RepTv.visibility = View.GONE
            Glide.with(context)
                .asBitmap()
                .load(mod.repBytes)
                .apply(options)
                .override(80)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.b.repImgRv)
        } else {
            holder.b.repImgRv.visibility = View.GONE
            if (mod.replyType == MsgMediaType.FILE)
                holder.b.RepTv.text = mod.replyMediaFileName ?: "FILE"
        }

        holder.b.msgTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        holder.b.RepTv.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize.times(.86f))

        if (mod.bytes == null) {
            holder.b.mediaCont.visibility = View.GONE
            holder.b.imgFileType.visibility = View.GONE
            if (mod.isEmojiOnly) {
                holder.b.msgBg.hideBg()
                holder.b.msgTv.visibility = View.GONE
                holder.b.emojiTv.visibility = View.VISIBLE
            } else {
                holder.b.msgTv.visibility = View.VISIBLE
                holder.b.emojiTv.visibility = View.GONE
            }
        } else {
            //There is Media File either upload or download
            holder.b.mediaCont.visibility = View.VISIBLE
            if (mod.msgType == MsgMediaType.GIF || mod.msgType == MsgMediaType.IMAGE || mod.msgType == MsgMediaType.VIDEO)
                Glide.with(context)
                    .load(mod.bytes)
                    .apply(options)
                    .skipMemoryCache(true)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(holder.b.mediaImg)
                    .also { holder.b.mediaImg.setCorners(corners) }
            else holder.b.mediaImg.setImageDrawable(null)

            if (mod.msgType == MsgMediaType.FILE) {
                holder.b.msgTv.visibility = View.VISIBLE
                holder.b.imgFileType.visibility = View.VISIBLE
                holder.b.imgFileType.setImageDrawable(ContextCompat.getDrawable(context, AdapPerson.getDrawableId(mod.mediaFileName ?: "FILE")))
                "${mod.mediaFileName ?: "File"}\n${mod.mediaSize} • ${Utils.getFileExt(mod.mediaFileName ?: "").uppercase()}".also { holder.b.msgTv.text = it }
            } else {
                holder.b.msgTv.visibility = View.GONE
                holder.b.imgFileType.visibility = View.GONE
            }
            holder.b.emojiTv.visibility = View.GONE


            if (mod.isProg) {
                holder.b.rvProg.visibility = View.VISIBLE
                holder.b.layMediaRoot.visibility = View.VISIBLE
                holder.b.layUpload.visibility = View.GONE
                holder.b.layDownload.visibility = View.GONE
            } else {
                if (mod.isUploaded && mod.isDownloaded) {
                    holder.b.layMediaRoot.visibility = View.GONE
                    return
                }
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

    }

    override fun getItemCount(): Int = chatItems.size

    inner class ChatViewHolder(val b: MsgItemBinding, private val callBack: ChatViewEvents) : RecyclerView.ViewHolder(b.root) {
        private val onCli = View.OnClickListener {
            when (it.id) {
                b.root.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.ROOT)
                b.layUpload.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.UPLOAD)
                b.layDownload.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.DOWNLOAD)
                b.repImgRv.id, b.RepTv.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.REPLY)
                b.msgTv.id, b.imgFileType.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.FILE)
                b.mediaImg.id -> callBack.onChatItemClicked(absoluteAdapterPosition, ClickEvents.IMAGE)
            }
        }

        private val onLongClickLis = View.OnLongClickListener {
            callBack.onChatItemLongClicked(absoluteAdapterPosition)
            true
        }

        init {
            b.root.setOnClickListener(onCli)
            b.layUpload.setOnClickListener(onCli)
            b.layDownload.setOnClickListener(onCli)
            b.repImgRv.setOnClickListener(onCli)
            b.RepTv.setOnClickListener(onCli)
            b.msgTv.setOnClickListener(onCli)
            b.imgFileType.setOnClickListener(onCli)
            b.mediaImg.setOnClickListener(onCli)


            b.root.setOnLongClickListener(onLongClickLis)
            b.mediaImg.setOnLongClickListener(onLongClickLis)
            b.imgFileType.setOnLongClickListener(onLongClickLis)
            b.msgTv.setOnLongClickListener(onLongClickLis)
            b.repImgRv.setOnLongClickListener(onLongClickLis)
            b.RepTv.setOnLongClickListener(onLongClickLis)
        }
    }

    //region COMMU

    fun addItem(msg: UiMsgModal) {
        chatItems.addFirst(msg)
        notifyItemInserted(0)
    }

    fun setValues(textSize: Float, corners: Float, gradModel: GradModel) {
        this.textSize = textSize
        this.corners = corners
        this.partnerGrad = gradModel
    }

    fun setValues(textSize: Float, corners: Float, @ColorInt color: Int) {
        this.textSize = textSize
        this.corners = corners
        this.partnerGrad = null
        this.partnerColor = color
    }

    //endregion COMMU

}