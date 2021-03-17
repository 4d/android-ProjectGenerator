# ProjectGenerator

## Requierements

- [kscript](https://github.com/holgerbrandl/kscript), installed for instance with sdkman or homebrew
- gradle to package

## Usage

```shell
kscript main.kt \
    --project-editor "<path_to_json>" \
    --files-to-copy "<path_to_files_to_copy>" \
    --template-files "<path_to_template_files>" \
    --template-forms "<path_to_template_forms>" \
    --host-db "<path_to_host_database>"
```

## Build

```shell
kscript --package main.kt
```

or use `compiler.sh` which copy it to the component script directory.

Component path is defined by:

- `PJGEN_PATH` env variable: absolute path to a script dir where the jar must be moved (ex: export PJGEN_PATH="/Applications/4D.app/Contents/Resources/Internal User Components/4D Mobile App.4dbase/Resources/scripts/")
- or `PERFORCE_PATH` env variable: to copy it to a specific perforce repo (must provide root path, path before 4eDimension)
- or if not env variable defined, expect that you are in perforce code, and script will use the relative path

> /!\ Do not commit androidprojectgenerator.jar if it's not the one builded from perforce sources
