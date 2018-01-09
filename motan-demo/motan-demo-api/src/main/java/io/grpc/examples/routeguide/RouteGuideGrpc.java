package io.grpc.examples.routeguide;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.*;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.*;

/**
 * <pre>
 * Interface exported by the server.
 * </pre>
 */
@javax.annotation.Generated(
        value = "by gRPC proto compiler (version 1.0.0)",
        comments = "Source: route_guide.proto")
public class RouteGuideGrpc {

    public static final String SERVICE_NAME = "routeguide.RouteGuide";
    // Static method descriptors that strictly reflect the proto.
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
    public static final io.grpc.MethodDescriptor<io.grpc.examples.routeguide.Point,
            io.grpc.examples.routeguide.Feature> METHOD_GET_FEATURE =
            io.grpc.MethodDescriptor.create(
                    io.grpc.MethodDescriptor.MethodType.UNARY,
                    generateFullMethodName(
                            "routeguide.RouteGuide", "GetFeature"),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.Point.getDefaultInstance()),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.Feature.getDefaultInstance()));
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
    public static final io.grpc.MethodDescriptor<io.grpc.examples.routeguide.Rectangle,
            io.grpc.examples.routeguide.Feature> METHOD_LIST_FEATURES =
            io.grpc.MethodDescriptor.create(
                    io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING,
                    generateFullMethodName(
                            "routeguide.RouteGuide", "ListFeatures"),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.Rectangle.getDefaultInstance()),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.Feature.getDefaultInstance()));
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
    public static final io.grpc.MethodDescriptor<io.grpc.examples.routeguide.Point,
            io.grpc.examples.routeguide.RouteSummary> METHOD_RECORD_ROUTE =
            io.grpc.MethodDescriptor.create(
                    io.grpc.MethodDescriptor.MethodType.CLIENT_STREAMING,
                    generateFullMethodName(
                            "routeguide.RouteGuide", "RecordRoute"),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.Point.getDefaultInstance()),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.RouteSummary.getDefaultInstance()));
    @io.grpc.ExperimentalApi("https://github.com/grpc/grpc-java/issues/1901")
    public static final io.grpc.MethodDescriptor<io.grpc.examples.routeguide.RouteNote,
            io.grpc.examples.routeguide.RouteNote> METHOD_ROUTE_CHAT =
            io.grpc.MethodDescriptor.create(
                    io.grpc.MethodDescriptor.MethodType.BIDI_STREAMING,
                    generateFullMethodName(
                            "routeguide.RouteGuide", "RouteChat"),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.RouteNote.getDefaultInstance()),
                    io.grpc.protobuf.ProtoUtils.marshaller(io.grpc.examples.routeguide.RouteNote.getDefaultInstance()));
    private static final int METHODID_GET_FEATURE = 0;
    private static final int METHODID_LIST_FEATURES = 1;
    private static final int METHODID_RECORD_ROUTE = 2;
    private static final int METHODID_ROUTE_CHAT = 3;

    private RouteGuideGrpc() {
    }

    /**
     * Creates a new async stub that supports all call types for the service
     */
    public static RouteGuideStub newStub(io.grpc.Channel channel) {
        return new RouteGuideStub(channel);
    }

    /**
     * Creates a new blocking-style stub that supports unary and streaming output calls on the service
     */
    public static RouteGuideBlockingStub newBlockingStub(
            io.grpc.Channel channel) {
        return new RouteGuideBlockingStub(channel);
    }

    /**
     * Creates a new ListenableFuture-style stub that supports unary and streaming output calls on the service
     */
    public static RouteGuideFutureStub newFutureStub(
            io.grpc.Channel channel) {
        return new RouteGuideFutureStub(channel);
    }

    public static io.grpc.ServiceDescriptor getServiceDescriptor() {
        return new io.grpc.ServiceDescriptor(SERVICE_NAME,
                METHOD_GET_FEATURE,
                METHOD_LIST_FEATURES,
                METHOD_RECORD_ROUTE,
                METHOD_ROUTE_CHAT);
    }

    /**
     * <pre>
     * Interface exported by the server.
     * </pre>
     */
    public static abstract class RouteGuideImplBase implements io.grpc.BindableService {

        /**
         * <pre>
         * A simple RPC.
         * Obtains the feature at a given position.
         * A feature with an empty name is returned if there's no feature at the given
         * position.
         * </pre>
         */
        public void getFeature(io.grpc.examples.routeguide.Point request,
                               io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Feature> responseObserver) {
            asyncUnimplementedUnaryCall(METHOD_GET_FEATURE, responseObserver);
        }

        /**
         * <pre>
         * A server-to-client streaming RPC.
         * Obtains the Features available within the given Rectangle.  Results are
         * streamed rather than returned at once (e.g. in a response message with a
         * repeated field), as the rectangle may cover a large area and contain a
         * huge number of features.
         * </pre>
         */
        public void listFeatures(io.grpc.examples.routeguide.Rectangle request,
                                 io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Feature> responseObserver) {
            asyncUnimplementedUnaryCall(METHOD_LIST_FEATURES, responseObserver);
        }

        /**
         * <pre>
         * A client-to-server streaming RPC.
         * Accepts a stream of Points on a route being traversed, returning a
         * RouteSummary when traversal is completed.
         * </pre>
         */
        public io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Point> recordRoute(
                io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteSummary> responseObserver) {
            return asyncUnimplementedStreamingCall(METHOD_RECORD_ROUTE, responseObserver);
        }

        /**
         * <pre>
         * A Bidirectional streaming RPC.
         * Accepts a stream of RouteNotes sent while a route is being traversed,
         * while receiving other RouteNotes (e.g. from other users).
         * </pre>
         */
        public io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> routeChat(
                io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> responseObserver) {
            return asyncUnimplementedStreamingCall(METHOD_ROUTE_CHAT, responseObserver);
        }

        @java.lang.Override
        public io.grpc.ServerServiceDefinition bindService() {
            return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
                    .addMethod(
                            METHOD_GET_FEATURE,
                            asyncUnaryCall(
                                    new MethodHandlers<
                                            io.grpc.examples.routeguide.Point,
                                            io.grpc.examples.routeguide.Feature>(
                                            this, METHODID_GET_FEATURE)))
                    .addMethod(
                            METHOD_LIST_FEATURES,
                            asyncServerStreamingCall(
                                    new MethodHandlers<
                                            io.grpc.examples.routeguide.Rectangle,
                                            io.grpc.examples.routeguide.Feature>(
                                            this, METHODID_LIST_FEATURES)))
                    .addMethod(
                            METHOD_RECORD_ROUTE,
                            asyncClientStreamingCall(
                                    new MethodHandlers<
                                            io.grpc.examples.routeguide.Point,
                                            io.grpc.examples.routeguide.RouteSummary>(
                                            this, METHODID_RECORD_ROUTE)))
                    .addMethod(
                            METHOD_ROUTE_CHAT,
                            asyncBidiStreamingCall(
                                    new MethodHandlers<
                                            io.grpc.examples.routeguide.RouteNote,
                                            io.grpc.examples.routeguide.RouteNote>(
                                            this, METHODID_ROUTE_CHAT)))
                    .build();
        }
    }

    /**
     * <pre>
     * Interface exported by the server.
     * </pre>
     */
    public static final class RouteGuideStub extends io.grpc.stub.AbstractStub<RouteGuideStub> {
        private RouteGuideStub(io.grpc.Channel channel) {
            super(channel);
        }

        private RouteGuideStub(io.grpc.Channel channel,
                               io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected RouteGuideStub build(io.grpc.Channel channel,
                                       io.grpc.CallOptions callOptions) {
            return new RouteGuideStub(channel, callOptions);
        }

        /**
         * <pre>
         * A simple RPC.
         * Obtains the feature at a given position.
         * A feature with an empty name is returned if there's no feature at the given
         * position.
         * </pre>
         */
        public void getFeature(io.grpc.examples.routeguide.Point request,
                               io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Feature> responseObserver) {
            asyncUnaryCall(
                    getChannel().newCall(METHOD_GET_FEATURE, getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * A server-to-client streaming RPC.
         * Obtains the Features available within the given Rectangle.  Results are
         * streamed rather than returned at once (e.g. in a response message with a
         * repeated field), as the rectangle may cover a large area and contain a
         * huge number of features.
         * </pre>
         */
        public void listFeatures(io.grpc.examples.routeguide.Rectangle request,
                                 io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Feature> responseObserver) {
            asyncServerStreamingCall(
                    getChannel().newCall(METHOD_LIST_FEATURES, getCallOptions()), request, responseObserver);
        }

        /**
         * <pre>
         * A client-to-server streaming RPC.
         * Accepts a stream of Points on a route being traversed, returning a
         * RouteSummary when traversal is completed.
         * </pre>
         */
        public io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Point> recordRoute(
                io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteSummary> responseObserver) {
            return asyncClientStreamingCall(
                    getChannel().newCall(METHOD_RECORD_ROUTE, getCallOptions()), responseObserver);
        }

        /**
         * <pre>
         * A Bidirectional streaming RPC.
         * Accepts a stream of RouteNotes sent while a route is being traversed,
         * while receiving other RouteNotes (e.g. from other users).
         * </pre>
         */
        public io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> routeChat(
                io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote> responseObserver) {
            return asyncBidiStreamingCall(
                    getChannel().newCall(METHOD_ROUTE_CHAT, getCallOptions()), responseObserver);
        }
    }

    /**
     * <pre>
     * Interface exported by the server.
     * </pre>
     */
    public static final class RouteGuideBlockingStub extends io.grpc.stub.AbstractStub<RouteGuideBlockingStub> {
        private RouteGuideBlockingStub(io.grpc.Channel channel) {
            super(channel);
        }

        private RouteGuideBlockingStub(io.grpc.Channel channel,
                                       io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected RouteGuideBlockingStub build(io.grpc.Channel channel,
                                               io.grpc.CallOptions callOptions) {
            return new RouteGuideBlockingStub(channel, callOptions);
        }

        /**
         * <pre>
         * A simple RPC.
         * Obtains the feature at a given position.
         * A feature with an empty name is returned if there's no feature at the given
         * position.
         * </pre>
         */
        public io.grpc.examples.routeguide.Feature getFeature(io.grpc.examples.routeguide.Point request) {
            return blockingUnaryCall(
                    getChannel(), METHOD_GET_FEATURE, getCallOptions(), request);
        }

        /**
         * <pre>
         * A server-to-client streaming RPC.
         * Obtains the Features available within the given Rectangle.  Results are
         * streamed rather than returned at once (e.g. in a response message with a
         * repeated field), as the rectangle may cover a large area and contain a
         * huge number of features.
         * </pre>
         */
        public java.util.Iterator<io.grpc.examples.routeguide.Feature> listFeatures(
                io.grpc.examples.routeguide.Rectangle request) {
            return blockingServerStreamingCall(
                    getChannel(), METHOD_LIST_FEATURES, getCallOptions(), request);
        }
    }

    /**
     * <pre>
     * Interface exported by the server.
     * </pre>
     */
    public static final class RouteGuideFutureStub extends io.grpc.stub.AbstractStub<RouteGuideFutureStub> {
        private RouteGuideFutureStub(io.grpc.Channel channel) {
            super(channel);
        }

        private RouteGuideFutureStub(io.grpc.Channel channel,
                                     io.grpc.CallOptions callOptions) {
            super(channel, callOptions);
        }

        @java.lang.Override
        protected RouteGuideFutureStub build(io.grpc.Channel channel,
                                             io.grpc.CallOptions callOptions) {
            return new RouteGuideFutureStub(channel, callOptions);
        }

        /**
         * <pre>
         * A simple RPC.
         * Obtains the feature at a given position.
         * A feature with an empty name is returned if there's no feature at the given
         * position.
         * </pre>
         */
        public com.google.common.util.concurrent.ListenableFuture<io.grpc.examples.routeguide.Feature> getFeature(
                io.grpc.examples.routeguide.Point request) {
            return futureUnaryCall(
                    getChannel().newCall(METHOD_GET_FEATURE, getCallOptions()), request);
        }
    }

    private static class MethodHandlers<Req, Resp> implements
            io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
            io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
        private final RouteGuideImplBase serviceImpl;
        private final int methodId;

        public MethodHandlers(RouteGuideImplBase serviceImpl, int methodId) {
            this.serviceImpl = serviceImpl;
            this.methodId = methodId;
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_GET_FEATURE:
                    serviceImpl.getFeature((io.grpc.examples.routeguide.Point) request,
                            (io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Feature>) responseObserver);
                    break;
                case METHODID_LIST_FEATURES:
                    serviceImpl.listFeatures((io.grpc.examples.routeguide.Rectangle) request,
                            (io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.Feature>) responseObserver);
                    break;
                default:
                    throw new AssertionError();
            }
        }

        @java.lang.Override
        @java.lang.SuppressWarnings("unchecked")
        public io.grpc.stub.StreamObserver<Req> invoke(
                io.grpc.stub.StreamObserver<Resp> responseObserver) {
            switch (methodId) {
                case METHODID_RECORD_ROUTE:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl.recordRoute(
                            (io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteSummary>) responseObserver);
                case METHODID_ROUTE_CHAT:
                    return (io.grpc.stub.StreamObserver<Req>) serviceImpl.routeChat(
                            (io.grpc.stub.StreamObserver<io.grpc.examples.routeguide.RouteNote>) responseObserver);
                default:
                    throw new AssertionError();
            }
        }
    }

}
