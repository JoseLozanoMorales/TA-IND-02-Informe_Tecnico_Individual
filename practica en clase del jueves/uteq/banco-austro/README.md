# Práctica de Bases de Datos Distribuidas - Banco del Austro

Prototipo académico de una base de datos distribuida para las oficinas de Cuenca, Quito y Guayaquil del Banco del Austro. El proyecto utiliza tres nodos PostgreSQL independientes, ejecutados con Docker, y una aplicación Spring Boot que determina en qué nodo consultar cada cuenta.

## Objetivo

La práctica demuestra las siguientes propiedades de una base de datos distribuida:

- Fragmentación horizontal de las tablas `cuentas` y `transacciones` por oficina.
- Consulta de los clientes almacenados en todos los nodos disponibles.
- Transparencia de ubicación mediante un único servicio REST.
- Autonomía local: Cuenca continúa atendiendo sus consultas si Quito deja de funcionar, y viceversa.
- Persistencia independiente mediante un volumen Docker para cada nodo.

## Arquitectura

| Componente | Dirección | Función |
|---|---|---|
| PostgreSQL Cuenca | `localhost:5442` | Almacena las cuentas cuyo número empieza con `22` |
| PostgreSQL Quito | `localhost:5443` | Almacena las cuentas cuyo número empieza con `17` |
| PostgreSQL Guayaquil | `localhost:5444` | Almacena las cuentas cuyo número empieza con `09` |
| Aplicación Spring Boot | `localhost:8080` | Expone la API y enruta cada consulta |

Los tres motores PostgreSQL escuchan internamente en el puerto `5432`. Los puertos `5442`, `5443` y `5444` corresponden a su publicación en la computadora anfitriona. Esta asignación evita conflictos con una instalación local de PostgreSQL.

## Tecnologías

- Java 21
- Spring Boot 3.4.12
- Spring Web
- Spring JDBC
- PostgreSQL 16 Alpine
- Docker Compose
- Maven

## Estructura del proyecto

```text
banco-austro/
|-- docker-compose.yml
|-- pom.xml
|-- sql-cuenca/
|   |-- 01_schema.sql
|   `-- 02_datos.sql
|-- sql-quito/
|   |-- 01_schema.sql
|   `-- 02_datos.sql
|-- sql-guayaquil/
|   |-- 01_schema.sql
|   `-- 02_datos.sql
`-- src/
    `-- main/
        |-- java/ec/edu/uteq/bancoaustro/
        |   |-- BancoAustroApplication.java
        |   |-- config/DataSourceConfig.java
        |   |-- controller/ApiExceptionHandler.java
        |   |-- controller/BancoController.java
        |   |-- controller/TransferenciaRequest.java
        |   `-- service/ConsultaDistribuidaService.java
        `-- resources/application.yml
```

## Requisitos

Antes de ejecutar la práctica se necesita:

1. Docker Desktop iniciado.
2. JDK 21 configurado en IntelliJ IDEA.
3. Maven, ya sea instalado en el sistema o integrado en IntelliJ IDEA.
4. Los puertos `5442`, `5443`, `5444` y `8080` disponibles.

## Ejecución

### 1. Levantar los nodos PostgreSQL

Desde una terminal abierta en la raíz del proyecto:

```powershell
docker compose up -d
docker compose ps
```

El segundo comando debe mostrar los contenedores `banco-cuenca`, `banco-quito` y `banco-guayaquil` en estado `Up`.

Los scripts de las carpetas `sql-cuenca` y `sql-quito` se ejecutan automáticamente cuando se crean por primera vez los volúmenes.

### 2. Ejecutar la aplicación

En IntelliJ IDEA:

1. Abrir el proyecto `banco-austro`.
2. Seleccionar JDK 21 como SDK del proyecto.
3. Recargar el proyecto Maven.
4. Abrir `BancoAustroApplication.java`.
5. Ejecutar el método `main`.
6. Esperar el mensaje `Tomcat started on port 8080`.

También se puede ejecutar desde una terminal que tenga Maven disponible:

```powershell
mvn spring-boot:run
```

## Endpoints

### Consultar una cuenta de Cuenca

```http
GET http://localhost:8080/api/banco/saldo/2201000001
```

Respuesta esperada:

```json
{
  "numero": "2201000001",
  "saldo": 15000.00,
  "oficina": "CUENCA"
}
```

### Consultar una cuenta de Quito

```http
GET http://localhost:8080/api/banco/saldo/1701000001
```

Respuesta esperada:

```json
{
  "numero": "1701000001",
  "saldo": 8000.00,
  "oficina": "QUITO"
}
```

### Consultar todos los clientes

```http
GET http://localhost:8080/api/banco/clientes
```

La respuesta contiene la lista de clientes disponibles, el total recuperado, los nodos no disponibles y una advertencia cuando la consulta es parcial. Con los tres nodos activos debe devolver nueve clientes y una lista vacía de nodos no disponibles.

### Consultar una cuenta de Guayaquil

```http
GET http://localhost:8080/api/banco/saldo/0901000001
```

La respuesta esperada indica un saldo de `12500.00` y la oficina `GUAYAQUIL`.

### Realizar una transferencia

```http
POST http://localhost:8080/api/banco/transferencia
Content-Type: application/json
```

Ejemplo local en Cuenca:

```json
{
  "origen": "2201000001",
  "destino": "2201000002",
  "monto": 25.00
}
```

Ejemplo entre Quito y Guayaquil:

```json
{
  "origen": "1701000001",
  "destino": "0901000001",
  "monto": 20.00
}
```

Las transferencias locales se ejecutan dentro de una transacción del nodo. En transferencias entre sedes se debita el origen y se acredita el destino; si el segundo paso falla, la aplicación intenta compensar automáticamente el débito. Este mecanismo es didáctico y no sustituye un protocolo de commit en dos fases.

## Prueba de autonomía local

Con la aplicación en ejecución, detener temporalmente el nodo Quito:

```powershell
docker stop banco-quito
```

Durante la interrupción:

- La consulta de la cuenta `2201000001` debe continuar respondiendo porque pertenece a Cuenca.
- La consulta de la cuenta `1701000001` debe fallar porque el nodo Quito está detenido.
- Con el fallback incorporado, la consulta de clientes ya no falla completamente: devuelve los clientes de los nodos disponibles junto con una advertencia.

Restaurar Quito al finalizar la prueba:

```powershell
docker start banco-quito
docker compose ps
```

## Detener el ambiente

Para detener los contenedores conservando los datos:

```powershell
docker compose down
```

Para eliminar también los volúmenes y reiniciar completamente las bases de datos:

```powershell
docker compose down -v
```

> El segundo comando elimina los datos almacenados. Al levantar nuevamente el ambiente, los scripts SQL se ejecutarán desde cero.

## Credenciales académicas

| Propiedad | Valor |
|---|---|
| Usuario | `banco` |
| Contraseña | `banco123` |
| Base de datos Cuenca | `banco_cuenca` |
| Base de datos Quito | `banco_quito` |
| Base de datos Guayaquil | `banco_guayaquil` |

Estas credenciales se utilizan únicamente con fines didácticos y no son apropiadas para un sistema de producción.

## Informe en LaTeX

El informe técnico realizado se encuentra en `informe/informe_practica.tex` e incorpora las ocho evidencias obtenidas durante la implementación y validación de la práctica. Puede compilarlo localmente con una distribución LaTeX o importalo directamente en Overleaf.
