package edu.eci.arsw.primefinder;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Control extends Thread {

    private static final int NTHREADS = 3;
    private static final int MAXVALUE = 30000000;
    private static final int TMILISECONDS = 5000;

    private static volatile boolean paused = false;

    private final int NDATA = MAXVALUE / NTHREADS;
    private final PrimeFinderThread[] pft;

    private Control() {
        pft = new PrimeFinderThread[NTHREADS];

        int i;
        for (i = 0; i < NTHREADS - 1; i++) {
            pft[i] = new PrimeFinderThread(i * NDATA, (i + 1) * NDATA);
        }
        pft[i] = new PrimeFinderThread(i * NDATA, MAXVALUE + 1);
    }

    public static Control newControl() {
        return new Control();
    }

    public static boolean isPaused() {
        return paused;
    }

    @Override
    public void run() {

        for (PrimeFinderThread t : pft) {
            t.start();
        }

        try {
            while (true) {
                Thread.sleep(TMILISECONDS);

                synchronized (Control.class) {
                    paused = true;
                }

                int totalPrimes = 0;
                for (PrimeFinderThread t : pft) {
                    totalPrimes += t.getPrimes().size();
                }

                System.out.println("\n⏸ Pausado");
                System.out.println("🔢 Primos encontrados hasta ahora: " + totalPrimes);
                System.out.println("👉 Presione ENTER para continuar...");

                new BufferedReader(new InputStreamReader(System.in)).readLine();

                synchronized (Control.class) {
                    paused = false;
                    Control.class.notifyAll();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
