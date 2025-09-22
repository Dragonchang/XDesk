package com.xd.xdcontrol

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import com.xd.xdcontrol.base.BaseActivity
import com.xd.xdcontrol.databinding.ActivityChatBinding
import com.xd.xdcontrol.rtc.ImsCallBack
import com.xd.xdcontrol.rtc.Logger
import com.xd.xdcontrol.rtc.MessageType
import com.xd.xdcontrol.rtc.SignalClientManager
import org.jetbrains.anko.doAsync
import org.json.JSONException
import org.json.JSONObject
import org.webrtc.*
import org.webrtc.PeerConnection.*
import java.util.*

class ChatActivity : BaseActivity<ActivityChatBinding>(), ImsCallBack {
    //判断是否拨出通话
    private var mIsOutgoing = false

    //拨打通话对象和来电的对象
    var mCallFrom: String? = null

    //被呼叫的用户
    private var mCallTo: String? = null

    //off时候老师端的sdp信息
    private var mSdpInfo: String? = null

    //1->优先usb  0->优先后置
    private var mType: Int = 0

    /**
     * ---------和webrtc相关-----------
     */
    // 视频信息
    private val VIDEO_RESOLUTION_WIDTH = 480
    private val VIDEO_RESOLUTION_HEIGHT = 480
    private val VIDEO_RESOLUTION_WIDTH_BIG = 1920
    private val VIDEO_RESOLUTION_HEIGHT_BIG = 1080
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

    //是否处于通话状态
    private var isInCalling = false

    companion object {
        @JvmStatic
        fun openActivity(context: Context, isOutgoing: Boolean, callFrom: String?, callTo: String?, mSdpInfo: String?, type: Int = 0) {
            val intent = Intent(context, ChatActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra("isOutgoing", isOutgoing)
            intent.putExtra("callFrom", callFrom)
            intent.putExtra("callTo", callTo)
            intent.putExtra("mSdpInfo", mSdpInfo)
            intent.putExtra("type", type)
            context.startActivity(intent)
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): ActivityChatBinding = ActivityChatBinding.inflate(layoutInflater)

    override fun loadData() {
    }

    override fun initView(savedInstanceState: Bundle?) {
        Log.e("ZFL", "registerImsCallback")
        SignalClientManager.INSTANCE(this).registerImsCallback(this)
        val intent = intent
        mIsOutgoing = intent.getBooleanExtra("isOutgoing", false)
        mCallFrom = intent.getStringExtra("callFrom")
        mCallTo = intent.getStringExtra("callTo")
        mSdpInfo = intent.getStringExtra("mSdpInfo")
        mType = intent.getIntExtra("type", 0)
        // 用户打印信息
        binding.hangupImageView.setOnClickListener(View.OnClickListener { hangUp("user cancel") })
        mRootEglBase = EglBase.create()

        mRootEglBase?.let {
            // 用于展示本地和远端视频
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

            mSurfaceTextureHelper = SurfaceTextureHelper.create("CaptureThread", it.eglBaseContext)
            val videoSource = mPeerConnectionFactory!!.createVideoSource(false)
            mVideoCapturer!!.initialize(mSurfaceTextureHelper, applicationContext, videoSource.capturerObserver)

            mVideoTrack = mPeerConnectionFactory!!.createVideoTrack(VIDEO_TRACK_ID, videoSource)
            mVideoTrack?.let { mV ->
                mV.setEnabled(true)
                mV.addSink(binding.LocalSurfaceView) // 设置渲染到本地surfaceview上
            }

            //AudioSource 和 AudioTrack 与VideoSource和VideoTrack相似，只是不需要AudioCapturer 来获取麦克风，
            val audioSource = mPeerConnectionFactory!!.createAudioSource(MediaConstraints())
            mAudioTrack = mPeerConnectionFactory!!.createAudioTrack(AUDIO_TRACK_ID, audioSource)
            mAudioTrack?.setEnabled(true)

            if (!mIsOutgoing) {
                //被动接收到老师的通话请求
                onRemoteOfferReceived(mSdpInfo)
            } else {
                doStartCall()
            }
            isInCalling = true
        }

    }

    override fun onResume() {
        super.onResume()
        // 开始采集并本地显示
        if (mType == 0) {
            mVideoCapturer!!.startCapture(VIDEO_RESOLUTION_WIDTH, VIDEO_RESOLUTION_HEIGHT, VIDEO_FPS)
        } else {
            mVideoCapturer!!.startCapture(VIDEO_RESOLUTION_WIDTH_BIG, VIDEO_RESOLUTION_HEIGHT_BIG, VIDEO_FPS)
        }

    }

    override fun onPause() {
        super.onPause()
        try {
            // 停止采集
            mVideoCapturer?.stopCapture()
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

            Log.e("ZFL", "unRegisterImsConnectCallBack")
        }
        SignalClientManager.INSTANCE(this).unRegisterImsConnectCallBack(this)
    }

    open class SimpleSdpObserver : SdpObserver {
        override fun onCreateSuccess(sessionDescription: SessionDescription) {
            Logger.d("SdpObserver: onCreateSuccess !")
        }

        override fun onSetSuccess() {
            Logger.d("SdpObserver: onSetSuccess")
        }

        override fun onCreateFailure(msg: String) {
            Logger.d("SdpObserver onCreateFailure: $msg")
        }

        override fun onSetFailure(msg: String) {
            Logger.d("SdpObserver onSetFailure: $msg")
        }
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

    /**
     * 将学生端的sdp信息发送给老师端
     */
    fun doAnswerCall() {
        printInfoOnScreen("Answer Call, Wait ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        val sdpMediaConstraints = MediaConstraints()
        Logger.d("Create answer ...")
        mPeerConnection!!.createAnswer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Logger.d("Create answer success !")
                mPeerConnection!!.setLocalDescription(
                    SimpleSdpObserver(),
                    sessionDescription
                )
                val message = JSONObject()
                try {
                    message.put("type", MessageType.ANSWER.getId())
                    message.put("sdp", sessionDescription.description)
                    sendMessage(message.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, sdpMediaConstraints)
        updateCallState(false)
    }

    fun doLeave() {
        doAsync {
            printInfoOnScreen("Leave room, Wait ...")
            printInfoOnScreen("Hangup Call, Wait ...")
            if (mPeerConnection != null) {
                mPeerConnection!!.close()
                mPeerConnection = null
            }
            printInfoOnScreen("Hangup Done.")
            updateCallState(true)
        }
    }

    fun createPeerConnection(): PeerConnection? {
        Logger.d("Create PeerConnection ...")
        val iceServers = LinkedList<IceServer>()

        // 设置ICE服务器
        val ice_server = IceServer.builder("turn:xxxx:3478")
            .setPassword("xxx")
            .setUsername("xxx")
            .createIceServer()
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
        val connection: PeerConnection = mPeerConnectionFactory?.createPeerConnection(
            rtcConfig,
            mPeerConnectionObserver
        )!! // PC的observer
        if (connection == null) {
            Logger.d("Failed to createPeerConnection !")
            return null
        }
        val mediaStreamLabels = listOf("ARDAMS")
        connection.addTrack(mVideoTrack, mediaStreamLabels)
        connection.addTrack(mAudioTrack, mediaStreamLabels)
        return connection
    }

    fun createPeerConnectionFactory(context: Context?): PeerConnectionFactory? {
        val encoderFactory: VideoEncoderFactory
        val decoderFactory: VideoDecoderFactory
        encoderFactory = DefaultVideoEncoderFactory(
            mRootEglBase!!.eglBaseContext,
            false /* enableIntelVp8Encoder */,
            true
        )
        decoderFactory = DefaultVideoDecoderFactory(mRootEglBase!!.eglBaseContext)
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions.builder(context)
                .setEnableInternalTracer(true)
                .createInitializationOptions()
        )
        val builder = PeerConnectionFactory.builder()
            .setVideoEncoderFactory(encoderFactory)
            .setVideoDecoderFactory(decoderFactory)
        builder.setOptions(null)
        return builder.createPeerConnectionFactory()
    }

    /*
     * Read more about Camera2 here
     * https://developer.android.com/reference/android/hardware/camera2/package-summary.html
     **/
    private fun createVideoCapturer(): VideoCapturer? {
        return if (Camera2Enumerator.isSupported(this)) {
            createCameraCapturer(Camera2Enumerator(this))
        } else {
            createCameraCapturer(Camera1Enumerator(true))
        }
    }

//    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
//        val deviceNames = enumerator.deviceNames
//
//        // First, try to find front facing camera
//        Logger.d("Looking for front facing cameras.")
//        for (deviceName in deviceNames) {
//            if (enumerator.isBackFacing(deviceName)) {
//                Logger.d("Creating front facing camera capturer.")
//                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
//                if (videoCapturer != null) {
//                    return videoCapturer
//                }
//            }
//        }
//
//        // Front facing camera not found, try something else
//        Logger.d("Looking for other cameras.")
//        for (deviceName in deviceNames) {
//            if (!enumerator.isBackFacing(deviceName)) {
//                Logger.d("Creating other camera capturer.")
//                val videoCapturer: VideoCapturer? = enumerator.createCapturer(deviceName, null)
//                if (videoCapturer != null) {
//                    return videoCapturer
//                }
//            }
//        }
//        return null
//    }

    private fun createCameraCapturer(enumerator: CameraEnumerator): VideoCapturer? {
        val deviceNames = enumerator.deviceNames

        // 调试：打印所有摄像头信息
        deviceNames.forEach { deviceName ->
            val isFront = enumerator.isFrontFacing(deviceName)
            val isBack = enumerator.isBackFacing(deviceName)
            Logger.d("Camera $deviceName - Front: $isFront, Back: $isBack")
        }

        if (mType == 0) { // type0->优先后置摄像头
            // 次选：后置摄像头
            deviceNames.forEach { deviceName ->
                if (enumerator.isBackFacing(deviceName)) {
                    Logger.d("Using back-facing camera: $deviceName")
                    enumerator.createCapturer(deviceName, null)?.let {
                        return it
                    }
                }
            }
        } else if (mType == 1) {
            // 优先寻找外部摄像头（USB）
            if (enumerator is Camera2Enumerator) {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
                deviceNames.forEach { deviceName ->
                    try {
                        // 使用 Android 原生 API 获取摄像头特性
                        val characteristics = cameraManager.getCameraCharacteristics(deviceName)
                        val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)

                        if (lensFacing == CameraCharacteristics.LENS_FACING_EXTERNAL) {
                            Logger.d("Found external USB camera: $deviceName")
                            enumerator.createCapturer(deviceName, null)?.let {
                                return it
                            }
                        }
                    } catch (e: Exception) {
                        Logger.e("Error processing $deviceName: ${e.message}")
                    }
                }
            }
        }

        // 最后选择：其他可用摄像头
        deviceNames.forEach { deviceName ->
            if (!enumerator.isBackFacing(deviceName) && !enumerator.isFrontFacing(deviceName)) {
                Logger.d("Using non-standard camera: $deviceName")
                enumerator.createCapturer(deviceName, null)?.let {
                    return it
                }
            }
        }

        return null
    }

    private val mPeerConnectionObserver: PeerConnection.Observer = object : PeerConnection.Observer {
        override fun onSignalingChange(signalingState: SignalingState) {
            Logger.d("onSignalingChange: $signalingState")
        }

        override fun onIceConnectionChange(iceConnectionState: IceConnectionState) {
            Logger.d("onIceConnectionChange: $iceConnectionState")
        }

        override fun onIceConnectionReceivingChange(b: Boolean) {
            Logger.d("onIceConnectionChange: $b")
        }

        override fun onIceGatheringChange(iceGatheringState: IceGatheringState) {
            Logger.d("onIceGatheringChange: $iceGatheringState")
        }

        override fun onIceCandidate(iceCandidate: IceCandidate) {
            Logger.d("onIceCandidate: $iceCandidate")
            // 得到candidate，就发送给信令服务器
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

        override fun onIceCandidatesRemoved(iceCandidates: Array<IceCandidate>) {
            for (i in iceCandidates.indices) {
                Logger.d("onIceCandidatesRemoved: " + iceCandidates[i])
            }
            mPeerConnection!!.removeIceCandidates(iceCandidates)
        }

        override fun onAddStream(mediaStream: MediaStream) {
            Logger.d("onAddStream: " + mediaStream.videoTracks.size)
        }

        override fun onRemoveStream(mediaStream: MediaStream) {
            Logger.d("onRemoveStream")
        }

        override fun onDataChannel(dataChannel: DataChannel) {
            Logger.d("onDataChannel")
        }

        override fun onRenegotiationNeeded() {
            Logger.d("onRenegotiationNeeded")
        }

        // 收到了媒体流
        override fun onAddTrack(rtpReceiver: RtpReceiver, mediaStreams: Array<MediaStream>) {
            val track = rtpReceiver.track()
            if (track is VideoTrack) {
                Logger.d("onAddVideoTrack")
                val remoteVideoTrack = track
                remoteVideoTrack.setEnabled(true)
                remoteVideoTrack.addSink(binding.RemoteSurfaceView)
            }
        }
    }

    private fun sendMessage(message: JSONObject) {
        SignalClientManager.INSTANCE(this).send(message.toString())
    }

    private fun sendMessage(message: String) {
        SignalClientManager.INSTANCE(this).send(message)
    } //学生接听方，收到offer

    fun onRemoteOfferReceived(description: String?) {
        printInfoOnScreen("Receive Remote Call ...")
        if (description == null) {
            Log.e("ChatSingleActivity", "onRemoteOfferReceived description == null")
            return
        }
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        mPeerConnection!!.setRemoteDescription(
            SimpleSdpObserver(),
            SessionDescription(
                SessionDescription.Type.OFFER,
                description
            )
        )
        printInfoOnScreen("收到offer...调用doAnswerCall")
        doAnswerCall()
    }

    /**
     * 呼叫老师
     */
    fun doStartCall() {
        printInfoOnScreen("Start Call, Wait ...")
        if (mPeerConnection == null) {
            mPeerConnection = createPeerConnection()
        }
        val mediaConstraints = MediaConstraints()
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true")) // 接收远端音频
        mediaConstraints.mandatory.add(MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true")) // 接收远端视频
        mediaConstraints.optional.add(MediaConstraints.KeyValuePair("DtlsSrtpKeyAgreement", "true"))
        mPeerConnection!!.createOffer(object : SimpleSdpObserver() {
            override fun onCreateSuccess(sessionDescription: SessionDescription) {
                Logger.d(
                    """
                    Create local offer success: 
                    ${sessionDescription.description}
                    """.trimIndent()
                )
                mPeerConnection!!.setLocalDescription(SimpleSdpObserver(), sessionDescription)
                val message = JSONObject()
                try {
                    message.put("type", MessageType.OFFER.id)
                    message.put("sdp", sessionDescription.description)
                    message.put("id", mCallFrom)
                    sendMessage(message.toString())
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
            }
        }, mediaConstraints)
    }

    override fun onRemoteCandidateReceived(message: JSONObject?) {
        printInfoOnScreen("Receive Remote Candidate ...")
        try {
            // candidate 候选者描述信息
            // sdpMid 与候选者相关的媒体流的识别标签
            // sdpMLineIndex 在SDP中m=的索引值
            // usernameFragment 包括了远端的唯一识别
            val remoteIceCandidate = IceCandidate(
                message!!.getString("id"),
                message.getInt("label"),
                message.getString("candidate")
            )
            printInfoOnScreen("收到Candidate.....")
            mPeerConnection!!.addIceCandidate(remoteIceCandidate)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    override fun onRemoteAnswerReceived(message: JSONObject?) {
        printInfoOnScreen("Receive Remote Answer ...")
        try {
            val description = message!!.getString("sdp")
            mPeerConnection!!.setRemoteDescription(
                SimpleSdpObserver(),
                SessionDescription(
                    SessionDescription.Type.ANSWER,
                    description
                )
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        printInfoOnScreen("收到answer.....")
        updateCallState(false)
    }

    override fun onHangup(reason: String?) {
        printInfoOnScreen("onHangup ... reason： $reason")
        finish()
    }

    private fun printInfoOnScreen(msg: String) {
        Logger.d(msg)
//        runOnUiThread {
//            val output: String = binding.LogcatView.getText().toString() + "\n" + msg
//            binding.LogcatView.setText(output)
//        }
    }

    private fun hangUp(reason: String) {
        doAsync {
            val message = JSONObject()
            try {
                message.put("type", MessageType.HANGUP.id)
                message.put("reason", reason)
                sendMessage(message.toString())
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
        isInCalling = false
        finish()
    }

}