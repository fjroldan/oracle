// 
// ------------------------------------------------------------
// - Todos los derechos reservados 2023                       -
// - Banco AV Villas                                          -
// - $ Oracle_CD pipeline script                              -
// ------------------------------------------------------------
// Componente que define el pipeline de aplicacion de scripts
// SQL en base de datos de Oracle.
// @autor Equipo - Vass Company
// @version 0.0.1.0
// @date 29/05/23
//
// Define el pipeline

/** 
 * Define funcion utilitaria para listar archivos.
 * @param dir Directorio a listar.
 * @return Mapa de archivos.
 */
def listFiles(File dir) {
    def result = [:]
    dir.eachFile { file ->
        if (file.isDirectory()) {
            result.put(file.getName(), listFiles(file))
        } else {
            result.put(file.getName(), "File")
        }
    }
    return result
}

// Define el pipeline.
pipeline {

    // Define el agente.
    agent { label 'production_agent' }

    // Define parametros.
    parameters {
        string(
            name: 'secuency_list_param',
            description: 'Ingrese la secuencia de ejecución de los nombres de las carpetas separadas por comas',
            defaultValue: 'com-a,com-b'
        ),
        string(
            name: 'directory_param',
            description: 'Ingrese la ruta hacia los archivos .sql a aplicar',
            defaultValue: 'project/example'
        ),
        string(
            name: 'repo_param',
            description: 'Ingrese la URL del repositorio a aplicar',
            defaultValue: 'https://github.com/fjroldan/oracle.git'
        ),
        string(
            name: 'branch_param',
            description: 'Ingrese la rama del repositorio a aplicar',
            defaultValue: 'main'
        )
    }

    // Define variables.
    script {
        def secuency_list = params.secuency_list_param.split(',');
        echo "[INFO]: Aplicando secuencia ${secuency_list}";
        def fileMap = null;
    }
    
    stages {

        // Trae el repositorio.
        stage('Checkout Característica') {
            steps {
                checkout([
                    $class: 'GitSCM', 
                    branches: ['${params.branch_param}'],
                    userRemoteConfigs: [[url: '${params.repo_param}']]
                ])
            }  
        } // Fin de traer el repositorio.

        // Se ubica y prepara los scripts
		stage ("Preparacion de los scripts") {
			steps {
				dir(params.directory_param) {
                    script {
                        fileMap = listFiles(params.directory_param)
                    }
                }
			}
		} // Fin de preparacion de los scripts.

        // Transfiere los scripts
		stage ("transfiere los scripts") {
            withCredentials([sshUserPrivateKey(credentialsId: 'ssh-creds', keyFileVariable: 'SSH_KEYFILE', passphraseVariable: '', usernameVariable: 'SSH_USER')]) {
                def dir = ""
                def remoteHost = 'your.remote.host.com'
                def remoteUser = "${env.SSH_USER}"
                def sshKeyFile = "${env.SSH_KEYFILE}"
                def remotePath = '/remote/path/to/destination'
                for (directory in secuency_list_param) {
                    dir = "/local/path/to/${directory}"
                    sh "scp -i ${sshKeyFile} ${dir} ${remoteUser}@${remoteHost}:${remotePath}"
                }
            }
        } // Fin de transferencia de los scripts.

        // Ejecuta los scripts
		stage ("Ejecucion de los scripts") {
			withCredentials([sshUserPrivateKey(credentialsId: 'ssh-creds', keyFileVariable: 'SSH_KEYFILE', passphraseVariable: '', usernameVariable: 'SSH_USER')]) {
                def localFile = ""
                def localFilesList = null
                def remoteHost = 'your.remote.host.com'
                def remoteUser = "${env.SSH_USER}"
                def sshKeyFile = "${env.SSH_KEYFILE}"
                def remotePath = '/remote/path/to/destination'
                for (directory in secuency_list_param) {
                    localFilesList = fileMap[directory]
                    for (localFileName in localFilesList) {
                        localFile = "/local/path/to/${localFileName}"
                        try {
                            //ssh remote: "${remoteUser}@${remoteHost}", command: "cat ${remotePath}/${localFile} | sqlplus scott/tiger@orcl"
                            //ssh remote: "${remoteUser}@${remoteHost}", command: "sqlplus -S scott/tiger@orcl << EOF\n$(cat ${remotePath}/${localFile})\nEOF"
                            //sh "ssh ${remoteUser}@${remoteHost} 'cat ${remotePath}/${localFile} | sqlplus -S scott/tiger@orcl'"
                        
                            def sqlResult = sh (
                                script: """
                                    ssh ${remoteUser}@${remoteHost} 'sqlplus -S ${DB_USER}/${DB_PASS}@orcl' << EOF
                                    \$(cat /path/to/sql/file.sql)
                                    EOF
                                """,
                                returnStdout: true
                            )
                            stash name: 'sql-result', includes: 'sql-result.txt'
                            writeFile file: 'sql-result.txt', text: sqlResult
                        } catch (err) {
                            error "Error running SQL file: ${err}"
                            //...
                        }
                    }
                }
            } 
		} // Fin de ejecucion de los scripts.
    
    } // Fin de stages.

    post {
        always {
            cleanWs(cleanWhenNotBuilt: false,
                    deleteDirs: true,
                    disableDeferredWipeout: true,
                    notFailBuild: true,
                    patterns: [])
        }
    }
}