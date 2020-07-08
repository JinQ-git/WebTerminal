# Web Browser Terminal

## Overview

[See here](doc/Overview.md)

## Build Projects

All Java sources are in **src** directory, compile it with the *Tomcat Library* and place under *WebContent/WEB-INF/classes*.

If you want use *eclipse* or *VSCode* to build this java project, create **.project** and **.classpath** files in project root directory.

**.project**
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<projectDescription>
  <name>DevTerminal</name>
  <comment/>
  <projects>&#xD;
    </projects>
  <buildSpec>
    <buildCommand>
      <name>org.eclipse.jdt.core.javabuilder</name>
      <arguments>&#xD;
            </arguments>
    </buildCommand>
  </buildSpec>
  <natures>
    <nature>org.eclipse.jdt.core.javanature</nature>
  </natures>
</projectDescription>
```

**.classpath**

> **NOTE:** You have to switch **${ABSOLUTE_PATH_TO_...}** to your project environment value.

```xml
<?xml version="1.0" encoding="UTF-8"?>
<classpath>
	<classpathentry kind="con" path="org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-9"/>
	<classpathentry kind="src" path="src"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_PROJECT_ROOT}/WebContent/WEB-INF/lib/jsch-0.1.55.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_PROJECT_ROOT}/WebContent/WEB-INF/lib/gson-2.8.6.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_TOMCAT}/lib/jsp-api.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_TOMCAT}/lib/websocket-api.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_TOMCAT}/lib/servlet-api.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_TOMCAT}/lib/annotations-api.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_TOMCAT}/lib/el-api.jar"/>
	<classpathentry kind="lib" path="${ABSOLUTE_PATH_TO_TOMCAT}/lib/jaspic-api.jar"/>
	<classpathentry kind="output" path="WebContent/WEB-INF/classes"/>
</classpath>
```

## **WAR** packaging

After build the project, just packaging all files in _WebContent_ directory. If you use command-line tool, try following command.

```sh
$ cd ./WebContent
$ jar cvf WebTerminal.war *
```

