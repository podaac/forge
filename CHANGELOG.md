# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).


## [Unreleased]

### Added
- **Terraform Fix Upload Images **
  - update way we are deploying 
### Changed
### Deprecated
### Removed
### Fixed
### Security


## [0.10.0]

### Added
 - **Terraform Upload Image**
   - Added terraform code to upload ecr docker images
 - **Add Forge Cli output file**
   - Forge cli now output a footprint file
### Changed
### Deprecated
### Removed
### Fixed
### Security


## [0.9.0]

### Added
- **JAVA 11**
  - Update forge to java 11
- **Forge CLI**
  - Added a cli command to run forge with jar file
- **Github actions update**
  - Update various github actions commands
- **Forge Update**
  - Updated java libraries with security holes, and update libraries for cumulus 16
### Changed
### Deprecated
### Removed
### Fixed
### Security


## [0.8.0]

### Added
- **Actions updates**
  - Updated Github Actions to tag releases properly and deploy to Cumulus
### Changed
### Deprecated
### Removed
### Fixed
### Security


## [0.7.5]

### Added
### Changed
- **Moved repo to Github.com**
  - Repo moved here: https://github.com/podaac/forge
  - Now builds and deploys in github.com Actions instead of Jenkins 
### Deprecated
### Removed
### Fixed
### Security


## [0.7.0]

### Added
### Changed
### Deprecated
### Removed
### Fixed
- **SWOT FIX**
  - Added a new new swot_linestring strategy where we process only the side1 and not other to generate a linestring cause the lat lon is a flat 1d
- **PODAAC-5547**
 - Add a try catch in polar strategy that was in original code
### Security


## [0.6.0]

### Added
- **Fargate Changes**
 - Added terraform fargate code, added in forge docker code, added snky jenkins pipeline, added docker jenkins deployment
### Changed
### Deprecated
### Removed
### Fixed
### Security


## [0.5.1]

### Added
### Changed
### Deprecated
### Removed
- **PODAAC-3668**
  - Removed Fixed strategy as we don't want any fixed footprint and rather use a granules bounding box
- **PODAAC-4883**
  - Fix bug for found non-noded intersection between LINESTRING also change how we calculate tolerance for creating polygons.
- **PODAAC-5066**
  - Fix bug where we generate empty polygon when we simplify after union.
### Fixed
### Security


## [0.5.0]

### Added
- **PODAAC-4186**
  - Created S3 buckets to run forge in AWS without cumulus
- **PODAAC-4187**
  - Created IAM roles to run forge in AWS without cumulus
- **PODAAC-4198**
  - Created lambda layer to run forge lambda in AWS without cumulus
- **PODAAC-4199**
  - Created forge modules to run in aws without cumulus
- **PODAAC-4205**
  - Update jenkins to deploy forge into service-sit and service-uat
- **PODAAC-4239**
  - Clean out tmp directory after running forge to prevent files build up
- **PODAAC-4249**
  - Clean out tmp directory so space to create temporary directory for forge to run
- **PODAAC-4423**
  - Update input/output of forge for cumulus 11.x.x upgrade
  - Update java libraries for snyk vulnerabilities
- **PODAAC-4424**
  - Added execution name to fp file for hitide backfilling.
### Changed
### Deprecated
### Removed
### Fixed
- **PODAAC-4238**
  - Fix java.lang.IllegalArgumentException: holes must not contain null bug.
- **PODAAC-4242
  - Fix multiple intersection point on footprint strategy polar.
- **PODAAC-3977**
  - Remove duplicate points when we union polygons.
- **PODAAC-3978**
  - Fix line segments that doesn't intersect.
### Security

## [0.4.0]

### Added
### Changed
### Deprecated
### Removed
### Fixed
- **PODAAC-3697**
  -Fix infinite looping in periodic strategy.
- **PODAAC-3975**
  - Add try catch around creating linear ring in periodic strategy.
- **PODAAC-3974**
  -Fix casting a multipolygon to a polygon in PolarSidesOnly footprint strategy.
- **PODAAC-3976**
  - Add original polygon when simplify creates an empty polygon.
### Security
- **PODAAC-4048**
  - CVE-2021-44228 Update log4j to 2.16.0, cumulus message adapter 1.3.4, and org.apache.commons commons-lang3 to 3.12.0
- Update cumulus message adapter to 1.3.5 to address CVE-2021-45046

## [0.3.3]
### Security
- Update cumulus message adapter to 1.3.9 to address CVE-2021-45046
- Update log4j to 2.17.1

## [0.3.2]
### Security
- Update cumulus message adapter to 1.3.5 to address CVE-2021-45046

## [0.3.1]
### Added
### Changed
### Deprecated
### Removed
### Fixed
### Security
- **PODAAC-4048**
  - CVE-2021-44228 Update log4j to 2.16.0, cumulus message adapter 1.3.4, and org.apache.commons commons-lang3 to 3.12.0

## [0.3.0]

### Added
- **PODAAC-3609**
  - Added in the options to download configuration files from a url.
- **PODAAC-3696**
  - Updated forge log level to info by default and added log level to be set via env setting.
### Changed
### Deprecated
### Removed
### Fixed
- **PODAAC-3502**
  - Fixed bug in smap when side1 is empty.
- **PODAAC-3702**
  - Fixed bug where split function returns [[]] to return [].
- **PODAAC-3721**
  - Fixed margins from 179.5 to 179.99.
### Security

## [0.2.0]

### Added
### Changed
### Deprecated
### Removed
### Fixed
- **PODAAC-3489**
  - Fixed bug in periodic footprint strategy when processing bottom cap
  - Added normalization to case where Polygon geometry intersects meridian and antimeridian, fixing self-intersection issue.
### Security

## [0.1.0]

### Added
- **PODAAC-2597**
  - Initial implementation of footprint lambda leveraging CMA (Cumulus Message Adapter) functionalities
- **PODAAC-2498**
  - Added unit tests
- **PODAAC-2738**
  - Build forge as a module for other terraform project to load.
### Changed
- **PODAAC-2735**
  - Cleanup/refactored code. Migrated to Gradle.
- **PODAAC-2499**
  - automatic build/test based on jenkinsfile
### Deprecated
### Removed
### Fixed
### Security
