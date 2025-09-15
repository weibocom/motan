package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.common.URLParamType;
import com.weibo.api.motan.exception.MotanServiceException;
import com.weibo.api.motan.mock.MockReferer;
import com.weibo.api.motan.rpc.*;
import com.weibo.api.motan.util.MotanSwitcherUtil;
import org.jmock.Expectations;

import java.util.ArrayList;
import java.util.List;

public class DefaultClusterGroupTest extends BaseTestCase {
    private DefaultClusterGroup<Object> clusterGroup;
    private Cluster<Object> masterCluster;
    private List<Cluster<Object>> backupClusters;
    private List<Cluster<Object>> sandboxClusters;
    private List<Cluster<Object>> greyClusters;
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

    public void testInitWithGreyClusters() {
        greyClusters = new ArrayList<>();
        final Cluster<Object> greyCluster = mockery.mock(Cluster.class, "greyCluster");
        greyClusters.add(greyCluster);
        Referer<Object> referer = mockery.mock(Referer.class, "referer");
        List<Referer<Object>> referers = new ArrayList<>();
        referers.add(referer);

        final URL url = new URL("motan", "localhost", 8001, "testService");
        url.addParameter(URLParamType.clusterSelector.getName(), "default");
        final URL greyUrl = new URL("motan", "localhost", 8003, "testService");
        greyUrl.addParameter("group", "grey-group");
        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            allowing(greyCluster).getUrl();
            will(returnValue(greyUrl));
            oneOf(greyCluster).getReferers();
            will(returnValue(referers));
            oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
            will(returnValue("other-group, grey-group"));
            oneOf(greyCluster).call(request);
            will(returnValue(response));
        }});

        clusterGroup.setGreyClusters(greyClusters);
        clusterGroup.init();

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

        greyClusters = new ArrayList<>();
        Cluster<Object> greyCluster = mockery.mock(Cluster.class, "greyCluster");
        greyClusters.add(greyCluster);

        final URL url = new URL("motan", "localhost", 8001, "testService");
        url.addParameter(URLParamType.clusterSelector.getName(), "default");
        final URL sandboxUrl = new URL("motan", "localhost", 8003, "testService");
        sandboxUrl.addParameter("group", "sandbox-group");
        final URL greyUrl = new URL("motan", "localhost", 8003, "testService");
        greyUrl.addParameter("group", "grey-group");
        final List<Referer<Object>> referers = new ArrayList<>();

        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            allowing(sandboxCluster).getUrl();
            will(returnValue(sandboxUrl));
            allowing(sandboxCluster).getReferers();
            will(returnValue(referers));
            allowing(greyCluster).getUrl();
            will(returnValue(greyUrl));
            allowing(greyCluster).getReferers();
            will(returnValue(referers));
        }});

        clusterGroup.setSandboxClusters(sandboxClusters);
        clusterGroup.setGreyClusters(greyClusters);
        clusterGroup.init();

        final Request testRequest = new DefaultRequest();
        final Response sandboxResponse = mockery.mock(Response.class, "sandboxResponse");
        final Response greyResponse = mockery.mock(Response.class, "greyResponse");

        mockery.checking(new Expectations() {{
            allowing(masterCluster).call(testRequest);
            will(returnValue(response));
            allowing(sandboxCluster).call(testRequest);
            will(returnValue(sandboxResponse));
            allowing(greyCluster).call(testRequest);
            will(returnValue(greyResponse));
        }});

        // Case 1: masterCluster should be called when not has ROUTE_GROUP_KEY
        Response result = clusterGroup.call(testRequest);
        assertSame(response, result);

        // Case 2: masterCluster should be called when sandboxCluster has empty referers
        testRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX);
        result = clusterGroup.call(testRequest);
        assertSame(response, result);

        // Case 3: sandboxCluster should be called when sandboxCluster has referers
        testRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX);
        referers.add(new MockReferer<>());
        result = clusterGroup.call(testRequest);
        assertSame(sandboxResponse, result);

        // Case 4: masterCluster should be called when greyCluster has empty referers
        referers.clear();
        testRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, "other-group, grey-group");
        result = clusterGroup.call(testRequest);
        assertSame(response, result);

        // Case 5: greyCluster should be called when greyCluster has referers
        referers.add(new MockReferer<>());
        testRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, "other-group, grey-group");
        result = clusterGroup.call(testRequest);
        assertSame(greyResponse, result);

        // Case 6: sandboxCluster should be called. The priority is determined by the order of group names in the value.
        testRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, "other-group, sandbox-group, grey-group");
        result = clusterGroup.call(testRequest);
        assertSame(sandboxResponse, result);

        // Case 7: greyCluster should be called. The priority is determined by the order of group names in the value.
        testRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, "other-group, grey-group, sandbox-group");
        result = clusterGroup.call(testRequest);
        assertSame(greyResponse, result);
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

        greyClusters = new ArrayList<>();
        Cluster<Object> greyCluster = mockery.mock(Cluster.class, "greyCluster");
        greyClusters.add(greyCluster);

        final URL url = new URL("motan", "localhost", 8001, "testService");
        url.addParameter(URLParamType.clusterSelector.getName(), "default");

        mockery.checking(new Expectations() {{
            allowing(masterCluster).getUrl();
            will(returnValue(url));
            oneOf(sandboxCluster).getUrl();
            will(returnValue(new URL("motan", "localhost", 8002, "testService")));
            oneOf(sandboxCluster).getReferers();
            will(returnValue(new ArrayList<>()));
            oneOf(greyCluster).getUrl();
            will(returnValue(new URL("motan", "localhost", 8003, "testService")));
            oneOf(greyCluster).getReferers();
            will(returnValue(new ArrayList<>()));
        }});

        clusterGroup.setSandboxClusters(sandboxClusters);
        clusterGroup.setGreyClusters(greyClusters);
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