# Build
Java 11, maven
```
mvn clean package
```

# Install
Install as a systemd service
```
mvn clean package
cd ansible
ansible-playbook -i hosts install.yml
```

# Firewall rule

```shell
sudo ufw allow proto udp from any port 4445
```
