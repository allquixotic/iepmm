 # Internet Explorer Protected Mode Manager
 ## (IEPMM)
 
 ## Summary
 
 This Java library manages the state of Internet Explorer's Protected Mode flags. In IE's security model, each of the four security zones has a separate Protected Mode flag.
 
 If the flag is enabled, IE will behave a bit more "safely", but breaking backwards compatibility with very, *very* old sites that may expect the bad old IE 6 style behavior (ActiveX, really old HTML, etc.) This is definitely the mode you want turned on in the "Internet" zone if you use IE to browse random public Internet websites.
 
 If the flag is disabled, IE will behave closer to the original functionality of IE that had a myriad of terrible security problems to accompany its many features. This may be OK on specific LAN sites, or trusted sites where you are relying on old software. An example of such a site you might need in the modern era of the Web is the HPE Application Lifecycle Management (ALM) application, which is (still) served as an ActiveX control.
 
 Every time you visit a website in IE, the browser categorizes that website into one of the four zones. When you move from one zone to another, if the "Enable Protected Mode" flag *changes* from the previous zone to the new zone, IE will *destroy* the entire browser instance it had before, and recreate a new one using the new protected mode setting. 
 
 For example, if I have:
  - Internet zone with the Protected Mode enabled
  - Intranet zone with the Protected Mode disabled
  - I navigate from a site in Intranet zone to Internet (or vice versa)
 
 In this case, IE will free up all resources that were associated with the original browser instance, and create a new one. That means the Process ID (pid) associated with the original browser will be lost, as will its Window Handle (HWND).
 
 ## Why do we need a tool to change these flags?
 
 The IEPMM is a Java-based library for modifying these flags dynamically, by updating the registry. This turns out to be very important for testing websites using [Selenium](https://seleniumhq.org) and Internet Explorer, given the following tend to be true:

 - In corporate environments, in my experience, the default settings are: Internet and Restricted Sites, Protected Mode is enabled. Local Intranet and Trusted Sites, Protected Mode is disabled.
 - Corporate Group Policy configurations often prevent end-users from changing these settings.
 - However, the settings from Group Policy or the `HKEY_LOCAL_MACHINE` tree can be *overridden* by writing to the `HKEY_CURRENT_USER` tree of the registry, which is typically read/write to the user in many environments.
 - [Selenium](https://seleniumhq.org)-based automated tests do not work reliably when traversing zones with different Protected Mode values.

 **In summary, this library is intended to be used as part of your Selenium WebDriver test cases, if the following are true:**
 
 1. Your Group Policy or System-wide Protected Mode settings differ between zones.
 2. You have test cases that have to traverse zone boundaries with different Protected Mode settings.
 3. You are unable to change the Protected Mode settings using the UI in Internet Options.
 4. You need to avoid using the [INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS](https://seleniumhq.github.io/selenium/docs/api/java/org/openqa/selenium/ie/InternetExplorerDriver.html#INTRODUCE_FLAKINESS_BY_IGNORING_SECURITY_DOMAINS) flag because it, well, makes your tests *flaky* (meaning: unreliable/unstable), causing spurious test failures or broken functionality. 
 
 Here is how the flags look in Internet Options:
 
 ![Image of Internet Options on Windows 10 displaying Enable Protected Mode flag](https://i.imgur.com/hV68b1v.png)
 
 However, if your Group Policy is managing these settings, the checkboxes will be grayed out.

 ## How might I use this tool?
 
```java
 IEProtectedModeManager ieppm = new IEProtectedModeManager();
 ieppm.enableAll();
 WebDriver wd = new InternetExplorerDriver();
 //Now run your test with the consistent "Protected Mode" setting - in this case, all enabled
 ieppm.restoreOrigState();
 //Now the original state of the flags is restored, whatever that was
 ```
 
 Note that, by default, IEPMM adds a shutdown hook to your Java process that will restore the original state of the flags when the associated JVM exits. To *disable* this behavior (so that your settings will stick after the JVM exits), add this line:
 
```java
ieppm.setRestoreOnExit(false);
```

 ## Dependencies
  - Maven 3.3 or newer
  - Recent JNA and Guava for runtime
  - For unit testing IEPMM itself: JMockit, JUnit 4.x, AssertJ 2.x
  - Java 7 or later
 
 ## To-do list
  - Documentation (JavaDocs)
  - Code comments
  - CI/CD
  - Complete the unit tests
  - Get this library hosted on Maven Central
 
 ## License
  - [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)
 
 ## Further Reading
 1. http://jimevansmusic.blogspot.com/2012/08/youre-doing-it-wrong-protected-mode-and.html
 2. https://stackoverflow.com/questions/29642940/unable-to-open-ie11-driver-instance-using-selenium-webdriver-with-java