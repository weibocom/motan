package com.weibo.api.motan.cluster.group;

import com.weibo.api.motan.BaseTestCase;
import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.exception.MotanFrameworkException;
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
            }
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNull(selector.routeGroups);
    }

    public void testInitWithSandboxClusters() {
        final List<Cluster<Object>> sandboxClusters = new ArrayList<>();
        sandboxClusters.add(sandboxCluster);
        final URL sandboxUrl = new URL("motan", "localhost", 8001, "testService");
        sandboxUrl.addParameter("group", "testGroup");

        mockery.checking(new Expectations() {
            {
                oneOf(clusterGroup).getSandboxClusters();
                will(returnValue(sandboxClusters));
                oneOf(sandboxCluster).getUrl();
                will(returnValue(sandboxUrl));
            }
        });

        selector.init(clusterGroup);
        assertSame(clusterGroup, selector.clusterGroup);
        assertNotNull(selector.routeGroups);
        assertEquals(2, selector.routeGroups.size());
        assertSame(sandboxCluster, selector.routeGroups.get(DefaultClusterSelector.DEFAULT_ROUTE_GROUP_SANDBOX));
        assertSame(sandboxCluster, selector.routeGroups.get("testGroup"));
    }

    public void testSelectWithoutRouteGroup() {
        mockery.checking(new Expectations() {
            {
                oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
                will(returnValue(null));
                oneOf(clusterGroup).getMasterCluster();
                will(returnValue(masterCluster));
            }
        });

        selector.init(clusterGroup);
        Cluster<Object> selected = selector.select(request);
        assertSame(masterCluster, selected);
    }

    @SuppressWarnings("unchecked")
    public void testSelectWithValidRouteGroup() {
        final List<Cluster<Object>> sandboxClusters = new ArrayList<>();
        sandboxClusters.add(sandboxCluster);
        final URL sandboxUrl = new URL("motan", "localhost", 8001, "testService");
        sandboxUrl.addParameter("group", "testGroup");
        final List<Referer<Object>> referers = new ArrayList<>();
        referers.add(mockery.mock(Referer.class));

        mockery.checking(new Expectations() {
            {
                oneOf(clusterGroup).getSandboxClusters();
                will(returnValue(sandboxClusters));
                oneOf(sandboxCluster).getUrl();
                will(returnValue(sandboxUrl));
                oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
                will(returnValue("testGroup"));
                oneOf(sandboxCluster).getReferers();
                will(returnValue(referers));
            }
        });

        selector.init(clusterGroup);
        Cluster<Object> selected = selector.select(request);
        assertSame(sandboxCluster, selected);
    }

    public void testSelectWithInvalidRouteGroup() {
        mockery.checking(new Expectations() {
            {
                oneOf(request).getAttachment(MotanConstants.ROUTE_GROUP_KEY);
                will(returnValue("invalidGroup"));
                oneOf(clusterGroup).getMasterCluster();
                will(returnValue(masterCluster));
            }
        });

        selector.init(clusterGroup);
        Cluster<Object> selected = selector.select(request);
        assertSame(masterCluster, selected);
    }

    public void testDestroy() {
        selector.destroy();
        // Currently destroy() is empty, just call it for coverage
    }
}