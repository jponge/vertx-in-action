# Using Prometheus and Grafana

## Setup

Clone https://github.com/coreos/kube-prometheus then follow the installation instructions.

This will install and configure Prometheus, Grafana and the Alert Manager.

## Configure Prometheus to collect our metrics

Apply the resource definitions of this folder:

    kubectl apply -f sensor-gateway/k8s-metrics

You should now see log entries where metrics are being collected.

## Grafana dashboard

You can access Grafana at http://localhost:3000 using port forwarding:

    kubectl --namespace monitoring port-forward svc/grafana 3000

Use `admin / admin` as default credentials.

The Vert.x examples contain a Grafana dashboard at https://github.com/vert-x3/vertx-examples/tree/master/micrometer-metrics-examples/grafana

Just import the JSON definition into Grafana.
Use Prometheus as the database.
Enjoy!
