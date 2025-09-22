package com.xd.xdmanager.model;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.xd.xdmanager.config.WManager;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ListStatus {
    private static volatile ListStatus instance;
    private final int x;
    private final int y;
    private final MutableLiveData<Boolean> _allLockLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> _inCallLiveData = new MutableLiveData<>(false);

    public LiveData<Boolean> getAllLockLiveData() {
        return _allLockLiveData;
    }

    public LiveData<Boolean> getInCallLiveData() {
        return _inCallLiveData;
    }

    public void setAllLock(Boolean allLock) {
        _allLockLiveData.postValue(allLock); // 支持后台线程调用
    }

    public void setInCall(Boolean inCall) {
        _inCallLiveData.postValue(inCall);
    }

    public Boolean getAllLock() {
        return _allLockLiveData.getValue() != null ? _allLockLiveData.getValue() : false;
    }

    public Boolean getInCall() {
        return _inCallLiveData.getValue() != null ? _inCallLiveData.getValue() : false;
    }

    private final Map<String, StatusData> mData = new ConcurrentHashMap<>();
    private final MutableLiveData<List<StatusData>> mDataLiveData = new MutableLiveData<>();
    private final LinkedHashMap<String, StatusData> orderedMap = new LinkedHashMap<>();

    private ListStatus(Context c) {
        this.x = WManager.Companion.instance(c).getXNum();
        this.y = WManager.Companion.instance(c).getYNum();
        initializeFixedSizeMap();
    }

    public static synchronized ListStatus getInstance(Context c) {
        if (instance == null) {
            synchronized (ListStatus.class) {
                if (instance == null) {
                    instance = new ListStatus(c);
                }
            }
        }
        return instance;
    }

    private void initializeFixedSizeMap() {
        List<Map.Entry<String, StatusData>> entries = new ArrayList<>(); // 生成排序用的临时列表
        for (int i = 1; i <= x; i++) {
            for (int j = 1; j <= y; j++) {
                String key = String.format("x%dy%d", i, j);
                entries.add(new AbstractMap.SimpleEntry<>(key, new StatusData(key, false)));
            }
        }
        Collections.sort(entries, new Comparator<Map.Entry<String, StatusData>>() {
            @Override
            public int compare(Map.Entry<String, StatusData> o1, Map.Entry<String, StatusData> o2) {
                return o1.getKey().compareTo(o2.getKey());
            }
        });
        for (Map.Entry<String, StatusData> entry : entries) {
            orderedMap.put(entry.getKey(), entry.getValue());
            mData.put(entry.getKey(), entry.getValue());
        }
        postSortedValues();
    }

    private void postSortedValues() {
        mDataLiveData.postValue(new ArrayList<>(orderedMap.values()));
    }

    public void updateData(String client, StatusData statusData) {
        synchronized (this) {
            if (mData.containsKey(client)) {
                statusData.setCheck(mData.get(client).isCheck());
                mData.put(client, statusData);
                orderedMap.put(client, statusData);
                postSortedValues();
            }
        }
    }

    public void updateAllDataCheck(Boolean tag) {
        synchronized (this) {
            for (StatusData statusData : mData.values()) {
                if (statusData.isConnected())
                    statusData.setCheck(tag);
            }
            postSortedValues();
        }
    }

    public void updateClientConnect(String clientid, Boolean connectStatus) {
        synchronized (this) {
            if (mData.containsKey(clientid)) {
                StatusData statusData = mData.get(clientid);
                statusData.setConnected(connectStatus);
                orderedMap.put(clientid, statusData);
                postSortedValues();
            }
        }
    }

    public LiveData<List<StatusData>> getDataLiveData() {
        return mDataLiveData;
    }

    public Map<String, StatusData> getCurrentSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(mData));
    }

    public List<String> getCheckClientids() {
        List<String> checkedClients = new ArrayList<>();
        for (Map.Entry<String, StatusData> entry : mData.entrySet()) {
            if (entry.getValue().isCheck()) {
                checkedClients.add(entry.getKey()); // clientId 即 Map 的键
            }
        }
        return checkedClients;
    }

//    public List<String> getCheckClientids() {
//        List<String> checkedClients = new ArrayList<>();
//        for (Map.Entry<String, StatusData> entry : mData.entrySet()) {
//            if (entry.getValue().isCheck()) {
//                checkedClients.add(entry.getKey()); // clientId 即 Map 的键
//            }
//        }
//        return checkedClients;
//    }
}