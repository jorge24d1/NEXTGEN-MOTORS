async function consultarOfertas(url, datos) {
    const opciones = { headers: { Accept: 'application/json' }, cache: 'no-store' };
    if (datos) {
        opciones.method = 'POST';
        opciones.headers['Content-Type'] = 'application/json';
        opciones.headers['X-CSRF-TOKEN'] = document.querySelector('meta[name="csrf-ofertas"]').content;
        opciones.body = JSON.stringify(datos);
    }
    const respuesta = await fetch(url, opciones);
    if (!respuesta.headers.get('content-type')?.includes('application/json')) {
        throw new Error('No se pudo confirmar la operación. Comprueba tu sesión.');
    }
    const resultado = await respuesta.json();
    if (!respuesta.ok) {
        const error = new Error(resultado.mensaje || 'No se pudo completar la operación.');
        error.estado = respuesta.status;
        throw error;
    }
    return resultado;
}

async function compartirOferta(boton, vehiculoId) {
    if (boton.disabled) return;
    boton.disabled = true;
    const clave = 'oferta-pendiente:' + vehiculoId;
    try {
        const url = '/asesor/vehiculos/' + encodeURIComponent(vehiculoId) + '/compartir';
        const vista = await consultarOfertas(url);
        if (!vista.destinatarios) throw new Error('Todavía no hay clientes suscritos a las ofertas.');
        const destino = vista.prueba ? 'solo al correo de prueba' : 'a ' + vista.destinatarios + ' clientes suscritos';
        if (!confirm('¿Enviar la oferta de ' + vista.vehiculo + ' ' + destino + '?')) return;
        const solicitud = sessionStorage.getItem(clave) || crypto.randomUUID();
        sessionStorage.setItem(clave, solicitud);
        const envio = await consultarOfertas(url, { solicitud, prueba: vista.prueba, destinatarios: vista.destinatarios });
        sessionStorage.removeItem(clave);
        mostrarNotificacion((envio.prueba ? 'Prueba aceptada por Brevo.' : 'Campaña aceptada por Brevo.')
            + ' Campaña ' + envio.campana + '.', 'success');
    } catch (error) {
        mostrarNotificacion(error.message, 'error');
        if (error.estado === 422 && confirm('¿Ya comprobaste en Brevo que el envío no salió y quieres preparar un nuevo intento?')) {
            sessionStorage.removeItem(clave);
        }
    } finally {
        boton.disabled = false;
    }
}

const panelOfertas = document.getElementById('preferencia-ofertas');
if (panelOfertas) {
    const aceptar = panelOfertas.querySelector('[data-aceptar]');
    const guardar = panelOfertas.querySelector('[data-guardar]');
    const estado = panelOfertas.querySelector('[data-estado]');
    let ocupado = false;
    async function actualizarPreferencia(datos) {
        if (ocupado) return;
        ocupado = true;
        aceptar.disabled = guardar.disabled = true;
        estado.textContent = 'Consultando tu preferencia…';
        try {
            const preferencia = await consultarOfertas('/usuario/perfil/ofertas', datos);
            aceptar.checked = preferencia.suscrito;
            aceptar.disabled = guardar.disabled = preferencia.bloqueado;
            estado.textContent = preferencia.suscrito ? 'Estás suscrito a las ofertas.' : 'Tu correo está fuera de los envíos de ofertas.';
            if (preferencia.bloqueado) estado.textContent += ' Brevo tiene registrada una baja.';
        } catch (error) {
            estado.textContent = error.message + ' Cierra y vuelve a abrir esta sección para consultar tu preferencia.';
        } finally {
            ocupado = false;
        }
    }
    panelOfertas.addEventListener('toggle', () => {
        if (panelOfertas.open) actualizarPreferencia();
    });
    guardar.addEventListener('click', () => actualizarPreferencia({ aceptar: aceptar.checked }));
}
