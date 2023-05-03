# ProjectGenerator

## Requirements

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

## Test using command line

You could use `test.sh`

### configure

This script need the 4D Mobile App component. It will find it by default in `/Applications/4D.app/Contents/Resources/Internal User Components/4D Mobile App.4dbase`
but you could define env var `PERFORCE_PATH` to use the most recent source from perforce

```bash
export PERFORCE_PATH=$HOME/Perforce
```

### use it

```bash
./test.sh /path/to/your/4d/host/database /path/to/some/file/like/lastAndroidBuild.4dmobile
```

if no `.4dmobile` file defined, it will use the last one generated in `$HOME/Library/Caches/com.4d.mobile/lastAndroidBuild.4dmobile`

```bash
./test.sh /path/to/your/4d/database
```

then if you do not provide the database it will try to use the `4D Mobile App.4dbase` (in perforce or 4D.app)

```bash
./test.sh
```

## Build

```shell
./build.sh
```

### Requirements

- java 11
- kscript v3.x

could be installed using `setup.sh`

### for deployement only

or use `compiler.sh` which copy it to the component script directory.

Destination path is defined by:

- first argument passed to `compiler.sh` 
  - ex: `compiler.sh "/Applications/4D.app/Contents/Resources/Internal User Components/4D Mobile App.4dbase/Resources/scripts/"`
- or `PJGEN_PATH` env variable: absolute path to a script dir where the jar must be moved 
  - ex: `export PJGEN_PATH="/Applications/4D.app/Contents/Resources/Internal User Components/4D Mobile App.4dbase/Resources/scripts/"`
- or `PERFORCE_PATH` env variable: to copy it to a specific perforce repo (must provide root path, path before `4eDimension`)
- or if not env variable defined, expect that you are in perforce code, and script will use the relative path

> ⚠️ Do not commit androidprojectgenerator.jar if it's not the one builded from perforce sources
