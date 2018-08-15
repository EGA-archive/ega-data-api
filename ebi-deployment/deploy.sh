helm upgrade --install config-service ./configApp/

sleep 120

helm upgrade --install mainapp-service ./mainApp/
