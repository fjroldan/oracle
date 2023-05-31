// 
// ------------------------------------------------------------
// - Todos los derechos reservados 2023                       -
// - Banco AV Villas                                          -
// - $ Oracle_CI pipeline script                              -
// ------------------------------------------------------------
// Componente que define el pipeline de aplicacion de scripts
// SQL en base de datos de Oracle.
// @autor Equipo - Vass Company
// @version 0.0.1.0
// @date 31/05/23
//

// Define el pipeline.
pipeline {

    // Define el agente.
    agent { label 'production_agent' }

    // Define parametros.
    parameters {
        string(
            name: 'repo_param',
            description: 'Ingrese la URL del repositorio a aplicar',
            defaultValue: 'https://github.com/fjroldan/oracle.git'
        )
        string(
            name: 'branch_param',
            description: 'Ingrese la rama del repositorio a aplicar',
            defaultValue: 'main'
        )
    }

    stages {

        // Trae el repositorio.
        stage('Checkout') {
            steps {
                // Trae el repositorio.
                git branch: "${params.branch_param}", url: "${params.repo_param}"
            }  
        } // Fin de traer el repositorio.

        // Define la fase escaneo con SonarQube.
        stage('Validación ZonarQube') {
            tools {
                jdk "jdk17"
            }
            steps {
                script {                                           
                    def scannerHome = tool 'testsonar'
                    withSonarQubeEnv() {
                        sh "${scannerHome}/bin/sonar-scanner -X"
                    }                    
                }
            }
        } // Fin de la fase escaneo con SonarQube.

        // Define la fase de validacion del escaneo.
        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        } // Fin de la fase de validacion del escaneo.

    } // Fin de stages.

    // Define post acciones.
    post {
        always {
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [])
        }
    } // Fin de post acciones.

} // Fin de pipeline.