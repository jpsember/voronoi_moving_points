package dynvorn;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class FortuneOper implements TestBedOperation, Globals {
  /*! .enum  .private 3500  slowsweep  showsort showtext fverbose
   eventline closestpair empcirc
  */

  private static final int SLOWSWEEP = 3500;//!
  private static final int SHOWSORT = 3501;//!
  private static final int SHOWTEXT = 3502;//!
  private static final int FVERBOSE = 3503;//!
  private static final int EVENTLINE = 3504;//!
  private static final int CLOSESTPAIR = 3505;//!
  private static final int EMPCIRC = 3506;//!
  /*!*/

  public void addControls() {
    C.sOpenTab("Fortune");
    {
      C
          .sStaticText("Fortune's plane sweep algorithm for generating Voronoi diagrams");

      C.sCheckBox(SLOWSWEEP, "smooth sweep", "Move sweep line smoothly, for "
          + "demonstration purposes", true);
      C.sCheckBox(FVERBOSE, "verbose", "displays additional algorithm steps",
          false);
      //        C.sCheckBox(SLOWALG, "simple",
      //            "use simple, slow algorithm for comparison purposes", false);
      C.sCheckBox(SHOWSORT, "show sort values",
          "displays values used to sort parabolas", false);
      C.sCheckBox(SHOWTEXT, "frontier text",
          "displays frontier as column of text", false);
      C.sCheckBox(EVENTLINE, "event lines",
          "displays lines for pending vertex events", false);
      C.sCheckBox(CLOSESTPAIR, "closest pair", null, false);
      C.sCheckBox(EMPCIRC, "empty disc", null, false);
    }
    C.sCloseTab();
  }

  public static FortuneOper singleton = new FortuneOper();

  private FortuneOper() {
  }

  public void processAction(TBAction a) {
  }

  public void runAlgorithm() {
    cpair = null;
    empcirc = null;
    emporg = null;
    VornFortune f = new VornFortune();
    f.setOptions((C.vb(SHOWSORT) ? VornFortune.OPT_SORTVALUES : 0)
        | (C.vb(SHOWTEXT) ? VornFortune.OPT_FRONTIERTEXT : 0)
        | (C.vb(FVERBOSE) ? VornFortune.OPT_VERBOSE : 0)
        | (C.vb(EVENTLINE) ? VornFortune.OPT_EVENTLINE : 0));
    if (C.vb(SLOWSWEEP))
      f.setSlowSweep(.4);
    T.show(f);
    VornGraph vornGraph = new VornGraph(Main.getSites(false), true);
    Main.setVornGraph(vornGraph);
    vornGraph.build(f);

    if (C.vb(CLOSESTPAIR)) {
      DArray s = Main.getSites(false);
      double minLen = 1e10;
      for (int i = 0; i < s.size(); i++) {
        IVornSite si = (IVornSite) s.get(i);
        for (int j = i + 1; j < s.size(); j++) {
          IVornSite sj = (IVornSite) s.get(j);
          double len = FPoint2.distanceSquared(si.getSiteLocation(), sj
              .getSiteLocation());
          if (len < minLen) {
            minLen = len;
            cpair = new EdSegment(si.getSiteLocation(), sj.getSiteLocation());

          }
        }
      }

    }

    if (C.vb(EMPCIRC)) {
      FPoint2 best = null;
      double bestDist = 0;

      FRect r = V.viewRect;

      for (int i = vornGraph.startVert(); i < vornGraph.endVert(); i++) {
        VornVertex vv = vornGraph.getVertex(i);

        FPoint2 c = vv.getLocation();
        if (!r.contains(c))
          continue;

        double rad = 1e10;
        for (int j = 0; j < vornGraph.nSites(); j++) {
          IVornSite vs = vornGraph.getSite(j);
          double dist = FPoint2.distance(c, vs.getSiteLocation());
          rad = Math.min(rad, dist);
        }
        if (best == null || bestDist < rad) {
          bestDist = rad;
          best = c;
        }
      }
      if (best != null) {
        empcirc = new EdDisc(best, bestDist);
        emporg = best;
      }
    }

  }
  private Renderable cpair;
  private Renderable empcirc;
  private FPoint2 emporg;
  public void paintView() {
    // T.render(cpair, MyColor.cRED, STRK_THICK, -1);
    if (cpair != null) {
      EdSegment s = (EdSegment) cpair;
      FPoint2 m = FPoint2.midPoint(s.getPoint(0), s.getPoint(1));
      double rad = FPoint2.distance(s.getPoint(0), s.getPoint(1));
      T.render(new EdDisc(m, rad + 1.2));
    }
    T.render(empcirc, MyColor.cRED, STRK_THICK, -1);
    T.render(emporg, MyColor.cRED, -1, MARK_X);
  }
}
