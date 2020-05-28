package de.georgsieber.ballbreak;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.SENSOR_SERVICE;


public class GameView extends View {
    private int xMax;
    private int yMax;
    private boolean showText = false;
    private int currentFontSize = 10;
    private int maxFontSize = 280;
    private int minFontSize = 20;
    private RectF bounds = new RectF();
    private RectF mouseBounds = new RectF();
    private Paint paint = new Paint();
    private Vector2 mousePosition = new Vector2(0,0);
    private int borderSize = 100;
    private double mouseSize = 14;
    private int circleAmount = -1;
    private boolean running = false;
    private int ballSize = 13;
    private int boxSize = 15;
    private float modX = 0;
    private float modY = 0;
    private final float modFactor = 12;
    private float[] mAccel = new float[3];
    private static final float ALPHA = 0.25f;
    // lower alpha should equal smoother movement

    private List<Ball> balls = new ArrayList<>();
    private List<Particle> particles = new ArrayList<>();
    private List<Box> boxes = new ArrayList<>();

    public GameView(Context context) {
        super(context);
        setupListener();
    }
    public GameView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
        setupListener();
    }
    public GameView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setupListener();
    }

    private void setupListener() {
        this.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
        this.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    mousePosition.x = motionEvent.getX();
                    mousePosition.y = motionEvent.getY();
                }
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN){
                    if(!running) startNewGame();
                }
                return true;
            }
        });

        Sensor rotationVectorSensor = null;
        SensorManager sensorManager = (SensorManager) this.getContext().getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        if (rotationVectorSensor == null) {
            Log.e("sensors", "Sensor not available.");
        } else {
            SensorEventListener sensorListener = new SensorEventListener() {
                @Override
                public void onSensorChanged(SensorEvent sensorEvent) {
                    mAccel = applyLowPassFilter(sensorEvent.values.clone(), mAccel);
                    modX = mAccel[0] * modFactor*(-1);
                    modY = mAccel[1] * modFactor;
                }
                @Override
                public void onAccuracyChanged(Sensor sensor, int i) {
                }
            };
            sensorManager.registerListener(
                    sensorListener,
                    rotationVectorSensor,
                    SensorManager.SENSOR_DELAY_GAME
            );
        }
    }

    private float[] applyLowPassFilter(float[] input, float[] output) {
        if ( output == null ) return input;

        for ( int i=0; i<input.length; i++ ) {
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        }
        return output;
    }

    private RectF getRealPosition(float left, float top, float right, float bottom) {
        return new RectF(left+modX, top+modY, right+modX, bottom+modY);
    }

    // Called back to draw the view. Also called by invalidate().
    @Override
    protected void onDraw(Canvas canvas) {
        // draw items
        paint.setStyle(Paint.Style.FILL);
        for (Box b : boxes) {
            bounds.set(getRealPosition((float)b.x-b.width, (float)b.y-b.height, (float)b.x+b.width, (float)b.y+b.height));
            paint.setARGB( 0xff, b.color_r, b.color_g, b.color_b );
            canvas.drawRect(bounds, paint);
        }

        for (Particle p : particles) {
            bounds.set((float)p.x-p.size, (float)p.y-p.size, (float)p.x+p.size, (float)p.y+p.size);
            paint.setARGB( 0xff, p.color_r, p.color_g, p.color_b );
            canvas.drawOval(bounds, paint);
        }

        for (Ball b : balls) {
            bounds.set((float)b.x-b.size, (float)b.y-b.size, (float)b.x+b.size, (float)b.y+b.size);
            paint.setARGB( 0xff, b.color_r, b.color_g, b.color_b );
            canvas.drawOval(bounds, paint);
        }

        // draw cursor
        mouseBounds.set((float)(mousePosition.x-mouseSize), (float)(mousePosition.y-mouseSize), (float)(mousePosition.x+mouseSize), (float)(mousePosition.y+mouseSize));
        paint.setColor( Color.WHITE );
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth( 5 );
        //canvas.drawOval(mouseBounds, paint);
        canvas.drawLine(
                (mouseBounds.left+(mouseBounds.width()/2)),
                (mouseBounds.top),
                (mouseBounds.left+(mouseBounds.width()/2)),
                (mouseBounds.bottom),
                paint);
        canvas.drawLine(
                (mouseBounds.left),
                (mouseBounds.top+(mouseBounds.height()/2)),
                (mouseBounds.right),
                (mouseBounds.top+(mouseBounds.height()/2)),
                paint);

        paint.setStyle(Paint.Style.FILL);
        if(showText) {
            String text = Integer.toString(circleAmount);
            paint.setTextSize(currentFontSize);
            byte alpha = (byte)(255-((255*currentFontSize)/maxFontSize));
            paint.setARGB( alpha, 255,255,255 );
            Rect measuredSize = new Rect();
            paint.getTextBounds(text,0,text.length(),measuredSize);
            canvas.drawText(text, (xMax>>1)-(measuredSize.width()>>1), (yMax>>1)-(measuredSize.height()>>1), paint);
            currentFontSize += 5;
            if(currentFontSize > maxFontSize) {
                currentFontSize = minFontSize;
                showText = false;
            }
        }
        if(!running && circleAmount > 0) {
            showText = false;

            bounds.set((float)0, (float)0, (float)xMax, (float)yMax);
            paint.setARGB( 150, 255,0,0 );
            canvas.drawRect(bounds, paint);

            String text = "Game Over";
            paint.setTextSize(100);
            paint.setARGB( 255, 255,255,255 );
            Rect measuredSize = new Rect();
            paint.getTextBounds(text,0,text.length(),measuredSize);
            canvas.drawText(text, (xMax>>1)-(measuredSize.width()>>1), (yMax/3)-(measuredSize.height()>>1), paint);

            text = Integer.toString(circleAmount);
            paint.setTextSize(200);
            paint.setARGB( 255, 255,255,255 );
            paint.getTextBounds(text,0,text.length(),measuredSize);
            canvas.drawText(text, (xMax>>1)-(measuredSize.width()>>1), (yMax>>1)-(measuredSize.height()>>1), paint);
        } else if(!running && circleAmount < 0) {
            String text = "Tap to start";
            paint.setTextSize(100);
            paint.setARGB( 255, 255,255,255 );
            Rect measuredSize = new Rect();
            paint.getTextBounds(text,0,text.length(),measuredSize);
            canvas.drawText(text, (xMax>>1)-(measuredSize.width()>>1), (yMax/3)-(measuredSize.height()>>1), paint);
        }

        // update the position of the ball, including collision detection and reaction.
        if(running) update();

        // delay
        try {
            Thread.sleep(30);
        } catch (InterruptedException e) {
            //e.printStackTrace();
        }

        invalidate();  // force re-draw
    }

    // detect collision and update the position of the ball.
    private void update() {
        if(running) {
            List<Particle> removeParticleList = new ArrayList<>();
            for (Particle p : particles) {
                p.x += p.direction.x;
                p.y += p.direction.y;
                if(p.x<0 || p.y<0 || p.x>xMax || p.y>yMax)
                    removeParticleList.add(p);
            }
            for (Particle p : removeParticleList) {
                particles.remove(p);
            }

            List<Ball> removeBallList = new ArrayList<>();
            for(Ball ba : balls) {
                // calc bounds of this ball
                bounds.set((float)ba.x-ba.size, (float)ba.y-ba.size, (float)ba.x+ba.size, (float)ba.y+ba.size);
                // check collision with mouse
                if(bounds.intersect(mouseBounds)) {
                    gameOver();
                    break;
                }
                // check collision with box
                for(Box bo : boxes) {
                    RectF boxBounds = new RectF(getRealPosition((float)bo.x-bo.width, (float)bo.y-bo.height, (float)bo.x+bo.width, (float)bo.y+bo.height));
                    if(bounds.intersect(boxBounds)) {
                        removeBallList.add(ba);
                        for(int i = 0; i<8; i++) {
                            Vector2 direction;
                            do {
                                direction = new Vector2(randInt(-10, 10), randInt(-10, 10));
                            } while(direction.x == 0 || direction.y == 0);
                            particles.add(
                                    new Particle(
                                            ba.x,
                                            ba.y,
                                            (int)Math.ceil(ballSize/3),
                                            direction,
                                            ba.color_r,
                                            ba.color_g,
                                            ba.color_b,
                                            (byte)randInt(175,235)
                                    )
                            );
                        }
                    }
                }

                // move ball
                Vector2 circleToMouse = new Vector2((int)mousePosition.x-ba.x, (int)mousePosition.y-ba.y);
                circleToMouse.multiply(0.065);
                ba.x += circleToMouse.x;
                ba.y += circleToMouse.y;
            }
            for (Ball b : removeBallList) {
                balls.remove(b);
            }
            for(Box bo : boxes) {
                RectF boxBounds = new RectF(getRealPosition((float)bo.x-bo.width, (float)bo.y-bo.height, (float)bo.x+bo.width, (float)bo.y+bo.height));
                // check collision with mouse
                if(boxBounds.intersect(mouseBounds)) {
                    gameOver();
                    break;
                }
            }
        }

        if(balls.size() == 0) {
            circleAmount ++;
            startRound();
        }
    }

    private void startRound() {
        currentFontSize = minFontSize;
        showText = true;

        balls.clear();
        boxes.clear();

        for(int i = 0; i < circleAmount; i++) {
            Vector2 newCirclePosition = new Vector2();
            switch (randInt(1, 4)) {
                case 1:
                    newCirclePosition.x = randInt(0, xMax);
                    break;
                case 2:
                    newCirclePosition.y = randInt(0, yMax);
                    break;
                case 3:
                    newCirclePosition.y = yMax;
                    newCirclePosition.x = randInt(0, xMax);
                    break;
                case 4:
                    newCirclePosition.y = randInt(0, yMax);
                    newCirclePosition.x = xMax;
                    break;
            }
            balls.add(
                    new Ball(
                            (int)newCirclePosition.x,
                            (int)newCirclePosition.y,
                            ballSize,
                            (byte)randInt(30,255),
                            (byte)randInt(30,255),
                            (byte)randInt(30,255)
                    )
            );

            boolean validBoxPosition = false;
            int newBoxX = 0;
            int newBoxY = 0;
            while(!validBoxPosition) {
                newBoxX = randInt(borderSize,xMax-(borderSize*2));
                newBoxY = randInt(borderSize,yMax-(borderSize*2));
                RectF pruefRect = new RectF(newBoxX-(boxSize*3), newBoxY-(boxSize*3), newBoxX+(boxSize*3), newBoxY+(boxSize*3));
                if(!mouseBounds.intersect(pruefRect))
                    validBoxPosition = true;
            }
            boxes.add(
                    new Box(
                            newBoxX,
                            newBoxY,
                            boxSize,
                            (byte)randInt(90,190),
                            (byte)randInt(90,190),
                            (byte)randInt(90,190)
                    )
            );
        }
    }

    private void startNewGame() {
        // adjust ball size to screen size
        ballSize = Math.round(Math.min(xMax,yMax)/83);
        boxSize = Math.round(Math.min(xMax,yMax)/71);
        mouseSize = ballSize;

        circleAmount = 1;
        running = true;
        particles.clear();
        startRound();
        listener.onGameStart();
    }

    private GameStateListener listener = null;
    protected interface GameStateListener {
        // These methods are the different events and
        // need to pass relevant arguments related to the event triggered
        void onGameOver(int points);
        // or when data has been loaded
        void onGameStart();
    }
    protected void setGameStateListener(GameStateListener listener) {
        this.listener = listener;
    }

    private void gameOver() {
        running = false;
        listener.onGameOver(circleAmount);
    }

    public static int randInt(int min, int max) {
        return min + (int)(Math.random() * ((max - min) + 1));
    }

    @Override
    public void onSizeChanged(int w, int h, int oldW, int oldH) {
        // Set the movement bounds for the ball
        xMax = w-1;
        yMax = h-1;
    }
}
