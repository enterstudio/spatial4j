/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.spatial4j.core.shape.impl;

import com.spatial4j.core.context.SpatialContext;
import com.spatial4j.core.shape.*;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

/**
 * A BufferedLineString is a collection of {@link BufferedLine} shapes,
 * resulting in what some call a "Track" or "Polyline". It's buffered.
 */
public class BufferedLineString implements Shape {

  //TODO add some geospatial awareness like:
  // varying bufs based on latitude (do in BufferedLine)
  // segment that spans the dateline (split it at DL).

  private final ShapeCollection<BufferedLine> segments;
  private final double buf;

  /**
   * Needs at least 1 point, usually more than that.  If just one then it's
   * internally treated like 2 points.
   */
  public BufferedLineString(List<Point> points, double buf, SpatialContext ctx) {
    this(points, buf, false, ctx);
  }

  /**
   * @param points
   * @param buf
   * @param expandBufForLongitudeSkew See {@link BufferedLine
   * #expandBufForLongitudeSkew(com.spatial4j.core.shape.Point,
   * com.spatial4j.core.shape.Point, double)}.
   *                                  If true then the buffer for each segment
   *                                  is computed.
   * @param ctx
   */
  public BufferedLineString(List<Point> points, double buf, boolean expandBufForLongitudeSkew,
                            SpatialContext ctx) {
    if (points.isEmpty())
      throw new IllegalArgumentException("Need at least 1 point.");
    List<BufferedLine> segments = new ArrayList<BufferedLine>(points.size() - 1);

    Point prevPoint = null;
    for (Point point : points) {
      if (prevPoint != null) {
        double segBuf = buf;
        if (expandBufForLongitudeSkew) {
          segBuf = BufferedLine.expandBufForLongitudeSkew(prevPoint, point, buf);
        }
        segments.add(new BufferedLine(prevPoint, point, segBuf, ctx));
      }
      prevPoint = point;
    }
    if (segments.isEmpty()) {
      segments.add(new BufferedLine(prevPoint, prevPoint, buf, ctx));
    }
    this.segments = new ShapeCollection<BufferedLine>(segments, ctx);
    this.buf = buf;
  }

  public ShapeCollection<BufferedLine> getSegments() {
    return segments;
  }

  public double getBuf() {
    return buf;
  }

  @Override
  public double getArea(SpatialContext ctx) {
    return segments.getArea(ctx);
  }

  @Override
  public SpatialRelation relate(Shape other) {
    return segments.relate(other);
  }

  @Override
  public boolean hasArea() {
    return segments.hasArea();
  }

  @Override
  public Point getCenter() {
    return segments.getCenter();
  }

  @Override
  public Rectangle getBoundingBox() {
    return segments.getBoundingBox();
  }

  @Override
  public String toString() {
    StringBuilder str = new StringBuilder(100);
    str.append("BufferedLineString(buf=").append(buf).append(" pts=");
    boolean first = true;
    for (Point point : getPoints()) {
      if (first) {
        first = false;
      } else {
        str.append(", ");
      }
      str.append(point.getX()).append(' ').append(point.getY());
    }
    str.append(')');
    return str.toString();
  }

  public List<Point> getPoints() {
    final List<BufferedLine> lines = segments.getShapes();
    return new AbstractList<Point>() {
      @Override
      public Point get(int index) {
        if (index == 0)
          return lines.get(0).getA();
        return lines.get(index - 1).getB();
      }

      @Override
      public int size() {
        return lines.size() + 1;
      }
    };
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    BufferedLineString that = (BufferedLineString) o;

    if (Double.compare(that.buf, buf) != 0) return false;
    if (!segments.equals(that.segments)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result;
    long temp;
    result = segments.hashCode();
    temp = buf != +0.0d ? Double.doubleToLongBits(buf) : 0L;
    result = 31 * result + (int) (temp ^ (temp >>> 32));
    return result;
  }
}
