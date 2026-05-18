document.addEventListener('DOMContentLoaded', function () {
    console.log('📅 Inicializando gestión de reuniones...');

    const formReunion = document.getElementById('form-reunion');
    const listaReunionesBody = document.getElementById('lista-reuniones-body');

    // Cargar reuniones inicialmente
    cargarReuniones();

    // Manejar envío del formulario
    if (formReunion) {
        formReunion.addEventListener('submit', function (e) {
            e.preventDefault();
            
            const formData = new FormData(formReunion);
            const reunionData = {
                titulo: formData.get('titulo'),
                descripcion: formData.get('descripcion'),
                fecha: formData.get('fecha'),
                hora: formData.get('hora'),
                categoria: formData.get('categoria')
            };

            fetch('/admin/reuniones', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(reunionData)
            })
            .then(response => {
                if (response.ok) {
                    mostrarNotificacion('Reunión programada con éxito', 'success');
                    cerrarModal('modal-reunion');
                    formReunion.reset();
                    cargarReuniones();
                } else {
                    return response.text().then(text => { throw new Error(text) });
                }
            })
            .catch(error => {
                console.error('Error:', error);
                mostrarNotificacion('Error al programar la reunión', 'error');
            });
        });
    }

    // Escuchar clics en el sidebar para recargar reuniones si se selecciona la sección
    document.querySelectorAll('.sidebar-link').forEach(link => {
        link.addEventListener('click', function() {
            if (this.getAttribute('href') === '#reuniones') {
                cargarReuniones();
            }
        });
    });

    function cargarReuniones() {
        if (!listaReunionesBody) return;

        fetch('/admin/reuniones')
            .then(response => response.json())
            .then(data => {
                renderizarReuniones(data);
            })
            .catch(error => {
                console.error('Error al cargar reuniones:', error);
            });
    }

    function renderizarReuniones(reuniones) {
        if (!listaReunionesBody) return;

        if (reuniones.length === 0) {
            listaReunionesBody.innerHTML = `
                <tr>
                    <td colspan="6" class="px-4 py-8 text-center text-gray-500">
                        <div class="flex flex-col items-center">
                            <i class='bx bx-calendar-event text-4xl mb-2 opacity-20'></i>
                            <p>No hay reuniones programadas</p>
                        </div>
                    </td>
                </tr>
            `;
            return;
        }

        listaReunionesBody.innerHTML = reuniones.map(r => `
            <tr class="hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors">
                <td class="px-4 py-4 whitespace-nowrap text-sm font-bold text-blue-600 dark:text-blue-400">#${r.id}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm font-medium text-gray-900 dark:text-white">${r.titulo}</td>
                <td class="px-4 py-4 text-sm text-gray-600 dark:text-gray-400 max-w-xs truncate">${r.descripcion || '-'}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">
                    <span class="px-2 py-1 rounded-full text-xs font-semibold ${getCategoriaColor(r.categoria)}">
                        ${capitalize(r.categoria)}
                    </span>
                </td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">${formatFecha(r.fecha)}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm text-gray-900 dark:text-white">${r.hora}</td>
                <td class="px-4 py-4 whitespace-nowrap text-sm font-medium">
                    <button onclick="eliminarReunion(${r.id})" class="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300 transition-colors">
                        <i class='bx bx-trash'></i> Eliminar
                    </button>
                </td>
            </tr>
        `).join('');
    }

    window.eliminarReunion = function(id) {
        if (!confirm('¿Estás seguro de que deseas eliminar esta reunión?')) return;

        fetch(`/admin/reuniones/${id}`, {
            method: 'DELETE'
        })
        .then(response => {
            if (response.ok) {
                mostrarNotificacion('Reunión eliminada', 'success');
                cargarReuniones();
            } else {
                mostrarNotificacion('Error al eliminar la reunión', 'error');
            }
        })
        .catch(error => {
            console.error('Error:', error);
            mostrarNotificacion('Error de conexión', 'error');
        });
    };

    function getCategoriaColor(cat) {
        switch(cat) {
            case 'analista': return 'bg-blue-100 text-blue-800';
            case 'gestion': return 'bg-purple-100 text-purple-800';
            case 'marketing': return 'bg-pink-100 text-pink-800';
            case 'acesoria': return 'bg-yellow-100 text-yellow-800';
            default: return 'bg-gray-100 text-gray-800';
        }
    }

    function capitalize(s) {
        return s.charAt(0).toUpperCase() + s.slice(1);
    }

    function formatFecha(fechaStr) {
        const parts = fechaStr.split('-');
        return `${parts[2]}/${parts[1]}/${parts[0]}`;
    }

    function mostrarNotificacion(msg, type) {
        // Asumiendo que existe una función global de notificación o simplemente alert
        if (window.mostrarNotificacion) {
            window.mostrarNotificacion(msg, type);
        } else {
            alert(msg);
        }
    }
});
