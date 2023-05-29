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

// Define el pipeline
pipeline {

    agent { label 'production_agent' }

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
    
    stages {

        // Trae el repositorio
        stage('Checkout Característica') {
            steps {
                checkout([
                    $class: 'GitSCM', 
                    branches: ['develop'],
                    userRemoteConfigs: [[url: 'https://github.com/fjroldan/oracle.git']]
                ])
            }  
        }

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
        stage('Ejecutar Scripts') {
            steps {
                script {
                    def secuency_list = params.secuency_list_param.split(',');
                    echo "[INFO]: Aplicando secuencia ${secuency_list}";
                    def fileMap = null;
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
                    dir(params.directory_param) {
                        script {
                            fileMap = listFiles(params.directory_param)
                        }
                    }
                }
            }
        }

    }

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