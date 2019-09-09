# m3version-maven-plugin
[![Build Status](https://travis-ci.org/KiuIras/m3version-maven-plugin.svg?branch=master)](https://travis-ci.org/KiuIras/m3version-maven-plugin)

## Description
M3 Version Maven Plugin provides a update-module goal that can be used to update the version of a module and of all dependant modules.

## How-To
### Prerequisites
This plugin works with the "Major.Minor.Patch" [Semantic Versioning](https://semver.org/). 

### Configuration
To use this plugin, clone and compile it via 
```bash
mvn install
```
Then add to the plugin section of the POM of your Maven Project
```bash
[...]
<plugin>
  <groupId>it.kiuiras.maven.plugin</groupId>
  <artifactId>m3version-maven-plugin</artifactId>
  <version>0.0.1-SNAPSHOT</version>
</plugin>
[...]
```

### Run
Run the `update-module` goal via
```bash
mvn m3version:update-module
```
#### Parameters
By default, the `update-module` goal increment the Patch number of the version. You can also decide to increment Major or Minor number by `versionType` variable. For example:
```bash
mvn m3version:update-module -DversionType=MAJOR
```
It is also possible to configure if the plugin should update the version of the parent of an updated module or not via the `updateParent` variable (default is true):
```bash
mvn m3version:update-module -DupdateParent=false
```
