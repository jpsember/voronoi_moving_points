package dynvorn;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class VornFortune implements VornAlgorithm, Renderable, Globals {
  //  private static final boolean VERTEVENTFILTER = false;

  private static final double EPSILON2 = 1e-8;

  private static final int EVT_SITE = 0;
  private static final int EVT_VERTEX = 1;
  private boolean verbose;

  private static boolean option(int n) {
    return (options & n) == n;
  }

  public void setOptions(int n) {
    options = n;
    verbose = (options & OPT_VERBOSE) != 0;
  }
  private static int options;

  public static final int OPT_SORTVALUES = (1 << 0);
  public static final int OPT_FRONTIERTEXT = (1 << 1);
  public static final int OPT_VERBOSE = (1 << 2);
  public static final int OPT_EVENTLINE = (1 << 3);

  public void setSlowSweep(double stepSize) {
    sweepSpeed = stepSize;
  }

  public DArray build(DArray sites) {
    final boolean db = verbose && true;

    this.sites = sites;

    sweepLine = new SweepLine(T.active() ? sweepSpeed : 0);
    vornEdges = new DArray();

    if (db && T.update())
      T.msg("VornFortune build, sites" + T.showAll(sites, MyColor.cRED));
    initFrontier();
    initEventQueue();

    FPoint2 lastSitePos = null;

    while (true) {
      if (sweepLine.slowFlag()) {
        double nextPos = Math.max(sweepLine.position(), V.viewRect.width);
        if (!eventQueue.isEmpty()) {
          VornEvent evt = (VornEvent) eventQueue.first();
          nextPos = evt.loc.x;
        }
        if (!sweepLine.advanceTo(nextPos))
          continue;
      }
      if (eventQueue.isEmpty()) {
        break;
      }

      VornEvent evt = popEvent();

      if (!sweepLine.slowFlag())
        sweepLine.advanceTo(evt.loc.x);

      switch (evt.type) {
      case EVT_SITE:
        if (lastSitePos != null && lastSitePos.equals(evt.loc))
          break;
        lastSitePos = evt.loc;
        processSiteEvent(evt);
        break;
      case EVT_VERTEX:
        processVertexEvent(evt);
        break;
      }
    }

    addRemainingEdges();
    // throw out sites array, so we don't render anything
    // now that we're complete
    this.sites = null;
    return vornEdges;
  }

  private void addRemainingEdges() {
    final boolean db = verbose && true;
    if (db && T.update())
      T.msg("addRemainingEdges from frontier");
    for (Iterator it = frontier.iterator(); it.hasNext();) {
      ActiveEdge ae = (ActiveEdge) it.next();
      if (db && T.update())
        T.msg("adding edge for " + ae + T.show(ae.vornEdge));
      vornEdges.add(ae.vornEdge);
    }
  }

  private void processVertexEvent(VornEvent evt) {
    final boolean db = verbose && true;

    if (db && T.update())
      T.msg("processVertexEvent" + T.show(evt, Color.red)
          + T.show(evt.loc2, null, -1, MARK_X) + " e1=" + evt.e1 + " e2="
          + evt.e2);

    do {
      // are edges still active, and still neighbors?
      if (!evt.e1.isActive() || !evt.e2.isActive() || evt.e1.nextEdge != evt.e2) {
        if (db && T.update())
          T.msg("edges are no longer neighbors, " + evt.e1 + " active="
              + evt.e1.isActive() + "\n " + evt.e2 + " active="
              + evt.e2.isActive());
        break;
      }

      // in degenerate situations, more than two edges may have reached
      // this location. Iterate in both directions to find the lowest and
      // highest such edges.

      ActiveEdge eLow = evt.e1;
      ActiveEdge eHigh = evt.e2;

      while (true) {
        ActiveEdge ePrev = eLow.prevEdge;
        if (ePrev == null)
          break;
        FPoint2 pt = ePrev.calcCurrentPoint0(sweepLine.position());
        if (pt == INFINITY)
          break;
        double dist = pt.distance(evt.loc2);
        if (db && T.update())
          T.msg("comparing previous edge" + T.show(ePrev) + " position"
              + T.show(pt) + " with vertex position, dist=" + dist);
        if (dist > EPSILON2)
          break;
        if (db && T.update())
          T.msg("extending eLow");
        eLow = ePrev;
      }
      while (true) {
        ActiveEdge eNext = eHigh.nextEdge;
        if (eNext == null)
          break;
        FPoint2 pt = eNext.calcCurrentPoint0(sweepLine.position());
        if (pt == INFINITY)
          break;
        double dist = pt.distance(evt.loc2);
        if (db && T.update())
          T.msg("comparing next edge" + T.show(eNext) + " position"
              + T.show(pt) + " with vertex position, dist=" + dist);
        if (dist > EPSILON2)
          break;
        if (db && T.update())
          T.msg("extending eHigh");
        eHigh = eNext;
      }

      //      DArray sl = null;
      //
      //      if (VERTEVENTFILTER) {
      //        sl = new DArray();
      //        sl.add(eLow.rightSite().getSiteName());
      //      }
      //
      for (ActiveEdge edge = eLow;; edge = edge.nextEdge) {
        //        if (VERTEVENTFILTER)
        //          sl.add(edge.leftSite().getSiteName());
        if (db && T.update())
          T.msg("terminating edge " + T.show(edge));
        edge.terminate(evt.loc2);
        if (edge == eHigh)
          break;
      }
      //      if (VERTEVENTFILTER) {
      //        postVertexFlags(sl, evt);
      //      }

      // create active edge excluding the removed parabola
      if (db && T.update())
        T.msg("creating active edge, leftSite=" + eHigh.leftSite()
            + ", rightSite=" + eLow.rightSite());
      VornEdge ve = new VornEdge(eHigh.leftSite(), eLow.rightSite());

      if (!ve.valid()) {
        T.err("vornEdge not valid: " + T.show(ve) + ve);
      }

      // clip edge to this vertex
      if (db && T.update())
        T.msg("clipping edge to " + evt.loc2.toString(true));
      ve.setStart(evt.loc2);

      ActiveEdge a1 = new ActiveEdge(ve, 0);
      a1.setSortValue(eLow, eHigh);

      join(eLow.prevEdge, a1);
      join(a1, eHigh.nextEdge);

      ActiveEdge eRem = eLow;
      while (true) {
        ActiveEdge eNext = eRem.nextEdge;
        if (db && T.update())
          T.msg("removing edge" + T.show(eRem));
        removeActiveEdge(eRem);
        if (eRem == eHigh)
          break;
        eRem = eNext;
      }

      if (db && T.update())
        T.msg("adding new edge" + T.show(a1));
      frontier.add(a1);

      // remove pending status of this event
      evt.e1.pendingVertexEvent = null;

      // post additional vertex events, since order
      // of edges has changed
      if (a1.prevEdge != null)
        postVertexEvent(a1.prevEdge);
      postVertexEvent(a1);
    } while (false);
  }

  //  private void postVertexFlags(IVornSite sa, IVornSite sb, IVornSite sc,
  //      VornEvent evt) {
  //    DArray sl = new DArray(3);
  //    sl.add(sa.getSiteName());
  //    sl.add(sb.getSiteName());
  //    sl.add(sc.getSiteName());
  //    postVertexFlags(sl, evt);
  //  }
  //
  //  private void postVertexFlags(DArray sl, VornEvent evt) {
  //    final boolean db = verbose && true;
  //    // sort sites by name
  //    sl.sort(String.CASE_INSENSITIVE_ORDER);
  //    StringBuilder sb = new StringBuilder();
  //    for (int i = 0; i < sl.size(); i++) {
  //      for (int j = i + 1; j < sl.size(); j++) {
  //        for (int k = j + 1; k < sl.size(); k++) {
  //          sb.setLength(0);
  //          sb.append(sl.get(i));
  //          sb.append(sl.get(j));
  //          sb.append(sl.get(k));
  //          String lbl = sb.toString();
  //          if (db && T.update())
  //            T.msg("adding vertex event label " + lbl + " to set");
  //          vertEventFlags.put(lbl, evt);
  //        }
  //      }
  //    }
  //  }
  //
  private void removeActiveEdge(ActiveEdge ae) {
    if (ae.other != null)
      ae.other.other = null;
    frontier.remove(ae);
    ae.nextEdge = ae.prevEdge = null;
    ae.sortValue = -1;
  }

  private void processSiteEvent(VornEvent evt) {
    final boolean db = verbose && true;
    final boolean db2 = db && true;

    IVornSite site = evt.siteA;

    if (db && T.update())
      T.msg("processSiteEvent" + T.show(site, Color.red));

    // if this is the first site encountered, we cannot construct
    // an edge; just mark this as the first site
    if (frontier.isEmpty() && firstSite == null) {
      firstSite = site;
      if (db && T.update())
        T.msg("setting firstSite to " + site);
    } else {

      // determine which existing site's parabola we are going to split
      IVornSite splitSite = null;
      ActiveEdge existing = null;
      boolean existingToLeft = false;

      double sortLow = VornEvent.SORT_MIN;
      double sortHigh = VornEvent.SORT_MAX;

      if (frontier.isEmpty()) {
        splitSite = firstSite;
      } else {

        if (db && T.update())
          T.msg("looking for insert position for site " + site + T.show(site));

        TreeSet insertPos = (TreeSet) frontier.tailSet(site);

        if (db2 && T.update())
          T.msg("insertPos="
              + (insertPos.isEmpty() ? "<end>" : insertPos.first()));

        if (insertPos.isEmpty()) {
          existing = (ActiveEdge) frontier.last();
          sortLow = existing.sortValue();
          //          sortHigh = VornEvent.SORT_MAX;
          splitSite = existing.leftSite();
          existingToLeft = true;
        } else {
          existing = (ActiveEdge) insertPos.first();
          sortHigh = existing.sortValue();
          //          sortLow = 0;
          if (existing.prevEdge != null)
            sortLow = existing.prevEdge.sortValue();

          splitSite = existing.rightSite();
          if (existing.prevEdge != null) {
            if (existing.prevEdge.leftSite() != existing.rightSite())
              T.err("existing site mismatch");
          }
        }
      }

      FPoint2 special = null;

      // special case: if site has appeared at same y-coordinate as
      // active edge, generate new arcs and edges immediately
      if (existing != null) {
        if (db && T.update())
          T.msg("special case" + T.show(existing));
        ActiveEdge sp = null;
        double ySplit = site.getSiteLocation().y;
        for (int pass = 0; sp == null && pass < 3; pass++) {
          ActiveEdge e2 = existing;
          if (pass == 1)
            e2 = existing.nextEdge;
          else if (pass == 2)
            e2 = existing.prevEdge;
          if (e2 == null)
            continue;
          FPoint2 pt = e2.calcCurrentPoint0(sweepLine.position());
          if (pt == INFINITY)
            continue;
          if (db && T.update())
            T.msg("checking if distance between pt.y=" + pt.y + " and ySplit="
                + ySplit + " < eps; " + Math.abs(pt.y - ySplit));
          final double EPSILON = .06;
          if (Math.abs(pt.y - ySplit) < EPSILON) {
            special = pt;
            sp = e2;
          }
        }
        if (sp != null) {
          existing = sp;
          if (db && T.update())
            T.msg("special case, process immediately at " + sp);
        }
      }

      if (special != null) {
        ActiveEdge c = existing;
        if (db && T.update())
          T.msg("terminating edge" + T.show(c) + " at point" + T.show(special)
              + ", c=" + c);
        c.terminate(special);

        // the new site will have the site being split will be to right, 
        // new site will be to left
        VornEdge ve1 = new VornEdge(site, c.rightSite());
        VornEdge ve2 = new VornEdge(c.leftSite(), site);
        if (db && T.update()) {
          T.msg("clipping new edges to " + special + T.show(ve1) + T.show(ve2));
        }
        ve1.setStart(special);
        ve2.setStart(special);

        ActiveEdge a1 = new ActiveEdge(ve1, 0);
        ActiveEdge a2 = new ActiveEdge(ve2, 0);

        if (db && T.update())
          T.msg("new active edges"
              + T.show(a1, MyColor.cDARKGREEN, STRK_THICK, -1)
              + T.show(a2, MyColor.cDARKGREEN, STRK_THICK, -1));
        sortLow = VornEvent.SORT_MIN;
        sortHigh = VornEvent.SORT_MAX;
        if (c.prevEdge != null)
          sortLow = c.prevEdge.sortValue();
        if (c.nextEdge != null)
          sortHigh = c.nextEdge.sortValue();

        if (db && T.update())
          T.msg("sortLow=" + sortLow + " high=" + sortHigh);
        a1.setSortValue(sortLow + .33 * (sortHigh - sortLow));
        a2.setSortValue(sortLow + .66 * (sortHigh - sortLow));
        if (db && T.update())
          T.msg("joining prevEdge " + c.prevEdge + " to a1=" + a1 + " to a2="
              + a2 + " to nextEdge=" + c.nextEdge);

        join(a1, a2);
        join(c.prevEdge, a1);
        join(a2, c.nextEdge);
        if (db && T.update())
          T.msg("after joining");

        //        if (VERTEVENTFILTER) 
        //          postVertexFlags(c.leftSite(), c.rightSite(), site, evt);

        removeActiveEdge(c);
        if (db && T.update())
          T.msg("after removing " + c);
        // remove pending status of this event; not sure if this is required
        c.pendingVertexEvent = null;

        frontier.add(a1);
        frontier.add(a2);
        if (db && T.update())
          T.msg("after adding a1=" + a1 + " and a2=" + a2);
        // delete pending vertex event that involved triple that 
        // new site has been inserted between
        if (a1.prevEdge != null) {
          VornEvent rem = a1.prevEdge.pendingVertexEvent;
          a1.prevEdge.pendingVertexEvent = null;
          if (rem != null) {
            eventQueue.remove(rem);
          }
        }
        postVertexEvent(a1.prevEdge);
        if (!(a2.nextEdge != null && a2.nextEdge.leftSite() == a1.rightSite())) {
          postVertexEvent(a2);
        }

      } else {
        if (db && T.update())
          T.msg("site to be split is " + splitSite + T.show(splitSite));

        // the new site will be to the site being split will be to right, new site will be to left
        VornEdge ve = new VornEdge(site, splitSite);

        // if new site is at same x as split site, it's a special
        // case; we generate a single active edge, with its starting
        // endpoint at infinity (off the screen to the left)
        ActiveEdge a2 = null;
        ActiveEdge a1 = new ActiveEdge(ve, 0);
        if (site.getSiteLocation().x == splitSite.getSiteLocation().x) {
          if (db2 && T.update())
            T.msg("setting sort values, sortLow=" + sortLow + ", high="
                + sortHigh);
          a1.setSortValue(sortLow + .5 * (sortHigh - sortLow));
          if (db2 && T.update())
            T.msg("adding " + a1 + " to frontier" + T.show(a1));

          if (existing != null) {
            if (existingToLeft) {
              ActiveEdge tmp = existing.nextEdge;
              join(existing, a1);
              join(a1, tmp);
            } else {
              ActiveEdge tmp = existing.prevEdge;
              join(a1, existing);
              join(tmp, a1);
            }
          }
          frontier.add(a1);
          // delete pending vertex event that involved triple that 
          // new site has been inserted between
          if (a1.prevEdge != null) {
            VornEvent rem = a1.prevEdge.pendingVertexEvent;
            a1.prevEdge.pendingVertexEvent = null;
            if (rem != null) {
              if (db && T.update())
                T.msg("removing pending vertex event" + T.show(rem));
              eventQueue.remove(rem);
            }
          }
          if (db2 && T.update())
            T.msg("new site=" + site + " existing=" + splitSite);
          postVertexEvent(a1.prevEdge);
        } else {
          a2 = new ActiveEdge(ve, 1);
          if (db2 && T.update())
            T.msg("setting sort values, sortLow=" + sortLow + ", high="
                + sortHigh);
          a1.setSortValue(sortLow + .33 * (sortHigh - sortLow));
          a2.setSortValue(sortLow + .66 * (sortHigh - sortLow));

          a1.setAssociatedEdge(a2);
          a2.setAssociatedEdge(a1);
          if (db2 && T.update())
            T.msg("adding " + a1 + "," + a2 + " to frontier" + T.show(a1)
                + T.show(a2));
          join(a1, a2);

          if (existing != null) {
            if (existingToLeft) {
              ActiveEdge tmp = existing.nextEdge;
              join(existing, a1);
              join(a2, tmp);
            } else {
              ActiveEdge tmp = existing.prevEdge;
              join(a2, existing);
              join(tmp, a1);
            }
          }
          frontier.add(a1);
          frontier.add(a2);
          // delete pending vertex event that involved triple that 
          // new site has been inserted between
          if (a1.prevEdge != null) {
            VornEvent rem = a1.prevEdge.pendingVertexEvent;
            a1.prevEdge.pendingVertexEvent = null;
            if (rem != null) {
              if (db && T.update())
                T.msg("removing pending vertex event" + T.show(rem));
              eventQueue.remove(rem);
            }
          }

          if (db2 && T.update())
            T.msg("new site=" + site + " existing=" + splitSite);

          postVertexEvent(a1.prevEdge);
          postVertexEvent(a2);
        }
      }
    }
  }
  //  private Inf inf = new Inf("vornfortune", 10000);

  /**
   * Post vertex events, if necessary, as a result of an edge being 
   * created or deleted
   * @param ae lower of the two edges contributing sites
   */
  private void postVertexEvent(ActiveEdge ae) {

    do {
      final boolean db = verbose && true;
      final boolean db2 = verbose && true;
      if (db && T.update())
        T.msg("postVertexEvent " + ae);
      if (ae == null)
        break;

      ActiveEdge e2 = ae.nextEdge;
      if (e2 == null)
        break;

      if (db2 && T.update())
        T.msg("sites involved: " + ae.rightSite() + "/" + ae.leftSite() + "/"
            + e2.leftSite());
      if (ae.rightSite() == e2.leftSite())
        break;

      //      // if the three sites involved were just involved in a vertex event,
      //      // ignore.
      //      if (VERTEVENTFILTER) {
      //        DArray a = new DArray();
      //        a.add(ae.rightSite().getSiteName());
      //        a.add(ae.leftSite().getSiteName());
      //        a.add(e2.leftSite().getSiteName());
      //        // sort sites by name
      //        a.sort(String.CASE_INSENSITIVE_ORDER);
      //        String lbl = a.getString(0) + a.getString(1) + a.getString(2);
      //        if (db && T.update())
      //          T.msg("seeing if vertex event labelled '" + lbl + "' exists: "
      //              + vertEventFlags.get(lbl));
      //        if (vertEventFlags.containsKey(lbl)) {
      //          if (db && T.update())
      //            T.msg("yes, ignoring");
      //          break;
      //        }
      //      }

      FPoint2 p0 = ae.rightSite().getSiteLocation();
      FPoint2 p1 = ae.leftSite().getSiteLocation();
      FPoint2 p2 = e2.leftSite().getSiteLocation();

      if (false) { // don't know if this is required
        // sort points lexicographically, so we get consistent results;
        // we want the same set of points to report the same circumcenter
        // regardless of the order.
        FPoint2 tmp;
        if (p0.x > p1.x) {
          tmp = p0;
          p0 = p1;
          p1 = tmp;
        }
        if (p1.x > p2.x) {
          tmp = p1;
          p1 = p2;
          p2 = tmp;
        }
        if (p0.x > p1.x) {
          tmp = p0;
          p0 = p1;
          p1 = tmp;
        }
      }

      DArray rc = MyMath.calcCircumCenter(p0, p1, p2);
      double radius = rc.getDouble(0);
      if (radius > 20000 || Double.isNaN(radius) || Double.isInfinite(radius)) {
        if (db && T.update())
          T.msg("radius is undefined for\n p0=" + p0.toString(true) + "\n p1="
              + p1.toString(true) + "\n p2=" + p2.toString(true));
        break;
      }
      FPoint2 center = rc.getFPoint2(1);

      if (db && T.update())
        T.msg("circumCenter of points" + T.show(p0) + T.show(p1) + T.show(p2)
            + T.show(new EdDisc(center, radius))
            + T.show(center, null, 1, MARK_X));

      FPoint2 loc = new FPoint2(center.x + radius, center.y);

      VornEvent evt = null;

      {
        boolean ignore = false;
        for (int edgePass = 0; edgePass < 2; edgePass++) {

          ActiveEdge actEdge = (edgePass == 0) ? ae : e2;
          // make sure the circumcenter is in the right direction along
          // our edge
          double t = actEdge.vornEdge.lineEqn().parameterFor(center);
          FPoint2 c0 = actEdge.calcCurrentPoint0(sweepLine.position());
          if (c0 == INFINITY) {
            ignore = true;
            break;
          }
          double t0 = actEdge.vornEdge.lineEqn().parameterFor(c0);

          if (true) {
            double dist = FPoint2.distance(center, c0);
            if (db && T.update())
              T.msg("distance from current position=" + dist);
            if (dist < EPSILON2) {
              if (db && T.update())
                T.msg("too close to current position, ignoring"
                    + T.show(center));
              ignore = true;
              break;
            }
          }

          if (MyMath.sign(t - t0) != (actEdge.orientation == 0 ? 1 : -1)) {
            //            if (false && Math.abs(t - t0) < EPSILON2) {
            //              center = c0;
            //              loc.x = sweepLine.position();
            //              if (db && T.update())
            //                T.msg("almost zero, adjusted center "
            //                    + "to be equal to current position");
            //            } else {
            if (db && T.update())
              T.msg("sign is not correct; t=" + t + " t0=" + t0 + " orient="
                  + actEdge.orientation);
            ignore = true;
            break;
            //            }
          }
        }
        if (ignore)
          break;

        if (loc.x < sweepLine.position()) {
          if (db && T.update())
            T.msg("occurs before current sweep line, ignoring");
          break;
        } else {
          evt = VornEvent.vertexEvent(ae, e2, loc, center);
        }
      }
      if (evt != null)
        postEvent(evt);
      ae.pendingVertexEvent = evt;
    } while (false);
  }

  private IVornSite site(int i) {
    return (IVornSite) sites.get(i);
  }

  private int nSites() {
    return sites.size();
  }

  private void initFrontier() {
    frontier = new TreeSet(new ActiveEdgeComparator(sweepLine));
    firstSite = null;
  }

  private void initEventQueue() {
    eventQueue = new TreeSet(VornEvent.comparator);

    // add site events
    for (int i = 0; i < nSites(); i++) {
      IVornSite s = site(i);
      postEvent(VornEvent.siteEvent(s));
    }
  }
  private VornEvent popEvent() {
    final boolean db = verbose && false;
    VornEvent ev = (VornEvent) eventQueue.first();
    eventQueue.remove(ev);
    if (db && T.update())
      T.msg("popEvent: " + ev + T.show(ev, MyColor.cRED));
    return ev;
  }

  private void postEvent(VornEvent event) {
    final boolean db = verbose && false;
    if (db && T.update())
      T.msg("postEvent: " + event + T.show(event));
    eventQueue.add(event);
  }

  private static class VornEvent implements Renderable {
    public static final double SORT_MIN = 0, SORT_MAX = 1000;

    public static Comparator comparator = new Comparator() {

      public int compare(Object o1, Object o2) {
        final boolean db = false;
        VornEvent e1 = (VornEvent) o1, e2 = (VornEvent) o2;
        int ret = FPoint2.compareLex(e1.loc, e2.loc, false);
        if (db && T.update())
          T.msg("Event.compare " + T.show(o1, MyColor.cRED)
              + T.show(o2, MyColor.cPURPLE) + "returning " + ret);
        return ret;
      }
    };

    public static VornEvent siteEvent(IVornSite siteA) {
      VornEvent evt = new VornEvent();
      evt.type = EVT_SITE;
      evt.siteA = siteA;
      evt.loc = siteA.getSiteLocation();
      return evt;
    }
    public static VornEvent vertexEvent(ActiveEdge e1, ActiveEdge e2,
        FPoint2 loc, FPoint2 vertexPos) {
      VornEvent evt = new VornEvent();
      evt.type = EVT_VERTEX;
      evt.e1 = e1;
      evt.e2 = e2;
      evt.loc = loc;
      evt.loc2 = vertexPos;
      return evt;
    }
    private VornEvent() {
    }
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("<");
      switch (type) {
      case EVT_SITE:
        sb.append("SITE");
        //sb.append(siteA.getSiteName());
        break;
      case EVT_VERTEX:
        sb.append("VERT ");
        sb.append(e1.rightSite());
        sb.append("/");
        sb.append(e1.leftSite());
        sb.append("/");
        sb.append(e2.leftSite());
        break;
      }
      sb.append(">");
      return sb.toString();
    }
    public int type;
    public IVornSite siteA;
    public ActiveEdge e1, e2;
    public FPoint2 loc, loc2;
    public int flarg;

    public void render(Color c, int stroke, int markType) {

      if (type == EVT_VERTEX) {
        if (option(OPT_EVENTLINE)) {
          V.pushColor(c, MyColor.cDARKGRAY);
          V.pushStroke(stroke, STRK_THIN);
          V.drawLine(loc2, loc);
          V.mark(loc2, MARK_X);
          V.pop(2);
        }
      }

      V.pushScale(.7);
      if (type == EVT_VERTEX) {
        T.render(loc, MyColor.cRED, stroke, MARK_X);
      } else {
        T.render(loc, MyColor.cPURPLE, stroke, markType);
      }
      V.pop(1);

      if (false && Editor.withLabels(false)) {
        Editor.plotLabel(toString(), MyMath.ptOnCircle(loc, 3 * Math.PI / 2,
            2.5), true, c);
      }

    }
  }

  private static void join(ActiveEdge a1, ActiveEdge a2) {
    final boolean db = false;
    if (db && T.update())
      T.msg("join: " + a1 + " with " + a2);
    if (a1 != null)
      a1.nextEdge = a2;
    if (a2 != null)
      a2.prevEdge = a1;
  }

  private class ActiveEdge implements Renderable {

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(rightSite().getSiteName());
      sb.append('/');
      sb.append(leftSite().getSiteName());

      if (false)
        sb.append("(" + (int) sortValue() + ")");
      return sb.toString();
    }

    public void setSortValue(double d) {
      if (d < 0)
        T.err("sort value set to " + Tools.f(d) + ", was " + sortValue);
      this.sortValue = d;

    }

    public void setSortValue(ActiveEdge e1, ActiveEdge e2) {
      sortValue = (e1.sortValue() + e2.sortValue()) * .5;
    }
    public double sortValue() {
      return sortValue;
    }

    /**
     * Terminate underlying edge at vertex
     * @param vertex point to clip to
     */
    public void terminate(FPoint2 vertex) {
      final boolean db = false;

      if (db && T.update())
        T.msg("clipping edge " + this + T.show(vornEdge, MyColor.cRED)
            + " at vertex, " + T.show(vertex) + "orientation=" + orientation);

      if (orientation == 0)
        vornEdge.setFinish(vertex);
      else
        vornEdge.setStart(vertex);
      if (db && T.update())
        T.msg("after clipping" + T.show(vornEdge, MyColor.cRED));

      // if underlying edge is not active anymore, add it to the output list
      if (!vornEdge.adjustCounter(-1))
        vornEdges.add(vornEdge);
    }

    public ActiveEdge(VornEdge vEdge, int orientation) {
      this.vornEdge = vEdge;
      this.orientation = orientation;
      vornEdge.adjustCounter(1);
    }

    public void setAssociatedEdge(ActiveEdge ae) {
      final boolean db = false;

      if (other != ae)
        if (db && T.update())
          T.msg("setAssociatedEdge for " + this + " from " + other + " to "
              + ae);
      this.other = ae;
    }

    public void render(Color c, int stroke, int markType) {

      final boolean db = false;

      FPoint2 p1 = calcCurrentPoint0(sweepLine.position());
      if (p1 == INFINITY)
        return;
      if (db)
        Streams.out.println("plot: " + this + ", p1=" + p1);

      FPoint2 p2;
      if (other != null) {
        if (other.isActive())
          p2 = other.calcCurrentPoint0(sweepLine.position());
        else
          p2 = vornEdge.endpoint(orientation ^ 1);

        if (p2 == INFINITY) {
          Tools.unimp("");
          return;
        }

        if (db)
          Streams.out.println(" other, p2=" + p2);
      } else {
        p2 = vornEdge.endpoint(orientation);
      }
      V.pushColor(c, MyColor.cRED);
      V.pushStroke(stroke, STRK_THICK);

      p1 = new FPoint2(p1);
      p2 = new FPoint2(p2);
      if (MyMath.clipSegmentToRect(p1, p2, V.viewRect)) {
        V.drawLine(p1, p2);
        if (markType >= 0) {
          V.mark(p1, markType);
          V.mark(p2, markType);
        }
      }
      V.pop();
      if (option(OPT_SORTVALUES)) {
        V.pushScale(.7);
        V.pushColor(Color.DARK_GRAY);
        V.draw("" + (int) sortValue(), p1.x + 5, p1.y);
        V.pop(2);
      }
      V.pop();
    }

    private boolean isActive() {
      return sortValue >= 0;
    }

    private FPoint2 cachedPoint;
    private double cachedS;

    /**
     * Calculate how far along the edge we've advanced, with
     * respect to the sweep line
     * @param sweepLine
     * @return FPoint2
     */
    private FPoint2 calcCurrentPoint0(double s) {
      if (cachedPoint != null && s == cachedS)
        return cachedPoint;

      FPoint2 sR = rightSite().getSiteLocation();
      FPoint2 sL = leftSite().getSiteLocation();

      FPoint2 ret;

      ret = calcIntersect0(sR, sL, s);
      cachedS = s;
      cachedPoint = ret;
      return ret;
    }

    private IVornSite rightSite() {
      return vornEdge.site(1 ^ orientation);
    }
    private IVornSite leftSite() {
      return vornEdge.site(0 ^ orientation);
    }

    // the underlying edge for this ActiveEdge
    private VornEdge vornEdge;

    // each ActiveEdge is responsible for one endpoint of the
    // underlying edge; this tells us which of these endpoints
    // this ActiveEdge is to manipulate
    private int orientation;

    // the ActiveEdge sharing the same VornEdge (maybe for
    // display purposes only)
    private ActiveEdge other;

    private ActiveEdge prevEdge;
    private ActiveEdge nextEdge;
    private double sortValue;
    private VornEvent pendingVertexEvent;
  }

  private static class ActiveEdgeComparator implements Comparator {
    public ActiveEdgeComparator(SweepLine sweepLine) {
      this.sweepLine = sweepLine;
    }
    private SweepLine sweepLine;

    public int compare(Object o1, Object o2) {
      final boolean db = false;
      if (o1 == o2)
        return 0;

      if (db && T.update())
        T.msg("ActiveEdge.comparator: " + Tools.d(o1) + " with " + Tools.d(o2));

      int ret = 0;

      if (o1 instanceof IVornSite) {
        // we're comparing a site to an active edge, 
        // to see whether it contains it
        IVornSite vs = (IVornSite) o1;
        ActiveEdge ae = (ActiveEdge) o2;

        FPoint2 pt = ae.calcCurrentPoint0(sweepLine.position());
        if (pt == INFINITY) {
          if (db && T.update())
            T.msg("special case, point is at infinity");
          return 1;
        }

        if (db && T.update())
          T.msg("current point is " + pt.x + "," + pt.y + T.show(pt));
        ret = MyMath.sign(vs.getSiteLocation().y - pt.y);

        if (db && T.update()) {
          FPoint2 sr = ae.rightSite().getSiteLocation();
          FPoint2 sl = ae.leftSite().getSiteLocation();

          T.msg("calculated current point for\n sr=" + sr.x + " " + sr.y
              + "\n sl=" + sl.x + " " + sl.y + "\n pt=" + pt.x + " " + pt.y
              + "\n ret " + ret + T.show(vs) + T.show(pt) + "\n right="
              + ae.rightSite() + " left=" + ae.leftSite() + "\n new site="
              + vs.getSiteLocation().x + "," + vs.getSiteLocation().y);
        }

        //        if (db && T.update())
        //          T.msg(" site=" + vs + ":" + vs.getSiteLocation() + T.show(vs)
        //              + " current point=" + pt + T.show(pt) + " ret=" + ret);
      } else {
        ActiveEdge e1 = (ActiveEdge) o1;
        ActiveEdge e2 = (ActiveEdge) o2;
        ret = MyMath.sign(e1.sortValue() - e2.sortValue());
      }

      if (db && T.update())
        T.msg("ActiveEdge.compare " + T.show(o1, MyColor.cRED)
            + T.show(o2, MyColor.cPURPLE) + "returning " + ret);
      return ret;
    }
  }

  /**
   * Render the frontier, which includes the active edges, and
   * the parabolas
   */
  private void renderFrontier() {

    if (frontier != null) {
      // construct text representation of edges
      if (option(OPT_FRONTIERTEXT)) {
        final boolean COMPARE = false;
        {
//          int k = 0;
          StringBuilder sb = new StringBuilder();
          for (Iterator it = frontier.iterator(); it.hasNext(); ) {
            ActiveEdge ae = (ActiveEdge) it.next();
            sb.append(ae.rightSite());
            sb.append("\n");
            sb.append(ae.leftSite());
            sb.append("\n\n");
          }
          sb.reverse();
          V.pushScale(.7);
          V.draw(sb.toString(), 0, V.viewRect.height, TX_CLAMP | TX_BGND | 60);
          V.pop();
        }

        if (COMPARE) {
          ActiveEdge first = null;
          ActiveEdge last = null;
          String s1 = null, s2 = null, s3 = null;
          {
            StringBuilder sb = new StringBuilder();
            for (Iterator it = frontier.iterator(); it.hasNext();) {
              ActiveEdge ae = (ActiveEdge) it.next();
              if (first == null)
                first = ae;
              Tools.addSp(sb);
              sb.append(ae);
              last = ae;
            }
            s1 = sb.toString();
          }

          {
            StringBuilder sb = new StringBuilder();
            int cnt = 0;

            while (first != null) {
              if (cnt++ >= 100)
                break;
              Tools.addSp(sb);
              sb.append(first);
              first = first.nextEdge;
            }
            s2 = sb.toString();
          }
          {
            DArray stack = new DArray();
            int cnt = 0;

            while (last != null) {
              if (cnt++ >= 100)
                break;
              stack.push(last);
              last = last.prevEdge;
            }

            StringBuilder sb = new StringBuilder();

            while (!stack.isEmpty()) {
              Tools.addSp(sb);
              sb.append(stack.pop());
            }
            s3 = sb.toString();
          }
          String res = s1;
          if (!s1.equals(s2))
            res += "\n" + s2;
          if (!s1.equals(s3))
            res += "\n" + s3;
          V.draw(res, 0, V.viewRect.height, TX_CLAMP | TX_BGND | 60);
        }

      }

      T.renderAll(frontier, MyColor.cBLUE, STRK_NORMAL, -1);

      DArray siteList = new DArray();

      IVornSite lastSite = firstSite;
      for (Iterator it = frontier.iterator(); it.hasNext();) {
        ActiveEdge ae = (ActiveEdge) it.next();
        siteList.add(ae.rightSite());
        lastSite = ae.leftSite();
      }
      if (lastSite != null)
        siteList.add(lastSite);

      final boolean db = false;

      if (db)
        Streams.out.print("RenderFrontier:");

      for (int i = 0; i < siteList.size(); i++) {
        IVornSite curr = (IVornSite) siteList.get(i);
        if (db)
          Streams.out.print(" " + curr);
        IVornSite prev = null;
        IVornSite next = null;
        if (i > 0)
          prev = (IVornSite) siteList.get(i - 1);
        if (i + 1 < siteList.size())
          next = (IVornSite) siteList.get(i + 1);

        IVornSite site = curr;
        {
          double s = sweepLine.position();

          FPoint2 focus = site.getSiteLocation();

          // determine min/max parameter based on neighboring sites
          Parabola p = new Parabola(focus, s);

          if (db)
            Streams.out.println(" plotHyp, site=" + site.getSiteName());

          FPoint2 pt = null;
          if (prev != null) {
            if (prev == site) {
              Streams.out.println("*** prev=site! " + site);
              return;
            }
            pt = calcIntersect0(prev.getSiteLocation(), site.getSiteLocation(),
                s);
            if (db)
              Streams.out.println(" intersect=" + pt + " (inf="
                  + (pt == INFINITY) + ")");
            if (pt != INFINITY)
              p.setMin(pt.y);
          }

          if (pt != INFINITY && next != null) {
            if (next == site) {
              //   Tools.warn("next=site!" + site);
              return;
            }
            pt = calcIntersect0(site.getSiteLocation(), next.getSiteLocation(),
                s);
            if (pt != INFINITY)
              p.setMax(pt.y);
          }
          if (pt == INFINITY)
            p.setDegIntercept(new FPoint2(-100, focus.y));
          p.render(null, -1, -1);
        }

        // plotHyp(curr, prev, next);
      }
      if (db)
        Streams.out.println();
    }
  }
  /**
   * Plot Fortune algorithm structures
   */
  public void render(Color c, int stroke, int markType) {
    if (sites != null) {
      T.render(sweepLine);
      renderFrontier();
      T.renderAll(vornEdges, null);

      T.renderAll(eventQueue, null);
    }
  }

  /**
   * Calculate the intersection of two site's parabolas
   * @param sR right site
   * @param sL left site
   * @param s sweep line position
   * @return intersection point; will be INFINITY if point is at infinity
   *  (this only happens if both sites lie on the sweep line)
   */
  private static FPoint2 calcIntersect0(FPoint2 sR, FPoint2 sL, double s) {
    FPoint2 ret = null;
    double a = sR.x - s, b = sR.y, c = sL.x - s, d = sL.y;

    if (a == 0) {
      if (c != 0) {
        double x = (b * (b - 2 * d) + c * c + d * d) / (2 * c);
        ret = new FPoint2(x + s, b);
      } else
        ret = INFINITY;
    } else if (c == 0) {
      double x = (d * (d - 2 * b) + a * a + b * b) / (2 * a);
      ret = new FPoint2(x + s, d);
    } else if (c == a) {
      double y = (d * d - b * b) / (2 * (d - b));
      double x = s + (a * a + b * b + y * (y - 2 * b)) / (2 * a);
      ret = new FPoint2(x, y);
    } else {
      double A = (c - a);
      double B = 2 * (a * d - b * c);
      double C = a * a * c + b * b * c - a * c * c - a * d * d;

      double q = (B * B - 4 * A * C);
      if (Math.abs(q) < EPSILON2)
        q = 0;
      //      if (q < 0)
      //        throw new FPError("q < 0:" + q + "\n A=" + A + " B=" + B + " C=" + C);
      double qr = Math.sqrt(q);
      double y1 = (-B + qr) / (2 * A);
      double y2 = (-B - qr) / (2 * A);
      double x1 = (y1 * y1 - 2 * y1 * b + a * a + b * b) / (2 * a) + s;
      double x2 = (y2 * y2 - 2 * y2 * b + a * a + b * b) / (2 * a) + s;
      // if sR lies to the right of the line x1y1...x2y2, use x2y2;

      if (MyMath.sideOfLine(x1, y1, x2, y2, sR.x, sR.y) <= 0) {
        ret = new FPoint2(x2, y2);
      } else
        ret = new FPoint2(x1, y1);
    }
    return ret;
  }

  public VornFortune() {
  }

  private TreeSet frontier;
  //  private MyTree frontier;
  private TreeSet eventQueue;
  private SweepLine sweepLine;
  // the first site; if no other sites added yet, no edge can be
  // added to the frontier
  private IVornSite firstSite;
  //  private static final FRect viewRect = new FRect(0, 0, 100, 100);
  private double sweepSpeed;
  private DArray vornEdges;
  private DArray sites;
  //  private Map vertEventFlags = new HashMap();

  private static final FPoint2 INFINITY = new FPoint2();

}
