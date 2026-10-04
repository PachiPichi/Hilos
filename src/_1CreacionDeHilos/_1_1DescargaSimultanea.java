// Creación de hilos que simulan 4 descargas usando Thread

public static void main(String[] args) throws InterruptedException {
    for (int j = 1; j <= 4; j++) {
        Thread hilo = new Thread(() -> {
            try {
                for (int i = 1; i <= 100; i++) {
                    IO.println("Descarga en " + Thread.currentThread().getName() + ": " + i + "%");
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        hilo.start();
    }

}
