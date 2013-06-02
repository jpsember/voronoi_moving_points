package dynvorn;

import java.awt.*;
import base.*;
import testbed.*;

public class ConvexHull implements Renderable {

  // private double time;

  public ConvexHull(DArray vornSites, double time) {
    // this.time = time;
    sites = vornSites;
    //  sites.addAll(getPoints(Main.getSites(false)));
    DArray pts = new DArray();
    for (int i = 0; i < sites.size(); i++)
      pts.add(((IVornSite) sites.get(i)).getSiteLocation(time));
    hullInd = MyMath.convexHull(pts).toIntArray();

    int start = 0;
    String lowest = null;
    for (int i = 0; i < hullInd.length; i++) {
      String s = site(hullInd[i]).getSiteName();
      if (lowest == null || s.compareTo(lowest) < 0) {
        lowest = s;
        start = i;
      }
    }
    poly = new EdPolygon();

    triples = new DArray();

    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < hullInd.length; i++) {
      IVornSite sit = site(i);
      poly.addPoint(sit.getSiteLocation());
      if (i != 0)
        sb.append(' ');
      sb.append(sit.getSiteName());
    }

    for (int i = 0; i < hullInd.length; i++) {
      //      int h0 = hullInd[MyMath.mod(i - 1 + start, hullInd.length)];
      int h1 = hullInd[MyMath.mod(i + start, hullInd.length)];
      //      int h2 = hullInd[MyMath.mod(i + 1 + start, hullInd.length)];
      IVornSite sit = site(h1);
      triples.add(new TopEvent(sit.getSiteLocation(), 0, sit.getSiteName()));
    }
    topology = sb.toString();
  }

  private DArray triples;

  public void render(Color c, int stroke, int markType) {
    if (c == null)
      c = MyColor.cDARKGREEN;
    if (poly != null)
      poly.render(c, -1, -1);
  }
  private int[] hullInd;
  //  private int nSites2() {
  //    return sites.size();
  //  }
  //  private IVornSite site2b(int i) {
  //    return (IVornSite) sites.getMod(i);
  //  }
  public String getTopology() {
    return topology;
  }
  public IVornSite site(int i) {
    return (IVornSite) sites.get(hullInd[MyMath.mod(i, hullInd.length)]);
  }
  public int nSites() {
    return hullInd.length;
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

  //  public int nEvents() {
  //    getTopology();
  //    //    constructEvents();
  //    return triples.size();
  //  }

  private DArray sites;
  private EdPolygon poly;
  private String topology;
}
