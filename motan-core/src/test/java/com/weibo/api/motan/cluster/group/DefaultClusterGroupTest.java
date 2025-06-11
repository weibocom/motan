package com.weibo.api.motan.cluster.group;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import com.weibo.api.motan.rpc.URL;
import com.weibo.api.motan.util.MotanSwitcherUtil;

public class DefaultClusterGroupTest extends BaseTestCase {
    private DefaultClusterGroup<Object> clusterGroup;
    private Cluster<Object> masterCluster;
    private List<Cluster<Object>> backupClusters;
    private List<Cluster<Object>> sandboxClusters;
    private ClusterSelector<Object> selector;
    private Request request;
    private Response response;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        masterCluster = mockery.mock(Cluster.class, "masterCluster");
        selector = mockery.mock(ClusterSelector.class);
        request = mockery.mock(Request.class);
        response = mockery.mock(Response.class);
        
        clusterGroup = new DefaultClusterGroup<>(masterCluster);
    }

    public void testConstructorAndBasicMethods() {
        final URL url = new URL("motan", "localhost", 8001, "testService");
        final Class<?> interfaceClass = TestInterface.class;

        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            allowing(masterCluster).getInterface();
            will(returnValue(interfaceClass));
            allowing(masterCluster).desc();
            will(returnValue("test desc"));
            allowing(masterCluster).isAvailable();
            will(returnValue(true));
        }});

        assertSame(masterCluster, clusterGroup.getMasterCluster());
        assertSame(url, clusterGroup.getUrl());
        assertSame(interfaceClass, clusterGroup.getInterface());
        assertEquals("test desc", clusterGroup.desc());
        assertTrue(clusterGroup.isAvailable());
    }

    @SuppressWarnings("unchecked")
    public void testInitWithSandboxClusters() {
        sandboxClusters = new ArrayList<>();
        Cluster<Object> sandboxCluster = mockery.mock(Cluster.class, "sandboxCluster");
        sandboxClusters.add(sandboxCluster);
        Referer<Object> referer = mockery.mock(Referer.class, "referer");
        List<Referer<Object>> referers = new ArrayList<>();
        referers.add(referer);

        final URL url = new URL("motan", "localhost", 8001, "testService");
        url.addParameter(URLParamType.clusterSelector.getName(), "default");

        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            oneOf(sandboxCluster).getUrl();
            will(returnValue(new URL("motan", "localhost", 8002, "testService")));
            oneOf(sandboxCluster).getReferers();
            will(returnValue(referers));
        }});

        clusterGroup.setSandboxClusters(sandboxClusters);
        clusterGroup.init();

        // Verify selector is initialized by making a call
        mockery.checking(new Expectations() {{
            oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
            will(returnValue(DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX));
            oneOf(sandboxCluster).call(request);
            will(returnValue(response));
        }});

        Response result = clusterGroup.call(request);
        assertSame(response, result);
    }

    @SuppressWarnings("unchecked")
    public void testInitWithBackupClusters() {
        backupClusters = new ArrayList<>();
        Cluster<Object> backupCluster1 = mockery.mock(Cluster.class, "backupCluster1");
        Cluster<Object> backupCluster2 = mockery.mock(Cluster.class, "backupCluster2");
        backupClusters.add(backupCluster1);
        backupClusters.add(backupCluster2);

        clusterGroup.setBackupClusters(backupClusters);
        clusterGroup.init();
        Response response2 = mockery.mock(Response.class, "response2");

        // Enable backup cluster switcher
        MotanSwitcherUtil.setSwitcherValue(DefaultClusterGroup.BACKUP_CLUSTER_SWITCHER_NAME, true);

        // Verify backup clusters are used in round-robin fashion
        mockery.checking(new Expectations() {{
            allowing(masterCluster).call(request);
            will(throwException(new MotanServiceException("No available referers")));
            allowing(backupCluster1).call(request);
            will(returnValue(response));
            allowing(backupCluster2).call(request);
            will(returnValue(response2));
        }});

        // First call should use backupCluster2
        Response result1 = clusterGroup.call(request);
        assertSame(response2, result1);

        // Second call should use backupCluster1
        Response result2 = clusterGroup.call(request);
        assertSame(response, result2);

        // Third call should use backupCluster2 again
        Response result3 = clusterGroup.call(request);
        assertSame(response2, result3);
    }

    public void testCallWithMasterCluster() {
        mockery.checking(new Expectations() {{
            oneOf(masterCluster).call(request);
            will(returnValue(response));
        }});

        Response result = clusterGroup.call(request);
        assertSame(response, result);
    }

    @SuppressWarnings("unchecked")
    public void testCallWithSelector() {
        sandboxClusters = new ArrayList<>();
        Cluster<Object> sandboxCluster = mockery.mock(Cluster.class, "sandboxCluster");
        sandboxClusters.add(sandboxCluster);

        final URL url = new URL("motan", "localhost", 8001, "testService");
        url.addParameter(URLParamType.clusterSelector.getName(), "default");

        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            oneOf(sandboxCluster).getUrl();
            will(returnValue(new URL("motan", "localhost", 8002, "testService")));
            oneOf(sandboxCluster).getReferers();
            will(returnValue(new ArrayList<>())); // empty referers
        }});

        clusterGroup.setSandboxClusters(sandboxClusters);
        clusterGroup.init();

        // masterCluster should be called because sandboxCluster has empty referers
        mockery.checking(new Expectations() {{
            oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
            will(returnValue(DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX));
            oneOf(masterCluster).call(request);
            will(returnValue(response));
        }});

        Response result = clusterGroup.call(request);
        assertSame(response, result);
    }

    @SuppressWarnings("unchecked")
    public void testCallWithBackupClusterSwitcherOff() {
        backupClusters = new ArrayList<>();
        Cluster<Object> backupCluster = mockery.mock(Cluster.class, "backupCluster");
        backupClusters.add(backupCluster);
        clusterGroup.setBackupClusters(backupClusters);
        clusterGroup.init();

        // Disable backup cluster switcher
        MotanSwitcherUtil.setSwitcherValue(DefaultClusterGroup.BACKUP_CLUSTER_SWITCHER_NAME, false);

        final MotanServiceException exception = new MotanServiceException("No available referers");
        mockery.checking(new Expectations() {{
            oneOf(masterCluster).call(request);
            will(throwException(exception));
        }});

        try {
            clusterGroup.call(request);
            fail("Should throw exception when backup switcher is off");
        } catch (MotanServiceException e) {
            assertSame(exception, e);
        }
    }

    @SuppressWarnings("unchecked")
    public void testDestroy() {
        sandboxClusters = new ArrayList<>();
        Cluster<Object> sandboxCluster = mockery.mock(Cluster.class, "sandboxCluster");
        sandboxClusters.add(sandboxCluster);

        final URL url = new URL("motan", "localhost", 8001, "testService");
        url.addParameter(URLParamType.clusterSelector.getName(), "default");

        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            oneOf(sandboxCluster).getUrl();
            will(returnValue(new URL("motan", "localhost", 8002, "testService")));
            oneOf(sandboxCluster).getReferers();
            will(returnValue(new ArrayList<>()));
        }});

        clusterGroup.setSandboxClusters(sandboxClusters);
        clusterGroup.init();
        selector = mockery.mock(ClusterSelector.class, "selector");
        clusterGroup.selector = selector;
        mockery.checking(new Expectations() {{
            oneOf(selector).destroy();
        }});
        clusterGroup.destroy();
    }

    private interface TestInterface {
    }
}