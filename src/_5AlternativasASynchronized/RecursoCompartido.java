package _5AlternativasASynchronized;


import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class RecursoCompartido {
    private static final ReentrantLock lock = new ReentrantLock();

    public static void utilizarRecurso() {
        boolean conseguido = false;

        while (!conseguido) {
            try {
                conseguido = lock.tryLock(500, TimeUnit.MILLISECONDS);

                if (!conseguido) {
                    System.out.println(
                            Thread.currentThread().getName()
                                    + ": Ocupado, lo intento más tarde"
                    );
                    continue;
                }

                try {
                    System.out.println(
                            Thread.currentThread().getName()
                                    + ": Usando el recurso"
                    );

                    Thread.sleep(2000);

                } finally {
                    lock.unlock();
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Thread hilo1 = new Thread(
                RecursoCompartido::utilizarRecurso, "Hilo-1"
        );

        Thread hilo2 = new Thread(
                RecursoCompartido::utilizarRecurso, "Hilo-2"
        );

        hilo1.start();
        hilo2.start();

        hilo1.join();
        hilo2.join();
    }
}