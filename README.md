# Conan Artipie adapter

Conan is a C/C++ repository, this adapter is an SDK for working with Conan data and metadata and a HTTP endpoint for the Conan repository.


## How does Conan work

### Conan recipe example:

```
zmqpp
├── all
│   ├── CMakeLists.txt           # This recipe uses CMake as C++ build system
│   ├── conandata.yml            # Source codes (URLs), patches and versions mapping
│   ├── conanfile.py             # Main python script of Conan recipe
│   ├── patches
│   │   ├── 0001-fix_zmqpp_export_header.patch
│   │   └── 0002-fix_zmqpp_libsodium_deps.patch
│   └── test_package
│       ├── CMakeLists.txt
│       ├── conanfile.py
│       └── test_package.cpp
└── config.yml                   # Versions & directories mapping
```


### Conan recipe lifecycle:

```
Conan package sources (uses library sources) -> 
local package recipe check (conan create ...) -> 
push to Conan package recipes git -> 
remote recipe checks/CI (e.g. GitHub Actions) -> 
Recipe code review -> Merge recipe code -> 
build popular package configurations on CI VMs/containers -> 
Upload package recipe with package binaries to Conan repository (conan upload zmqpp/4.2.0 -r conan-local --all) -> 
Download Conan recipes from Conan repository (with dependencies) ->
Download compatible prebuilt package binaries ->
build missing package binaries from package recipe (i.e. from sources).
```


### Conan package uploaded, directory structure for Conan python server:

```
zmqpp/
└── 4.2.0
    ├── _
    │   └── _
    │       ├── 0
    │       │   ├── export
    │       │   │   ├── conan_export.tgz
    │       │   │   ├── conanfile.py
    │       │   │   ├── conanmanifest.txt
    │       │   │   └── conan_sources.tgz
    │       │   └── package
    │       │       ├── 05d8bc6b87d38fd66f059ec637f04f9ebcc3aa8d
    │       │       │   ├── 0
    │       │       │   │   ├── conaninfo.txt
    │       │       │   │   ├── conanmanifest.txt
    │       │       │   │   └── conan_package.tgz
    │       │       │   ├── revisions.txt
    │       │       │   └── revisions.txt.lock
    │       │       ├── ........................................
    │       ├── revisions.txt
    │       └── revisions.txt.lock
    └── demo
        └── testing
            ├── 0
            │   ├── export
            │   │   ├── conan_export.tgz
            │   │   ├── conanfile.py
            │   │   ├── conanmanifest.txt
            │   │   └── conan_sources.tgz
            │   └── package
            │       └── f1c4d98ac919d48870f68bc5674f6b1be6efa827
            │           ├── 0
            │           │   ├── conaninfo.txt
            │           │   ├── conanmanifest.txt
            │           │   └── conan_package.tgz
            │           ├── revisions.txt
            │           └── revisions.txt.lock
            ├── revisions.txt
            └── revisions.txt.lock
```


* Conan repository stores Conan packages, which contain conan package recipe + package binaries (>=0)
* Every Conan repository package (artifact) recipe is the directory with fixed structure. *.yml is used for metadata, and conanfile.py is used for build & packaging logic. Source code may be part of the package source, but often downloaded from original repository by package recipe. Package binaries stored for popular build configurations, which is stored in text format (conaninfo.txt). https://docs.conan.io/en/latest/creating_packages/getting_started.html
* Basic configuration options for every Conan recipe: OS (Windows/Linux/macOS), architecture, compiler, static/shared mode, debug/release mode. Configureation stored in text format (conaninfo.txt). Every conan package may have dependencies on other conan packages. https://docs.conan.io/en/latest/creating_packages/define_abi_compatibility.html
* Conan uses Python scripts with some base classes provided by Conan project, which wrap steps like patch/configure/make/install, as well as dependencies configuration.
* Makefile/Automake and CMake are supported well by Conan python framework, custom builds could also be implemented manually. https://docs.conan.io/en/latest/reference/build_helpers.html
* Every package recipe contains simple executable test project, which is used for build & packaging checks (successful run and exit without crash, dependency resolution, etc).
* Due to the complexity of C/C++ dependency configuration for multi-platform, CI/build checks is important step, so Conan recipes first committed in conan index git repository.
* After reviewing and merging Conan recipe and package binaries for popular configurations are pushed to Conan repository server. Other configurations built locally on client by downloading and running Conan recipe.
* Conan provides its own server on Python with all basic functionality implemented. SSL/https and authentication is supported but not required there. https://docs.conan.io/en/latest/uploading_packages/running_your_server.html
* Conan supports multiple remotes for multiple sources of the packagess, including company-internal hosting. https://docs.conan.io/en/latest/uploading_packages/remotes.html
* Package binary's hash is SHA-1 and computed based on full build configuration info. Package archive contains include files and library binaries. https://docs.conan.io/en/latest/creating_packages/understand_packaging.html#package-creation-process


## How to use Artipie Conan SDK

TODO

## How to configure and start Artipie Conan endpoint

TODO

