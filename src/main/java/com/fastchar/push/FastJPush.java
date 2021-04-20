package com.fastchar.push;

import cn.jiguang.common.ClientConfig;
import cn.jiguang.common.utils.Base64;
import cn.jpush.api.JPushClient;
import cn.jpush.api.push.PushResult;
import cn.jpush.api.push.model.Message;
import cn.jpush.api.push.model.Options;
import cn.jpush.api.push.model.Platform;
import cn.jpush.api.push.model.PushPayload;
import cn.jpush.api.push.model.audience.Audience;
import cn.jpush.api.push.model.audience.AudienceTarget;
import cn.jpush.api.push.model.notification.AndroidNotification;
import cn.jpush.api.push.model.notification.IosNotification;
import cn.jpush.api.push.model.notification.Notification;
import com.fastchar.core.FastChar;
import com.fastchar.push.exception.FastPushException;
import com.fastchar.utils.FastStringUtils;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.net.URI;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class FastJPush {

    public static FastJPush getInstance() {
        return getInstance(null);
    }
    public static FastJPush getInstance(String configOnlyCode) {
        FastJPush fastJPush = FastChar.getOverrides().newInstance(FastJPush.class);
        fastJPush.pushConfig = FastChar.getConfig(configOnlyCode, FastJPushConfig.class);
        return fastJPush;
    }

    private FastJPush() {
    }

    private JPushClient jpushClient;
    private FastJPushConfig pushConfig;
    private boolean async = false;//是否异步执行

    private void check() {
        if (FastStringUtils.isEmpty(pushConfig.getAppKey())) {
            throw new FastPushException("极光配置appKey不可为空！");
        }
        if (FastStringUtils.isEmpty(pushConfig.getMasterSecret())) {
            throw new FastPushException("极光配置masterSecret不可为空！");
        }

        ClientConfig clientConfig = ClientConfig.getInstance();
        jpushClient = new JPushClient(pushConfig.getMasterSecret(), pushConfig.getAppKey(), null, clientConfig);
    }

    public boolean isAsync() {
        return async;
    }

    public FastJPush setAsync(boolean async) {
        this.async = async;
        return this;
    }

    /**
     * 推送通知，所有消息
     *
     * @param title
     * @param msg
     */
    public void pushAllAlert(final String title, final String msg) {
        pushAllAlert(title, msg, null);
    }

    /**
     * 推送通知，所有消息
     *
     * @param title
     * @param msg
     */
    public void pushAllAlert(final String title, final String msg, final Map<String, String> extra) {
        pushAllAlert("default", title, msg, extra);
    }

    /**
     * 推送通知，所有消息
     *
     * @param title
     * @param msg
     */
    public void pushAllAlert(final String iosSound, final String title, final String msg, final Map<String, String> extra) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    AndroidNotification.Builder androidBd = AndroidNotification.newBuilder()
                            .setTitle(title)
                            .setAlert(msg);
                    if (pushConfig.isAndroidMsg()) {
                        androidBd.setAlert("");
                    }

                    IosNotification.Builder iosBd = IosNotification.newBuilder()
                            .setAlert(msg)
                            .setMutableContent(true)
                            .setSound(iosSound);
                    if (extra != null && extra.size() != 0) {
                        extra.put("title", title);
                        extra.put("msg", msg);
                        iosBd.addExtras(extra);
                        androidBd.addExtras(extra);
                    }

                    PushPayload payload = PushPayload.newBuilder()
                            .setPlatform(Platform.all())
                            .setAudience(Audience.all())
                            .setNotification(Notification.newBuilder()
                                    .setAlert(msg)
                                    .addPlatformNotification(iosBd.build())
                                    .addPlatformNotification(androidBd.build())
                                    .build())
                            .setOptions(Options
                                    .newBuilder()
                                    .setTimeToLive(86400 * 3)
                                    .setApnsProduction(pushConfig.isApn()).build())//推送iPhone 通知
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.toString());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到设备注册！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        }else{
            runnable.run();
        }
    }


    /**
     * 推送指定别名的通知消息
     *
     * @param title
     * @param msg
     * @param alias
     */
    public void pushAllAlertByAlias(final String title, final String msg, final String... alias) {
        pushAllAlertByAlias(title, msg, null, alias);
    }


    /**
     * 推送指定别名的通知消息
     *
     * @param title
     * @param msg
     * @param alias
     */
    public void pushAllAlertByAlias(
            final String title,
            final String msg,
            final Map<String, String> extra,
            final String... alias) {
        pushAllAlertByAlias("default", title, msg, extra, alias);
    }

    /**
     * 推送指定别名的通知消息
     *
     * @param title
     * @param msg
     * @param alias
     */
    public void pushAllAlertByAlias(
            final String iosSound,
            final String title,
            final String msg,
            final Map<String, String> extra,
            final String... alias) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (pushConfig.isDebug()) {
                    FastChar.getLog().info("推送的别名：" + Arrays.toString(alias));
                }
                try {
                    check();
                    AndroidNotification.Builder androidBd = AndroidNotification.newBuilder()
                            .setTitle(title)
                            .setAlert(msg);
                    if (pushConfig.isAndroidMsg()) {
                        androidBd.setAlert("");
                    }

                    IosNotification.Builder iosBd =
                            IosNotification.newBuilder()
                                    .setAlert(msg)
                                    .setMutableContent(true)
                                    .setSound(iosSound);
                    if (extra != null && extra.size() != 0) {
                        extra.put("title", title);
                        extra.put("msg", msg);

                        androidBd.addExtras(extra);
                        iosBd.addExtras(extra);
                    }

                    iosBd.addExtra("title", title);
                    androidBd.addExtra("title", title);

                    PushPayload payload = PushPayload.newBuilder()
                            .setPlatform(Platform.all())
                            .setAudience(Audience.alias(alias))
                            .setOptions(Options.newBuilder()
                                    .setTimeToLive(86400 * 3)
                                    .setApnsProduction(pushConfig.isApn()).build())//推送iPhone 通知
                            .setNotification(Notification.newBuilder()
                                    .setAlert(msg)
                                    .addPlatformNotification(iosBd.build())
                                    .addPlatformNotification(androidBd.build())
                                    .build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带有别名[" + FastStringUtils.join(alias, ",") + "]的设备！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }


    /**
     * 推送指定标签的通知消息
     *
     * @param title
     * @param msg
     * @param tags  客户端包含其中一个tag就可收到
     */
    public void pushAllAlertByTags(final String title, final String msg, final String... tags) {
        pushAllAlertByTags(title, msg, null, tags);
    }

    /**
     * 推送指定标签的通知消息
     *
     * @param title
     * @param msg
     * @param tags  客户端包含其中一个tag就可收到
     */
    public void pushAllAlertByTags(final String title, final String msg, final Map<String, String> extra, final String... tags) {
        pushAllAlertByTags("default", title, msg, extra, tags);
    }

    /**
     * 推送指定标签的通知消息
     *
     * @param title
     * @param msg
     * @param tags  客户端包含其中一个tag就可收到
     */
    public void pushAllAlertByTags(final String iosSound, final String title, final String msg, final Map<String, String> extra, final String... tags) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    IosNotification.Builder iosBd = IosNotification.newBuilder()
                            .setMutableContent(true)
                            .setAlert(msg)
                            .setSound(iosSound);

                    AndroidNotification.Builder androidBd = AndroidNotification.newBuilder()
                            .setTitle(title)
                            .setAlert(msg);
                    if (pushConfig.isAndroidMsg()) {
                        androidBd.setAlert("");
                    }

                    if (extra != null && extra.size() != 0) {
                        extra.put("title", title);
                        extra.put("msg", msg);

                        androidBd.addExtras(extra);
                        iosBd.addExtras(extra);
                    }

                    iosBd.addExtra("title", title);
                    androidBd.addExtra("title", title);

                    PushPayload payload = PushPayload.newBuilder()
                            .setPlatform(Platform.all())
                            .setAudience(Audience.tag(tags))
                            .setOptions(Options.newBuilder().setTimeToLive(86400 * 3).setApnsProduction(pushConfig.isApn()).build())//推送iPhone 通知
                            .setNotification(Notification.newBuilder()
                                    .setAlert(msg)
                                    .addPlatformNotification(iosBd.build())
                                    .addPlatformNotification(androidBd.build())
                                    .build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带有标签[" + FastStringUtils.join(tags, ",") + "]的设备！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }

    /**
     * 推送指定标签的通知消息
     *
     * @param title
     * @param msg
     * @param tags  客户端必须同时全部包含传入tag
     */
    public void pushAllAlertByAndTags(final String title, final String msg, final Map<String, String> extra, final String... tags) {
        pushAllAlertByAndTags("default", title, msg, extra, tags);
    }

    /**
     * 推送指定标签的通知消息
     *
     * @param title
     * @param msg
     * @param tags  客户端必须同时全部包含传入tag
     */
    public void pushAllAlertByAndTags(final String iosSound, final String title, final String msg, final Map<String, String> extra, final String... tags) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    IosNotification.Builder iosBd = IosNotification.newBuilder()
                            .setAlert(msg)
                            .setMutableContent(true)
                            .setSound(iosSound);

                    AndroidNotification.Builder androidBd = AndroidNotification.newBuilder()
                            .setTitle(title)
                            .setAlert(msg);
                    if (pushConfig.isAndroidMsg()) {
                        androidBd.setAlert("");
                    }

                    if (extra != null && extra.size() != 0) {
                        androidBd.addExtras(extra);
                        iosBd.addExtras(extra);
                    }

                    iosBd.addExtra("title", title);
                    androidBd.addExtra("title", title);

                    PushPayload payload = PushPayload.newBuilder()
                            .setPlatform(Platform.all())
                            .setAudience(Audience.tag_and(tags))
                            .setOptions(Options.newBuilder().setTimeToLive(86400 * 3).setApnsProduction(pushConfig.isApn()).build())//推送iPhone 通知
                            .setNotification(Notification.newBuilder().setAlert(msg)
                                    .addPlatformNotification(iosBd.build())
                                    .addPlatformNotification(androidBd.build())
                                    .build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带有标签[" + FastStringUtils.join(tags, ",") + "]的设备！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }


    /**
     * 推送自定义消息，Android或IOS 不会有Notification提醒
     *
     * @param alias
     */
    public void pushAllMsgByAlias(final Map<String, String> extra, final String... alias) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    Message.Builder msgBd = Message.newBuilder().setMsgContent("m");
                    if (extra != null && extra.size() != 0) {
                        msgBd.addExtras(extra);
                    }
                    PushPayload payload = PushPayload.newBuilder()
                            .setPlatform(Platform.all())
                            .setAudience(Audience.newBuilder().addAudienceTarget(AudienceTarget.alias(alias)).build())
                            .setMessage(msgBd.build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带有别名[" + FastStringUtils.join(alias, ",") + "]的设备！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }


    /**
     * 推送自定义消息，Android或IOS 不会有Notification提醒
     *
     * @param tags 客户端包含其中一个tag就可收到
     */
    public void pushAllMsgByTag(final Map<String, String> extra, final String... tags) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    Message.Builder msgBd = Message.newBuilder().setMsgContent("m");
                    if (extra != null && extra.size() != 0) {
                        msgBd.addExtras(extra);
                    }
                    PushPayload payload = PushPayload.newBuilder()
                            .setPlatform(Platform.all())
                            .setAudience(Audience.newBuilder().addAudienceTarget(AudienceTarget.tag(tags)).build())
                            .setMessage(msgBd.build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带有标签[" + FastStringUtils.join(tags, ",") + "]的设备！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }


    /**
     * 推送自定义消息，Android或IOS 不会有Notification提醒
     *
     * @param tags 客户端必须同时全部包含传入tag
     */
    public void pushAllMsgByAndTag(final Map<String, String> extra, final String... tags) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    Message.Builder msgBd = Message.newBuilder().setMsgContent("m");
                    if (extra != null && extra.size() != 0) {
                        msgBd.addExtras(extra);
                    }
                    PushPayload payload = PushPayload.newBuilder().setPlatform(Platform.all())
                            .setAudience(Audience.newBuilder().addAudienceTarget(AudienceTarget.tag_and(tags)).build())
                            .setMessage(msgBd.build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带有标签[" + FastStringUtils.join(tags, ",") + "]的设备！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        } else {
            runnable.run();
        }
    }


    /**
     * 推送自定义穿透消息，Android或IOS 不会有Notification提醒
     *
     * @param
     */
    public void pushAllMsg(final String title, final String message, final Map<String, String> extra) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    check();
                    Message.Builder msgBd = Message.newBuilder().setMsgContent(message).setTitle(title);
                    if (extra != null && extra.size() != 0) {
                        msgBd.addExtras(extra);
                    }
                    PushPayload payload = PushPayload.newBuilder().setPlatform(Platform.all())
                            .setMessage(msgBd.build())
                            .setAudience(Audience.newBuilder().setAll(true).build())
                            .build();
                    PushResult result = jpushClient.sendPush(payload);
                    if (result.isResultOK()) {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().info("推送结果：" + result.toString());
                        }
                    } else {
                        if (pushConfig.isDebug()) {
                            FastChar.getLog().error("推送失败：" + result.getOriginalContent());
                        }
                    }
                } catch (Exception e) {
                    if (pushConfig.isDebug()) {
                        e.printStackTrace();
                        FastChar.getLog().error("推送异常：未检测到带设备注册！");
                    }
                } finally {
                    jpushClient.close();
                }
            }
        };
        if (isAsync()) {
            new Thread(runnable).start();
        }else{
            runnable.run();
        }
    }




}
