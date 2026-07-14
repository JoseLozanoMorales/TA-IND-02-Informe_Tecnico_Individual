# Práctica de Bases de Datos Distribuidas - Banco del Austro

Prototipo académico de una base de datos distribuida para las oficinas de Cuenca y Quito del Banco del Austro. El proyecto utiliza dos nodos PostgreSQL independientes, ejecutados con Docker, y una aplicación Spring Boot que determina en qué nodo consultar cada cuenta.

## Objetivo

La práctica demuestra las siguientes propiedades de una base de datos distribuida:

- Fragmentación horizontal de las tablas `cuentas` y `transacciones` por oficina.
- Consulta de los clientes almacenados en ambos nodos.
- Transparencia de ubicación mediante un único servicio REST.
- Autonomía local: Cuenca continúa atendiendo sus consultas si Quito deja de funcionar, y viceversa.
- Persistencia independiente mediante un volumen Docker para cada nodo.

## Arquitectura

| Componente | Dirección | Función |
|---|---|---|
| PostgreSQL Cuenca | `localhost:5442` | Almacena las cuentas cuyo número empieza con `22` |
| PostgreSQL Quito | `localhost:5443` | Almacena las cuentas cuyo número empieza con `17` |
| Aplicación Spring Boot | `localhost:8080` | Expone la API y enruta cada consulta |

Los dos motores PostgreSQL escuchan internamente en el puerto `5432`. Los puertos `5442` y `5443` corresponden a su publicación en la computadora anfitriona. Esta asignación sigue la alternativa recomendada por la guía cuando el puerto `5432` ya está ocupado por una instalación local de PostgreSQL.

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
`-- src/
    `-- main/
        |-- java/ec/edu/uteq/bancoaustro/
        |   |-- BancoAustroApplication.java
        |   |-- config/DataSourceConfig.java
        |   |-- controller/BancoController.java
        |   `-- service/ConsultaDistribuidaService.java
        `-- resources/application.yml
```

## Requisitos

Antes de ejecutar la práctica se necesita:

1. Docker Desktop iniciado.
2. JDK 21 configurado en IntelliJ IDEA.
3. Maven, ya sea instalado en el sistema o integrado en IntelliJ IDEA.
4. Los puertos `5442`, `5443` y `8080` disponibles.

## Ejecución

### 1. Levantar los nodos PostgreSQL

Desde una terminal abierta en la raíz del proyecto:

```powershell
docker compose up -d
docker compose ps
```

El segundo comando debe mostrar los contenedores `banco-cuenca` y `banco-quito` en estado `Up`.

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

La respuesta esperada es un arreglo con seis clientes: tres procedentes de Cuenca y tres de Quito.

## Prueba de autonomía local

Con la aplicación en ejecución, detener temporalmente el nodo Quito:

```powershell
docker stop banco-quito
```

Durante la interrupción:

- La consulta de la cuenta `2201000001` debe continuar respondiendo porque pertenece a Cuenca.
- La consulta de la cuenta `1701000001` debe fallar porque el nodo Quito está detenido.
- La consulta conjunta de clientes también debe fallar porque intenta acceder a ambos nodos.

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

Estas credenciales se utilizan únicamente con fines didácticos y no son apropiadas para un sistema de producción.

## Informe en LaTeX

El informe técnico realizado se encuentra en `informe/informe_practica.tex` e incorpora las ocho evidencias obtenidas durante la implementación y validación de la práctica. Puede compilarlo localmente con una distribución LaTeX o importalo directamente en Overleaf.
