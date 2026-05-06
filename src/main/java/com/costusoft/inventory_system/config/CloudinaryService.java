package com.costusoft.inventory_system.config;

import com.cloudinary.Cloudinary;
import com.cloudinary.Transformation;
import com.cloudinary.utils.ObjectUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Servicio para gestionar imágenes en Cloudinary.
 *
 * Cada foto de perfil se sube al folder costusoft/avatars con el public_id
 * fijo "user_{id}", de modo que una nueva carga sobreescribe la anterior y
 * no quedan imágenes huérfanas.
 */
@Slf4j
@Service
public class CloudinaryService {

    private static final String FOLDER   = "costusoft/avatars";
    private static final int    WIDTH    = 400;
    private static final int    HEIGHT   = 400;
    private static final long   MAX_SIZE = 5 * 1024 * 1024; // 5 MB

    private final Cloudinary cloudinary;

    public CloudinaryService(
            @Value("${cloudinary.cloud-name}") String cloudName,
            @Value("${cloudinary.api-key}")    String apiKey,
            @Value("${cloudinary.api-secret}") String apiSecret) {

        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key",    apiKey,
                "api_secret", apiSecret,
                "secure",     true));

        log.info("CloudinaryService inicializado — cloud: {}", cloudName);
    }

    /**
     * Sube una imagen de perfil.
     * Transforma la imagen a 400×400 centrada en la cara, optimiza calidad
     * y formato. Sobreescribe la imagen anterior del mismo usuario.
     *
     * @return URL HTTPS de la imagen procesada en Cloudinary
     */
    @SuppressWarnings("unchecked")
    public String subirFotoPerfil(MultipartFile archivo, Long userId) throws IOException {
        validarArchivo(archivo);

        String publicId = FOLDER + "/user_" + userId;

        Transformation transformation = new Transformation()
                .width(WIDTH).height(HEIGHT).crop("fill").gravity("face")
                .chain()
                .quality("auto").fetchFormat("auto");

        Map<String, Object> options = ObjectUtils.asMap(
                "public_id",      publicId,
                "overwrite",      true,
                "invalidate",     true,
                "transformation", transformation);

        Map result = cloudinary.uploader().upload(archivo.getBytes(), options);
        String url = (String) result.get("secure_url");

        log.info("Foto subida a Cloudinary — userId:{} | url:{}", userId, url);
        return url;
    }

    /**
     * Elimina la foto de perfil de Cloudinary.
     * No lanza excepción si la imagen no existe — el estado local ya se limpia.
     */
    public void eliminarFotoPerfil(Long userId) {
        String publicId = FOLDER + "/user_" + userId;
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
            log.info("Foto eliminada de Cloudinary — userId:{}", userId);
        } catch (Exception e) {
            log.warn("No se pudo eliminar foto de Cloudinary userId:{} — {}", userId, e.getMessage());
        }
    }

    // ── Validaciones ───────────────────────────────────────────────────────

    private void validarArchivo(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo no puede estar vacío.");
        }
        if (archivo.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("El archivo no puede superar los 5 MB.");
        }
        String ct = archivo.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("Solo se permiten imágenes (jpg, png, webp).");
        }
    }
}
