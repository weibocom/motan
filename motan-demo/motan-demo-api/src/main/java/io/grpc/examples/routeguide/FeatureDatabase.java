// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: route_guide.proto

package io.grpc.examples.routeguide;

/**
 * <pre>
 * Not used in the RPC.  Instead, this is here for the form serialized to disk.
 * </pre>
 * <p>
 * Protobuf type {@code routeguide.FeatureDatabase}
 */
public final class FeatureDatabase extends
        com.google.protobuf.GeneratedMessageV3 implements
        // @@protoc_insertion_point(message_implements:routeguide.FeatureDatabase)
        FeatureDatabaseOrBuilder {
    public static final int FEATURE_FIELD_NUMBER = 1;
    private static final long serialVersionUID = 0L;
    // @@protoc_insertion_point(class_scope:routeguide.FeatureDatabase)
    private static final io.grpc.examples.routeguide.FeatureDatabase DEFAULT_INSTANCE;
    private static final com.google.protobuf.Parser<FeatureDatabase>
            PARSER = new com.google.protobuf.AbstractParser<FeatureDatabase>() {
        public FeatureDatabase parsePartialFrom(
                com.google.protobuf.CodedInputStream input,
                com.google.protobuf.ExtensionRegistryLite extensionRegistry)
                throws com.google.protobuf.InvalidProtocolBufferException {
            return new FeatureDatabase(input, extensionRegistry);
        }
    };

    static {
        DEFAULT_INSTANCE = new io.grpc.examples.routeguide.FeatureDatabase();
    }

    private java.util.List<io.grpc.examples.routeguide.Feature> feature_;
    private byte memoizedIsInitialized = -1;

    // Use FeatureDatabase.newBuilder() to construct.
    private FeatureDatabase(com.google.protobuf.GeneratedMessageV3.Builder<?> builder) {
        super(builder);
    }

    private FeatureDatabase() {
        feature_ = java.util.Collections.emptyList();
    }

    private FeatureDatabase(
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
                        if (!((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                            feature_ = new java.util.ArrayList<io.grpc.examples.routeguide.Feature>();
                            mutable_bitField0_ |= 0x00000001;
                        }
                        feature_.add(
                                input.readMessage(io.grpc.examples.routeguide.Feature.parser(), extensionRegistry));
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
            if (((mutable_bitField0_ & 0x00000001) == 0x00000001)) {
                feature_ = java.util.Collections.unmodifiableList(feature_);
            }
            makeExtensionsImmutable();
        }
    }

    public static final com.google.protobuf.Descriptors.Descriptor
    getDescriptor() {
        return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_FeatureDatabase_descriptor;
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(
            com.google.protobuf.ByteString data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(
            com.google.protobuf.ByteString data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(byte[] data)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(
            byte[] data,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws com.google.protobuf.InvalidProtocolBufferException {
        return PARSER.parseFrom(data, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseDelimitedFrom(java.io.InputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseDelimitedFrom(
            java.io.InputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(
            com.google.protobuf.CodedInputStream input)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase parseFrom(
            com.google.protobuf.CodedInputStream input,
            com.google.protobuf.ExtensionRegistryLite extensionRegistry)
            throws java.io.IOException {
        return com.google.protobuf.GeneratedMessageV3
                .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public static Builder newBuilder() {
        return DEFAULT_INSTANCE.toBuilder();
    }

    public static Builder newBuilder(io.grpc.examples.routeguide.FeatureDatabase prototype) {
        return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }

    public static io.grpc.examples.routeguide.FeatureDatabase getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    public static com.google.protobuf.Parser<FeatureDatabase> parser() {
        return PARSER;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
        return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }

    protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
    internalGetFieldAccessorTable() {
        return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_FeatureDatabase_fieldAccessorTable
                .ensureFieldAccessorsInitialized(
                        io.grpc.examples.routeguide.FeatureDatabase.class, io.grpc.examples.routeguide.FeatureDatabase.Builder.class);
    }

    /**
     * <code>repeated .routeguide.Feature feature = 1;</code>
     */
    public java.util.List<io.grpc.examples.routeguide.Feature> getFeatureList() {
        return feature_;
    }

    /**
     * <code>repeated .routeguide.Feature feature = 1;</code>
     */
    public java.util.List<? extends io.grpc.examples.routeguide.FeatureOrBuilder>
    getFeatureOrBuilderList() {
        return feature_;
    }

    /**
     * <code>repeated .routeguide.Feature feature = 1;</code>
     */
    public int getFeatureCount() {
        return feature_.size();
    }

    /**
     * <code>repeated .routeguide.Feature feature = 1;</code>
     */
    public io.grpc.examples.routeguide.Feature getFeature(int index) {
        return feature_.get(index);
    }

    /**
     * <code>repeated .routeguide.Feature feature = 1;</code>
     */
    public io.grpc.examples.routeguide.FeatureOrBuilder getFeatureOrBuilder(
            int index) {
        return feature_.get(index);
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
        for (int i = 0; i < feature_.size(); i++) {
            output.writeMessage(1, feature_.get(i));
        }
    }

    public int getSerializedSize() {
        int size = memoizedSize;
        if (size != -1) return size;

        size = 0;
        for (int i = 0; i < feature_.size(); i++) {
            size += com.google.protobuf.CodedOutputStream
                    .computeMessageSize(1, feature_.get(i));
        }
        memoizedSize = size;
        return size;
    }

    @java.lang.Override
    public boolean equals(final java.lang.Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof io.grpc.examples.routeguide.FeatureDatabase)) {
            return super.equals(obj);
        }
        io.grpc.examples.routeguide.FeatureDatabase other = (io.grpc.examples.routeguide.FeatureDatabase) obj;

        boolean result = true;
        result = result && getFeatureList()
                .equals(other.getFeatureList());
        return result;
    }

    @java.lang.Override
    public int hashCode() {
        if (memoizedHashCode != 0) {
            return memoizedHashCode;
        }
        int hash = 41;
        hash = (19 * hash) + getDescriptorForType().hashCode();
        if (getFeatureCount() > 0) {
            hash = (37 * hash) + FEATURE_FIELD_NUMBER;
            hash = (53 * hash) + getFeatureList().hashCode();
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
    public com.google.protobuf.Parser<FeatureDatabase> getParserForType() {
        return PARSER;
    }

    public io.grpc.examples.routeguide.FeatureDatabase getDefaultInstanceForType() {
        return DEFAULT_INSTANCE;
    }

    /**
     * <pre>
     * Not used in the RPC.  Instead, this is here for the form serialized to disk.
     * </pre>
     * <p>
     * Protobuf type {@code routeguide.FeatureDatabase}
     */
    public static final class Builder extends
            com.google.protobuf.GeneratedMessageV3.Builder<Builder> implements
            // @@protoc_insertion_point(builder_implements:routeguide.FeatureDatabase)
            io.grpc.examples.routeguide.FeatureDatabaseOrBuilder {
        private int bitField0_;
        private java.util.List<io.grpc.examples.routeguide.Feature> feature_ =
                java.util.Collections.emptyList();
        private com.google.protobuf.RepeatedFieldBuilderV3<
                io.grpc.examples.routeguide.Feature, io.grpc.examples.routeguide.Feature.Builder, io.grpc.examples.routeguide.FeatureOrBuilder> featureBuilder_;

        // Construct using io.grpc.examples.routeguide.FeatureDatabase.newBuilder()
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
            return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_FeatureDatabase_descriptor;
        }

        protected com.google.protobuf.GeneratedMessageV3.FieldAccessorTable
        internalGetFieldAccessorTable() {
            return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_FeatureDatabase_fieldAccessorTable
                    .ensureFieldAccessorsInitialized(
                            io.grpc.examples.routeguide.FeatureDatabase.class, io.grpc.examples.routeguide.FeatureDatabase.Builder.class);
        }

        private void maybeForceBuilderInitialization() {
            if (com.google.protobuf.GeneratedMessageV3
                    .alwaysUseFieldBuilders) {
                getFeatureFieldBuilder();
            }
        }

        public Builder clear() {
            super.clear();
            if (featureBuilder_ == null) {
                feature_ = java.util.Collections.emptyList();
                bitField0_ = (bitField0_ & ~0x00000001);
            } else {
                featureBuilder_.clear();
            }
            return this;
        }

        public com.google.protobuf.Descriptors.Descriptor
        getDescriptorForType() {
            return io.grpc.examples.routeguide.RouteGuideProto.internal_static_routeguide_FeatureDatabase_descriptor;
        }

        public io.grpc.examples.routeguide.FeatureDatabase getDefaultInstanceForType() {
            return io.grpc.examples.routeguide.FeatureDatabase.getDefaultInstance();
        }

        public io.grpc.examples.routeguide.FeatureDatabase build() {
            io.grpc.examples.routeguide.FeatureDatabase result = buildPartial();
            if (!result.isInitialized()) {
                throw newUninitializedMessageException(result);
            }
            return result;
        }

        public io.grpc.examples.routeguide.FeatureDatabase buildPartial() {
            io.grpc.examples.routeguide.FeatureDatabase result = new io.grpc.examples.routeguide.FeatureDatabase(this);
            int from_bitField0_ = bitField0_;
            if (featureBuilder_ == null) {
                if (((bitField0_ & 0x00000001) == 0x00000001)) {
                    feature_ = java.util.Collections.unmodifiableList(feature_);
                    bitField0_ = (bitField0_ & ~0x00000001);
                }
                result.feature_ = feature_;
            } else {
                result.feature_ = featureBuilder_.build();
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
            if (other instanceof io.grpc.examples.routeguide.FeatureDatabase) {
                return mergeFrom((io.grpc.examples.routeguide.FeatureDatabase) other);
            } else {
                super.mergeFrom(other);
                return this;
            }
        }

        public Builder mergeFrom(io.grpc.examples.routeguide.FeatureDatabase other) {
            if (other == io.grpc.examples.routeguide.FeatureDatabase.getDefaultInstance()) return this;
            if (featureBuilder_ == null) {
                if (!other.feature_.isEmpty()) {
                    if (feature_.isEmpty()) {
                        feature_ = other.feature_;
                        bitField0_ = (bitField0_ & ~0x00000001);
                    } else {
                        ensureFeatureIsMutable();
                        feature_.addAll(other.feature_);
                    }
                    onChanged();
                }
            } else {
                if (!other.feature_.isEmpty()) {
                    if (featureBuilder_.isEmpty()) {
                        featureBuilder_.dispose();
                        featureBuilder_ = null;
                        feature_ = other.feature_;
                        bitField0_ = (bitField0_ & ~0x00000001);
                        featureBuilder_ =
                                com.google.protobuf.GeneratedMessageV3.alwaysUseFieldBuilders ?
                                        getFeatureFieldBuilder() : null;
                    } else {
                        featureBuilder_.addAllMessages(other.feature_);
                    }
                }
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
            io.grpc.examples.routeguide.FeatureDatabase parsedMessage = null;
            try {
                parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
            } catch (com.google.protobuf.InvalidProtocolBufferException e) {
                parsedMessage = (io.grpc.examples.routeguide.FeatureDatabase) e.getUnfinishedMessage();
                throw e.unwrapIOException();
            } finally {
                if (parsedMessage != null) {
                    mergeFrom(parsedMessage);
                }
            }
            return this;
        }

        private void ensureFeatureIsMutable() {
            if (!((bitField0_ & 0x00000001) == 0x00000001)) {
                feature_ = new java.util.ArrayList<io.grpc.examples.routeguide.Feature>(feature_);
                bitField0_ |= 0x00000001;
            }
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public java.util.List<io.grpc.examples.routeguide.Feature> getFeatureList() {
            if (featureBuilder_ == null) {
                return java.util.Collections.unmodifiableList(feature_);
            } else {
                return featureBuilder_.getMessageList();
            }
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public int getFeatureCount() {
            if (featureBuilder_ == null) {
                return feature_.size();
            } else {
                return featureBuilder_.getCount();
            }
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public io.grpc.examples.routeguide.Feature getFeature(int index) {
            if (featureBuilder_ == null) {
                return feature_.get(index);
            } else {
                return featureBuilder_.getMessage(index);
            }
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder setFeature(
                int index, io.grpc.examples.routeguide.Feature value) {
            if (featureBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureFeatureIsMutable();
                feature_.set(index, value);
                onChanged();
            } else {
                featureBuilder_.setMessage(index, value);
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder setFeature(
                int index, io.grpc.examples.routeguide.Feature.Builder builderForValue) {
            if (featureBuilder_ == null) {
                ensureFeatureIsMutable();
                feature_.set(index, builderForValue.build());
                onChanged();
            } else {
                featureBuilder_.setMessage(index, builderForValue.build());
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder addFeature(io.grpc.examples.routeguide.Feature value) {
            if (featureBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureFeatureIsMutable();
                feature_.add(value);
                onChanged();
            } else {
                featureBuilder_.addMessage(value);
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder addFeature(
                int index, io.grpc.examples.routeguide.Feature value) {
            if (featureBuilder_ == null) {
                if (value == null) {
                    throw new NullPointerException();
                }
                ensureFeatureIsMutable();
                feature_.add(index, value);
                onChanged();
            } else {
                featureBuilder_.addMessage(index, value);
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder addFeature(
                io.grpc.examples.routeguide.Feature.Builder builderForValue) {
            if (featureBuilder_ == null) {
                ensureFeatureIsMutable();
                feature_.add(builderForValue.build());
                onChanged();
            } else {
                featureBuilder_.addMessage(builderForValue.build());
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder addFeature(
                int index, io.grpc.examples.routeguide.Feature.Builder builderForValue) {
            if (featureBuilder_ == null) {
                ensureFeatureIsMutable();
                feature_.add(index, builderForValue.build());
                onChanged();
            } else {
                featureBuilder_.addMessage(index, builderForValue.build());
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder addAllFeature(
                java.lang.Iterable<? extends io.grpc.examples.routeguide.Feature> values) {
            if (featureBuilder_ == null) {
                ensureFeatureIsMutable();
                com.google.protobuf.AbstractMessageLite.Builder.addAll(
                        values, feature_);
                onChanged();
            } else {
                featureBuilder_.addAllMessages(values);
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder clearFeature() {
            if (featureBuilder_ == null) {
                feature_ = java.util.Collections.emptyList();
                bitField0_ = (bitField0_ & ~0x00000001);
                onChanged();
            } else {
                featureBuilder_.clear();
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public Builder removeFeature(int index) {
            if (featureBuilder_ == null) {
                ensureFeatureIsMutable();
                feature_.remove(index);
                onChanged();
            } else {
                featureBuilder_.remove(index);
            }
            return this;
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public io.grpc.examples.routeguide.Feature.Builder getFeatureBuilder(
                int index) {
            return getFeatureFieldBuilder().getBuilder(index);
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public io.grpc.examples.routeguide.FeatureOrBuilder getFeatureOrBuilder(
                int index) {
            if (featureBuilder_ == null) {
                return feature_.get(index);
            } else {
                return featureBuilder_.getMessageOrBuilder(index);
            }
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public java.util.List<? extends io.grpc.examples.routeguide.FeatureOrBuilder>
        getFeatureOrBuilderList() {
            if (featureBuilder_ != null) {
                return featureBuilder_.getMessageOrBuilderList();
            } else {
                return java.util.Collections.unmodifiableList(feature_);
            }
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public io.grpc.examples.routeguide.Feature.Builder addFeatureBuilder() {
            return getFeatureFieldBuilder().addBuilder(
                    io.grpc.examples.routeguide.Feature.getDefaultInstance());
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public io.grpc.examples.routeguide.Feature.Builder addFeatureBuilder(
                int index) {
            return getFeatureFieldBuilder().addBuilder(
                    index, io.grpc.examples.routeguide.Feature.getDefaultInstance());
        }

        /**
         * <code>repeated .routeguide.Feature feature = 1;</code>
         */
        public java.util.List<io.grpc.examples.routeguide.Feature.Builder>
        getFeatureBuilderList() {
            return getFeatureFieldBuilder().getBuilderList();
        }

        private com.google.protobuf.RepeatedFieldBuilderV3<
                io.grpc.examples.routeguide.Feature, io.grpc.examples.routeguide.Feature.Builder, io.grpc.examples.routeguide.FeatureOrBuilder>
        getFeatureFieldBuilder() {
            if (featureBuilder_ == null) {
                featureBuilder_ = new com.google.protobuf.RepeatedFieldBuilderV3<
                        io.grpc.examples.routeguide.Feature, io.grpc.examples.routeguide.Feature.Builder, io.grpc.examples.routeguide.FeatureOrBuilder>(
                        feature_,
                        ((bitField0_ & 0x00000001) == 0x00000001),
                        getParentForChildren(),
                        isClean());
                feature_ = null;
            }
            return featureBuilder_;
        }

        public final Builder setUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return this;
        }

        public final Builder mergeUnknownFields(
                final com.google.protobuf.UnknownFieldSet unknownFields) {
            return this;
        }


        // @@protoc_insertion_point(builder_scope:routeguide.FeatureDatabase)
    }

}

