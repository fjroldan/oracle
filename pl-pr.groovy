//--------------------------------------------------------
// Importa paquetes
//--------------------------------------------------------

@Library('pr-manager-library') _
import com.avv.PRManager

//--------------------------------------------------------
// Define funciones
//--------------------------------------------------------

/**
 * Evalua el Pull Request asociado he identifica el SHA.
 */
def checkPR() {
    def prManager = new PRManager()
    def jwtToken = prManager.generateJwtToken("/root/jenkins/keys/rest-key.2023-04-19.private-key.pem", "321135")
    def token = prManager.getToken("36625786", jwtToken)
    def owner = 'fjroldan'
    def repo = 'oracle'
    def branch = 'feature/test_sonar'
    def target = 'develop'
    def prID = 0
    def commit_sha = ''
    def prData = null
    if (prID == 0) {
        prData = prManager.createPR(owner, repo, token, branch, target)
    } else {
        prData = [
            "prID": prID,
            "commit_sha": commit_sha
        ]
    }
    def msg = "Se aborta el pipeline por error en verificción del Pull Request"
    if (prData) {
        echo "[INFO]: Datos del Pull Request: ${prData}"
        def shaRes = prManager.waitConflicts(owner, repo, prData.prID, token)
        if (shaRes) {
            echo "[INFO]: SHA de trabajo: ${shaRes}"
            env.COMMIT_SHA = shaRes
            env.OWNER = owner
            env.REPO = repo
            env.TOKEN = token
            env.PR_ID = prData.prID
            echo "[INFO]: Inicio de fase de construcción con el SHA: ${shaRes}"
            gitHubCheckNotify(prManager, owner, repo, "pending", shaRes, token)
        } else {
            error(msg)
        }
    } else {
        error(msg)
    }
}

/**
 * Evalua el Pull Request asociado he identifica el SHA.
 */
def gitHubCheckNotify(prManager, owner, repo, state, shaRes, token) {
    def notifyRes = prManager.notifyCheck(owner, repo, state, shaRes, token)
    if (notifyRes) {
        echo "[INFO]: Se notifico con éxito el update status del check(continuous-integration) sobre SHA(${shaRes})"
    } else {
        echo "[ERROR]: Se presentó un error del update status del check(continuous-integration) sobre SHA(${shaRes})"
    }
}

//--------------------------------------------------------
// Define el pipeline
//--------------------------------------------------------

pipeline {

    agent { label 'production_agent' }

    tools {
        jfrog 'jfrog-cli'
    }
    
    stages {
        //------------------------------------------------
        // Define la fase de validación del pull request
        // asociado.
        stage('Validación PR') {
            steps {
                checkPR()
            }  
        }
        //------------------------------------------------
        // Define la fase checkout a la rama 
        // caracteristica.
        stage('Checkout Característica') {
            steps {
                checkout([
                    $class: 'GitSCM', 
                    branches: [[name: env.COMMIT_SHA ]],
                    userRemoteConfigs: [[url: 'https://github.com/fjroldan/oracle.git']]
                ])
            }  
        }
        //------------------------------------------------
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
        }

        //------------------------------------------------
        // Define la fase de validacion del escaneo.
        stage('Quality Gate') {
            steps {
                timeout(time: 10, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
                script {
                    def shaRes = env.COMMIT_SHA
                    def owner = env.OWNER
                    def repo = env.REPO
                    def token = env.TOKEN
                    if (shaRes) {
                        def prManager = new PRManager()
                        echo "[INFO]: Fase de construcción del SHA(${shaRes}) terminada con éxito"
                        gitHubCheckNotify(prManager, owner, repo, "success", shaRes, token)
                    }
                }
            }
        }

        //------------------------------------------------
        // Define la fase de validacion del escaneo.
        stage('Construccion artefactos') {
            steps {
                script {
                    def shaRes = env.COMMIT_SHA
                    def owner = env.OWNER
                    def repo = env.REPO
                    def token = env.TOKEN
                    def prID = env.PR_ID
                    if (shaRes) {
                        def prManager = new PRManager()
                        echo "[INFO]: Validando cierre de PR ${prID}..."
                        prManager.getClosePR(owner, repo, prID, token)
                        sh "tar -czvf sql_oracle_build${env.BUILD_ID}.tar.gz project/example"
                    }
                }
            }
        }

        //------------------------------------------------
        // Define la fase de validacion del escaneo.
        stage('Carga Artefatory') {
            steps {
                jf "rt u sql_oracle_build${env.BUILD_ID}.tar.gz bavv-oracle-dev-local/ --url=http://172.176.202.38:8082/artifactory/ --user admin --password 814852Jfrog#"
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