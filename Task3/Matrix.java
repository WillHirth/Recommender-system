
// FOUND IN Matrix.java FILE
import java.util.Random;

// Class to handle vector operations
public class Matrix {
    // Return a random n dimensional vector with values between a and b (where a
    // must be less than b)
    public static double[] randomVector(int n, double a, double b) {
        if (!(a < b))
            throw new IllegalArgumentException("Invalid range: [" + a + ", " + b + ")");
        Random random = new Random();
        double[] x = new double[n];
        for (int i = 0; i < n; i++)
            x[i] = a + random.nextDouble() * (b - a);
        return x;
    }

    // Returns an n dimensional vector filled with 0s
    public static double[] vectorOfZeros(int n) {
        double[] x = new double[n];
        for (int i = 0; i < n; i++)
            x[i] = 0;
        return x;
    }

    // Returns the dot product of x transposed and y
    public static double multiply(double[] x, double[] y) {
        if (x.length != y.length)
            throw new RuntimeException("Vector dimensions do not match");
        double sum = 0.0;
        for (int i = 0; i < x.length; i++)
            sum += x[i] * y[i];
        return sum;
    }

    // Return the addition of x and y
    public static double[] add(double[] x, double[] y) {
        int n = x.length;
        double[] z = new double[n];
        for (int i = 0; i < n; i++)
            z[i] = x[i] + y[i];
        return z;
    }

    // Return the subtraction of y from x
    public static double[] subtract(double[] x, double[] y) {
        int n = x.length;
        double[] z = new double[n];
        for (int i = 0; i < n; i++)
            z[i] = x[i] - y[i];
        return z;
    }

    // Returns the result of multiplying the vector x by the scalar a
    public static double[] multiply(double a, double[] x) {
        int n = x.length;
        double[] y = new double[n];
        for (int i = 0; i < n; i++)
            y[i] += x[i] * a;
        return y;
    }

    // Prints out the vector a in a readable format
    public static void printVector(double[] a) {
        System.out.println("Printing Vector");
        System.out.println("-----------");
        for (double d : a)
            System.out.println(d);
        System.out.println("-----------");
    }
}