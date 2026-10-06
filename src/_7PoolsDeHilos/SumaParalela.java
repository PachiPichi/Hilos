package _7PoolsDeHilos;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SumaParalela {

    public static void main(String[] args)
            throws InterruptedException, java.util.concurrent.ExecutionException {

        int[] numeros = new int[10_000_000];

        for (int i = 0; i < numeros.length; i++) {
            numeros[i] = (i % 10) + 1;
        }

        int numeroTareas = 4;

        try (ExecutorService executor =
                     Executors.newFixedThreadPool(numeroTareas)) {

            List<Future<Long>> futuros = new ArrayList<>();

            for (int i = 0; i < numeroTareas; i++) {

                int inicio = i * numeros.length / numeroTareas;
                int fin = (i + 1) * numeros.length / numeroTareas;

                Callable<Long> tarea = () -> {
                    long sumaParcial = 0;

                    for (int j = inicio; j < fin; j++) {
                        sumaParcial += numeros[j];
                    }

                    return sumaParcial;
                };

                Future<Long> futuro = executor.submit(tarea);
                futuros.add(futuro);
            }

            long sumaTotal = 0;

            for (Future<Long> futuro : futuros) {
                sumaTotal += futuro.get();
            }

            System.out.println("Suma total: " + sumaTotal);
        }
    }
}