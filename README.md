# Swan

## 关于 JNI 和 APM 需要了解的基础知识

前段时间在学习 JNI 和性能优化相关的知识，这里面涉及的点真的是太多了。

- C/C++ 、操作系统基础知识
- 如何检测 Native Crash ？使用信号需要注意什么？如何获取函数堆栈信息？
- 如何 Hook 系统函数？比如发生 ANR 时如何获取系统日志信息？
- 如何绕过系统限制调用系统方法？比如 KOOM 是如何实现暂停和恢复线程的？

很多优秀的开源库已经实现了上述功能，如 xCrash、xHook、xDL（蔡克伦🐮🍺）、KOOM、Matrix 等。实际开发中，我们可以直接拿来用，但是还是很有必要学习了解其中的基础知识及其原理。这样才能得心应手，或者二次开发自定义功能。

下面总结了一个大纲，如有遗漏后面会补充，接下来就是慢慢完善，希望有更多的人一起学习交流，也希望能给学习 JNI 和性能优化的小伙伴提供一些帮助，少走一些弯路。

### 一、C/C++ 基础

[第13章 高级指针话题](https://www.notion.so/13-d23f13198a934b439eb73ced204c69b9)

[****C 陷阱与缺陷****](https://www.notion.so/C-584c55b51c6a4d52931ec8e591488ce7)

[C 专家编程](https://www.notion.so/C-5c11d5f59ecd4447ae8a01854350b3a9)

[****C++ Primer 中文版（第 5 版）****](https://www.notion.so/C-Primer-5-bdb8fe729b4c47f88e80591f2d0d4dba)

[Unix环境高级编程（第三版）](https://www.notion.so/Unix-29d76046cbff481384d444f2d2060e32)

### 二、 JNI 基础

[JNI（一）JNI 访问 Java 中的成员变量和方法](https://www.notion.so/JNI-JNI-Java-1d55b467f40f4bc093ecf999d62784f7)

[JNI（二）本地C代码创建Java对象及引用](https://www.notion.so/JNI-C-Java-b36eedf07a25442b8e70ef6675587695)

[JNI（三）JNI 静态注册与动态注册](https://www.notion.so/JNI-JNI-4ad7c2f677514b6dacd21d0e530cb058)

[JNI 参考资料](https://www.notion.so/JNI-4bd0e5e72eab40879aaa8fe6cb380cb9)

### 三、so 相关

[静态链接](https://www.notion.so/b45eaaada0cf45439da7c416ffde6318)

[动态链接](https://www.notion.so/94806eb83a804c63911c8864a3678df1)

[目标文件格式](https://www.notion.so/3c4857ec29d54836b05d3c234e837389)

[自己实现一个动态链接库](https://www.notion.so/8a1939f7efec4b959218696d9f9d53d6)

[如何 Hook 系统函数](https://www.notion.so/Hook-748356fa425a4a57b343045fe527792d)

[虚拟内存](https://www.notion.so/92ff6ef4a7b140a4bc0562cb6db0dbaf)

### 四、APM 专项

[崩溃优化](https://www.notion.so/dc775e0772cb429390731a47b3aad9ef)

[内存优化](https://www.notion.so/0ee31e7a22054fd2bc87568c2242a147)

[卡顿优化](https://www.notion.so/183da63647a34e31a19bd506b8c2d218)