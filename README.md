<!--[![Maven Package upon a push](https://github.com/mosip/print/actions/workflows/push_trigger.yml/badge.svg?branch=release-1.2.0.1)](https://github.com/mosip/print/actions/workflows/push_trigger.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?branch=release-1.2.0.1&project=mosip_admin-services&id=mosip_admin-services&metric=alert_status)](https://sonarcloud.io/dashboard?branch=release-1.2.0.1&id=mosip_admin-services) -->
# Data MIgrator Tool
## Overview
A reference implementation to data migration from source system (`ORACLE`, `POSTGRES`, `MSSQL`, `MYSQL`) to MOSIP powered Destination system.

This Tool read individual Data (Demograhic, Biometric & Documents) from source system and perform conversion and formatting as per the MOSIP defined ID Schema.

Create JSON that combines formatted demographic, biometric & documents which MOSIP packet manager understand and call packet manager as library to create packet in local storage and encrypt.

Once packet created, system will sync then upload same into registration processor to process further.


## Architecture
![](docs/MIgration_Tool_Architecture.png)

## prerequisite
1. This Tool will run as either `Data Migrator` or `Quality Analysis` (Disabling Packet creation & Upload function by configuration).
2. This Tool can run either `Batch mode` or `Interactive mode (REST API Call)`

    ###For Data Migrator
    1. This Tool should be run either `Desktop` or `Laptop`, which have TPM Enabled. (TPM Required for Packet Signing)
    2. Country should have MOSIP working environment (Sandbox). ID Schema & Master Data will fetch from MOSIP during Initialization. 
    3. ID Schema should be finalized and should be updated into MOSIP master database before starting migration.
    4. Machine (Desktop/Laptop) should be onboarded into MOSIP.
    5. MockMDS L0(Registration Device) should be created and place the `.p12` file into mockmds folder in resource path. 
    6. Registration Processor API's (`sync` & `upload`) should be accessable in the system.
   
    ###For Quality Analyzer
    1. This Tool can run in either server or Local system. TPM not played any role here because of no packet creation.
    2. Country not necessary to have MOSIP working environment (Sandbox). but ID Schema should be present. 
    3. ID Schema should be finalized before starting quality analyzer.

<!--## Build and run (for developers)
Refer [Build and Run](docs/build-and-run.md). -->
    
## Deploy
The deploy Data Migrator/Quality Analyzer follow the given steps:

1. Update application-default.properties for hosts, Database, Reference Class, BIOSDK api's, etc
2. Place your ID Schema (`idschema.json`) file in `../src/main/resources` folder.
3. Place your CBEFF (`cbeff.xsd`) file in  `../src/main/resources` folder.
4. Place your Identity (`identity.json`) file in  `../src/main/resources` folder.
5. Place your updated External DB SQL (`external_db.sql`) file in  `../src/main/resources` folder.
6. For Data Migrator
   1. Onboard Device Partner for MOCK MDS and place `Device.p12` file in `../src/main/resources/mockmds` folder.
   2. Create Mvel Expression if any based on your requirement and place into `../src/main/resources` folder.
   3. Configure mosip-config (`application-default.properties` & `registration-processor-default.properties`) for Tool (Source = MIGRATOR) Configuration.
   4. Create new Camel Route (`registration-processor-camel-routes-migrator-default.xml`) and place into `mosip-config` repository
7. For Quality Analyzer
   1. Enable following property in application-default.properties
   `mosip.extractor.enable.quality.check.only=false`
8. For Batch Mode
   1. Place your API Request JSON (ApiRequest.json) file in `../src/main/resources` folder. 
9. Build and run as given <!-- [here](docs/build-and-run.md). -->

<!-- ## Configuration
Refer to the [configuration guide](docs/configuration.md).

## Test
Automated functaionl tests available in [Functional Tests repo](https://github.com/mosip/mosip-functional-tests).

## License
This project is licensed under the terms of [Mozilla Public License 2.0](LICENSE).
-->

### Property Changes
1. application-default.properties
```mosip.kernel.idobjectvalidator.mandatory-attributes.reg-processor.migrator=IDSchemaVersion
packetmanager.default.priority=source:REGISTRATION_CLIENT\/process:BIOMETRIC_CORRECTION|NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:OPENCRVS\/process:OPENCRVS_NEW,source:DATAMIGRATOR\/process:MIGRATOR
provider.packetreader.mosip=source:REGISTRATION_CLIENT|DATAMIGRATOR,process:NEW|UPDATE|LOST|BIOMETRIC_CORRECTION|MIGRATOR,classname:io.mosip.commons.packet.impl.PacketReaderImpl
provider.packetwriter.mosip=source:REGISTRATION_CLIENT|DATAMIGRATOR,process:NEW|UPDATE|LOST|BIOMETRIC_CORRECTION|MIGRATOR,classname:io.mosip.commons.packet.impl.PacketWriterImpl
```

2. registration-processor-default.properties
```camel.secure.active.flows.file.names=registration-processor-camel-routes-new-default.xml,registration-processor-camel-routes-update-default.xml,registration-processor-camel-routes-activate-default.xml,registration-processor-camel-routes-res-update-default.xml,registration-processor-camel-routes-deactivate-default.xml,registration-processor-camel-routes-lost-default.xml,registration-processor-camel-routes-res-reprint-default.xml,registration-processor-camel-routes-biometric-correction-default.xml,registration-processor-camel-routes-opencrvs_new-default.xml,registration-processor-camel-routes-migrator-default.xml
registration.processor.main-processes=NEW,UPDATE,LOST,RES_UPDATE,ACTIVATE,DEACTIVATE,OPENCRVS_NEW,MIGRATOR
registration.processor.sub-processes=BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.packet.validator.validate-applicant-document.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.cmd-validator.center-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.cmd-validator.machine-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
mosip.regproc.cmd-validator.device-validation.processes=NEW,UPDATE,LOST,BIOMETRIC_CORRECTION,MIGRATOR
packetmanager.provider.uingenerator.individualBiometrics[Finger]=source:REGISTRATION_CLIENT\/process:NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:DATAMIGRATOR\/process:MIGRATOR
packetmanager.provider.uingenerator.individualBiometrics[Iris]=source:REGISTRATION_CLIENT\/process:NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:DATAMIGRATOR\/process:MIGRATOR
packetmanager.provider.uingenerator.individualBiometrics[Face]=source:REGISTRATION_CLIENT\/process:NEW|UPDATE|LOST,source:RESIDENT\/process:ACTIVATED|DEACTIVATED|RES_UPDATE|RES_REPRINT,source:DATAMIGRATOR\/process:MIGRATOR
```
## Rules & Restriction
1. In Packet Creator Mode, If user specified specific field as applicationID for the Packet then MOSIP System will limit to accept packet only once. If user try to upload second time MOSIP will reject.
    Ex: `mosip.extractor.application.id.column=CITIZEN_ID`


