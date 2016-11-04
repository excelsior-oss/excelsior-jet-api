[![Maven Central](https://img.shields.io/maven-central/v/com.excelsiorjet/excelsior-jet-api.svg)](https://maven-badges.herokuapp.com/maven-central/com.excelsiorjet/excelsior-jet-api)
Excelsior JET Java API
=====

The common part of Excelsior JET [Maven](https://github.com/excelsior-oss/excelsior-jet-maven-plugin)
and [Gradle](https://github.com/excelsior-oss/excelsior-jet-gradle-plugin) plugins.
Though we have no plans to provide plugins for other build tools such as ANT or SBT at this point,
you can use this API to write your own plugin for those build tools or for your own build tool,
so as to automate your Excelsior JET builds.

### Usage and classes overview
When writing your own Excelsior JET plugin for a build system of your choice,
you will most likely use the following three classes:

#### JetProject
[`JetProject`](https://github.com/excelsior-oss/excelsior-jet-api/blob/master/src/main/java/com/excelsiorjet/api/tasks/JetProject.java)
is essentially a collection of Excelsior JET compiler and packager parameters
that configure a build of a Java project with Excelsior JET.

So first, you need to create an instance of `JetProject`, the constructor of which has the following arguments:

* `projectName` -  name of the project
* `groupId` - project group id. Unique identifier that can be shared by multiple projects.
               Usually a reverse domain name is used as a group id, such as "com.example".
* `version` - project version
* `appType` - application type. Two types of applications are currently supported:
    - `PLAIN` -  **Plain Java SE applications**, i.e. applications that have a main class
                and have all their dependencies explicitly listed in the JVM classpath at launch time, and
    - `TOMCAT` - **Tomcat Web applications** &mdash; `.war` files that can be deployed to the
                Apache Tomcat application server.
    -  `INVOCATION_DYNAMIC_LIBRARY` - **Invocation Dynamic Libraries**, dynamic library (e.g. Windows DLL) callable
        from applications written in another language

    -  `WINDOWS_SERVICE` - **Windows Services**, is a special long-running process that may be launched
         during operating system bootstrap (for Windows only)


* `targetDir` - target build directory
* `jetResourcesDir` - directory that contains Excelsior JET specific resource files such as application icons,
                       installer splash,  etc.

All the above parameters are required and cannot be `null`.

Then you set other parameters via `JetProject`
build pattern methods, such as `.mainClass(ProjectMainClass)` and
`.dependencies(ProjectDependencies)` for `PLAIN` Java SE applications.

#### JetBuildTask
Once you have configured a `JetProject` instance, you may build it with
[`JetBuildTask`](https://github.com/excelsior-oss/excelsior-jet-api/blob/master/src/main/java/com/excelsiorjet/api/tasks/JetBuildTask.java)
that has the only method `execute()`. So the sketch of your plugin will look like this:
(taken from the [Maven](https://github.com/excelsior-oss/excelsior-jet-maven-plugin/blob/master/src/main/java/com/excelsiorjet/maven/plugin/JetMojo.java) plugin):

```java
    JetProject jetProject = new JetProject(project.getArtifactId(), project.getGroupId(), project.getVersion(),
                                        getAppType(), targetDir, jetResourcesDir)
                                .jetHome(jetHome)
                                .mainClass(mainClass)
                                .dependencies(getArtifacts())
                                //.anotherConfigParameter(...)
                                //.anotherConfigParameter(...)
    ;
    new JetBuildTask(jetProject).execute();
```

The native build is performed in the `jet` subdirectory of the `targetDir` project parameter.
First, the task copies the main application jar to the `jet/build` directory,
and copies all its run time dependencies to `jet/build/lib`.
Then it invokes the Excelsior JET AOT compiler to compile all those jars into a native executable.
Upon success, it copies that executable and the required Excelsior JET Runtime files
into the `jet/app` directory, binds the executable to that copy of the Runtime,
and copies the contents of the directory to which the `packageFilesDir` project parameter points
recursively to `jet/app`.

> Your natively compiled application is ready for distribution at this point: you may copy
> the contents of the `jet/app` directory to another computer that has neither Excelsior JET nor
> the Oracle JRE installed, and the executable should work as expected.

Finally, the task packs the contents of the `jet/app` directory into
a zip archive named `${artifactName}.zip` so as to aid single file re-distribution.
On Windows and Linux, you can also set the `excelsiorJetPackaging` parameter to `excelsior-installer`
to have the task create an Excelsior Installer setup instead,
and on OS X, setting `excelsiorJetPackaging` to `osx-app-bundle` will result in the creation
of an application bundle and, optionally, a native OS X installer package (`.pkg` file).

#### TestRunTask
Before compiling the application with Excelsior JET, it can be useful to run it 
on the Excelsior JET JVM using a JIT compiler. This so-called Test Run helps Excelsior JET:

* verify that your application can be executed successfully on the Excelsior JET JVM.
  Usually, if the Test Run completes normally, the natively compiled application also works well.
* detect the optional parts of the Excelsior JET Runtime that are used by your application.
* collect profile information to optimize your app more effectively. The profiles will be used by the Startup Optimizer
  and the Global Optimizer.

To provide a Test Run for your plugin you may use
[TestRunTask](https://github.com/excelsior-oss/excelsior-jet-api/blob/master/src/main/java/com/excelsiorjet/api/tasks/TestRunTask.java)
that takes a `JetProject` instance in its constructor and provides the `execute()`  method that performs a Test Run.
