package problemas;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

public class ParkingInteligente {

    static final int PLAZAS_NORMALES = 20;
    static final int PLAZAS_VIP = 5;
    static final int CAPACIDAD_COLA = 10;
    static final int TOTAL_COCHES = 200;

    static final Semaphore semaforoNormal =
            new Semaphore(PLAZAS_NORMALES, true);

    static final Semaphore semaforoVip =
            new Semaphore(PLAZAS_VIP, true);

    static final Semaphore barreraEntrada =
            new Semaphore(1, true);

    static final Semaphore barreraSalida =
            new Semaphore(1, true);

    static int normalesOcupadas = 0;
    static int vipOcupadas = 0;

    static final ReentrantLock parkingLock =
            new ReentrantLock();

    static final Queue<Coche> colaEspera =
            new ArrayDeque<>();

    static final AtomicInteger vehiculosProcesados =
            new AtomicInteger(0);

    static final AtomicInteger vehiculosAtendidos =
            new AtomicInteger(0);

    static final AtomicInteger vehiculosRechazados =
            new AtomicInteger(0);

    static final AtomicInteger ocupacionMaxima =
            new AtomicInteger(0);

    static final AtomicLong tiempoTotalEstancia =
            new AtomicLong(0);

    static final AtomicLong ingresosCentimos =
            new AtomicLong(0);

    static final ArrayDeque<String> ultimosEventos =
            new ArrayDeque<>();

    static final ReentrantLock eventosLock =
            new ReentrantLock();

    static final int MAX_EVENTOS = 5;

    static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    enum TipoVehiculo {

        NORMAL(1.0),
        VIP(2.0);

        private final double tarifaPorMinuto;

        TipoVehiculo(double tarifaPorMinuto) {
            this.tarifaPorMinuto = tarifaPorMinuto;
        }

        public double getTarifaPorMinuto() {
            return tarifaPorMinuto;
        }
    }

    static class Coche {

        private final int id;
        private final TipoVehiculo tipo;
        private final int tiempoEstancia;
        private String plaza;
        private long momentoEntrada;

        public Coche(
                int id,
                TipoVehiculo tipo,
                int tiempoEstancia) {

            this.id = id;
            this.tipo = tipo;
            this.tiempoEstancia = tiempoEstancia;
        }

        public String getNombre() {

            return String.format(
                    "Coche-%03d",
                    id
            );
        }

        public TipoVehiculo getTipo() {
            return tipo;
        }

        public int getTiempoEstancia() {
            return tiempoEstancia;
        }

        public String getPlaza() {
            return plaza;
        }

        public void setPlaza(String plaza) {
            this.plaza = plaza;
        }

        public long getMomentoEntrada() {
            return momentoEntrada;
        }

        public void setMomentoEntrada(
                long momentoEntrada) {

            this.momentoEntrada =
                    momentoEntrada;
        }
    }

    static String horaActual() {

        return LocalTime.now()
                .format(FORMATO_HORA);
    }

    static void registrarEvento(String evento) {

        eventosLock.lock();

        try {

            ultimosEventos.addLast(
                    "[" +
                            horaActual() +
                            "] " +
                            evento
            );

            while (ultimosEventos.size() >
                    MAX_EVENTOS) {

                ultimosEventos.removeFirst();
            }

        } finally {

            eventosLock.unlock();
        }

        System.out.println(
                "[" +
                        horaActual() +
                        "] " +
                        evento
        );
    }

    static int obtenerOcupacion() {

        parkingLock.lock();

        try {

            return normalesOcupadas +
                    vipOcupadas;

        } finally {

            parkingLock.unlock();
        }
    }

    static void actualizarOcupacionMaxima() {

        int ocupacion =
                obtenerOcupacion();

        int anterior;

        do {

            anterior =
                    ocupacionMaxima.get();

            if (ocupacion <= anterior) {
                return;
            }

        } while (
                !ocupacionMaxima.compareAndSet(
                        anterior,
                        ocupacion
                )
        );
    }

    static boolean intentarEntrada(
            Coche coche) {

        if (coche.getTipo()
                == TipoVehiculo.NORMAL) {

            if (semaforoNormal.availablePermits()
                    == 0) {

                return false;
            }

        } else {

            if (
                    semaforoVip.availablePermits()
                            == 0
                            &&
                            semaforoNormal.availablePermits()
                                    == 0
            ) {

                return false;
            }
        }

        try {

            barreraEntrada.acquire();

            try {

                if (coche.getTipo()
                        == TipoVehiculo.NORMAL) {

                    if (!semaforoNormal
                            .tryAcquire()) {

                        return false;
                    }

                    asignarPlazaNormal(coche);

                } else {

                    if (semaforoVip.tryAcquire()) {

                        asignarPlazaVip(coche);

                    } else if (
                            semaforoNormal.tryAcquire()
                    ) {

                        asignarPlazaNormal(coche);

                    } else {

                        return false;
                    }
                }

                Thread.sleep(2000);

                coche.setMomentoEntrada(
                        System.currentTimeMillis()
                );

                parkingLock.lock();

                try {

                    if (coche.getPlaza()
                            .startsWith("N")) {

                        normalesOcupadas++;

                    } else {

                        vipOcupadas++;
                    }

                } finally {

                    parkingLock.unlock();
                }

                actualizarOcupacionMaxima();

                registrarEvento(
                        "🚗 " +
                                coche.getNombre() +
                                " (" +
                                coche.getTipo() +
                                ") entra - Plaza " +
                                coche.getPlaza()
                );

                return true;

            } finally {

                barreraEntrada.release();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return false;
        }
    }

    static void asignarPlazaNormal(
            Coche coche) {

        parkingLock.lock();

        try {

            for (int i = 1;
                 i <= PLAZAS_NORMALES;
                 i++) {

                String plaza =
                        "N-" + i;

                boolean ocupada =
                        parkingLockPlazaNormal(
                                plaza
                        );

                if (!ocupada) {

                    coche.setPlaza(plaza);

                    plazasNormalesOcupadas.add(
                            plaza
                    );

                    return;
                }
            }

        } finally {

            parkingLock.unlock();
        }
    }

    static void asignarPlazaVip(
            Coche coche) {

        parkingLock.lock();

        try {

            for (int i = 1;
                 i <= PLAZAS_VIP;
                 i++) {

                String plaza =
                        "V-" + i;

                if (!plazasVipOcupadas
                        .contains(plaza)) {

                    coche.setPlaza(plaza);

                    plazasVipOcupadas.add(
                            plaza
                    );

                    return;
                }
            }

        } finally {

            parkingLock.unlock();
        }
    }

    static final java.util.Set<String>
            plazasNormalesOcupadas =
            new java.util.HashSet<>();

    static final java.util.Set<String>
            plazasVipOcupadas =
            new java.util.HashSet<>();

    static boolean parkingLockPlazaNormal(
            String plaza) {

        return plazasNormalesOcupadas
                .contains(plaza);
    }

    static void liberarPlaza(
            Coche coche) {

        parkingLock.lock();

        try {

            if (coche.getPlaza()
                    .startsWith("N")) {

                plazasNormalesOcupadas.remove(
                        coche.getPlaza()
                );

                normalesOcupadas--;

            } else {

                plazasVipOcupadas.remove(
                        coche.getPlaza()
                );

                vipOcupadas--;
            }

        } finally {

            parkingLock.unlock();
        }

        if (coche.getPlaza()
                .startsWith("N")) {

            semaforoNormal.release();

        } else {

            semaforoVip.release();
        }
    }

    static double calcularPrecio(
            Coche coche) {

        double minutos =
                coche.getTiempoEstancia()
                        / 60.0;

        return minutos *
                coche.getTipo()
                        .getTarifaPorMinuto();
    }

    static void procesarSalida(
            Coche coche) {

        try {

            Thread.sleep(
                    coche.getTiempoEstancia()
                            * 1000L
            );

            barreraSalida.acquire();

            try {

                Thread.sleep(1000);

                double precio =
                        calcularPrecio(coche);

                long centimos =
                        Math.round(
                                precio * 100
                        );

                ingresosCentimos
                        .addAndGet(
                                centimos
                        );

                tiempoTotalEstancia
                        .addAndGet(
                                coche.getTiempoEstancia()
                                        * 1000L
                        );

                liberarPlaza(coche);

                vehiculosAtendidos
                        .incrementAndGet();

                registrarEvento(
                        "🚗 " +
                                coche.getNombre() +
                                " (" +
                                coche.getTipo() +
                                ") sale - Plaza " +
                                coche.getPlaza() +
                                " - Pagó: " +
                                String.format(
                                        "%.2f€",
                                        precio
                                )
                );

            } finally {

                barreraSalida.release();
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();
        }
    }

    static void mostrarDashboard() {

        parkingLock.lock();

        try {

            int normalesLibres =
                    PLAZAS_NORMALES -
                            normalesOcupadas;

            int vipLibres =
                    PLAZAS_VIP -
                            vipOcupadas;

            int ocupacion =
                    normalesOcupadas +
                            vipOcupadas;

            int cola =
                    colaEspera.size();

            double ingresos =
                    ingresosCentimos.get()
                            / 100.0;

            System.out.println();
            System.out.println(
                    "=================================================="
            );

            System.out.println(
                    "🅿️  Estado actual: [" +
                            horaActual() +
                            "]"
            );

            System.out.println(
                    "--------------------------------------------------"
            );

            System.out.printf(
                    "│ PLAZAS NORMALES: %2d/%-2d%n",
                    normalesOcupadas,
                    PLAZAS_NORMALES
            );

            System.out.printf(
                    "│ PLAZAS VIP:      %2d/%-2d%n",
                    vipOcupadas,
                    PLAZAS_VIP
            );

            System.out.printf(
                    "│ COLA DE ESPERA:  %2d/%-2d%n",
                    cola,
                    CAPACIDAD_COLA
            );

            System.out.printf(
                    "│ INGRESOS HOY:    %8.2f€%n",
                    ingresos
            );

            System.out.printf(
                    "│ OCUPACIÓN:       %2d/%-2d%n",
                    ocupacion,
                    PLAZAS_NORMALES +
                            PLAZAS_VIP
            );

            System.out.println(
                    "--------------------------------------------------"
            );

            System.out.println(
                    "Últimos eventos:"
            );

            eventosLock.lock();

            try {

                for (String evento :
                        ultimosEventos) {

                    System.out.println(evento);
                }

            } finally {

                eventosLock.unlock();
            }

            System.out.println(
                    "=================================================="
            );

        } finally {

            parkingLock.unlock();
        }
    }

    static Thread iniciarDashboard() {

        Thread dashboard =
                Thread.ofVirtual()
                        .name("Dashboard")
                        .start(() -> {

                            while (
                                    vehiculosProcesados
                                            .get()
                                            < TOTAL_COCHES
                            ) {

                                try {

                                    Thread.sleep(3000);

                                } catch (
                                        InterruptedException e) {

                                    Thread.currentThread()
                                            .interrupt();

                                    break;
                                }

                                mostrarDashboard();
                            }
                        });

        return dashboard;
    }

    public static void main(String[] args)
            throws InterruptedException {

        System.out.println(
                "=== PARKING INTELIGENTE ==="
        );

        System.out.println(
                "Plazas normales: " +
                        PLAZAS_NORMALES
        );

        System.out.println(
                "Plazas VIP: " +
                        PLAZAS_VIP
        );

        System.out.println(
                "Capacidad cola: " +
                        CAPACIDAD_COLA
        );

        System.out.println(
                "Simulando " +
                        TOTAL_COCHES +
                        " coches..."
        );

        System.out.println();

        long inicio =
                System.currentTimeMillis();

        Thread dashboard =
                iniciarDashboard();

        List<Thread> hilosCoches =
                new ArrayList<>();

        for (int i = 1;
             i <= TOTAL_COCHES;
             i++) {

            int id = i;

            Thread hilo =
                    Thread.ofVirtual()
                            .name(
                                    "Coche-" +
                                            String.format(
                                                    "%03d",
                                                    id
                                            )
                            )
                            .start(() -> {

                                TipoVehiculo tipo;

                                if (
                                        ThreadLocalRandom
                                                .current()
                                                .nextInt(100)
                                                < 20
                                ) {

                                    tipo =
                                            TipoVehiculo.VIP;

                                } else {

                                    tipo =
                                            TipoVehiculo.NORMAL;
                                }

                                int estancia =
                                        ThreadLocalRandom
                                                .current()
                                                .nextInt(
                                                        10,
                                                        31
                                                );

                                Coche coche =
                                        new Coche(
                                                id,
                                                tipo,
                                                estancia
                                        );

                                vehiculosProcesados
                                        .incrementAndGet();

                                boolean entro =
                                        intentarEntrada(
                                                coche
                                        );

                                if (!entro) {

                                    boolean enCola = false;

                                    parkingLock.lock();

                                    try {

                                        if (
                                                colaEspera.size()
                                                        < CAPACIDAD_COLA
                                        ) {

                                            colaEspera.add(
                                                    coche
                                            );

                                            enCola = true;

                                            registrarEvento(
                                                    "🚗 " +
                                                            coche.getNombre() +
                                                            " (" +
                                                            coche.getTipo() +
                                                            ") esperando en cola"
                                            );
                                        }

                                    } finally {

                                        parkingLock.unlock();
                                    }

                                    if (enCola) {

                                        while (true) {

                                            try {

                                                Thread.sleep(500);

                                                boolean puedeIntentar =
                                                        false;

                                                parkingLock.lock();

                                                try {

                                                    if (
                                                            colaEspera
                                                                    .peek()
                                                                    == coche
                                                    ) {

                                                        puedeIntentar =
                                                                true;
                                                    }

                                                } finally {

                                                    parkingLock.unlock();
                                                }

                                                if (puedeIntentar) {

                                                    if (
                                                            intentarEntrada(
                                                                    coche
                                                            )
                                                    ) {

                                                        parkingLock
                                                                .lock();

                                                        try {

                                                            colaEspera
                                                                    .poll();

                                                        } finally {

                                                            parkingLock
                                                                    .unlock();
                                                        }

                                                        break;
                                                    }
                                                }

                                            } catch (
                                                    InterruptedException e) {

                                                Thread.currentThread()
                                                        .interrupt();

                                                return;
                                            }
                                        }

                                    } else {

                                        vehiculosRechazados
                                                .incrementAndGet();

                                        registrarEvento(
                                                "❌ " +
                                                        coche.getNombre() +
                                                        " rechazado: parking y cola llenos"
                                        );

                                        return;
                                    }
                                }

                                procesarSalida(coche);
                            });

            hilosCoches.add(hilo);

            Thread.sleep(500);
        }

        for (Thread hilo :
                hilosCoches) {

            hilo.join();
        }

        dashboard.interrupt();

        dashboard.join();

        long fin =
                System.currentTimeMillis();

        long tiempoTotal =
                fin - inicio;

        double promedioEstancia =
                vehiculosAtendidos.get() == 0
                        ? 0
                        :
                        (double) tiempoTotalEstancia.get()
                                /
                                vehiculosAtendidos.get()
                                /
                                1000.0;

        double ingresos =
                ingresosCentimos.get()
                        / 100.0;

        double porcentajeAtendidos =
                (double)
                        vehiculosAtendidos.get()
                        /
                        TOTAL_COCHES
                        *
                        100.0;

        System.out.println();
        System.out.println(
                "--- RESUMEN DEL DÍA ---"
        );

        System.out.println(
                "Vehículos procesados: " +
                        vehiculosProcesados.get()
        );

        System.out.printf(
                "Vehículos atendidos: %d (%.1f%%)%n",
                vehiculosAtendidos.get(),
                porcentajeAtendidos
        );

        System.out.println(
                "Vehículos rechazados: " +
                        vehiculosRechazados.get() +
                        " (parking + cola llenos)"
        );

        System.out.printf(
                "Tiempo promedio de estancia: %.1fs%n",
                promedioEstancia
        );

        System.out.printf(
                "Ingresos totales: %.2f€%n",
                ingresos
        );

        System.out.printf(
                "Ocupación máxima: %d/%d plazas (%.1f%%)%n",
                ocupacionMaxima.get(),
                PLAZAS_NORMALES + PLAZAS_VIP,
                (double) ocupacionMaxima.get()
                        /
                        (PLAZAS_NORMALES + PLAZAS_VIP)
                        * 100.0
        );

        System.out.printf(
                "Tiempo total de simulación: %.1fs%n",
                tiempoTotal / 1000.0
        );
    }
}
