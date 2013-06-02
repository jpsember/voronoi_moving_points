package dynvorn;

import java.util.*;
import testbed.*;
import base.*;

/**
 * Convex hull history, ordered by time.
 * Keeps track of what topology is occurring within a particular 
 * range of time.
 */
public class HullHistory {

  public static final boolean THREADED = true;

  //  private static final boolean VERBOSE = true;

  private double epsilon;

  private static final boolean db = false;

  /**
   * Construct Voronoi history
   * @param vs array of IVornSites
   * @param timeSpan duration of history to construct
   * @param eps minimum time slice to examine
   */
  public HullHistory(DArray vs, double timeSpan, double eps) {
    this.epsilon = eps;
    this.maxTime = timeSpan;
    this.vs = new DArray();
    this.vs.addAll(vs);
    this.nSites = vs.size();
    entries = new TreeSet(HEnt.comparator);
    if (db)
      Streams.out.println("constructing VornHistory for " + vs.size()
          + " sites");
    startThread();
  }

  private Thread thread;
  //  private long startTime;
  //  private long elapsed;
  private void startThread() {
    final int SLICE = 50;

    if (thread != null)
      throw new IllegalStateException();

    status = "?";

    if (THREADED) {
      thread = new Thread(new Runnable() {
        public void run() {
          //        startTime = System.currentTimeMillis();
          while (true) {
            if (thread == null)
              break;
            boolean done = build(SLICE);
            V.repaint(500);
            if (done)
              break;
            Thread.yield();
          }
          //        elapsed = System.currentTimeMillis() - startTime;
        }
      });
      thread.setPriority(Thread.MIN_PRIORITY);
      thread.start();
    } else {
      build(0);
      if (db)
        Streams.out.println("done building history:\n" + this);

    }
  }
  public void stopThread() {
    thread = null;
  }

  private static double[] failureTimeAdj = { 0, 1e-2, 1e-1, 1.0, };

  /**
   * If entry covering this time doesn't exist in the tree,
   * (1) calculate the convex hull;
   * (2) see if it matches 
   *        an immediate neighbor in the tree; 
   * (3) if so, extend that neighbor's range to include this time,
   *   and return its entry
   * (4) otherwise, create a new entry and return it
   * @param t time 
   * @return HEnt, or null if unrecoverable error occurred
   */
  private HEnt constructEntryFor(double t) {
    final boolean db2 = false;

    if (db2)
      Streams.out.println("constructEntryFor " + Tools.f(t));

    HEnt h = null;
    do {
      HEnt find = new HEnt();
      find.t0 = find.t1 = t;
      SortedSet follow = entries.tailSet(find);

      if (!follow.isEmpty()) {
        h = (HEnt) follow.first();
        if (db2)
          Streams.out.println(" first in tailset=   " + h);

        if (h.includes(t)) {
          break;
        }
      }

      if (db)
        Streams.out.println(" constructing Vorn diag for time " + Tools.f(t)
            + " : " + t);

      ConvexHull g = null;

      for (int pass = 0; pass < failureTimeAdj.length; pass++) {
        g = new ConvexHull(Main.getSites(true), t + failureTimeAdj[pass]);
        //            EdMovingPt.getSitesAtTime(vs, t
        //            + failureTimeAdj[pass]));
      }

      if (h != null && g.getTopology().equals(h.diag.getTopology())) {
        updateKnown(h.extendTo(t, tKnown, g), h,
            "topology matches, extending t");
        if (db2)
          Streams.out.println(" matched tailset");
        break;
      }

      //      if (h0 != null && g.getTopology().equals(h0.diag.getTopology())) {
      //        updateKnown(h0.extendTo(t, tKnown), h0, "topology matches, extending t");
      //        if (db2)
      //          Streams.out.println(" matched headset");
      //        break;
      //      }

      SortedSet before = entries.headSet(find);

      if (!before.isEmpty()) {
        h = (HEnt) before.last();
        if (db2)
          Streams.out.println(" last in headset=    " + h);

        if (g.getTopology().equals(h.diag.getTopology())) {
          updateKnown(h.extendTo(t, tKnown, g), h,
              "topology matches tail, extending");
          if (db2)
            Streams.out.println(" matched headset");
          break;
        }
      }

      h = new HEnt();
      h.t0 = h.t1 = t;
      h.diag = g;
      entries.add(h);
      nEntries = entries.size();
      if (db)
        Streams.out.println(" no match, added new:" + h);

    } while (false);
    if (db2)
      Streams.out.println(" returning=          " + h);

    return h;
  }

  public DArray getEvents(double t0, double t1) {
    final boolean db = false;
    if (db)
      Streams.out
          .println("getEvents, t0=" + Tools.f(t0) + " t1=" + Tools.f(t1));

    DArray ret = new DArray();

    TopEvent seek = new TopEvent(new FPoint2(), t0, null);

    synchronized (events) {
      Iterator it = events.tailSet(seek).iterator();
      while (it.hasNext()) {
        TopEvent evt = (TopEvent) it.next();
        if (db)
          Streams.out.println(" " + evt);

        if (evt.getTime() > t1)
          break;
        ret.add(evt);
        if (db)
          Streams.out.println("  adding");

      }
    }
    return ret;
  }
  private HEnt lastKnownEnt;

  private static class PtEvent {
    public PtEvent(ConvexHull hull, int siteIndex, double time) {
      this.hull = hull;
      this.time = time;
      this.site = siteIndex;
    }
    private ConvexHull hull;
    private int site;
    private double time;
    public String siteName() {
      return hull.site(site).getSiteName();
    }
    public IVornSite siteLoc() {
      IVornSite v = hull.site(site);
      return v;
      //      return hull.site(site).getSiteLocation();
    }
  }

  private void updateKnown(double t, HEnt entry, String reason) {
    //    final boolean db = true;
    final boolean db2 = false;
    //final boolean db = true;

    if (tKnown != t) {
      if (false && db2)
        Streams.out.println("updateKnown, t=" + Tools.f(t) + " tKnown="
            + Tools.f(tKnown) + " reason=" + reason + "\n lastKnownEnt="
            + lastKnownEnt + "\n " + "thisKnownEnt=" + entry);

      if (lastKnownEnt != entry) {
        if (lastKnownEnt == null) {
          //          ConvexHull g1 = entry.diag;
          //          if (db)
          //            Streams.out.println("storing initial graph:\n" + g1.getTopology());
          //          for (int k = 0; k < g1.nEvents(); k++) {
          //            TopEvent newEvt = g1.event(k);
          //            TopEvent ne = new TopEvent(newEvt.getLocation(), t, newEvt
          //                .getLabel());
          //            if (db)
          //              Streams.out.println(" adding " + ne);
          //            events.add(ne);
          //          }
        } else {
          ConvexHull g0 = null;
          ConvexHull g1 = entry.diag;

          g0 = lastKnownEnt.diag;
          if (db2)
            Streams.out.println("comparing graphs:\n" + Tools.f(tKnown) + ": "
                + g0.getTopology() + "\n" + Tools.f(t) + ": "
                + g1.getTopology()); //+ "\n" + Tools.st());

          {
            Map map = new HashMap();
            for (int pass = 0; pass < 2; pass++) {
              ConvexHull h = (pass == 0) ? g0 : g1;
              // Streams.out.println("pass="+pass+", hull pts="+h.nSites()+" top="+h.getTopology());
              for (int i = 0; i < h.nSites(); i++) {
                PtEvent evt = new PtEvent(h, i, pass == 0 ? entry.t0 : entry.t0);
                String key = evt.siteName();
                //Streams.out.println("key="+key+", map.contains="+map.containsKey(key));
                if (map.containsKey(key))
                  map.remove(key);
                else
                  map.put(key, evt);
              }
            }
            for (Iterator it = map.keySet().iterator(); it.hasNext();) {
              PtEvent evt = (PtEvent) map.get(it.next());
              TopEvent ne = new TopEvent(evt.siteLoc()
                  .getSiteLocation(evt.time), entry.t0, evt.siteName());
              events.add(ne);
            }
          }
        }
        lastKnownEnt = entry;
      }
      tKnown = t;
    }
  }
  private SortedSet events = new TreeSet(TopEvent.comparator);
  private Random rnd = new Random();

  private boolean build(int maxMS) {

    long currTime = System.currentTimeMillis();
    // determine next time we would like to know 
    double t = 0;

    if (db)
      Streams.out.println("build maxMS=" + maxMS);

    synchronized (events) {
      while (true) {
        if (false && db)
          Streams.out.println("tKnown=" + Tools.f(tKnown) + " t=" + Tools.f(t));

        if (tKnown >= maxTime) {
          if (db)
            Streams.out
                .println(" tKnown " + Tools.f(tKnown) + " > n, stopping");
          status = ".";
          break;
        }

        // stop if we've exceed our time slice
        if (maxMS > 0) {
          long elapsed = System.currentTimeMillis() - currTime;
          if (elapsed >= maxMS) {
            if (db)
              Streams.out.println("exceed time slice");
            break;
          }
        }

        if (t <= tKnown) {
          double newTime = Math.min(tKnown + maxTime
              * (rnd.nextDouble() * .05 + .05), maxTime);
          if (db)
            Streams.out.println("t < known, (" + Tools.f(t) + " < "
                + Tools.f(tKnown) + ") increasing to " + Tools.f(newTime));
          t = newTime;
        }
        // construct entry for this Voronoi diagram
        HEnt h = constructEntryFor(t);
        if (h == null) {
          status = "!E";
          return true;
        }

        if (false && db)
          Streams.out.println("constructed entry for " + Tools.f(t));

        // if time is zero, store as initial topology
        if (t == 0) {
          updateKnown(0, h, "storing time=0 initial topology");
          continue;
        }

        // if entry includes known time, loop; we will search for
        // a higher time value
        if (h.includes(tKnown)) {
          if (db)
            Streams.out.println("Entry includes tKnown " + Tools.f(tKnown)
                + ", looping");
          continue;
        }

        //        if (!h.includes(tKnown)) {
        if (t - tKnown <= epsilon) {
          if (db)
            Streams.out.println(Tools.f(t) + " - " + Tools.f(tKnown)
                + " difference very small, updating known");
          updateKnown(h.t1, h, "difference very small");
          continue;
        }
        t = (t + tKnown) * .5;
        if (false && db)
          Streams.out
              .println(" entry is not in known range, cutting t in half");
      }
    }
    return tKnown >= maxTime;
  }
  // time that we know events up to
  private double tKnown = -1;
  private DArray vs;

  private static class HEnt {
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("t[");
      sb.append(Tools.f(t0));
      sb.append("..");
      sb.append(Tools.f(t1));
      sb.append("]");
      if (diag != null)
        sb.append(" " + Tools.d(diag.getTopology(), 40, false));
      return sb.toString();
    }

    private ConvexHull diag;
    private double t0;
    private double t1;
    public static final Comparator comparator = new Comparator() {
      final boolean db = false;

      public int compare(Object o1, Object o2) {
        HEnt h1 = (HEnt) o1, h2 = (HEnt) o2;
        int ret = 0;
        if (h1.t1 < h2.t0)
          ret = -1;
        else if (h2.t1 < h1.t0)
          ret = 1;
        //        
        //        int ret = MyMath.sign(h1.t0 - h2.t0);
        if (db)
          Streams.out.println("compare entries: \n" + h1 + "\n" + h2
              + " returning " + ret);
        return ret;
      }
    };
    public boolean includes(double t) {
      return t0 <= t && t1 >= t;
    }

    /**
     * Extend range to include a time value; increase previous known
     * value if it includes this range
     * @param t
     * @param prevKnown previous known value
     * @return new previous known value, which may have been increased
     */
    public double extendTo(double t, double prevKnown, ConvexHull g) {
      double prev0 = t0;
      double prev1 = t1;
      double pk = prevKnown;
      if (t0 > t)
        this.diag = g;
      t0 = Math.min(t0, t);
      t1 = Math.max(t1, t);
      if (prevKnown >= t0 && prevKnown < t1)
        prevKnown = t1;
      if (db)
        Streams.out.println("extending entry from " + Tools.f(prev0) + "/"
            + Tools.f(prev1) + " to " + Tools.f(t0) + "/" + Tools.f(t1)
            + " prevKnown=" + Tools.f(pk) + " to " + Tools.f(prevKnown));

      return prevKnown;
    }
  }

  public String toString() {
    StringBuilder sb = new StringBuilder();
    synchronized (events) {
      sb.append("HullHistory, tKnown=" + Tools.f(tKnown) + "\n");
      sb.append("# sites:" + vs.size() + "\n");
      sb.append("# entries:" + entries.size() + "\n");
      int count = 0;
      for (Iterator it = entries.iterator(); it.hasNext(); count++) {
        if (count >= 5) {
          sb.append("..." + (entries.size() - count) + " more");
          break;
        }
        HEnt h = (HEnt) it.next();
        sb.append(" " + h);
        sb.append("\n");
      }
    }
    return sb.toString();
  }

  public String getInfo() {
    StringBuilder sb = new StringBuilder();
    sb.append("n=" + nSites);
    sb.append(" e=" + nEntries);
    if (status != null)
      sb.append(status);
    sb.append(" t=" + Tools.f(tKnown));
    return sb.toString();
  }

  private String status;
  private int nSites;
  private SortedSet entries;
  private double maxTime;
  private int nEntries;
}
