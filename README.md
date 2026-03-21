# CostuSoft Backend Core 🧵👔

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/License-Confidential-red)

**CostuSoft** es una plataforma SaaS de vanguardia diseñada específicamente para la gestión y control de inventario en empresas de confección de uniformes escolares. Este repositorio contiene el `inventario-api`, el núcleo del sistema que gestiona la lógica de negocio, seguridad y orquestación de servicios.

## 🌟 Características Principales

- [cite_start]**Control de Ciclo Completo:** Gestión desde la recepción de insumos hasta el despacho de uniformes terminados[cite: 18].
- [cite_start]**Arquitectura Robusta:** Implementación modular por capas (Controller, Service, Repository, DTO, Mapper)[cite: 44, 48].
- [cite_start]**Seguridad Avanzada:** Autenticación stateless mediante JWT con algoritmos HMAC-SHA512 y rotación de Refresh Tokens[cite: 73, 75, 85].
- [cite_start]**Calculadora de Disponibilidad:** Algoritmo de precisión con `BigDecimal` para determinar la viabilidad de producción según stock actual[cite: 130, 137].
- [cite_start]**Integración ML:** Conectividad nativa con un microservicio de Python para predicción de agotamiento de stock[cite: 31, 160].

## 🛠️ Stack Tecnológico

- [cite_start]**Lenguaje:** Java 17 / 21[cite: 10, 29].
- [cite_start]**Framework:** Spring Boot 3.3[cite: 10].
- [cite_start]**Persistencia:** Spring Data JPA + Hibernate[cite: 220].
- [cite_start]**Base de Datos:** PostgreSQL 16 (Alojada en Supabase)[cite: 10, 49].
- [cite_start]**Documentación:** Swagger UI / OpenAPI 3.0[cite: 220].
- [cite_start]**Mapeo:** MapStruct para conversión eficiente Entity-DTO[cite: 48, 220].

## 📐 Arquitectura del Sistema

El sistema sigue un flujo de comunicación moderno y seguro:

1.  [cite_start]**NextJS → Spring Boot:** Comunicación REST con Bearer Token[cite: 38].
2.  [cite_start]**Spring Boot → Python:** Llamadas internas vía `RestClient` con `X-API-Token` secreto[cite: 39, 162].
3.  [cite_start]**Persistencia:** Uso de HikariCP para gestión de conexiones hacia Supabase[cite: 40, 51].

## 🚀 Guía de Inicio Rápido

### Requisitos Previos

- JDK 21
- Maven 3.9+
- Instancia de PostgreSQL o Supabase

### Configuración

1.  Clona el repositorio.
2.  [cite_start]Configura tus variables de entorno en `src/main/resources/application-dev.properties`[cite: 228].
3.  Ejecuta el proyecto:
    ```bash
    mvn spring-boot:run -Dspring-boot.run.profiles=dev
    ```

### Acceso por Defecto (Desarrollo)

- **Admin:** `admin` / `admin123`
- **Usuario:** `usuario` / `usuario123`
  > [cite_start]⚠️ **Nota:** Cambiar credenciales antes de desplegar a producción[cite: 255].

## 📊 Módulos de Negocio

| Módulo               | Responsabilidad                                                                                         |
| :------------------- | :------------------------------------------------------------------------------------------------------ |
| **Auth**             | [cite_start]Gestión de ciclo de vida JWT y roles (ADMIN/USER)[cite: 83, 87].                            |
| **Insumos**          | [cite_start]Control de stock y niveles de riesgo (BAJO a CRÍTICO)[cite: 93, 96].                        |
| **Entradas/Salidas** | [cite_start]Operaciones atómicas con rollback automático para integridad de stock[cite: 118, 126, 244]. |
| **Reportes**         | [cite_start]Generación dinámica de PDF (OpenPDF) y Excel (Apache POI)[cite: 153].                       |

---

[cite_start]© 2026 CostuSoft - Documentación Técnica v1.0.0[cite: 4, 8].
