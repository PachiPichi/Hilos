// Creación de hilos que simulan 10000 descargas usando Thread.ofVirtual()

public static class Descarga implements Runnable {
    @Override
    public void run() {
        try {
            for (int i = 1; i <= 100; i++) {
                IO.println("Descarga en " + Thread.currentThread().getName() + ": " + i + "%");
                Thread.sleep(200);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}


public static void main(String[] args) throws InterruptedException {

    List<Thread> hilos = new ArrayList<>();

    for (int j = 1; j <= 10000; j++) {
        Descarga descarga = new Descarga();
        //Si se usa ofPlatform los hilos se desincronizan, si se usa ofVirtual van a la par
        Thread hilo = Thread.ofVirtual().name("Descarga-" + j).start(descarga);
        hilos.add(hilo);
    }
    for (Thread t : hilos)
        t.join();
}
