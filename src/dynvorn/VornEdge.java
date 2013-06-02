package dynvorn;


import java.awt.*;
import testbed.*;
import base.*;

public class VornEdge implements Renderable {
  private static final boolean db = false;

  private IVornSite siteI, siteJ;

  public IVornSite site(int index) {
    return index == 0 ? siteI : siteJ;
  }
  public VornEdge(IVornSite siteLeft, IVornSite siteRight) {
    this.siteI = siteLeft;
    this.siteJ = siteRight;
    FPoint2 ptLeft = siteLeft.getSiteLocation();
    FPoint2 ptRight = siteRight.getSiteLocation();

    if (FPoint2.distance(ptLeft, ptRight) >= 1e-6) {
      double theta = MyMath.polarAngle(ptLeft, ptRight);
      lineEqn = new LineEqn(FPoint2.midPoint(ptLeft, ptRight), theta + Math.PI
          / 2);
    }
  }

  /**
   * @deprecated
   * @return
   */
  public FPoint2[] calcEndpoints() {
    endpoint(0);
    endpoint(1);
    return endpoints;
    //    FPoint2 p0 = endPoint(0);
    //    FPoint2 p1 = endPoint(1);
    //    FPoint2[] ep = new FPoint2[2];
    //    ep[0] = p0;
    //    ep[1] = p1;
    //    return ep;
  }

  public boolean infinite(int ep) {
    return (ep == 0) ? !finite0 : !finite1;
  }

  public LineEqn lineEqn() {
    return lineEqn;
  }

  private FPoint2[] endpoints = new FPoint2[2];

  public FPoint2 endpoint(int i) {
    if (!nonEmpty())
      throw new IllegalStateException();

    if (endpoints[i] == null) {
      final double MAX = 10000;
      double t;
      if (i == 0) {
        if (!finite0) {
          t = -MAX;
        } else {
          t = c0;
        }
      } else {
        if (!finite1) {
          t = MAX;
        } else {
          t = c1;
        }
      }
      FPoint2 r = lineEqn.pt(t);
      endpoints[i] = r;
    }
    return endpoints[i];
  }

  /**
   * Determine if line is valid (endpoints are distinct)
   * @return true if so
   */
  public boolean valid() {
    return lineEqn != null;
  }

  /**
   * Determine if clipped line contains any points
   * @return true if so
   */
  public boolean nonEmpty() {
    final double EPS = 1e-6;
    return valid()
        && ((!finite0 || !finite1) || c0 + EPS < c1 || endpoints[0] != null || endpoints[1] != null

        );
  }

  public static void clip(VornEdge lineA, VornEdge lineB) {
    final boolean db = false;

    if (db && T.update())
      T.msg("clip\n " + lineA + "\n to \n " + lineB);
    if (lineA.valid() && lineB.valid()) {
      FPoint2 ipt = LineEqn.intersection(lineA.lineEqn, lineB.lineEqn);
      if (db && T.update())
        T.msg("pt of intersection" + T.show(ipt));
      if (ipt != null) {
        double t1 = lineA.lineEqn.parameterFor(ipt);
        double t2 = lineB.lineEqn.parameterFor(ipt);
        FPoint2 testPt = lineB.lineEqn.pt(t2 - 5);
        if (lineA.lineEqn.sideOfLine(testPt) >= 0) {
          lineA.clipBelow(t1);
          lineB.clipAbove(t2);
        } else {
          lineA.clipAbove(t1);
          lineB.clipBelow(t2);
        }
      } else {
        if (lineA.lineEqn.sideOfLine(lineB.lineEqn.pt(0)) <= 0) {
          if (db && T.update())
            T.msg("clipping all of B");
          lineB.clipAll();
        }
        if (lineB.lineEqn.sideOfLine(lineA.lineEqn.pt(0)) <= 0) {
          if (db && T.update())
            T.msg("clipping all of A");
          lineA.clipAll();
        }
      }
    }
    if (db && T.update())
      T.msg("aftr clipping, A=\n " + lineA + "\n B=\n " + lineB);

  }

  /**
   * @deprecated
   */
  public void clipAll() {
    finite0 = finite1 = true;
    c0 = 0;
    c1 = -1;
  }

  /**
   * @deprecated
   * @param c
   */
  public void clipBelow(double c) {
    if (db && T.update())
      T.msg("clipBelow c=" + c + T.show(this) + " pt="
          + lineEqn.pt(c).toString(true));
    if (!finite0 || c0 < c) {
      finite0 = true;
      c0 = c;
    }
  }

  /**
   * @deprecated
   * @param pt
   */
  public void clipBelow(FPoint2 pt) {
    if (db && T.update())
      T.msg("clipBelow pt=" + pt + T.show(this) + T.show(pt));
    double t = lineEqn.parameterFor(pt);
    clipBelow(t);
  }

  public void clipAbove(FPoint2 pt) {
    if (db && T.update())
      T.msg("clipAbove pt=" + pt + T.show(this) + T.show(pt));
    double t = lineEqn.parameterFor(pt);
    clipAbove(t);
  }

  public void setStart(FPoint2 pt) {
    finite0 = true;
    endpoints[0] = pt;
  }

  public void setFinish(FPoint2 pt) {
    finite1 = true;
    endpoints[1] = pt;
  }

  /**
   * @deprecated
   * @param c
   */
  public void clipAbove(double c) {
    if (db && T.update())
      T.msg("clipAbove c=" + c + T.show(this) + " pt="
          + lineEqn.pt(c).toString(true));
    if (!finite1 || c1 > c) {
      finite1 = true;
      c1 = c;
    }
  }

  /**
   * @deprecated
   * @return
   */
  public double getClipParam() {
    double t = -1;
    if (!finite0 && finite1)
      t = c1;
    else if (finite0 && !finite1)
      t = c0;
    else {
      Tools.warn("getClipParam: finite0=" + finite0 + " 1=" + finite1);
      t = 0;
    }
    return t;
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("ClipLine[");
    sb.append(lineEqn);
    if (!finite0)
      sb.append(" inf0");
    else
      sb.append(" c0=" + Tools.f(c0));
    if (!finite1)
      sb.append(" inf1");
    else
      sb.append(" c1=" + Tools.f(c1));
    sb.append("]");
    return sb.toString();
  }
  private LineEqn lineEqn;
  private double c0, c1;
  private boolean finite0, finite1;
  private int usageCounter;
  public boolean adjustCounter(int amt) {
    usageCounter += amt;
    return usageCounter != 0;
  }

  public void render(Color c, int stroke, int markType) {
    if (nonEmpty()) {
      FPoint2 p1 = new FPoint2(endpoint(0)), p2 = new FPoint2(endpoint(1));

      if (MyMath.clipSegmentToRect(p1, p2, V.viewRect)) {
        V.pushColor(c, MyColor.cBLUE);
        V.pushStroke(stroke);
        V.drawLine(p1,p2);
        V.pop(2);
      }
    }
  }
  //  public FPoint2 start() {
  //    return endPoint(0);
  //  }
  //  public FPoint2 finish() {
  //    return endPoint(1);
  //  }
  //  public FPoint2 endpoint(int i) {
  //    return endPoint[i];
  //  }
}
