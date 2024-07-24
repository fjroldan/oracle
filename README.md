# Proyecto Oracle

Proyecto que integra tecnologías avanzadas para crear una extensión personalizada de SonarQube, diseñada para validar reglas de scripts SQL específicos para Oracle. El proyecto está basado en Java utilizando Spring Boot para el desarrollo del backend, lo que proporciona una arquitectura robusta y escalable. Además, implementamos Python para scripts de automatización y pruebas, facilitando la integración y el manejo de datos. Jenkins se utiliza para la implementación de DevOps, asegurando un flujo continuo de integración y entrega (CI/CD), mientras que SonarQube se encarga de la revisión de código y la calidad del software. La extensión propuesta permitirá a los equipos de desarrollo verificar automáticamente el cumplimiento de las mejores prácticas y estándares en sus scripts SQL para Oracle, mejorando la calidad y consistencia del código en sus proyectos. Este enfoque no solo automatiza el proceso de validación, sino que también optimiza la eficiencia del desarrollo y la integración de bases de datos Oracle.

## Estructura del Proyecto

La estructura del proyecto incluye herramientas y scripts para desarrollar una extensión de SonarQube que valida scripts SQL para Oracle. En la carpeta `build`, se encuentran el script de despliegue (`deploy.sh`) y el archivo de configuración de Docker Compose (`docker-compose.yml`). El directorio `src` contiene el código fuente en Groovy, incluyendo el `PRManager.groovy` para gestionar pull requests. `sonarqube` incluye el plugin de SonarQube (`sonar-sql-plugin-1.3.0.jar`). Los scripts en `project` y `vars` permiten ejecutar y probar scripts SQL, mientras que `README.md` proporciona la documentación del proyecto.

```
├── build
│   ├── deploy.sh              # Script de despliegue que automatiza la implementación de la aplicación en el entorno de producción o pruebas.
│   └── docker-compose.yml     # Archivo de configuración para Docker Compose, que define y ejecuta los contenedores necesarios para el proyecto.
├── exp.py                     # Script en Python para la automatización de tareas, pruebas o análisis relacionado con la extensión de SonarQube.
├── Oracle_CD.groovy           # Script Groovy para la integración continua y el despliegue continuo (CD) de scripts SQL para Oracle.
├── Oracle_CI.groovy           # Script Groovy para la integración continua (CI), configurando la validación y pruebas de scripts SQL.
├── oracle_ejecutarscripts_ci.groovy # Script Groovy que ejecuta scripts SQL como parte del proceso de integración continua, asegurando la calidad del código.
├── oracle_ejecutarscripts.groovy     # Script Groovy para ejecutar scripts SQL en Oracle, que se utiliza en diferentes fases del desarrollo y despliegue.
├── pl-pr.groovy               # Script Groovy para la gestión de pull requests y el análisis de calidad del código en los cambios de scripts SQL.
├── project
│   ├── example
│   │   ├── com-a
│   │   │   ├── 1-query.sql     # Archivo SQL que contiene un conjunto de consultas para pruebas y validación de la extensión de SonarQube.
│   │   │   └── 2-query.sql     # Archivo SQL adicional para pruebas, asegurando que diferentes tipos de consultas sean validadas correctamente.
│   │   └── com-b
│   │       └── 3-query.sql     # Archivo SQL con más consultas para la validación exhaustiva de la extensión de SonarQube.
│   ├── sonar-project.properties # Archivo de configuración de SonarQube que define los parámetros y propiedades del proyecto para el análisis de calidad del código.
│   └── sonarqube-test.sh       # Script de prueba para ejecutar análisis de SonarQube y verificar que la extensión funcione correctamente.
├── README.md                  # Documento de documentación principal que proporciona una descripción general del proyecto, instrucciones de configuración y uso.
├── sonarqube
│   └── sonar-sql-plugin-1.3.0.jar # Archivo JAR del plugin de SonarQube que implementa la validación de reglas específicas para scripts SQL en Oracle.
├── src
│   └── com
│       └── avv
│           └── PRManager.groovy  # Código fuente en Groovy para gestionar las solicitudes de extracción (pull requests) y realizar análisis relacionados.
└── vars
    └── hackyWorkaround.groovy  # Script Groovy que contiene soluciones temporales o "hacky" para problemas específicos en la integración y despliegue.

```

## Despligue


El despliegue del proyecto sigue estos pasos detallados:

1. **Preparación del Entorno**: Configura el entorno de desarrollo utilizando Docker. Ejecuta `docker-compose.yml` para levantar los contenedores necesarios, que incluyen la base de datos Oracle y el servidor de SonarQube. Esto crea un entorno controlado y reproducible para la prueba y validación de la extensión.
2. **Construcción y Pruebas**: Compila y construye el proyecto. Utiliza `gradle` o `maven` (según la configuración) para generar el archivo JAR de la extensión de SonarQube. Asegúrate de que el archivo JAR se coloque en la carpeta `sonarqube`. Corre el script de pruebas `sonarqube-test.sh` para validar que la extensión funciona correctamente y cumple con las reglas definidas.
3. **Despliegue de la Extensión**: Copia el archivo JAR (`sonar-sql-plugin-1.3.0.jar`) a la carpeta de plugins de SonarQube dentro del contenedor. Reinicia el servidor de SonarQube para cargar la nueva extensión.
4. **Configuración y Validación**: Configura SonarQube para utilizar la nueva extensión siguiendo las directrices en `sonar-project.properties`. Ejecuta análisis de calidad utilizando `sonar-scanner` para asegurar que los scripts SQL son validados correctamente según las reglas definidas.
5. **Automatización y Mantenimiento**: Usa Jenkins para automatizar el proceso de integración y despliegue continuo, configurando pipelines en `Oracle_CI.groovy` y `Oracle_CD.groovy`. Estos scripts gestionan la integración continua y el despliegue continuo, garantizando un flujo de trabajo ágil y eficiente.

Este enfoque asegura un despliegue ordenado y controlado, facilitando el desarrollo y la validación continua de la extensión de SonarQube.
