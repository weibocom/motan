/*
 * Copyright 2015, Google Inc. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *    * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *    * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *
 *    * Neither the name of Google Inc. nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.weibo.motan.demo.server;

import com.google.protobuf.util.JsonFormat;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.FeatureDatabase;
import io.grpc.examples.routeguide.Point;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.List;

/**
 * Common utilities for the RouteGuide demo.
 */
public class RouteGuideUtil {
    private static final double COORD_FACTOR = 1e7;

    /**
     * Gets the latitude for the given point.
     */
    public static double getLatitude(Point location) {
        return location.getLatitude() / COORD_FACTOR;
    }

    /**
     * Gets the longitude for the given point.
     */
    public static double getLongitude(Point location) {
        return location.getLongitude() / COORD_FACTOR;
    }

    /**
     * Gets the default features file from classpath.
     */
    public static URL getDefaultFeaturesFile() {
        return RouteGuideUtil.class.getResource("/route_guide_db.json");
    }

    /**
     * Parses the JSON input file containing the list of features.
     */
    public static List<Feature> parseFeatures(URL file) throws IOException {
        InputStream input = file.openStream();
        try {
            Reader reader = new InputStreamReader(input);
            try {
                FeatureDatabase.Builder database = FeatureDatabase.newBuilder();
                JsonFormat.parser().merge(reader, database);
                return database.getFeatureList();
            } finally {
                reader.close();
            }
        } finally {
            input.close();
        }
    }

    /**
     * Indicates whether the given feature exists (i.e. has a valid name).
     */
    public static boolean exists(Feature feature) {
        return feature != null && !feature.getName().isEmpty();
    }
}
