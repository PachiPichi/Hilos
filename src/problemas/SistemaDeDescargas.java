package problemas;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class SistemaDeDescargas {

    private static final int NUM_CLIENTES = 50;
    private static final int NUM_ARCHIVOS = 100;
    private static final int MAX_DESCARGAS_SIMULTANEAS = 5;
    private static final int MAX_REINTENTOS = 3;

    private static final double FACTOR_TIEMPO = 0.1;

    private static final Random random = new Random();

    private static final ExecutorService servidor =
            Executors.newFixedThreadPool(MAX_DESCARGAS_SIMULTANEAS);

    private static final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    private static final Semaphore conexiones =
            new Semaphore(MAX_DESCARGAS_SIMULTANEAS);

    private static final List<Descarga> descargasActivas =
            new CopyOnWriteArrayList<>();

    private static final AtomicInteger completadas =
            new AtomicInteger();

    private static final AtomicInteger fallidas =
            new AtomicInteger();

    private static final AtomicInteger reintentos =
            new AtomicInteger();

    private static final AtomicLong datosTransferidos =
            new AtomicLong();

    public static void main(String[] args) throws InterruptedException {

        long inicio = System.currentTimeMillis();

        List<Archivo> archivos = crearArchivos();

        scheduler.scheduleAtFixedRate(
                SistemaDeDescargas::mostrarDashboard,
                0,
                1,
                TimeUnit.SECONDS
        );

        List<Thread> clientes = new ArrayList<>();

        for (int i = 1; i <= NUM_CLIENTES; i++) {

            int numeroCliente = i;

            Thread cliente = Thread.ofVirtual().start(() -> {

                Archivo archivo =
                        archivos.get(random.nextInt(archivos.size()));

                Cliente usuario =
                        new Cliente(
                                "Usuario-" + numeroCliente,
                                archivo
                        );

                procesarDescarga(usuario);
            });

            clientes.add(cliente);
        }

        for (Thread cliente : clientes) {
            cliente.join();
        }

        servidor.shutdown();
        servidor.awaitTermination(1, TimeUnit.HOURS);

        scheduler.shutdown();

        long tiempoTotal =
                System.currentTimeMillis() - inicio;

        mostrarEstadisticasFinales(tiempoTotal);
    }

    private static void procesarDescarga(Cliente cliente) {

        Descarga descarga =
                new Descarga(cliente);

        for (int intento = 1; intento <= MAX_REINTENTOS; intento++) {

            try {

                CompletableFuture<Void> futuro =
                        CompletableFuture.runAsync(
                                () -> realizarDescarga(descarga),
                                servidor
                        );

                futuro.join();

                completadas.incrementAndGet();

                System.out.println(
                        cliente.getNombre()
                                + " completó: "
                                + cliente.getArchivo().getNombre()
                );

                return;

            } catch (Exception e) {

                reintentos.incrementAndGet();

                if (intento < MAX_REINTENTOS) {

                    System.out.println(
                            cliente.getNombre()
                                    + " reintentando ("
                                    + (intento + 1)
                                    + "/"
                                    + MAX_REINTENTOS
                                    + "): "
                                    + cliente.getArchivo().getNombre()
                    );

                } else {

                    fallidas.incrementAndGet();

                    System.out.println(
                            cliente.getNombre()
                                    + " falló definitivamente: "
                                    + cliente.getArchivo().getNombre()
                    );
                }
            }
        }
    }

    private static void realizarDescarga(Descarga descarga) {

        Cliente cliente = descarga.getCliente();
        Archivo archivo = cliente.getArchivo();

        try {

            conexiones.acquire();

            descargasActivas.add(descarga);

            System.out.println(
                    cliente.getNombre()
                            + " inicia: "
                            + archivo.getNombre()
            );

            int pasos = 20;

            long tiempoTotal =
                    (long) (
                            archivo.getTiempoDescargaMs()
                                    * FACTOR_TIEMPO
                    );

            long tiempoPorPaso =
                    tiempoTotal / pasos;

            for (int i = 1; i <= pasos; i++) {

                Thread.sleep(tiempoPorPaso);

                int progreso =
                        i * 100 / pasos;

                descarga.setProgreso(progreso);

                datosTransferidos.addAndGet(
                        archivo.getSizeMB() / pasos
                );
            }

            if (random.nextInt(100) < 5) {
                throw new RuntimeException(
                        "Error simulado de red"
                );
            }

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Descarga interrumpida",
                    e
            );

        } finally {

            descargasActivas.remove(descarga);

            conexiones.release();
        }
    }

    private static void mostrarDashboard() {

        System.out.println();
        System.out.println(
                "=== GESTOR DE DESCARGAS ==="
        );

        System.out.println(
                "Servidor: "
                        + descargasActivas.size()
                        + "/"
                        + MAX_DESCARGAS_SIMULTANEAS
                        + " conexiones activas"
        );

        System.out.println();
        System.out.println("Descargas activas:");

        for (Descarga descarga : descargasActivas) {

            System.out.printf(
                    "%-12s | %-20s | %3d%%%n",
                    descarga.getCliente().getNombre(),
                    descarga.getCliente()
                            .getArchivo()
                            .getNombre(),
                    descarga.getProgreso()
            );
        }

        System.out.println();

        System.out.println(
                "❌ Fallos: "
                        + fallidas.get()
                        + " | ✅ Completadas: "
                        + completadas.get()
        );

        System.out.println();
    }

    private static void mostrarEstadisticasFinales(
            long tiempoTotal
    ) {

        System.out.println();
        System.out.println(
                "--- ESTADÍSTICAS DEL SERVIDOR ---"
        );

        System.out.println(
                "Tiempo total funcionamiento: "
                        + tiempoTotal
                        + " ms"
        );

        System.out.println(
                "Archivos descargados: "
                        + NUM_CLIENTES
        );

        System.out.println(
                "Descargas exitosas: "
                        + completadas.get()
        );

        System.out.println(
                "Descargas fallidas: "
                        + fallidas.get()
        );

        System.out.println(
                "Reintentos realizados: "
                        + reintentos.get()
        );

        System.out.println(
                "Datos transferidos: "
                        + datosTransferidos.get()
                        + " MB"
        );

        if (tiempoTotal > 0) {

            double segundos =
                    tiempoTotal / 1000.0;

            double velocidad =
                    datosTransferidos.get()
                            / segundos;

            System.out.printf(
                    "Velocidad promedio: %.2f MB/s%n",
                    velocidad
            );
        }
    }

    private static List<Archivo> crearArchivos() {

        List<Archivo> archivos =
                new ArrayList<>();

        for (int i = 1; i <= NUM_ARCHIVOS; i++) {

            TipoArchivo tipo =
                    TipoArchivo.values()[
                            random.nextInt(
                                    TipoArchivo.values().length
                            )
                            ];

            String extension =
                    switch (tipo) {
                        case DOCUMENTO -> ".pdf";
                        case IMAGEN -> ".jpg";
                        case VIDEO -> ".mp4";
                        case JUEGO -> ".zip";
                    };

            archivos.add(
                    new Archivo(
                            "archivo_" + i + extension,
                            tipo
                    )
            );
        }

        return archivos;
    }
}

enum TipoArchivo {

    DOCUMENTO(10),
    IMAGEN(50),
    VIDEO(500),
    JUEGO(2000);

    private final int sizeMB;

    TipoArchivo(int sizeMB) {
        this.sizeMB = sizeMB;
    }

    public int getSizeMB() {
        return sizeMB;
    }

    public long getTiempoDescargaMs() {
        return sizeMB * 100L;
    }
}

class Archivo {

    private final String nombre;
    private final TipoArchivo tipo;

    public Archivo(String nombre, TipoArchivo tipo) {
        this.nombre = nombre;
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public TipoArchivo getTipo() {
        return tipo;
    }

    public int getSizeMB() {
        return tipo.getSizeMB();
    }

    public long getTiempoDescargaMs() {
        return tipo.getTiempoDescargaMs();
    }
}

class Cliente {

    private final String nombre;
    private final Archivo archivo;

    public Cliente(String nombre, Archivo archivo) {
        this.nombre = nombre;
        this.archivo = archivo;
    }

    public String getNombre() {
        return nombre;
    }

    public Archivo getArchivo() {
        return archivo;
    }
}

class Descarga {

    private final Cliente cliente;

    private volatile int progreso;

    public Descarga(Cliente cliente) {
        this.cliente = cliente;
        this.progreso = 0;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public int getProgreso() {
        return progreso;
    }

    public void setProgreso(int progreso) {
        this.progreso = progreso;
    }
}