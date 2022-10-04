# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## 0.7.0 - 2022-10-04
### Added
- Support reading dates as instances (thanks @henryw374)
- Support data literal for mongo id (thanks @henryw374)
- Support for implicit transactions (thanks @AdamClements)
- Support for aggregation pipeline in find-one-and-update (requires 4.2+, thanks @jacobemcken)

## 0.6.0 - 2020-01-10
### Added
- Support for bulk-write

### Changed
- Moved option creators and document conversion to the `mongo-driver-3.model` namespace (breaking change)

## 0.5.0 - 2019-11-22
### Added
- Support for transactions

## 0.4.0 - 2019-11-19
### Added
- list collections
- start session
- remove reflection warnings

## 0.3.1 - 2019-11-17
### Added
- More documentation

## 0.3.0 - 2019-11-15
### Added
- Added aggregate function
- `skip` option to `find`

### Changed
- Better docs for cljdoc
- Merged `find-maps` into `find`
- Added ? suffix to boolean params
- Renamed `find-one-as-map` to `find-one`

## 0.2.0 - 2019-11-14
### Added
- expose operators

## 0.1.0 - 2019-11-14
### Added
- Initial release