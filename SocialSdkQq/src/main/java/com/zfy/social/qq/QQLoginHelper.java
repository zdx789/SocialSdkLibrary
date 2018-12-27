package com.zfy.social.qq;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.tencent.connect.UserInfo;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;
import com.zfy.social.core.common.Target;
import com.zfy.social.core.exception.SocialError;
import com.zfy.social.core.listener.OnLoginListener;
import com.zfy.social.core.model.LoginResult;
import com.zfy.social.core.model.token.AccessToken;
import com.zfy.social.core.util.JsonUtil;
import com.zfy.social.core.util.SocialUtil;
import com.zfy.social.qq.model.QQAccessToken;
import com.zfy.social.qq.model.QQUser;

import org.json.JSONObject;

import java.lang.ref.WeakReference;

/**
 * CreateAt : 2016/12/6
 * Describe : qq 登录辅助
 *
 * @author chendong
 */

class QQLoginHelper {

    public static final String TAG = QQLoginHelper.class.getSimpleName();

    private int mLoginTarget;
    private Tencent mTencentApi;
    private WeakReference<Activity> mActivityRef;
    private OnLoginListener mOnLoginListener;
    private LoginUiListener mUiListener;


    QQLoginHelper(Activity activity, Tencent mTencentApi, OnLoginListener onQQLoginListener) {
        this.mActivityRef = new WeakReference<>(activity);
        this.mTencentApi = mTencentApi;
        this.mOnLoginListener = onQQLoginListener;
        this.mLoginTarget = Target.LOGIN_QQ;
    }

    private Context getContext() {
        return mActivityRef.get().getApplicationContext();
    }

    public QQAccessToken getToken() {
        return AccessToken.getToken(getContext(), mLoginTarget, QQAccessToken.class);
    }

    // 接受登录结果
    void handleResultData(Intent data) {
        Tencent.handleResultData(data, this.mUiListener);
    }

    // 登录
    public void login() {
        QQAccessToken qqToken = getToken();
        if (qqToken != null) {
            mTencentApi.setAccessToken(qqToken.getAccess_token(), String.valueOf(qqToken.getExpires_in()));
            mTencentApi.setOpenId(qqToken.getOpenid());
            if (mTencentApi.isSessionValid()) {
                getUserInfo(qqToken);
            } else {
                mUiListener = new LoginUiListener();
                mTencentApi.login(mActivityRef.get(), "all", mUiListener);
            }
        } else {
            mUiListener = new LoginUiListener();
            mTencentApi.login(mActivityRef.get(), "all", mUiListener);
        }
    }

    // 登录监听包装类
    private class LoginUiListener implements IUiListener {
        @Override
        public void onComplete(Object o) {
            JSONObject jsonResponse = (JSONObject) o;
            QQAccessToken qqToken = JsonUtil.getObject(jsonResponse.toString(), QQAccessToken.class);
            SocialUtil.e(TAG, "获取到 qq token = " + qqToken);
            if (qqToken == null) {
                mOnLoginListener.onFailure(SocialError.make(SocialError.CODE_PARSE_ERROR, TAG + "#LoginUiListener#qq token is null, data = " + qqToken));
                return;
            }
            // 保存token
            AccessToken.saveToken(getContext(), mLoginTarget, qqToken);
            mTencentApi.setAccessToken(qqToken.getAccess_token(), qqToken.getExpires_in() + "");
            mTencentApi.setOpenId(qqToken.getOpenid());
            getUserInfo(qqToken);
        }


        @Override
        public void onError(UiError e) {
            mOnLoginListener.onFailure(SocialError.make(SocialError.CODE_SDK_ERROR, TAG + "#LoginUiListener#获取用户信息失败 " + QQPlatform.parseUiError(e)));
        }

        @Override
        public void onCancel() {
            mOnLoginListener.onCancel();
        }
    }

    // 获取用户信息
    private void getUserInfo(final QQAccessToken qqToken) {
        UserInfo info = new UserInfo(getContext(), mTencentApi.getQQToken());
        info.getUserInfo(new IUiListener() {
            @Override
            public void onComplete(Object object) {
                QQUser qqUserInfo = JsonUtil.getObject(object.toString(), QQUser.class);
                if (qqUserInfo == null) {
                    if (mOnLoginListener != null) {
                        mOnLoginListener.onFailure(SocialError.make(SocialError.CODE_PARSE_ERROR, TAG + "#getUserInfo#解析 qq user 错误, data = " + object.toString()));
                    }
                } else {
                    qqUserInfo.setOpenId(mTencentApi.getOpenId());
                    if (mOnLoginListener != null) {
                        mOnLoginListener.onSuccess(new LoginResult(mLoginTarget, qqUserInfo, qqToken));
                    }
                }
            }

            @Override
            public void onError(UiError e) {
                mOnLoginListener.onFailure(SocialError.make(SocialError.CODE_SDK_ERROR, TAG + "#getUserInfo#qq获取用户信息失败  " + QQPlatform.parseUiError(e)));
            }

            @Override
            public void onCancel() {
                mOnLoginListener.onCancel();
            }

        });
    }
}
