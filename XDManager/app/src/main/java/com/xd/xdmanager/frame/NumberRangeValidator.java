package com.xd.xdmanager.frame;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;

public class NumberRangeValidator {
    private double mMin = 1.5;
    private double mMax = 24.0;
    private final EditText mEditText;

    public NumberRangeValidator(EditText editText) {
        this.mEditText = editText;
        setupValidation();
    }

    // 动态设置范围
    public void setRange(double min, double max) {
        this.mMin = min;
        this.mMax = max;
        validateInput(); // 修改范围后立即校验当前值
    }

    private void setupValidation() {
        mEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                validateInput();
            }
        });
    }

    private void validateInput() {
        String input = mEditText.getText().toString();
        if (input.isEmpty()) return;

        try {
            double value = Double.parseDouble(input);
            if (value < mMin || value > mMax) {
                showError("数值范围应在 " + mMin + " ~ " + mMax);
            } else {
                clearError();
            }
        } catch (NumberFormatException e) {
            showError("无效数字格式");
        }
    }

    private void showError(String message) {
        mEditText.setError(message);
    }

    private void clearError() {
        mEditText.setError(null);
    }
}