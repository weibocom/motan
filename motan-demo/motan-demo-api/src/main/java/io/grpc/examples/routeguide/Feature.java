// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: route_guide.proto

package io.grpc.examples.routeguide;

/**
 * <pre>
 * A feature names something at a given point.
 * If a feature could not be named, the name is empty.
 * </pre>
 * <p>
 * Protobuf type {@code routeguide.Feature}
 */
public final class Feature extends
        com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:routeguide.Feature)
        FeatureOrBuilder {
    public static final int NAME_FIELD_NUMBER = 1;
    public static final int LOCATION_FIELD_NUMBER = 2;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:routeguide.Feature)
    private static final io.grpc.examples.routeguide.Feature DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<Feature>
            PARSER = new com.google.protobuf.AbstractParser<Feature>() {
        public Feature parsePartialFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return new Feature(input, extensionRegistry);
        }
    };

    static {
        DEFAULT_INSTANCE = new io.grpc.examples.routeguide.Feature();
    }

    private volatile java.lang.Object name_;
    private io.grpc.examples.routeguide.Point location_;
    private byte memoizedIsInitialized = -1;

    // Use Feature.newBuilder() to construct.
    private Feature(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private Feature() {
        name_ = "";
    }

    private Feature(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        this();
        int mutable_bitField0_ = 0;
        try {
            boolean done = false;
            while (!done) {
                int tag = input.readTag();
                switch (tag) {
                    case 0:
                        done = true;
                        break;
                    default: {
                        if (!input.skipField(tag)) {
                            done = true;
                        }
                        break;
                    }
                    case 10: {
                        java.lang.String s = input.readStringRequireUtf8();

                        name_ = s;
                        break;
                    }
                    case 18: {
                        io.grpc.examples.routeguide.Point.Builder subBuilder = null;
                        if (location_ != null) {
                            subBuilder = location_.toBuilder();
                        }
                        location_ = input.readMessage(io.grpc.examples.routeguide.Point.parser(), extensionRegistry);
                        if (subBuilder != null) {
                            subBuilder.mergeFrom(location_);
                            location_ = subBuilder.buildPartial();
                        }

                        break;
                    }
                }
            }
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
            throw e.setUnfinishedMessage(this);
        } catch (java.io.IOException e) {
            throw new com.google.protobuf.InvalidProtocolBufferException(
                    e).setUnfinishedMessage(this);
        } finally {
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
        return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_Feature_descriptor;
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.Feature parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input);
    }

    public static io.grpc.examples.routeguide.Feature parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static io.grpc.examples.routeguide.Feature parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(io.grpc.examples.routeguide.Feature prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static io.grpc.examples.routeguide.Feature getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<Feature> parser() {
        return PARSER;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
        return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
        return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_Feature_fieldAccessorTable
                .ensureFieldAccessorsInitialized(
                        io.grpc.examples.routeguide.Feature.class, io.grpc.examples.routeguide.Feature.Builder.class);
    }

    /**
     * <pre>
     * The name of the feature.
     * </pre>
     * <p>
     * <code>optional string name = 1;</code>
     */
    public java.lang.String getName() {
        java.lang.Object ref = name_;
        if (ref instanceof java.lang.String) {
            return (java.lang.String) ref;
        } else {
            com.google.protobuf.ByteString bs =
                    (com.google.protobuf.ByteString) ref;
            java.lang.String s = bs.toStringUtf8();
            name_ = s;
            return s;
        }
    }

    /**
     * <pre>
     * The name of the feature.
     * </pre>
     * <p>
     * <code>optional string name = 1;</code>
     */
    public com.google.protobuf.ByteString
    getNameBytes() {
        java.lang.Object ref = name_;
        if (ref instanceof java.lang.String) {
            com.google.protobuf.ByteString b =
                    com.google.protobuf.ByteString.copyFromUtf8(
                            (java.lang.String) ref);
            name_ = b;
            return b;
        } else {
            return (com.google.protobuf.ByteString) ref;
        }
    }

    /**
     * <pre>
     * The point where the feature is detected.
     * </pre>
     * <p>
     * <code>optional .routeguide.Point location = 2;</code>
     */
    public boolean hasLocation() {
        return location_ != null;
    }

    /**
     * <pre>
     * The point where the feature is detected.
     * </pre>
     * <p>
     * <code>optional .routeguide.Point location = 2;</code>
     */
    public io.grpc.examples.routeguide.Point getLocation() {
        return location_ == null ? io.grpc.examples.routeguide.Point.getDefaultInstance() : location_;
    }

    /**
     * <pre>
     * The point where the feature is detected.
     * </pre>
     * <p>
     * <code>optional .routeguide.Point location = 2;</code>
     */
    public io.grpc.examples.routeguide.PointOrBuilder getLocationOrBuilder() {
        return getLocation();
    }

    public final boolean isInitialized() {
        byte isInitialized = memoizedIsInitialized;
        if (isInitialized == 1) return true;
        if (isInitialized == 0) return false;

        memoizedIsInitialized = 1;
        return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
            throws java.io.IOException {
        if (!getNameBytes().isEmpty()) {
            com.google.protobuf.GeneratedMessageV3.writeString(output, 1, name_);
        }
        if (location_ != null) {
            output.writeMessage(2, getLocation());
        }
    }

    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        if (!getNameBytes().isEmpty()) {
            size += com.google.protobuf.GeneratedMessageV3.computeStringSize(1, name_);
        }
        if (location_ != null) {
            size += com.google.protobuf.CodedOutputStream
                    .computeMessageSize(2, getLocation());
        }
        memoizedSize = size;
        return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof io.grpc.examples.routeguide.Feature)) {
            return super.equals(obj);
        }
        io.grpc.examples.routeguide.Feature other = (io.grpc.examples.routeguide.Feature) obj;

        boolean result = true;
        result = result && getName()
                .equals(other.getName());
        result = result && (hasLocation() == other.hasLocation());
        if (hasLocation()) {
            result = result && getLocation()
                    .equals(other.getLocation());
        }
        return result;
    }

    @java.lang.Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptorForType().hashCode();
        hash = (37 * hash) + NAME_FIELD_NUMBER;
        hash = (53 * hash) + getName().hashCode();
        if (hasLocation()) {
            hash = (37 * hash) + LOCATION_FIELD_NUMBER;
            hash = (53 * hash) + getLocation().hashCode();
        }
        hash = (29 * hash) + unknownFields.hashCode();
        memoizedHashCode = hash;
        return hash;
    }

    public Builder newBuilderForType() {
        return newBuilder();
    }

    public Builder toBuilder() {
        return this == DEFAULT_INSTANCE
                ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
            com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
        Builder builder = new Builder(parent);
        return builder;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Feature> getParserForType() {
        return PARSER;
    }

    public io.grpc.examples.routeguide.Feature getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    /**
     * <pre>
     * A feature names something at a given point.
     * If a feature could not be named, the name is empty.
     * </pre>
     * <p>
     * Protobuf type {@code routeguide.Feature}
     */
    public static final class Builder extends
            com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:routeguide.Feature)
            io.grpc.examples.routeguide.FeatureOrBuilder {
        private java.lang.Object name_ = "";
        private io.grpc.examples.routeguide.Point location_ = null;
        private com.google.protobuf.SingleFieldBuilderV3<
                io.grpc.examples.routeguide.Point, io.grpc.examples.routeguide.Point.Builder, io.grpc.examples.routeguide.PointOrBuilder> locationBuilder_;

        // Construct using io.grpc.examples.routeguide.Feature.newBuilder()
        private Builder() {
            maybeForceBuilderInitialization();
        }

        private Builder(
                com.google.protobuf.GeneratedMessageV3.BuilderParent parent) {
            super(parent);
            maybeForceBuilderInitialization();
        }

        public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
            return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_Feature_descriptor;
        }

        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_Feature_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            io.grpc.examples.routeguide.Feature.class, io.grpc.examples.routeguide.Feature.Builder.class);
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3
                    .alwaysUseFieldBuilders) {
            }
        }

        public Builder clear() {
            super.clear();
            name_ = "";

            if (locationBuilder_ == null) {
                location_ = null;
            } else {
                location_ = null;
                locationBuilder_ = null;
            }
            return this;
        }

        public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
            return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_Feature_descriptor;
        }

        public io.grpc.examples.routeguide.Feature getDefaultInstanceForType() {
            return io.grpc.examples.routeguide.Feature.getDefaultInstance();
        }

        public io.grpc.examples.routeguide.Feature build() {
            io.grpc.examples.routeguide.Feature result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        public io.grpc.examples.routeguide.Feature buildPartial() {
            io.grpc.examples.routeguide.Feature result = new io.grpc.examples.routeguide.Feature(this);
            result.name_ = name_;
            if (locationBuilder_ == null) {
                result.location_ = location_;
            } else {
                result.location_ = locationBuilder_.build();
            }
            onBuilt();
            return result;
        }

        public Builder clone() {
            return (Builder) super.clone();
        }

        public Builder setField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                Object value) {
            return (Builder) super.setField(field, value);
        }

        public Builder clearField(
                com.google.protobuf.Descriptors.FieldDescriptor field) {
            return (Builder) super.clearField(field);
        }

        public Builder clearOneof(
                com.google.protobuf.Descriptors.OneofDescriptor oneof) {
            return (Builder) super.clearOneof(oneof);
        }

        public Builder setRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                int index, Object value) {
            return (Builder) super.setRepeatedField(field, index, value);
        }

        public Builder addRepeatedField(
                com.google.protobuf.Descriptors.FieldDescriptor field,
                Object value) {
            return (Builder) super.addRepeatedField(field, value);
        }

        public Builder mergeFrom(com.google.protobuf.Message other) {
            if (other instanceof io.grpc.examples.routeguide.Feature) {
                return mergeFrom((io.grpc.examples.routeguide.Feature) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(io.grpc.examples.routeguide.Feature other) {
            if (other == io.grpc.examples.routeguide.Feature.getDefaultInstance()) return this;
            if (!other.getName().isEmpty()) {
                name_ = other.name_;
                onChanged();
            }
            if (other.hasLocation()) {
                mergeLocation(other.getLocation());
            }
            onChanged();
            return this;
        }

        public final boolean isInitialized() {
            return true;
        }

        public Builder mergeFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws java.io.IOException {
            io.grpc.examples.routeguide.Feature parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (io.grpc.examples.routeguide.Feature) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        /**
         * <pre>
         * The name of the feature.
         * </pre>
         * <p>
         * <code>optional string name = 1;</code>
         */
        public java.lang.String getName() {
            java.lang.Object ref = name_;
            if (!(ref instanceof java.lang.String)) {
                com.google.protobuf.ByteString bs =
                        (com.google.protobuf.ByteString) ref;
                java.lang.String s = bs.toStringUtf8();
                name_ = s;
                return s;
            } else {
                return (java.lang.String) ref;
            }
        }

        /**
         * <pre>
         * The name of the feature.
         * </pre>
         * <p>
         * <code>optional string name = 1;</code>
         */
        public Builder setName(
                java.lang.String value) {
            if (value == null) {
                throw new NullPointerException();
            }

            name_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The name of the feature.
         * </pre>
         * <p>
         * <code>optional string name = 1;</code>
         */
        public com.google.protobuf.ByteString
        getNameBytes() {
            java.lang.Object ref = name_;
            if (ref instanceof String) {
                com.google.protobuf.ByteString b =
                        com.google.protobuf.ByteString.copyFromUtf8(
                                (java.lang.String) ref);
                name_ = b;
                return b;
            } else {
                return (com.google.protobuf.ByteString) ref;
            }
        }

        /**
         * <pre>
         * The name of the feature.
         * </pre>
         * <p>
         * <code>optional string name = 1;</code>
         */
        public Builder setNameBytes(
                com.google.protobuf.ByteString value) {
            if (value == null) {
                throw new NullPointerException();
            }
            checkByteStringIsUtf8(value);

            name_ = value;
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The name of the feature.
         * </pre>
         * <p>
         * <code>optional string name = 1;</code>
         */
        public Builder clearName() {

            name_ = getDefaultInstance().getName();
            onChanged();
            return this;
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public boolean hasLocation() {
            return locationBuilder_ != null || location_ != null;
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public io.grpc.examples.routeguide.Point getLocation() {
            if (locationBuilder_ == null) {
                return location_ == null ? io.grpc.examples.routeguide.Point.getDefaultInstance() : location_;
            } else {
                return locationBuilder_.getMessage();
            }
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public Builder setLocation(io.grpc.examples.routeguide.Point value) {
            if (locationBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                location_ = value;
                onChanged();
            } else {
                locationBuilder_.setMessage(value);
            }

            return this;
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public Builder setLocation(
                io.grpc.examples.routeguide.Point.Builder builderForValue) {
            if (locationBuilder_ == null) {
                location_ = builderForValue.build();
                onChanged();
            } else {
                locationBuilder_.setMessage(builderForValue.build());
            }

            return this;
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public Builder mergeLocation(io.grpc.examples.routeguide.Point value) {
            if (locationBuilder_ == null) {
                if (location_ != null) {
                    location_ =
                            io.grpc.examples.routeguide.Point.newBuilder(location_).mergeFrom(value).buildPartial();
                } else {
                    location_ = value;
                }
                onChanged();
            } else {
                locationBuilder_.mergeFrom(value);
            }

            return this;
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public Builder clearLocation() {
            if (locationBuilder_ == null) {
                location_ = null;
                onChanged();
            } else {
                location_ = null;
                locationBuilder_ = null;
            }

            return this;
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public io.grpc.examples.routeguide.Point.Builder getLocationBuilder() {

            onChanged();
            return getLocationFieldBuilder().getBuilder();
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        public io.grpc.examples.routeguide.PointOrBuilder getLocationOrBuilder() {
            if (locationBuilder_ != null) {
                return locationBuilder_.getMessageOrBuilder();
            } else {
                return location_ == null ?
                        io.grpc.examples.routeguide.Point.getDefaultInstance() : location_;
            }
        }

        /**
         * <pre>
         * The point where the feature is detected.
         * </pre>
         * <p>
         * <code>optional .routeguide.Point location = 2;</code>
         */
        private com.google.protobuf.SingleFieldBuilderV3<
                io.grpc.examples.routeguide.Point, io.grpc.examples.routeguide.Point.Builder, io.grpc.examples.routeguide.PointOrBuilder>
        getLocationFieldBuilder() {
            if (locationBuilder_ == null) {
                locationBuilder_ = new com.google.protobuf.SingleFieldBuilderV3<
                        io.grpc.examples.routeguide.Point, io.grpc.examples.routeguide.Point.Builder, io.grpc.examples.routeguide.PointOrBuilder>(
                        getLocation(),
                        getParentForChildren(),
                        isClean());
                location_ = null;
            }
            return locationBuilder_;
        }

        public final Builder setUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return this;
        }

        public final Builder mergeUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return this;
        }


        // @@protoc_insertion_point(builder_scope:routeguide.Feature)
    }

}

