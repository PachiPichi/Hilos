package problemas;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class RestauranteConcurrente {


    static final int NUM_CLIENTES = 100;
    static final int NUM_CAMAREROS = 5;
    static final int NUM_COCINEROS = 3;

    static final int CAPACIDAD_MESA = 10;

    static final BlockingQueue<Cliente> clientesEsperando =
            new ArrayBlockingQueue<>(NUM_CLIENTES);


    static final BlockingQueue<Pedido> mesaPedidos =
            new ArrayBlockingQueue<>(CAPACIDAD_MESA);


    static final AtomicInteger clientesAtendidos =
            new AtomicInteger(0);

    static final AtomicInteger platosServidos =
            new AtomicInteger(0);

    static final AtomicLong tiempoTotalEspera =
            new AtomicLong(0);

    static final AtomicInteger vecesMesaLlena =
            new AtomicInteger(0);

    static final AtomicLong tiempoCocinerosEsperando =
            new AtomicLong(0);

    static final AtomicInteger clientesCompletados =
            new AtomicInteger(0);

    static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm:ss");

    enum TipoPlato {

        ENSALADA(2000),
        PASTA(3000),
        PIZZA(4000),
        CARNE(5000);

        private final int tiempoMs;

        TipoPlato(int tiempoMs) {
            this.tiempoMs = tiempoMs;
        }

        public int getTiempoMs() {
            return tiempoMs;
        }
    }

    static class Cliente {

        private final int id;

        public Cliente(int id) {
            this.id = id;
        }

        public int getId() {
            return id;
        }

        public String getNombre() {
            return String.format(
                    "Cliente-%03d",
                    id
            );
        }
    }

    static class Pedido {

        private final Cliente cliente;
        private final TipoPlato plato;

        private final long momentoPedido;

        public Pedido(
                Cliente cliente,
                TipoPlato plato) {

            this.cliente = cliente;
            this.plato = plato;

            this.momentoPedido =
                    System.currentTimeMillis();
        }

        public Cliente getCliente() {
            return cliente;
        }

        public TipoPlato getPlato() {
            return plato;
        }

        public long getMomentoPedido() {
            return momentoPedido;
        }
    }

    static String horaActual() {

        return LocalTime.now()
                .format(FORMATO_HORA);
    }


    static synchronized void mostrarEstadisticas() {

        System.out.println(
                "[ESTADÍSTICAS] " +
                        "Clientes atendidos: " +
                        clientesAtendidos.get() +
                        "/" +
                        NUM_CLIENTES +
                        " | Platos servidos: " +
                        platosServidos.get() +
                        " | Pedidos en mesa: " +
                        mesaPedidos.size() +
                        "/" +
                        CAPACIDAD_MESA
        );
    }


    static void crearClientes()
            throws InterruptedException {

        List<Thread> hilosClientes =
                new ArrayList<>();


        for (int i = 1; i <= NUM_CLIENTES; i++) {

            int id = i;

            Thread cliente =
                    Thread.ofVirtual()
                            .name(
                                    String.format(
                                            "Cliente-%03d",
                                            id
                                    )
                            )
                            .start(() -> {

                                Cliente nuevoCliente =
                                        new Cliente(id);

                                try {


                                    if (id > 1) {

                                        Thread.sleep(500);
                                    }

                                    System.out.printf(
                                            "[%s] %s llega al restaurante%n",
                                            horaActual(),
                                            nuevoCliente.getNombre()
                                    );

                                    clientesEsperando.put(
                                            nuevoCliente
                                    );

                                } catch (
                                        InterruptedException e) {

                                    Thread.currentThread()
                                            .interrupt();
                                }
                            });

            hilosClientes.add(cliente);
        }

        for (Thread hilo : hilosClientes) {

            hilo.join();
        }
    }
    static void iniciarCamareros(
            List<Thread> hilosCamareros) {

        for (int i = 1;
             i <= NUM_CAMAREROS;
             i++) {

            int numeroCamarero = i;

            Thread camarero =
                    Thread.ofPlatform()
                            .name(
                                    "Camarero-" +
                                            numeroCamarero
                            )
                            .start(() -> {

                                while (true) {

                                    if (clientesAtendidos.get()
                                            >= NUM_CLIENTES) {

                                        break;
                                    }

                                    Cliente cliente;

                                    try {

                                        cliente =
                                                clientesEsperando
                                                        .poll();

                                        if (cliente == null) {

                                            if (
                                                    clientesAtendidos
                                                            .get()
                                                            >= NUM_CLIENTES
                                            ) {
                                                break;
                                            }

                                            Thread.sleep(10);

                                            continue;
                                        }

                                        TipoPlato plato =
                                                elegirPlato();

                                        System.out.printf(
                                                "[%s] Camarero-%d toma pedido %s de %s%n",
                                                horaActual(),
                                                numeroCamarero,
                                                plato,
                                                cliente.getNombre()
                                        );

                                        Thread.sleep(1000);

                                        Pedido pedido =
                                                new Pedido(
                                                        cliente,
                                                        plato
                                                );

                                        long inicioEsperaMesa =
                                                System.currentTimeMillis();

                                        if (
                                                mesaPedidos.remainingCapacity()
                                                        == 0
                                        ) {

                                            vecesMesaLlena
                                                    .incrementAndGet();

                                            System.out.printf(
                                                    "[%s] Camarero-%d espera: mesa llena%n",
                                                    horaActual(),
                                                    numeroCamarero
                                            );
                                        }

                                        mesaPedidos.put(pedido);

                                        long finEsperaMesa =
                                                System.currentTimeMillis();

                                        long esperaMesa =
                                                finEsperaMesa -
                                                        inicioEsperaMesa;

                                        clientesAtendidos
                                                .incrementAndGet();

                                        System.out.printf(
                                                "[%s] Camarero-%d deja %s de %s en la mesa%n",
                                                horaActual(),
                                                numeroCamarero,
                                                plato,
                                                cliente.getNombre()
                                        );

                                        mostrarEstadisticas();

                                    } catch (
                                            InterruptedException e) {

                                        Thread.currentThread()
                                                .interrupt();

                                        break;
                                    }
                                }

                                System.out.println(
                                        Thread.currentThread()
                                                .getName() +
                                                " termina su turno."
                                );
                            });

            hilosCamareros.add(camarero);
        }
    }

    static void iniciarCocineros(
            List<Thread> hilosCocineros) {

        for (int i = 1;
             i <= NUM_COCINEROS;
             i++) {

            int numeroCocinero = i;

            Thread cocinero =
                    Thread.ofPlatform()
                            .name(
                                    "Cocinero-" +
                                            numeroCocinero
                            )
                            .start(() -> {

                                while (true) {

                                    if (
                                            clientesAtendidos.get()
                                                    >= NUM_CLIENTES
                                                    &&
                                                    mesaPedidos.isEmpty()
                                    ) {
                                        break;
                                    }

                                    Pedido pedido;

                                    try {

                                        long inicioEspera =
                                                System.currentTimeMillis();

                                        pedido =
                                                mesaPedidos.poll(
                                                        100,
                                                        java.util.concurrent
                                                                .TimeUnit.MILLISECONDS
                                                );

                                        long finEspera =
                                                System.currentTimeMillis();

                                        long tiempoEspera =
                                                finEspera -
                                                        inicioEspera;

                                        if (pedido == null) {

                                            tiempoCocinerosEsperando
                                                    .addAndGet(
                                                            tiempoEspera
                                                    );

                                            continue;
                                        }

                                        long esperaPedido =
                                                System.currentTimeMillis()
                                                        -
                                                        pedido.getMomentoPedido();

                                        tiempoTotalEspera
                                                .addAndGet(
                                                        esperaPedido
                                                );

                                        System.out.printf(
                                                "[%s] Cocinero-%d empieza %s para %s%n",
                                                horaActual(),
                                                numeroCocinero,
                                                pedido.getPlato(),
                                                pedido.getCliente()
                                                        .getNombre()
                                        );

                                        Thread.sleep(
                                                pedido.getPlato()
                                                        .getTiempoMs()
                                        );

                                        platosServidos
                                                .incrementAndGet();

                                        clientesCompletados
                                                .incrementAndGet();

                                        System.out.printf(
                                                "[%s] Cocinero-%d termina %s para %s%n",
                                                horaActual(),
                                                numeroCocinero,
                                                pedido.getPlato(),
                                                pedido.getCliente()
                                                        .getNombre()
                                        );

                                        mostrarEstadisticas();

                                    } catch (
                                            InterruptedException e) {

                                        Thread.currentThread()
                                                .interrupt();

                                        break;
                                    }
                                }

                                System.out.println(
                                        Thread.currentThread()
                                                .getName() +
                                                " termina su turno."
                                );
                            });

            hilosCocineros.add(cocinero);
        }
    }

    static TipoPlato elegirPlato() {

        TipoPlato[] platos =
                TipoPlato.values();

        int posicion =
                ThreadLocalRandom.current()
                        .nextInt(platos.length);

        return platos[posicion];
    }

    public static void main(String[] args)
            throws InterruptedException {

        System.out.println(
                "=== RESTAURANTE CONCURRENTE ==="
        );

        System.out.println(
                "Iniciando servicio con " +
                        NUM_COCINEROS +
                        " cocineros y " +
                        NUM_CAMAREROS +
                        " camareros..."
        );

        System.out.println();

        long inicio =
                System.currentTimeMillis();

        List<Thread> hilosCocineros =
                new ArrayList<>();

        iniciarCocineros(
                hilosCocineros
        );

        List<Thread> hilosCamareros =
                new ArrayList<>();

        iniciarCamareros(
                hilosCamareros
        );

        crearClientes();

        for (Thread hilo : hilosCamareros) {

            hilo.join();
        }

        for (Thread hilo : hilosCocineros) {

            hilo.join();
        }

        long fin =
                System.currentTimeMillis();

        long tiempoTotal =
                fin - inicio;

        System.out.println();
        System.out.println(
                "--- ESTADÍSTICAS FINALES ---"
        );

        System.out.println(
                "Clientes atendidos: " +
                        clientesAtendidos.get() +
                        "/" +
                        NUM_CLIENTES +
                        " " +
                        (
                                clientesAtendidos.get()
                                        == NUM_CLIENTES
                                        ? "✅"
                                        : "❌"
                        )
        );

        System.out.println(
                "Platos servidos: " +
                        platosServidos.get()
        );

        double promedioEspera =
                platosServidos.get() == 0
                        ? 0
                        :
                        (double) tiempoTotalEspera.get()
                                / platosServidos.get()
                                / 1000.0;

        System.out.printf(
                "Tiempo promedio de espera: %.2fs%n",
                promedioEspera
        );

        System.out.println(
                "Mesa llena (veces): " +
                        vecesMesaLlena.get()
        );

        System.out.printf(
                "Cocineros esperando: %.2fs total%n",
                tiempoCocinerosEsperando.get()
                        / 1000.0
        );

        System.out.printf(
                "Tiempo total restaurante: %.2fs%n",
                tiempoTotal / 1000.0
        );

        double eficiencia =
                (double) platosServidos.get()
                        / NUM_CLIENTES
                        * 100.0;

        System.out.printf(
                "Eficiencia: %.1f%%%n",
                eficiencia
        );
    }
}