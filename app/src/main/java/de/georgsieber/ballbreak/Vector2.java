package de.georgsieber.ballbreak;

public class Vector2 {
    public double x = 0;
    public double y = 0;

    Vector2() {
    }
    Vector2(int _x, int _y) {
        x = _x;
        y = _y;
    }

    public double length() {
        return Math.sqrt(Math.pow(x,2) + Math.pow(y,2));
    }

    public void normalize() {
        double l = length();
        x = (x / l);
        y = (y / l);
    }

    public void multiply(double factor) {
        x = (x * factor);
        y = (y * factor);
    }
}
