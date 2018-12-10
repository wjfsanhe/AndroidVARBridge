package com.koushikdutta.async.sample.floatUtil;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Created by yhao on 2017/12/23.
 * https://github.com/yhaolpz
 */

public class Screen {
    public static final int width = 3840;
    public static final int height = 2160;

    @IntDef({width, height})
    @Retention(RetentionPolicy.SOURCE)
    @interface screenType {
    }
}
