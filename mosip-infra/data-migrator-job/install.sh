#!/bin/bash
# Script to initialize the DB. 
## Usage: ./install.sh [kubeconfig]

if [ $# -ge 1 ] ; then
  export KUBECONFIG=$1
fi

function data-migrator-install() {
  NS=data-migrator
  CHART_VERSION=12.0.2
  kubectl label ns $NS istio-injection=enabled --overwrite
  helm repo update
  helm dependency update helm/

  kubectl delete configmap property-config -n $NS --ignore-not-found=true
  kubectl create configmap property-config  \
    --from-file=../../packet-generator/data-extractor-conversion/src/main/resources/externalsamples/ApiRequest.json \
    --from-file=../../packet-generator/data-extractor-conversion/src/main/resources/externalsamples/application-default.properties \
    --from-file=../../packet-generator/data-extractor-conversion/src/main/resources/externalsamples/BioFile.mvel \
    --from-file=../../packet-generator/data-extractor-conversion/src/main/resources/externalsamples/external_db.sql \
    --from-file=../../packet-generator/data-extractor-conversion/src/main/resources/externalsamples/identity.json \
    --from-file=../../packet-generator/data-extractor-conversion/src/main/resources/externalsamples/idschema.json -n $NS

  echo Installing data-migrator service
  helm -n $NS install data-migrator helm/ --wait --wait-for-jobs --version $CHART_VERSION -f migrator.yaml
  return 0
}

# set commands for error handling.
set -e
set -o errexit   ## set -e : exit the script if any statement returns a non-true return value
set -o nounset   ## set -u : exit the script if you try to use an uninitialised variable
set -o errtrace  # trace ERR through 'time command' and other functions
set -o pipefail  # trace ERR through pipes
data-migrator-install   # calling function
