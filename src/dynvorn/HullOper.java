package dynvorn;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class HullOper implements TestBedOperation, Globals {
  /*! .enum  .private 4200   
    plotarr _
  */

    private static final int PLOTARR          = 4200;//!
/*!*/

  public void addControls() {
    C.sOpenTab("Hull");
    {
      C.sStaticText("Convex hull of moving points");
      C
          .sStaticText("Speed of point can be adjusted by holding down the alt key.");
      C.sCheckBox(PLOTARR, "Plot rays", null, false);
      //      C.sCheckBox(TOPOLOGY, "Plot topology", null, false);
    }
    C.sCloseTab();
  }

  public static HullOper singleton = new HullOper();

  private HullOper() {
  }

  public void processAction(TBAction a) {
  }

  //  private static DArray getPoints(DArray sites) {
  //    DArray a = new DArray();
  //    for (int i = 0; i < sites.size(); i++) {
  //      IVornSite v = (IVornSite) sites.get(i);
  //      a.add(v.getSiteLocation());
  //    }
  //    return a;
  //  }
  //
  public void runAlgorithm() {
    hull = null;
    //  Main.buildVorn();
    buildHull();
  }

  public void buildHull() {
    hull = new ConvexHull(Main.getSites(false), 0);
    //    DArray sites = new DArray();
    //    DArray vs = Main.getSites(false);
    //    sites.addAll(getPoints(vs));
    //    //  sites.addAll(getPoints(Main.getSites(false)));
    //    int[] si = MyMath.convexHull(sites);
    //    hull = new EdPolygon();
    //    for (int i = 0; i < si.length; i++)
    //      hull.addPoint(sites.getFPoint2(si[i]));
    //    // if (C.vb(TOPOLOGY))
    Main.setTopology(hull.getTopology());//getHullTopology(vs, si));
    Main.setConvexHull(hull);
  }

  public static String getHullTopology(DArray vornSites, int[] hullInd) {

    int lowest = 0;
    String lowestTr = null;

    DArray tr = new DArray();
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hullInd.length; i++) {
      IVornSite vi = (IVornSite) vornSites.get(hullInd[i]);
      String triple = vi.getSiteName();
      //      
      //      
      //      int j = MyMath.mod(i + 1, hullInd.length);
      //      int k = MyMath.mod(i + 2, hullInd.length);
      //      String triple = getTriple(vornSites, i, j, k);
      tr.add(triple);
      if (lowestTr == null || triple.compareTo(lowestTr) < 0) {
        lowestTr = triple;
        lowest = i;
      }
    }
    for (int i = 0; i < tr.size(); i++) {
      if (i != 0)
        sb.append(' ');
      sb.append(tr.getMod(i + lowest));
    }
    return sb.toString();
  }
  //  public static String getTriple(DArray vornSites, int i, int j, int k) {
  //    IVornSite vi = (IVornSite) vornSites.get(i);
  ////    IVornSite vj = (IVornSite) vornSites.get(j);
  ////    IVornSite vk = (IVornSite) vornSites.get(k);
  //    StringBuilder sb = new StringBuilder();
  //    sb.append(vi.getSiteName());
  ////    sb.append(vj.getSiteName());
  ////    sb.append(vk.getSiteName());
  //    return sb.toString();
  //  }

  public void paintView() {
    if (C.vb(PLOTARR)) {
      V.pushColor(MyColor.cLIGHTGRAY);
      V.pushStroke(STRK_THIN);
      DArray s = Editor.readObjects(EdMovingPt.FACTORY, false, true);
      for (int i = 0; i < s.size(); i++) {
        EdMovingPt pt = (EdMovingPt) s.get(i);
        FPoint2 pt1 = pt.positionAt(-200);
        FPoint2 pt2 = pt.positionAt(200);
        V.drawLine(pt1, pt2);
      }
      V.pop(2);
    }
    //  T.render(hull);
    //    if (topology != null) {
    //      V.pushColor(MyColor.cDARKGRAY);
    //      V.pushScale(.7);
    //      V.draw(topology, 0, 100, TX_CLAMP|60);
    //      V.pop(2);
    //      Streams.out.println("plotted "+topology);
    //    }
  }
  //  private EdPolygon hull;
  // private String topology;
  private ConvexHull hull;
}
