package _6ColeccionesSeguras;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ContadorConcurrente {

    static void contarPalabras(
            String fragmento,
            ConcurrentHashMap<String, Integer> contador) {

        String[] palabras = fragmento
                .toLowerCase()
                .split("\\s+");

        for (String palabra : palabras) {
            palabra = palabra.replaceAll(
                    "^[^\\p{L}\\p{N}]+|[^\\p{L}\\p{N}]+$",
                    ""
            );

            if (!palabra.isEmpty()) {
                contador.merge(palabra, 1, Integer::sum);
            }
        }
    }

    public static void main(String[] args)
            throws InterruptedException {

        String[] fragmentos = {
                "Java tiene hilos y Java tiene clases",
                "Los hilos ejecutan tareas concurrentes",
                "Java permite ejecutar tareas concurrentes",
                "Los hilos comparten recursos"
        };

        ConcurrentHashMap<String, Integer> contador =
                new ConcurrentHashMap<>();

        Thread[] hilos = new Thread[fragmentos.length];

        for (int i = 0; i < fragmentos.length; i++) {
            final String fragmento = fragmentos[i];

            hilos[i] = new Thread(
                    () -> contarPalabras(fragmento, contador)
            );

            hilos[i].start();
        }

        for (Thread hilo : hilos) {
            hilo.join();
        }

        contador.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(System.out::println);
    }
}