#!/bin/bash
# Installs sample mpesa service
## Usage: ./restart.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

NS=data-migrator
CHART_VERSION=12.0.1

echo Create $NS namespace
kubectl create ns $NS 

function installing_migrator() {
  echo Istio label
  kubectl label ns $NS istio-injection=enabled --overwrite
  helm repo update
  helm dependency update helm/

DATABASE_IP_ADDRESS=

echo Installing Migrator

  echo Installing bqat service
  helm -n $NS install bqat-service helm/ --wait --version $CHART_VERSION -f bqat.yaml
  return 0
}

# set commands for error handling.
set -e
set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
set -o errtrace  # trace ERR through 'time command' and other functions
set -o pipefail  # trace ERR through pipes
installing_migrator   # calling function
