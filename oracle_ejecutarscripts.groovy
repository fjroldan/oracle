// 
// ------------------------------------------------------------
// - Todos los derechos reservados 2023                       -
// - Banco AV Villas                                          -
// - $ oracle_inf_ejecutarscripts pipeline script             -
// ------------------------------------------------------------
// Componente que define el pipeline de aplicacion de scripts
// SQL en base de datos de Oracle.
// @autor Equipo IaC - Vass Latam
// @version 0.0.1.0
// @date 13/04/23
//
// Define el pipeline
pipeline {
	// Define el agente
    agent { label 'production_agent' }
    // Define variables de entorno
    // ...
    // Define parametros
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
        )
    }
    // Define variables
    script {
        def secuency_list = params.secuency_list_param.split(',');
        echo "[INFO]: Aplicando secuencia ${secuency_list}";
        def fileMap = null;
    }
    // Define funciones utilitarias
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
    // Define los stages
	stages {
        // Define el setup stage
        // ...
        /*
        // Obtiene las fuentes de los scripts
        stage('Git checkout del repo. proyecto'){
            steps{
            //    git branch: "${params.RELEASE_TAG}", credentialsId: 'github_credentials_app', url: "${params.REPO_PATH}"
                checkout([$class: 'GitSCM', branches: [[name: "refs/tags/${params.RELEASE_TAG}"]], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: false, recursiveSubmodules: false, reference: '', trackingSubmodules: false]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: "github_credentials_app", url: "${params.REPO_PATH}"]]])
            }  
        }
        */
        // Se ubica y prepara los scripts
		stage ("Preparacion de los scripts") {
			steps {
				dir(params.directory_param) {
                    script {
                        fileMap = listFiles(params.directory_param)
                    }
                }
			}
		}
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
        }
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
		}
    }
    // Limpia el espacio de trabajo
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