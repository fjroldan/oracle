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
def listFiles(String directory) {
    def result = [:]
    script {
        def output = sh(script: "find ${directory} -type f -o -type d", returnStdout: true).trim()
        def files = output.tokenize('\n')
        files.each { file ->
            def fileName = file.replaceFirst(directory, '')
            result.put(fileName, file.endsWith("/") ? 'Directory' : 'File')
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
        )
        string(
            name: 'directory_param',
            description: 'Ingrese la ruta hacia los archivos .sql a aplicar',
            defaultValue: 'project/example'
        )
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
        string(
            name: 'remote_host_param',
            description: 'Ingrese el servidor destino',
            defaultValue: '74.208.211.150'
        )
        string(
            name: 'remote_user_param',
            description: 'Ingrese el usuario remoto',
            defaultValue: 'root'
        )
        string(
            name: 'remote_path_param',
            description: 'Ingrese la ruta destino o espacio de trabajo',
            defaultValue: '/tmp/archivos'
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

        // Se ubica y prepara los scripts
		stage ("Preparacion de los scripts") {
			steps {
				dir(params.directory_param) {
                    script {
                        echo "[INFO]: Preparando scripts";
                        env.FILE_MAP = listFiles("./")
                        print("Mapa de archivos: ${env.FILE_MAP}")
                    }
                }
			}
		} // Fin de preparacion de los scripts.

        // Transfiere los scripts
		stage ("transfiere los scripts") {
            steps {
                dir(params.directory_param) {
                    script {
                        withCredentials([usernamePassword(credentialsId: 'nuevoprod', usernameVariable: 'SSH_USERNAME', passwordVariable: 'SSH_PASSWORD')]) {
                            def remoteHost = params.remote_host_param
                            def remotePath = params.remote_path_param
                            def secuency_list = params.secuency_list_param.split(',')
                            secuency_list.each { directory ->
                                print("[INFO]: Transfiriendo directorio: ${directory}")
                                sh """
                                    sshpass -p \"\${SSH_PASSWORD}\" scp -r ${directory} \${SSH_USERNAME}@${remoteHost}:${remotePath}
                                """
                            }
                        }
                    }
                }
            }
        } // Fin de transferencia de los scripts.

/*
        // Ejecuta los scripts
		stage ("Ejecucion de los scripts") {
            steps {
                script {
                    //withCredentials([sshUserPrivateKey(credentialsId: 'your-credentials-id', keyFileVariable: 'SSH_KEY')]) {
                    //sshagent(['production']) {
                    withCredentials([usernamePassword(credentialsId: 'production', usernameVariable: 'SSH_USERNAME', passwordVariable: 'SSH_PASSWORD')]) {
  
                        def localFile = ""
                        def localFilesList = null
                        def remoteHost = params.remote_host_param
                        def remoteUser = params.remote_user_param
                        def sshKeyFile = "${env.SSH_KEYFILE}"
                        def remotePath = params.remote_path_param
                        for (directory in secuency_list_param) {
                            localFilesList = fileMap[directory]
                            for (localFileName in localFilesList) {
                                localFile = "${params.remote_path_param}/${localFileName}"
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
                }   
            } 
		} // Fin de ejecucion de los scripts.
*/    
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