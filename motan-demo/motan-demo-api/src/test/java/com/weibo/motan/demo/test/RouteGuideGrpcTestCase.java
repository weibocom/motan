package io.grpc.examples.routeguide;

import com.diffblue.deeptestutils.Reflector;
import io.grpc.MethodDescriptor.Marshaller;
import io.grpc.examples.routeguide.Feature;
import io.grpc.examples.routeguide.Point;
import io.grpc.examples.routeguide.Rectangle;
import io.grpc.examples.routeguide.RouteNote;
import io.grpc.examples.routeguide.RouteSummary;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@RunWith(PowerMockRunner.class)
public class RouteGuideGrpcTestCase {

  @Rule public ExpectedException thrown = ExpectedException.none();

  /* testedClasses: RouteGuideGrpc */
  /*
   * Test generated by Diffblue Deeptest.
   * This test case covers the entire method.
   */
  @PrepareForTest(
      fullyQualifiedNames = {"io.grpc.examples.routeguide.Feature$1",
                             "io.grpc.examples.routeguide.Point$1",
                             "io.grpc.examples.routeguide.Rectangle$1",
                             "io.grpc.examples.routeguide.RouteNote$1",
                             "io.grpc.examples.routeguide.RouteSummary$1"},
      value = {io.grpc.MethodDescriptor.class, io.grpc.examples.routeguide.Feature.class,
               io.grpc.examples.routeguide.Point.class, io.grpc.examples.routeguide.Rectangle.class,
               io.grpc.examples.routeguide.RouteNote.class,
               io.grpc.examples.routeguide.RouteSummary.class, io.grpc.protobuf.ProtoUtils.class})
  @Test
  public void
  staticInitOutputVoid() throws Exception, InvocationTargetException {

    // Setup mocks
    org.powermock.api.mockito.PowerMockito.mockStatic(io.grpc.protobuf.ProtoUtils.class);
    org.powermock.api.mockito.PowerMockito.mockStatic(io.grpc.MethodDescriptor.class);

    // Arrange
    org.mockito.Mockito
        .when(io.grpc.MethodDescriptor.create(
            org.mockito.AdditionalMatchers.or(
                org.mockito.Matchers.isA(io.grpc.MethodDescriptor.MethodType.class),
                org.mockito.Matchers.isNull(io.grpc.MethodDescriptor.MethodType.class)),
            org.mockito.AdditionalMatchers.or(org.mockito.Matchers.isA(String.class),
                                              org.mockito.Matchers.isNull(String.class)),
            org.mockito.AdditionalMatchers.or(
                org.mockito.Matchers.isA(io.grpc.MethodDescriptor.Marshaller.class),
                org.mockito.Matchers.isNull(io.grpc.MethodDescriptor.Marshaller.class)),
            org.mockito.AdditionalMatchers.or(
                org.mockito.Matchers.isA(io.grpc.MethodDescriptor.Marshaller.class),
                org.mockito.Matchers.isNull(io.grpc.MethodDescriptor.Marshaller.class))))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);
    Marshaller marshaller7 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller6 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller5 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller4 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller3 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller2 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller1 =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    Marshaller marshaller =
        ((Marshaller)Reflector.getInstance("io.grpc.MethodDescriptor$Marshaller"));
    org.mockito.Mockito
        .when(io.grpc.protobuf.ProtoUtils.marshaller(org.mockito.AdditionalMatchers.or(
            org.mockito.Matchers.isA(com.google.protobuf.Message.class),
            org.mockito.Matchers.isNull(com.google.protobuf.Message.class))))
        .thenReturn(null)
        .thenReturn(marshaller)
        .thenReturn(null)
        .thenReturn(marshaller1)
        .thenReturn(null)
        .thenReturn(marshaller2)
        .thenReturn(null)
        .thenReturn(marshaller3)
        .thenReturn(null)
        .thenReturn(marshaller4)
        .thenReturn(null)
        .thenReturn(marshaller5)
        .thenReturn(null)
        .thenReturn(marshaller6)
        .thenReturn(null)
        .thenReturn(marshaller7);
    org.mockito.Mockito
        .when(io.grpc.MethodDescriptor.generateFullMethodName(
            org.mockito.AdditionalMatchers.or(org.mockito.Matchers.isA(String.class),
                                              org.mockito.Matchers.isNull(String.class)),
            org.mockito.AdditionalMatchers.or(org.mockito.Matchers.isA(String.class),
                                              org.mockito.Matchers.isNull(String.class))))
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null)
        .thenReturn(null);
    Object object = org.powermock.api.mockito.PowerMockito.mock(
        Class.forName("io.grpc.examples.routeguide.Point$1"));
    org.powermock.api.mockito.PowerMockito.whenNew("io.grpc.examples.routeguide.Point$1")
        .withAnyArguments()
        .thenReturn(object);
    Rectangle rectangle = ((Rectangle)org.mockito.Mockito.mock(Rectangle.class));
    Reflector.setField(rectangle, "lo_", null);
    Reflector.setField(rectangle, "memoizedHashCode", 0);
    Reflector.setField(rectangle, "memoizedIsInitialized", (byte)-1);
    Reflector.setField(rectangle, "hi_", null);
    Reflector.setField(rectangle, "unknownFields", null);
    Reflector.setField(rectangle, "memoizedSize", 0);
    org.powermock.api.mockito.PowerMockito.whenNew(io.grpc.examples.routeguide.Rectangle.class)
        .withAnyArguments()
        .thenReturn(rectangle);
    Point point = ((Point)org.mockito.Mockito.mock(Point.class));
    Reflector.setField(point, "unknownFields", null);
    Reflector.setField(point, "latitude_", 0);
    Reflector.setField(point, "memoizedHashCode", 0);
    Reflector.setField(point, "memoizedSize", 0);
    Reflector.setField(point, "longitude_", 0);
    Reflector.setField(point, "memoizedIsInitialized", (byte)-1);
    org.powermock.api.mockito.PowerMockito.whenNew(io.grpc.examples.routeguide.Point.class)
        .withAnyArguments()
        .thenReturn(point);
    RouteSummary routeSummary = ((RouteSummary)org.mockito.Mockito.mock(RouteSummary.class));
    Reflector.setField(routeSummary, "featureCount_", 0);
    Reflector.setField(routeSummary, "memoizedHashCode", 0);
    Reflector.setField(routeSummary, "elapsedTime_", 0);
    Reflector.setField(routeSummary, "memoizedIsInitialized", (byte)-1);
    Reflector.setField(routeSummary, "unknownFields", null);
    Reflector.setField(routeSummary, "distance_", 0);
    Reflector.setField(routeSummary, "memoizedSize", 0);
    Reflector.setField(routeSummary, "pointCount_", 0);
    org.powermock.api.mockito.PowerMockito.whenNew(io.grpc.examples.routeguide.RouteSummary.class)
        .withAnyArguments()
        .thenReturn(routeSummary);
    Object object1 = org.powermock.api.mockito.PowerMockito.mock(
        Class.forName("io.grpc.examples.routeguide.RouteSummary$1"));
    org.powermock.api.mockito.PowerMockito.whenNew("io.grpc.examples.routeguide.RouteSummary$1")
        .withAnyArguments()
        .thenReturn(object1);
    Object object2 = org.powermock.api.mockito.PowerMockito.mock(
        Class.forName("io.grpc.examples.routeguide.Rectangle$1"));
    org.powermock.api.mockito.PowerMockito.whenNew("io.grpc.examples.routeguide.Rectangle$1")
        .withAnyArguments()
        .thenReturn(object2);
    RouteNote routeNote = ((RouteNote)org.mockito.Mockito.mock(RouteNote.class));
    Reflector.setField(routeNote, "memoizedHashCode", 0);
    Reflector.setField(routeNote, "message_", "");
    Reflector.setField(routeNote, "memoizedIsInitialized", (byte)-1);
    Reflector.setField(routeNote, "unknownFields", null);
    Reflector.setField(routeNote, "memoizedSize", 0);
    Reflector.setField(routeNote, "location_", null);
    org.powermock.api.mockito.PowerMockito.whenNew(io.grpc.examples.routeguide.RouteNote.class)
        .withAnyArguments()
        .thenReturn(routeNote);
    Object object3 = org.powermock.api.mockito.PowerMockito.mock(
        Class.forName("io.grpc.examples.routeguide.Feature$1"));
    org.powermock.api.mockito.PowerMockito.whenNew("io.grpc.examples.routeguide.Feature$1")
        .withAnyArguments()
        .thenReturn(object3);
    Object object4 = org.powermock.api.mockito.PowerMockito.mock(
        Class.forName("io.grpc.examples.routeguide.RouteNote$1"));
    org.powermock.api.mockito.PowerMockito.whenNew("io.grpc.examples.routeguide.RouteNote$1")
        .withAnyArguments()
        .thenReturn(object4);
    Feature feature = ((Feature)org.mockito.Mockito.mock(Feature.class));
    Reflector.setField(feature, "unknownFields", null);
    Reflector.setField(feature, "memoizedIsInitialized", (byte)-1);
    Reflector.setField(feature, "memoizedSize", 0);
    Reflector.setField(feature, "location_", null);
    Reflector.setField(feature, "memoizedHashCode", 0);
    Reflector.setField(feature, "name_", "");
    org.powermock.api.mockito.PowerMockito.whenNew(io.grpc.examples.routeguide.Feature.class)
        .withAnyArguments()
        .thenReturn(feature);

    // Act, using constructor to test static initializer
    Object constructed = Reflector.getInstance("io.grpc.examples.routeguide.RouteGuideGrpc");

    // Method returns void, testing that no exception is thrown
  }
}
