# EBI-deployment

Helm chart to deploy EGA-DATA-API to any Kubernetes cluster.

## Table of Contents

- [Prerequisites](#Prerequisites)
- [Installing the Chart](#Installing-the-Chart)
- [Uninstalling the Chart](#Uninstalling-the-Chart)

## Prerequisites

Before deploying the Helm charts `Helm` should be installed. Please follow [these ](https://docs.helm.sh/using_helm#install-helm) instructions to install `Helm`.

Then you should also have a running Kubernetes cluster and configured correctly. You may follow [these](https://kubenow.readthedocs.io/en/stable/getting_started/bootstrap.html#deploy-on-openstack) instructions to create a Kubernetes cluster using the tool KubeNow. It is recommended to create a node with preferably 16GB of RAM so that this Helm Chart can be deployed smoothly.

Please run

    helm ls

to check if your Kubernetes cluster is correctly configured. If so, you should not see any error message from the above command.

Finally, you should also label your node as `group1` by 

    kubectl label nodes <node-name> group=group1

You can get the `<node-name>` by the following command

    kubectl get nodes

## Installing the Chart

The Helm chart can be deployed as follows

First deploy the `config-service`

    helm upgrade --install config-service ./configApp/

Wait about two minutes until the Pods `config` and `postgres` are ready. Then deploy the `mainapp` by

    helm upgrade --install mainapp-service ./mainApp/

After that, you can check the status of the Pods by the following command 

    kubectl get pods

The status of all Pods should be `Running` on successful deployment

After that, you can get the Ports and Accessing point of all services by 

    kubectl get svc

The microservices can be accessed by the URL `http://<ip-of-master-node>:<port-number>`


## Uninstalling the Chart

    helm del --purge config-service mainapp-service

