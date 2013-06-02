package dynvorn;

import base.*;
import testbed.*;

/**
 * Polynomials class
 *
 * The term 'curve' in this class means a quadratic polynomial
 * in two variables, and is stored as a 5-degree polynomial in
 * one variable, with a special flag (isCurve()) to indicate that
 * it is a curve.
 * 
 * Rework to implement IVector class
 */
public final class Polyn implements Globals, IVector {

  // two-variable polynomial with degree zero, represents 0
  public static final Polyn ZERO = new Polyn(0);

  private static double NEARZERO = 1E-16;

  public static double nearZero() {
    return NEARZERO;
  }

  public static double sqrt(double val) {
    if (val < 0)
      throw new FPError("*Attempt to take root of negative: " + val);
    return Math.sqrt(val);
  }

//  public static double sqrt(double val, double zeroVal) {
//    if (val < 0 && val > -zeroVal) return 0;
//    return sqrt(val);
//  }
  
  /**
   * Calculate the cube root of a value
   * @param a : value to calculate root of
   * @return
   */
  private static double cubeRoot(double a) {
    if (a >= 0) {
      return Math.pow(a, 1.0 / 3);
    } else {
      return -Math.pow(-a, 1.0 / 3);
    }
  }

  private Polyn() {
  }

  public Polyn(double[] coeffs) {
    int count = coeffs.length;
    int start = 0;
    this.coefficients = new double[count];
    System.arraycopy(coeffs, start, coefficients, 0, count);
    calcDegree();
  }

  /**
   * Construct a quartic
   * @param x4 double
   * @param x3 double
   * @param x2 double
   * @param x1 double
   * @param x0 double
   */
  public Polyn(double x4, double x3, double x2, double x1, double x0) {
    coefficients = new double[5];
    coefficients[0] = x0;
    coefficients[1] = x1;
    coefficients[2] = x2;
    coefficients[3] = x3;
    coefficients[4] = x4;
    calcDegree();
  }

  /**
   * Construct a cubic
   * @param x3 double
   * @param x2 double
   * @param x1 double
   * @param x0 double
   */
  public Polyn(double x3, double x2, double x1, double x0) {
    coefficients = new double[4];
    coefficients[0] = x0;
    coefficients[1] = x1;
    coefficients[2] = x2;
    coefficients[3] = x3;
    calcDegree();
  }

  /**
   * Construct a quadratic
   * @param x2 double
   * @param x1 double
   * @param x0 double
   */
  public Polyn(double x2, double x1, double x0) {
    coefficients = new double[3];
    coefficients[0] = x0;
    coefficients[1] = x1;
    coefficients[2] = x2;
    calcDegree();
  }

  /**
   * Construct a linear polynomial
   * @param x1 double
   * @param x0 double
   */
  public Polyn(double x1, double x0) {
    coefficients = new double[2];
    coefficients[0] = x0;
    coefficients[1] = x1;
    calcDegree();
  }

  /**
   * Construct a constant
   * @param x1 double
   * @param x0 double
   */
  public Polyn(double x0) {
    coefficients = new double[1];
    coefficients[0] = x0;
    calcDegree();
  }

  /**
   * Calculate the degree of the polynomial, as well as
   * its magnitude
   */
  private void calcDegree() {
    {
      int i = coefficients.length - 1;
      for (; i > 0; i--) {
        if (!isCoefficientZero(i)) {
          break;
        }
      }
      degree = i;
      while (++i < coefficients.length) {
        coefficients[i] = 0;
      }
    }
    magnitude = 0;
    for (int i = 0; i < degree; i++) {
      double v = Math.abs(coefficients[i]);
      if (v > magnitude)
        magnitude = v;
    }
  }

  /**
   * Determine the degree of the polynomial
   * @return degree, where 0=constant, 4=quartic
   */
  public int degree() {
    return degree;
  }

  /**
   * Determine if a coefficient is zero
   * @param index : index of coefficient (0=constant term)
   * @return true if coefficient is (essentially) zero
   */
  public boolean isCoefficientZero(int index) {
    return isZero(c(index));
  }

  public boolean isZero() {
    return degree() == 0 && isZero(c(0));
  }

  /**
   * Determine if a value is zero
   * @param v : value
   * @return true if value is (essentially) zero
   * 
   */
  public static boolean isZero(double v) {
    return (Math.abs(v) < NEARZERO);
  }

  /**
   * Determine if a value is zero
   * @param v : value
   * @param limit : cutoff value
   * @return true if |v| < limit
   */
  public static boolean isZero(double v, double limit) {
    return (Math.abs(v) < limit);
  }

  public static boolean nearZero(double v) {
    return (Math.abs(v) < NEARZERO * 100);
  }

  public double c(int index) {
    return coefficients[index];
  }

  public String toString() {
    return toString(true, 4, 6);
  }

  public String toString(boolean allDigits) {
    return toString(allDigits, 4, 6);
  }

  public String toString(boolean allDigits, int intDig, int fracDig) {
    StringBuilder sb = new StringBuilder();
    int d = degree();
    {
      sb.append("Polyn (deg=");
      sb.append(d);
      sb.append(") ");
    }

    if (allDigits) {
      sb.append("\n");
    }

    for (int i = d; i >= 0; i--) {
      if (allDigits) {
        if (i < d) {
          sb.append("\n + ");
        } else {
          sb.append("   ");
        }
      } else {
        if (i < d) {
          sb.append(" + ");
        }
      }
      if (allDigits) {
        String dg = Double.toString(c(i));
        if (dg.charAt(0) != '-') {
          sb.append(' ');
        }
        sb.append(dg);
      } else {
        sb.append(Tools.f(c(i), intDig, fracDig)); //dblStr(c(i), true, intDig, fracDig));
      }

      sb.append(' ');
      {
        switch (i) {
        case 0:
          break;
        case 1:
          sb.append('x');
          break;
        default:
          sb.append('x');
          sb.append((char) (i + '0'));
          break;
        }
      }
    }
    return sb.toString();
  }

  //  public static void warnIfNearZero(double v, String desc) {
  //    if (nearZero(v)) {
  //      System.out.println("*** WARNING: value " + desc + " is very near zero: "
  //          + Tools.fz(v));
  //    }
  //  }

  /**
   * Evaluate polynomail
   * @param x : value of the single variable
   * @return double : evaluation
   */
  public double eval(double x) {
    double res = 0;

    switch (degree()) {
    default:
      throw new IllegalArgumentException();

    case 4:
      res = c(4);
    case 3:
      res = res * x + c(3);
    case 2:
      res = res * x + c(2);
    case 1:
      res = res * x + c(1);
    case 0:
      res = res * x + c(0);
      break;
    }
    return res;
  }

  /**
   * Test program.
   *
   * Attempts to find formula for the intersection of
   * two quadratics in two variables, each of which
   * represents a plane curve (i.e. a conic section)
   */
  public static void main(String[] args) {

    if (true) {
      final Polyn[] testpolys = { new Polyn(6.460895572133681E-8,
          -1.4472406081579444E-5, 0.0011905582232607812, -0.04257159006154124,
          0.5590504286200949),

      };

      DArray a = new DArray();
      for (int i = 0; i < testpolys.length; i++) {
        Polyn p = testpolys[i];
        try {
          p.solve(a);
          Streams.out.println("Roots for " + p + ":\n" + Tools.d(a));
        } catch (TBError e) {
          System.out.println(e.toString());
        }
      }

      return;
    }

  }

  /**
   * Find roots for this polynomial, which is assumed to be
   * a quadratic
   * @param roots : an EMPTY array to store Doubles in
   */
  private void solveQuadratic(DArray roots, double fuzzyMargin) {
    final boolean db = false;

    double a = c(2), b = c(1), c = c(0);
    if (db) {
      System.out.println("solveQuadratic\n a=" + a + "\n b=" + b + "\n c=" + c
          + " \nfuzzyMargin=" + fuzzyMargin);
    }
    double radical = b * b - 4 * a * c;
    if (db) {
      System.out.println(" radical=" + radical);
    }
    if (radical < -fuzzyMargin) {
    } else if (radical <= fuzzyMargin) {
      roots.addDouble(-b / (2 * a));
    } else {
      double recip = 1 / (2 * a);
      double root = Polyn.sqrt(radical);
      roots.addDouble(recip * (root - b));
      roots.addDouble(recip * (-root - b));
    }
    if (db) {
      System.out.println("Roots:\n" + roots);
    }
  }

  // for debug use, to suppress verbose output of nested
  // calls to solve()
  private static int dbNest;

  /**
   * Find roots for the polynomial
   * @param roots : Doubles are returned in this array, one for
   *  each root found
   * @param fuzzyMargin : if not 0, a positive value close to zero
   *  to allow some floating point error when calculating roots;
   *  specifically, if the radical in a quadratic equation is negative
   *  with magnitude less than this amount, it's treated as zero
   */
  public void solve(DArray roots, double fuzzyMargin) {
    boolean db = false;
    if (db) {
      dbNest++;
      if (db) {
        System.out.println("solve " + this.toString(true));
      }
    }
    roots.clear();
    switch (degree()) {
    default:
      Tools.ASSERT(false, "Unsupported: finding roots for " + this);
      break;
    case 0:
      {
        // constant function; return zero if constant is zero
        if (isZero(c(0))) {
          roots.addDouble(0);
        }
      }
      break;
    case 1:
      {
        // linear function
        roots.addDouble(-c(0) / c(1));
      }
      break;
    case 2:
      solveQuadratic(roots, fuzzyMargin);
      break;
    case 3:
      solveCubic(roots);
      break;
    case 4:
      solveQuartic(roots);
      break;
    }
  }

  /**
   * Find roots for the polynomial
   * @param roots : Doubles are returned in this array, one for
   *  each root found
   */
  public void solve(DArray roots) {
    solve(roots, 0);
  }

  /**
   * Calculate the roots of a cubic.
   * @param roots : an EMPTY array to store Doubles in
   */
  private void solveCubic(DArray roots) {

    final boolean db = false;

    // Scale so a is 1.0

    double a0 = c(3), b0 = c(2), c0 = c(1), d0 = c(0);
    double as = 1.0 / a0;

    double a = b0 * as;
    double b = c0 * as;
    double c = d0 * as;
    if (db) {
      System.out.println("solveCubic a=" + a + " b=" + b + " c=" + c);
    }
    double asq = a * a;

    double p = (1 / 3.0) * asq - b;
    double q = (-2 / 27.0) * (asq * a) + (a * b) / 3 - c;

    if (db) {
      System.out.println(" q*q/4-p*p*p/27= " + (q * q / 4 - p * p * p / 27));
    }
    if (q * q / 4 - p * p * p / 27 >= 0) {
      // Solve using Cardano
      double ra = q / 2, rb = p / 3;

      double rad = Polyn.sqrt(ra * ra - (rb * rb * rb) );
      double s1 = cubeRoot(ra + rad);
      double s2 = cubeRoot(ra - rad);

      roots.addDouble(s1 + s2 - a / 3);
      if (db) {
        System.out.println(" cardano " + (s1 + s2 - a / 3));
      }
    } else {
      // Solve using Viete
      double k = Polyn.sqrt(4 * p / 3);
      double cv = 3 * q / (p * k);
      double th = Math.acos(cv);
      double cval = Math.cos(th / 3);
      double y = k * cval;
      if (db) {
        System.out.println(" viete " + (y - a / 3));
      }
      roots.addDouble(y - a / 3);
    }
    if (db) {
      System.out.println("roots=" + roots);
    }
  }

  /**
   * Calculate the roots of a general quartic equation.
   * @param roots : an EMPTY array to store Doubles in
   */
  private void solveQuartic(DArray roots) {
    final boolean db = false;

    if (db) {
      System.out.println("solveQuartic");
    }

    // get coefficients into locals b0...b4
    double b4 = c(4), b3 = c(3), b2 = c(2), b1 = c(1), b0 = c(0);
    double b4i = 1.0 / b4;

    // scale to a0...a4 such that a4 = 1
    double a3 = b3 * b4i, a2 = b2 * b4i, a1 = b1 * b4i, a0 = b0 * b4i;

    double asq = a3 * a3;
    double p = a2 - (3.0 / 8) * asq;
    double q = a1 - .5 * a2 * a3 + asq * a3 / 8;
    double r = a0 - a1 * a3 / 4 + a2 * asq / 16 - 3 * asq * asq / 256;

    // find u as solution to cubic u^3 - p*u^2 -4u + (-4*pr-q^2) = 0
    Polyn cubic = new Polyn(1, -p, -4 * r, 4 * p * r - q * q);
    
    if (db) 
      Streams.out.println(" solving cubic:\n"+cubic);
          
    final DArray cRoots = new DArray();
    cubic.solve(cRoots);
    if (db) {
      System.out.println(  " solutions:" + cRoots);
    }
    if (cRoots.size() == 0) {
      throw new FPError(
          "Quartic solver, failed to find root for cubic;\n quartic=" + this
              + "\n cubic=" + cubic);
    }

    double u = cRoots.getDouble(0);

    // the problem is related to u being very close to p,
    // and less than p...

    final boolean zeroTest = true;

    double A = 0;

    if (!(zeroTest && u < p))
      A = Polyn.sqrt(u - p);

    double qb = A;
    double qc = u / 2;
    if (zeroTest && A == 0) {
      qc = 0;
    } else
      qc -= (q / (2 * A));

    final DArray qRoots = new DArray();

    Polyn quadratic = new Polyn(1, qb, qc);
    quadratic.solve(qRoots);

    for (int i = 0; i < qRoots.size(); i++) {
      roots.addDouble(qRoots.getDouble(i) - a3 * .25);
    }

    qb = -A;
    qc = u / 2;
    if (zeroTest && A == 0)
      qc = 0;
    else
      qc += q / (2 * A);
    Polyn quadratic2 = new Polyn(1, qb, qc);
    quadratic2.solve(qRoots);
    for (int i = 0; i < qRoots.size(); i++) {
      roots.addDouble(qRoots.getDouble(i) - a3 * .25);
    }
  }

  private void prepareCoeff(int deg) {
    coefficients = new double[deg + 1];
    degree = deg;
  }

  public double magnitude() {
    return magnitude;
  }

  /**
   * Calculate product of two polynomials
   * @param a Polyn
   * @param b Polyn
   * @return Polyn
   */
  public static Polyn product(Polyn a, Polyn b) {
    int d = a.degree + b.degree;

    Polyn p = new Polyn();
    p.prepareCoeff(d);

    for (int i = 0; i <= a.degree; i++) {
      for (int j = 0; j <= b.degree; j++) {
        double v = a.coefficients[i] * b.coefficients[j];
        p.coefficients[i + j] += v;
      }
    }
    p.calcDegree();
    return p;
  }

  public static Polyn add(Polyn a, double amult, Polyn b, double bmult) {
    Polyn p = new Polyn();

    int deg = Math.max(a.degree, b.degree);
    p.prepareCoeff(deg);

    for (int i = 0; i <= deg; i++) {
      double av = 0;
      if (i <= a.degree)
        av = amult * a.c(i);
      double bv = 0;
      if (i <= b.degree)
        bv = bmult * b.c(i);
      p.coefficients[i] = av + bv;
    }
    p.calcDegree();
    return p;
  }

  /**
   * Scale polynomial by a constant
   * @param s : scale factor to apply
   * @return copy of this polynomial, with every coefficient multiplied
   *  by s
   */
  public Polyn scaleBy(double s) {
    double[] c = DArray.copy(coefficients);
    for (int i = 0; i < c.length; i++)
      c[i] *= s;
    return new Polyn(c);
  }

  // -------------------------------------------------------
  // IVector interface 
  public double get(int y) {
    return coefficients[y];
  }

  public double length() {
    throw new UnsupportedOperationException();
  }

  public double lengthSq() {
    throw new UnsupportedOperationException();
  }

  public void negate() {
    for (int i = 0; i < degree; i++)
      coefficients[i] = -coefficients[i];
  }

  public double normalize() {
    throw new UnsupportedOperationException();
  }

  public void set(int index, double v) {
    throw new UnsupportedOperationException();
  }

  public void setTo(IVector v) {
    throw new UnsupportedOperationException();
  }

  public void setX(double x) {
    throw new UnsupportedOperationException();
  }

  public void setY(double y) {
    throw new UnsupportedOperationException();
  }

  public void setZ(double z) {
    throw new UnsupportedOperationException();
  }

  public int size() {
    return degree();
  }

  public double x() {
    throw new UnsupportedOperationException();
  }

  public double y() {
    throw new UnsupportedOperationException();
  }

  public double z() {
    throw new UnsupportedOperationException();
  }

  public void clear() {
    throw new UnsupportedOperationException();
  }

  public double[] coeff() {
    return coefficients;
  }

  public double get(int y, int x) {
    return get(y);
  }

  public int height() {
    return degree();
  }

  public void set(int y, int x, double v) {
    throw new UnsupportedOperationException();
  }

  public int width() {
    return 1;
  }

  public void scale(double s) {
    throw new UnsupportedOperationException();
  }

  // -------------------------------------------------------

  // coefficients of the polynomial, in increasing order of degree
  // (i.e. constant coefficient is first)
  private double[] coefficients;

  // degree of polynomial
  private int degree;

  // largest absolute value of any coefficient
  private double magnitude;
}
