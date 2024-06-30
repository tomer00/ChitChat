package com.tomer.chitchat.adap

import android.annotation.SuppressLint
import android.content.Context
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


class ChatAdapter(
    private val callBack: ChatViewEvents,
    private val context: Context,
    private val chatItems : MutableList<UiMsgModal>
) : RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {



    @SuppressLint("CheckResult")
    private val options = RequestOptions().apply {
        placeholder(R.drawable.ic_gifs)
        error(R.drawable.ic_search)
        transform(RoundedCorners(12))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder =
        ChatViewHolder(MsgItemBinding.inflate(LayoutInflater.from(parent.context), parent, false), callBack)


    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {

        val params = holder.b.msgLay.layoutParams as ConstraintLayout.LayoutParams
        val mod = chatItems[position]
        if (!mod.isSent) {
            params.horizontalBias = 0f
            holder.b.msgLay.setLayoutParams(params)
            holder.b.innerLay.gravity = Gravity.START
            holder.b.msgLay.gravity = Gravity.START
            holder.b.innerLay.background = ContextCompat.getDrawable(context, R.drawable.msg_rec)
            holder.b.RepTv.setGravity(Gravity.START)
            holder.b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.white))
        } else {
            params.startToStart = ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
            params.horizontalBias = 1f
            holder.b.msgLay.setLayoutParams(params)
            holder.b.innerLay.gravity = Gravity.END
            holder.b.msgLay.gravity = Gravity.END
            holder.b.innerLay.background = ContextCompat.getDrawable(context, R.drawable.msg_sent)
            holder.b.RepTv.setGravity(Gravity.END)
            holder.b.msgTv.setTextColor(ContextCompat.getColor(context, R.color.fore))
        }

        if (mod.isRep) holder.b.RepTv.visibility = View.VISIBLE
        else holder.b.RepTv.visibility = View.GONE

        if (mod.isRepMedia) {
            holder.b.repImgRv.setVisibility(View.VISIBLE)
            holder.b.RepTv.visibility = View.GONE
            Glide.with(context)
                .asBitmap()
                .load(mod.repBytes)
                .apply(options)
                .override(60)
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .skipMemoryCache(true)
                .into(holder.b.repImgRv)
        } else holder.b.repImgRv.setVisibility(View.GONE)


        if (mod.bytes == null) {
            holder.b.mediaCont.visibility = View.GONE
            holder.b.msgTv.visibility = View.VISIBLE
        } else {
            holder.b.mediaCont.visibility = View.VISIBLE
            if (mod.isGif) Glide.with(context).asGif().load(mod.bytes).apply(options).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(holder.b.mediaImg)
            else Glide.with(context).asBitmap().load(mod.bytes).apply(options).diskCacheStrategy(DiskCacheStrategy.NONE).skipMemoryCache(true).into(holder.b.mediaImg)

            holder.b.msgTv.visibility = View.GONE

            if (mod.isRetry) setScale1(holder.b.btRet)
            else setScale0(holder.b.btRet)

            if (mod.isDone) setScale0(holder.b.rvProg)
            else setScale1(holder.b.rvProg)

            if (mod.isDDone) setScale0(holder.b.btDRet)
            else setScale1(holder.b.btDRet)
        }

        holder.b.msgTv.text = mod.msg
        holder.b.RepTv.text = mod.rep

    }

    private fun setScale1(view: View) {
        view.scaleX = 1f
        view.scaleY = 1f
    }

    private fun setScale0(view: View) {
        view.scaleX = 0f
        view.scaleY = 0f
    }

    override fun getItemCount(): Int = chatItems.size

    inner class ChatViewHolder(val b: MsgItemBinding, val callBack: ChatViewEvents) : RecyclerView.ViewHolder(b.root) {

    }

    //region COMMU

    fun addItem(msg: UiMsgModal){
        chatItems.add(msg)
        notifyItemInserted(chatItems.size)
    }

    //endregion COMMU


    interface ChatViewEvents {
        fun onClick(pos: Int)
    }
}