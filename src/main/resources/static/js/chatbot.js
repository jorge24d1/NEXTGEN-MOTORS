document.addEventListener('DOMContentLoaded', function() {
    // Elementos del DOM
    const chatContainer = document.getElementById('chat-container');
    const chatToggle = document.getElementById('chat-toggle');
    const closeChatBtn = document.getElementById('close-chat');
    const chatMessages = document.getElementById('chat-messages');
    const userInput = document.getElementById('user-input');
    const sendBtn = document.getElementById('send-btn');

    // Crear botón de reset
    const resetBtn = document.createElement('button');
    resetBtn.id = 'reset-chat';
    resetBtn.textContent = 'Restablecer chat';
    resetBtn.className = 'reset-button';
    chatContainer.querySelector('.chat-header').appendChild(resetBtn);

    // Variables para el timeout de inactividad
    let inactivityTimer;
    const INACTIVITY_TIMEOUT = 180000; // 3 minutos en milisegundos
    const RESET_AFTER_PROMPT = 60000; // 1 minuto después del mensaje de inactividad

    // Determinar el rol basado en la URL y elementos del DOM
    const path = window.location.pathname;
    let userRole = 'usuario';
    if (path.startsWith('/admin')) {
        if (document.getElementById('role-worker')) {
            userRole = 'trabajador';
        } else {
            userRole = 'administrador';
        }
    } else if (path.startsWith('/perfil_gestor') || path.startsWith('/perfil_analisis') || path.startsWith('/perfil_asesor') || path.startsWith('/trabajador')) {
        userRole = 'trabajador';
    }

    // Función para obtener el saludo inicial
    function getWelcomeMessage() {
        if (userRole === 'administrador') {
            return {
                text: '¡Hola Administrador! ¿En qué puedo asistirte en la gestión del sistema hoy?',
                options: [
                    { text: 'Crear reunión', value: 'crear reunion' },
                    { text: 'Listar reuniones', value: 'listar reuniones' }
                ]
            };
        } else if (userRole === 'trabajador') {
            return {
                text: '¡Hola equipo! ¿En qué te puedo ayudar hoy con tus tareas?',
                options: [
                    { text: 'Crear reunión', value: 'crear reunion' },
                    { text: 'Listar reuniones', value: 'listar reuniones' }
                ]
            };
        } else {
            return {
                text: '¡Hola! Bienvenido al concesionario. ¿En qué puedo ayudarte hoy?',
                options: [
                    { text: 'Vehículos disponibles', value: 'vehiculos' },
                    { text: 'Agendar cita', value: 'agendar' },
                    { text: 'Contactar asesor', value: 'asesor' },
                    { text: 'Busca tu vehiculo ideal', value: 'ideal' }
                ]
            };
        }
    }

    // Función para restablecer el chat completamente
    function resetChat() {
        chatMessages.innerHTML = '';
        sessionStorage.removeItem('chatHistory');
        sessionStorage.removeItem('chatMessagesArray'); // Limpiar el array de historial
        const welcome = getWelcomeMessage();
        addBotMessage(welcome.text, welcome.options);
        resetInactivityTimer();
    }

    // Función para manejar el timeout de inactividad
    function resetInactivityTimer() {
        clearTimeout(inactivityTimer);

        inactivityTimer = setTimeout(() => {
            addBotMessage('¿Sigues ahí? ¿En qué más puedo ayudarte?', [
                { text: 'Sí, quiero continuar', value: 'continuar' },
                { text: 'No, gracias', value: 'salir' }
            ]);

            inactivityTimer = setTimeout(() => {
                resetChat();
            }, RESET_AFTER_PROMPT);
        }, INACTIVITY_TIMEOUT);
    }

    // Evento para el botón de reset
    resetBtn.addEventListener('click', resetChat);

    // Alternar visibilidad del chat
    chatToggle.addEventListener('click', function() {
        chatContainer.classList.toggle('hidden');
        if (!chatContainer.classList.contains('hidden')) {
            resetInactivityTimer();
        }
    });

    // Cerrar el chat
    closeChatBtn.addEventListener('click', function() {
        chatContainer.classList.add('hidden');
    });

    // Cargar historial del chat al iniciar
    loadChatHistory();

    // Mostrar mensaje inicial si no hay historial
    if (!sessionStorage.getItem('chatHistory')) {
        const welcome = getWelcomeMessage();
        addBotMessage(welcome.text, welcome.options);
    }

    // Eventos que indican actividad del usuario
    const activityEvents = ['mousedown', 'keypress', 'scroll', 'touchstart'];
    activityEvents.forEach(event => {
        document.addEventListener(event, resetInactivityTimer, false);
    });

    // Enviar mensaje al presionar Enter o el botón
    userInput.addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            sendMessage();
        }
        resetInactivityTimer();
    });

    sendBtn.addEventListener('click', function() {
        sendMessage();
        resetInactivityTimer();
    });

    function sendMessage() {
        const message = userInput.value.trim();
        if (message) {
            addUserMessage(message);
            userInput.value = '';
            resetInactivityTimer();

            // 1. Intentar procesar como opción de menú fija (opcional)
            const opcionesFijas = ['vehiculos', 'agendar', 'asesor', 'menu', 'hola', 'ideal'];

            if (opcionesFijas.includes(message.toLowerCase())) {
                processUserInput(message.toLowerCase());
            } else if (esPreguntaPredeterminada(message)) {
                // Ya se procesó la respuesta de forma local sin usar cuota de IA
            } else {
                // 2. SI NO ES UNA OPCIÓN FIJA, QUE RESPONDA LA IA SIEMPRE
                buscarVehiculosInteligente(message);
            }
        }
    }

    function esPreguntaPredeterminada(mensaje) {
        const msg = mensaje.toLowerCase().trim();
        
        if (msg.includes('quien eres') || msg.includes('quién eres') || msg.includes('cómo te llamas') || msg.includes('como te llamas')) {
            addBotMessage('Soy Dante, el asesor y asistente virtual de NextGen Motors. Mi objetivo es ayudarte a encontrar el vehículo ideal, resolver dudas y agendar tus citas. ¿En qué te puedo asesorar hoy?', [
                { text: 'Ir al Inicio', value: 'menu' }
            ]);
            return true;
        }

        if (msg === 'cita' || msg === 'citas' || msg === 'agendar' || msg === 'agendar cita' || msg === 'quiero agendar una cita' || msg === 'como agendo una cita' || msg === 'sacar cita' || msg === 'pedir cita') {
            addBotMessage('¡Por supuesto! Te redirigiré al apartado de citas en un momento para que puedas agendar tu espacio...', [
                { text: 'Ir a Citas ahora', value: 'cotizar' }
            ]);
            setTimeout(() => {
                window.location.href = '/usuario/cita';
            }, 1800);
            return true;
        }

        //que haces

        if (msg === 'gracias' || msg === 'muchas gracias' || msg === 'ok gracias') {
            addBotMessage('¡De nada! Recuerda que estoy aquí 24/7 para ayudarte con tus consultas automotrices. ¿Deseas explorar algo más?', [
                { text: 'Vehículos disponibles', value: 'vehiculos' },
                { text: 'Terminar chat', value: 'salir' }
            ]);
            return true;
        }

        if (msg === 'adios' || msg === 'adiós' || msg === 'chao' || msg === 'hasta luego') {
            addBotMessage('¡Hasta pronto! Vuelve cuando quieras a NextGen Motors.');
            resetChat();
            return true;
        }
        
        return false;
    }

    function esBusquedaDeVehiculo(mensaje) {
        const mensajeLower = mensaje.toLowerCase();
        const palabrasVehiculo = [
            // Tipos básicos
            'auto', 'carro', 'vehículo', 'coche', 'moto',

            // ✅ TODOS TUS TIPOS DE VEHÍCULOS
            'pick-ups', 'pick ups', 'pickup', 'pick up',
            'camioneta', 'camionetas',
            'automóvil', 'automovil',
            'performance',
            'híbrido', 'hibrido', 'eléctrico', 'electrico',
            'comercial', 'comerciales',
            'suv', 'suvs',
            'deportivo', 'deportivos',

            // Características
            'económico', 'economico', 'barato', 'accesible',
            'familiar', 'espacioso', 'grande',
            'potente', 'rápido', 'rapido', 'veloz',
            'nuevo', 'usado', 'seminuevo',

            // Marcas
            'chevrolet', 'toyota', 'ford', 'nissan', 'bmw',
            'mercedes', 'audi', 'honda', 'hyundai', 'kia',
            'volkswagen', 'mazda', 'subaru', 'jeep',

            // Acciones
            'precio', 'costo', 'valor', 'comprar', 'adquirir',
            'busco', 'quiero', 'necesito', 'deseo',
            'recomienda', 'recomiéndame', 'sugiere', 'encuentra',
            'cotizar', 'cotización'
        ];

        return palabrasVehiculo.some(palabra => mensajeLower.includes(palabra));
    }

    async function buscarVehiculosInteligente(mensaje) {
        // Mostrar typing
        const typingDiv = document.createElement('div');
        typingDiv.className = 'message bot-message typing-indicator';
        typingDiv.innerHTML = '<div class="typing-dots"><span></span><span></span><span></span></div>';
        chatMessages.appendChild(typingDiv);
        chatMessages.scrollTop = chatMessages.scrollHeight;

        try {
            // Obtener historial de mensajes (últimos 6 para no saturar)
            const historial = JSON.parse(sessionStorage.getItem('chatMessagesArray') || '[]');
            const ultimosMensajes = historial.slice(-6);

            const response = await fetch('/api/chatbot/mensaje', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ 
                    mensaje: mensaje,
                    historial: ultimosMensajes
                })
            });

            let data;
            try {
                data = await response.json();
            } catch (e) {
                data = null;
            }

            // Remover typing
            typingDiv.remove();

            if (!response.ok) {
                if (data && data.respuesta) {
                    addBotMessage(data.respuesta);
                } else {
                    addBotMessage('Lo siento, en este momento mis servidores están saturados. Por favor intenta de nuevo en un minuto.');
                }
                return;
            }

            // Mostrar respuesta del bot
            addBotMessage(data.respuesta);

            // ✅ MOSTRAR VEHÍCULOS RECOMENDADOS (solo nombre, imagen y botón)
            if (data.vehiculosRecomendados && data.vehiculosRecomendados.length > 0) {
                mostrarVehiculosSimplificado(data.vehiculosRecomendados);
            }

        } catch (error) {
            console.error('Error:', error);
            if (document.body.contains(typingDiv)) typingDiv.remove();
            
            // Si hay un error grave de red, usar el sistema normal de reserva
            addBotMessage('Ha ocurrido un error de conexión, pero aún puedo ayudarte con estas opciones:');
            processUserInput('menu');
        }
    }

    function mostrarVehiculosSimplificado(vehiculos) {
        const container = document.createElement('div');
        container.className = 'vehiculos-recomendados';

        vehiculos.forEach(vehiculo => {
            const vehiculoCard = document.createElement('div');
            vehiculoCard.className = 'vehiculo-recomendado';

            vehiculoCard.innerHTML = `
                <div class="vehiculo-simple">
                    <img src="${vehiculo.imagenUrl}" alt="${vehiculo.marca} ${vehiculo.modelo}"
                         onerror="this.src='/images/placeholder.jpg'">
                    <div class="vehiculo-info-simple">
                        <h4>${vehiculo.marca} ${vehiculo.modelo}</h4>
                        <button onclick="window.location.href='/vehiculos/explorar/${vehiculo.id}'"
                                class="btn-explorar-simple">
                            <i class="fas fa-search"></i> Explorar
                        </button>
                    </div>
                </div>
            `;

            container.appendChild(vehiculoCard);
        });

        chatMessages.appendChild(container);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        saveChatHistory();
    }

    // Guardar historial del chat
    function saveChatHistory() {
        sessionStorage.setItem('chatHistory', chatMessages.innerHTML);
    }

    // Cargar historial del chat
    function loadChatHistory() {
        const savedHistory = sessionStorage.getItem('chatHistory');
        if (savedHistory) {
            chatMessages.innerHTML = savedHistory;
            restoreOptionButtons();
        }
    }

    // Restaurar eventos de los botones de opciones
    function restoreOptionButtons() {
        document.querySelectorAll('.option-button').forEach(button => {
            button.addEventListener('click', function() {
                const text = this.textContent;
                const value = this.getAttribute('data-value') || text.toLowerCase();
                addUserMessage(text);
                processUserInput(value);
            });
        });
    }

    function processUserInput(input) {
        if (input === 'continuar') {
            addBotMessage('Perfecto, ¿en qué más puedo ayudarte?');
            showMainMenu();
            resetInactivityTimer();
            return;
        }
        else if (input === 'salir') {
            resetChat();
            return;
        }
        else if (input === 'crear reunion') {
            // Mostrar la sección de reuniones
            document.querySelectorAll('section[id]').forEach(s => s.classList.add('hidden'));
            const target = document.getElementById('reuniones');
            if (target) {
                target.classList.remove('hidden');
            }
            // Actualizar clase active en el sidebar
            document.querySelectorAll('.sidebar-link').forEach(link => {
                if (link.getAttribute('href') === '#reuniones') {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
            // Abrir el modal de creación de reunión
            if (typeof mostrarModal === 'function') {
                mostrarModal('modal-reunion');
            }
            addBotMessage('He abierto el formulario para crear una reunión.');
        }
        else if (input === 'listar reuniones') {
            // Mostrar la sección de reuniones
            document.querySelectorAll('section[id]').forEach(s => s.classList.add('hidden'));
            const target = document.getElementById('reuniones');
            if (target) {
                target.classList.remove('hidden');
            }
            // Actualizar clase active en el sidebar
            document.querySelectorAll('.sidebar-link').forEach(link => {
                if (link.getAttribute('href') === '#reuniones') {
                    link.classList.add('active');
                } else {
                    link.classList.remove('active');
                }
            });
            // Refrescar reuniones
            if (typeof refrescarReuniones === 'function') {
                refrescarReuniones();
            }
            addBotMessage('Aquí tienes la lista de todas tus reuniones programadas.');
        }
        else if (input === 'hola' || input === 'hi' || input === 'buenos días') {
            showMainMenu();
        }
        else if (input.includes('vehículo') || input.includes('vehiculo') || input === 'vehiculos' || input === '1') {
            showVehicleCategories();
        }
        else if (input.includes('agendar') || input.includes('cita') || input === '2') {
            showAppointmentOptions();
        }
        else if (input.includes('asesor') || input === '4') {
            showAdvisorOptions();
        }
        else if (input === 'menu' || input === 'volver' || input === 'inicio') {
            showMainMenu();
        }
        else if (input === 'información taller') {
            window.location.href = '/usuario/cita?tipo=Información Taller';
        }
        else if (input === 'mantenimiento') {
            window.location.href = '/usuario/cita?tipo=Mantenimiento';
        }
        else if (input === 'garantías') {
            window.location.href = '/usuario/cita?tipo=Garantías';
        }
        else if (input === 'otros') {
            window.location.href = '/usuario/cita?tipo=Otros';
        }
        else if (input === 'pick-ups' || input === 'pick ups') {
            redireccionarCategoria('#categoria-pick-ups');
        }
        else if (input === 'híbridos') {
            redireccionarCategoria('#categoria-hibridos');
        }
        else if (input === 'performance') {
            redireccionarCategoria('#categoria-performance');
        }
        else if (input === 'automóviles' || input === 'automoviles') {
            redireccionarCategoria('#categoria-automoviles');
        }
        else if (input === 'llamar') {
            addBotMessage('Puedes llamar al concesionario al número: <a href="tel:3054424835" style="color: #0066cc; text-decoration: underline;">305-442-4835</a>');
        }
        else if (input === 'whatsapp') {
            addBotMessage('Puedes contactarnos por WhatsApp: <a href="https://wa.me/3234615898" target="_blank" style="color: #0066cc; text-decoration: underline;">323-461-5898</a>');
        }
        else if (input === 'email') {
            addBotMessage('Puedes escribirnos al correo electrónico: <a href="mailto:Nexgenmotors@gmail.com" style="color: #0066cc; text-decoration: underline;">Nexgenmotors@gmail.com</a>');
        }
        else if (input === 'cotizar') {
            window.location.href = '/usuario/cita';
        }
        else if (input === 'ideal') {
            window.location.href = '/vehiculo-ideal';
        }
        else {
            addBotMessage('No entendí tu solicitud. Por favor selecciona una opción del menú:');
            showMainMenu();
        }

        resetInactivityTimer();
    }

    function redireccionarCategoria(hash) {
        if (window.location.pathname === '/vehiculos') {
            const id = hash.substring(1);
            const elemento = document.getElementById(id);
            if (elemento) {
                window.history.pushState(null, null, hash);
                elemento.scrollIntoView({ behavior: 'smooth' });
            } else {
                window.location.href = '/vehiculos' + hash;
            }
        } else {
            window.location.href = '/vehiculos' + hash;
        }
    }

    function showVehicleCategories() {
        addBotMessage('Tenemos estas categorías de vehículos:', [
            { text: 'Pick-Ups', value: 'pick-ups' },
            { text: 'Performance', value: 'performance' },
            { text: 'Híbridos', value: 'híbridos' },
            { text: 'Automóviles', value: 'automóviles' },
            { text: 'Volver al menú', value: 'menu' }
        ]);
    }

    function showAppointmentOptions() {
        addBotMessage('¿Qué tipo de cita deseas agendar?', [
            { text: 'Información Taller', value: 'información taller' },
            { text: 'Mantenimiento', value: 'mantenimiento' },
            { text: 'Garantías', value: 'garantías' },
            { text: 'Otros servicios', value: 'otros' },
            { text: 'Volver al menú', value: 'menu' }
        ]);
    }

    function showAdvisorOptions() {
        addBotMessage('Puedes contactar a un asesor:', [
            { text: 'Llamar al concesionario: 305-442-4835', value: 'llamar' },
            { text: 'WhatsApp: 323-461-5898', value: 'whatsapp' },
            { text: 'Correo electrónico: Nexgenmotors@gmail.com', value: 'email' },
            { text: 'Volver al menú', value: 'menu' }
        ]);
    }

    function showMainMenu() {
        if (userRole === 'administrador' || userRole === 'trabajador') {
            addBotMessage('¿En qué más te puedo ayudar hoy?', [
                { text: 'Crear reunión', value: 'crear reunion' },
                { text: 'Listar reuniones', value: 'listar reuniones' }
            ]);
        } else {
            addBotMessage('¿Cómo puedo ayudarte hoy?', [
                { text: 'Vehículos disponibles', value: 'vehiculos' },
                { text: 'Agendar cita', value: 'agendar' },
                { text: 'Contactar asesor', value: 'asesor' },
                { text: 'Busca tu vehiculo ideal', value: 'ideal' }
            ]);
        }
    }

    function addUserMessage(text) {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message user-message';
        messageDiv.textContent = text;

        const timestamp = document.createElement('div');
        timestamp.className = 'timestamp';
        timestamp.textContent = getCurrentTime();

        const container = document.createElement('div');
        container.className = 'message-container';
        container.appendChild(messageDiv);
        container.appendChild(timestamp);

        chatMessages.appendChild(container);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        // Guardar para memoria de la IA
        const historial = JSON.parse(sessionStorage.getItem('chatMessagesArray') || '[]');
        historial.push({ role: 'user', content: text });
        sessionStorage.setItem('chatMessagesArray', JSON.stringify(historial));

        saveChatHistory();
    }

    function addBotMessage(text, options = null) {
        const messageDiv = document.createElement('div');
        messageDiv.className = 'message bot-message';
        messageDiv.innerHTML = text;

        const timestamp = document.createElement('div');
        timestamp.className = 'timestamp';
        timestamp.textContent = getCurrentTime();

        const container = document.createElement('div');
        container.className = 'message-container';
        container.appendChild(messageDiv);
        container.appendChild(timestamp);

        if (options) {
            const optionsContainer = document.createElement('div');
            optionsContainer.className = 'options-container';

            options.forEach(option => {
                const button = document.createElement('button');
                button.className = 'option-button';
                button.textContent = option.text;
                button.setAttribute('data-value', option.value);
                button.addEventListener('click', () => {
                    addUserMessage(option.text);
                    processUserInput(option.value);
                });
                optionsContainer.appendChild(button);
            });

            container.appendChild(optionsContainer);
        }

        chatMessages.appendChild(container);
        chatMessages.scrollTop = chatMessages.scrollHeight;
        
        // Guardar para memoria de la IA
        const historial = JSON.parse(sessionStorage.getItem('chatMessagesArray') || '[]');
        historial.push({ role: 'assistant', content: text.replace(/<[^>]*>?/gm, '') });
        sessionStorage.setItem('chatMessagesArray', JSON.stringify(historial));

        saveChatHistory();
    }

    function getCurrentTime() {
        const now = new Date();
        return now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
    }

    // Exponer función globalmente para ser llamada desde otras páginas
    window.abrirChatbot = function(mensaje) {
        if (chatContainer.classList.contains('hidden')) {
            chatContainer.classList.remove('hidden');
        }
        if (mensaje) {
            addUserMessage("Análisis de mi Vehículo Ideal");
            buscarVehiculosInteligente(mensaje);
        }
    };

    // ✅ MANEJO DE MENSAJES PENDIENTES (Desde la herramienta Vehículo Ideal)
    const pendingMessage = sessionStorage.getItem('pendingDanteMessage');
    if (pendingMessage) {
        setTimeout(() => {
            window.abrirChatbot(pendingMessage);
            sessionStorage.removeItem('pendingDanteMessage');
        }, 1000); // Pequeño delay para asegurar que todo cargó
    }

    // Iniciar el temporizador de inactividad al cargar
    resetInactivityTimer();
});