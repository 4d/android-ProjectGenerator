# ProjectGenerator

This project create android project using inputs from [4D Mobile App](https://github.com/4d/4D-Mobile-App/).

## Requirements

- [kscript](https://github.com/holgerbrandl/kscript), installed for instance with sdkman or homebrew
- gradle to package

## Usage

Using [kscript](https://github.com/kscripting/kscript)

```shell
kscript main.kt \
    generate \
    --project-editor "<path_to_json>" \
    --files-to-copy "<path_to_files_to_copy>" \
    --template-files "<path_to_template_files>" \
    --template-forms "<path_to_template_forms>" \
    --host-db "<path_to_host_database>" \
	--catalog "<catalog-file-path"
```

with compiled jar replace `kscript main.kt` by `java -jar androidprojectgenerator.jar`

## Deploy

This tool is mainly used by [4D Mobile App](https://github.com/4d/4D-Mobile-App/blob/main/Resources/scripts/) to create Android project.

To inject a custom build you must place a compiled `androidprojectgenerator.jar` in [Resources/scripts](https://github.com/4d/4D-Mobile-App/blob/main/Resources/scripts/) of `4D Mobile App`.

> 💡 By default `4D Mobile App` will download the latest release on github, see [_copyJarIfmissing](https://github.com/4d/4D-Mobile-App/blob/main/Project/Sources/Classes/androidprojectgenerator.4dm#L92) function.

## Build

```shell
./build.sh
```

### Requirements

- java 11
- kscript v3.x

could be installed using `setup.sh`

## Test using command line

You could use `test.sh`

### configure

This test script need the `4D Mobile App` component to get some [templates files](https://github.com/4d/4D-Mobile-App/tree/main/Resources/templates/android/project). You must defined the env var `MOBILE_COMPONENT_PATH`.

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
