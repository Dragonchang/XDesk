package com.deepblue.log;

import android.text.TextUtils;

public class DeepBlueLogConfig {
    private static final long DAYS = 24 * 60 * 60 * 1000; //天
    private static final long M = 1024 * 1024; //M
    private static final long DEFAULT_DAY = 5 * DAYS; //默认删除天数
    private static final long DEFAULT_FILE_SIZE = 10 * M;
    private static final long DEFAULT_MIN_SDCARD_SIZE = 50 * M; //最小的SD卡小于这个大小不写入
    private static final int DEFAULT_QUEUE = 500;

    String mCachePath; //mmap缓存路径
    String mPathPath; //file文件路径

    long mMaxFile = DEFAULT_FILE_SIZE; //删除文件最大值
    long mDay = DEFAULT_DAY; //删除天数
    long mMaxQueue = DEFAULT_QUEUE;
    long mMinSDCard = DEFAULT_MIN_SDCARD_SIZE; //最小sdk卡大小

    private void setCachePath(String cachePath) {
        mCachePath = cachePath;
    }

    private void setPathPath(String pathPath) {
        mPathPath = pathPath;
    }

    private void setMaxFile(long maxFile) {
        mMaxFile = maxFile;
    }

    private void setDay(long day) {
        mDay = day;
    }

    private void setMinSDCard(long minSDCard) {
        mMinSDCard = minSDCard;
    }

    boolean isValid() {
        boolean valid = false;
        if (!TextUtils.isEmpty(mCachePath) && !TextUtils.isEmpty(mPathPath)) {
            valid = true;
        }
        return valid;
    }

    public static final class Builder {
        String mCachePath; //mmap缓存路径
        String mPath; //file文件路径
        long mMaxFile = DEFAULT_FILE_SIZE; //删除文件最大值
        long mDay = DEFAULT_DAY; //删除天数
        long mMinSDCard = DEFAULT_MIN_SDCARD_SIZE;

        public Builder setCachePath(String cachePath) {
            mCachePath = cachePath;
            return this;
        }

        public Builder setPath(String path) {
            mPath = path;
            return this;
        }

        public Builder setMaxFile(long maxFile) {
            mMaxFile = maxFile * M;
            return this;
        }

        public Builder setDay(long day) {
            mDay = day * DAYS;
            return this;
        }

        public Builder setMinSDCard(long minSDCard) {
            this.mMinSDCard = minSDCard;
            return this;
        }

        public DeepBlueLogConfig build() {
            DeepBlueLogConfig config = new DeepBlueLogConfig();
            config.setCachePath(mCachePath);
            config.setPathPath(mPath);
            config.setMaxFile(mMaxFile);
            config.setMinSDCard(mMinSDCard);
            config.setDay(mDay);
            return config;
        }
    }
}

