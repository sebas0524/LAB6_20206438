/*package com.example.lab6;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServicioAlmacenamiento {
    private static final String TAG = "ServicioAlmacenamiento";
    private Cloudinary cloudinary;
    private ExecutorService executor;
    private Context context;

    // Configuración de Cloudinary - Reemplaza con tus credenciales
    private static final String CLOUD_NAME = "dyqop6anx";
    private static final String API_KEY = "543935339712672";
    private static final String API_SECRET = "ZyfTAYjpM6SAC_aFAbmQcvzUrU4";

    public ServicioAlmacenamiento(Context context) {
        this.context = context;
        this.executor = Executors.newCachedThreadPool();
        conexionServicio();
    }
    public void conexionServicio() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("api_key", API_KEY);
            config.put("api_secret", API_SECRET);
            config.put("secure", "true");

            cloudinary = new Cloudinary(config);
            Log.d(TAG, "Conexión con Cloudinary establecida exitosamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al conectar con Cloudinary: " + e.getMessage());
        }
    }
    public void guardarArchivo(Uri archivoUri, String nombreArchivo, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Convertir URI a File temporal
                File archivoTemporal = crearArchivoTemporal(archivoUri);

                if (archivoTemporal != null) {
                    // Configurar parámetros de subida
                    Map<String, Object> params = ObjectUtils.asMap(
                            "public_id", nombreArchivo,
                            "folder", "gestion_dinero/comprobantes",
                            "resource_type", "image",
                            "quality", "auto:good",
                            "fetch_format", "auto"
                    );

                    // Subir archivo a Cloudinary
                    Map resultado = cloudinary.uploader().upload(archivoTemporal, params);

                    String urlImagen = (String) resultado.get("secure_url");
                    String publicId = (String) resultado.get("public_id");

                    // Eliminar archivo temporal
                    archivoTemporal.delete();

                    // Ejecutar callback en hilo principal
                    if (callback != null) {
                        ((android.app.Activity) context).runOnUiThread(() ->
                                callback.onSuccess(urlImagen, publicId));
                    }

                    Log.d(TAG, "Archivo guardado exitosamente: " + urlImagen);
                } else {
                    if (callback != null) {
                        ((android.app.Activity) context).runOnUiThread(() ->
                                callback.onError("No se pudo crear archivo temporal"));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error al guardar archivo: " + e.getMessage());
                if (callback != null) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            callback.onError("Error al subir imagen: " + e.getMessage()));
                }
            }
        });
    }
    public void obtenerArchivo(String publicId, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Generar URL de descarga optimizada
                String urlDescarga = cloudinary.url()
                        .resourceType("image")
                        .publicId(publicId)
                        .quality("auto:good")
                        .fetchFormat("auto")
                        .generate();

                // Ejecutar callback en hilo principal
                if (callback != null) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            callback.onSuccess(urlDescarga, publicId));
                }

                Log.d(TAG, "URL de archivo obtenida: " + urlDescarga);

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener archivo: " + e.getMessage());
                if (callback != null) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            callback.onError("Error al obtener imagen: " + e.getMessage()));
                }
            }
        });
    }
    public void descargarArchivo(String publicId, String nombreArchivo, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Obtener URL de descarga
                String urlDescarga = cloudinary.url()
                        .resourceType("image")
                        .publicId(publicId)
                        .quality("auto:best")
                        .generate();

                // Aquí podrías implementar la descarga real del archivo
                // usando HttpURLConnection o una librería como OkHttp

                if (callback != null) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            callback.onSuccess(urlDescarga, nombreArchivo));
                }

            } catch (Exception e) {
                Log.e(TAG, "Error al descargar archivo: " + e.getMessage());
                if (callback != null) {
                    ((android.app.Activity) context).runOnUiThread(() ->
                            callback.onError("Error al descargar: " + e.getMessage()));
                }
            }
        });
    }
    private File crearArchivoTemporal(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File archivoTemporal = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(archivoTemporal);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return archivoTemporal;

        } catch (IOException e) {
            Log.e(TAG, "Error al crear archivo temporal: " + e.getMessage());
            return null;
        }
    }
    public void limpiarRecursos() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
    public interface CloudinaryCallback {
        void onSuccess(String url, String publicId);
        void onError(String error);
    }
}*/
package com.example.lab6;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServicioAlmacenamiento {
    private static final String TAG = "ServicioAlmacenamiento";
    private Cloudinary cloudinary;
    private ExecutorService executor;
    private Context context;
    private Handler mainHandler;

    // Configuración de Cloudinary
    private static final String CLOUD_NAME = "dyqop6anx";
    private static final String API_KEY = "543935339712672";
    private static final String API_SECRET = "ZyfTAYjpM6SAC_aFAbmQcvzUrU4";

    public ServicioAlmacenamiento(Context context) {
        this.context = context;
        this.executor = Executors.newCachedThreadPool();
        this.mainHandler = new Handler(Looper.getMainLooper());
        conexionServicio();
    }

    /**
     * Método para realizar la conexión con el servicio de Cloudinary
     */
    public void conexionServicio() {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            config.put("api_key", API_KEY);
            config.put("api_secret", API_SECRET);
            config.put("secure", "true");

            cloudinary = new Cloudinary(config);
            Log.d(TAG, "Conexión con Cloudinary establecida exitosamente");
        } catch (Exception e) {
            Log.e(TAG, "Error al conectar con Cloudinary: " + e.getMessage());
        }
    }

    /**
     * Método para guardar archivo en el servicio de nube
     * @param archivoUri URI del archivo a subir
     * @param nombreArchivo Nombre que tendrá el archivo en la nube
     * @param callback Callback para manejar la respuesta
     */
    public void guardarArchivo(Uri archivoUri, String nombreArchivo, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Convertir URI a File temporal
                File archivoTemporal = crearArchivoTemporal(archivoUri);

                if (archivoTemporal != null) {
                    // Configurar parámetros de subida
                    Map<String, Object> params = ObjectUtils.asMap(
                            "public_id", nombreArchivo,
                            "folder", "gestion_dinero/comprobantes",
                            "resource_type", "image",
                            "quality", "auto:good",
                            "fetch_format", "auto"
                    );

                    // Subir archivo a Cloudinary
                    Map resultado = cloudinary.uploader().upload(archivoTemporal, params);

                    String urlImagen = (String) resultado.get("secure_url");
                    String publicId = (String) resultado.get("public_id");

                    // Eliminar archivo temporal
                    archivoTemporal.delete();

                    // Ejecutar callback en hilo principal
                    if (callback != null) {
                        mainHandler.post(() -> callback.onSuccess(urlImagen, publicId));
                    }

                    Log.d(TAG, "Archivo guardado exitosamente: " + urlImagen);
                } else {
                    if (callback != null) {
                        mainHandler.post(() -> callback.onError("No se pudo crear archivo temporal"));
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error al guardar archivo: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Error al subir imagen: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Método para obtener un archivo específico del servicio de nube
     * @param publicId ID público del archivo en Cloudinary
     * @param callback Callback para manejar la respuesta
     */
    /*public void obtenerArchivo(String publicId, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Generar URL de descarga optimizada
                String urlDescarga = cloudinary.url()
                        .resourceType("image")
                        .publicId(publicId)
                        .quality("auto:good")
                        .fetchFormat("auto")
                        .generate();

                // Ejecutar callback en hilo principal
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(urlDescarga, publicId));
                }

                Log.d(TAG, "URL de archivo obtenida: " + urlDescarga);

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener archivo: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Error al obtener imagen: " + e.getMessage()));
                }
            }
        });
    }*/
    public void obtenerArchivo(String publicId, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Generar URL de descarga optimizada
                String urlDescarga = cloudinary.url()
                        .resourceType("image")
                        .publicId(publicId)
                        .generate();

                // Ejecutar callback en hilo principal
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(urlDescarga, publicId));
                }

                Log.d(TAG, "URL de archivo obtenida: " + urlDescarga);

            } catch (Exception e) {
                Log.e(TAG, "Error al obtener archivo: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Error al obtener imagen: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Método para descargar archivo y guardarlo en el dispositivo
     * @param publicId ID público del archivo
     * @param nombreArchivo Nombre para guardar en el dispositivo
     * @param callback Callback para manejar la respuesta
     */
    /*public void descargarArchivo(String publicId, String nombreArchivo, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Obtener URL de descarga
                String urlDescarga = cloudinary.url()
                        .resourceType("image")
                        .publicId(publicId)
                        .quality("auto:best")
                        .generate();

                // Ejecutar callback en hilo principal
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(urlDescarga, nombreArchivo));
                }

                Log.d(TAG, "Archivo disponible para descarga: " + urlDescarga);

            } catch (Exception e) {
                Log.e(TAG, "Error al descargar archivo: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Error al descargar: " + e.getMessage()));
                }
            }
        });
    }*/
    public void descargarArchivo(String publicId, String nombreArchivo, CloudinaryCallback callback) {
        executor.execute(() -> {
            try {
                // Obtener URL de descarga
                String urlDescarga = cloudinary.url()
                        .resourceType("image")
                        .publicId(publicId)
                        .generate();

                // Ejecutar callback en hilo principal
                if (callback != null) {
                    mainHandler.post(() -> callback.onSuccess(urlDescarga, nombreArchivo));
                }

                Log.d(TAG, "Archivo disponible para descarga: " + urlDescarga);

            } catch (Exception e) {
                Log.e(TAG, "Error al descargar archivo: " + e.getMessage());
                if (callback != null) {
                    mainHandler.post(() -> callback.onError("Error al descargar: " + e.getMessage()));
                }
            }
        });
    }

    /**
     * Crear archivo temporal desde URI
     */
    private File crearArchivoTemporal(Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File archivoTemporal = new File(context.getCacheDir(), "temp_image_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(archivoTemporal);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return archivoTemporal;

        } catch (IOException e) {
            Log.e(TAG, "Error al crear archivo temporal: " + e.getMessage());
            return null;
        }
    }

    /**
     * Limpiar recursos
     */
    public void limpiarRecursos() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }

    /**
     * Interface para callbacks de Cloudinary
     */
    public interface CloudinaryCallback {
        void onSuccess(String url, String publicId);
        void onError(String error);
    }
}