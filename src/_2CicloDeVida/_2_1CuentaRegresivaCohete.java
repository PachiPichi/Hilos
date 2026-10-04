package _2CicloDeVida;

public class _2_1CuentaRegresivaCohete {
    public static void main(String[] args) throws InterruptedException {

        Thread hilo = new Thread(() -> {
            for (int i = 10; i > 0; i--) {
                System.out.println("Cuenta regresiva: " + i);

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        });

        hilo.start();

        hilo.join();

        System.out.println("¡Despegue!");
    }
}
