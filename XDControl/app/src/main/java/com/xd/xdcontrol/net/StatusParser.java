package com.xd.xdcontrol.net;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class StatusParser {
    private static volatile StatusParser instance;

    private StatusParser() {
    }

    public static synchronized StatusParser getInstance() {
        if (instance == null) {
            synchronized (StatusParser.class) {
                if (instance == null) {
                    instance = new StatusParser();
                }
            }
        }
        return instance;
    }

    private WorkStatus mWorkStatus = new WorkStatus();
    private ErrorStatus mErrorStatus = new ErrorStatus();
    private volatile float mPosition = 0.0f;
    private PowerStatus mPowerStatus = new PowerStatus();
    private Boolean isOn220AC = false;
    private Boolean isLock = false;
    private String clientId = "";
    private String ip = "";

    private final MutableLiveData<WorkStatus> mWorkStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<ErrorStatus> mErrorStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Float> mPositionLiveData = new MutableLiveData<>();
    private final MutableLiveData<PowerStatus> mPowerStatusLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mIsOn220ACLiveData = new MutableLiveData<>();

    public synchronized void updateLockStatus(Boolean islock_) {
        this.isLock = islock_;
    }

    public synchronized void updateClientId(String clientId_) {
        this.clientId = clientId_;
    }

    public synchronized void updateIP(String ip_) {
        this.ip = ip_;
    }

    public synchronized void updateWorkStatus(byte high, byte low) {
        mWorkStatus = parseWorkStatus(high, low);
        mWorkStatusLiveData.postValue(mWorkStatus); // 推送新状态
    }

    public synchronized void updateErrorStatus(byte low) {
        mErrorStatus = parseErrorStatus(low);
        mErrorStatusLiveData.postValue(mErrorStatus);
    }

    public synchronized void updatePosition(byte high, byte low) {
        int mmValue = ((high & 0xFF) << 8) | (low & 0xFF);
        mPosition = mmValue / 1000.0f;
        mPositionLiveData.postValue(mPosition);
    }

    public synchronized void updatePowerStatus(Boolean isACon, Float acVoltage, Boolean isDCon, Float dcVoltage) {
        if (isACon != null) {
            mPowerStatus.isACon = isACon;
        }
        if (acVoltage != null) {
            mPowerStatus.acVoltage = acVoltage;
        }
        if (isDCon != null) {
            mPowerStatus.isDCon = isDCon;
        }
        if (dcVoltage != null) {
            mPowerStatus.dcVoltage = dcVoltage;
        }
        mPowerStatusLiveData.postValue(mPowerStatus);
    }

    public synchronized void updateIsOn220AC(boolean tag) {
        isOn220AC = tag;
        mIsOn220ACLiveData.postValue(isOn220AC);
    }

    public LiveData<WorkStatus> getWorkStatusLiveData() {
        return mWorkStatusLiveData;
    }

    public LiveData<ErrorStatus> getErrorStatusLiveData() {
        return mErrorStatusLiveData;
    }

    public LiveData<Float> getPositionLiveData() {
        return mPositionLiveData;
    }

    public LiveData<PowerStatus> getPowerStatusLiveData() {
        return mPowerStatusLiveData;
    }

    public LiveData<Boolean> getIsOn220ACLiveData() {
        return mIsOn220ACLiveData;
    }

    public WorkStatus getWorkStatus() {
        return mWorkStatus;
    }

    public ErrorStatus getErrorStatus() {
        return mErrorStatus;
    }

    public float getPosition() {
        return mPosition;
    }

    public PowerStatus getPowerStatus() {
        return mPowerStatus;
    }

    public Boolean getOn220AC() {
        return isOn220AC;
    }

    public Boolean getLock() {
        return isLock;
    }

    public String getClientId() {
        return clientId;
    }

    public String getIp() {
        return ip;
    }

    // 工作状态解析
    public static class WorkStatus {
        public boolean eggMotorOrigin;      // 蛋体电机原点到位
        public boolean eggMotorEnd;         // 蛋体电机下降终点到位
        public boolean topMotorOrigin;       // 顶柜电机原点到位
        public boolean topMotorPosition;     // 顶柜绝对位置到位

        public boolean eggMotorStopped;      // 蛋体电机停止
        public boolean eggMotorRaising;      // 蛋体电机上升中
        public boolean eggMotorFalling;      // 蛋体电机下降中
        public boolean acOutputOn;           // 交流输出开启

        public boolean topMotorStopped;      // 顶柜电机停止
        public boolean topMotorRaising;      // 顶柜电机上升中
        public boolean topMotorFalling;      // 顶柜电机下降中

    }

    public static class ErrorStatus {
        public boolean acOverCurrent;      // 交流过流
        public boolean eggEndTimeout;      // 蛋体终点超时
        public boolean eggOriginTimeout;   // 蛋体原点超时
        public boolean topOriginTimeout;   // 顶柜原点超时
    }

    public static WorkStatus parseWorkStatus(byte high, byte low) {
        WorkStatus status = new WorkStatus();
        // 高字节解析
        status.eggMotorOrigin = (high & 0x08) != 0;
        status.eggMotorEnd = (high & 0x04) != 0;
        status.topMotorOrigin = (high & 0x02) != 0;
        status.topMotorPosition = (high & 0x01) != 0;

        // 低字节解析
        status.eggMotorStopped = (low & 0x40) != 0;
        status.eggMotorRaising = (low & 0x20) != 0;
        status.eggMotorFalling = (low & 0x10) != 0;
        status.acOutputOn = (low & 0x08) != 0;
        status.topMotorStopped = (low & 0x04) != 0;
        status.topMotorRaising = (low & 0x02) != 0;
        status.topMotorFalling = (low & 0x01) != 0;

        return status;
    }

    public static ErrorStatus parseErrorStatus(byte low) {
        ErrorStatus status = new ErrorStatus();
        status.acOverCurrent = (low & 0x08) != 0;    // 0000 1000
        status.eggEndTimeout = (low & 0x04) != 0;    // 0000 0100
        status.eggOriginTimeout = (low & 0x02) != 0; // 0000 0010
        status.topOriginTimeout = (low & 0x01) != 0; // 0000 0001
        return status;
    }

    public static class PowerStatus {
        public boolean isACon = false;
        public float acVoltage = 0f;
        public boolean isDCon = false;
        public float dcVoltage = 0f;
    }
}