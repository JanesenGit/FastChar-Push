package com.fastchar.push;

import com.fastchar.annotation.AFastClassFind;
import com.fastchar.core.FastChar;
import com.fastchar.interfaces.IFastConfig;

@AFastClassFind(value = "cn.jiguang.common.ClientConfig", url = "https://mvnrepository.com/artifact/cn.jpush.api/jpush-client")
public class FastJPushConfig implements IFastConfig {

    private String masterSecret;
    private String appKey;
    private boolean debug;
    private boolean apn = true;//ios 是否是发布appstore环境 默认为true
    private boolean androidMsg = false;

    public FastJPushConfig() {
        if (FastChar.getConstant().isDebug()) {
            FastChar.getLog().info("已启用JPush极光推送！");
        }
    }

    public String getMasterSecret() {
        return masterSecret;
    }

    public FastJPushConfig setMasterSecret(String masterSecret) {
        this.masterSecret = masterSecret;
        return this;
    }

    public String getAppKey() {
        return appKey;
    }

    public FastJPushConfig setAppKey(String appKey) {
        this.appKey = appKey;
        return this;
    }

    public boolean isDebug() {
        return debug;
    }

    public FastJPushConfig setDebug(boolean debug) {
        this.debug = debug;
        return this;
    }

    public boolean isApn() {
        return apn;
    }

    public FastJPushConfig setApn(boolean apn) {
        this.apn = apn;
        return this;
    }

    public boolean isAndroidMsg() {
        return androidMsg;
    }

    public FastJPushConfig setAndroidMsg(boolean androidMsg) {
        this.androidMsg = androidMsg;
        return this;
    }
}
