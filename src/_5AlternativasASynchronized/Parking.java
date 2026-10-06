package _5AlternativasASynchronized;

import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;

public class Parking {
    public static void main(String[] args) throws InterruptedException {
        Semaphore parking = new Semaphore(3);
        Thread[] coches = new Thread[10];

        for (int i = 0; i < 10; i++) {
            final int id = i + 1;

            coches[i] = new Thread(() -> {
                try {
                    if (!parking.tryAcquire()) {
                        System.out.println(
                                "Coche " + id + " espera: parking lleno"
                        );
                        parking.acquire();
                    }

                    try {
                        System.out.println("Coche " + id + " entra");

                        int tiempo = ThreadLocalRandom.current()
                                .nextInt(1000, 4001);
                        Thread.sleep(tiempo);

                        System.out.println("Coche " + id + " sale");
                    } finally {
                        parking.release();
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            coches[i].start();
        }

        for (Thread coche : coches) {
            coche.join();
        }

        System.out.println("Parking vacío");
    }
}