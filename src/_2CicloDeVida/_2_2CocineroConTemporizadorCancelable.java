package _2CicloDeVida;

public class _2_2CocineroConTemporizadorCancelable {
    public static void main(String[] args) throws InterruptedException {

        Thread hilo = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                System.out.println("Cocinando...");

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            System.out.println("Cocción cancelada");
        });

        IO.println("Estado actual: " + hilo.getState());

        hilo.start();

        IO.println("Estado actual: " + hilo.getState());

        Thread.sleep(3000);
        hilo.interrupt();

        IO.println("Estado actual: " + hilo.getState());

        Thread.sleep(1000);
        IO.println("Estado actual: " + hilo.getState());
    }
}
