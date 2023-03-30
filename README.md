# L2SS Footprint Lambda function
Table of Content
* [Overview](#overview)
* [Build](#build)
  * [Update project version](#update-project-version)
  * [Build Software](#build-software)
* [How to load and use forge module](#how-to-load-and-use-forge-module)
  * [Forge Input](#forge-input)
  * [Forge Output](#forge-output)
    
## Overview
   forge is built to be used within Cumulus ecosystem.  It is depending on cumulus CMA ([Cumulus Documentation](https://nasa.github.io/cumulus)).
   Please refer to the [Usage](#usage) section for inputs and outputs. Forge itself is a lambda function which runs on top of CMA as its lambda layer.

## Build
Building and deploying Forge is a manual process until the Jenkins pipeline template is applied to this project.

### Update project version

Works similar to poetry version <bump_rule>.
```shell script
./gradlew bumperVersionPrerelease     # e.g.  0.5.0 --> 0.5.1-alpha.0 and 0.5.1-alpha.0 --> 0.5.1-alpha.1
./gradlew bumperVersionPatch          # e.g.  0.5.1-alpha.1 --> 0.5.1 and 0.5.1 --> 0.5.2
./gradlew bumperVersionMinor          # e.g.  0.5.0-alpha.2 --> 0.5.0 and 0.5.1 --> 0.6.0
./gradlew bumperVersionPreminor       # e.g.  0.6.0 --> 0.7.0-alpha.0
```

Test can be run using the command
```shell script
./gradlew test
```

This will produce a test report in `build/reports/tests/test/index.html`

### Build Software

To build the software, first set the version of the software in `build.gradle`

Then, to build the project simply run:

```shell script
./gradlew buildZip
```



```shell script
Then, to build the project simply run:
./gradlew buildZip

Output current version to standard output line.  Donot forget the -q (quiet) command line option
./gradlew currVersion -q

List all tasks in group.  group is defined inside task
./gradlew tasks

To set arbitary version into project. There is no version validation in this task
Please be aware there is no vaidation of input. Any input string will be set to current version
./gradlew setCurrentVersion -Pargs=3.2.0
```



This will produce a zip file in `build/libs` with the version number in the name. This zip file is used by the AWS 
lambda.  build.sh is a temporary solution to build forge module (lambda code and terraform script) and push forge artifact to CAE artifactory.

## How to load and use forge module
    Project using forge can include/use the forge as following:
```shell script
    module "forge_module" {
    // Required parameters
    prefix = var.prefix
    region = var.region
    cmr_environment = var.cmr_environment

    config_bucket = "my-internal"
    config_dir    = "dataset-config"

    footprint_output_bucket = "my-internal"
    footprint_output_dir    = "dataset-footprint"

    lambda_role = module.cumulus.lambda_processing_role_arn
    layers = [var.cumulus_message_adapter_lambda_layer_version_arn]
    security_group_ids = [aws_security_group.no_ingress_all_egress.id]
    subnet_ids = var.subnet_ids
    }

    resource "aws_cloudwatch_log_group" "forge_task" {
    name              = "${module.forge_module.forge_function_name}"
    retention_in_days = var.task_logs_retention_in_days
    tags              = merge(local.tags, { Project = var.prefix })
    }
```
    and the module input variables explained as below.
| field name | type | default | values | description
| ---------- | ---- | ------- | ------ | -----------
| prefix | string | (required) | | A prefix string of lambda function. Ex. prefix = "sample" , created lambda : sample-forge
| region | string | (required) | | AWS region where forge lambda is running upon.  Ex. us-west-2
| cmr_environment | string | (required) | | dev, sit, ops
| config_bucket | string | (required) | | bucket where dataset config resides
| config_dir | string | (required) | | directory where dataset config file resides. dataset-config file follows the collection_shortname.cfg pattern. Ex. MODIS_A-JPL-L2P-v2019.0.cfg
| footprint_output_bucket | string | (required) | | bucket where footprint file is created and written
| footprint_output_dir | string | (required) | | output directory of created footprint(fp) file. file will be created as s3://footprint_output_bucket/footprint_output_dir/collection_short_name/granule_id.fp. ex. s3://my-cumulus-internaldataset-footprint/ MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.fp
| lambda_role | string | (required) | | aws user role to run forge lambda
| layers | list(string) | (required) | | list of layers' arn where forge runs upon.
| security_group_ids | list(string) | (required) | | security group ids
| subnet_ids | list(string) | (required) | | subnet ids where forge runs within
    
    module output variables
| field name | type | default | values | description
| ---------- | ---- | ------- | ------ | -----------
| forge_function_name | string | (required) | | The name of deployed forge lambda function
| forge_task_arn | string | (required) | | Forge lambda aws arn


### Forge Input
   Cumulus message with granules payload.  Example below
```json
{
  "granules": [
    {
      "files": [
        {
          "filename": "s3://bucket/file/with/checksum.dat",
          "checksumType": "md5",
          "checksum": "asdfdsa"
        },
        {
          "filename": "s3://bucket/file/without/checksum.dat",
        }
      ]
    }
  ]
}
```

### Forge Output
   * A footprint file will be created under configured footprint_output_bucket and footprint-output-dir.  filename as granuleId.fp. Ex. s3://my-cumulus-internaldataset-footprint/ MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.fp
   * A file object will be appended to the files[] of processed granule. Example:
```json
{
  "granules": [
    {
      "granuleId": "20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0",
      "dataType": "MODIS_A-JPL-L2P-v2019.0",
      "sync_granule_duration": 2603,
      "files": [
        {
          "bucket": "my-protected",
          "path": "MODIS_A-JPL-L2P-v2019.0/2020/001",
          "filename": "s3://my-protected/MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc",
          "size": 18232098,
          "name": "20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc",
          "checksumType": "md5",
          "checksum": "aa5204f125ae83847b3b80fa2e571b00",
          "type": "data",
          "url_path": "{cmrMetadata.CollectionReference.ShortName}",
          "filepath": "MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc",
          "duplicate_found": true
        },
        {
          "bucket": "my-public",
          "path": "MODIS_A-JPL-L2P-v2019.0/2020/001",
          "filename": "s3://my-public/MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc.md5",
          "size": 98,
          "name": "20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc.md5",
          "type": "metadata",
          "url_path": "{cmrMetadata.CollectionReference.ShortName}",
          "filepath": "MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc.md5",
          "duplicate_found": true
        },
        {
          "bucket": "my-public",
          "filename": "s3://my-public/MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.cmr.json",
          "size": 1617,
          "name": "20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.cmr.json",
          "type": "metadata",
          "url_path": "{cmrMetadata.CollectionReference.ShortName}",
          "filepath": "MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.cmr.json",
          "etag": "\"3e5b9259c5ee7eae5fe71467f151498b\""
        },
        {
          "bucket": "my-internal",
          "filename": "s3://my-internal/dataset-footprint/MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.fp",
          "filepath": "dataset-footprint/MODIS_A-JPL-L2P-v2019.0/20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.fp",
          "size": 452,
          "name": "20200101000000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.fp",
          "type": "metadata"
        }
      ],
}
```

