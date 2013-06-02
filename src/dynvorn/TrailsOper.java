package dynvorn;

import base.*;
import testbed.*;

public class TrailsOper implements TestBedOperation, Globals {
  /*! .enum  .private 3700  res
  */

    private static final int RES              = 3700;//!
/* !*/

  public void addControls() {
    C.sOpenTab("Trails");
    {
      C.sStaticText("Generates Voronoi diagrams at even intervals, "
          + "plots curves formed by its vertices; this can be very slow!"
          + " It was added as an experiment to find out what these "
          + "paths were.");
      C.sIntSpinner(RES, "resolution", "Sampling resolution", //
          1, 20, 5, 1);
    }
    C.sCloseTab();
  }

  public void paintView() {
    V.pushScale(.2);
    T.renderAll(traces, MyColor.cPURPLE, -1, MARK_DISC);
    V.popScale();
  }

  public void processAction(TBAction a) {
  }

  public void runAlgorithm() {
    {
      VornGraph vornGraph;
      traces = new DArray();
      double res = Math.pow(C.vi(RES), 2) * .05;
      for (double t = 0; t <= Main.dispTime(); t += res) {
        //          vornSites = EdMovingPt.getSitesAtTime(vs, t);
        vornGraph = new VornGraph(EdMovingPt.getSitesAtTime(
            Main.getSites(true), t), false);
        Main.setVornGraph(vornGraph);
        vornGraph.build(new VornFortune());
        for (int ni = vornGraph.startVert(); ni < vornGraph.endVert(); ni++) {
          VornVertex vert = vornGraph.getVertex(ni);
          traces.add(vert.getLocation());
        }
      }
    }
  }
  private DArray traces;

}
