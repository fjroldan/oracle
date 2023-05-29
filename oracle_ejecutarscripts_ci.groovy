pipeline {
    agent { label 'production_agent' }
    
    parameters {
        string(
            name: 'directory',
            description: 'Ingrese la ruta hacia los archivos .sql a aplicar',
            defaultValue: 'project/example'
        )
    }

    tools {
        jfrog 'jfrog-cli'
    }
    
    stages {
        stage('Compress SQL files') {
            steps {
                // Compress the SQL files with gzip
                sh "tar -czvf sql.tar.gz ${params.directory}"
            }
        }
        
        stage('Upload compressed SQL files to JFrog') {
            steps {
                jf 'rt u sql.tar.gz bavv-oracle-dev-local/ --url=http://172.176.202.38:8082/artifactory/ --user admin --password 814852Jfrog#'
            }
        }
    }
}