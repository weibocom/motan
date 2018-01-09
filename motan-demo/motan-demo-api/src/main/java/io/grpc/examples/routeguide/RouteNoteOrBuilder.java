// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: route_guide.proto

package io.grpc.examples.routeguide;

public interface RouteNoteOrBuilder extends
        // @@protoc_insertion_point(interface_extends:routeguide.RouteNote)
        com.google.protobuf.MessageOrBuilder {

    /**
     * <pre>
     * The location from which the message is sent.
     * </pre>
     * <p>
     * <code>optional .routeguide.Point location = 1;</code>
     */
    boolean hasLocation();

    /**
     * <pre>
     * The location from which the message is sent.
     * </pre>
     * <p>
     * <code>optional .routeguide.Point location = 1;</code>
     */
    io.grpc.examples.routeguide.Point getLocation();

    /**
     * <pre>
     * The location from which the message is sent.
     * </pre>
     * <p>
     * <code>optional .routeguide.Point location = 1;</code>
     */
    io.grpc.examples.routeguide.PointOrBuilder getLocationOrBuilder();

    /**
     * <pre>
     * The message to be sent.
     * </pre>
     * <p>
     * <code>optional string message = 2;</code>
     */
    java.lang.String getMessage();

    /**
     * <pre>
     * The message to be sent.
     * </pre>
     * <p>
     * <code>optional string message = 2;</code>
     */
    com.google.protobuf.ByteString
    getMessageBytes();
}
