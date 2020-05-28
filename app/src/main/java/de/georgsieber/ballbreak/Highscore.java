package de.georgsieber.ballbreak;

public class Highscore {
    public String date;
    public String name;
    public int points;
    Highscore(String _name, String _date, int _points) {
        date = _date;
        name = _name;
        points = _points;
    }
}
