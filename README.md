[![Maven Package upon a push](https://github.com/mosip/print/actions/workflows/push_trigger.yml/badge.svg?branch=release-1.2.0.1)](https://github.com/mosip/print/actions/workflows/push_trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.2.0.1&project=mosip_admin-services&id=mosip_admin-services&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.2.0.1&id=mosip_admin-services)
# Print Service

## Overview
A reference implementation to print `euin`, `reprint`, `qrcode` [credential types](https://docs.mosip.io/1.2.0/modules/id-repository#credential-types) in PDF format. This service is intended to be custimized and used by a card printing agency who need to onboard onto MOSIP as [Credential Partner](https://docs.mosip.io/1.2.0/partners#credential-partner-cp) before deploying the service.  

![](docs/print-service.png)

1. Receives events from WebSub.
2. Fetches templates from Masterdata.
3. After creating PDF card print service upload the same to [Datashare](https://docs.mosip.io/1.2.0/modules/data-share).
4. Publishes event to WebSub with updated status and Datashare link.

The card data in JSON format is publised as WebSub event.  The print service consumes the data from event, decrypts using partner private key and converts into PDF using a predefined [template](docs/configuration.md#template).

## Build and run (for developers)
Refer [Build and Run](docs/build-and-run.md).
    
## Deploy
The deploy print service in production follow the given steps:

1. Onboard your organisation as [Credential Partner](https://docs.mosip.io/1.2.0/partners).
1. Place your `.p12` file in `../src/main/resources` folder.
1. Set configuration as in given [here](docs/configuation.md).
1. Build and run as given [here](docs/build-and-run.md).

## Configuration
Refer to the [configuration guide](docs/configuration.md).

## Test
Automated functaionl tests available in [Functional Tests repo](https://github.com/mosip/mosip-functional-tests).

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).


application-default.properties
mosip.kernel.idobjectvalidator.mandatory-attributes.reg-processor.migrator=IDSchemaVersion
packetmanager.default.priority=source:REGISTRATION_CLIENT\/process:BIOMETRIC_CORRECTION|NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:OPENCRVS\/process:OPENCRVS_NEW,source:DATAMIGRATOR\/process:MIGRATOR
provider.packetreader.mosip=source:REGISTRATION_CLIENT|DATAMIGRATOR,process:NEW|UPDATE|LOST|BIOMETRIC_CORRECTION|MIGRATOR,classname:io.mosip.commons.packet.impl.PacketReaderImpl
provider.packetwriter.mosip=source:REGISTRATION_CLIENT|DATAMIGRATOR,process:NEW|UPDATE|LOST|BIOMETRIC_CORRECTION|MIGRATOR,classname:io.mosip.commons.packet.impl.PacketWriterImpl


registration-processor-default.properties
camel.secure.active.flows.file.names=registration-processor-camel-routes-new-default.xml,registration-processor-camel-routes-update-default.xml,registration-processor-camel-routes-activate-default.xml,registration-processor-camel-routes-res-update-default.xml,registration-processor-camel-routes-deactivate-default.xml,registration-processor-camel-routes-lost-default.xml,registration-processor-camel-routes-res-reprint-default.xml,registration-processor-camel-routes-biometric-correction-default.xml,registration-processor-camel-routes-opencrvs_new-default.xml,registration-processor-camel-routes-migrator-default.xml
registration.processor.main-processes=NEW,UPDATE,LOST,RES_UPDATE,ACTIVATE,DEACTIVATE,OPENCRVS_NEW,MIGRATOR
registration.processor.sub-processes=BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.packet.validator.validate-applicant-document.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.cmd-validator.center-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.cmd-validator.machine-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.cmd-validator.device-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
packetmanager.provider.uingenerator.individualBiometrics[Finger]=source:REGISTRATION_CLIENT\/process:NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:DATAMIGRATOR\/process:MIGRATOR
packetmanager.provider.uingenerator.individualBiometrics[Iris]=source:REGISTRATION_CLIENT\/process:NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:DATAMIGRATOR\/process:MIGRATOR
packetmanager.provider.uingenerator.individualBiometrics[Face]=source:REGISTRATION_CLIENT\/process:NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:DATAMIGRATOR\/process:MIGRATOR

## Rules & Restriction
1. In Packet Creator Mode, If user specified specific field as applicationID for the Packet then MOSIP System will limit to accept packet only once. If user try to upload second time MOSIP will reject.
    Ex: mosip.extractor.application.id.column=CITIZEN_ID
2. 


