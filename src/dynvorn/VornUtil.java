package dynvorn;


import testbed.*;
import base.*;

public class VornUtil {

  /**
   * Calculate position of Voronoi vertex of moving points.
   * See http://mathworld.wolfram.com/Circumcircle.html for reference.
   * @param site1 first site
   * @param site2 second site
   * @param site3 third site
   * @param t time from start position
   * @return predicted location of Voronoi vertex associated with these sites
   * @deprecated unused at present
   */
  public static FPoint2 vornVertexPos(IVornSite site1, IVornSite site2,
      IVornSite site3, double t) {
    if (true)
      return vornVertexPos2(site1, site2, site3, t);

    double t2 = t * t;
    double t3 = t2 * t;

    FPoint2 ret = null;

    FPoint2 loc1 = site1.getSiteLocation(t);
    FPoint2 loc2 = site2.getSiteLocation(t);
    FPoint2 loc3 = site3.getSiteLocation(t);

    double x1 = loc1.x, y1 = loc1.y;
    double x2 = loc2.x, y2 = loc2.y;
    double x3 = loc3.x, y3 = loc3.y;

    double a = (x2 * y3 - x3 * y2) + (x3 * y1 - x1 * y3) + (x1 * y2 - x2 * y1);
    double A1 = x1 * x1 + y1 * y1, A2 = x2 * x2 + y2 * y2, A3 = x3 * x3 + y3
        * y3;

    double bx = (A3 * y2 - A2 * y3) + (A1 * y3 - A3 * y1) + (A2 * y1 - A1 * y2);
    double by = (A2 * x3 - A3 * x2) + (A3 * x1 - A1 * x3) + (A1 * x2 - A2 * x1);

    double x0 = -bx / (2 * a);
    double y0 = -by / (2 * a);
    ret = new FPoint2(x0, y0);

    return ret;
  }

  /**
   * Calculate position of Voronoi vertex of moving points.
   * See http://mathworld.wolfram.com/Circumcircle.html for reference.
   * @param site1 first site
   * @param site2 second site
   * @param site3 third site
   * @param t time from start position
   * @return predicted location of Voronoi vertex associated with these sites
   * @deprecated unused
   */
  public static FPoint2 vornVertexPos2(IVornSite site1, IVornSite site2,
      IVornSite site3, double t) {

    double t2 = t * t;
    //    double t3 = t2 * t;

    FPoint2 ret = null;
    FPoint2 start1 = site1.getSiteLocation();
    FPoint2 start2 = site2.getSiteLocation();
    FPoint2 start3 = site3.getSiteLocation();
    FPoint2 vel1 = site1.getVelocity();
    FPoint2 vel2 = site2.getVelocity();
    FPoint2 vel3 = site3.getVelocity();

    double s1 = start1.x, q1 = start1.y;
    double s2 = start2.x, q2 = start2.y;
    double s3 = start3.x, q3 = start3.y;

    double u1 = vel1.x, v1 = vel1.y;
    double u2 = vel2.x, v2 = vel2.y;
    double u3 = vel3.x, v3 = vel3.y;

    double d1 = u1 * u1 + v1 * v1, e1 = 2 * (s1 * u1 + q1 * v1), f1 = (s1 * s1 + q1
        * q1);
    double d2 = u2 * u2 + v2 * v2, e2 = 2 * (s2 * u2 + q2 * v2), f2 = (s2 * s2 + q2
        * q2);
    double d3 = u3 * u3 + v3 * v3, e3 = 2 * (s3 * u3 + q3 * v3), f3 = (s3 * s3 + q3
        * q3);

    Streams.out.println("d1=" + d1 + " d2=" + d2 + " d3=" + d3);
    // calculate coefficients for bx, a quadratic in t
    // (it's not a quadratic if points are not moving at same speed) 
    //bx3 is always 0!  only if speed is constant.
    //    double bx3 = (d3 * v2 - d2 * v3 + d1 * v3 - d3 * v1 + d2 * v1 - d1 * v2);
    double bx2 = (d3 * q2 - d2 * q3 + d1 * q3 - d3 * q1 + d2 * q1 - d1 * q2
        + e3 * v2 - e2 * v3 + e1 * v3 - e3 * v1 + e2 * v1 - e1 * v2);
    double bx1 = (q2 * e3 - q3 * e2 + q3 * e1 - q1 * e3 + q1 * e2 - q2 * e1
        + f3 * v2 - f2 * v3 + f1 * v3 - f3 * v1 + f2 * v1 - f1 * v2);
    double bx0 = (f3 * q2 - q3 * f2 + q3 * f1 - f3 * q1 + q1 * f2 - q2 * f1);
    //
    double bx =
    //t3 * bx3 +
    t2 * bx2 + t * bx1 + bx0;

    // calculate coefficients for by, a cubic polynomial in t
    // double by3 = (d2 * u3 - d3 * u2 + d3 * u1 - d1 * u3 + d1 * u2 - d2 * u1);
    double by2 = (d2 * s3 - d3 * s2 + d3 * s1 - d1 * s3 + d1 * s2 - d2 * s1
        + e2 * u3 - e3 * u2 + e3 * u1 - e1 * u3 + e1 * u2 - e2 * u1);
    double by1 = (s3 * e2 - s2 * e3 + s1 * e3 - s3 * e1 + s2 * e1 - s1 * e2
        + f2 * u3 - f3 * u2 + f3 * u1 - f1 * u3 + f1 * u2 - f2 * u1);
    double by0 = (s3 * f2 - f3 * s2 + f3 * s1 - s3 * f1 + s2 * f1 - s1 * f2);
    //
    double by = //  t3 * by3 +
    t2 * by2 + t * by1 + by0;

    // calculate coefficients for a, a quadratic polynomial in t
    double at2 = u2 * v3 - u3 * v2 + u3 * v1 - u1 * v3 + u1 * v2 - u2 * v1;
    double at1 = s2 * v3 - s3 * v2 + u2 * q3 - u3 * q2 + s3 * v1 - s1 * v3 + u3
        * q1 - u1 * q3 + s1 * v2 - s2 * v1 + u1 * q2 - u2 * q1;
    double at0 = s2 * q3 - s3 * q2 + s3 * q1 - s1 * q3 + s1 * q2 - s2 * q1;
    //
    double a = t2 * at2 + t * at1 + at0;

    if (false) {
      Streams.out.println("a coff=" + (-2 * at2) + " " + (-2 * at1) + " "
          + (-2 * at0));
      Streams.out.println("bx cff="
      //+ bx3 
          + " " + bx2 + " " + bx1 + " " + bx0);
    }

    // calculate x, y 
    double x0 = -bx / (2 * a);
    double y0 = -by / (2 * a);

    ret = new FPoint2(x0, y0);

    return ret;
  }

  /**
   * Calculate polynomials for parameterizing Voronoi vertex
   * of moving points as a function of time
   * See http://mathworld.wolfram.com/Circumcircle.html for reference.
   * @param site1 first site
   * @param site2 second site
   * @param site3 third site
   * @param t time from start position
   * @return array of coefficients, satisfying
   *   x*(a[0]*t^2 + a[1]*t + a[2]) + a[3]*t^2 + a[4]*t + a[5] = 0
   *  and 
   *   y*(a[0]*t^2 + a[1]*t + a[2]) + a[6]*t^2 + a[7]*t + a[8] = 0
   */
  public static double[] vornVertexPath(IVornSite site1, IVornSite site2,
      IVornSite site3) {

    double[] ret = new double[9];

    //    double t2 = t * t;
    //    double t3 = t2 * t;

    //    FPoint2 ret = null;
    FPoint2 start1 = site1.getSiteLocation();
    FPoint2 start2 = site2.getSiteLocation();
    FPoint2 start3 = site3.getSiteLocation();
    FPoint2 vel1 = site1.getVelocity();
    FPoint2 vel2 = site2.getVelocity();
    FPoint2 vel3 = site3.getVelocity();

    double s1 = start1.x, q1 = start1.y;
    double s2 = start2.x, q2 = start2.y;
    double s3 = start3.x, q3 = start3.y;

    double u1 = vel1.x, v1 = vel1.y;
    double u2 = vel2.x, v2 = vel2.y;
    double u3 = vel3.x, v3 = vel3.y;

    double d1 = u1 * u1 + v1 * v1, e1 = 2 * (s1 * u1 + q1 * v1), f1 = (s1 * s1 + q1
        * q1);
    double d2 = u2 * u2 + v2 * v2, e2 = 2 * (s2 * u2 + q2 * v2), f2 = (s2 * s2 + q2
        * q2);
    double d3 = u3 * u3 + v3 * v3, e3 = 2 * (s3 * u3 + q3 * v3), f3 = (s3 * s3 + q3
        * q3);

    //Streams.out.println("d1="+d1+" d2="+d2+" d3="+d3);
    // calculate coefficients for bx, a quadratic in t
    // (it's not a quadratic if points are not moving at same speed) 
    //bx3 is always 0!  only if speed is constant.
    //    double bx3 = (d3 * v2 - d2 * v3 + d1 * v3 - d3 * v1 + d2 * v1 - d1 * v2);
    double bx2 = (d3 * q2 - d2 * q3 + d1 * q3 - d3 * q1 + d2 * q1 - d1 * q2
        + e3 * v2 - e2 * v3 + e1 * v3 - e3 * v1 + e2 * v1 - e1 * v2);
    double bx1 = (q2 * e3 - q3 * e2 + q3 * e1 - q1 * e3 + q1 * e2 - q2 * e1
        + f3 * v2 - f2 * v3 + f1 * v3 - f3 * v1 + f2 * v1 - f1 * v2);
    double bx0 = (f3 * q2 - q3 * f2 + q3 * f1 - f3 * q1 + q1 * f2 - q2 * f1);
    //

    //    double bx =  
    //      //t3 * bx3 +
    //      t2 * bx2 + t * bx1 + bx0;
    ret[3] = bx2;
    ret[4] = bx1;
    ret[5] = bx0;

    // calculate coefficients for by, a cubic polynomial in t
    // double by3 = (d2 * u3 - d3 * u2 + d3 * u1 - d1 * u3 + d1 * u2 - d2 * u1);
    double by2 = (d2 * s3 - d3 * s2 + d3 * s1 - d1 * s3 + d1 * s2 - d2 * s1
        + e2 * u3 - e3 * u2 + e3 * u1 - e1 * u3 + e1 * u2 - e2 * u1);
    double by1 = (s3 * e2 - s2 * e3 + s1 * e3 - s3 * e1 + s2 * e1 - s1 * e2
        + f2 * u3 - f3 * u2 + f3 * u1 - f1 * u3 + f1 * u2 - f2 * u1);
    double by0 = (s3 * f2 - f3 * s2 + f3 * s1 - s3 * f1 + s2 * f1 - s1 * f2);
    //
    //    double by =//  t3 * by3 +
    //    t2 * by2 + t * by1 + by0;
    ret[6] = by2;
    ret[7] = by1;
    ret[8] = by0;
    // calculate coefficients for a, a quadratic polynomial in t
    double at2 = u2 * v3 - u3 * v2 + u3 * v1 - u1 * v3 + u1 * v2 - u2 * v1;
    double at1 = s2 * v3 - s3 * v2 + u2 * q3 - u3 * q2 + s3 * v1 - s1 * v3 + u3
        * q1 - u1 * q3 + s1 * v2 - s2 * v1 + u1 * q2 - u2 * q1;
    double at0 = s2 * q3 - s3 * q2 + s3 * q1 - s1 * q3 + s1 * q2 - s2 * q1;
    //
    //    double a = t2 * at2 + t * at1 + at0;
    ret[0] = -2 * at2;
    ret[1] = -2 * at1;
    ret[2] = -2 * at0;

    if (false) {
      Streams.out.println("a coff=" + (-2 * at2) + " " + (-2 * at1) + " "
          + (-2 * at0));
      Streams.out.println("bx cff="
      //+ bx3 
          + " " + bx2 + " " + bx1 + " " + bx0);
    }

    //    // calculate x, y 
    //    double x0 = -bx / (2 * a);
    //    double y0 = -by / (2 * a);
    //
    //    ret = new FPoint2(x0, y0);

    return ret;
  }

  /**
   * Calculate point on vertex path
   * @param a array of 9 doubles; returned by vornVertexPath
   * @param t time
   * @return position of vertex
   */
  public static FPoint2 calcPointOnVertexPath(double[] a, double t) {
    double d = a[0] * t * t + a[1] * t + a[2];
    double xf = a[3] * t * t + a[4] * t + a[5];
    double yf = a[6] * t * t + a[7] * t + a[8];
    return new FPoint2(xf / d, yf / d);
  }

  /**
   * Calculate the next time that two Voronoi paths will cross
   * @param p1 first path
   * @param p2 second path 
   * @param currTime current time
   * @return nearest time greater than current, or -1 if no more
   *   intersections detected
   */
  public static double calcNextVornPathCrossing(double[] p1, double[] p2,
      double currTime) {
    final boolean db = true;

    double d = p1[0], e = p1[1], f = p1[2];
    double a = p1[3], b = p1[4], c = p1[5];
    double k = p2[0], m = p2[1], n = p2[2];
    double g = p2[3], h = p2[4], j = p2[5];

    Polyn poly = new Polyn(a * k - d * g, a * m - d * h + b * k - e * g, a * n
        - d * j + b * m - e * h + c * k - f * g, b * n - e * j + c * m - f * h,
        c * n - f * j);
    DArray roots = new DArray();
    poly.solve(roots);

    double nextTime = -1;

    final double EPS = 1e-5;
    for (int i = 0; i < roots.size(); i++) {
      double tr = roots.getDouble(i);
      if (db && T.update())
        T.msg("nextVornPathCrossing, root #" + i + " is " + Tools.f(tr));
      if (tr <= currTime + EPS) {
        if (db && T.update())
          T.msg(" before current time " + Tools.f(currTime));
        continue;
      }
      // verify that both vertices will actually reach this point
      FPoint2 pt1 = calcPointOnVertexPath(p1, tr);
      FPoint2 pt2 = calcPointOnVertexPath(p2, tr);
      double dist = FPoint2.distance(pt1, pt2);
      if (db && T.update())
        T.msg("verified points, distance=" + Tools.f(dist) + T.show(pt1)
            + T.show(pt2));
      if (dist > 1e-1) {
        if (db && T.update())
          T.msg("point is not on both paths" + T.show(pt1) + T.show(pt2));
        continue;
      }

      if (nextTime < 0 || nextTime > tr)
        nextTime = tr;
    }
    return nextTime;
  }


//  private static class SimpleSite implements IVornSite {
//    public FPoint2 location;
//    public String label;
//
//    public SimpleSite(FPoint2 loc, String label) {
//      this.location = new FPoint2(loc);
//      this.label = label;
//    }
//    public String toString() {
//      return label;
//    }
//
//    public FPoint2 getSiteLocation() {
//      return location;
//    }
//    public FPoint2 getSiteLocation(double time) {
//      return location;
//    }
//    public String getSiteName() {
//      return label;
//    }
//    public FPoint2 getVelocity() {
//      throw new UnsupportedOperationException();
//    }
//  }
}
