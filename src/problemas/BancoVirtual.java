package problemas;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.locks.ReentrantLock;

public class BancoVirtual {

    private static final int SALDO_INICIAL = 10_000;
    private static final int NUM_CLIENTES = 50;
    private static final int OPERACIONES_POR_CLIENTE = 10;



    interface Cuenta {

        boolean retirar(double cantidad);

        void ingresar(double cantidad);

        double consultarSaldo();

        List<String> obtenerHistorial();
    }

    static class Operacion {
        enum Tipo{
            RETIRO,
            INGRESO
        }

        private final Tipo tipo;
        private final double cantidad;

        private Operacion(Tipo tipo, double cantidad) {
            this.tipo = tipo;
            this.cantidad = cantidad;
        }

        public Tipo getTipo() {
            return tipo;
        }
        public double getCantidad() {
            return cantidad;
        }
    }

    static class Resultado {

        double saldoFinal;
        int exitosas;
        int fallidas;
        long tiempo;

        public Resultado(
                double saldoFinal,
                int exitosas,
                int fallidas,
                long tiempo) {

            this.saldoFinal = saldoFinal;
            this.exitosas = exitosas;
            this.fallidas = fallidas;
            this.tiempo = tiempo;
        }
    }

    static class CuentaLock implements Cuenta {

        private double saldo;

        private final ReentrantLock lock =
                new ReentrantLock();

        private final List<String> historial =
                new ArrayList<>();

        public CuentaLock(double saldoInicial) {
            saldo = saldoInicial;
        }

        public boolean retirar(double cantidad) {

            lock.lock();

            try {

                if (saldo >= cantidad) {

                    saldo -= cantidad;

                    registrar(
                            "RETIRO -" + cantidad
                    );

                    return true;
                }

                registrar(
                        "RETIRO FALLIDO -" + cantidad
                );

                return false;

            } finally {
                lock.unlock();
            }
        }

        public void ingresar(double cantidad) {

            lock.lock();

            try {

                saldo += cantidad;

                registrar(
                        "INGRESO +" + cantidad
                );

            } finally {
                lock.unlock();
            }
        }

        public double consultarSaldo() {

            lock.lock();

            try {
                return saldo;
            } finally {
                lock.unlock();
            }
        }

        private void registrar(String mensaje) {

            historial.add(
                    System.currentTimeMillis()
                            + " | "
                            + mensaje
            );
        }

        public List<String> obtenerHistorial() {

            lock.lock();

            try {
                return new ArrayList<>(historial);
            } finally {
                lock.unlock();
            }
        }
    }



    static class CuentaSynchronized implements Cuenta {

        private double saldo;

        private final List<String> historial =
                new ArrayList<>();

        public CuentaSynchronized(double saldoInicial) {
            saldo = saldoInicial;
        }

        public synchronized boolean retirar(double cantidad) {

            if (saldo >= cantidad) {

                saldo -= cantidad;

                registrar(
                        "RETIRO -" + cantidad
                );

                return true;
            }

            registrar(
                    "RETIRO FALLIDO -" + cantidad
            );

            return false;
        }

        public synchronized void ingresar(double cantidad) {

            saldo += cantidad;

            registrar(
                    "INGRESO +" + cantidad
            );
        }

        public synchronized double consultarSaldo() {
            return saldo;
        }

        private void registrar(String mensaje) {

            historial.add(
                    System.currentTimeMillis()
                            + " | "
                            + mensaje
            );
        }

        public synchronized List<String> obtenerHistorial() {

            return new ArrayList<>(historial);
        }
    }


    static class CuentaVolatile implements Cuenta {

        private volatile double saldo;

        private final List<String> historial =
                new ArrayList<>();

        public CuentaVolatile(double saldoInicial) {
            saldo = saldoInicial;
        }

        public boolean retirar(double cantidad) {

            if (saldo >= cantidad) {

                saldo -= cantidad;

                registrar(
                        "RETIRO -" + cantidad
                );

                return true;
            }

            registrar(
                    "RETIRO FALLIDO -" + cantidad
            );

            return false;
        }

        public void ingresar(double cantidad) {

            saldo += cantidad;

            registrar(
                    "INGRESO +" + cantidad
            );
        }

        public double consultarSaldo() {
            return saldo;
        }

        private void registrar(String mensaje) {

            historial.add(
                    System.currentTimeMillis()
                            + " | "
                            + mensaje
            );
        }

        public List<String> obtenerHistorial() {
            return new ArrayList<>(historial);
        }
    }

    private static List<Operacion> generarOperaciones() {

        List<Operacion> operaciones = new ArrayList<>();

        for (int i = 0; i < 500; i++) {

            double probabilidad =
                    ThreadLocalRandom.current().nextDouble();

            if (probabilidad < 0.60) {

                double cantidad =
                        ThreadLocalRandom.current()
                                .nextInt(1, 101);

                operaciones.add(
                        new Operacion(
                                Operacion.Tipo.RETIRO,
                                cantidad
                        )
                );

            } else {

                double cantidad =
                        ThreadLocalRandom.current()
                                .nextInt(1, 51);

                operaciones.add(
                        new Operacion(
                                Operacion.Tipo.INGRESO,
                                cantidad
                        )
                );
            }
        }

        return operaciones;
    }

    private static void ejecutarClientes(
            Cuenta cuenta)
            throws InterruptedException {

        Thread[] clientes =
                new Thread[NUM_CLIENTES];

        for (int i = 0; i < NUM_CLIENTES; i++) {

            clientes[i] = new Thread(() -> {

                for (int j = 0;
                     j < OPERACIONES_POR_CLIENTE;
                     j++) {

                    try {

                        int tiempo =
                                ThreadLocalRandom.current()
                                        .nextInt(100, 301);

                        Thread.sleep(tiempo);

                        double probabilidad =
                                ThreadLocalRandom.current()
                                        .nextDouble();

                        if (probabilidad < 0.60) {

                            double cantidad =
                                    ThreadLocalRandom.current()
                                            .nextInt(1, 101);

                            cuenta.retirar(cantidad);

                        } else {

                            double cantidad =
                                    ThreadLocalRandom.current()
                                            .nextInt(1, 51);

                            cuenta.ingresar(cantidad);
                        }

                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            });

            clientes[i].start();
        }

        for (Thread cliente : clientes) {
            cliente.join();
        }
    }



    public static void main(String[] args)
            throws InterruptedException {

        System.out.println(
                "=== BANCO VIRTUAL ==="
        );

        System.out.println(
                "Saldo inicial: 10000.00€"
        );

        System.out.println(
                "50 clientes realizando "
                        + "500 operaciones totales..."
        );


        CuentaLock cuentaLock =
                new CuentaLock(SALDO_INICIAL);

        long inicioLock =
                System.currentTimeMillis();

        ejecutarClientes(cuentaLock);

        long finLock =
                System.currentTimeMillis();

        System.out.println();
        System.out.println(
                "--- CON REENTRANTLOCK ---"
        );

        System.out.printf(
                "Saldo final: %.2f€%n",
                cuentaLock.consultarSaldo()
        );

        System.out.println(
                "Tiempo total: "
                        + (finLock - inicioLock)
                        + "ms"
        );


        CuentaSynchronized cuentaSync =
                new CuentaSynchronized(SALDO_INICIAL);

        long inicioSync =
                System.currentTimeMillis();

        ejecutarClientes(cuentaSync);

        long finSync =
                System.currentTimeMillis();

        System.out.println();
        System.out.println(
                "--- CON SYNCHRONIZED ---"
        );

        System.out.printf(
                "Saldo final: %.2f€%n",
                cuentaSync.consultarSaldo()
        );

        System.out.println(
                "Tiempo total: "
                        + (finSync - inicioSync)
                        + "ms"
        );



        CuentaVolatile cuentaVolatile =
                new CuentaVolatile(SALDO_INICIAL);

        long inicioVolatile =
                System.currentTimeMillis();

        ejecutarClientes(cuentaVolatile);

        long finVolatile =
                System.currentTimeMillis();

        System.out.println();
        System.out.println(
                "--- CON VOLATILE ---"
        );

        System.out.printf(
                "Saldo final: %.2f€%n",
                cuentaVolatile.consultarSaldo()
        );

        System.out.println(
                "Tiempo total: "
                        + (finVolatile - inicioVolatile)
                        + "ms"
        );
    }
}