docker exec sonarqube rm /opt/sonarqube/extensions/plugins/sonar-sql-plugin-1.3.0.jar;
docker cp /wsp/projects/vass/avv/exp/sonarqube/sonar-sql-plugin/src/sonar-sql-plugin/target/sonar-sql-plugin-1.3.0.jar 4ee381bf1e85:/opt/sonarqube/extensions/plugins;
docker-compose stop;
docker-compose start;