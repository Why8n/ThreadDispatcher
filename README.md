

[![License](https://img.shields.io/badge/license-Apache%202-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

ThreadDispatcher
----------------
[**中文文档**](http://www.jianshu.com/p/beb428f9ef6b)

easy thread dispatch for code block/method library for Java/Android.
* switch thread for code block/method only by one single calling.
* support switch methods executing thread by annotating methods with @Switch.
* support methods alias by annotating methods with @Switch,then you can call the method with alias name.
* support public,non-public and static method dispatch.
* support getting result from async calling.


ThreadDispatcher in 3 steps
---------------------------
&emsp;1. configuration
```Java
//configure defautl switcher
Switcher.builder()
        .setUiExecutor(new UiDispatcher())
        .setIndex(new DispatcherIndex())
        .installDefaultThreadDispatcher();
```
&emsp;2. dispatching
```Java
//normal usage
Switcher.getDefault().main(new Runnable() {
         @Override
         public void run() {
             //this will run on main thread
             String msg = "main in main thread--> thread.name = " + Thread.currentThread().getName();
             log(msg);
             toast(msg);
         }
     }).background(new Runnable() {
         @Override
         public void run() {
             //this will run on background thread
             String msg = "background in main thread --> thread.name = " + Thread.currentThread().getName();
             log(msg);
             toast(msg);
         }
     });

//interface method dispatch
 public interface ITestInterface {
    @Switch(threadMode = MAIN)
    void doMain(List<String> test);
}
ITestInterface proxy = Switcher.getDefault().create(this);
proxy.doMain(null);

//alias dispatch
@Switch(alias = "runPublic", threadMode = ThreadMode.BACKGROUND)
   public void publicMethod(int i) {
       log(String.format("publicMethod:threadMode = %s, Thread.name = %s",
               ThreadMode.BACKGROUND, Thread.currentThread().getName()));
       log("publicMethod:params = " + i);
   }
//calling method by alias
switcher.run("runPublic", this,1);
```
&emsp;3. remember to stop pending tasks when exit.
```Java
Switcher.getDefault().shutdown();
```
Download
--------
Via Gradle:
```groovy
compile 'com.whyn:threaddispatcher:1.1.1'
```
if it is Android porject,add:
```groovy
compile 'com.whyn:threaddispatcher4android:1.0.0'
```
if you wanna use alias method,add:
```groovy
annotationProcessor 'com.whyn:threaddispatcherprocessor:1.1.1'

#app build.gradle
android {
    defaultConfig{
    ···
    ···
        javaCompileOptions {
            annotationProcessorOptions {
                arguments = [dispatcherIndex: 'com.yn.DispatcherIndex'] //generated file package name
            }
        }
    }
}
```
License
-------

    Copyright 2017 Whyn

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
