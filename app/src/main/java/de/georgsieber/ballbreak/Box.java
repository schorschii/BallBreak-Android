package de.georgsieber.ballbreak;

public class Box {
    public int x;
    public int y;
    public int width = 4;
    public int height = 4;
    public byte color_r = (byte)0xff;
    public byte color_g = (byte)0xff;
    public byte color_b = (byte)0xff;

    Box(int _x, int _y, int _size, byte _r, byte _g, byte _b) {
        x = _x;
        y = _y;
        width = _size;
        height = _size;
        color_r = _r;
        color_g = _g;
        color_b = _b;
    }
}
