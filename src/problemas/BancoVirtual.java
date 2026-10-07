package problemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class BancoVirtual {


    static final int CLIENTES = 50;
    static final int OPERACIONES_POR_CLIENTE = 10;
    static final int TOTAL_OPERACIONES = CLIENTES * OPERACIONES_POR_CLIENTE;

    static final double SALDO_INICIAL = 10_000.00;


    enum TipoOperacion {
        RETIRO,
        INGRESO
    }

    // Una operación que realizará un cliente
    static class Operacion {

        private final int id;
        private final TipoOperacion tipo;
        private final double cantidad;

        public Operacion(int id, TipoOperacion tipo, double cantidad) {
            this.id = id;
            this.tipo = tipo;
            this.cantidad = cantidad;
        }

        public int getId() {
            return id;
        }

        public TipoOperacion getTipo() {
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
        long tiempoMs;

        public Resultado(
                double saldoFinal,
                int exitosas,
                int fallidas,
                long tiempoMs) {

            this.saldoFinal = saldoFinal;
            this.exitosas = exitosas;
            this.fallidas = fallidas;
            this.tiempoMs = tiempoMs;
        }
    }


    interface Cuenta {

        boolean retirar(double cantidad);

        void ingresar(double cantidad);

        double consultarSaldo();

        void registrarOperacion(String texto);

        List<String> obtenerHistorial();
    }


    static class CuentaReentrantLock implements Cuenta {

        private double saldo;

        private final ReentrantLock lock = new ReentrantLock();

        private final List<String> historial =
                new ArrayList<>();

        public CuentaReentrantLock(double saldoInicial) {
            this.saldo = saldoInicial;
        }

        @Override
        public boolean retirar(double cantidad) {

            lock.lock();

            try {

                if (saldo >= cantidad) {

                    saldo -= cantidad;

                    registrarOperacion(
                            "RETIRO -" + String.format("%.2f", cantidad)
                    );

                    return true;

                } else {

                    registrarOperacion(
                            "RETIRO FALLIDO -" +
                                    String.format("%.2f", cantidad)
                    );

                    return false;
                }

            } finally {

                lock.unlock();
            }
        }

        @Override
        public void ingresar(double cantidad) {

            lock.lock();

            try {

                saldo += cantidad;

                registrarOperacion(
                        "INGRESO +" +
                                String.format("%.2f", cantidad)
                );

            } finally {

                lock.unlock();
            }
        }

        @Override
        public double consultarSaldo() {

            lock.lock();

            try {
                return saldo;

            } finally {
                lock.unlock();
            }
        }

        @Override
        public void registrarOperacion(String texto) {

            historial.add(texto);
        }

        @Override
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
            this.saldo = saldoInicial;
        }

        @Override
        public synchronized boolean retirar(double cantidad) {

            if (saldo >= cantidad) {

                saldo -= cantidad;

                registrarOperacion(
                        "RETIRO -" +
                                String.format("%.2f", cantidad)
                );

                return true;

            } else {

                registrarOperacion(
                        "RETIRO FALLIDO -" +
                                String.format("%.2f", cantidad)
                );

                return false;
            }
        }

        @Override
        public synchronized void ingresar(double cantidad) {

            saldo += cantidad;

            registrarOperacion(
                    "INGRESO +" +
                            String.format("%.2f", cantidad)
            );
        }

        @Override
        public synchronized double consultarSaldo() {

            return saldo;
        }

        @Override
        public synchronized void registrarOperacion(String texto) {

            historial.add(texto);
        }

        @Override
        public synchronized List<String> obtenerHistorial() {

            return new ArrayList<>(historial);
        }
    }

    static class CuentaVolatile implements Cuenta {

        private volatile double saldo;

        private final ConcurrentLinkedQueue<String> historial =
                new ConcurrentLinkedQueue<>();

        public CuentaVolatile(double saldoInicial) {
            this.saldo = saldoInicial;
        }

        @Override
        public boolean retirar(double cantidad) {



            if (saldo >= cantidad) {

                saldo -= cantidad;

                registrarOperacion(
                        "RETIRO -" +
                                String.format("%.2f", cantidad)
                );

                return true;

            } else {

                registrarOperacion(
                        "RETIRO FALLIDO -" +
                                String.format("%.2f", cantidad)
                );

                return false;
            }
        }

        @Override
        public void ingresar(double cantidad) {



            saldo += cantidad;

            registrarOperacion(
                    "INGRESO +" +
                            String.format("%.2f", cantidad)
            );
        }

        @Override
        public double consultarSaldo() {

            return saldo;
        }

        @Override
        public void registrarOperacion(String texto) {

            historial.add(texto);
        }

        @Override
        public List<String> obtenerHistorial() {

            return new ArrayList<>(historial);
        }
    }


    static List<List<Operacion>> generarOperaciones() {

        Random random = new Random(12345);

        List<List<Operacion>> operacionesClientes =
                new ArrayList<>();

        int idOperacion = 1;

        for (int cliente = 0; cliente < CLIENTES; cliente++) {

            List<Operacion> operaciones =
                    new ArrayList<>();

            for (int i = 0;
                 i < OPERACIONES_POR_CLIENTE;
                 i++) {

                // 60% retiro
                // 40% ingreso

                if (random.nextDouble() < 0.60) {

                    double cantidad =
                            1 + random.nextInt(100);

                    operaciones.add(
                            new Operacion(
                                    idOperacion++,
                                    TipoOperacion.RETIRO,
                                    cantidad
                            )
                    );

                } else {

                    double cantidad =
                            1 + random.nextInt(50);

                    operaciones.add(
                            new Operacion(
                                    idOperacion++,
                                    TipoOperacion.INGRESO,
                                    cantidad
                            )
                    );
                }
            }

            operacionesClientes.add(operaciones);
        }

        return operacionesClientes;
    }


    static Resultado ejecutarClientes(
            Cuenta cuenta,
            List<List<Operacion>> operacionesClientes) {

        AtomicInteger exitosas =
                new AtomicInteger(0);

        AtomicInteger fallidas =
                new AtomicInteger(0);

        List<Thread> hilos =
                new ArrayList<>();

        Random random = new Random();

        long inicio = System.currentTimeMillis();


        for (int i = 0; i < CLIENTES; i++) {

            List<Operacion> operaciones =
                    operacionesClientes.get(i);

            int numeroCliente = i + 1;

            Thread hilo = new Thread(() -> {

                for (Operacion operacion : operaciones) {

                    // Simulamos tiempo de procesamiento
                    try {

                        int espera =
                                100 + random.nextInt(201);

                        Thread.sleep(espera);

                    } catch (InterruptedException e) {

                        Thread.currentThread().interrupt();
                        return;
                    }

                    boolean resultado;

                    if (operacion.getTipo()
                            == TipoOperacion.RETIRO) {

                        resultado =
                                cuenta.retirar(
                                        operacion.getCantidad()
                                );

                    } else {

                        cuenta.ingresar(
                                operacion.getCantidad()
                        );

                        resultado = true;
                    }

                    if (resultado) {

                        exitosas.incrementAndGet();

                    } else {

                        fallidas.incrementAndGet();
                    }
                }

            }, "Cliente-" + numeroCliente);

            hilos.add(hilo);
        }


        for (Thread hilo : hilos) {
            hilo.start();
        }


        for (Thread hilo : hilos) {

            try {

                hilo.join();

            } catch (InterruptedException e) {

                Thread.currentThread().interrupt();
            }
        }

        long fin = System.currentTimeMillis();

        long tiempo =
                fin - inicio;

        return new Resultado(
                cuenta.consultarSaldo(),
                exitosas.get(),
                fallidas.get(),
                tiempo
        );
    }


    static void mostrarResultado(
            String nombre,
            Resultado resultado,
            int operacionesTotales) {

        System.out.println();
        System.out.println(
                "--- " + nombre + " ---"
        );

        System.out.printf(
                "Saldo final: %.2f€%n",
                resultado.saldoFinal
        );

        System.out.println(
                "Operaciones exitosas: " +
                        resultado.exitosas +
                        "/" +
                        operacionesTotales
        );

        System.out.println(
                "Operaciones fallidas: " +
                        resultado.fallidas +
                        " (fondos insuficientes)"
        );

        System.out.printf(
                "Operaciones procesadas: %d/%d%n",
                resultado.exitosas + resultado.fallidas,
                operacionesTotales
        );

        System.out.printf(
                "Tiempo total: %.2fs%n",
                resultado.tiempoMs / 1000.0
        );

        if (resultado.exitosas +
                resultado.fallidas
                == operacionesTotales) {

            System.out.println(
                    "Todas las operaciones fueron procesadas."
            );

        } else {

            System.out.println(
                    " OPERACIONES PERDIDAS"
            );
        }
    }


    public static void main(String[] args) {

        System.out.println(
                "=== BANCO VIRTUAL ==="
        );

        System.out.printf(
                "Saldo inicial: %.2f€%n",
                SALDO_INICIAL
        );

        System.out.println(
                CLIENTES +
                        " clientes realizando " +
                        TOTAL_OPERACIONES +
                        " operaciones totales..."
        );


        List<List<Operacion>> operaciones =
                generarOperaciones();


        CuentaReentrantLock cuentaLock =
                new CuentaReentrantLock(
                        SALDO_INICIAL
                );

        Resultado resultadoLock =
                ejecutarClientes(
                        cuentaLock,
                        operaciones
                );

        mostrarResultado(
                "CON REENTRANTLOCK",
                resultadoLock,
                TOTAL_OPERACIONES
        );

        CuentaSynchronized cuentaSynchronized =
                new CuentaSynchronized(
                        SALDO_INICIAL
                );

        Resultado resultadoSynchronized =
                ejecutarClientes(
                        cuentaSynchronized,
                        operaciones
                );

        mostrarResultado(
                "CON SYNCHRONIZED",
                resultadoSynchronized,
                TOTAL_OPERACIONES
        );

        CuentaVolatile cuentaVolatile =
                new CuentaVolatile(
                        SALDO_INICIAL
                );

        Resultado resultadoVolatile =
                ejecutarClientes(
                        cuentaVolatile,
                        operaciones
                );

        mostrarResultado(
                "CON VOLATILE",
                resultadoVolatile,
                TOTAL_OPERACIONES
        );

        System.out.println();
        System.out.println(
                "=== COMPARACIÓN ==="
        );

        System.out.printf(
                "ReentrantLock : %.2f€%n",
                resultadoLock.saldoFinal
        );

        System.out.printf(
                "Synchronized  : %.2f€%n",
                resultadoSynchronized.saldoFinal
        );

        System.out.printf(
                "Volatile      : %.2f€%n",
                resultadoVolatile.saldoFinal
        );

        System.out.println();

        if (Math.abs(
                resultadoLock.saldoFinal -
                        resultadoSynchronized.saldoFinal
        ) < 0.001) {

            System.out.println(
                    "ReentrantLock y synchronized producen " +
                            "el mismo saldo."
            );

        } else {

            System.out.println(
                    "ReentrantLock y synchronized han tenido " +
                            "un orden de ejecución diferente."
            );
        }

        if (resultadoVolatile.saldoFinal !=
                resultadoLock.saldoFinal) {

            System.out.println(
                    "VOLATILE: el resultado puede ser incorrecto " +
                            "por condiciones de carrera."
            );
        }
    }
}