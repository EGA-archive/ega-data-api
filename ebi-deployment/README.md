# EBI-deployment

Helm chart to deploy EGA-DATA-API to any Kubernetes cluster.

## Table of Contents

- [Prerequisites](#Prerequisites)
- [Installing the Chart](#Installing-the-Chart)
    - [Configuration](#Chart-configuration)
    - [Deployment](#Chart-deployment)
- [Retrieving the data](#Retrieving-the-data)
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

### Configuration

You will need to set up the `s3` backend in order to test the data extraction.
Set up the correct credentials in the file
[mainApp/values.yml](mainApp/values.yml) for the `s3` service.

```yaml
s3:
  access:
      key: <s3-access-key>
      secret: <s3-secret-key>
      url: <s3-url>
      region: europe
```

### Deployment

The Helm chart can be deployed as follows once the configurations are done.

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

## Retrieving the data

### Retrieving the data through the `res` service

The archived data files stored in the `s3` storage can be retrieved through the
`res` service by providing the correct `sourceKey` and `sourceIV`. The
`sourceKey` and `sourceIV` are the  passphrase and the initialization
vector for the AES encryption that are randomly generated when the original
data file is encrypted by `lega-cryptor`.

You may use the following command to retrieve a file named `testfile` at the `s3`
storage (note that the file is AES encrypted).

    curl -G http://<ip-of-master-node>:30090/file -d sourceKey=$sourceKey -d sourceIV=$sourceIV -d filePath=testfile > outfile

The retrieved file `outfile` is unencrypted.

## Uninstalling the Chart

    helm del --purge config-service mainapp-service

