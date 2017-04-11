# start_undeclared_activity
Start an activity that is not declared in the AndroidManifest.xml (It's a demo, just for fun)

## 原理：
1. 生成ActivityManager的代理，调用startActiivty时将Intent替换为StubActivity（这个Activity已经在AndroidManifest中注册），并将原来的保存在Extra中。
2. 上面一个步骤将骗过System process中的ActivityManagerService，最终回到应用进程的Handler。
3. 为ActivityThread的Handler设置一个callback，Handler中的callback会优先执行，返回FALSE，不影响后面的流程。
4. 在Handler的callback中取到启动的Intent并替换成第一步保存在Extra中的Intent。
