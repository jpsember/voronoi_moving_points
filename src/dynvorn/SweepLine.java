package dynvorn;

import java.awt.*;
import testbed.*;
import base.*;

  class SweepLine implements Renderable {
  public void render(Color c, int stroke, int markType) {
    if (defined) {
      V.pushColor(c, MyColor.cDARKGREEN);
      V.pushStroke(stroke);
      V.drawLine(position, 0, position, V.viewRect.height);
      V.pop(2);
    }
  }

  /**
   * Advance sweep line
   * @param x position to advance to
   * @return true if advanced to x, or false if doing slow sweep and we
   *  didn't get that far
   */
  public boolean advanceTo(double x) {

    final boolean db = true;

    if (defined && x < position) {
      T.err("attempt to move sweep line back to " + x + "!" + T.show(this));
    }

    boolean ret = false;

    if (!defined) {
      position = Math.min(x, 0);
      position -= MyMath.mod(position, maxStep);
      stepRemaining = maxStep;
      defined = true;
    }
    {
      if (!slowFlag) {
        ret = true;
        position = x;
      } else {
        double maxMove = Math.min(stepRemaining, x - position);
        position += maxMove;
        ret = position == x;
        stepRemaining -= maxMove;
        if (stepRemaining == 0) {
          stepRemaining = maxStep;
        }
      }
    }
    if (db && T.update())
      T.msg("");
    return ret;
  }

  public SweepLine(double sweepSpeed) {
    this.slowFlag = sweepSpeed > 0;
    maxStep = sweepSpeed;
  }
  public double position() {
    return position;
  }
  public boolean slowFlag() {
    return slowFlag;
  }
  private boolean slowFlag;
  private boolean defined;
  private double position;
  private double stepRemaining;
  private double maxStep = 1.0;
}
