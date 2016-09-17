# Progressor - Advanced Executor Configuration Options

## Command-Line Arguments

The **Executor** accepts a number of command-line arguments:

* `-p port` or `-port port` will override the default port (9001).
* `-c [false|no]` or `-cleanup [false|no]` will prevent temporary files (e.g. generated code files, compiled executables or *Docker* containers) from being deleted.
  * `-d [true|yes]` or `-docker [true|yes]` has no effect because because cleanup is enabled by default.
* `-d [false|no]` or `-docker [false|no]` will disable *Docker* support on Linux.
  * `-d [true|yes]` or `-docker [true|yes]` has no effect because because *Docker* is enabled by default on Linux.
    If invoked on Windows, the application will abort because this feature is not yet supported.
* `-t [true|yes]` or `-test [true|yes]` will run a test client instead of the server (request handler).
  * `-t [false|no]` or `-test [false|no]` has no effect because server is the standard mode.
  * `-h hostname` or `-host hostname` can be used to test an instance different from the local one.

## Extensibility

The **Executor** uses a [ServiceLoader](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html) to load the different code executors at runtime.

New languages can be supported by simply implementing the service *ch.bfh.progressor.executor.api.CodeExecutor* and making it discoverable by the **Executor**.
Additional information can be found in the [Java SE API Specification](http://docs.oracle.com/javase/8/docs/api/java/util/ServiceLoader.html).
