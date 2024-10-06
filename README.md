
# Z-Shell Auto-Complete Functions for Apache Kafka CLI.

## Introduction

This project is a set of Z-Shell auto-complete functions for the Apache Kafka CLI.
It is a work in progress, covering the commands I use the most and then expanding to additional commands.
It includes the lookup of *topics*, *groups*, and *users* when applicable.

Apache Kafka CLI tools have subcommands, even though they look like arguments.
For example, `kafka-topics` has `--list` and `--create`, which cannot be used together but can be placed anywhere in the command line argument list.
These autocomplete scripts rely on those arguments happening early in the command, as they will control when additional commands unique to them. 

The connection configurations of `--bootstrap-server` and the various config arguments (e.g. `--command-config`) must
exist before the *topics*, *groups*, and *users* lookups are done, since they are required for connecting to the Kafka cluster.

Please see the [LICENSE](/LICENSE) file for specifics around the Apache 2.0 License for this project.
For this reason, as with any open-source project, it is always good to inspect the source code carefully.

## `kaflist` build and installation

This utility makes the autocompletion of *topics* and *groups* faster than using the `kafka-topics` and `kafka-consumer-groups`.
The command also adds support for getting current *users* as well, which is useful for `kafka-acls`. 
In order to use this, it needs to be built for your computer's architecture using *GraalVM*. 
If this utility is not installed and added to your path, the autocomplete functions will use the standard CLI tools, resulting in a a noticeable delay.

1. **Install GraalVM**

- Visit [GraalVM.org](https://www.graalvm.org/downloads/) and install it according to their instructions.

2. **Add `GRAALVM_HOME` to your Environment Variables**

```shell
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-jdk-22.0.2+9.1/Contents/Home
```

Adjusting to the version downloaded and installed path.

3. **Build `kaflist`**

```shell
./gradlew nativeCompile
```

4. **Add `kaflist` to Your Path**

Place this in a directory that is part of your path, such as `~/bin`. 
Any path directory will do, provided you have permissions.

```shell
cp ./kaflist/build/native/nativeCompile/kaflist ~/bin/
```

Verify `kaflist` works:

```shell
kaflist topics --bootstrap-server localhost:9092 [--command-config ./file.properties]
```

You will get a list of all topics, one per line, with the cluster you connect to.

## Enabling Z-Shell Autocomplete

Z-Shell autocomplete is not automatically enabled. 
However, many Zsh frameworks will manage this for you.
There are many frameworks out there *Oh My Zsh*, *Prezto*, *zgen* and others. 
I do not use a framework, as I have have had issues with bloated configurations slowing my shell experience down.
Here are the steps to do this manually, but adjust to your environment.


1. **Add a Directory for Custom Z-Shell Functions**

While these auto-complete functions can be added to an existing function directory,
it is best to separate them to avoid losing them on an OS or shell upgrade.

   ```shell
   mkdir -p ~/.zsh/functions/
   ```

2. **Add This Directory to Your `fpath`**

While this could easily be done with adding the following to your `.zshrc` file,
it is best to set up changes to `.zshrc` file that don't add the directory multiple times when re-sourced.

```shell
fpath=(~/.zsh/functions $fpath)
```

Instead, find current fpath by doing `echo $fpath` and then creating one as follows:

```shell
fpath=(~/.zsh/functions /usr/local/share/zsh/site-functions /usr/share/zsh/site-functions /usr/share/zsh/5.9/functions)
```

3. **Enable Z-Shell autocomplete**

If you are using a zsh framework, read up on their documentation to determine their way of enabling autocomplete.
Typically, this is done by adding the following to your `~/.zshrc` file.

```shell
autoload -Uz compinit
compinit
```

## Adding These Auto-Complete Functions

This is the step you will need to repeat if/when these functions are improved.

1. **Copy Auto-Complete Functions to Your Functions Directory**

```shell
cp ./functions/* ~/.zsh/functions/
```

If you use the Apache Kafka distribution and use/prefer the *.sh suffix, please update functions accordingly.

```shell
mv ~/.zsh/functions/_kafka-topics ~/.zsh/functions/_kafka-topics.sh
```

2. *Refresh Opened Z-Shell Instances*

To enable these functions in an open shell, source the z-shell configuration file and rehash functions.

```shell
source ~/.zshrc
rehash
```

## Troubleshooting

* Make sure `fpath` is defined before enabling autcomplete `compinit`.

* If you make changes to path or function path to a running zsh, try `rehash` to rebuild the internal hash table for commands.

* `unfunction _kafka-topics` will unload a function, and `autoload -Uz _kafka-topics` would then reapply; this is useful if a command is changed.

* `declare -f _kafka-topics` will list the function, before the function is used it should be a lazy defined, function as follows:

```shell
_kafka-topics () {
        # undefined
        builtin autoload -XUz
}
```

once the function as been used/loaded, `declare -f _kafka-topics` will show the actual function.
  

## Supported Auto-Complete Functions

### **kafka-topics**

### **kafka-consumer-groups**

### **kafka-console-consumer**

### **kafka-console-producer**

### **kafka-configs**

