package _6ColeccionesSeguras;


import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ProductorConsumidor {

    public static void main(String[] args)
            throws InterruptedException {

        BlockingQueue<Integer> cola =
                new ArrayBlockingQueue<>(5);

        int FIN = -1;

        Thread productor = new Thread(() -> {
            try {
                for (int i = 1; i <= 20; i++) {
                    cola.put(i);
                    System.out.println("Producido: " + i);
                }

                // Señal de finalización
                cola.put(FIN);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        Thread consumidor = new Thread(() -> {
            try {
                while (true) {
                    int numero = cola.take();

                    if (numero == FIN) {
                        break;
                    }

                    System.out.println("Consumido: " + numero);

                    Thread.sleep(200);
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        productor.start();
        consumidor.start();

        productor.join();
        consumidor.join();

        System.out.println("Proceso terminado");
    }
}