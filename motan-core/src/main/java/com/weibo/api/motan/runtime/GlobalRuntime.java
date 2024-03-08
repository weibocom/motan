/*
 *
 *   Copyright 2009-2024 Weibo, Inc.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 *
 */

package com.weibo.api.motan.runtime;

import com.weibo.api.motan.cluster.Cluster;
import com.weibo.api.motan.registry.Registry;
import com.weibo.api.motan.rpc.Exporter;
import com.weibo.api.motan.transport.MeshClient;
import com.weibo.api.motan.transport.Server;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @author zhanglei28
 * @date 2024/2/29.
 */
public class GlobalRuntime {
    // all runtime registries
    private static final ConcurrentHashMap<String, Registry> runtimeRegistries = new ConcurrentHashMap<>();

    // all runtime exporters
    private static final ConcurrentHashMap<String, Exporter<?>> runtimeExporters = new ConcurrentHashMap<>();

    // all runtime clusters
    private static final ConcurrentHashMap<String, Cluster<?>> runtimeClusters = new ConcurrentHashMap<>();

    // all runtime meshClients
    private static final ConcurrentHashMap<String, MeshClient> runtimeMeshClients = new ConcurrentHashMap<>();

    // all runtime servers
    private static final ConcurrentHashMap<String, Server> runtimeServers = new ConcurrentHashMap<>();

    // add runtime registry
    public static void addRegistry(String id, Registry registry) {
        runtimeRegistries.put(id, registry);
    }

    // remove runtime registry
    public static Registry removeRegistry(String id) {
        return runtimeRegistries.remove(id);
    }

    // add runtime exporter
    public static void addExporter(String id, Exporter<?> exporter) {
        runtimeExporters.put(id, exporter);
    }

    // remove runtime exporter
    public static Exporter<?> removeExporter(String id) {
        return runtimeExporters.remove(id);
    }

    // add runtime cluster
    public static void addCluster(String id, Cluster<?> cluster) {
        runtimeClusters.put(id, cluster);
    }

    // remove runtime cluster
    public static Cluster<?> removeCluster(String id) {
        return runtimeClusters.remove(id);
    }

    // add runtime mesh client
    public static void addMeshClient(String id, MeshClient meshClient) {
        runtimeMeshClients.put(id, meshClient);
    }

    // remove runtime mesh client
    public static MeshClient removeMeshClient(String id) {
        return runtimeMeshClients.remove(id);
    }

    // add runtime server
    public static void addServer(String id, Server server) {
        runtimeServers.put(id, server);
    }

    // remove runtime server
    public static Server removeServer(String id) {
        return runtimeServers.remove(id);
    }

    public static ConcurrentHashMap<String, Registry> getRuntimeRegistries() {
        return runtimeRegistries;
    }

    public static ConcurrentHashMap<String, Exporter<?>> getRuntimeExporters() {
        return runtimeExporters;
    }

    public static ConcurrentHashMap<String, Cluster<?>> getRuntimeClusters() {
        return runtimeClusters;
    }

    public static ConcurrentHashMap<String, MeshClient> getRuntimeMeshClients() {
        return runtimeMeshClients;
    }

    public static ConcurrentHashMap<String, Server> getRuntimeServers() {
        return runtimeServers;
    }
}
