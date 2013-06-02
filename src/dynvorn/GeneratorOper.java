package dynvorn;

import base.*;
import testbed.*;

public class GeneratorOper implements TestBedOperation, Globals {
  /*! .enum  .private 3560  count seed 
  gentype  nsides noise radius dynamic disc polygon  quadratic rect
  generateend
  */

    private static final int COUNT            = 3560;//!
    private static final int SEED             = 3561;//!
    private static final int GENTYPE          = 3562;//!
    private static final int NSIDES           = 3563;//!
    private static final int NOISE            = 3564;//!
    private static final int RADIUS           = 3565;//!
    private static final int DYNAMIC          = 3566;//!
    private static final int DISC             = 3567;//!
    private static final int POLYGON          = 3568;//!
    private static final int QUADRATIC        = 3569;//!
    private static final int RECT             = 3570;//!
    private static final int GENERATEEND      = 3571;//!
/* !*/

  public void addControls() {
    C.sOpenTab("Generator");
    {
      C
          .sStaticText("Generates static or dynamic Voronoi sites based on various parameters");

      C.sOpen();
      C.sIntSlider(COUNT, "Count", "# of sites", 0, 200, 30, 10);
      C.sCheckBox(DYNAMIC, "Dynamic", "generate dynamic vs static sites", true);
      C.sNewColumn();
      C.sIntSlider(SEED, "Seed", "seed for generator", 0, 10000, 1, 1);
      C.sIntSlider(RADIUS, "Radius", null, 0, 1000, 800, 10);
      C.sIntSlider(NOISE, "noise", null, 0, 1000, 20, 10);
      C.sClose();
      C.sOpenTabSet(GENTYPE);
      C.sOpenTab(DISC, "Disc");
      C.sCloseTab();
      C.sOpenTab(POLYGON, "Polygon");
      C.sIntSlider(NSIDES, "# sides", "# of sides in polygon", 3, 100, 6, 1);
      C.sCloseTab();
      C.sOpenTab(QUADRATIC, "Quadratic");
      C.sCloseTab();
      C.sOpenTab(RECT, "Rect");
      C.sCloseTab();
      C.sCloseTabSet();
    }
    C.sCloseTab();
  }

  public static GeneratorOper singleton = new GeneratorOper();

  private GeneratorOper() {
  }

  public void paintView() {
    Editor.render(); //true, true, false);

  }

  public void processAction(TBAction a) {
    if (a.ctrlId >= COUNT && a.ctrlId <= GENERATEEND)
      generate(false);
    //      if (a.ctrlId >= COUNT && a.ctrlID < GENERATE)
  }

  public void runAlgorithm() {
  }

  private static EdObject gen(FPoint2 pt, double theta) {
    if (C.vb(DYNAMIC))
      return new EdMovingPt(pt, theta);
    return new EdPoint(pt);
  }
  private static EdObject gen(double x, double y, double theta) {
    return gen(new FPoint2(x, y), theta);
  }

  public static void generate(boolean newSeed) {

    if (newSeed)
      C.seti(SEED, C.vi(SEED) + 1);

    MyMath.seed(C.vi(SEED));
    int n = C.vi(COUNT);
    FPoint2 origin = new FPoint2(50, 50);
    double rad = C.vi(RADIUS) * 55.0 / 1000;

    DArray a = new DArray();
    double noise = C.vi(NOISE) / 1000.0;
    switch (C.vi(GENTYPE)) {
    case DISC:
      for (int i = 0; i < n; i++) {
        FPoint2 pt = MyMath.rndPtInDisc(origin, rad, null);
        double theta = MyMath.rnd(Math.PI * 2);
        a.add(gen(pt, theta));
      }
      break;
    case RECT:
      rad *= 2;
      for (int i = 0; i < n; i++) {
        FPoint2 pt = new FPoint2(origin.x + MyMath.rnd(rad) - rad/2, origin.y + MyMath.rnd(rad) - rad/2);
        double theta = MyMath.rnd(Math.PI * 2);
        a.add(gen(pt, theta));
      }
      break;
    case QUADRATIC:
      {
        noise = noise / 10;
        if (noise == 0)
          noise = 1e-2;

        final double X = 2, Y = 65;
        for (int i = 0; i < n / 2; i++) {
          a.add(gen(MyMath.ptOnCircle(new FPoint2(X + 8, Y - 20),
              Math.PI * .75, (i * rad) / n), 0)); //+ MyMath.rnd(noise) - noise / 2));
        }
        final double K = 12;
        final double M = rad * .5;
        final double M1 = 25;
        final double M2 = 25;
        for (int i = 0; i < n / 2; i++) {
          a.add(gen(X + K * 1.2 + (i * M * M1) / n, Y + K + (i * M * M2) / n,
              -Math.PI / 2 + +MyMath.rnd(noise) - noise / 2));
        }
      }
      break;

    case POLYGON:
      {
        int nsides = C.vi(NSIDES);
        int sitesPerSide = Math.max(1, n / nsides);
        double ang = Math.PI * 2 / nsides;

        double baseLen = Math.cos(Math.PI / nsides);
        double height = Math.sin(Math.PI / nsides);

        for (int i = 0; i < n; i++) {
          int edge = i / sitesPerSide;
          double distAlongEdge = i % sitesPerSide;

          distAlongEdge = distAlongEdge / sitesPerSide - .5;

          double y = distAlongEdge * height * 2;

          double dist = Math.sqrt(baseLen * baseLen + y * y);
          double th = Math.atan2(y, baseLen);

          FPoint2 pt = MyMath.ptOnCircle(origin, th + edge * ang, dist * rad);
          addNoiseTo(pt, noise);

          double theta = edge * ang + Math.PI + rnd(noise) * Math.PI * 2;
          a.add(gen(pt, theta));
        }

      }
      break;
    }
    Editor.replaceAllObjects(a);
  }
  private static void addNoiseTo(FPoint2 pt, double noise) {
    pt.x += rnd(noise * 8);
    pt.y += rnd(noise * 8);
  }

  private static double rnd(double n) {
    return MyMath.rnd(n) - n / 2;
  }

}
