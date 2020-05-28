package de.georgsieber.ballbreak;

public class Ball {
    public int x;
    public int y;
    public int size = 2;
    public byte color_r = (byte)0xff;
    public byte color_g = (byte)0xff;
    public byte color_b = (byte)0xff;

    Ball(int _x, int _y, int _size, byte _r, byte _g, byte _b) {
        x = _x;
        y = _y;
        size = _size;
        color_r = _r;
        color_g = _g;
        color_b = _b;
    }
}
