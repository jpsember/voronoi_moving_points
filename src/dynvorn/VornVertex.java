package dynvorn;


import base.*;

public class VornVertex {

  public VornVertex(FPoint2 location, boolean inf) {
    this.atInfinity = inf;
   this.location = new FPoint2(location);
  }
  public FPoint2 getLocation() {
//    if (isAtInfinity())
//      throw new IllegalStateException();
    return location;
  }
  public boolean isAtInfinity() {
    return atInfinity;
  }
  public int getNodeNumber() {
    return nodeNumber;
  }
  public void setNodeNumber(int i) {
    this.nodeNumber = i;
  }
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("VornVertex");
    sb.append(" loc=" + location);
    sb.append(" atInf=" + Tools.f(isAtInfinity()));
    sb.append(" node#=" + nodeNumber);
    return sb.toString();
  }
  private int nodeNumber;
  private boolean atInfinity;
  private FPoint2 location;
}
