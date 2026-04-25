@rem
@rem Gradle wrapper batch script
@rem

set DIRNAME=%~dp0
set JAVA_EXE=java.exe
set CLASSPATH=%DIRNAME%gradle\wrapper\gradle-wrapper.jar

%JAVA_EXE% -classpath %CLASSPATH% org.gradle.wrapper.GradleWrapperMain %*
