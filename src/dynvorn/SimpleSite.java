package dynvorn;

import java.awt.*;
import testbed.*;
import base.*;

public class SimpleSite implements IVornSite, Renderable {

  /**
   * Construct an array of IVornSites from an existing array.
   * Creates IVornSites that are simply points.
   * @param sites
   * @return
   */
  public static DArray buildSites(DArray sites) {
    DArray ret = new DArray();
    for (int i = 0; i < sites.size(); i++) {
      IVornSite s = (IVornSite) sites.get(i);
      ret.add(new SimpleSite(s.getSiteLocation(), s.getSiteName()));
    }
    return ret;
  }

  public FPoint2 location;
  public String label;

  public SimpleSite(FPoint2 loc, String label) {
    this.location = new FPoint2(loc);
    this.label = label;
  }
  public String toString() {
    return label;
  }

  public FPoint2 getSiteLocation() {
    return location;
  }
  public FPoint2 getSiteLocation(double time) {
    return location;
  }
  public String getSiteName() {
    return label;
  }
  private static final FPoint2 zero = new FPoint2();
  public FPoint2 getVelocity() {
    return zero;
  }
  public void render(Color c, int stroke, int markType) {
    T.render(location, c, stroke, markType);
    Editor.plotLabel(label, MyMath.ptOnCircle(location, Math.PI / 4, 2.0),
        false, null);
  }
}
