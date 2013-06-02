package dynvorn;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class HullSearchOper implements TestBedOperation, Globals {
  /*! .enum  .private 3650  sampleres printhist persist allevents
  */

    private static final int SAMPLERES        = 3650;//!
    private static final int PRINTHIST        = 3651;//!
    private static final int PERSIST          = 3652;//!
    private static final int ALLEVENTS        = 3653;//!
/* !*/

  public void addControls() {
    C.sOpenTab("Hull search");
    {
      C.sStaticText("Determines topologically distinct convex hulls.");

      C.sHide();
      C.sIntSpinner(SAMPLERES, "sample res",
          "Sampling resolution.  Sets delta t to 2^(-x).", 0, 20, 8, 1);
      C.sHide();
      C.sCheckBox(PRINTHIST, "plot text", "Displays history text", false);
      C.sOpen();
      C.sIntSlider(PERSIST, "lifespan",
          "adjusts amount of time individual event is displayed", 0, 20, 4, 1);
      C.sNewColumn();
      C.sCheckBox(ALLEVENTS, "plot all", "Plot all events at once", false);
      C.sClose();
    }
    C.sCloseTab();
  }

  public static HullSearchOper singleton = new HullSearchOper();

  private HullSearchOper() {
  }
  private void updatePointSet(DArray vs) {

    {
      StringBuilder sb = new StringBuilder();
      for (Iterator it = vs.iterator(); it.hasNext();) {
        IVornSite pt = (IVornSite) it.next();
        FPoint2 loc = pt.getSiteLocation();
        sb.append('|');
        if (pt instanceof EdMovingPt) {
          sb.append((int) (((EdMovingPt) pt).getTheta() * 100));
          sb.append(' ');
        }
        sb.append((int) (loc.x * 100));
        sb.append(' ');
        sb.append((int) (loc.y * 100));
      }
      String rep = sb.toString(); 
      double sampleRes = Math.pow(2, -C.vi(SAMPLERES));

      if (history == null || lastPointSet == null || !lastPointSet.equals(rep)
          || lastSampleRes != sampleRes) {
        lastPointSet = rep;
        lastSampleRes = sampleRes;
        if (history != null)
          history.stopThread();
        history = new HullHistory(vs, 1200, lastSampleRes);
      }
    }
  }

  public void paintView() {
    if (C.vb(PRINTHIST) && history != null) {
      V.pushScale(.6);
      V.pushColor(Color.darkGray);
      V.draw(history.toString(), 10, 90, TX_FRAME | TX_BGND | TX_CLAMP | 90);
      V.popColor();
      V.popScale();
    }
    if (history != null) {
      V.pushColor(MyColor.cBLUE);
      V.pushScale(1.0);
      V.draw(history.getInfo(), 100, 100, TX_BGND | TX_CLAMP);
      V.pop(2);
    }
    if (history != null) {

      if (C.vb(ALLEVENTS)) {
        DArray evts = history.getEvents(0, 10000);

        for (int i = 0; i < evts.size(); i++) {
          TopEvent evt = (TopEvent) evts.get(i);

          DArray br = bringPointIntoView(evt.getLocation());
          FPoint2 loc = br.getFPoint2(0);
          boolean moved = br.getBoolean(1);
          double theta = br.getDouble(2);

          V.pushColor(MyColor.cRED);
          V.drawCircle(loc, .6);
          if (moved) {
            EdSegment.plotArrowHead(loc, theta);
          }
          V.popColor();
          if (Editor.withLabels(false)) {
            V.pushScale(.6);
            V.draw(evt.getLabel() + "(" + Tools.f(evt.getTime()).trim() + ")",
                loc.x + 5, loc.y, TX_FRAME | TX_BGND);
            V.pop();
          }
        }
        //        V.pop();
      } else {
        double persist = (C.vi(PERSIST) + 5) / 3;
        double span = persist * persist;

        double t0 = Main.dispTime();
        DArray evts = history.getEvents(t0 - span, t0);

        for (int i = 0; i < evts.size(); i++) {
          TopEvent evt = (TopEvent) evts.get(i);

          DArray br = bringPointIntoView(evt.getLocation());
          FPoint2 loc = br.getFPoint2(0);
          boolean moved = br.getBoolean(1);
          double theta = br.getDouble(2);

          double s = (t0 - evt.getTime()) / span;
          V.pushColor(MyColor.get(MyColor.RED, .5 + s * 1));
          V.drawCircle(loc, (s + .2) * 1.4);
          if (moved) {
            EdSegment.plotArrowHead(loc, theta);
          }
          V.popColor();
          Editor.plotLabel(evt.getLabel(), loc.x + 5, loc.y, true);
        }
      }
    }
  }
  /**
   * Clip a point into range of the view
   * @param pt point to bring into range
   * @return DArray containing [0] adjusted point, [1] Boolean: true if
   *   it moved, [2] angle from center of view to point
   */
  public static DArray bringPointIntoView(FPoint2 pt) {
    FRect inset = new FRect(V.viewRect);
    inset.inset(3);
    FPoint2 mid = inset.midPoint();
    FPoint2 cpt = new FPoint2(pt);
    MyMath.clipSegmentToRect(mid, cpt, inset);
    DArray ret = new DArray(3);
    ret.add(cpt);
    ret.add(new Boolean(!cpt.equals(pt)));
    ret.addDouble(MyMath.polarAngle(mid, pt));
    return ret;
  }

  public void processAction(TBAction a) {
  }

  public void runAlgorithm() {
    updatePointSet(Main.getSites(true));
    HullOper.singleton.buildHull();
  }

  // string representation of last set of sites, to test if 
  // new has changed
  private String lastPointSet;
  private double lastSampleRes;
  private HullHistory history;

  public void cancelHistory() {
    if (history != null) {
      history.stopThread();
      history = null;
    }
  }
}
