/*
 * Copyright 2009-2016 Weibo, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.weibo.motan.demo.service;

import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.Rectangle;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.examples.routeguide.RouteSummary;
import io.grpc.stub.StreamObserver;

import com.weibo.api.motan.protocol.grpc.annotation.GrpcConfig;

@GrpcConfig(grpc = "io.grpc.examples.routeguide.RouteGuideGrpc")
public interface GrpcService {
    public Feature getFeature(Point request);

    public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver);

    public StreamObserver<Point> recordRoute(final StreamObserver<RouteSummary> responseObserver);

    public StreamObserver<RouteNote> routeChat(final StreamObserver<RouteNote> responseObserver);

}
