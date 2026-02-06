package edu.eci.arsw.primefinder;

import java.util.LinkedList;
import java.util.List;

public class PrimeFinderThread extends Thread {

    private final int a, b;
    private final List<Integer> primes;

    public PrimeFinderThread(int a, int b) {
        this.primes = new LinkedList<>();
        this.a = a;
        this.b = b;
    }

    @Override
    public void run() {
        for (int i = a; i < b; i++) {

            // Punto de sincronización
            synchronized (Control.class) {
                while (Control.isPaused()) {
                    try {
                        Control.class.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }

            if (isPrime(i)) {
                primes.add(i);
            }
        }
    }

    boolean isPrime(int n) {
        if (n <= 1) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }

    public List<Integer> getPrimes() {
        return primes;
    }
}
