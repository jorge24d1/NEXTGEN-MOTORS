# Informe Técnico: Modelo de Optimización de Vehículos
**Proyecto:** NEXTGEN-MOTORS  
**Componente:** Motor de Recomendación MILP (Mixed-Integer Linear Programming)

## 1. Descripción del Modelo
El sistema utiliza un algoritmo de **Maximización de Satisfacción Multiobjetivo**. El objetivo es encontrar el vehículo $i$ dentro del catálogo que mejor se adapte a las restricciones de presupuesto y preferencias subjetivas del usuario.

## 2. Definición del Modelo Matemático

### Función Objetivo
Maximizar el Índice de Satisfacción ($Z$):

$$Max \ Z = \sum_{i=1}^{n} (W_{precio} \cdot S_{p,i} + W_{año} \cdot S_{a,i} + W_{pas} \cdot S_{pas,i} + W_{cat} \cdot S_{c,i} + W_{pref} \cdot S_{pref,i}) \cdot x_i$$

**Significado de los componentes:**
*   **$x_i$**: Variable binaria (1 si el auto $i$ es recomendado, 0 si no).
*   **$S_{p,i}$**: Score de Precio (Normalización inversa: $\frac{P_{max} - p_i}{P_{max} - P_{min} + 1}$).
*   **$S_{a,i}$**: Score de Modernidad (Normalización lineal del año del vehículo).
*   **$W$**: Pesos asignados dinámicamente según el perfil (Deportivo, Familiar, etc.).

---

### Sujeto a (Restricciones - S.A.)

| Restricción | Ecuación Matemática | Descripción Técnica |
| :--- | :--- | :--- |
| **Presupuesto Máximo** | $p_i \leq P_{max}$ | El precio debe ser menor o igual al presupuesto. |
| **Presupuesto Mínimo** | $p_i \geq P_{min}$ | Límite inferior para filtrar gamas no deseadas. |
| **Capacidad** | $pas_i \geq Pas_{req}$ | Requisito de espacio (pasajeros). |
| **Año Mínimo** | $a_i \geq Año_{min}$ | Filtro de obsolescencia del vehículo. |
| **Naturaleza Variable** | $x_i \in \{0, 1\}$ | Variable de decisión entera binaria. |

---

## 3. Lógica de Ponderación Dinámica
El modelo no es estático. Dependiendo del **Uso Principal** seleccionado por el usuario, el algoritmo reasigna los valores de $W$:

*   **Perfil Deportivo:** Prioriza $W_{año}$ (tecnología) y $W_{cat}$ (estética/potencia).
*   **Perfil Familiar:** Prioriza $W_{pas}$ (espacio) y $W_{precio}$ (economía).
*   **Perfil Trabajo:** Prioriza $W_{precio}$ y capacidad de carga.
