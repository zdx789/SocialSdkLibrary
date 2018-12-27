package com.zfy.social.core.platform;

import android.content.Context;

/**
 * CreateAt : 2018/12/21
 * Describe :
 *
 * @author chendong
 */
public interface PlatformFactory {

    IPlatform create(Context context, int target);

    int getPlatformTarget();

    default boolean checkShareTarget(int shareTarget) {
        return false;
    }

    default boolean checkLoginTarget(int loginTarget) {
        return false;
    }
}
