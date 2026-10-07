package _8SincronizacionDeFases;


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;

public class SalidaCarrera {

    public static void main(String[] args)
            throws InterruptedException {

        CountDownLatch corredoresListos = new CountDownLatch(5);
        CountDownLatch salida = new CountDownLatch(1);

        Thread[] corredores = new Thread[5];

        for (int i = 0; i < 5; i++) {
            final int dorsal = i + 1;

            corredores[i] = new Thread(() -> {
                try {
                    System.out.println(
                            "Corredor " + dorsal + ": calentando..."
                    );

                    int tiempo = ThreadLocalRandom.current()
                            .nextInt(1000, 3001);

                    Thread.sleep(tiempo);

                    System.out.println(
                            "Corredor " + dorsal + ": ¡estoy listo!"
                    );

                    corredoresListos.countDown();

                    salida.await();

                    System.out.println(
                            "Corredor " + dorsal + ": ¡SALGO CORRIENDO!"
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            corredores[i].start();
        }

        Thread juez = new Thread(() -> {
            try {
                corredoresListos.await();

                System.out.println(
                        "JUEZ: ¡Todos preparados! ¡A la de tres...!"
                );

                Thread.sleep(1000);

                System.out.println("JUEZ: ¡YA!");

                salida.countDown();

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        juez.start();

        juez.join();

        for (Thread corredor : corredores) {
            corredor.join();
        }

        System.out.println("La carrera ha comenzado para todos.");
    }
}