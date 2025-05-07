package com.weibo.api.motan.cluster.group;

import java.util.ArrayList;
import java.util.List;

import org.jmock.Expectations;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
import com.weibo.api.motan.rpc.DefaultRequest;
import com.weibo.api.motan.rpc.Referer;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.URL;

public class DefaultClusterSelectorTest extends BaseTestCase {
    private DefaultClusterSelector<Object> selector;
    private ClusterGroup<Object> clusterGroup;
    private Cluster<Object> masterCluster;
    private Cluster<Object> sandboxCluster;
    private Request request;

    @Override
    @SuppressWarnings("unchecked")
    public void setUp() throws Exception {
        super.setUp();
        selector = new DefaultClusterSelector<>();
        clusterGroup = mockery.mock(ClusterGroup.class);
        masterCluster = mockery.mock(Cluster.class, "masterCluster");
        sandboxCluster = mockery.mock(Cluster.class, "sandboxCluster");
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
            }    
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNull(selector.defaultSandboxCluster);

        Cluster<?> cluster = selector.select(request);
        assertSame(masterCluster, cluster);
    }

    public void testInitWithEmptySandboxClusters() {
        mockery.checking(new Expectations() {
            {
                oneOf(clusterGroup).getSandboxClusters();
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

        Cluster<?> cluster = selector.select(request);
        assertSame(masterCluster, cluster);
    }

    @SuppressWarnings("unchecked")
    public void testInitWithSandboxClusters() {
        final List<Cluster<Object>> sandboxClusters = new ArrayList<>();
        sandboxClusters.add(sandboxCluster);
        Cluster<Object> sandboxCluster2 = mockery.mock(Cluster.class, "sandboxCluster2");
        sandboxClusters.add(sandboxCluster2);
        Cluster<Object> sandboxCluster3 = mockery.mock(Cluster.class, "sandboxCluster3");
        sandboxClusters.add(sandboxCluster3);
        final URL sandboxUrl = new URL("motan", "localhost", 8001, "testService");
        sandboxUrl.addParameter("group", "testGroup");
        final URL sandboxUrl2 = new URL("motan", "localhost", 8001, "testService");
        sandboxUrl2.addParameter("group", "testGroup2");
        final URL sandboxUrl3 = new URL("motan", "localhost", 8001, "testService");
        sandboxUrl3.addParameter("group", "testGroup3");
        List<Referer<Object>> referers = new ArrayList<>();
        Referer<Object> referer = mockery.mock(Referer.class, "referer");
        referers.add(referer);

        mockery.checking(new Expectations() {
            {
                allowing(clusterGroup).getMasterCluster();
                will(returnValue(masterCluster));
                oneOf(clusterGroup).getSandboxClusters();
                will(returnValue(sandboxClusters));
                oneOf(sandboxCluster).getUrl();
                will(returnValue(sandboxUrl));
                oneOf(sandboxCluster2).getUrl();
                will(returnValue(sandboxUrl2));
                oneOf(sandboxCluster3).getUrl();
                will(returnValue(sandboxUrl3));
                allowing(sandboxCluster).getReferers();
                will(returnValue(referers));
                allowing(sandboxCluster2).getReferers();
                will(returnValue(referers));
                allowing(sandboxCluster3).getReferers();
                will(returnValue(new ArrayList<>())); // empty referers
            }
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNotNull(selector.defaultSandboxCluster);
        assertSame(sandboxCluster, selector.defaultSandboxCluster);

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
                allowing(clusterGroup).getUrl();
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