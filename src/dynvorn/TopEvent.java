package dynvorn;

import java.util.*;
import base.*;

public class TopEvent {
  public TopEvent(FPoint2 loc, double time, String label) {
    this.loc = loc;
    this.time = time;
    this.label = label;
//    Streams.out.println("constructed: " + this);
  }
  public static final Comparator LABELCOMPARATOR = new Comparator() {

    public int compare(Object arg0, Object arg1) {
      TopEvent e0 = (TopEvent) arg0, e1 = (TopEvent) arg1;
      return String.CASE_INSENSITIVE_ORDER.compare(e0.label, e1.label);
    }
  };
  public static Comparator comparator = new Comparator() {
    public int compare(Object arg0, Object arg1) {
      TopEvent e0 = (TopEvent) arg0, e1 = (TopEvent) arg1;
      return MyMath.sign(e0.time - e1.time);
    }
  };

  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("TopEvt ");
    sb.append("t:");
    sb.append(Tools.f(time));
    sb.append(" p:");
    sb.append(loc);
    if (label != null)
      sb.append(" " + label);
    return sb.toString();
  }
  private FPoint2 loc;
  private double time;
  public FPoint2 getLocation() {
    return loc;
  }
  public double getTime() {
    return time;
  }
  private String label;
  public String getLabel() {
    return label;
  }
}
