package com.octoblu.gateblu;

import android.webkit.ValueCallback;

import java.util.Random;

public class Util {
    public static int randInt(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    }

    public static class IgnoreReturnValue implements ValueCallback<String> {
        @Override
        public void onReceiveValue(String value) {}
    }
}
