package _5AlternativasASynchronized;

import java.util.concurrent.atomic.AtomicInteger;

public class ContadorVisitas {
    public static void main(String[] args) throws InterruptedException {
        AtomicInteger visitas = new AtomicInteger(0);
        Thread[] hilos = new Thread[1000];

        for (int i = 0; i < 1000; i++) {
            hilos[i] = new Thread(visitas::incrementAndGet);

            hilos[i].start();
        }

        for (Thread hilo : hilos) {
            hilo.join();
        }

        System.out.println("Visitas: " + visitas.get());
    }
}