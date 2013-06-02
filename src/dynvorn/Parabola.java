package dynvorn;

import java.awt.*;
import testbed.*;
import base.*;

class Parabola implements Renderable {

  /**
   * Mark parabola as degenerate
   * @param focus focus of degenerate parabola
   */
  public void setDegIntercept(FPoint2 pt) {
    degenerate = pt;
  }
  private FPoint2 degenerate;

  public Parabola(FPoint2 focus, double sweepX) {
    this.focus = focus;
    this.sweepX = sweepX;
    y0 = -10000;
    y1 = 10000;
  }

  public void setMin(double t) {
    y0 = t;
  }
  public void setMax(double t) {
    y1 = t;
  }

  public void render(Color c, int stroke, int markType) {

    V.pushStroke(stroke);
    V.pushColor(c, MyColor.get(MyColor.RED, .5));
    do {
      if (degenerate != null) {
        V.drawLine(degenerate, focus);
        break;
      }

      // clip to view
      double yAt0H = yAt(0);
      if (Double.isNaN(yAt0H)) {
        Tools.warn("yAt x=" + Tools.f(0) + " is NaN");
        break;
      }

      double yAt0L = 2 * focus.y - yAt0H;
      y0 = Math.max(y0, yAt0L);
      y0 = Math.max(y0, 0);
      y1 = Math.min(y1, yAt0H);
      y1 = Math.min(y1, V.viewRect.height);
      if (y0 > y1)
        break;

      FPoint2 prev = null;
      double step = .5;

      for (double t = y0;;) {
        FPoint2 pt = pt(t);
        if (prev != null)
          V.drawLine(prev, pt);
        prev = pt;
        if (t == y1)
          break;
        t = Math.min(t + step, y1);
      }

    } while (false);
    V.pop(2);
  }

  private FPoint2 pt(double y) {
    return new FPoint2(xAt(y), y);
  }

  private double xAt(double y) {
    double a = focus.x;
    double s = sweepX;
    double t = y - focus.y;
    double x = (s * s - a * a - t * t) / (2 * (s - a));
    return x;
  }

  private double yAt(double x) {
    double a = focus.x;
    double b = focus.y;
    double s = sweepX;
    double B = -2 * b;
    double C = 2 * x * (s - a) + a * a + b * b - s * s;
    double q = B * B - 4 * C;
    if (Math.abs(q) < 1e-5)
      q = 0;
    if (q < 0) {
      Tools.warn("q < 0:" + q + " focus=" + focus + " s=" + sweepX);
      return 0;
    }
    double y = (-B + Math.sqrt(q)) / 2;
    return y;
  }

  private FPoint2 focus;
  private double sweepX;
  private double y0, y1;
}
