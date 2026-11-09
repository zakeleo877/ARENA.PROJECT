# CI diagnostics

Run: 42711459008
Commit: fc447a80b3f0027750d3e7113ef58581fdb731c3

## runner probe
```
ANDROID_HOME=/usr/local/lib/android/sdk
ANDROID_SDK_ROOT=/usr/local/lib/android/sdk
--- /usr/local/lib/android/sdk ---
total 72
drwxrwxrwx 11 root root  4096 Nov  1 21:33 .
drwxr-xr-x  3 root root  4096 Nov  1 21:30 ..
-rw-r--r--  1 root root    16 Nov  1 21:33 .knownPackages
drwxrwxrwx  2 root root  4096 Nov  1 21:33 .temp
drwxrwxrwx  8 root root  4096 Nov  1 21:30 build-tools
drwxrwxrwx  4 root root  4096 Nov  1 21:33 cmake
drwxrwxrwx  3 root root  4096 Nov  1 21:30 cmdline-tools
drwxrwxrwx  4 root root  4096 Nov  1 21:33 extras
drwxrwxrwx  2 root root  4096 Nov  1 21:30 licenses
drwxrwxrwx  5 root root  4096 Nov  1 21:33 ndk
-rw-r--r--  1 root root 23609 Nov  1 21:33 packages-list.txt
drwxrwxrwx  3 root root  4096 Nov  1 21:30 platform-tools
drwxrwxrwx 20 root root  4096 Nov  1 21:31 platforms
--- /opt/android ---
ls: cannot access '/opt/android': No such file or directory
--- /home/runner/Android ---
ls: cannot access '/home/runner/Android': No such file or directory
--- sdkmanager binaries ---
/usr/local/lib/android/sdk/cmdline-tools/latest/bin/sdkmanager
--- java ---
openjdk version "17.0.21" 2026-10-20
OpenJDK Runtime Environment Temurin-17.0.21+9 (build 17.0.21+9)
OpenJDK 64-Bit Server VM Temurin-17.0.21+9 (build 17.0.21+9, mixed mode, sharing)
--- installed platforms directory ---
android-34
android-34-ext10
android-34-ext11
android-34-ext12
android-34-ext8
android-35
android-35-ext14
android-35-ext15
android-36
android-36-ext18
android-36-ext19
android-36.1
android-37.0
android-37.1
android-37.2
android-37.2-beta1
android-37.2-beta2
android-37.2-beta3
--- installed build-tools directory ---
34.0.0
35.0.0
35.0.1
36.0.0
36.1.0
37.0.0
--- sdkmanager --list (filtered) ---
  build-tools;34.0.0                 | 34.0.0        | Android SDK Build-Tools 34       | build-tools/34.0.0                
  build-tools;35.0.0                 | 35.0.0        | Android SDK Build-Tools 35       | build-tools/35.0.0                
  build-tools;35.0.1                 | 35.0.1        | Android SDK Build-Tools 35.0.1   | build-tools/35.0.1                
  build-tools;36.0.0                 | 36.0.0        | Android SDK Build-Tools 36       | build-tools/36.0.0                
  build-tools;36.1.0                 | 36.1.0        | Android SDK Build-Tools 36.1     | build-tools/36.1.0                
  build-tools;37.0.0                 | 37.0.0        | Android SDK Build-Tools 37       | build-tools/37.0.0                
  platforms;android-34               | 3             | Android SDK Platform 34          | platforms/android-34              
  platforms;android-34-ext10         | 1             | Android SDK Platform 34-ext10    | platforms/android-34-ext10        
  platforms;android-34-ext11         | 1             | Android SDK Platform 34-ext11    | platforms/android-34-ext11        
  platforms;android-34-ext12         | 1             | Android SDK Platform 34-ext12    | platforms/android-34-ext12        
  platforms;android-34-ext8          | 1             | Android SDK Platform 34-ext8     | platforms/android-34-ext8         
  platforms;android-35               | 2             | Android SDK Platform 35          | platforms/android-35              
  platforms;android-35-ext14         | 1             | Android SDK Platform 35-ext14    | platforms/android-35-ext14        
  platforms;android-35-ext15         | 1             | Android SDK Platform 35-ext15    | platforms/android-35-ext15        
  platforms;android-36               | 2             | Android SDK Platform 36          | platforms/android-36              
  platforms;android-36-ext18         | 1             | Android SDK Platform 36-ext18    | platforms/android-36-ext18        
  platforms;android-36-ext19         | 1             | Android SDK Platform 36-ext19    | platforms/android-36-ext19        
  platforms;android-36.1             | 1             | Android SDK Platform 36.1        | platforms/android-36.1            
  platforms;android-37.0             | 2             | Android SDK Platform 37.0        | platforms/android-37.0            
  platforms;android-37.1             | 1             | Android SDK Platform 37.1        | platforms/android-37.1            
  platforms;android-37.2             | 2             | Android SDK Platform 37.2        | platforms/android-37.2            
  platforms;android-37.2-beta1       | 1             | Android SDK Platform 37.2-beta1  | platforms/android-37.2-beta1      
  platforms;android-37.2-beta2       | 2             | Android SDK Platform 37.2-beta2  | platforms/android-37.2-beta2      
  platforms;android-37.2-beta3       | 3             | Android SDK Platform 37.2-beta3  | platforms/android-37.2-beta3      
  build-tools;19.1.0                                                              | 19.1.0            | Android SDK Build-Tools 19.1                                                    
  build-tools;20.0.0                                                              | 20.0.0            | Android SDK Build-Tools 20                                                      
  build-tools;21.1.2                                                              | 21.1.2            | Android SDK Build-Tools 21.1.2                                                  
  build-tools;22.0.1                                                              | 22.0.1            | Android SDK Build-Tools 22.0.1                                                  
  build-tools;23.0.1                                                              | 23.0.1            | Android SDK Build-Tools 23.0.1                                                  
  build-tools;23.0.2                                                              | 23.0.2            | Android SDK Build-Tools 23.0.2                                                  
  build-tools;23.0.3                                                              | 23.0.3            | Android SDK Build-Tools 23.0.3                                                  
  build-tools;24.0.0                                                              | 24.0.0            | Android SDK Build-Tools 24                                                      
  build-tools;24.0.1                                                              | 24.0.1            | Android SDK Build-Tools 24.0.1                                                  
  build-tools;24.0.2                                                              | 24.0.2            | Android SDK Build-Tools 24.0.2                                                  
  build-tools;24.0.3                                                              | 24.0.3            | Android SDK Build-Tools 24.0.3                                                  
  build-tools;25.0.0                                                              | 25.0.0            | Android SDK Build-Tools 25                                                      
  build-tools;25.0.1                                                              | 25.0.1            | Android SDK Build-Tools 25.0.1                                                  
  build-tools;25.0.2                                                              | 25.0.2            | Android SDK Build-Tools 25.0.2                                                  
  build-tools;25.0.3                                                              | 25.0.3            | Android SDK Build-Tools 25.0.3                                                  
  build-tools;26.0.0                                                              | 26.0.0            | Android SDK Build-Tools 26                                                      
grep: write error: Broken pipe
--- sdkmanager install output ---
Loading package information...                                                  Loading local repository...                                                     [                                       ] 3% Loading local repository...        [                                       ] 3% Fetch remote repository...         [=                                      ] 3% Fetch remote repository...         [=                                      ] 4% Fetch remote repository...         [=                                      ] 5% Fetch remote repository...         [==                                     ] 5% Fetch remote repository...         [==                                     ] 6% Fetch remote repository...         [==                                     ] 7% Fetch remote repository...         [==                                     ] 7% Computing updates...               [===                                    ] 8% Computing updates...               [===                                    ] 10% Computing updates...              [=======================================] 100% Computing updates...             

install status: 0
```

## version probe
```
## Plugin and library versions
200  https://plugins.gradle.org/m2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/2.4.21/org.jetbrains.kotlin.plugin.compose.gradle.plugin-2.4.21.pom
200  https://plugins.gradle.org/m2/org/jetbrains/kotlin/plugin/compose/org.jetbrains.kotlin.plugin.compose.gradle.plugin/2.4.20/org.jetbrains.kotlin.plugin.compose.gradle.plugin-2.4.20.pom
303  https://plugins.gradle.org/m2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/2.3.12/com.google.devtools.ksp.gradle.plugin-2.3.12.pom
404  https://repo1.maven.org/maven2/androidx/room3/room3-runtime/3.0.3/room3-runtime-3.0.3.pom
200  https://repo1.maven.org/maven2/com/google/dagger/hilt-android/2.60.1/hilt-android-2.60.1.pom
404  https://repo1.maven.org/maven2/androidx/compose/compose-bom/2026.09.00/compose-bom-2026.09.00.pom
200  https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/9.5.0/gradle-9.5.0.pom
```

## Package verification
```
PASS: 1. resources.arsc is stored uncompressed
PASS: 2. zipalign verified
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v3.2 scheme (APK Signature Scheme v3.2): false
Verified using v4 scheme (APK Signature Scheme v4): false
Verified for SourceStamp: false
Number of signers: 1
V2 Signer: certificate DN: CN=Saeed Stores, OU=Dev, O=Saeed, L=Algiers, C=DZ
V2 Signer: certificate SHA-256 digest: 016a23ace3e322e1b925d136fc9ef77c4b7dd4b3514dc391ab46de08140e66c6
V2 Signer: certificate SHA-1 digest: a6224c5a653d0971e6e2e267bac07ecb306d9fad
V2 Signer: certificate MD5 digest: 32af3e5fa0517733bdbaab310957f232
V2 Signer: key algorithm: RSA
V2 Signer: key size (bits): 2048
V2 Signer: public key SHA-256 digest: dedee111b1ee2ff92f05cd753a09989210314c7b56a31b04b9f2ae5e983f605d
V2 Signer: public key SHA-1 digest: 80d007e2406504bff20119b1d59d2749f31a505a
V2 Signer: public key MD5 digest: 6278859592d99a5a3de345544f1b61f7
PASS: 3. APK is signed with v2 or newer
package: name='com.zakeleo877.saeedstore' versionCode='1' versionName='1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
application-label:'محلات السعيد'
application-label-ar:'محلات السعيد'
PASS: 4. minSdkVersion is 24
PASS: 4. versionName is 1.0
PASS: 5. APK is not debuggable
PASS: 6. exported attribute present
PASS: 7. package integrity verified
package: com.zakeleo877.saeedstore
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.ACCESS_COARSE_LOCATION'
uses-permission: name='android.permission.ACCESS_FINE_LOCATION'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='android.permission.RECEIVE_BOOT_COMPLETED'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
permission: com.zakeleo877.saeedstore.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.zakeleo877.saeedstore.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
PASS: 8. no unexpected permissions
PASS: 9. customer APK contains no admin code
PASS: 10. APK size is 2788 KiB (limit 20 MiB)

All package checks passed for saeed-store.
PASS: 1. resources.arsc is stored uncompressed
PASS: 2. zipalign verified
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v3.2 scheme (APK Signature Scheme v3.2): false
Verified using v4 scheme (APK Signature Scheme v4): false
Verified for SourceStamp: false
Number of signers: 1
V2 Signer: certificate DN: CN=Saeed Stores, OU=Dev, O=Saeed, L=Algiers, C=DZ
V2 Signer: certificate SHA-256 digest: 016a23ace3e322e1b925d136fc9ef77c4b7dd4b3514dc391ab46de08140e66c6
V2 Signer: certificate SHA-1 digest: a6224c5a653d0971e6e2e267bac07ecb306d9fad
V2 Signer: certificate MD5 digest: 32af3e5fa0517733bdbaab310957f232
V2 Signer: key algorithm: RSA
V2 Signer: key size (bits): 2048
V2 Signer: public key SHA-256 digest: dedee111b1ee2ff92f05cd753a09989210314c7b56a31b04b9f2ae5e983f605d
V2 Signer: public key SHA-1 digest: 80d007e2406504bff20119b1d59d2749f31a505a
V2 Signer: public key MD5 digest: 6278859592d99a5a3de345544f1b61f7
PASS: 3. APK is signed with v2 or newer
package: name='com.zakeleo877.saeedadmin' versionCode='1' versionName='1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
application-label:'إدارة محلات السعيد'
application-label-ar:'إدارة محلات السعيد'
PASS: 4. minSdkVersion is 24
PASS: 4. versionName is 1.0
PASS: 5. APK is not debuggable
PASS: 6. exported attribute present
PASS: 7. package integrity verified
package: com.zakeleo877.saeedadmin
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.RECEIVE_BOOT_COMPLETED'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
permission: com.zakeleo877.saeedadmin.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.zakeleo877.saeedadmin.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
PASS: 8. no unexpected permissions
PASS: 9. admin APK contains no customer code
PASS: 10. APK size is 2585 KiB (limit 20 MiB)

All package checks passed for saeed-admin.
```

## Lint issues
```
(not reached)
```

## Unit test failures
```
## Optional orders API
```
PASS: optional orders API returns the four public settings.
```

## Package verification
```
PASS: 1. resources.arsc is stored uncompressed
PASS: 2. zipalign verified
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v3.2 scheme (APK Signature Scheme v3.2): false
Verified using v4 scheme (APK Signature Scheme v4): false
Verified for SourceStamp: false
Number of signers: 1
V2 Signer: certificate DN: CN=Saeed Stores, OU=Dev, O=Saeed, L=Algiers, C=DZ
V2 Signer: certificate SHA-256 digest: 016a23ace3e322e1b925d136fc9ef77c4b7dd4b3514dc391ab46de08140e66c6
V2 Signer: certificate SHA-1 digest: a6224c5a653d0971e6e2e267bac07ecb306d9fad
V2 Signer: certificate MD5 digest: 32af3e5fa0517733bdbaab310957f232
V2 Signer: key algorithm: RSA
V2 Signer: key size (bits): 2048
V2 Signer: public key SHA-256 digest: dedee111b1ee2ff92f05cd753a09989210314c7b56a31b04b9f2ae5e983f605d
V2 Signer: public key SHA-1 digest: 80d007e2406504bff20119b1d59d2749f31a505a
V2 Signer: public key MD5 digest: 6278859592d99a5a3de345544f1b61f7
PASS: 3. APK is signed with v2 or newer
package: name='com.zakeleo877.saeedstore' versionCode='1' versionName='1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
application-label:'محلات السعيد'
application-label-ar:'محلات السعيد'
PASS: 4. minSdkVersion is 24
PASS: 4. versionName is 1.0
PASS: 5. APK is not debuggable
PASS: 6. exported attribute present
PASS: 7. package integrity verified
package: com.zakeleo877.saeedstore
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.ACCESS_COARSE_LOCATION'
uses-permission: name='android.permission.ACCESS_FINE_LOCATION'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='android.permission.RECEIVE_BOOT_COMPLETED'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
permission: com.zakeleo877.saeedstore.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.zakeleo877.saeedstore.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
PASS: 8. no unexpected permissions
PASS: 9. customer APK contains no admin code
PASS: 10. APK size is 2788 KiB (limit 20 MiB)

All package checks passed for saeed-store.
PASS: 1. resources.arsc is stored uncompressed
PASS: 2. zipalign verified
Verifies
Verified using v1 scheme (JAR signing): false
Verified using v2 scheme (APK Signature Scheme v2): true
Verified using v3 scheme (APK Signature Scheme v3): false
Verified using v3.1 scheme (APK Signature Scheme v3.1): false
Verified using v3.2 scheme (APK Signature Scheme v3.2): false
Verified using v4 scheme (APK Signature Scheme v4): false
Verified for SourceStamp: false
Number of signers: 1
V2 Signer: certificate DN: CN=Saeed Stores, OU=Dev, O=Saeed, L=Algiers, C=DZ
V2 Signer: certificate SHA-256 digest: 016a23ace3e322e1b925d136fc9ef77c4b7dd4b3514dc391ab46de08140e66c6
V2 Signer: certificate SHA-1 digest: a6224c5a653d0971e6e2e267bac07ecb306d9fad
V2 Signer: certificate MD5 digest: 32af3e5fa0517733bdbaab310957f232
V2 Signer: key algorithm: RSA
V2 Signer: key size (bits): 2048
V2 Signer: public key SHA-256 digest: dedee111b1ee2ff92f05cd753a09989210314c7b56a31b04b9f2ae5e983f605d
V2 Signer: public key SHA-1 digest: 80d007e2406504bff20119b1d59d2749f31a505a
V2 Signer: public key MD5 digest: 6278859592d99a5a3de345544f1b61f7
PASS: 3. APK is signed with v2 or newer
package: name='com.zakeleo877.saeedadmin' versionCode='1' versionName='1.0' platformBuildVersionName='17' platformBuildVersionCode='37' compileSdkVersion='37' compileSdkVersionCodename='17'
application-label:'إدارة محلات السعيد'
application-label-ar:'إدارة محلات السعيد'
PASS: 4. minSdkVersion is 24
PASS: 4. versionName is 1.0
PASS: 5. APK is not debuggable
PASS: 6. exported attribute present
PASS: 7. package integrity verified
package: com.zakeleo877.saeedadmin
uses-permission: name='android.permission.INTERNET'
uses-permission: name='android.permission.ACCESS_NETWORK_STATE'
uses-permission: name='android.permission.POST_NOTIFICATIONS'
uses-permission: name='android.permission.RECEIVE_BOOT_COMPLETED'
uses-permission: name='android.permission.WAKE_LOCK'
uses-permission: name='android.permission.FOREGROUND_SERVICE'
permission: com.zakeleo877.saeedadmin.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
uses-permission: name='com.zakeleo877.saeedadmin.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION'
PASS: 8. no unexpected permissions
PASS: 9. admin APK contains no customer code
PASS: 10. APK size is 2585 KiB (limit 20 MiB)

All package checks passed for saeed-admin.
```

## Lint issues
```
(not reached)
```

## Unit test failures
### ./customer/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedstore.ui.DemoCatalogTest.xml
### ./customer/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedstore.ui.CatalogFilteringTest.xml
### ./customer/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedstore.ui.CustomerDesignUiTest.xml
### ./customer/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedstore.ui.CustomerNavigationTest.xml
### ./admin/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedadmin.data.AdminSessionRecoveryTest.xml
### ./admin/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedadmin.notifications.OrderNotificationCursorTest.xml
### ./admin/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedadmin.ui.AdminDesignUiTest.xml
### ./admin/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedadmin.ui.DashboardStatsTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.CatalogApiTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.ProductDiscountTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.ArabicSearchTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.ColorContrastTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.CatalogRepositoryTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.ProductWriteVerificationTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.SessionExpiryTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.ProductPriceParsingTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.CsvExporterTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.IsolatedBackendTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.DatabaseMigrationTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.DiagnosticsTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.OrderRepositoryTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.RoomIntegrationTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.AdminAuthHeaderTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.PriceInputTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.CartPersistenceTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.DesignSystemUiTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.UtilitiesTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.CartStoreTest.xml
### ./core/build/test-results/testDebugUnitTest/TEST-com.zakeleo877.saeedcore.AdminOrdersDecodingTest.xml
```

## Gradle test and lint
```
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.8.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :admin:preBuild UP-TO-DATE
> Task :admin:preDebugBuild UP-TO-DATE
> Task :admin:generateDebugBuildConfig
> Task :admin:extractDebugSupportedLocales
> Task :admin:mapDebugSourceSetPaths
> Task :core:preBuild UP-TO-DATE
> Task :core:preDebugBuild UP-TO-DATE
> Task :core:extractDebugSupportedLocales
> Task :admin:generateDebugResources
> Task :core:processDebugNavigationResources
> Task :admin:generateDebugLocaleConfig
> Task :admin:processDebugNavigationResources
> Task :admin:packageDebugResources
> Task :core:generateDebugResources
> Task :core:packageDebugResources
> Task :admin:parseDebugLocalResources
> Task :core:parseDebugLocalResources
> Task :core:javaPreCompileDebug
> Task :core:preDebugUnitTestBuild UP-TO-DATE
> Task :admin:generateDebugRFile
> Task :core:writeDebugAarMetadata
> Task :core:generateDebugRFile
> Task :core:mapDebugSourceSetPaths
> Task :core:checkDebugUnitTestAarMetadata
> Task :core:mapDebugUnitTestSourceSetPaths
> Task :core:generateDebugUnitTestResources
> Task :core:compileDebugLibraryResources
> Task :core:extractDeepLinksDebug
> Task :core:javaPreCompileDebugUnitTest
> Task :admin:javaPreCompileDebug
> Task :core:processDebugManifest
> Task :admin:checkDebugAarMetadata
> Task :core:mergeDebugUnitTestResources
> Task :core:mergeDebugUnitTestManifest
> Task :core:processDebugUnitTestManifest
> Task :admin:compileDebugNavigationResources
> Task :core:processDebugUnitTestResources
> Task :admin:createDebugCompatibleScreenManifests
> Task :admin:extractDeepLinksDebug
> Task :admin:processDebugMainManifest
> Task :admin:processDebugManifest
> Task :admin:mergeDebugResources
> Task :admin:preDebugUnitTestBuild UP-TO-DATE
> Task :admin:generateDebugAssets UP-TO-DATE
> Task :core:generateDebugAssets UP-TO-DATE
> Task :core:mergeDebugAssets
> Task :admin:mergeDebugAssets
> Task :admin:processDebugManifestForPackage

> Task :admin:mergeDebugUnitTestManifest
/home/runner/work/ARENA.PROJECT/ARENA.PROJECT/admin/src/main/AndroidManifest.xml:29:13-31:39 Warning:
	meta-data#androidx.work.WorkManagerInitializer was tagged at AndroidManifest.xml:29 to remove other declarations but no other declaration present

> Task :admin:processDebugUnitTestManifest
> Task :core:generateDebugUnitTestAssets UP-TO-DATE
> Task :admin:javaPreCompileDebugUnitTest
> Task :core:mergeDebugUnitTestAssets
> Task :core:packageDebugUnitTestForUnitTest

--- failure lines ---
(none)

--- tail ---
w: file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/customer/src/test/java/com/zakeleo877/saeedstore/ui/CustomerDesignUiTest.kt:40:33 'fun createComposeRule(effectContext: CoroutineContext = ...): ComposeContentTestRule' is deprecated. Use `androidx.compose.ui.test.junit4.v2.createComposeRule` instead. The v2 APIs use StandardTestDispatcher instead of UnconfinedTestDispatcher, which aligns with standard coroutine behavior by queuing tasks rather than executing them immediately. Tests relying on immediate execution may require explicit synchronization. Please refer to the migration guide for more details.

> Task :admin:lintAnalyzeDebugAndroidTest
> Task :core:generateDebugAndroidTestLintModel
> Task :core:generateDebugUnitTestLintModel
> Task :customer:compressDebugUnitTestJavaRes
> Task :customer:extractDebugUnitTestJavaRes
> Task :customer:generateDebugUnitTestConfig
> Task :customer:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :customer:hiltCollectClassesDebugUnitTest
> Task :customer:hiltAggregateDepsDebugUnitTest
> Task :customer:hiltJavaCompileDebugUnitTest NO-SOURCE
> Task :customer:transformDebugUnitTestClassesWithAsm
> Task :core:lintAnalyzeDebugAndroidTest
> Task :admin:lintAnalyzeDebugUnitTest
> Task :customer:testDebugUnitTest
> Task :customer:test
> Task :core:generateDebugLintReportModel
> Task :customer:generateDebugAndroidTestLintModel
> Task :customer:generateDebugLintReportModel
> Task :customer:generateDebugUnitTestLintModel
> Task :customer:lintAnalyzeDebugAndroidTest
> Task :customer:lintAnalyzeDebugUnitTest
> Task :admin:lintAnalyzeDebug
> Task :core:lintAnalyzeDebugUnitTest

> Task :core:lintReportDebug
Wrote HTML report to file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/core/build/reports/lint-results-debug.html
Wrote SARIF report to file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/core/build/reports/lint-results-debug.sarif

> Task :core:lintDebug
> Task :core:lint

> Task :admin:lintReportDebug
Wrote HTML report to file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/admin/build/reports/lint-results-debug.html
Wrote SARIF report to file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/admin/build/reports/lint-results-debug.sarif

> Task :admin:lintDebug
> Task :admin:lint
> Task :customer:lintAnalyzeDebug

> Task :customer:lintReportDebug
Wrote HTML report to file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/customer/build/reports/lint-results-debug.html
Wrote SARIF report to file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/customer/build/reports/lint-results-debug.sarif

> Task :customer:lintDebug
> Task :customer:lint
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_9-1794263167980.json

[Incubating] Problems report is available at: file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 2m 57s
161 actionable tasks: 161 executed
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.2/userguide/configuration_cache_enabling.html
```

## Gradle build
```
To honour the JVM settings for this build a single-use Daemon process will be forked. For more on this, please refer to https://docs.gradle.org/9.8.2/userguide/gradle_daemon.html#sec:disabling_the_daemon in the Gradle documentation.
Daemon will be stopped at the end of the build 
> Task :core:preBuild UP-TO-DATE
> Task :core:preReleaseBuild UP-TO-DATE
> Task :core:mergeReleaseJniLibFolders
> Task :core:mergeReleaseNativeLibs NO-SOURCE
> Task :customer:preBuild UP-TO-DATE
> Task :customer:preReleaseBuild UP-TO-DATE
> Task :core:copyReleaseJniLibsProjectOnly
> Task :customer:mergeReleaseJniLibFolders
> Task :core:prepareReleaseArtProfile
> Task :customer:mergeReleaseNativeLibs
> Task :customer:mergeReleaseArtProfile
> Task :core:generateReleaseResources

> Task :customer:stripReleaseDebugSymbols
Unable to strip the following libraries, packaging them as they are: libandroidx.graphics.path.so, libdatastore_shared_counter.so. Run with --info option to learn more.

> Task :customer:extractReleaseNativeSymbolTables
> Task :core:packageReleaseResources
> Task :customer:mergeReleaseNativeDebugMetadata NO-SOURCE
> Task :core:processReleaseNavigationResources
> Task :core:javaPreCompileRelease
> Task :core:parseReleaseLocalResources
> Task :customer:expandReleaseL8ArtProfileWildcards
> Task :customer:generateReleaseBuildConfig
> Task :customer:checkReleaseDuplicateClasses
> Task :core:extractReleaseSupportedLocales
> Task :core:generateReleaseRFile
> Task :customer:extractReleaseSupportedLocales
> Task :customer:mapReleaseSourceSetPaths
> Task :customer:generateReleaseResources
> Task :customer:processReleaseNavigationResources
> Task :customer:javaPreCompileRelease
> Task :core:mapReleaseSourceSetPaths
> Task :core:compileReleaseLibraryResources
> Task :core:writeReleaseAarMetadata
> Task :customer:compileReleaseNavigationResources
> Task :customer:createReleaseCompatibleScreenManifests
> Task :core:extractDeepLinksRelease
> Task :customer:checkReleaseAarMetadata
> Task :customer:extractDeepLinksRelease
> Task :customer:extractProguardFiles UP-TO-DATE
> Task :customer:mergeReleaseStartupProfile
> Task :core:generateReleaseAssets UP-TO-DATE
> Task :core:mergeReleaseAssets
> Task :customer:generateReleaseLocaleConfig
> Task :core:processReleaseManifest
> Task :customer:packageReleaseResources
> Task :customer:parseReleaseLocalResources
> Task :customer:generateReleaseRFile
> Task :customer:mergeReleaseResources
> Task :customer:processReleaseMainManifest
> Task :customer:processReleaseManifest
> Task :customer:generateReleaseAssets UP-TO-DATE
> Task :customer:mergeReleaseAssets
> Task :customer:processReleaseManifestForPackage
> Task :customer:compressReleaseAssets
> Task :customer:extractReleaseVersionControlInfo
> Task :core:extractProguardFiles UP-TO-DATE

--- failure lines ---
(none)

--- tail ---
> Task :customer:compressReleaseJavaRes
> Task :customer:hiltAggregateDepsRelease
> Task :customer:mergeReleaseJavaResource
> Task :customer:hiltJavaCompileRelease
> Task :customer:generateReleaseLintVitalReportModel
> Task :customer:transformReleaseClassesWithAsm
> Task :core:lintVitalAnalyzeRelease
> Task :admin:compileReleaseJavaWithJavac
> Task :customer:mergeReleaseGeneratedProguardFiles
> Task :admin:hiltCollectClassesRelease
> Task :admin:hiltAggregateDepsRelease
> Task :customer:expandReleaseArtProfileWildcards
> Task :admin:hiltJavaCompileRelease
> Task :admin:transformReleaseClassesWithAsm
> Task :customer:produceReleaseComposeMapping
> Task :customer:reportReleaseComposeMappingErrors
> Task :admin:mergeReleaseGeneratedProguardFiles
> Task :admin:compressReleaseJavaRes
> Task :admin:mergeReleaseJavaResource
> Task :admin:expandReleaseArtProfileWildcards
> Task :admin:generateReleaseLintVitalReportModel
> Task :admin:produceReleaseComposeMapping
> Task :admin:reportReleaseComposeMappingErrors
> Task :customer:lintVitalAnalyzeRelease
> Task :customer:lintVitalReportRelease
> Task :customer:lintVitalRelease
> Task :admin:lintVitalAnalyzeRelease
> Task :admin:lintVitalReportRelease
> Task :admin:lintVitalRelease
> Task :customer:minifyReleaseWithR8
> Task :customer:convertShrunkResourcesToBinaryRelease
> Task :customer:optimizeReleaseResources
> Task :customer:l8DexDesugarLibRelease
> Task :customer:mergeReleaseComposeMapping
> Task :customer:compileReleaseArtProfile
> Task :customer:packageRelease
> Task :customer:createReleaseApkListingFileRedirect
> Task :customer:assembleRelease
> Task :admin:minifyReleaseWithR8
> Task :admin:convertShrunkResourcesToBinaryRelease
> Task :admin:optimizeReleaseResources
> Task :admin:l8DexDesugarLibRelease
> Task :admin:mergeReleaseComposeMapping
> Task :admin:compileReleaseArtProfile
> Task :admin:packageRelease
> Task :admin:createReleaseApkListingFileRedirect
> Task :admin:assembleRelease
gradle/actions: Writing build results to /home/runner/work/_temp/.gradle-actions/build-results/__run_10-1794263346353.json

[Incubating] Problems report is available at: file:///home/runner/work/ARENA.PROJECT/ARENA.PROJECT/build/reports/problems/problems-report.html

Deprecated Gradle features were used in this build, making it incompatible with Gradle 10.

You can use '--warning-mode all' to show the individual deprecation warnings and determine if they come from your own scripts or plugins.

For more on this, please refer to https://docs.gradle.org/9.8.2/userguide/command_line_interface.html#sec:command_line_warnings in the Gradle documentation.

BUILD SUCCESSFUL in 4m 29s
159 actionable tasks: 155 executed, 4 up-to-date
Consider enabling configuration cache to speed up this build: https://docs.gradle.org/9.8.2/userguide/configuration_cache_enabling.html
```
