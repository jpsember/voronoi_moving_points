package dynvorn;

import java.awt.*;
import java.util.*;
import base.*;
import testbed.*;

public class Main extends TestBed {
  private static final int MAXTIME = 2000;

  /*! .enum  .private   4000
  time rndtest   plotvdiag  rotate resetspeed
     ploteditor generate _ plottop plothull timescale  
       */

    private static final int TIME             = 4000;//!
    private static final int RNDTEST          = 4001;//!
    private static final int PLOTVDIAG        = 4002;//!
    private static final int ROTATE           = 4003;//!
    private static final int RESETSPEED       = 4004;//!
    private static final int PLOTEDITOR       = 4005;//!
    private static final int GENERATE         = 4006;//!
    private static final int PLOTTOP          = 4008;//!
    private static final int PLOTHULL         = 4009;//!
    private static final int TIMESCALE        = 4010;//!
/* !*/

  public static void main(String[] args) {
    // Construct an object for the application and pass args to it
    new Main().doMainGUI(args);
  }

  // -------------------------------------------------------
  // TestBed overrides
  // -------------------------------------------------------

  public void addOperations() {
    addOper(FortuneOper.singleton);
    addOper(BinarySearchOper.singleton);
    addOper(GeneratorOper.singleton);
    addOper(new PredictOper());
    addOper(new TrailsOper());
    addOper(HullOper.singleton);
    addOper(HullSearchOper.singleton);
 }

  public void initEditor() {
    Editor.addObjectType(EdPoint.FACTORY);
    Editor.addObjectType(EdMovingPt.FACTORY);
  }

  public void setParameters() {
    parms.appTitle = "Voronoi Diagram, Convex Hull of Moving Points";
    parms.menuTitle = "Main";
    parms.fileExt = "pts";
    parms.traceSteps = 500;

    // using grid exposes degeneracy problems with the Fortune Voronoi
    // algorithm that I am not about to solve
    parms.includeGrid = false;
  }

  public void paintView() {

    final boolean db = false;
    if (db)
      Streams.out.println("Main.paintView()");

    boolean editOper = TestBed.operNum() == 0;
    vornSites = null;
    dynSites = null;
    vornGraph = null;
    topology = null;
    convexHull = null;

    if (!editOper) {
      getSites(true);
      Editor.render(C.vb(PLOTEDITOR)
          || (dispTime == 0 && TestBed.oper() != FortuneOper.singleton), true,
          false);

      double t = dispTime();
      if (t > 0) {
        V.pushColor(MyColor.cBLUE);
        V.pushScale(1.0);
        V.draw("T=" + Tools.f(t).trim(), 0, 100, TX_CLAMP);
        V.pop(2);
      }
    }

    if (db)
      Streams.out.println("calling super.paintView()");

    if (TestBed.oper() != BinarySearchOper.singleton)
      BinarySearchOper.singleton.cancelHistory();
    super.paintView();

    if (C.vb(RNDTEST)) {
      if (T.lastEvent() == null) {
        GeneratorOper.generate(true);
        V.repaint();
      } else
        C.setb(RNDTEST, false);
    }

    //    if (T.lastEvent() != null) {
    //      C.setb(RNDTEST, false);
    //    }
    //
    if (vornSites != null) {
      for (int i = 0; i < vornSites.size(); i++) {
        IVornSite vs = (IVornSite) vornSites.get(i);
        V.pushScale(.65);
        FPoint2 pt = vs.getSiteLocation(dispTime);
        T.render(pt, MyColor.cBLUE);
        V.popScale();
        if (Editor.withLabels(false))
          Editor.plotLabel(vs.getSiteName(), MyMath.ptOnCircle(pt, Math.PI / 4,
              2.2), false, null);
      }
    }

    if (convexHull == null && C.vb(PLOTHULL))
       HullOper.singleton.buildHull();

    //    V.pushScale(.2);
    //    T.renderAll(traces, MyColor.cPURPLE, -1, MARK_DISC);
    //    V.popScale();
    //
    if (db)
      Streams.out
          .println("plotVDiag? vornGraph is null:" + (vornGraph == null));

    if (C.vb(PLOTVDIAG))
      T.render(vornGraph);
   // if (C.vb(PLOTHULL))
      T.render(convexHull);

    //    if (samples != null) {
    //      V.pushColor(MyColor.cRED);
    //
    //      FPoint2 prev = null;
    //      for (int i = 0; i < samples.size(); i++) {
    //        FPoint2 ps = samples.getFPoint2(i);
    //        if (prev != null)
    //          V.drawLine(prev, ps);
    //        prev = ps;
    //      }
    //      V.popColor();
    //    }
    //    //    T.render(testPt, MyColor.cRED, MARK_DISC);
    //    V.pushScale(1.7);
    //    T.render(nextEventPoint, MyColor.cRED, -1, MARK_CIRCLE);
    //    V.popScale();

    // Streams.out.println("vornGr=" + vornGraph);
    if (vornGraph != null) {
      //      if (C.vb(PLOTTOP)) 
      topology = vornGraph.getTopology();

      //      int cell = C.vi(SHOWCELL);
      //      if (cell >= 0 && cell < vornGraph.nSites()) {
      //        EdPolygon p = vornGraph.getCell(vornGraph.getSite(cell));
      //        T.render(p, MyColor.cRED, STRK_THICK, -1);
      //      }
    }

    if (C.vb(PLOTTOP) && topology != null) {
      V.pushScale(.8);
      V.pushColor(Color.darkGray);
      V.draw(topology, 10, 90, TX_FRAME | TX_BGND | TX_CLAMP | 60);
      V.popColor();
      V.popScale();
    }

    if (db)
      Streams.out.println("finished paintView()\n\n\n");

  }
  //  public static DArray getOrigSites() {
  //  getSites(true);
  //  return 
  //  }

  public static DArray getSites(boolean dynamic) {
    if (vornSites == null || dynSites == null) {
      dynSites = EdMovingPt.getEditorSites();
      DArray ps = Editor.readObjects(EdPoint.FACTORY, false, true);
      for (int i = 0; i < ps.size(); i++) {
        EdObject pt = (EdObject) ps.get(i);
        dynSites.add(new SimpleSite(pt.getPoint(0), pt.getLabel()));
      }

      siteMap = new HashMap();
      for (int i = 0; i < dynSites.size(); i++) {
        IVornSite s = (IVornSite) dynSites.get(i);
        siteMap.put(s.getSiteName(), s);
      }
      double tmScale = 2.4 / Math.pow(2, C.vi(TIMESCALE) + 1);
      dispTime = C.vi(TIME) * tmScale;
      vornSites = EdMovingPt.getSitesAtTime(dynSites, dispTime);
    }
    return dynamic ? dynSites : vornSites;
  }
  public static double dispTime() {
    return dispTime;
  }
  public void processAction(TBAction a) {
    if (a.code == TBAction.CTRLVALUE) {
      switch (a.ctrlId) {
      case RESETSPEED:
        {
          for (Iterator it = Editor.readObjects(EdMovingPt.FACTORY, true, true)
              .iterator(); it.hasNext();) {
            EdMovingPt pt = (EdMovingPt) it.next();
            pt.setSpeed(1.0);
          }
        }
        break;
      case ROTATE:
        {
          int rv = C.vi(ROTATE);
          if (prevValue < 0) {
            prevValue = rv;
          }
          int diff = prevValue - rv;
          for (Iterator it = Editor.readObjects(EdMovingPt.FACTORY, true, true)
              .iterator(); it.hasNext();) {
            EdMovingPt pt = (EdMovingPt) it.next();
            double theta = pt.getTheta();
            pt.setTheta(theta + Math.PI / 300 * diff);
          }
          prevValue = rv;
        }
        break;
      case GENERATE:
        GeneratorOper.generate(true);
        break;
      }
      //      if (a.ctrlId >= GENERATE && a.ctrlId <= GENERATEEND)
      //        generate();
      //      if (a.ctrlId >= COUNT && a.ctrlID < GENERATE)
      //        C.
    }
  }
  public void addControls() {
    //    C.sStaticText("Display Voronoi diagram of moving points.  "
    //        + "Hold 'alt/option' key to adjust speed of site.");
    {
      C.sOpen("Time");
      C.sIntSpinner(TIMESCALE, "Scale:", "Sets scale for time adjustment", 0,
          10, 5, 1);
      C.sNewColumn();
      C.sIntSlider(TIME, "t:", "Set time to calculate Voronoi diagram for", 0,
          MAXTIME, 0, 1);
      C.sClose();

      C.sOpen("Display");
      C.sCheckBox(PLOTEDITOR, "show items",
          "if unchecked, hides unselected editor items", false);
      C.sCheckBox(PLOTVDIAG, "plot Voronoi diagram", null, true);
      C.sNewColumn();
      C.sCheckBox(PLOTHULL, "plot convex hull", null, false);
      C.sCheckBox(PLOTTOP, "topology",
          "Display string describing diagram's topology", false);
      //      C.sIntSpinner(SHOWCELL, "db:cell",
      //          "Highlight a cell of the Voronoi diagram", -1, 50, -1, 1);
      C.sClose();
    }

    {
      //      C.sOpen("Editing");
      C.sHide();
      C.sIntSlider(ROTATE, "Rotate", "Rotate selected sites", //
          0, 1000, 0, 100);
      C.sHide();
      C.sButton(RESETSPEED, "Reset speed",
          "Reset speed of selected sites to default");
      //C.sNewColumn();
      C.sButton(GENERATE, "Generate",
          "Generate sites based on Generator tab settings");
      C.sHide();
      C.sCheckBox(RNDTEST, "Test", "Repeatedly generate random sites", false);

      //      C.sClose();
    }
    //
  }

  public static void setVornGraph(VornGraph g) {
    vornGraph = g;
  }
  public static void setConvexHull(ConvexHull hull) {
    convexHull = hull;
  }

  public static void setTopology(String s) {
    topology = s;
  }
  private static String topology;

  public static VornGraph buildVorn() {
    DArray v2 = getSites(false);

    vornGraph = new VornGraph(v2, false);
    //    Main.setVornGraph(vornGraph);
    vornGraph.build(new VornFortune());
    return vornGraph;
  }

  // Voronoi sites at time t
  private static DArray vornSites;
  private static DArray dynSites;
  private static VornGraph vornGraph;

  // Map of site names -> vornSites object
  private static Map siteMap;
  private static double dispTime;

  //  private static DArray polygons;
  private static int prevValue = -1;
  private static ConvexHull convexHull;

}
