# Kubernetes deployment instructions

Currently the deployment was only done on Kubernetes, the plans are to support Mesosphere as well.

To install Kubernetes:
* https://github.com/fernandohackbart/bikeride-lagom/issues/155
* https://github.com/fernandohackbart/bikeride-lagom/issues/162
* https://github.com/fernandohackbart/ansible-k8s-centos

After setting up Kubernetes the customized setup can be started


## Minikube setup
```
virsh list --all
virsh net-list
minikube config set vm-driver kvm
minikube config set WantReportErrorPrompt false
minikube start --memory 7600 --cpus 3 --disk-size 40g
kubectl get po -n kube-system
```

To expose the minikube containers to the internal network  (issue:
https://github.com/fernandohackbart/bikeride-lagom/issues/162)

```
generate-minikube-proxy.sh
```

To clean up and rebuild from scratch:
```
minikube delete
virsh net-destroy docker-machines
virsh net-undefine docker-machines
```

If `minikube delete` does not work:
```
virsh destroy minikube
virsh undefine minikube
```

For the example the kubectl was installed at `/opt/kubernetes/bin/kubectl`.

### Deploy Jenkins on Minikube:
```
mkdir -p /opt/kubernetes
cd /opt/kubernetes
git clone https://github.com/fernandohackbart/bikeride-lagom.git
cd bikeride-lagom
/opt/kubernetes/bin/kubectl create -f deploy/kubernetes/jenkins-ephemeral.yml
/opt/kubernetes/bin/kubectl get po
```

Should take some time to be in the `running` state, after in running state

### Get access to Jenkins (if using Minikube)
* `kubectl get po` to get the Jenkins pod id
* `kubectl logs <Jenkins pod id>` to get the key to unlock Jenkins
* browser to `http://<minikube machine>:30010` to unlock Jenkins

## Configure Jenkins pipelines

* Install suggested plugins
* Install sbt plugin
* Create ssh credentials for the docker build machine
  * https://www.digitalocean.com/community/tutorials/how-to-set-up-ssh-keys--2
* Create configuration for SBT
  * `SBT at docker`
  * `/opt/sbt/bin/sbt-launch.jar` (SBT was installed there)
* Configure nodes
  * Reduce executor of master to 0
  * Create docker (somewhere you have docker installed and running)
    * Name: `docker`
    * no. of executors: `1`
    * remote root: `/opt/jenkins`
    * labels: `docker`
    * Laung methods: `Launch slave agents via SSH`
      * Host: `<IP of the node>`
      * Credentials:  `<root credentials with ssh key>`
      * Host Key verification strategy: `Non verifying Verification Strategy`
    * Tools Locations:
      * (Sbt) `SBT at docker`
      * Home: `/opt/sbt`
* Create a credential called `bikerideATdockerhub` with the credentials to connect to the Docker registry
* Create Pipelines (https://jenkins.io/doc/book/pipeline) using GIT based Jenkinsfile (FIX PATHS !!)
  * build-bikeride
    * https://github.com/fernandohackbart/bikeride-lagom.git
    * `build/Jenkins/build-bikeride/Jenkinsfile`
  * push-bikeride
    * https://github.com/fernandohackbart/bikeride-lagom.git
    * `build/Jenkins/push-bikeride/Jenkinsfile`
  * deploy-bikeride
    * https://github.com/fernandohackbart/bikeride-lagom.git
    * `build/Jenkins/deploy-bikeride/Jenkinsfile`

So now basically is a matter of running the `build-bikeride` job, it will try to push the images to the dockerhub repo and so far that can only be done by me :P, you can skip this job if you arre not planning to customize it for you own repo.

Deploy bikeride will deploy the Bikeride components into minikube, there are some steps that require waiting:
* `deploy/kubernetes/cassandra.yml` should be finished before start the database configuration
* `deploy/kubernetes/kong_migration_cassandra.yml` should be finished before removing the job from minikube...
 
After Kong (Deploy Kong: `Kubernetes/kong_cassandra.yml`) is configured you can create the Kong routes ( that are not yet automated but sometime in  the future will be)


If using Minikube
```
export KONG_URL=http://`minikube ip`:30022
```

Running over LoadBalancer (NOT WORKING!!!! It is returning the service port and not the administration port)
```
export KONG_URL=http://`kubectl get services|grep "kong-service "|awk '{print $4}'`:`kubectl get service |grep "kong-service " |awk '{print $5}' |awk '{split($0,a,":"); print a[1]}'`
```

Create the API into Kong
```
curl -i -X POST --url ${KONG_URL}/apis/ --data 'name=authentication-api' --data 'strip_uri=false' --data 'uris=/api/authn' --data 'upstream_url=http://_lagom._tcp.auth-bikeride-authentication.default.svc.cluster.local'
curl -i -X POST --url ${KONG_URL}/apis/ --data 'name=biker-api' --data 'strip_uri=false' --data 'uris=/api/biker,/api/bikers' --data 'upstream_url=http://_lagom._tcp.biker-bikeride-biker.default.svc.cluster.local'
curl -i -X POST --url ${KONG_URL}/apis/ --data 'name=track-api' --data 'strip_uri=false' --data 'uris=/api/track' --data 'upstream_url=http://_lagom._tcp.track-bikeride-track.default.svc.cluster.local'
curl -i -X POST --url ${KONG_URL}/apis/ --data 'name=ride-api' --data 'strip_uri=false' --data 'uris=/api/ride' --data 'upstream_url=http://_lagom._tcp.ride-bikeride-ride-.default.svc.cluster.local'
```

Note that the `kubernetes/*-service.yml` has hard coded port numbers, if you already have other stuff running on minikube this may badly collide.

## Some documentation used during the setup of Kubernetes and minikube

* https://github.com/kubernetes/kubernetes/tree/master/examples/guestbook-go
* https://kubernetes.io/docs/concepts/overview/working-with-objects/kubernetes-objects/
* https://kubernetes.io/docs/concepts/workloads/controllers/deployment/
* https://github.com/openshift/origin
* https://github.com/kubernetes/kubernetes/blob/master/examples/guestbook/all-in-one/frontend.yaml
* https://kubernetes.io/docs/concepts/services-networking/connect-applications-service/
* https://kubernetes.io/docs/concepts/containers/images/

