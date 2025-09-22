package com.xd.xdmanager.frame;

import android.text.InputFilter;
import android.text.Spanned;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DecimalDigitsInputFilter implements InputFilter {
    private final Pattern mPattern;

    public DecimalDigitsInputFilter() {
        mPattern = Pattern.compile("^\\d+(\\.\\d{0,1})?$");
    }

    @Override
    public CharSequence filter(CharSequence source, int start, int end,
                               Spanned dest, int dstart, int dend) {
        String newString = dest.toString().substring(0, dstart)
                + source.subSequence(start, end)
                + dest.toString().substring(dend);

        Matcher matcher = mPattern.matcher(newString);
        if (!matcher.matches()) {
            return "";
        }
        return null;
    }
}