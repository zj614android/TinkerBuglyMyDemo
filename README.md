## 前言

[上一篇](http://blog.csdn.net/user11223344abc/article/details/78516567)我们将sdk接入了进来，那么这一篇我们来模拟一次真实项目中使用热更新的场景，当然，是结合bugly来做。

还记得我们上一篇开始说了准备一个自带bug的项目（目录2,3），这里再来回忆下：


## 流程梳理
- 1.集成tinker sdk
- 2.打出基线版本，build内生成一个bakapk的目录
- 3.bug修复，java代码，so文件，资源文件。
- 4.修改tinker-support.gradle内的baseApkDir为基准包当前的路径名称为当前基线版本（上2生成出来的）的路径。
- 5.修改tinker-support.gradle内的tinkerId
- 6.打出补丁版本
- 7.基线版本上报联网
- 8.上传补丁包
- 9.补丁下发成功，热更新完成

那么我们就照着这个流程一步步的实现热更新吧（下文关于集成sdk的部分我就略了，因为上一篇已经讲了这部分内容了）。

## step1:打出基线版本
这个没啥可说的，在本系列第一篇博客内就写了tinker脚本gradle task的打包的方式，就是在正确接入bugly sdk之后，我们去gradle task内找到这俩个task进行基准包的构建（app/Tasks/build下的assembleRelease或assembleDebug），这里我选择使用release的，顺带提一句，app/gradle内记得配置打包参数，否则执行这个task打出来的包还是未签名的。

完成之后，build内bakapk目录下生成了如下3个文件：
> app-release.apk
app-release-mapping.txt
app-release-R.txt

另外提醒下打包之前注意配置好打包信息：
```java
    signingConfigs {
        // your debug keystore
        debug {
            storeFile file("buglytestreleasekey.jks")
            storePassword "buglytestreleasekey"
            keyAlias "buglytestreleasekey"
            keyPassword "buglytestreleasekey"
        }

        release{
            storeFile file("buglytestreleasekey.jks")
            storePassword "buglytestreleasekey"
            keyAlias "buglytestreleasekey"
            keyPassword "buglytestreleasekey"
        }
    }

    buildTypes {
        release {
            minifyEnabled true
            proguardFiles getDefaultProguardFile('proguard-android.txt'), 'proguard-rules.pro'
            signingConfig signingConfigs.release  //这里比较重要 不要漏掉了
        }
    }
```

## step2:修改tinker-support.gradle内的baseApkDir为基准包当前的路径名称
```java
apply plugin: 'com.tencent.bugly.tinker-support'

def bakPath = file("${buildDir}/bakApk/")

/**
 * 此处填写每次构建生成的基准包目录
 */
def baseApkDir = "app-0208-15-10-00"

/**
 * 对于插件各参数的详细解析请参考
 */
tinkerSupport {
...
```

## step3:修改tinker-support.gradle内的tinkerId
这个参数官网建议是一一对应，譬如：
>基准包:base-1.0.0，对应补丁包:patch-1.0.0
基准包:base-1.0.1，对应补丁包:patch-1.0.1
......
基准包:base-8.0.1，对应补丁包:patch-8.0.1

所以我们定义这个的时候最好也按照官网的来吧。并且需要注意的是，这个版本号的根据是你项目内gradle文件内的version code，也就是跟真实版本号同步。

## step4:修改项目代码内的bug
这个根据每个项目自身情况来改，就本文而言，我所做的事就是：
1.将1+1=3 改为 1+1=2
2.将中间图标叉换成android默认图标
3.将按钮跳转的逻辑崩溃bug代码注释掉，更换成正确的跳转逻辑代码。

## step5:打出补丁版本
怎么打呢？还是和tinker一样的，去找它的gradle task。
路径为：app/Tasks/tinker-support/buildTinkerPatchRelease

完成之后去这俩个目录下检查看有无生成东西：
> TinkerBuglyDemo\app\build\outputs\patch
TinkerBuglyDemo\app\build\outputs\tinkerPatch

不出意外肯定是有的，这里简单说下这俩个目录的作用：
**patch：**
目录内有三个文件，这三个文件内就包含了补丁文件：
> patch_signed.apk
patch_signed_7zip.apk
patch_unsigned.apk

一个个的说。
![](https://wx4.sinaimg.cn/mw1024/0061ejqJgy1flvwim9zfej311y0jagqh.jpg)
> Created-Time: 2017-11-26 23:09:21.772
Created-By: YaFix(1.1)
YaPatchType: 2
VersionName: 1.0
VersionCode: 1
From: zjbase-1.0.1
To: zjpatch-1.0.1

这个mf文件内记载的是一些版本相关的信息，关注最后那个from和to。
这个目录下这几个apk都有这个YAPATCH.MF文件。

另外需要注意的是，这里这个7zip的文件就是我们的补丁包。一会我们要上传到bugly后台去的。

**tinkerPatch**
这个里头信息很多，这个目录是tinker最原始的修改文件，当我们出了问题的时候可以进来看下这个目录下的记录来排查我们操作究竟是那儿出了问题。

## step6:基线版本上报联网
上报给谁，上报给bugly。这都是bugly sdk帮我们做了的事。

这个过程其实很简单，但是很重要，因为这一步不确认OK的话，我们的补丁是无法上传到bugly后台去的。

具体操作就是，安装基线版本，打开基线版本，观察是否上报联网成功了。

log:
```java
11-26 23:50:47.977 3830-3851/zj.tinkerbuglydemo D/CrashReport: [Upload] Run upload task with cmd: 804
11-26 23:50:47.997 3830-3851/zj.tinkerbuglydemo D/CrashReport: [Upload] Upload to http://android.bugly.qq.com/rqd/async?aid=b15e26f3-2efa-4bac-9d5e-4cb5a28f9c3a with cmd 804 (pid=3830 | tid=3851).
11-26 23:50:48.127 3830-3851/zj.tinkerbuglydemo I/CrashReport: [Upload] Success: 804
11-26 23:50:48.137 3830-3851/zj.tinkerbuglydemo I/CrashReport: upload succ:[804] [sended 813] [recevied 129]
```

804说明是上报联网ok了。接下来我们要去bugly平台上传我们的补丁了。（bugly平台的注册在上一篇有提）

## step6:上传补丁包
2张图来演示操作步骤
![](https://wx2.sinaimg.cn/mw1024/0061ejqJgy1flvxjid0rhj311x0gq0tn.jpg)
![](https://wx2.sinaimg.cn/mw1024/0061ejqJgy1flvxmg3lr4j30pp0exweq.jpg)

至于一些细节，测试设备应该还要去bugly备案，这里我就选择全量设备了。这里上传的补丁就是上面打出来补丁包内的7zip文件，跟tinker一样的。

然后我们店立即下发。点完之后我们重启app然后就能看见我们改动的东西了。

不过有时候可能下发会稍有些延迟，不知道为啥，可能是我家里网比较慢。后续再对热更新进行一些高级的操作。

温馨提示：
如果大家要测试，请用本文的demo内的key，因为上一篇的blog中的key是有问题的。
还有一个小小的遗憾就是没有对so文件进行热更新。就是一个复制黏贴的操作，所以就先这样吧。

效果如图：
![](https://wx3.sinaimg.cn/mw690/0061ejqJgy1flvy4s6rmkg309d0et0ze.gif)

## 附上Demo
[apk测试地址](https://github.com/zj614android/TinkerBuglyMyDemo/blob/master/zipbackup/bugly%E7%83%AD%E6%9B%B4%E6%96%B0%E6%B5%8B%E8%AF%95apk.zip)
[本文demo代码下载地址](https://github.com/zj614android/TinkerBuglyMyDemo/blob/master/zipbackup/bugly%E7%83%AD%E6%9B%B4%E6%96%B0%E6%B5%8B%E8%AF%95%E4%BB%A3%E7%A0%81TinkerBuglyDemo.zip)
[github](https://github.com/zj614android/TinkerBuglyMyDemo)



