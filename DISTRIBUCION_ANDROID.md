# Distribución de NextGen Motors para Android

La página pública es `/descargar-app`. La consulta `GET /api/movil/version` devuelve 204 mientras no haya publicación y 200 con la ficha vigente (codigo, version, novedades, publicada y descarga). Ambas rutas permiten el acceso antes del login. El QR apunta a la URL pública de Render.

## Publicar una actualización

1. Incrementa el código en `pubspec.yaml` del proyecto móvil, por ejemplo `1.0.1+2`. Compila el APK release con la misma clave oficial. Verifica el identificador, el código y la firma con las herramientas de Android.
2. En el repositorio público `jorge24d1/NEXTGEN-MOTORS_MOBILE`, crea un GitHub Release con la etiqueta `android-2` y sube el archivo firmado como `nextgen-motors.apk`. Usa una etiqueta y un archivo nuevos para cada compilación. Verifica la descarga como visitante.
3. Entra como ADMINISTRADOR en `/admin/versiones-app`. Guarda el borrador con el código, la versión y las novedades. Revisa el enlace y pulsa *Publicar para clientes*. El servidor comprueba en GitHub que el APK es accesible antes de marcarlo como publicado.
4. La web y Flutter usan la publicación con el código más alto. Un cliente con la misma versión o una posterior continúa normalmente. En una instalación anterior compatible, confirma que aparece el aviso, que *Más tarde* permite continuar y que *Actualizar* abre la descarga oficial.

La primera compilación es `1.0.0+1`. Al arrancar en Render, con `RENDER=true`, el backend prepara y publica esa primera ficha únicamente si GitHub tiene la descarga y todavía no hay versión vigente. Un fallo deja la publicación pendiente en el panel. MongoDB almacena los borradores y las publicaciones en `versiones_android`.

Una APK de desarrollo con la firma debug no se puede actualizar con la APK oficial. Para el primer ensayo usa un dispositivo limpio o respalda antes los datos locales de la instalación de desarrollo. Las siguientes actualizaciones oficiales deben conservar el paquete y la firma.
