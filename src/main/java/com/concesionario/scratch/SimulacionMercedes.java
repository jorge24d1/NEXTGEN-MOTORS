package com.concesionario.scratch;

public class SimulacionMercedes {

    static class Vehiculo {
        String marca, modelo, categoria, transmision, combustible, descripcion;
        int año;
        double precio;
        Integer pasajeros;

        Vehiculo(String ma, String mo, int a, double p, String cat, String trans, String comb, int pas, String desc) {
            marca = ma; modelo = mo; año = a; precio = p; categoria = cat;
            transmision = trans; combustible = comb; pasajeros = pas; descripcion = desc;
        }
    }

    public static void main(String[] args) {
        // Datos del Mercedes
        Vehiculo mercedes = new Vehiculo(
            "Mercedes", "AMG GT", 2026, 13000000.0, "Automóviles", 
            "Automática", "Gasolina", 4, 
            "La cumbre del rendimiento deportivo y el lujo exclusivo. Este superdeportivo cupé..."
        );

        // Entrada del Usuario
        double minPrecio = 1000000;
        double maxPrecio = 15000000;
        int minPasajeros = 4;
        int minAño = 2023;
        String usoPrincipal = "Deportivo";
        String prefTransmision = "Automática";
        String prefCombustible = "Gasolina";

        System.out.println("--- SIMULACIÓN DE OPTIMIZACIÓN ---");
        System.out.println("Usuario busca: " + usoPrincipal + " entre " + minPrecio + " y " + maxPrecio);

        // 1. Filtrado
        boolean pasaFiltros = mercedes.precio >= minPrecio && mercedes.precio <= maxPrecio &&
                             mercedes.pasajeros >= minPasajeros &&
                             mercedes.año >= minAño;

        System.out.println("¿Mercedes pasa filtros?: " + pasaFiltros);

        if (pasaFiltros) {
            // 2. Pesos para "Deportivo"
            double wPrecio = 1.0;
            double wAño = 4.5;
            double wPasajeros = 0.5;
            double wCat = 2.0;
            double wComb = 2.0;

            // 3. Normalización (Supongamos minAño=2015, maxAño=2026, maxPas=7)
            double minAñoCat = 2015;
            double maxAñoCat = 2026;
            double maxPasCat = 7;

            // Score Precio (Economía)
            double sPrecio = (maxPrecio - mercedes.precio) / (maxPrecio - minPrecio + 1);
            
            // Score Año (Modernidad)
            double sAño = (mercedes.año - minAñoCat) / (maxAñoCat - minAñoCat);
            
            // Score Pasajeros
            double sPasajeros = (double) mercedes.pasajeros / maxPasCat;

            // Match Categoría / Uso
            String desc = mercedes.descripcion.toLowerCase();
            String cat = mercedes.categoria.toLowerCase();
            boolean matchCat = cat.contains("deportiv") || cat.contains("performance") || desc.contains("deportiv");
            double sCat = matchCat ? 1.0 : 0.0;

            // Match Preferencias
            boolean matchComb = mercedes.combustible.equalsIgnoreCase(prefCombustible);
            boolean matchTrans = mercedes.transmision.equalsIgnoreCase(prefTransmision);
            double scorePrefs = (matchComb ? 0.6 : 0.0) + (matchTrans ? 0.4 : 0.0);

            // Índice Final
            double index = (wPrecio * sPrecio) + (wAño * sAño) + (wPasajeros * sPasajeros) + (wCat * sCat) + (wComb * scorePrefs);

            System.out.println("\nResultados para Mercedes AMG GT:");
            System.out.println("- Score Precio (0-1): " + sPrecio);
            System.out.println("- Score Año (0-1): " + sAño);
            System.out.println("- Match Deportivo: " + matchCat);
            System.out.println("- Match Preferencias (Comb+Trans): " + scorePrefs);
            System.out.println("\n=> ÍNDICE DE SATISFACCIÓN (S): " + index);
            System.out.println("\nInterpretación: Con S=" + String.format("%.2f", index) + " de un máximo de 10, el Mercedes AMG GT es un 'Match' de alto nivel.");
        }
    }
}
