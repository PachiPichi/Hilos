package _8SincronizacionDeFases;


import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ProyectoPorFases {

    public static void main(String[] args)
            throws InterruptedException {

        int numeroTrabajadores = 4;
        int numeroFases = 3;

        AtomicInteger faseActual = new AtomicInteger(1);

        CyclicBarrier barrera = new CyclicBarrier(
                numeroTrabajadores,
                () -> {
                    int fase = faseActual.getAndIncrement();

                    System.out.println(
                            "Fase " + fase
                                    + " completada, todos avanzan."
                    );
                }
        );

        Thread[] trabajadores = new Thread[numeroTrabajadores];

        for (int i = 0; i < numeroTrabajadores; i++) {
            final int id = i + 1;

            trabajadores[i] = new Thread(() -> {
                try {
                    for (int fase = 1; fase <= numeroFases; fase++) {

                        System.out.println(
                                "Trabajador " + id
                                        + ": trabajando en fase " + fase
                        );

                        int tiempo = ThreadLocalRandom.current()
                                .nextInt(500, 2001);

                        Thread.sleep(tiempo);

                        System.out.println(
                                "Trabajador " + id
                                        + ": he terminado la fase " + fase
                        );

                        barrera.await();
                    }

                    System.out.println(
                            "Trabajador " + id
                                    + ": proyecto terminado."
                    );

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (BrokenBarrierException e) {
                    System.out.println(
                            "Trabajador " + id
                                    + ": la barrera se ha roto."
                    );
                }
            });

            trabajadores[i].start();
        }

        for (Thread trabajador : trabajadores) {
            trabajador.join();
        }

        System.out.println("Todos los trabajadores han terminado.");
    }
}