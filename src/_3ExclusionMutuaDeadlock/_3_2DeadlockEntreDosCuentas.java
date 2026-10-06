package _3ExclusionMutuaDeadlock;

import java.util.concurrent.atomic.AtomicInteger;

class CuentaBancariaDeadlock {
    private static final AtomicInteger SIGUIENTE_ID =
            new AtomicInteger(0);

    private final int id = SIGUIENTE_ID.incrementAndGet();

    int saldo;
    final String nombre;

    CuentaBancariaDeadlock(String nombre, int saldo) {
        this.nombre = nombre;
        this.saldo = saldo;
    }

    public void transferir(CuentaBancariaDeadlock destino, int cantidad) {
        CuentaBancariaDeadlock primera = this.id < destino.id ? this : destino;
        CuentaBancariaDeadlock segunda = this.id < destino.id ? destino : this;

        synchronized (primera) {
            synchronized (segunda) {
                if (saldo < cantidad) {
                    throw new IllegalArgumentException(
                            "Saldo insuficiente en " + nombre
                    );
                }

                saldo -= cantidad;
                destino.saldo += cantidad;

                System.out.println(
                        Thread.currentThread().getName()
                                + ": transferencia de " + cantidad
                                + " de " + nombre + " a " + destino.nombre
                );
            }
        }
    }
}

public class _3_2DeadlockEntreDosCuentas {
    public static void main(String[] args) throws InterruptedException {
        CuentaBancariaDeadlock a = new CuentaBancariaDeadlock("A", 1000);
        CuentaBancariaDeadlock b = new CuentaBancariaDeadlock("B", 1000);

        Thread hilo1 = new Thread(
                () -> a.transferir(b, 100), "Hilo-1"
        );

        Thread hilo2 = new Thread(
                () -> b.transferir(a, 200), "Hilo-2"
        );

        hilo1.start();
        hilo2.start();

        hilo1.join();
        hilo2.join();

        System.out.println("Saldo A: " + a.saldo);
        System.out.println("Saldo B: " + b.saldo);
    }
}