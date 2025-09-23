package com.deepblue.log;

public class Action {
    static final int WRITE = 1;
    static final int FLUSH = 2;
    int mAction;
    boolean isValid() {
        return true;
    }
}
