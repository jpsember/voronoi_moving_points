package dynvorn;


import base.*;

public interface IVornSite {
  public String getSiteName();
  public FPoint2 getSiteLocation();
  public FPoint2 getSiteLocation(double time);
  public FPoint2 getVelocity();
}
