package com.xd.xdmanager

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.xd.xdmanager.base.BaseActivity
import com.xd.xdmanager.databinding.ActivityChatBinding
import com.xd.xdmanager.model.StatusParser.Companion.toChineseSeat
import com.xd.xdmanager.rtc.ImsCallback
import com.xd.xdmanager.rtc.MessageType
import com.xd.xdmanager.rtc.SignalServerManager
import org.java_websocket.WebSocket
import org.jetbrains.anko.doAsync
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import java.util.*

class ChatActivity : BaseActivity<ActivityChatBinding>(), ImsCallback {
    private val VIDEO_RESOLUTION_WIDTH = 2880
    private val VIDEO_RESOLUTION_HEIGHT = 1840
    private val VIDEO_FPS = 30

    // Opengl es
    private var mRootEglBase: EglBase? = null

    // 纹理渲染
    private var mSurfaceTextureHelper: SurfaceTextureHelper? = null


    // 音视频数据
    val VIDEO_TRACK_ID = "1" //"ARDAMSv0";

    val AUDIO_TRACK_ID = "2" //"ARDAMSa0";

    private var mVideoTrack: VideoTrack? = null
    private var mAudioTrack: AudioTrack? = null

    // 视频采集
    private var mVideoCapturer: VideoCapturer? = null

    //用于数据传输
    private var mPeerConnection: PeerConnection? = null
    private var mPeerConnectionFactory: PeerConnectionFactory? = null

    //判断是否拨出通话
    private var mIsOutgoing = false

    //拨打通话对象和来电的对象
    var mCallFrom: String? = null

    //被呼叫的用户
    private var mCallTo: String? = null
    private var mSdpInfo: String? = null

    private var mType: Int = 0 // 0->优先后置  1->优先usb

    private var mCallFromConnect: WebSocket? = null

    private var mCallToConnect: WebSocket? = null

    //是否处于通话状态
    var isInCalling = false

//    fun openActivity(
//        context: Context, isOutgoing: Boolean, callFrom: String?, callTo: String?, sdpInfo: String?
//    ) {
//        val intent = Intent(context, ChatActivity::class.java)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        intent.putExtra("isOutgoing", isOutgoing)
//        intent.putExtra("callFrom", callFrom)
//        intent.putExtra("callTo", callTo)
//        intent.putExtra("sdpInfo", sdpInfo)
//        context.startActivity(intent)
//    }

    companion object {
        @JvmStatic
        fun openActivity(context: Context, isOutgoing: Boolean, callFrom: String?, callTo: String?, mSdpInfo: String?, type: Int = 0) {
            val intent = Intent(context, ChatActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("isOutgoing", isOutgoing)
            intent.putExtra("callFrom", callFrom)
            intent.putExtra("callTo", callTo)
            intent.putExtra("sdpInfo", mSdpInfo)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityChatBinding = ActivityChatBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        SignalServerManager.INSTANCE(this).registerImsCallback(this)

        val intent = intent
        mIsOutgoing = intent.getBooleanExtra("isOutgoing", false)
        mCallFrom = intent.getStringExtra("callFrom")
        mCallTo = intent.getStringExtra("callTo")
        mSdpInfo = intent.getStringExtra("sdpInfo")
        mType = intent.getIntExtra(("type"), 0)

        binding.tvChatTitle.setText("正在与 ${mCallTo?.toChineseSeat()} 进行视频通话")

        binding.hangupImageView.setOnClickListener { hangUp("user cancel") }
        mRootEglBase = EglBase.create()
        mRootEglBase?.let {
            binding.LocalSurfaceView.init(it.getEglBaseContext(), null)
            binding.LocalSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            binding.LocalSurfaceView.setMirror(true)
            binding.LocalSurfaceView.setEnableHardwareScaler(false /* enabled */)
            binding.LocalSurfaceView.setZOrderMediaOverlay(true) // 注意这句，因为2个surfaceview是叠加的


            binding.RemoteSurfaceView.init(it.getEglBaseContext(), null)
            binding.RemoteSurfaceView.setScalingType(RendererCommon.ScalingType.SCALE_ASPECT_FILL)
            binding.RemoteSurfaceView.setMirror(true)
            binding.RemoteSurfaceView.setEnableHardwareScaler(true /* enabled */)

            // 创建PC factory , PC就是从factory里面获取的
            mPeerConnectionFactory = createPeerConnectionFactory(this)

            // NOTE: this _must_ happen while PeerConnectionFactory is alive!
            Logging.enableLogToDebugOutput(Logging.Severity.LS_WARNING)

            // 创建视频采集器
            mVideoCapturer = createVideoCapturer()

            mSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", it.getEglBaseContext())
            val videoSource = mPeerConnectionFactory!!.createVideoSource(false)
            mVideoCapturer!!.initialize(mSurfaceTextureHelper, applicationContext, videoSource.capturerObserver)

            mVideoTrack = mPeerConnectionFactory!!.createVideoTrack(VIDEO_TRACK_ID, videoSource)
            mVideoTrack!!.setEnabled(true)
            mVideoTrack!!.addSink(binding.LocalSurfaceView) // 设置渲染到本地surfaceview上
        }


        //AudioSource 和 AudioTrack 与VideoSource和VideoTrack相似，只是不需要AudioCapturer 来获取麦克风，
        val audioSource = mPeerConnectionFactory!!.createAudioSource(MediaConstraints())
        mAudioTrack = mPeerConnectionFactory!!.createAudioTrack(AUDIO_TRACK_ID, audioSource)
        mAudioTrack!!.setEnabled(true)
        if (mIsOutgoing) {
            mCallToConnect = SignalServerManager.INSTANCE(this).getClientById(mCallTo)
            mCallToConnect?.let {
                doStartCall(it)
            }
        } else {
            mCallFromConnect = SignalServerManager.INSTANCE(this).getClientById(mCallFrom)
            onRemoteOfferReceived(mSdpInfo!!)
        }
        isInCalling = true
    }

    override fun onResume() {
        super.onResume()
        // 开始采集并本地显示
        mVideoCapturer!!.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS)
    }

    override fun onPause() {
        super.onPause()
        try {
            // 停止采集
            mVideoCapturer!!.stopCapture()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        doAsync {
            if (isInCalling) {
                isInCalling = false
                hangUp("abnormal quit")
            }
            doLeave()
            binding.LocalSurfaceView.release()
            binding.RemoteSurfaceView.release()
            mVideoCapturer!!.dispose()
            mSurfaceTextureHelper!!.dispose()
            PeerConnectionFactory.stopInternalTracingCapture()
            PeerConnectionFactory.shutdownInternalTracer()
            mPeerConnectionFactory!!.dispose()
        }
        SignalServerManager.INSTANCE(this).unRegisterImsConnectCallBack(this)
    }

    override fun refeshClent() {
    }

    override fun onRemoteAnswerReceived(message: JSONObject?) {
        printInfoOnScreen("Receive Remote Answer ...")
        try {
            val description = message!!.getString("sdp")
            mPeerConnection!!.setRemoteDescription(
                SimpleSdpObserver(), SessionDescription(
                    SessionDescription.Type.ANSWER, description
                )
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        printInfoOnScreen("收到answer.....")
        updateCallState(false)
    }

    override fun onRemoteCandidateReceived(message: JSONObject?) {
        printInfoOnScreen("Receive Remote Candidate ...")
        try {
            // candidate 候选者描述信息
            // sdpMid 与候选者相关的媒体流的识别标签
            // sdpMLineIndex 在SDP中m=的索引值
            // usernameFragment 包括了远端的唯一识别
            val remoteIceCandidate = IceCandidate(
                message!!.getString("id"), message.getInt("label"), message.getString("candidate")
            )
            printInfoOnScreen("收到Candidate.....")
            mPeerConnection!!.addIceCandidate(remoteIceCandidate)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onHangup(reason: String?) {
        printInfoOnScreen("onHangup ...reason：$reason")
        finish()
    }

    private fun hangUp(reason: String) {
        doAsync {
            val message = JSONObject()
            try {
                message.put("type", MessageType.HANGUP.getId())
                message.put("reason", reason)
                sendMessage(message.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        isInCalling = false
        finish()
    }

    private fun createVideoCapturer(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(this)) {
            createCameraCapturer(Camera2Enumerator(this))
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

    fun createPeerConnectionFactory(context: Context?): PeerConnectionFactory? {
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = DefaultVideoEncoderFactory(
            mRootEglBase!!.eglBaseContext, false /* enableIntelVp8Encoder */, true
        )
        decoderFactory = DefaultVideoDecoderFactory(mRootEglBase!!.eglBaseContext)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context).setEnableInternalTracer(true).createInitializationOptions()
        )
        val builder = PeerConnectionFactory.builder().setVideoEncoderFactory(encoderFactory).setVideoDecoderFactory(decoderFactory)
        builder.setOptions(null)
        return builder.createPeerConnectionFactory()
    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames
        Log.d("ChatActivity", "Looking for other cameras.")
        for (deviceName in deviceNames) {
            if (!enumerator.isBackFacing(deviceName)) {
                Log.d("ChatActivity", "Creating other camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        // First, try to find front facing camera
        Log.d("ChatActivity", "Looking for front facing cameras.")
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                Log.d("ChatActivity", "Creating front facing camera capturer.")
                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
                if (videoCapturer != null) {
                    return videoCapturer
                }
            }
        }
        // Front facing camera not found, try something else
        return null
    }

    /**
     * 呼叫client用户
     */
    fun doStartCall(conn: WebSocket) {
        printInfoOnScreen("Start Call, Wait ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        if (mPeerConnection == null) {
            Log.e("ChatActivity", "Create mPeerConnection failed")
            finish()
            return
        }
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")) // 接收远端音频
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")) // 接收远端视频
        mediaConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        mPeerConnection!!.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d("ChatActivity", """  Create local offer success:  ${sessionDescription.description}  """.trimIndent())
                mPeerConnection!!.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("type", MessageType.OFFER.id)
                    message.put("sdp", sessionDescription.description)
                    message.put("youxian", mType)
                    conn.send(message.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, mediaConstraints)
    }

    private fun printInfoOnScreen(msg: String) {
        Log.d("ChatActivity", msg)
//        runOnUiThread {
//            val output: String = binding.LogcatView.getText().toString() + "\n" + msg
//            binding.LogcatView.setText(output)
//        }
    }

    fun createPeerConnection(): PeerConnection? {
        Log.d("ChatActivity", "Create PeerConnection ...")
        val iceServers = LinkedList<IceServer>()

        // 设置ICE服务器
        val ice_server = IceServer.builder("turn:xxxx:3478").setPassword("xxx").setUsername("xxx").createIceServer()
        iceServers.add(ice_server)
        val rtcConfig = RTCConfiguration(iceServers)
        // TCP candidates are only useful when connecting to a server that supports
        // ICE-TCP.
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED // 不要使用TCP
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE // max-bundle表示音视频都绑定到同一个传输通道
        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE // 只收集RTCP和RTP复用的ICE候选者，如果RTCP不能复用，就失败
        rtcConfig.continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY
        //rtcConfig.iceCandidatePoolSize = 10;
        rtcConfig.iceTransportsType = PeerConnection.IceTransportsType.ALL

        // Use ECDSA encryption.
        //rtcConfig.keyType = PeerConnection.KeyType.ECDSA;
        // Enable DTLS for normal calls and disable for loopback calls.
        rtcConfig.enableDtlsSrtp = true
        //rtcConfig.sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN;
        val connection: PeerConnection = mPeerConnectionFactory!!.createPeerConnection(
            rtcConfig, mPeerConnectionObserver
        )!! // PC的observer

        if (connection == null) {
            Log.d("ChatActivity", "Failed to createPeerConnection !")
            return null
        }
        val mediaStreamLabels = listOf("ARDAMS")
        connection.addTrack(mVideoTrack, mediaStreamLabels)
        connection.addTrack(mAudioTrack, mediaStreamLabels)
        return connection
    }

    private val mPeerConnectionObserver: PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onSignalingChange(signalingState: SignalingState) {
            Log.d("ChatActivity", "onSignalingChange: $signalingState")
        }

        override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
            Log.d("ChatActivity", "onIceConnectionChange: $iceConnectionState")
        }

        override fun onIceConnectionReceivingChange(b: Boolean) {
            Log.d("ChatActivity", "onIceConnectionChange: $b")
        }

        override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
            Log.d("ChatActivity", "onIceGatheringChange: $iceGatheringState")
        }

        override fun onIceCandidate(iceCandidate: IceCandidate) {
            Log.d("ChatActivity", "onIceCandidate: $iceCandidate")
            // 得到candidate，就发送给信令服务器
            doAsync {
                try {
                    val message = JSONObject()
                    //message.put("userId", RTCWebRTCSignalClient.getInstance().getUserId());
                    message.put("type", MessageType.ICE_CANDIDATE.id)
                    message.put("label", iceCandidate.sdpMLineIndex)
                    message.put("id", iceCandidate.sdpMid)
                    message.put("candidate", iceCandidate.sdp)
                    sendMessage(message)
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }

        }

        override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
            for (i in iceCandidates.indices) {
                Log.d("ChatActivity", "onIceCandidatesRemoved: " + iceCandidates[i])
            }
            mPeerConnection!!.removeIceCandidates(iceCandidates)
        }

        override fun onAddStream(mediaStream: MediaStream) {
            Log.d("ChatActivity", "onAddStream: " + mediaStream.videoTracks.size)
        }

        override fun onRemoveStream(mediaStream: MediaStream) {
            Log.d("ChatActivity", "onRemoveStream")
        }

        override fun onDataChannel(dataChannel: DataChannel) {
            Log.d("ChatActivity", "onDataChannel")
        }

        override fun onRenegotiationNeeded() {
            Log.d("ChatActivity", "onRenegotiationNeeded")
        }

        // 收到了媒体流
        override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
            val track = rtpReceiver.track()
            if (track is VideoTrack) {
                Log.d("ChatActivity", "onAddVideoTrack")
                val remoteVideoTrack = track
                remoteVideoTrack.setEnabled(true)
                remoteVideoTrack.addSink(binding.RemoteSurfaceView)
            }
        }
    }

    /**
     * 接收到学生的视频通话请求
     * @param description
     */
    fun onRemoteOfferReceived(description: String) {
        printInfoOnScreen("Receive Remote Call ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        mPeerConnection?.setRemoteDescription(SimpleSdpObserver(), SessionDescription(SessionDescription.Type.OFFER, description))
        printInfoOnScreen("收到offer...调用doAnswerCall")
        doAnswerCall()
    }

    fun doAnswerCall() {
        printInfoOnScreen("Answer Call, Wait ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        val sdpMediaConstraints = MediaConstraints()
        Log.d("ChatActivity", "Create answer ...")
        mPeerConnection!!.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Log.d("ChatActivity", "Create answer success !")
                mPeerConnection!!.setLocalDescription(
                    SimpleSdpObserver(), sessionDescription
                )
                val message = JSONObject()
                try {
                    message.put("type", MessageType.ANSWER.id)
                    message.put("sdp", sessionDescription.description)
                    sendMessage(message.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, sdpMediaConstraints)
        updateCallState(false)
    }

    private fun updateCallState(idle: Boolean) {
        runOnUiThread {
            if (idle) {
                binding.RemoteSurfaceView.setVisibility(View.GONE)
            } else {
                binding.RemoteSurfaceView.setVisibility(View.VISIBLE)
            }
        }
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            Log.d("ChatActivity", "SdpObserver: onCreateSuccess !")
        }

        override fun onSetSuccess() {
            Log.d("ChatActivity", "SdpObserver: onSetSuccess")
        }

        override fun onCreateFailure(msg: String) {
            Log.d("ChatActivity", "SdpObserver onCreateFailure: $msg")
        }

        override fun onSetFailure(msg: String) {
            Log.d("ChatActivity", "SdpObserver onSetFailure: $msg")
        }
    }

    private fun sendMessage(message: JSONObject) {
        if (mIsOutgoing) {
            SignalServerManager.INSTANCE(this).sendMessage(mCallTo, message.toString())
        } else {
            SignalServerManager.INSTANCE(this).sendMessage(mCallFrom, message.toString())
        }
    }

    private fun sendMessage(message: String) {
        if (mIsOutgoing) {
            SignalServerManager.INSTANCE(this).sendMessage(mCallTo, message)
        } else {
            SignalServerManager.INSTANCE(this).sendMessage(mCallFrom, message)
        }
    }

    fun doLeave() {
        doAsync {
            printInfoOnScreen("Leave room, Wait ...")
            printInfoOnScreen("Hangup Call, Wait ...")
            if (mPeerConnection != null) {
                mPeerConnection!!.close()
                mPeerConnection = null
                printInfoOnScreen("Hangup Done.")
                updateCallState(true)
            }

        }
    }

    private fun onRemoteHangup() {
        printInfoOnScreen("Receive Remote Hangup Event ...")
        doLeave()
    }

}