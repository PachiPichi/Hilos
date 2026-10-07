package problemas;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

public class ContadorVisitasWeb {

    static final int NUM_VISITANTES = 1000;

    // 1. SIN SINCRONIZACIÓN

    static class ContadorSinSincronizacion {

        private int visitas = 0;

        public void incrementarVisita() {
            visitas++;
        }

        public int getVisitas() {
            return visitas;
        }
    }

    // 2. CON SYNCHRONIZED

    static class ContadorSynchronized {

        private int visitas = 0;

        public synchronized void incrementarVisita() {
            visitas++;
        }

        public int getVisitas() {
            return visitas;
        }
    }

    // 3. CON ATOMICINTEGER

    static class ContadorAtomic {

        private final AtomicInteger visitas =
                new AtomicInteger(0);

        public void incrementarVisita() {
            visitas.incrementAndGet();
        }

        public int getVisitas() {
            return visitas.get();
        }
    }

    public static Thread[] crearVisitantes(Runnable tarea) {

        Thread[] hilos = new Thread[NUM_VISITANTES];

        for (int i = 0; i < NUM_VISITANTES; i++) {

            hilos[i] = new Thread(() -> {

                try {
                    int tiempo = ThreadLocalRandom
                            .current()
                            .nextInt(50, 151);

                    Thread.sleep(tiempo);

                    tarea.run();

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });

            hilos[i].start();
        }

        return hilos;
    }

    public static void esperarHilos(Thread[] hilos)
            throws InterruptedException {

        for (Thread hilo : hilos) {
            hilo.join();
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        System.out.println("=== CONTADOR DE VISITAS WEB ===");
        System.out.println("Esperando 1000 visitantes...");

        ContadorSinSincronizacion contador1 =
                new ContadorSinSincronizacion();

        long inicio1 = System.currentTimeMillis();

        Thread[] hilos1 = crearVisitantes(
                contador1::incrementarVisita
        );

        esperarHilos(hilos1);

        long fin1 = System.currentTimeMillis();

        System.out.println();
        System.out.println("--- SIN SINCRONIZACIÓN ---");
        System.out.println(
                "Visitas esperadas: " + NUM_VISITANTES
        );
        System.out.println(
                "Visitas contadas: " + contador1.getVisitas()
        );
        System.out.println(
                "Tiempo: " + (fin1 - inicio1) + "ms"
        );


        ContadorSynchronized contador2 =
                new ContadorSynchronized();

        long inicio2 = System.currentTimeMillis();

        Thread[] hilos2 = crearVisitantes(
                contador2::incrementarVisita
        );

        esperarHilos(hilos2);

        long fin2 = System.currentTimeMillis();

        System.out.println();
        System.out.println("--- CON SYNCHRONIZED ---");
        System.out.println(
                "Visitas esperadas: " + NUM_VISITANTES
        );
        System.out.println(
                "Visitas contadas: " + contador2.getVisitas()
        );
        System.out.println(
                "Tiempo: " + (fin2 - inicio2) + "ms"
        );


        ContadorAtomic contador3 =
                new ContadorAtomic();

        long inicio3 = System.currentTimeMillis();

        Thread[] hilos3 = crearVisitantes(
                contador3::incrementarVisita
        );

        esperarHilos(hilos3);

        long fin3 = System.currentTimeMillis();

        System.out.println();
        System.out.println("--- CON ATOMICINTEGER ---");
        System.out.println(
                "Visitas esperadas: " + NUM_VISITANTES
        );
        System.out.println(
                "Visitas contadas: " + contador3.getVisitas()
        );
        System.out.println(
                "Tiempo: " + (fin3 - inicio3) + "ms"
        );
    }
}
