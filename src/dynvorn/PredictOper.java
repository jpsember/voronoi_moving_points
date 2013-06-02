package dynvorn;

import java.util.*;
import base.*;
import testbed.*;

public class PredictOper implements TestBedOperation, Globals {
  /*! .enum  .private 3800   
  */

/* !*/

  public void addControls() {
    C.sOpenTab("Predict");
    {
      C.sStaticText("Predict next Topological Event."
          + "  Examines each edge of current Voronoi diagram, and"
          + " calculates when the circumcenters associated with each "
          + "endpoint will coincide; displays earliest such event.");
    }
    C.sCloseTab();
  }

  public void paintView() {
    //    T.render(testPt, MyColor.cRED, MARK_DISC);
    V.pushScale(1.7);
    T.render(nextEventPoint, MyColor.cRED, -1, MARK_CIRCLE);
    V.popScale();
  }

  public void processAction(TBAction a) {
  }

  public void runAlgorithm() {
    nextEventPoint = null;

    DArray vs =Main.getSites(true);
    siteMap = new HashMap();
    for (int i = 0; i < vs.size(); i++) {
      IVornSite s = (IVornSite) vs.get(i);
      siteMap.put(s.getSiteName(), s);
    }

    //    vornGraph = new VornGraph(vornSites);
    //    vornGraph.build(new VornFortune());
    //    predictNextEvent(dispTime);

    vornGraph = Main.buildVorn();
//    
//    DArray v2 = EdMovingPt.getSitesAtTime(vs, Main.dispTime());
//    
//    vornGraph = new VornGraph(v2);
//    Main.setVornGraph(vornGraph);
//    vornGraph.build(new VornFortune());
//    
//    
//    
    predictNextEvent(Main.dispTime());
  }
  private void predictNextEvent(double currTime) {

    final boolean db = true;

    do {
      if (db && T.update())
        T.msg("predict, vornGraph=" + vornGraph);
      if (vornGraph == null)
        break;

      if (db && T.update())
        T.msg("predictNextEvent");

      double nextEvtTime = -1;

      //      DArray nl = vornGraph.getNodeList();

      for (int vvId = vornGraph.startVert(); vvId < vornGraph.endVert(); vvId++) {

        //        int vvId = nl.getInt(i);

        if (db && T.update())
          T.msg("examining node " + vvId + T.show(vornGraph.getVertex(vvId)));

        //        Tools.warn("skipping for now...");if (false)

        if (vornGraph.getVertex(vvId).isAtInfinity()) {
          if (db && T.update())
            T.msg("node is at infinity, skipping");
          continue;
        }

        // examine each edge that we haven't already looked at
        for (int j = 0; j < 3; j++) {
          //vornGraph.nCount(vvId); j++) {
          int vvId2 = vornGraph.neighbor(vvId, j);

          IVornSite siteL = vornGraph.left(vvId, j);
          IVornSite siteR = vornGraph.right(vvId, j);
          if (db && T.update())
            T.msg(" neighbor " + j + ":" + vvId2 + " L=" + siteL.getSiteName()
                + " R=" + siteR.getSiteName());
          if (VornGraph.isInfiniteFace(siteL)
              || VornGraph.isInfiniteFace(siteR)) {
            if (db && T.update())
              T.msg(" one of them is infinite, skipping");
            continue;
          }

          // get site to right of next edge of this vertex

          IVornSite siteFar = vornGraph.right(vvId2, siteL);
          //          vornGraph.next(vvId2, siteL),
          //              siteL);
          //)fwdAdjacent(vvId2, siteL);
          if (db && T.update())
            T.msg("siteFar=" + siteFar.getSiteName() + " adjacent id=" + vvId2
                + " left=" + siteL.getSiteName() + "\n" + vornGraph);
          if (siteFar == null) {
            T.msg("no far site found:\nN=" + " L=" + siteL.getSiteName()
                + " R=" + siteR.getSiteName() + "\n" + vornGraph);
            continue;
          }
          if (VornGraph.isInfiniteFace(siteFar))
            continue;
          IVornSite siteNear = vornGraph.right(vornGraph.prev(vvId, siteL),
              siteL);
          //prev(vvId, siteL); //)prevSite(vvId, siteL);

          if (siteNear == null) {
            T.msg("no near site found:\n bwdAdjacent from vvId=" + vvId
                + ", siteL=" + siteL.getSiteName() + "\n" + "N=" + " L="
                + siteL.getSiteName() + " R=" + siteR.getSiteName() + " F="
                + siteFar.getSiteName() + "\n" + vornGraph);
            continue;
          }

          if (VornGraph.isInfiniteFace(siteNear))
            continue;

          if (siteNear == siteL || siteNear == siteR || siteNear == siteFar) {
            T.msg("near site same as one of other three:\nN="
                + siteNear.getSiteName() + " L=" + siteL.getSiteName() + " R="
                + siteR.getSiteName() + " F=" + siteFar.getSiteName() + "\n"
                + vornGraph);
            continue;
          }

          // convert sites to original, time=0 versions
          siteL = origSite(siteL);
          siteR = origSite(siteR);
          siteFar = origSite(siteFar);
          siteNear = origSite(siteNear);

          double[] p1 = VornUtil.vornVertexPath(siteNear, siteL, siteR);
          double[] p2 = VornUtil.vornVertexPath(siteL, siteR, siteFar);

          double t = VornUtil.calcNextVornPathCrossing(p1, p2, currTime);
          if (t < 0)
            continue;
          if (nextEvtTime < 0 || t < nextEvtTime) {
            nextEvtTime = t;
            nextEventPoint = VornUtil.calcPointOnVertexPath(p1, t);
          }
        }
      }
    } while (false);
  }

  private IVornSite origSite(IVornSite s) {
    IVornSite ret = (IVornSite) siteMap.get(s.getSiteName());
    if (ret == null)
      T.err("no original site found for: " + s.getSiteName());
    return ret;
  }

  private VornGraph vornGraph;
  // Map of site names -> vornSites object
  private Map siteMap;
  private FPoint2 nextEventPoint;

}
