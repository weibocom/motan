package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;
import org.jmock.Expectations;

import java.util.ArrayList;
import java.util.List;

public class DefaultClusterSelectorTest extends BaseTestCase {
    private DefaultClusterSelector<Object> selector;
    private ClusterGroup<Object> clusterGroup;
    private Cluster<Object> masterCluster;
    private Cluster<Object> sandboxCluster;
    private Cluster<Object> greyCluster;
    private Request request;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        selector = new DefaultClusterSelector<>();
        clusterGroup = mockery.mock(ClusterGroup.class);
        masterCluster = mockery.mock(Cluster.class, "masterCluster");
        sandboxCluster = mockery.mock(Cluster.class, "sandboxCluster");
        greyCluster = mockery.mock(Cluster.class, "greyCluster");
        request = mockery.mock(Request.class);
    }

    public void testInitWithNullClusterGroup() {
        try {
            selector.init(null);
            fail("Should throw exception when init with null cluster group");
        } catch (MotanFrameworkException e) {
            assertTrue(e.getMessage().contains("ClusterGroup cannot be null"));
        }
    }

    public void testInitWithoutSandboxClusters() {
        mockery.checking(new Expectations() {
            {
                oneOf(clusterGroup).getSandboxClusters();
                will(returnValue(null));
                oneOf(clusterGroup).getMasterCluster();
                will(returnValue(masterCluster));
                oneOf(clusterGroup).getGreyClusters();
                will(returnValue(null));
                oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
                will(returnValue(DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX));
            }
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNull(selector.defaultSandboxCluster);
        assertNull(selector.defaultGreyCluster);

        Cluster<?> cluster = selector.select(request);
        assertSame(masterCluster, cluster);
    }

    public void testInitWithEmptySandboxClusters() {
        mockery.checking(new Expectations() {
            {
                oneOf(clusterGroup).getSandboxClusters();
                will(returnValue(new ArrayList<>()));
                oneOf(clusterGroup).getGreyClusters();
                will(returnValue(new ArrayList<>()));
                oneOf(clusterGroup).getMasterCluster();
                will(returnValue(masterCluster));
                oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
                will(returnValue(DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX));
            }
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNull(selector.defaultSandboxCluster);
        assertNull(selector.defaultGreyCluster);

        Cluster<?> cluster = selector.select(request);
        assertSame(masterCluster, cluster);
    }

    @SuppressWarnings("unchecked")
    public void testInitWithSandboxClusters() {
        final List<Cluster<Object>> sandboxClusters = new ArrayList<>();
        sandboxClusters.add(sandboxCluster);
        final URL sandboxUrl = new URL("motan", "localhost", 8001, "testService");
        sandboxUrl.addParameter("group", "testGroup");
        List<Referer<Object>> referers = new ArrayList<>();
        Referer<Object> referer = mockery.mock(Referer.class, "referer");
        referers.add(referer);

        final List<Cluster<Object>> greyClusters = new ArrayList<>();
        greyClusters.add(greyCluster);

        mockery.checking(new Expectations() {
            {
                allowing(clusterGroup).getMasterCluster();
                will(returnValue(masterCluster));
                oneOf(clusterGroup).getSandboxClusters();
                will(returnValue(sandboxClusters));
                oneOf(clusterGroup).getGreyClusters();
                will(returnValue(greyClusters));
                allowing(sandboxCluster).getUrl();
                will(returnValue(sandboxUrl));
                allowing(sandboxCluster).getReferers();
                will(returnValue(referers));
                allowing(greyCluster).getUrl();
                will(returnValue(sandboxUrl));
                allowing(greyCluster).getReferers();
                will(returnValue(referers));
            }
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNotNull(selector.defaultSandboxCluster);
        assertSame(sandboxCluster, selector.defaultSandboxCluster);
        assertSame(greyCluster, selector.defaultGreyCluster);

        DefaultRequest defaultRequest = new DefaultRequest();
        // without route group
        Cluster<Object> selected = selector.select(defaultRequest);
        assertSame(masterCluster, selected);

        // with default sandbox group
        defaultRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY,
                DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX);
        selected = selector.select(defaultRequest);
        assertSame(sandboxCluster, selected);

        // with specific sandbox group
        // Test URL group matching
        final URL clusterGroupUrl = new URL("motan", "localhost", 8001, "testService");
        clusterGroupUrl.addParameter("group", "testGroup");

        mockery.checking(new Expectations() {
            {
                allowing(sandboxCluster).getUrl();
                will(returnValue(clusterGroupUrl));
            }
        });

        defaultRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, "testGroup");
        selected = selector.select(defaultRequest);
        assertSame(sandboxCluster, selected);

        // Test comma-separated group list
        defaultRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, "otherGroup,testGroup,anotherGroup,  ,,");
        selected = selector.select(defaultRequest);
        assertSame(sandboxCluster, selected);

        // Test cluster with empty references
        referers.clear();

        selected = selector.select(defaultRequest);
        assertSame(masterCluster, selected);

        // with invalid route group
        referers.add(referer); // referers not empty
        defaultRequest.setAttachment(MotanConstants.ROUTE_GROUP_KEY, ",, ,invalidGroup");
        selected = selector.select(defaultRequest);
        assertSame(masterCluster, selected);
    }
}