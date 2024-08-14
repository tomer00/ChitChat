package com.tomer.chitchat.ui.activities

import com.tomer.chitchat.utils.ConversionUtils.assetsVM as vmAssets
import com.tomer.chitchat.utils.ConversionUtils.chatVM as vm
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.tomer.chitchat.R
import com.tomer.chitchat.adap.ChatAdapter
import com.tomer.chitchat.adap.EmojiAdapter
import com.tomer.chitchat.databinding.ActivityChatBinding
import com.tomer.chitchat.databinding.MsgItemBinding
import com.tomer.chitchat.modals.msgs.ModelMsgSocket
import com.tomer.chitchat.modals.msgs.NoTyping
import com.tomer.chitchat.modals.msgs.Typing
import com.tomer.chitchat.modals.states.FlowType
import com.tomer.chitchat.modals.states.MsgStatus
import com.tomer.chitchat.modals.states.MsgsFlowState
import com.tomer.chitchat.modals.states.UiMsgModal
import com.tomer.chitchat.room.MsgMediaType
import com.tomer.chitchat.ui.views.MsgSwipeCon
import com.tomer.chitchat.ui.views.MsgSwipeCon.SwipeCA
import com.tomer.chitchat.utils.ConversionUtils
import com.tomer.chitchat.utils.EmojisHashingUtils
import com.tomer.chitchat.utils.Utils
import com.tomer.chitchat.utils.Utils.Companion.getDpLink
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.LinkedList


@SuppressLint("CheckResult")
@AndroidEntryPoint
class ChatActivity : AppCompatActivity(), ChatAdapter.ChatViewEvents, SwipeCA {

    private val b by lazy { ActivityChatBinding.inflate(layoutInflater) }

    private lateinit var adap: ChatAdapter
    private val emojiAdap by lazy {
        EmojiAdapter {
            sendTextMessage(EmojisHashingUtils.emojiList[it], true)
            b.replyLayout.visibility = View.GONE
            b.rvEmojiContainer.animate()
                .x(b.rvEmojiContainer.width.toFloat())
                .setDuration(220)
                .start()
        }
    }

    private lateinit var ll: LinearLayoutManager

    private val timeVisibilityQueue = LinkedList<Pair<Long, Long>>()

    private val options by lazy {
        RequestOptions().apply {
            transform(RoundedCorners(12))
            override(60)
        }
    }
    private val roundOptions by lazy {
        RequestOptions().apply {
            transform(RoundedCorners(60))
            override(60)
        }
    }

    //region TYPING TIMER

    private var lastSeenMillis = -1L

    private var isTimer: Boolean = false
    private var timer: CountDownTimer = object : CountDownTimer(2000, 2000) {
        override fun onTick(l: Long) {
        }

        override fun onFinish() {
            vm.sendMsg(NoTyping())
            isTimer = false
        }
    }
    //endregion TYPING TIMER


    //region MEDIA IO

    private var pickingMediaType = MsgMediaType.IMAGE

    private val mediaPicker: ActivityResultLauncher<PickVisualMediaRequest> =
        registerForActivityResult(
            ActivityResultContracts.PickVisualMedia()
        ) {
            b.btGallery.isClickable = true
            if (it != null) {
                sendMediaMsg(it)
            }
        }

    //endregion MEDIA IO


    private var currentReplyMsgData: UiMsgModal? = null

    override fun onResume() {
        super.onResume()
        vm.isChatActivityVisible = true
    }

    override fun onPause() {
        super.onPause()
        vm.isChatActivityVisible = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!intent.hasExtra("phone")) {
            finish()
            return
        }
        window.statusBarColor = ContextCompat.getColor(this, R.color.softBg)
        setContentView(b.root)

        vm.openChat(intent.getStringExtra("phone")!!)
        //QUEUE SERVICE EXECUTOR
        lifecycleScope.launch {
            while (true) {
                delay(200)
                var removeCount = 0
                if (timeVisibilityQueue.isEmpty())
                    continue

                for (i in timeVisibilityQueue.indices) {
                    if (timeVisibilityQueue[i].first < System.currentTimeMillis()) {
                        val b = getRvViewIfPossibleForId(timeVisibilityQueue[i].second)
                        b?.contTime?.visibility = View.GONE
                        removeCount++
                    }
                }
                while (removeCount-- != 0)
                    timeVisibilityQueue.removeFirst()
            }
        }

        b.etMsg.setKeyboardInputCall { info ->
            if (b.contRelation.visibility == View.VISIBLE) return@setKeyboardInputCall
            pickingMediaType = if (info.description.getMimeType(0).equals("image/gif")) MsgMediaType.GIF else MsgMediaType.IMAGE
            sendMediaMsg(info.contentUri)
        }

        adap = ChatAdapter(this, this, vm.chatMsgs)
        b.rvMsg.adapter = adap

        ll = LinearLayoutManager(this)
        val iT = ItemTouchHelper(MsgSwipeCon(this, this, vm.chatMsgs))
        iT.attachToRecyclerView(b.rvMsg)
        b.rvMsg.setLayoutManager(ll)
        adap.registerAdapterDataObserver(
            object : RecyclerView.AdapterDataObserver() {
                override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                    super.onItemRangeInserted(positionStart, itemCount)
                    b.rvMsg.smoothScrollToPosition(0)
                }
            }
        )
        ll.reverseLayout = true

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
        b.btImg.setOnClickListener {
            if (b.contRelation.visibility == View.VISIBLE) return@setOnClickListener
            if (emojiAdap.currentList.isEmpty()) {
                b.rvEmojiContainer.x = b.rvEmojiContainer.width.toFloat()
                b.rvEmoji.adapter = emojiAdap
                b.rvEmoji.setLayoutManager(GridLayoutManager(this, 3, GridLayoutManager.HORIZONTAL, false))
                emojiAdap.submitList(EmojisHashingUtils.emojiList)
            }
            if (b.rvEmojiContainer.x.toInt() > 0) {
                b.rvEmojiContainer.animate()
                    .x(0F)
                    .setDuration(220)
                    .setInterpolator(OvershootInterpolator(1.2f))
                    .start();

                return@setOnClickListener
            }

            b.rvEmojiContainer.animate()
                .x(b.rvEmojiContainer.width.toFloat())
                .setDuration(220)
                .start()
        }
        b.btSend.setOnClickListener {
            if (b.contRelation.visibility == View.VISIBLE) return@setOnClickListener
            b.btSend.playAnimation()
            val msgText = b.etMsg.text.toString().trim().ifEmpty { return@setOnClickListener }

            sendTextMessage(msgText, isOnlyEmoji(msgText))
            b.etMsg.setText("")
            Handler().postDelayed({ b.etMsg.requestFocus() }, 40)
            b.replyLayout.visibility = View.GONE
        }
        b.btCloseReplyLay.setOnClickListener {
            b.replyLayout.visibility = View.GONE
            Handler().postDelayed({ b.etMsg.requestFocus() }, 40)
        }
        b.btGallery.setOnClickListener {
            if (b.contRelation.visibility == View.VISIBLE) return@setOnClickListener
            b.btGallery.isClickable = false
            pickingMediaType = MsgMediaType.IMAGE
            mediaPicker.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }

        b.apply {
            btBack.setOnClickListener {
                super.onBackPressed()
            }
            tvPartnerName.text = Utils.currentPartner!!.partnerName.ifEmpty { Utils.currentPartner!!.partnerId }
            if (Utils.currentPartner!!.isAccepted)
                vm.showLastSeen()

            Glide.with(this@ChatActivity)
                .asBitmap()
                .apply(roundOptions)
                .load(Utils.currentPartner!!.partnerId.getDpLink())
                .placeholder(R.drawable.def_avatar)
                .error(R.drawable.def_avatar)
                .into(imgDp)

            if (Utils.currentPartner!!.isAccepted) return@apply

            contRelation.visibility = View.VISIBLE
            Glide.with(this@ChatActivity)
                .asBitmap()
                .circleCrop()
                .load(Utils.currentPartner!!.partnerId.getDpLink())
                .into(imgDpCard)
            tvPartnerNameCard.text = Utils.currentPartner!!.partnerName.ifEmpty { Utils.currentPartner!!.partnerId }

            if (Utils.currentPartner!!.isConnSent) {

                if (Utils.currentPartner!!.isRejected)
                    "Your request is rejected by user\nDo you want to send request again?"
                        .also { tvStatusCard.text = it }
                else "Your request not Accepted by user\nDo you want to send request again?"
                    .also { tvStatusCard.text = it }
                btNeg.visibility = View.GONE

                btPositive.setOnClickListener {
                    vm.genKeyAndSendNotification(Utils.currentPartner!!)
                }
                return@apply
            }

            if (Utils.currentPartner!!.isRejected && Utils.currentPartner!!.isConnSent) {
                return@apply
            }
            "sending you connection request\nDo you also want to connect?"
                .also { tvStatusCard.text = it }
            btPositive.setOnClickListener {
                vm.acceptConnection(true)
                contRelation.visibility = View.GONE
            }
            btNeg.setOnClickListener {
                vm.acceptConnection(false)
                contRelation.visibility = View.GONE
                finish()
            }
        }

        lifecycleScope.launch {
            vmAssets.flowEvents.collectLatest {
                handleFlow(it)
            }
        }
        lifecycleScope.launch {
            vm.flowMsgs.collectLatest {
                handleFlow(it)
            }
        }
        b.bigJson.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                b.bigJson.animate().scaleX(0f).scaleY(0f).setDuration(320).setInterpolator(AccelerateInterpolator()).start()
            }
        })

        vmAssets.getGifNow()

    }

    private fun sendTextMessage(text: String, isOnlyEmoji: Boolean) {
        val msgB = ModelMsgSocket.Builder()
        msgB.isReply(b.replyLayout.visibility == View.VISIBLE)
        msgB.setTimeMillis(System.currentTimeMillis())
        if (b.replyLayout.visibility == View.VISIBLE) {
            msgB.replyId(currentReplyMsgData!!.id)
            msgB.replyMsgType(currentReplyMsgData!!.msgType)
            msgB.replyData(currentReplyMsgData!!.msg)
            msgB.replyMediaFileName(currentReplyMsgData!!.mediaFileName.toString())
        }
        msgB.msgType(if (isOnlyEmoji) MsgMediaType.EMOJI else MsgMediaType.TEXT)
        msgB.msgData(text)
        vm.sendChatMsg(
            msgB.build(),
            if (b.replyLayout.visibility == View.VISIBLE) currentReplyMsgData!!.bytes else null
        )
    }

    private fun sendMediaMsg(uri: Uri) {
        val tempId = vm.getTempId()
        val msgB = ModelMsgSocket.Builder()
        msgB.isReply(b.replyLayout.visibility == View.VISIBLE)
        msgB.setTimeMillis(System.currentTimeMillis())
        msgB.msgType(pickingMediaType)
        if (b.replyLayout.visibility == View.VISIBLE) {
            msgB.replyId(currentReplyMsgData!!.id)
            msgB.replyMsgType(currentReplyMsgData!!.msgType)
            msgB.replyData(currentReplyMsgData!!.msg)
            msgB.replyMediaFileName(currentReplyMsgData!!.mediaFileName.toString())
        }
        msgB.msgData("Uploading")
        vmAssets.uploadFile(
            Utils.currentPartner?.partnerId ?: "0000000000", pickingMediaType, uri, this, msgB.build(), tempId,
            if (b.replyLayout.visibility == View.VISIBLE) currentReplyMsgData!!.bytes else null
        ) { msg ->
            runOnUiThread {
                adap.addItem(msg.data.also { it?.isUploaded = true } ?: return@runOnUiThread)
            }
        }
        b.replyLayout.visibility = View.GONE
        vm.updatePersonModel(msgB.build(), tempId)
    }

    private fun handleMsgStatusAnimation(serverRec: Boolean, id: Long?) {
        val index = vm.chatMsgs.indexOfFirst { id == it.id }
        vm.chatMsgs[index].status = if (serverRec) MsgStatus.SENT_TO_SERVER else MsgStatus.RECEIVED
        val b = getRvViewIfVisible(index) ?: return
        val animDur = 200L
        lifecycleScope.launch {
            delay(animDur)
            b.imgMsgStatus.setImageDrawable(ContextCompat.getDrawable(this@ChatActivity, if (serverRec) R.drawable.ic_tick else R.drawable.ic_double_tick))
            b.imgMsgStatus.animate().rotationYBy(-180f).setInterpolator(LinearInterpolator()).setDuration(animDur).start()
        }
        b.imgMsgStatus.animate().rotationYBy(180f).setInterpolator(LinearInterpolator()).setDuration(animDur).start()
    }

    //region FLOW EVENTS

    private fun handleFlow(msg: MsgsFlowState) {
        Log.d("TAG--", "handleFlow: CAHT ACTVITz $msg")
        if (msg.fromUser != Utils.currentPartner!!.partnerId) return
        when (msg.type) {
            FlowType.MSG -> {
                val msgL = msg.data ?: return
                if (msgL.msgType != MsgMediaType.TEXT && msgL.msgType != MsgMediaType.EMOJI)
                    vmAssets.downLoadFile(msgL.msg.split(",-,")[0], msgL.mediaFileName!!, msgL.msgType, msgL.id, Utils.currentPartner!!.partnerId) {
                        msgL.isDownloaded = true
                        msgL.isProg = false
                        msgL.bytes = it
                    }
                adap.addItem(msgL)

                if (msg.data.isSent) return
                if (msg.data.msgType != MsgMediaType.EMOJI) return


                val nameGoogleJson = EmojisHashingUtils.googleJHash[ConversionUtils.encode(msgL.msg)]
                if (!nameGoogleJson.isNullOrEmpty()) {
                    vmAssets.showGoogleJsonViaFlow(nameGoogleJson)
                    return
                }

                val nameJson = EmojisHashingUtils.jHash[ConversionUtils.encode(msgL.msg)]
                if (!nameJson.isNullOrEmpty()) {
                    vmAssets.showJsonViaFlow(nameJson)
                    return
                }

                val nameGif = EmojisHashingUtils.gHash[ConversionUtils.encode(msgL.msg)]
                if (!nameGif.isNullOrEmpty()) {
                    vmAssets.showGifViaFlow(nameGif)
                    return
                }

                val nameTeleGif = EmojisHashingUtils.teleHash[ConversionUtils.encode(msgL.msg)]
                if (!nameTeleGif.isNullOrEmpty()) {
                    vmAssets.showTeleGifViaFlow(nameTeleGif)
                    return
                }
            }

            FlowType.UPLOAD_SUCCESS -> {
                runOnUiThread {
                    val msgT = msg.data ?: return@runOnUiThread
                    val builder = ModelMsgSocket.Builder()

                    builder.replyId(msgT.replyId)
                    builder.msgData(msgT.msg)
                    builder.replyData(msgT.rep)
                    builder.msgType(msgT.msgType)
                    builder.replyMsgType(msgT.replyType)
                    builder.isReply(msgT.isReply)
                    builder.setTimeMillis(msg.msgId!!)
                    builder.replyMediaFileName(msgT.replyMediaFileName)
                    builder.mediaFileName(msgT.mediaFileName)

                    vm.sendMediaUploaded(builder.build(), msgT.id, msg.fromUser)
                    vm.updatePersonModel(builder.build(), msgT.id)
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msgT.id) {
                            vm.chatMsgs[i].isUploaded = true
                            vm.chatMsgs[i].isProg = false
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            animateTo0(b.rvProg)
                            break
                        }
                    }
                }
            }

            FlowType.UPLOAD_FAILS -> {
                runOnUiThread {
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msg.msgId) {
                            vm.chatMsgs[i].isUploaded = false
                            vm.chatMsgs[i].isProg = false
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread

                            animateTo0(b.rvProg)
                            animateTo1(b.btRet)
                            break
                        }
                    }
                }
            }

            FlowType.DOWNLOAD_SUCCESS -> {
                runOnUiThread {
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msg.msgId) {
                            vm.chatMsgs[i].isDownloaded = true
                            vm.chatMsgs[i].isProg = false
                            vm.chatMsgs[i].bytes = msg.data!!.bytes
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            animateTo0(b.rvProg)
                            Glide.with(this).load(msg.data.bytes)
                                .transition(DrawableTransitionOptions.withCrossFade())
                                .transform(RoundedCorners(12))
                                .into(b.mediaImg)
                            break
                        }
                    }
                }
            }

            FlowType.DOWNLOAD_FAILS -> {
                runOnUiThread {
                    for (i in vm.chatMsgs.indices) {
                        if (vm.chatMsgs[i].id == msg.msgId) {
                            vm.chatMsgs[i].isDownloaded = false
                            vm.chatMsgs[i].isProg = false
                            val b = getRvViewIfVisible(i) ?: return@runOnUiThread
                            b.rvProg.animate().apply {
                                scaleX(0f)
                                scaleY(0f)
                                setDuration(200)
                                start()
                            }
                            animateTo0(b.rvProg)
                            animateTo1(b.btDRet)
                            break
                        }
                    }
                }
            }


            FlowType.SERVER_REC -> {
                handleMsgStatusAnimation(true, msg.msgId)
            }

            FlowType.PARTNER_REC -> {
                handleMsgStatusAnimation(false, msg.msgId)
                timeVisibilityQueue.addLast(Pair(System.currentTimeMillis() + 2000, msg.msgId ?: -1L))
            }

            FlowType.TYPING -> "Typing...".also { b.tvDetails.text = it }

            FlowType.NO_TYPING -> {
                if (lastSeenMillis == -1L) "Online".also { b.tvDetails.text = it }
                else "last seen today at ${ConversionUtils.getRelativeTime(lastSeenMillis)}".also { b.tvDetails.text = it }
            }

            FlowType.ONLINE -> {
                "Online".also { b.tvDetails.text = it }
                lastSeenMillis = -1L
            }

            FlowType.OFFLINE -> {
                lastSeenMillis = msg.msgId!!
                val relTimeText = ConversionUtils.getRelativeTime(lastSeenMillis)
                if (relTimeText.contains(':'))
                    "last seen today at $relTimeText".also { b.tvDetails.text = it }
                else if (relTimeText == "Yesterday")
                    "last seen yesterday at ${ConversionUtils.millisToTimeText(lastSeenMillis)}".also { b.tvDetails.text = it }
                else "last seen on $relTimeText".also { b.tvDetails.text = it }
            }

            FlowType.RELOAD_RV -> {
                adap.notifyDataSetChanged()
            }

            FlowType.SEND_NEW_CONNECTION_REQUEST -> {
                vm.connectNew(msg.fromUser, false)
                finish()
            }

            FlowType.REQ_ACCEPTED -> {
                runOnUiThread {
                    b.contRelation.visibility = View.GONE
                    vm.openChat(msg.fromUser)
                }
            }

            FlowType.INCOMING_NEW_CONNECTION_REQUEST -> {
                runOnUiThread {
                    b.apply {
                        contRelation.visibility = View.VISIBLE
                        Glide.with(this@ChatActivity)
                            .asBitmap()
                            .circleCrop()
                            .load(Utils.currentPartner!!.partnerId.getDpLink())
                            .into(imgDpCard)
                        tvPartnerNameCard.text = Utils.currentPartner!!.partnerName.ifEmpty { Utils.currentPartner!!.partnerId }

                        "sending you connection request\nDo you also want to connect?"
                            .also { tvStatusCard.text = it }
                        btPositive.setOnClickListener {
                            vm.acceptConnection(true)
                            contRelation.visibility = View.GONE
                        }
                        btNeg.visibility = View.VISIBLE
                        btNeg.setOnClickListener {
                            vm.acceptConnection(false)
                            contRelation.visibility = View.GONE
                            finish()
                        }
                    }
                }
            }

            FlowType.CHANGE_GIF -> {
                runOnUiThread {
                    b.tCard.animate().apply {
                        scaleY(0f)
                        scaleX(0f)
                        setDuration(120)
                        start()
                    }
                    b.tCard.animate().scaleY(1f).scaleX(1f).setStartDelay(120).setDuration(120).start()
                    lifecycleScope.launch {
                        delay(120)
                        if (msg.data!!.msg.isEmpty()) return@launch
                        b.btImg.setAnimationFromJson(msg.data.msg, msg.data.mediaFileName)
                        b.btImg.playAnimation()
                    }
                }
            }

            FlowType.SHOW_BIG_JSON -> {
                runOnUiThread {
                    b.bigJson.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(AccelerateInterpolator()).start()
                    b.bigJson.setAnimationFromJson(msg.data!!.msg, msg.data.mediaFileName)
                    b.bigJson.playAnimation()
                }

            }

            FlowType.SHOW_BIG_GIF -> {
                runOnUiThread {
                    b.bigJson.animate().scaleX(1f).scaleY(1f).setDuration(140).setInterpolator(AccelerateInterpolator()).start()
                    Glide.with(this).load(msg.fileGif!!).skipMemoryCache(true).into(b.bigJson)
                    lifecycleScope.launch {
                        delay(3600)
                        b.bigJson.animate().scaleX(0f).scaleY(0f).setDuration(320).setInterpolator(AccelerateInterpolator()).start()
                        b.bigJson.postDelayed({ Glide.with(this@ChatActivity).clear(b.bigJson) }, 340)
                    }
                }
            }

            else -> {}
        }
    }

    //endregion FLOW EVENTS

    private fun getRvViewIfVisible(pos: Int): MsgItemBinding? {
        val view = ll.findViewByPosition(pos) ?: return null
        return MsgItemBinding.bind(view)
    }

    private fun getRvViewIfPossibleForId(msgId: Long): MsgItemBinding? {
        for (i in vm.chatMsgs.indices) {
            if (vm.chatMsgs[i].id == msgId) {
                val view = ll.findViewByPosition(i) ?: return null
                return MsgItemBinding.bind(view)
            }
        }
        return null
    }

    private fun isOnlyEmoji(text: String): Boolean {
        return (EmojisHashingUtils.googleJHash.containsKey(ConversionUtils.encode(text))
                || EmojisHashingUtils.jHash.containsKey(ConversionUtils.encode(text))
                || EmojisHashingUtils.gHash.containsKey(ConversionUtils.encode(text))
                || EmojisHashingUtils.teleHash.containsKey(ConversionUtils.encode(text)))
    }

    private fun startTimer() {
        if (!isTimer) vm.sendMsg(Typing())

        timer.cancel()
        timer.start()
        isTimer = true
    }

    //region CLICK LISTENERS

    override fun onChatItemClicked(pos: Int) {
        val b = getRvViewIfVisible(pos) ?: return
        if (vm.chatMsgs[pos].status != MsgStatus.RECEIVED) return
        b.contTime.visibility = View.VISIBLE
        timeVisibilityQueue.removeIf { it.second == vm.chatMsgs[pos].id }
        timeVisibilityQueue.addLast(Pair(System.currentTimeMillis() + 2000, vm.chatMsgs[pos].id))
    }

    override fun onChatItemLongClicked(pos: Int) {
        //todo DELETING MSGS
    }


    private fun animateTo0(v: View) {
        v.animate().apply {
            scaleX(0f)
            scaleY(0f)
            setDuration(200)
            start()
        }
        v.isClickable = false
    }

    private fun animateTo1(v: View) {
        v.animate().apply {
            scaleX(1f)
            scaleY(1f)
            setStartDelay(200)
            setDuration(200)
            start()
        }
        v.isClickable = true
    }

    override fun onChatItemDownloadClicked(pos: Int) {
        val mod = vm.chatMsgs[pos]
        vmAssets.downLoadFile(mod.msg.split(",-,")[0], mod.mediaFileName!!, mod.msgType, mod.id, Utils.currentPartner!!.partnerId) {
            runOnUiThread {
                vm.chatMsgs[pos].isDownloaded = true
                vm.chatMsgs[pos].isProg = false
                vm.chatMsgs[pos].bytes = it
                val b = getRvViewIfVisible(pos) ?: return@runOnUiThread
                animateTo0(b.rvProg)
                Glide.with(this).load(it)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .transform(RoundedCorners(12))
                    .into(b.mediaImg)
            }
        }
        val b = getRvViewIfVisible(pos) ?: return

        animateTo0(b.btDRet)
        animateTo1(b.rvProg)
    }

    override fun onChatItemUploadClicked(pos: Int) {
        vmAssets.uploadRetry(vm.chatMsgs[pos], Utils.currentPartner!!.partnerId)
        val b = getRvViewIfVisible(pos) ?: return

        animateTo0(b.btRet)
        animateTo1(b.rvProg)
    }

    override fun showRep(position: Int) {
        currentReplyMsgData = vm.chatMsgs[position]
        val mod = currentReplyMsgData ?: return
        b.apply {
            replyLayout.visibility = View.VISIBLE
            when (mod.msgType) {
                MsgMediaType.TEXT, MsgMediaType.EMOJI -> {
                    tvRep.text = mod.msg
                    imgReplyMedia.visibility = View.GONE
                }

                MsgMediaType.IMAGE, MsgMediaType.GIF, MsgMediaType.VIDEO -> {
                    imgReplyMedia.visibility = View.VISIBLE
                    Glide.with(tvPartnerNameCard).load(mod.bytes).apply(options).into(imgReplyMedia)
                    tvRep.text = mod.mediaFileName
                }

                MsgMediaType.FILE -> {
                    imgReplyMedia.visibility = View.VISIBLE
                    Glide.with(tvPartnerNameCard).load(R.drawable.ic_received).apply(options).into(imgReplyMedia)
                    tvRep.text = mod.mediaFileName
                }
            }
        }


    }
    //endregion CLICK LISTENERS

}
