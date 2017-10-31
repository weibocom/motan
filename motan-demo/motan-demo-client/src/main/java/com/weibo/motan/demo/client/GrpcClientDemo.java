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
package com.weibo.motan.demo.client;

import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.Rectangle;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.examples.routeguide.RouteSummary;
import io.grpc.stub.StreamObserver;

import java.util.Random;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.weibo.motan.demo.service.GrpcService;

public class GrpcClientDemo {

    public static void main(String[] args) throws Exception {
        ApplicationContext ctx = new ClassPathXmlApplicationContext(new String[] {"classpath:motan_demo_client_grpc.xml"});

        GrpcService service = (GrpcService) ctx.getBean("motanDemoReferer");

        // unary
        for (int i = 0; i < 2; i++) {
            Point request = Point.newBuilder().setLatitude(100 + i).setLongitude(150 + i).build();
            System.out.println(service.getFeature(request));
            Thread.sleep(1000);
        }


        // server streaming
        Rectangle request =
                Rectangle.newBuilder().setLo(Point.newBuilder().setLatitude(400000000).setLongitude(-750000000).build())
                        .setHi(Point.newBuilder().setLatitude(420000000).setLongitude(-730000000).build()).build();
        StreamObserver<Feature> responseObserver = new StreamObserver<Feature>() {

            @Override
            public void onNext(Feature value) {
                System.out.println(value);
            }

            @Override
            public void onError(Throwable t) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onCompleted() {
                System.out.println("response complete!");
            }

        };
        service.listFeatures(request, responseObserver);
        Thread.sleep(2000);

        // client streaming
        StreamObserver<RouteSummary> routeSummaryObserver = new StreamObserver<RouteSummary>() {

            @Override
            public void onNext(RouteSummary value) {
                System.out.println(value);
            }

            @Override
            public void onError(Throwable t) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onCompleted() {
                System.out.println("response complete!");
            }

        };
        StreamObserver<Point> requestObserver = service.recordRoute(routeSummaryObserver);
        Random random = new Random();
        for (int i = 0; i < 5; i++) {
            Point point = Point.newBuilder().setLatitude(random.nextInt()).setLongitude(random.nextInt()).build();
            requestObserver.onNext(point);
            Thread.sleep(200);
        }
        requestObserver.onCompleted();
        Thread.sleep(2000);

        // biderict-streaming
        StreamObserver<RouteNote> biRequestObserver = service.routeChat(new StreamObserver<RouteNote>() {

            public void onNext(RouteNote value) {
                System.out.println(value);
            }


            public void onError(Throwable t) {
                t.printStackTrace();
            }


            public void onCompleted() {
                System.out.println("routenote complete");
            }
        });

        try {
            RouteNote[] requests =
                    {newNote("First message", 0, 0), newNote("Second message", 0, 1), newNote("Third message", 1, 0),
                            newNote("Fourth message", 1, 1)};

            for (RouteNote note : requests) {
                biRequestObserver.onNext(note);
            }
        } catch (RuntimeException e) {
            biRequestObserver.onError(e);
            throw e;
        }
        biRequestObserver.onCompleted();

        Thread.sleep(2000);

        System.out.println("motan demo is finish.");
        System.exit(0);
    }

    private static RouteNote newNote(String message, int lat, int lon) {
        return RouteNote.newBuilder().setMessage(message).setLocation(Point.newBuilder().setLatitude(lat).setLongitude(lon).build())
                .build();
    }

}
