package de.georgsieber.ballbreak;

public class Particle {
    public int x;
    public int y;
    public int size = 2;
    public Vector2 direction = new Vector2();
    public byte color_r = (byte)0xff;
    public byte color_g = (byte)0xff;
    public byte color_b = (byte)0xff;
    public byte color_a = (byte)0xff;

    Particle(int _x, int _y, int _size, Vector2 _d, byte _r, byte _g, byte _b, byte _a) {
        x = _x;
        y = _y;
        size = _size;
        direction = _d;
        color_r = _r;
        color_g = _g;
        color_b = _b;
        color_a = _a;
    }
}
