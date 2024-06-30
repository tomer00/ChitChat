package com.tomer.chitchat.ui.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.ChatAdapter
import com.tomer.chitchat.adap.EmojiAdapter
import com.tomer.chitchat.databinding.ActivityChatBinding
import com.tomer.chitchat.modals.msgs.NoTyping
import com.tomer.chitchat.modals.msgs.Typing
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.ui.views.MsgSwipeCon
import com.tomer.chitchat.ui.views.MsgSwipeCon.SwipeCA
import com.tomer.chitchat.viewmodals.ChatViewModal
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


@SuppressLint("CheckResult")
@AndroidEntryPoint
class ChatActivity : AppCompatActivity(), ChatAdapter.ChatViewEvents, SwipeCA {

    private val b by lazy { ActivityChatBinding.inflate(layoutInflater) }
    private val vm by viewModels<ChatViewModal>()

    private lateinit var adap: ChatAdapter
    private val emojiAdap by lazy {
        EmojiAdapter {

        }
    }
    private lateinit var ll: LinearLayoutManager


    private val options by lazy {
        RequestOptions().apply {
            placeholder(R.drawable.round_image_24)
            transform( RoundedCorners (12))
            override(60)
        }
    }
    private val roundOptions by lazy {
        RequestOptions().apply {
            placeholder(R.drawable.ic_received)
            transform(RoundedCorners (60))
            override(60)
        }
    }
    private val msgQue: List<String> = ArrayList(0)
    private val imgs: List<String> = ArrayList(4)
    private val oldMsgs: List<String> = ArrayList(4)

    //    private val gifsAva: List<GifModal> = ArrayList<GifModal>(4)
    private val emoAva: List<String> = ArrayList(4)
    private val isCalled = false
    private var dofinish: kotlin.Boolean = true
    private var isContext: kotlin.Boolean = true
    private val currentRepByte: ByteArray? = null
    private val currentRepLink: String? = null

    private val byhF: ByteArray = ByteArray(0)
    private var byBF: kotlin.ByteArray = ByteArray(0)
    private var byLoad: kotlin.ByteArray = ByteArray(0)

    private var isTimer: Boolean = false
    private var isConn: Boolean = false
    private var timer: CountDownTimer = object : CountDownTimer(2000, 2000) {
        override fun onTick(l: Long) {
        }

        override fun onFinish() {
            if (!isConn) return
            vm.sendMsg(NoTyping())
            isTimer = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = ContextCompat.getColor(this, R.color.softBg)
        setContentView(b.root)



        adap = ChatAdapter(this, this, vm.chatMsgs)
        b.rvMsg.adapter = adap
        b.rvEmoji.adapter = emojiAdap

        ll = LinearLayoutManager(this)
        val iT = ItemTouchHelper(MsgSwipeCon(this, this))
        iT.attachToRecyclerView(b.rvMsg)
        b.rvMsg.setLayoutManager(ll)


        b.etMsg.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                startTimer()
                b.btAnimHelper.visibility = if (s.isEmpty()) View.VISIBLE else View.GONE
            }

            override fun afterTextChanged(s: Editable?) {
            }
        })

        Glide.with(this)
            .asBitmap()
            .apply(roundOptions)
            .load("https://picsum.photos/200")
            .into(b.imgDp)

        lifecycleScope.launch {
            vm.flowMsgs.collectLatest {
                when (it.type) {
                    FlowType.MSG -> adap.addItem(it.data!!)

                    FlowType.UPLOAD_SUCCESS -> {

                    }

                    FlowType.UPLOAD_FAILS -> {}
                    FlowType.DOWNLOAD_SUCCESS -> {}
                    FlowType.DOWNLOAD_FAILS -> {}
                }
            }
        }
    }


    private fun startTimer() {
        if (!isConn) return
        if (!isTimer) vm.sendMsg(Typing())

        timer.cancel()
        timer.start()
        isTimer = true
    }

    //region CLICK LISTENERS

    override fun onClick(pos: Int) {

    }

    override fun showRep(position: Int) {

    }
    //endregion CLICK LISTENERS
}
