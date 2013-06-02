package dynvorn;

import java.awt.*;
import testbed.*;
import base.*;

public class EdMovingPt extends EdObject implements Globals, Renderable,
    IVornSite {

  static final boolean db = false;

  private static final double RADIUS = 5;

  private EdMovingPt() {
  }

  public EdMovingPt(FPoint2 loc, double theta) {
    setPoint(0, loc);
    setTheta(theta);
  }

  public void scale(double factor) {
    FPoint2 pt = getPoint(0);
    scalePoint(pt, factor);
    setPoint(0, pt);
  }

  public String toString() {
    String lbl = getLabel();
    if (lbl == null)
      lbl = "<unlabelled>";
    return lbl;
  }

  public void setPoint(int ptIndex, FPoint2 pt, boolean useGrid, TBAction action) {

    switch (ptIndex) {
    case 0:
      super.setPoint(ptIndex, pt, useGrid, action);
      if (complete())
        calcPoint1();
      break;
    default:
      {
        FPoint2 or = getPoint(0);
        theta = MyMath.polarAngle(or, pt);
        if (action != null && action.altPressed()) {
          speed = FPoint2.distance(or, pt) / RADIUS;
        }
        calcPoint1();
      }
      break;
    }
  }

  private void calcPoint1() {
    FPoint2 or = getPoint(0);
    super.setPoint(1, //
        MyMath.ptOnCircle(or, theta, speed * RADIUS), false, null);
  }

  public boolean complete() {
    //    if (ONEPT)
    //      return nVert() == 1;
    //
    return nVert() == 2;
  }

  public double distFrom(FPoint2 pt) {
    FPoint2 p0 = getPoint(0);
    FPoint2 p1 = getPoint(1);
    return MyMath.ptDistanceToSegment(pt, p0, p1, null);
  }
  //  final boolean ONEPT = true;
  //  public FPoint2 getPoint(int ptIndex) {
  //    if (ONEPT) {
  //      if (ptIndex == 1)
  //        return calcPoint2();
  //    }
  //    return super.getPoint(ptIndex);
  //  }

  public EdObjectFactory getFactory() {
    return FACTORY;
  }

  /**
   * Get bounding rectangle of object
   * @return FRect
   */
  public FRect getBounds() {
    FRect r = null;
    if (nPoints() >= 1)
      r = FRect.add(r, getPoint(0));
    return r;
  }

  /** 
   * @return
   */
  public int nVert() {
    return nPoints();
  }

  /**
   * Move entire object by a displacement
   * Default implementation just adjusts each point.
   * @param orig : a copy of the original object
   * @param delta : amount to move by
   */
  public void moveBy(EdObject orig, FPoint2 delta) {
    setPoint(0, FPoint2.add(orig.getPoint(0), delta, null));
  }

  public void render() {
    render(null, -1, -1);
  }

  public void render(Color color, int stroke, int markType) {
    if (color == null) {
      color = isSelected() ? MyColor.cRED : MyColor.cBLUE;
    }

    do {
      //      if (!complete())
      //        break;
      FPoint2 p0 = getPoint(0);
      FPoint2 p1 = getPoint(1);

      if (color != null)
        V.pushColor(color);
      if (stroke >= 0)
        V.pushStroke(stroke);
      V.pushScale(.7);
      if (p0 != null)
        V.mark(p0, MARK_DISC);
      V.popScale();
      if (p1 != null)
        EdSegment.plotDirectedLine(p0, p1, false, true);

      if (stroke >= 0)
        V.popStroke();

      if (p1 != null)
        plotLabel(MyMath.ptOnCircle(p0, theta + Math.PI / 2, FPoint2.distance(
            p0, p1) * .5));

      if (color != null)
        V.popColor();
    } while (false);
  }

  public static EdObjectFactory FACTORY = new EdObjectFactory() {

    public EdObject construct() {
      return new EdMovingPt();
    }

    public String getTag() {
      return "movingpt";
    }
//    public void write(StringBuilder sb, EdObject obj) {
//      EdMovingPt seg = (EdMovingPt) obj;
//      sb.append(seg.toString());
//    }

    public void write(StringBuilder sb, EdObject obj) {
      EdMovingPt pt = (EdMovingPt) obj;
      if (!pt.isActive())
        toString(sb, !pt.isActive());
      toString(sb, pt.theta);
      toString(sb, pt.getPoint(0));
    }

    public EdObject parse(Tokenizer s, int flags) {
      final boolean db = false;
      if (db)
        Streams.out.println("EdMovingPt, parse, next=" + s.peek().debug());

      EdMovingPt obj = new EdMovingPt();
      obj.setFlags(flags);
      obj.theta = s.extractDouble();
      obj.addPoint(s.extractFPoint2());
      obj.calcPoint1();
      return obj;
    }


    public String getMenuLabel() {
      return "Add Voronoi site";
    }

    public String getKeyEquivalent() {
      return "v";
    }
  };

  public FPoint2 positionAt(double tm) {
    FPoint2 pt = getPoint(0);
    return MyMath.ptOnCircle(pt, theta, tm * speed);
  }
  public FPoint2 getSiteLocation() {
    return getSiteLocation(0);
  }

  public FPoint2 getSiteLocation(double time) {
    return positionAt(time);
  }
  public FPoint2 getVelocity() {
    return MyMath.ptOnCircle(new FPoint2(), theta, speed);
  }

  public String getSiteName() {
    return getLabel();
  }
  public double getTheta() {
    return theta;
  }
  private double theta;
  private double speed = 1.0;

  public double getSpeed() {
    return speed;
  }

  public void setTheta(double d) {
    theta = d;
    calcPoint1();
  }

  public void setSpeed(double d) {
    speed = d;
    calcPoint1();
  }

  /**
   * Read points from editor, use them to build an array of IVornSite objects.
   * These objects will have their coordinates modified so they equal their
   * printed versions (for ease in debugging) 
   * @return
   */
  public static DArray getEditorSites() {
    DArray vs = Editor.readObjects(FACTORY, false, true);
    DArray a = new DArray();
    for (int i = 0; i < vs.size(); i++) {
      EdMovingPt pt = (EdMovingPt) vs.get(i);
      pt = (EdMovingPt) pt.clone();

      FPoint2 p = pt.getPoint(0);
      pt.setPoint(0, MyMath.snapToGrid(p, 1e-2));

      pt.speed = quantize(pt.speed);
      pt.theta = quantize(pt.theta);
      quantizePoint(pt, 0);
      pt.calcPoint1();
      a.add(pt);
    }
    return a;
  }

  private static final double QUANT_SIZE = 1e-2;

  public static double quantize(double n) {
    return MyMath.snapToGrid(n, QUANT_SIZE);
  }
  public static void quantizePoint(EdObject obj, int ptIndex) {
    FPoint2 pt = obj.getPoint(ptIndex);
    obj.setPoint(ptIndex, MyMath.snapToGrid(pt, QUANT_SIZE));
  }

  public static DArray getSitesAtTime(DArray sites, double time) {
    DArray a = new DArray();
    for (int i = 0; i < sites.size(); i++) {
      IVornSite site = (IVornSite) sites.get(i);
      //      EdMovingPt site = (EdMovingPt) sites.get(i);
      FPoint2 loc = MyMath.snapToGrid(site.getSiteLocation(time), QUANT_SIZE);
      a.add(new SimpleSite(loc, site.getSiteName()));
      //      EdMovingPt site2 = new EdMovingPt(loc, 0);
      //      site2.setLabel(site.getLabel());
      //      a.add(site2);
    }
    return a;
  }
}
