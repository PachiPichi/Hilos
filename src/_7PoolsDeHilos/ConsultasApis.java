package _7PoolsDeHilos;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

public class ConsultasApis {

    public static void main(String[] args)
            throws InterruptedException, ExecutionException {

        List<Callable<String>> tareas = new ArrayList<>();

        for (int i = 1; i <= 5; i++) {
            final int id = i;

            Callable<String> tarea = () -> {
                int segundos =
                        ThreadLocalRandom.current().nextInt(1, 4);

                System.out.println(
                        "API " + id + ": iniciando consulta..."
                );

                Thread.sleep(segundos * 1000L);

                return "API " + id
                        + ": respuesta recibida en "
                        + segundos + " segundos";
            };

            tareas.add(tarea);
        }

        try (ExecutorService executor =
                     Executors.newVirtualThreadPerTaskExecutor()) {

            List<Future<String>> futuros = executor.invokeAll(tareas);

            for (Future<String> futuro : futuros) {
                System.out.println(futuro.get());
            }
        }
    }
}
