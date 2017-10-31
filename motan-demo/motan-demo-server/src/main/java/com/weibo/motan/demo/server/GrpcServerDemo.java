/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.weibo.motan.demo.server;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;
import static java.util.concurrent.TimeUnit.NANOSECONDS;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.Rectangle;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.examples.routeguide.RouteSummary;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.motan.demo.service.GrpcService;

public class GrpcServerDemo implements GrpcService {
    static Collection<Feature> features;
    public static void main(String[] args) throws Exception {
        features = RouteGuideUtil.parseFeatures(RouteGuideUtil.getDefaultFeaturesFile());
        ApplicationContext applicationContext = new ClassPathXmlApplicationContext("classpath:motan_demo_server_grpc.xml");
        System.out.println("grpc server start...");
        Thread.sleep(Long.MAX_VALUE);
        
    }
    
    private final ConcurrentMap<Point, List<RouteNote>> routeNotes =
        new ConcurrentHashMap<Point, List<RouteNote>>();


    public Feature getFeature(Point request) {
        return checkFeature(request);
      }

      /**
       * Gets all features contained within the given bounding {@link Rectangle}.
       *
       * @param request the bounding rectangle for the requested features.
       * @param responseObserver the observer that will receive the features.
       */
      
      public void listFeatures(Rectangle request, StreamObserver<Feature> responseObserver) {
        int left = min(request.getLo().getLongitude(), request.getHi().getLongitude());
        int right = max(request.getLo().getLongitude(), request.getHi().getLongitude());
        int top = max(request.getLo().getLatitude(), request.getHi().getLatitude());
        int bottom = min(request.getLo().getLatitude(), request.getHi().getLatitude());

        for (Feature feature : features) {
          if (!RouteGuideUtil.exists(feature)) {
            continue;
          }

          int lat = feature.getLocation().getLatitude();
          int lon = feature.getLocation().getLongitude();
          if (lon >= left && lon <= right && lat >= bottom && lat <= top) {
            responseObserver.onNext(feature);
          }
        }
        responseObserver.onCompleted();
      }

      /**
       * Gets a stream of points, and responds with statistics about the "trip": number of points,
       * number of known features visited, total distance traveled, and total time spent.
       *
       * @param responseObserver an observer to receive the response summary.
       * @return an observer to receive the requested route points.
       */
      
      public StreamObserver<Point> recordRoute(final StreamObserver<RouteSummary> responseObserver) {
        return new StreamObserver<Point>() {
          int pointCount;
          int featureCount;
          int distance;
          Point previous;
          long startTime = System.nanoTime();

          
          public void onNext(Point point) {
            pointCount++;
            if (RouteGuideUtil.exists(checkFeature(point))) {
              featureCount++;
            }
            // For each point after the first, add the incremental distance from the previous point to
            // the total distance value.
            if (previous != null) {
              distance += calcDistance(previous, point);
            }
            previous = point;
          }

          
          public void onError(Throwable t) {
          }

          
          public void onCompleted() {
            long seconds = NANOSECONDS.toSeconds(System.nanoTime() - startTime);
            responseObserver.onNext(RouteSummary.newBuilder().setPointCount(pointCount)
                .setFeatureCount(featureCount).setDistance(distance)
                .setElapsedTime((int) seconds).build());
            responseObserver.onCompleted();
          }
        };
      }

      /**
       * Receives a stream of message/location pairs, and responds with a stream of all previous
       * messages at each of those locations.
       *
       * @param responseObserver an observer to receive the stream of previous messages.
       * @return an observer to handle requested message/location pairs.
       */
      
      public StreamObserver<RouteNote> routeChat(final StreamObserver<RouteNote> responseObserver) {
        return new StreamObserver<RouteNote>() {
          
          public void onNext(RouteNote note) {
            List<RouteNote> notes = getOrCreateNotes(note.getLocation());

            // Respond with all previous notes at this location.
            for (RouteNote prevNote : notes.toArray(new RouteNote[0])) {
              responseObserver.onNext(prevNote);
            }

            // Now add the new note to the list
            notes.add(note);
          }

          
          public void onError(Throwable t) {
          }

          
          public void onCompleted() {
            responseObserver.onCompleted();
          }
        };
      }
      
      /**
       * Get the notes list for the given location. If missing, create it.
       */
      private List<RouteNote> getOrCreateNotes(Point location) {
        List<RouteNote> notes = Collections.synchronizedList(new ArrayList<RouteNote>());
        List<RouteNote> prevNotes = routeNotes.putIfAbsent(location, notes);
        return prevNotes != null ? prevNotes : notes;
      }

      /**
       * Gets the feature at the given point.
       *
       * @param location the location to check.
       * @return The feature object at the point. Note that an empty name indicates no feature.
       */
      private Feature checkFeature(Point location) {
        for (Feature feature : features) {
          if (feature.getLocation().getLatitude() == location.getLatitude()
              && feature.getLocation().getLongitude() == location.getLongitude()) {
            return feature;
          }
        }

        // No feature was found, return an unnamed feature.
        return Feature.newBuilder().setName("").setLocation(location).build();
      }

      /**
       * Calculate the distance between two points using the "haversine" formula.
       * This code was taken from http://www.movable-type.co.uk/scripts/latlong.html.
       *
       * @param start The starting point
       * @param end The end point
       * @return The distance between the points in meters
       */
      private static double calcDistance(Point start, Point end) {
        double lat1 = RouteGuideUtil.getLatitude(start);
        double lat2 = RouteGuideUtil.getLatitude(end);
        double lon1 = RouteGuideUtil.getLongitude(start);
        double lon2 = RouteGuideUtil.getLongitude(end);
        int r = 6371000; // metres
        double φ1 = toRadians(lat1);
        double φ2 = toRadians(lat2);
        double Δφ = toRadians(lat2 - lat1);
        double Δλ = toRadians(lon2 - lon1);

        double a = sin(Δφ / 2) * sin(Δφ / 2) + cos(φ1) * cos(φ2) * sin(Δλ / 2) * sin(Δλ / 2);
        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        return r * c;
      }

}
