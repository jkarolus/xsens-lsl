# XSensDot LSL Streamer
This project is based on the XSensDot Example app and extends it with an LSL streaming functionality.

## Build against (possibly outdated)
- Android NDK 21.0.6113669
- Android SDK 29
- Android Studio 4.1.1
- cMake 3.12.4 + Ninja 1.10.2 (copy to CMake\bin folder): "cmake.dir=<path>" in local.properties

## LSL and Android instructions
1. Copy liblsl and liblsl-java into your android project's root folder. Library sources can be found on the official labstreaminglayer repository.
2. Tell gradle to include liblsl-java and where to find it (i.e. its build.gradle file in the liblsl-java folder). Copy the snippet into settings.gradle. Change the project folder for liblsl-Java according to your configuration (if necessary).
```gradle
include 'liblsl-Java'

// Tell gradle where to find liblsl-Java
if(hasProperty("liblsl_java_dir")) {
    println("Looking for liblsl-Java in " + liblsl_java_dir)
    project(':liblsl-Java').projectDir = new File(liblsl_java_dir)
}
else {
    println("Looking for liblsl-Java in the default path.")
    println("Change this via 'gradle -Pliblsl_java_dir=path/here if this fails")
    project(':liblsl-Java').projectDir = new File("./liblsl-Java")
}
```
3. Sync gradle and make sure build.gradle for ...liblsl-java appears in the gradle scripts section
4. LSL needs to be compiled from source, hence the appropriate NDK and CMake inclduing a C++ compiler are needed. __The version of CMake is crucial!__
Make sure you have the following versions to compile against (other might work, e.g. SDK and Android Studio):
* Android NDK 21.1.6352462
* Android SDK 29
* Android Studio 4.1.1
* Gradle 6.5
* [CMake 3.12.4](https://cmake.org/files/v3.12/)
* [Ninja 1.10.2](https://ninja-build.org/) (copy to CMake\bin folder)

You might need to tell Android Studio where to find CMake (or the correct version in case you have multiple). Copy this snippet into local.properties:
```
cmake.dir=<path>
# e.g. cmake.dir=C\:\\Program Files\\CMake
```
5. If using a low minimum SDK version, you might need to add the INTERNET permission in the manifest
```xml
<uses-permission android:name="android.permission.INTERNET" />
```
