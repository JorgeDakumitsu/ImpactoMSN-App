# ImpactoMSN 1.0.0

**Versión estable** — Android, con base Kotlin Multiplatform para iOS/iPadOS y macOS.

## Novedades

- Compatibilidad MSNP18 para texto, formato, escritura a mano, zumbidos, guiños, clips de voz, fotografías, juegos y archivos.
- Comprobación de actualizaciones mediante GitHub Releases.
- Avisos de actualización y confirmación antes de descargar el APK o IPA.
- Diseños adaptables para móvil, tableta y escritorio.
- Ventanas Android redimensionables.
- Modos Estándar, Windows 7 Aero y Apple Liquid Glass.
- Aero y Liquid Glass compatibles con los tonos `Default`, `Dynamic` y `Scenes`.
- Emoticonos MSN originales y alternativa Unicode.
- Formato de texto, colores, fuentes y sonidos de Messenger Plus!
- Verificación del contenido incluido de Ruffle.

## Base multiplataforma

- Añadido el módulo Kotlin Multiplatform `:shared`.
- Añadidos destinos Android, iOS dispositivo/simulador y macOS Intel/Apple Silicon.
- Añadido Ktor Client Multiplatform con motores Android y Darwin.
- Añadido SQLDelight Multiplatform con controladores Android y Native.
- Añadida infraestructura de tamaños de ventana adaptables.
- Documentadas las opciones de compilación y distribución de Apple.

## Emoticonos personalizados MSN

- Reconocimiento de MSNObject tipo 2.
- Descarga mediante MSNSLP/P2P.
- Validación SHA-1 de las imágenes recibidas.
- Almacenamiento local persistente.
- Renderizado dinámico dentro del chat.
- Compatibilidad con alias como `m1`, `[M1]`, `zz` y `zzzz`.
- Las variantes con y sin corchetes se reconocen como el mismo alias.

## Compatibilidad y distribución

- Android puede compilarse desde Windows o Linux.
- Las compilaciones Apple requieren macOS/Xcode o un runner remoto de macOS.
- La instalación IPA depende de firma, TrollStore u otro método de sideloading.

## Limitaciones conocidas

- El proyecto host de Apple/Xcode todavía debe validarse en macOS.
- APNs, CallKit, AVFoundation, BGTaskScheduler, PushKit, Ruffle en WKWebView y Siren XCFramework requieren integración específica de Apple.
- Los emoticonos personalizados dependen de que el cliente remoto envíe el MSNObject requerido.

## Verificación

```text
:app:compileDebugKotlin
BUILD SUCCESSFUL
```
