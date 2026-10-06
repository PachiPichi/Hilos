package _3ExclusionMutuaDeadlock;


class CuentaBancaria {
    int saldo = 0;

    //Da 100 de saldo si está synchronized, si no está da 2 o 3 apenas
    public synchronized void depositar(int cantidad) {
        int saldoActual = saldo;

        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        saldo = saldoActual + cantidad;
    }
}

public class _3_1CuentaBancariaConCondicionDeCarrera {
    public static void main(String[] args) throws InterruptedException {
        CuentaBancaria cb = new CuentaBancaria();
        Thread[] hilos = new Thread[100];

        for (int i = 0; i < 100; i++) {
            hilos[i] = new Thread(() -> cb.depositar(1));
            hilos[i].start();
        }

        for (Thread hilo : hilos) {
            hilo.join();
        }

        IO.println("Saldo actual: " + cb.saldo);
    }
}