package dynvorn;

import java.awt.*;
import java.util.*;
import testbed.*;
import base.*;

public class VornGraph implements Renderable {

  public static final boolean CRIPPLED = false;

  private boolean db;

  private static final double GRIDSIZE = .05;

  /**
   * Get the number of sites
   * @return
   */
  public int nSites() {
    return sites.size();
  }

  /**
   * Get site 
   * @param index index of site, 0..nSites()-1
   * @return IVornSite
   */
  public IVornSite getSite(int index) {
    return (IVornSite) sites.get(index);
  }

  private IVornSite site(int si) {
    return (IVornSite) sites.get(si);
  }

  public boolean build(VornAlgorithm alg) {

    final int MAXPASSES = 1;

    DArray edges = alg.build(sites);

//    pass: 
      for (int pass = 0; pass < MAXPASSES; pass++) {
      if (db && pass != 0)
        Streams.out.println("<pass=" + pass + ">");

      {
        // initialize values for new pass
        vertMap = new HashMap();
        siteVertices = new HashMap();
        nVertices = 0;
        infVertNum = 0;
        shift += GRIDSIZE * (.2 / MAXPASSES);
        graph = new Graph(VERT_ID_BASE);
        topology = null;
      }

      for (int n = 0; n < edges.size(); n++) {
        VornEdge d = (VornEdge) edges.get(n);
        if (db && T.update())
          T.msg(" edge #" + n + " = " + d);
        if (d != null && d.nonEmpty()) {
          //          FPoint2[] endPts = d.calcEndpoints();
          addEdges(d.endpoint(0), /*endPts[0],*/d.infinite(0), d.endpoint(1), /*endPts[1],*/
          d.infinite(1), d.site(0), d.site(1));
        }
      }

      if (db && T.update())
        T.msg("built:\n" + this);

      sortEdges();
      removeDegeneracies();

      if (!CRIPPLED) {
        addInfiniteFace();

        // verify that each vertex has degree 3.
        for (int vi = startVert(); vi < endVert(); vi++) {
          if (graph.nCount(vi) != 3) {
           // if (MAXPASSES == 1)
              T.err("vertex " + vi + " has " + graph.nCount(vi)
                  + " neighbors:\n" + this);
//            continue pass;
          }
        }
        findFaceVertices();
      }

      proper = true;
      break;
    }
    return proper;
  }

  public boolean isProper() {
    return proper;
  }

  /**
   * Find lowest vertex adjacent to each face
   */
  private void findFaceVertices() {

    for (int vi = startVert(); vi < endVert(); vi++) {
      VornVertex v = getVertex(vi);
      for (int j = 0; j < 3; j++) {
        IVornSite site = left(vi, j);
        VornVertex rep = (VornVertex) siteVertices.get(site.getSiteName());
        if (rep == null
            || FPoint2.compareLex(rep.getLocation(), v.getLocation()) < 0) {
          siteVertices.put(site.getSiteName(), v);
        }
      }
    }

    //
    //          if (cellV == null
    //              || FPoint2.compareLex(cellV.getLocation(), v.getLocation()) < 0) {
    //            cellV = v;
    //            siteVertices.put(si.getSiteName(), v);
    //          }

  }

  /**
   * Create new nodes, if necessary, so that no node has degree > 3
   */
  private void removeDegeneracies() {
    final boolean db = false;

    //    Streams.out.println("before remove degen:\n" + this);
    if (db && T.update())
      T.msg("removing degeneracies\n" + this);
    DArray nl = graph.getNodeList();
    for (int i = 0; i < nl.size(); i++) {
      int id = nl.getInt(i);
      VornVertex v = getVertex(id);

      if (v.isAtInfinity())
        continue;
      splitVertex(id);
    }
    //    Streams.out.println("after remove degen:\n" + this);
  }

  private void splitVertex(int id) {

    final boolean db = false;

    do {
      int nEdges = graph.nCount(id);
      if (db && T.update())
        T.msg("splitVertex id=" + id + ", nEdges=" + nEdges);
      if (nEdges <= 3)
        break;

      // create a new vertex
      VornVertex vSrc = getVertex(id);

      FPoint2 loc = vSrc.getLocation();
      VornVertex vNew = new VornVertex(loc, false);
      vNew.setNodeNumber(newNode());
      graph.setNodeData(vNew.getNodeNumber(), vNew);

      int mStart = nEdges / 2;
      int mTotal = nEdges - mStart;

      // save destination vertex and edge contents of the edges
      // we're about to move.
      int[] destVertOfMoved = new int[mTotal];
      IVornSite[] sitesOfMoved = new IVornSite[mTotal * 2];

      for (int j = 0; j < mTotal; j++) {
        int indexOfMovingNeighbor = j + mStart;
        destVertOfMoved[j] = graph.neighbor(id, indexOfMovingNeighbor);
        sitesOfMoved[j * 2 + 0] = getSite(id, indexOfMovingNeighbor, false);
        sitesOfMoved[j * 2 + 1] = getSite(id, indexOfMovingNeighbor, true);

        if (db && T.update())
          T.msg(" about to move edge to " + destVertOfMoved[j] + " (nodes="
              + sitesOfMoved[j * 2 + 0].getSiteName() + ","
              + sitesOfMoved[j * 2 + 1].getSiteName() + ")");
      }

      for (int j = mTotal - 1; j >= 0; j--) {
        int id2 = destVertOfMoved[j];
        VornVertex vDest = getVertex(id2);
        if (db && T.update())
          T.msg("id2=" + id2 + ", before remove #1:\n" + this);
        int count = graph.findAndRemoveEdge(id, id2, false);
        if (db && T.update())
          T.msg("remove count=" + count + ", after remove #1:\n" + this);
        count += graph.findAndRemoveEdge(id2, id, false);
        if (db && T.update())
          T.msg("after 2, count=" + count + "\n" + this);
        addEdges(vNew, vDest, sitesOfMoved[j * 2 + 0], sitesOfMoved[j * 2 + 1]);
        if (db && T.update())
          T.msg("after addEdges:\n" + this);
      }

      // add edges from old to new
      addEdges(vSrc, vNew, //
          sitesOfMoved[(mTotal - 1) * 2], sitesOfMoved[1]);
      //      sitesOfMoved[(mTotal - 1) * 2 + 1], //
      //          sitesOfMoved[1]);

      if (db && T.update())
        T.msg("after adding edges from old to new:\n" + this);

      // resort edges at both nodes
      sortEdge(id);
      sortEdge(vNew.getNodeNumber());

      // split nodes recursively
      splitVertex(id);
      splitVertex(vNew.getNodeNumber());

    } while (false);

  }

  private void sortEdge(int id) {
    graph.sortEdges(id, new Comparator() {

      public int compare(Object a1, Object a2) {
        final boolean db = false;

        Object[] o1 = (Object[]) a1;
        Object[] o2 = (Object[]) a2;

        Graph g = (Graph) o1[0];
        int nodeId = ((Integer) o1[1]).intValue();
        int nbrInd1 = ((Integer) o1[2]).intValue();
        int nbrInd2 = ((Integer) o2[2]).intValue();

        VornVertex vSrc = (VornVertex) (g.nodeData(nodeId));
        if (vSrc.isAtInfinity())
          throw new IllegalStateException();

        // calculate polar angle as rotated bisector of sites, since
        // vertices may be very close or identical

        IVornSite s1L = getSite(nodeId, nbrInd1, false);
        IVornSite s1R = getSite(nodeId, nbrInd1, true);
        IVornSite s2L = getSite(nodeId, nbrInd2, false);
        IVornSite s2R = getSite(nodeId, nbrInd2, true);

        double p1 = MyMath.normalizeAnglePositive(Math.PI / 2
            + MyMath.polarAngle(s1L.getSiteLocation(), s1R.getSiteLocation()));
        double p2 = MyMath.normalizeAnglePositive(Math.PI / 2
            + MyMath.polarAngle(s2L.getSiteLocation(), s2R.getSiteLocation()));

        int ret = MyMath.sign(p1 - p2);
        if (db && T.update())
          T.msg("compare edge " + nodeId + ".." + nbrInd1 + " with " + nodeId
              + ".." + nbrInd2 + " ret=" + ret);
        return ret;
      }
    });

  }

  /**
   * Sort Voronoi edges around source vertices by polar angle
   */
  private void sortEdges() {
    DArray nl = graph.getNodeList();
    for (int i = 0; i < nl.size(); i++) {
      int id = nl.getInt(i);
      VornVertex v = getVertex(id);

      if (v.isAtInfinity())
        continue;

      sortEdge(id);
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("VornGraph");
    sb.append("\n");
    //    sb.append("time=" + Tools.f(time) + "\n");
    sb.append("nodes:\n");
    {
      DArray nl = graph.getNodeList();
      for (int i = 0; i < nl.size(); i++) {
        int vId = nl.getInt(i);
        sb.append(" " + Tools.f(vId) + ":");
        VornVertex v = getVertex(vId);
        String lbl = " ***";
        if (!v.isAtInfinity()) {
          FPoint2 loc = v.getLocation();
          lbl = "(" + (int) loc.x + "," + (int) loc.y + ")";
        }
        sb.append(Tools.f(lbl, 10));

        for (int j = 0; j < graph.nCount(vId); j++) {
          int nId = graph.neighbor(vId, j);
          sb.append(" " + nId);
          sb.append("<" + debSiteName(getSite(vId, j, false)) + "|");
          sb.append(debSiteName(getSite(vId, j, true)) + ">");

        }
        sb.append("\n");
      }
    }
    //    sb.append(super.toString());
    return sb.toString();
  }

  private static String debSiteName(IVornSite s) {
    String r = "null";
    if (s != null)
      r = s.getSiteName();
    return r;
  }

  private static FPoint2 mid = new FPoint2(50, 50);
  private static double extrapAngle(FPoint2 loc) {
    double rad = loc.distance(mid);
    if (rad == 0)
      throw new FPError("extrapolation problem: loc = midpoint");
    double scale = 2e5 / rad;
    FPoint2 loc2 = FPoint2.interpolate(mid, loc, scale);
    return MyMath.normalizeAnglePositive(MyMath.polarAngle(mid, loc2));
  }

  private DArray sortInfiniteVertices() {
    DArray infv = new DArray();
    //    VornVertex cellV = null;
    DArray nl = graph.getNodeList();
    for (int i = 0; i < nl.size(); i++) {
      int id = nl.getInt(i);
      VornVertex v = getVertex(id);
      if (!v.isAtInfinity())
        continue;
      infv.add(v);
      //
      //      if (cellV == null
      //          || FPoint2.compareLex(cellV.getLocation(), v.getLocation()) < 0) {
      //        cellV = v;
      //        siteVertices.put(infSite.getSiteName(), v);
      //      }
    }

    if (infv.size() == 1)
      T.err("only one vertex at infinity:\n" + this);

    infv.sort(new Comparator() {
      public int compare(Object arg0, Object arg1) {
        VornVertex v0 = (VornVertex) arg0;
        VornVertex v1 = (VornVertex) arg1;

        if (true) {
          double a0 = extrapAngle(v0.getLocation());
          double a1 = extrapAngle(v1.getLocation());
          return MyMath.sign(a0 - a1);
        } else {

          double a0 = calcAngleInfEdge(v0);
          double a1 = calcAngleInfEdge(v1);

          int diff = MyMath.sign(a0 - a1);
          if (db && T.update())
            T.msg("compare inf vert " + v0 + " with " + v1 + "\n a0=" + a0
                + "\n a1=" + a1 + "\n diff=" + diff);
          if (diff == 0) {
            diff = MyMath.sign(MyMath.sideOfLine(new FPoint2(50, 50), v1
                .getLocation(), v0.getLocation()));

          }
          return diff;
        }
      }
    });
    if (db && T.update())
      T.msg("inf vert, sorted by polar angle=\n" + infv.toString(true));

    // order may be incorrect since intersections far off screen may
    // never have been detected.

    return infv;
  }

  /**
   * Connect vertices at infinity
   */
  private void addInfiniteFace() {

    //    final boolean db = true;

    if (db && T.update())
      T.msg("addInfiniteFace, currently\n" + this);

    DArray infv = sortInfiniteVertices();
    for (int i = 0; i < infv.size(); i++) {
      VornVertex v0 = (VornVertex) infv.get(i);
      VornVertex v1 = (VornVertex) infv.getMod(i + 1);

      IVornSite v0Site = getSite(v0.getNodeNumber(), 0, true);
      IVornSite v1Site = getSite(v1.getNodeNumber(), 0, false);
      if (v0Site != v1Site) {
        String msg = "Infinite vertex " + v0.getNodeNumber() + " rightSite "
            + v0Site + "\n != vert " + v1.getNodeNumber() + " left site "
            + v1Site + "\n" + this;
        if (false) {
          Tools.warn(msg);
          continue;
        } else
          T.err(msg);
        //IllegalStateException(msg);
      }
      if (db && T.update())
        T.msg("adding edges between " + v0.getNodeNumber() + ".."
            + v1.getNodeNumber() + " v0Site=" + v0Site.getSiteName());
      addEdges(v0, v1, v0Site, infSite);
    }
  }

  private double calcAngleInfEdge(VornVertex v) {
    int infId = v.getNodeNumber();

    //    if (true) {
    //      FPoint2 vloc = v.getLocation();
    //      FPoint2 aloc = adjustedInfVertex(vloc);
    //      return MyMath.normalizeAnglePositive(MyMath.polarAngle(center, aloc));
    //    } else {

    if (graph.nCount(infId) != 1)
      T.err("infinite node has != 1 edges:" + infId + "\n" + this);

    IVornSite vL = getSite(infId, 0, true);
    IVornSite vR = getSite(infId, 0, false);

    // construct bisector for 

    double ang = MyMath.polarAngle(vL.getSiteLocation(), vR.getSiteLocation());
    return MyMath.normalizeAnglePositive(ang);
    //    }
  }
  //  private FPoint2 adjustedInfVertex(FPoint2 loc) {
  //    //    FPoint2 mid = V.viewRect.midPoint();
  //    double rad = loc.distance(center);
  //    final double MAXRAD = 4000;
  //    double scale = 1.0;
  //    if (rad > MAXRAD) {
  //      scale = MAXRAD / rad;
  //    }
  //    FPoint2 loc2 = FPoint2.interpolate(center, loc, scale);
  //    if (db && T.update())
  //      T.msg("adjusted inf vertex loc from " + loc + " to " + loc2);
  //    return loc2;
  //  }

  //  private Double boundRadius;
  //  public double boundingRadius() {
  //    if (boundRadius == null) {
  //      FRect r = new FRect(0, 0, 100, 100);
  //      DArray nl = graph.getNodeList();
  //      for (int i = 0; i < nl.size(); i++) {
  //        int id = nl.getInt(i);
  //        VornVertex v = getVertex(id);
  //        if (v.isAtInfinity())
  //          continue;
  //        r.add(v.getLocation());
  //      }
  //      double rad = FPoint2.distance(r.midPoint(), r.topLeft()) + 5;
  //      boundRadius = new Double(rad);
  //    }
  //    return boundRadius.doubleValue();
  //    //return rad;
  //  }

  //  private FPoint2 center;

  public VornGraph(DArray arrayIVornSite, boolean tracing) {
    this.db = tracing;

    sites = new DArray();
    // construct cell for each site
    for (int siteI = 0; siteI < arrayIVornSite.size(); siteI++) {
      IVornSite si = (IVornSite) arrayIVornSite.get(siteI);
      if (si.getSiteName().equals(INFSITENAME))
        throw new IllegalArgumentException("infinite name reserved");
      sites.add(si);
    }
  }

  public String dumpSites() {
    StringBuilder sb = new StringBuilder();
    sb.append("# Dump of sites\n");
    for (int i = 0; i < sites.size(); i++) {
      IVornSite s = site(i);
      //      EdMovingPt pt = new EdMovingPt(s.getSiteLocation(0), 0);
      //      movingpt 0     -2.524     84.709    56.472
      sb.append(EdMovingPt.FACTORY.getTag() + " 0 0 " + s.getSiteLocation());
      sb.append('\n');
    }
    return sb.toString();
  }

  /**
   * Get number of neighbor vertex
   * @param vertexNumber current vertex
   * @param nbrIndex index, 0..2
   * @return number of neighbor vertex
   */
  public int neighbor(int vertexNumber, int nbrIndex) {
    return graph.neighbor(vertexNumber, nbrIndex);
  }

  /**
   * Construct a polygon representing a cell 
   * @param site site to construct cell for
   * @return EdPolygon 
   * @deprecated let's not try to support this
   */
  public EdPolygon getCell(IVornSite site) {
    final boolean db = false;

    //    final boolean db = true;
    if (db)
      Streams.out.println("getCell, site=" + site.getSiteName());

    //    Inf inf = new Inf("poly", 100);
    EdPolygon p = null;
    int v = getVertexBounding(site);
    if (db)
      Streams.out.println(" initial vertex=" + v);

    if (v >= 0) {
      int v0 = v;
      p = new EdPolygon();
      while (true) {
        if (db)
          Streams.out.println("vertex=" + getVertex(v));

        //        Inf.update(inf);
        p.addPoint(getVertex(v).getLocation());
        int v2 = next(v, site);
        if (db)
          Streams.out.println(" fwdAround=" + v2);
        if (v2 < 0) {
          if (true) {
            Tools.warn("prob building");
            break;
          }

          T.err("problem building cell, vert #" + v + " site="
              + site.getSiteName() + " v2=" + v2 + "\n" + this);
        }
        if (v2 == v0)
          break;
        v = v2;
      }
    }

    return p;
  }

  /**
   * Get previous vertex around cell boundary
   * @param vertex current vertex
   * @param site site associated with cell
   * @return prev (ccw) vertex on cell boundary, or -1 if vertex
   *  is not adjacent to site
   */
  public int prev(int vertex, IVornSite site) {
    int ret = -1;
    do {
      int en = findEdgeBoundingCell(vertex, site, true);
      if (en < 0)
        break;
      ret = graph.neighbor(vertex, en);
    } while (false);
    return ret;
  }

  /**
   * Get next vertex ccw around cell boundary
   * @param vertex current vertex
   * @param site site associated with cell
   * @return next (ccw) vertex on cell boundary, or -1 if vertex
   *  is not adjacent to site
   *  @deprecated
   */
  public int next(int vertex, IVornSite site) {
    int ret = -1;
    do {
      int en = findEdgeBoundingCell(vertex, site, false);
      if (en < 0)
        break;
      ret = graph.neighbor(vertex, en);
    } while (false);
    return ret;
  }

  private int findEdgeBoundingCell(int vert, IVornSite site, boolean toRight) {
    int ret = -1;
    for (int i = 0; i < graph.nCount(vert); i++) {
      IVornSite s2 = getSite(vert, i, toRight);
      if (s2.getSiteName().equals(site.getSiteName())) {
        ret = i;
        break;
      }
    }
    return ret;
  }

  /**
   * Get an arbitrary vertex adjacent to a site
   * @param site 
   * @return id of vertex adjacent to site 
   */
  private int getVertexBounding(IVornSite site) {

    final boolean db = false;

    int ret = -1;
    VornVertex v = (VornVertex) siteVertices.get(site.getSiteName());
    if (v != null)
      ret = v.getNodeNumber();
    if (db)
      Streams.out.println("getVertexBoundingSite " + site.getSiteName() + " = "
          + ret);

    return ret;
  }

  /**
   * Get id of first vertex
   * @return id
   */
  public int startVert() {
    return VERT_ID_BASE;
  }
  /**
   * Get 1 + id of last vertex
   * @return 1 + id of last vertex
   */
  public int endVert() {
    return VERT_ID_BASE + nVertices;
  }

  private static final int VERT_ID_BASE = 20;

  /**
   * Get VornVertex from graph
   * @param vertId id of vertex
   * @return VornVertex
   */
  public VornVertex getVertex(int vertId) {
    return (VornVertex) graph.nodeData(vertId);
  }

  //  public int nVert() {
  //    return graph.nNodes();
  //  }

  public void render(Color c, int stroke, int markType) {
    if (graph != null) {

      FRect r = new FRect(V.viewRect);
      r.inset(1.5);
      V.pushColor(c, MyColor.cDARKGREEN);
      V.pushStroke(stroke);
      for (Iterator it = graph.getNodeList().iterator(); it.hasNext();) {
        int id = ((Integer) it.next()).intValue();

        VornVertex currVert = (VornVertex) graph.nodeData(id);

        boolean labelled = false;

        for (int n = 0; n < graph.nCount(id); n++) {
          int id2 = graph.neighbor(id, n);
          VornVertex nVert = (VornVertex) graph.nodeData(id2);
          FPoint2 p0 = new FPoint2(currVert.getLocation());
          FPoint2 p1 = new FPoint2(nVert.getLocation());

          if (!MyMath.clipSegmentToRect(p0, p1, r)) {
            continue;
          }

          if (Editor.withLabels(false)) {
            if (!labelled) {
              Editor.plotLabel(Integer.toString(currVert.getNodeNumber()),
                  MyMath.ptOnCircle(p0, MyMath.radians(id * 29), 3.4), true,
                  null);
              labelled = true;
            }
          }
          if (id2 < id)
            continue;

          if (currVert.isAtInfinity() && nVert.isAtInfinity()) {
            if (false) {
              V.pushStroke(Globals.STRK_RUBBERBAND);
              V.drawLine(p0, p1);
              V.popStroke();
            }
            continue;
          }

          V.drawLine(p0, p1);
          double theta = MyMath.polarAngle(p0, p1);

          if (currVert.isAtInfinity())
            EdSegment.plotArrowHead(p0, theta + Math.PI);
          if (nVert.isAtInfinity())
            EdSegment.plotArrowHead(p1, theta);

        }
      }
      V.pop(2);
    }
  }

  private static final double EPS = GRIDSIZE;

  private FPoint2 snapToGrid(double x, double y) {
    // snap resolution must not be greater than print resolution, for rounding!
    double gs = EPS;

    x += shift;
    y += shift;
    if (true) {
      double max = Math.max(Math.abs(x), Math.abs(y));
      //      gs = 1e-5;
      if (max > 1000) {
        gs = 1;
        if (max > 5000)
          gs = 4;
      }
    }
    FPoint2 sl = MyMath.snapToGrid(x, y, gs);
    //    Streams.out.println("snapped "+loc.toString(true)+": became "+sl.toString(true));
    return sl;
  }
  //  private static Object getMapKey(FPoint2 pt) {
  //    return pt.toString();
  //  }

  private int newNode() {
    int ret = graph.newNode();
    nVertices++;
    return ret;
  }

  private VornVertex addVertex(FPoint2 loc, boolean atInfinity,
      IVornSite sLeft, IVornSite sRight) {
    final boolean db2 = false;

    final boolean db = false; //VornGraph.db && false;

    if (db2)
      Streams.out.println("addVertex loc=" + loc.x + " " + loc.y + " atInf="
          + atInfinity);

    if (db && T.update())
      T.msg("addVertex " + T.show(loc) + loc.x + " " + loc.y + "inf="
          + atInfinity);
    VornVertex v;
    if (atInfinity) {
      Object key = null;
      FPoint2 snappedLoc = snapToGrid(loc.x, loc.y);
      if (atInfinity) {
        key = "*" + sLeft.getSiteName() + ":" + sRight.getSiteName();
        if (false) {
          Tools.warn("using diff key");
          key = "*" + infVertNum++;
        }
      }
      if (db2)
        Streams.out.println(" key=" + key);
      v = (VornVertex) vertMap.get(key);
      if (db && T.update())
        T.msg("key=" + key + " existing=" + v + "\n"
            + DArray.toString(vertMap.keySet(), true));

      if (v == null) {
        v = new VornVertex(snappedLoc, true);
        vertMap.put(key, v);
        v.setNodeNumber(newNode());
        graph.setNodeData(v.getNodeNumber(), v);
      }
    } else {
      Object key = null;
      //      final double PAD = GRIDSIZE * .1;

      FPoint2 snappedLoc = snapToGrid(loc.x, loc.y); //MyMath.snapToGrid(loc.x, loc.y, GRIDSIZE);
      if (db && T.update())
        T.msg("snapped " + loc.x + ", " + loc.y + " to grid=" + snappedLoc);
      //      )snapToGrid(loc.x, loc.y);

      key = snappedLoc.toString();

      //      outer: for (int j = -1; j <= 1; j++) {
      //        if (j == -1 &&  loc.y - PAD >= snappedLoc.y)
      //          continue;
      //        if (j == 1 && loc.y + PAD -GRIDSIZE<= snappedLoc.y)
      //          continue;
      //        for (int i = -1; i <= 1; i++) {
      //          if (i == -1 && loc.x - PAD >= snappedLoc.x)
      //            continue;
      //          if (i == 1 && loc.x + PAD - GRIDSIZE <= snappedLoc.x)
      //            continue;
      //          key = new FPoint2(snappedLoc.x + GRIDSIZE * i, snappedLoc.y
      //              + GRIDSIZE * j).toString();
      //          if (db && T.update())
      //            T.msg("looking for key: " + key + " in\n"
      //                + DArray.toString(vertMap.keySet(), true));
      //          if (vertMap.containsKey(key))
      //            break outer;
      //        }
      //      }

      //      int signX = 0;
      //      if (snappedLoc.x - loc.x > GRIDSIZE * .4)
      //        signX = 1;
      //      else if (loc.x - snappedLoc.x > GRIDSIZE * .4)
      //        signX = -1;
      //      int signY = 0;
      //      if (snappedLoc.y - loc.y > GRIDSIZE * .4)
      //        signY = 1;
      //      else if (loc.y - snappedLoc.y > GRIDSIZE * .4)
      //        signY = -1;
      //
      //      final double EPS2 = 1e-3;
      //      do {
      //        key = getMapKey(snappedLoc);
      //        if (vertMap.containsKey(key))
      //          break;
      //        //      v = (VornVertex) vertMap.get(key);
      //
      //        //      if (v == null) {
      //        snappedLoc = snapToGrid(loc.x + EPS2, loc.y + EPS);
      //        key = getMapKey(snappedLoc);
      //        if (vertMap.containsKey(key))
      //          break;
      //        //      }
      //        snappedLoc = snapToGrid(loc.x - EPS2, loc.y + EPS2);
      //        key = getMapKey(snappedLoc);
      //        if (vertMap.containsKey(key))
      //          break;
      //        //      }
      //        snappedLoc = snapToGrid(loc.x + EPS2, loc.y - EPS2);
      //        key = getMapKey(snappedLoc);
      //        if (vertMap.containsKey(key))
      //          break;
      //        snappedLoc = snapToGrid(loc.x - EPS2, loc.y - EPS2);
      //        key = getMapKey(snappedLoc);
      //        if (vertMap.containsKey(key))
      //          break;
      //      } while (false);
      v = (VornVertex) vertMap.get(key);
      if (db && T.update())
        T.msg("key=" + key + " existing=" + v + "\n"
            + DArray.toString(vertMap.keySet(), true));

      if (v == null) {
        v = new VornVertex(snappedLoc, false);
        vertMap.put(key, v);
        v.setNodeNumber(newNode());
        graph.setNodeData(v.getNodeNumber(), v);
      }
    }

    //    Object key = null;
    //    FPoint2 snappedLoc = snapToGrid(loc);
    //    if (atInfinity) {
    //      key = "*" + sLeft.getSiteName() + ":" + sRight.getSiteName();
    //      if (false) {
    //        Tools.warn("using diff key");
    //        key = "*" + infVertNum++;
    //      }
    //    } else {
    //      key = getMapKey(snappedLoc);
    //    }
    //    if (db2)
    //      Streams.out.println(" key=" + key);
    //    VornVertex v = (VornVertex) vertMap.get(key);
    //    if (db && T.update())
    //      T.msg("key=" + key + " existing=" + v + " atInf=" + atInfinity + "\n"
    //          + DArray.toString(vertMap.keySet(), true));
    //
    //    if (atInfinity && v != null) {
    //      double dist = snappedLoc.distance(v.getLocation());
    //      if (dist > 1e-2) {
    //        T.err("vertex at infinity, key already exists:\n" + key
    //            + "\n with location " + v.getLocation() + "\n new snapped loc="
    //            + snappedLoc);
    //      }
    //    }
    //
    //    if (v == null) {
    //      v = new VornVertex(snappedLoc, atInfinity);
    //      vertMap.put(key, v);
    //      v.setNodeNumber(newNode());
    //      graph.setNodeData(v.getNodeNumber(), v);
    //    }
    if (db && T.update())
      T.msg(" returning " + v);

    return v;
  }
  /**
   * Get site to right of edge
   * @param vertexId current vertex
   * @param site site to left of current edge
   * @return next (ccw) vertex on cell boundary, or -1 if vertex
   *  is not adjacent to site
   */
  public IVornSite right(int vertexId, IVornSite site) {
    IVornSite ret = null;
    int ei = findEdgeBoundingCell(vertexId, site, false);
    if (ei >= 0) {
      ret = right(vertexId, ei);
      //      IVornSite[] ed = (IVornSite[]) graph.edgeData(vertexId, ei);
      //      ret = ed[0];
    }
    return ret;
  }

  /**
   * Get the site to the left of an edge
   * @param vertexId current vertex
   * @param edgeNumber edge number, 0..2
   * @return site to left of this edge
   */
  public IVornSite left(int vertexId, int edgeNumber) {
    return getSite(vertexId, edgeNumber, false);
  }

  /**
   * Get the site to the right of an edge
   * @param vertexId current vertex
   * @param edgeNumber edge number, 0..2
   * @return site to right of this edge
   */
  public IVornSite right(int vertexId, int edgeNumber) {
    return getSite(vertexId, edgeNumber, true);
  }

  private IVornSite getSite(int vertexId, int edgeNumber, boolean toRight) {
    if (graph.nCount(vertexId) <= edgeNumber)
      T.err("vertex " + vertexId + " has only " + graph.nCount(vertexId)
          + " neighbors:\n" + this);
    IVornSite[] ed = (IVornSite[]) graph.edgeData(vertexId, edgeNumber);
    return ed[toRight ? 0 : 1];
  }

  private VornVertex addEdges(FPoint2 pt1, boolean inf1, FPoint2 pt2,
      boolean inf2, IVornSite siteLeft, IVornSite siteRight) {

    final boolean db = false;

    VornVertex v1 = addVertex(pt1, inf1, siteLeft, siteRight);
    VornVertex v2 = addVertex(pt2, inf2, siteRight, siteLeft);
    if (db) {
      Streams.out.println("addEdges, siteL=" + siteLeft + " R=" + siteRight
          + " inf1=" + inf1 + " inf2=" + inf2 + " n1=" + v1.getNodeNumber()
          + " n2=" + v2.getNodeNumber());
      if (v1.getNodeNumber() == 27 || v2.getNodeNumber() == 27)
        Streams.out.println(Tools.st());
    }
    do {
      if (v1.getNodeNumber() == v2.getNodeNumber())
        break;
      // if there are already edges between these sites, ignore
      if (graph.hasNeighbor(v1.getNodeNumber(), v2.getNodeNumber()) >= 0)
        break;
      addEdges(v1, v2, siteLeft, siteRight);
    } while (false);
    return v1;
  }

  private void addEdges(VornVertex v1, VornVertex v2, IVornSite siteLeft,
      IVornSite siteRight) {

    final boolean db2 = false && (v1.getNodeNumber() == 27 || v2
        .getNodeNumber() == 27);

    if (db2)
      Streams.out.println("addEdges\n v1=" + v1 + "\n v2=" + v2 + "\n L:"
          + siteLeft + "\n R:" + siteRight + "\n" + Tools.st());

    if (db && T.update())
      T.msg("adding edges between " + v1.getNodeNumber() + " and "
          + v2.getNodeNumber() + T.show(v1.getLocation())
          + T.show(v2.getLocation()));
    IVornSite[] ed1 = new IVornSite[2];
    ed1[1] = siteLeft;
    ed1[0] = siteRight;
    IVornSite[] ed2 = new IVornSite[2];
    ed2[0] = siteLeft;
    ed2[1] = siteRight;

    if (v1.getNodeNumber() == v2.getNodeNumber())
      throw new IllegalStateException("attempt to add edge to self");
    graph.addEdgesBetween(v1.getNodeNumber(), v2.getNodeNumber(), ed1, ed2);
  }

  //  private static FRect clipRect;

  //  static {
  //    clipRect = new FRect(0, 0, 100, 100);
  //    clipRect.inset(INS);
  //    if (false) {
  //      Tools.warn("insetting more");
  //      clipRect.inset(10);
  //    }
  //  }

  private static final IVornSite infSite;
  public static final String INFSITENAME = "*";
  static {
    EdMovingPt p = new EdMovingPt(new FPoint2(), 0);
    infSite = p;
    p.setLabel(INFSITENAME);
    //    )new IVornSite() {
    //    public FPoint2 getSiteLocation() {
    //      throw new UnsupportedOperationException();
    //    }
    //    public FPoint2 getSiteLocation(double time) {
    //      throw new UnsupportedOperationException();
    //    }
    //    public String getSiteName() {
    //      return "*";
    //    }
    //
    //    public FPoint2 getVelocity() {
    //      throw new UnsupportedOperationException();
    //    }
  }

  /**
   * Determine if site corresponds to the infinite face.
   * The vertices at infinity are connected in a polygon.  The
   * site to the right of each of its edges are assigned the 
   * infinite face
   * @param siteR
   * @return true if this site is the unbounded face
   */
  public static boolean isInfiniteFace(IVornSite siteR) {
    return siteR == infSite;
  }

  //  private static class TopEvent {
  //    public FPoint2 location;
  //    public String label;
  //    public TopEvent(FPoint2 loc, String lbl) {
  //      this.location = loc;
  //      this.label = lbl;
  //    }
  //    public String toString() {
  //      return label;
  //    }
  //  }

  private static final Comparator CASE_SENSITIVE_ORDER = new Comparator() {

    public int compare(Object arg0, Object arg1) {
      String s1 = (String) arg0;
      String s2 = (String) arg1;
      return s1.compareTo(s2);
    }
  };

  /**
   * Get a string representing the diagram's topology
   * @return a string representation of the topology
   */
  public String getTopology() {
    //    Tools.unimp("support degree != 3 vertices");
    if (topology == null) {
      StringBuilder sb = new StringBuilder();
      StringBuilder sb2 = new StringBuilder();

      triples = new DArray();
      for (int i = startVert(); i < endVert(); i++) {

        sb2.setLength(0);

        String l0 = left(i, 0).getSiteName();
        String l1 = left(i, 1).getSiteName();
        String l2 = left(i, 2).getSiteName();
        if (true) {
          String tmp;
          if (l0.compareTo(l1) > 0) {
            tmp = l0;
            l0 = l1;
            l1 = tmp;
          }
          if (l0.compareTo(l2) > 0) {
            tmp = l0;
            l0 = l2;
            l2 = tmp;
          }
          if (l1.compareTo(l2) > 0) {
            tmp = l1;
            l1 = l2;
            l2 = tmp;
          }
        } else {

          int c01 = l0.compareTo(l1);
          int c02 = l0.compareTo(l2);
          int c12 = l1.compareTo(l2);

          int shift = 0;
          if (c01 < 0 && c02 < 0) {
          } else if (c01 > 0 && c12 < 0) {
            shift = 1;
          } else {
            if (c12 < 0 || c02 < 0)
              T.err("sort prob");
            shift = 2;
          }
          while (shift-- != 0) {
            String tmp = l0;
            l0 = l1;
            l1 = l2;
            l2 = tmp;
          }
        }
        sb2.append(l0);
        sb2.append(l1);
        sb2.append(l2);

        String top = sb2.toString();

        if (false) {
          Tools.warn("testing label sorting");
          DArray s = new DArray();
          String tl0 = left(i, 0).getSiteName();
          String tl1 = left(i, 1).getSiteName();
          String tl2 = left(i, 2).getSiteName();
          s.add(tl0);
          s.add(tl1);
          s.add(tl2);
          s.sort(CASE_SENSITIVE_ORDER);
          String s2 = s.getString(0) + s.getString(1) + s.getString(2);
          if (!s2.equals(top)) {
            Tools.warn("topology= " + top + " expected=" + s2);
          }
        }

        TopEvent evt = new TopEvent(getVertex(i).getLocation(), 0, top);

        //        for (int j = 0; j < graph.nCount(i); j++) {
        //          String a = left(i, j).getSiteName();
        //          sb2.append(a);
        //        }
        triples.add(evt); //sb2.toString());
      }

      triples.sort(TopEvent.LABELCOMPARATOR);
      for (Iterator it = triples.iterator(); it.hasNext();) {
        TopEvent te = (TopEvent) it.next();
        Tools.addSp(sb);
        sb.append(te.getLabel());
      }
      topology = sb.toString();
    }
    return topology;
  }
  public TopEvent event(int index) {
    getTopology();
    return (TopEvent) triples.get(index);
  }

  public FPoint2 getEventLoc(int index) {
    getTopology();
    return event(index).getLocation();
  }
  public String getEventLabel(int index) {
    getTopology();
    return event(index).getLabel(); //label;
  }

  public int nEvents() {
    getTopology();
    //    constructEvents();
    return triples.size();
  }

  //  private void constructEvents() {
  //    if (events == null) {
  //      events = new DArray();
  //      for (int i = startVert(); i < endVert(); i++) {
  //        VornVertex v = getVertex(i);
  //        events.add(v.getLocation());
  //      }
  //    }
  //  }

  private DArray triples;
  //  private int infVertCount;
  private Map vertMap;
  // map of site labels -> lowest vertex on cell boundary
  private Map siteVertices;
  private int nVertices;
  private int infVertNum;

  private double shift;

  private boolean proper;

  private String topology;
  // underlying graph
  private Graph graph;
  // array of sites
  private DArray sites;
}
