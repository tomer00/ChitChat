package com.tomer.chitchat.adap

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tomer.chitchat.R
import com.tomer.chitchat.databinding.RowPersonBinding
import com.tomer.chitchat.modals.rv.PersonModel
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.getDpLink

class AdapPerson(
    private val clickLis: CallbackClick,
    private val context: Context
) :
    ListAdapter<PersonModel, AdapPerson.PersonHolder>(PersonDiff()) {


    private val selCol = ContextCompat.getColor(context, R.color.primary_light)
    private val deSelCol = ContextCompat.getColor(context, R.color.backgroundC)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PersonHolder(RowPersonBinding.inflate(LayoutInflater.from(parent.context), parent, false), clickLis)


    override fun onBindViewHolder(holder: PersonHolder, position: Int) {
        val model = getItem(position)
        holder.b.tvName.text = model.name
        holder.b.tvTime.text = model.lastDate
        holder.b.tvLastMsg.text = model.lastMsg

        if (model.isSelected) {
            holder.b.checkSelection.visibility = View.VISIBLE
            holder.b.root.setBackgroundColor(selCol)
        } else {
            holder.b.checkSelection.visibility = View.GONE
            holder.b.root.setBackgroundColor(deSelCol)
        }

        Glide.with(holder.b.imgProfile)
            .asBitmap()
            .load(model.phoneNo.getDpLink())
            .circleCrop()
            .override(200)
            .into(holder.b.imgProfile)

        if (model.unreadCount > 0) {
            holder.b.tvUnreadMsgCount.visibility = View.VISIBLE
            holder.b.tvUnreadMsgCount.text = model.unreadCount.toString()
            holder.b.tvTime.setTextColor(ContextCompat.getColor(context, R.color.primary))
        } else {
            holder.b.tvUnreadMsgCount.visibility = View.GONE
            holder.b.tvTime.setTextColor(ContextCompat.getColor(context, R.color.fore))
        }

        when (model.mediaType) {
            com.tomer.chitchat.room.MsgMediaType.TEXT -> {
                holder.b.msgType.visibility = View.GONE
            }

            com.tomer.chitchat.room.MsgMediaType.IMAGE -> {
                holder.b.msgType.visibility = View.VISIBLE
                holder.b.msgType.setImageResource(R.drawable.round_image_24)
            }

            com.tomer.chitchat.room.MsgMediaType.GIF -> {
                holder.b.msgType.visibility = View.VISIBLE
            }

            com.tomer.chitchat.room.MsgMediaType.FILE -> {
                holder.b.msgType.visibility = View.VISIBLE
            }

            com.tomer.chitchat.room.MsgMediaType.VIDEO -> {
                holder.b.msgType.visibility = View.VISIBLE
            }
        }
    }

    interface CallbackClick {
        fun onClick(pos: Int)
        fun onLongClick(pos: Int)
    }

    inner class PersonHolder(val b: RowPersonBinding, clickLis: CallbackClick) : RecyclerView.ViewHolder(b.root) {
        init {
            b.root.setOnClickListener {
                clickLis.onClick(absoluteAdapterPosition)
            }
            b.root.setOnLongClickListener {
                clickLis.onLongClick(absoluteAdapterPosition)
                true
            }
        }
    }

    class PersonDiff : DiffUtil.ItemCallback<PersonModel>() {
        override fun areItemsTheSame(oldItem: PersonModel, newItem: PersonModel) =
            oldItem.phoneNo == newItem.phoneNo

        override fun areContentsTheSame(oldItem: PersonModel, newItem: PersonModel) =
            oldItem.lastDate == newItem.lastDate && oldItem.lastMsg == newItem.lastMsg
    }


}