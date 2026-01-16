package uk.co.sexeys;

import uk.co.sexeys.*;

import java.awt.*;
import java.util.Random;
import java.util.concurrent.Semaphore;

/**
 * Created by Jim on 12/01/2019.
 *
 */
public class Continuous {
    private Fix currentFix;
    private StreamFrame.StreamPanel display;
    private Semaphore pause = new Semaphore(1);
    private Thread machine = null;
    private long resolutionSecond;
    private Random generator = new Random();
    private double headingVariance;
    Vector2 heading, nextheading;
    long turnTime;

    private DifferentialEvolution DE;
    DifferentialEvolution DE24;

    Continuous(StreamFrame.StreamPanel display,
               DifferentialEvolution DE){
        this.display = display;
        this.DE = DE;

    }

    void newDE(DifferentialEvolution DE) {
        this.DE = DE;
    }


    void startRunning() {
        machine = new Thread(new Machine());
        machine.start();
        pause.release();
    }

    void stop() {
        machine.interrupt();
    }

    void newFix() {
        pause.release();
    }

    boolean running() {
        if(machine == null)
            return false;
        return machine.isAlive();
    }

    double getTWA() {
        double TWA = 0;
         Vector2 tw = workingFix.wind.minus(workingFix.tide);
            double trueWIndSpeed = tw.mag();
            if (trueWIndSpeed != 0) {
                double cosAngle = -tw.dot(heading) / trueWIndSpeed;
                TWA = Math.toDegrees(Math.acos(cosAngle));
            }
        return TWA;
    }

    double getTWS() {
        Vector2 tw = workingFix.wind.minus(workingFix.tide);
        return tw.mag();
    }

    final Fix workingFix = new Fix();

    class Machine implements Runnable {
        @Override
        public void run() {
            long bestTime = Long.MAX_VALUE;
            long currentTime;
            while(true) {
                try {
                    pause.acquire();

                    DE.search(5,Main.continuousFactor,0);
                    currentTime = DE.getTime();
                    if (currentTime < bestTime ) {
                        bestTime= currentTime;
                        System.out.print(" "+currentTime);
                    }
                    display.setUTC(currentTime);
                    display.UpdateGraphics();
                    display.repaint();
                    pause.release();
                    System.out.println();
                } catch (InterruptedException e) {
                    break;
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
            System.out.println("out of while");

        }
    }
}