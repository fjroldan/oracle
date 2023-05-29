# oracle

ssh-keygen -f testet-key.pem -y > testet-key.pub

xclip -sel clip < testet-key.pub

ssh -o IdentitiesOnly=yes -i "testet-key.pem" azureuser@172.176.202.38

sudo -i;
subscription-manager register;
yum -y update;

wget https://releases.jfrog.io/artifactory/artifactory-rpms/artifactory-rpms.repo -O jfrog-artifactory-rpms.repo;
sudo mv jfrog-artifactory-rpms.repo /etc/yum.repos.d/;
sudo yum update && sudo yum install jfrog-artifactory-oss

systemctl start artifactory.service

systemctl status artifactory.service

lsof -i -P -n | grep LISTEN

systemctl status firewalld

firewall-cmd --add-port=8081/tcp --permanent

firewall-cmd --reload

firewall-cmd --add-port=8082/tcp --permanent

firewall-cmd --reload

http://172.176.202.38:8082/ui/login/


[Plugin]
https://plugins.jenkins.io/jfrog/
a


[Funciona]
1. Se crea la app en git hub 
settings>desarrollador>add app
2. Se habilita deviace
3. se ponen los permisos
4. Se genera el .pem
5. se ejecuta generate-key


[Python]
pip install jwt

[Sonarqube]
ssh -i testet-key.pem azureuser@20.65.10.53
