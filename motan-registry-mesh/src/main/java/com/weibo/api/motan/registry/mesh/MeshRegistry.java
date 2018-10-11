package com.weibo.api.motan.registry.mesh;

import com.weibo.api.motan.registry.NotifyListener;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.registry.support.FailbackRegistry;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.LoggerUtil;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author sunnights
 */
public class MeshRegistry extends FailbackRegistry {
    private static final ScheduledExecutorService SCHEDULED_SERVICE = new ScheduledThreadPoolExecutor(1, new BasicThreadFactory.Builder().namingPattern("mesh-registry-schedule-pool-%d").build());
    private Registry backupRegistry;
    private MeshClient meshClient;
    private boolean available;
    private ConcurrentHashMap<URL, ConcurrentHashMap<BackupServiceListener, List<URL>>> subscribeListeners = new ConcurrentHashMap<>();
    private int meshPort;

    public MeshRegistry(final URL url, MeshClient client, Registry backupRegistry) {
        super(url);
        this.meshClient = client;
        this.backupRegistry = backupRegistry;

        available = meshClient.checkAvailable();

        meshPort = meshClient.getMeshPort();

        SCHEDULED_SERVICE.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                boolean availableNow = meshClient.checkAvailable();
                if (availableNow && !available) {
                    available = true;
                    for (Map.Entry<URL, ConcurrentHashMap<BackupServiceListener, List<URL>>> urlMapEntry : subscribeListeners.entrySet()) {
                        for (BackupServiceListener backupServiceListener : urlMapEntry.getValue().keySet()) {
                            backupServiceListener.getNotifyListener().notify(urlMapEntry.getKey(), doDiscover(urlMapEntry.getKey()));
                        }
                    }
                } else if (!availableNow) {
                    for (Map.Entry<URL, ConcurrentHashMap<BackupServiceListener, List<URL>>> urlMapEntry : subscribeListeners.entrySet()) {
                        for (Map.Entry<BackupServiceListener, List<URL>> listenerListEntry : urlMapEntry.getValue().entrySet()) {
                            listenerListEntry.getKey().getNotifyListener().notify(urlMapEntry.getKey(), listenerListEntry.getValue());
                        }
                    }
                    available = false;
                    LoggerUtil.warn("using backup registry, url={}, backupUrl={}", url, meshClient.getBackupRegistryUrl());
                }
            }
        }, 10, 10, TimeUnit.SECONDS);
    }

    @Override
    protected void doRegister(URL url) {
        meshClient.register(url);
    }

    @Override
    protected void doUnregister(URL url) {
        meshClient.unregister(url);
    }

    @Override
    protected void doSubscribe(final URL url, final NotifyListener listener) {
        ConcurrentHashMap<BackupServiceListener, List<URL>> listenerUrlsMap = subscribeListeners.get(url);
        if (listenerUrlsMap == null) {
            subscribeListeners.putIfAbsent(url, new ConcurrentHashMap<BackupServiceListener, List<URL>>());
        }
        backupRegistry.subscribe(url, new BackupServiceListener(url, listener));

        meshClient.subscribe(url);
        listener.notify(url, doDiscover(url));
    }

    @Override
    protected void doUnsubscribe(URL url, NotifyListener listener) {
        for (Map.Entry<BackupServiceListener, List<URL>> listenerListEntry : subscribeListeners.get(url).entrySet()) {
            backupRegistry.unsubscribe(url, listenerListEntry.getKey());
        }

        meshClient.unsubscribe(url);
    }

    @Override
    protected List<URL> doDiscover(URL url) {
        URL meshUrl = url.createCopy();
        meshUrl.setHost(getUrl().getHost());
        meshUrl.setPort(meshPort);
        return Collections.singletonList(meshUrl);
    }

    @Override
    protected void doAvailable(URL url) {
        // do nothing
    }

    @Override
    protected void doUnavailable(URL url) {
        // do nothing
    }

    class BackupServiceListener implements NotifyListener {
        private URL serviceUrl;
        private NotifyListener notifyListener;

        public BackupServiceListener(URL serviceUrl, NotifyListener notifyListener) {
            this.serviceUrl = serviceUrl;
            this.notifyListener = notifyListener;
        }

        public NotifyListener getNotifyListener() {
            return notifyListener;
        }

        @Override
        public void notify(URL registryUrl, List<URL> urls) {
            LoggerUtil.info("backup service change notify, registry={}, service={}, urls={} ", registryUrl.getUri(), serviceUrl.getIdentity(), urls.toString());
            subscribeListeners.get(serviceUrl).put(this, urls);
        }

    }
}
