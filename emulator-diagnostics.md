# Emulator diagnostics

Run: 42711458971

## Emulator log
```
INFO         | Android emulator version 37.2.13.0 (build_id 16740729) (CL:N/A)
INFO         | Graphics backend: gfxstream
INFO         | Found systemPath /usr/local/lib/android/sdk/system-images/android-34/default/x86_64/
INFO         | Saving last run QEMU version for the AVD to 2
INFO         | Increasing RAM size to 2560MB
INFO         | Guest GLES Driver: Auto (ext controls)
INFO         | emuglConfig_init: vulkan_mode_selected:swiftshader gles_mode_selected:swiftshader
INFO         | Checking system compatibility:
INFO         |   Checking: hasSufficientDiskSpace
INFO         |      Ok: Disk space requirements to run avd: `test` are met
INFO         |   Checking: hasSufficientHwGpu
INFO         |      Ok: Hardware GPU compatibility checks are not required
INFO         |   Checking: hasSufficientSystem
INFO         |      Ok: System requirements to run avd: `test` are met
WARNING      | Feature QuickbootFileBacked is disabled due to stability issues, if you really want that, the environment variable ANDROID_EMU_FORCE_QUICKBOOT_FILE_BACKED need to set to 1
INFO         | Storing crashdata in: /tmp/android-runner/emu-crash-37.2.13.db, detection is enabled for process: 2795
INFO         | Initializing gfxstream backend
INFO         | startOpenglesRendererImpl: gpu info
INFO         | 
INFO         | initIcdPaths: ICD set to 'swiftshader', using Swiftshader ICD
INFO         | Setting ICD filenames for the loader = /usr/local/lib/android/sdk/emulator/lib64/vulkan/vk_swiftshader_icd.json
INFO         | SharedLibrary::open for [/usr/local/lib/android/sdk/emulator/qemu/linux-x86_64/lib64/vulkan/libvulkan.so]
INFO         | SharedLibrary::open for [/usr/local/lib/android/sdk/emulator/qemu/linux-x86_64/lib64/vulkan/libvulkan.so.1]
INFO         | SharedLibrary::open for [/usr/local/lib/android/sdk/emulator/lib64/vulkan/libvulkan.so]
INFO         | Added library: /usr/local/lib/android/sdk/emulator/lib64/vulkan/libvulkan.so
INFO         | Selecting Vulkan device: SwiftShader Device (Subzero), Version: 1.3.0
INFO         | Using Vulkan externalMemoryMode: OpaqueFd for VkEmulation
INFO         | Format VK_FORMAT_G10X6_B10X6R10X6_2PLANE_422_UNORM_3PACK16 does not support YCbCr sampler conversion
INFO         | SharedLibrary::open for [libX11]
WARNING      | Could not open libX11.so, try libX11.so.6
INFO         | SharedLibrary::open for [libX11.so.6]
INFO         | SharedLibrary::open for [libX11-xcb]
WARNING      | Could not open libX11-xcb.so, try libX11-xcb.so.1
INFO         | SharedLibrary::open for [libX11-xcb.so.1]
INFO         | Initializing VkEmulation features:
INFO         |     glInteropSupported: false
INFO         |     useDeferredCommands: true
INFO         |     createResourceWithRequirements: true
INFO         |     useVulkanComposition: false
INFO         |     useVulkanNativeSwapchain: false
INFO         |     enable guestRenderDoc: false
INFO         |     ASTC LDR emulation mode: Gpu
INFO         |     enable ETC2 emulation: true
INFO         |     enable Ycbcr emulation: false
INFO         |     guestVulkanOnly: false
INFO         |     useDedicatedAllocations: false
INFO         |     guestVulkanMaxApiVersion: 1.3.0
INFO         |     enableProtectedMemoryEmulation: false
INFO         | Graphics Adapter Vendor Google (Google Inc.)
INFO         | Graphics Adapter Android Emulator OpenGL ES Translator (Google SwiftShader)
INFO         | Graphics API Version OpenGL ES 3.0 (OpenGL ES 3.0 SwiftShader 4.0.0.1)
INFO         | Graphics API Extensions GL_OES_EGL_sync GL_OES_EGL_image GL_OES_EGL_image_external GL_OES_depth24 GL_OES_depth32 GL_OES_element_index_uint GL_OES_texture_float GL_OES_texture_float_linear GL_OES_compressed_paletted_texture GL_OES_compressed_ETC1_RGB8_texture GL_OES_depth_texture GL_OES_texture_half_float GL_OES_texture_half_float_linear GL_OES_packed_depth_stencil GL_OES_vertex_half_float GL_OES_standard_derivatives GL_OES_texture_npot GL_OES_rgb8_rgba8 GL_EXT_color_buffer_float GL_EXT_color_buffer_half_float GL_EXT_texture_format_BGRA8888 GL_APPLE_texture_format_BGRA8888 
INFO         | Graphics Device Extensions N/A
INFO         | Disabling sparse binding feature support
INFO         | Sending adb public key [QAAAAH+IQh2ByCoea9nqH5brMfOCWErD7rh1WAW40LpRExOJxDXQvTriBOSphHE5uFZqL4UCQoVTmqoMwHhjAhUldzLLTPo/dUDkDNi3AKqQv4Q6yyHiBsdyLPC3Ai0UykP9WQ3XdqeU1dpf4M1DrsNfSmFfMonzj/6FHTE9gICRQlWhfRFA/gUJokcTk5jOUdWg8+3+zJn/1p4fASR4PPviJMOjnm+zuIiorXCHwd13FCGDESmt4P2lIyFyRvUf3CLGaDTDFG9kjrHb8TkS3yL8x1z0kAtSqIJi41yxqgmQvoVBadGgxHQO6yW7TUEgOcTCprwe11cUYKkMlGi9qubkhqZi25mxRa2ST0XtGG2OarFporN4Ori+H2gIOPWp30lW/D94igS0t78HDVu7jelGK4LXkK0HIyaOvRb57DFVY9JZK7VXrAmcHxS1SCStICjUEj2DvBBFuAsaV81MqT0iSJUj8IwgNsZxvm14gchMTLI/XGSjSmAsCD3TKhESz5gQbYkoN1QuSFnffzzvwlC3eNB8I82Wsoobv7ia/GnT0pi6T/rLh+cNJf3z3msp/tN4VYQ0Hm+MGhbdj3H+W/i0LyzWAFOG1RhFqdgJWZxIE0j85JSGxde8Gejj0jmen9gsKAPRGCJFkPIbaFTvAq6N7FOaBIIuUH7kKAf7H+wFBGsjfK9QOQEAAQA= runner@unknown]
INFO         | Userspace boot properties:
INFO         |   androidboot.boot_devices=pci0000:00/0000:00:03.0 pci0000:00/0000:00:06.0
INFO         |   androidboot.dalvik.vm.heapsize=576m
INFO         |   androidboot.debug.hwui.renderer=skiagl
INFO         |   androidboot.debug.sf.nobootanimation=1
INFO         |   androidboot.hardware=ranchu
INFO         |   androidboot.hardware.gltransport=pipe
INFO         |   androidboot.hardware.vulkan=ranchu
INFO         |   androidboot.logcat=*:V
INFO         |   androidboot.opengles.version=196608
INFO         |   androidboot.qemu=1
INFO         |   androidboot.qemu.adb.pubkey=QAAAAH+IQh2ByCoea9nqH5brMfOCWErD7rh1WAW40LpRExOJxDXQvTriBOSphHE5uFZqL4UCQoVTmqoMwHhjAhUldzLLTPo/dUDkDNi3AKqQv4Q6yyHiBsdyLPC3Ai0UykP9WQ3XdqeU1dpf4M1DrsNfSmFfMonzj/6FHTE9gICRQlWhfRFA/gUJokcTk5jOUdWg8+3+zJn/1p4fASR4PPviJMOjnm+zuIiorXCHwd13FCGDESmt4P2lIyFyRvUf3CLGaDTDFG9kjrHb8TkS3yL8x1z0kAtSqIJi41yxqgmQvoVBadGgxHQO6yW7TUEgOcTCprwe11cUYKkMlGi9qubkhqZi25mxRa2ST0XtGG2OarFporN4Ori+H2gIOPWp30lW/D94igS0t78HDVu7jelGK4LXkK0HIyaOvRb57DFVY9JZK7VXrAmcHxS1SCStICjUEj2DvBBFuAsaV81MqT0iSJUj8IwgNsZxvm14gchMTLI/XGSjSmAsCD3TKhESz5gQbYkoN1QuSFnffzzvwlC3eNB8I82Wsoobv7ia/GnT0pi6T/rLh+cNJf3z3msp/tN4VYQ0Hm+MGhbdj3H+W/i0LyzWAFOG1RhFqdgJWZxIE0j85JSGxde8Gejj0jmen9gsKAPRGCJFkPIbaFTvAq6N7FOaBIIuUH7kKAf7H+wFBGsjfK9QOQEAAQA= runner@unknown
INFO         |   androidboot.qemu.avd_name=test
INFO         |   androidboot.qemu.camera_hq_edge_processing=0
INFO         |   androidboot.qemu.camera_protocol_ver=1
INFO         |   androidboot.qemu.cpuvulkan.version=4206592
INFO         |   androidboot.qemu.gltransport.drawFlushInterval=800
INFO         |   androidboot.qemu.gltransport.name=pipe
INFO         |   androidboot.qemu.hwcodec.avcdec=2
INFO         |   androidboot.qemu.hwcodec.hevcdec=2
INFO         |   androidboot.qemu.hwcodec.vpxdec=2
INFO         |   androidboot.qemu.settings.system.screen_off_timeout=2147483647
INFO         |   androidboot.qemu.skin=320x640
INFO         |   androidboot.qemu.virtiowifi=1
INFO         |   androidboot.qemu.vsync=60
INFO         |   androidboot.serialno=EMULATOR37X2X13X0
INFO         |   androidboot.vbmeta.digest=8bee4f6105b6acd7de887b1ee6629ab07066df02ee9bbca07663adffdad97fc1
INFO         |   androidboot.vbmeta.hash_alg=sha256
INFO         |   androidboot.vbmeta.size=6656
INFO         |   androidboot.veritymode=enforcing
WARNING: cannnot unmap ptr 0x7a5824601000 as it is in the protected range from 0x7a5784600000 to 0x7a5824800000
INFO         | Monitoring duration of emulator setup.
WARNING      | The emulator now requires a signed jwt token for gRPC access! Use the -grpc flag if you really want an open unprotected grpc port
INFO         | Using security allow list from: /usr/local/lib/android/sdk/emulator/lib/emulator_access.json
INFO         | The active JSON Web Key Sets can be found here: /run/user/1001/avd/running/2795/jwks/5aeb1732-030d-4c16-b727-c26d698420d8/active.jwk
INFO         | Scanning /run/user/1001/avd/running/2795/jwks/5aeb1732-030d-4c16-b727-c26d698420d8 for jwk keys.
INFO         | Started GRPC server at 127.0.0.1:8554, security: Local, auth: +token
INFO         | Advertising in: /run/user/1001/avd/running/pid_2795.ini
INFO         | Activated packet streamer for bluetooth emulation
INFO         | Successfully initialized netsim WiFi
INFO         | Setting display: 0 configuration to: 320x640, dpi: 160x160 
INFO         | setDisplayActiveConfig: id:0, 320x640
INFO         | emulatorSetupEnvironment: Environment scene is not required
USER_INFO    | Emulator is performing a full startup. This may take upto two minutes, or more.
WARNING      | Failed to process .ini file /home/runner/.android/.android/emu-update-last-check.ini for reading.
WARNING      | Failed to process .ini file /home/runner/.android/.android/emu-update-last-check.ini for reading.
INFO         | Boot completed in 24898 ms
INFO         | Increasing screen off timeout, logcat buffer size to 2M.
ERROR        | adb protocol fault (couldn't read status length)
```

## SDK environment
```
ANDROID_HOME=/usr/local/lib/android/sdk
ANDROID_SDK_ROOT=/usr/local/lib/android/sdk
ANDROID_AVD_HOME=/home/runner/.android/avd
--- the adb binaries ---
/usr/local/lib/android/sdk/platform-tools/adb
--- the devices ---
List of devices attached
emulator-5554	device

--- the running processes ---
runner      2795 32.8 20.2 4728304 3314644 ?     Sl   22:24   1:02 /usr/local/lib/android/sdk/emulator/qemu/linux-x86_64/qemu-system-x86_64-headless -avd test -no-window -no-audio -no-boot-anim -no-snapshot -gpu swiftshader_indirect -accel auto -wipe-data -memory 1536 -cores 2 -no-metrics -netdelay none -netspeed full
runner      2805  0.0  0.0 173100  4408 ?        Sl   22:24   0:00 /usr/local/lib/android/sdk/emulator/crashpad_handler --no-rate-limit --database=/tmp/android-runner/emu-crash-37.2.13.db --url=https://clients2.google.com/cr/report --annotation=prod=AndroidEmulator --annotation=ver=37.2.13-16740729 --initial-client-fd=4 --shared-client-connection
runner      2896  0.6  0.1 1063232 22404 ?       Sl   22:24   0:01 /usr/local/lib/android/sdk/emulator/netsimd --host-dns=127.0.0.53
runner      3140  0.0  0.0   7280  2360 ?        S    22:27   0:00 grep -i qemu\|emulator
```

## Customer run
```
The adb binaries that are tried: /usr/local/lib/android/sdk/platform-tools/adb /usr/local/lib/android/sdk/platform-tools/adb /usr/local/lib/android/sdk/platform-tools/adb /usr/local/lib/android/sdk/platform-tools/adb
Using adb at /usr/local/lib/android/sdk/platform-tools/adb for the device emulator-5554
PASS: the emulator is visible to adb
PASS: the emulator finished booting
PASS: the apk was installed on the emulator
Starting: Intent { cmp=com.zakeleo877.saeedstore/.MainActivity }
Status: ok
LaunchState: COLD
Activity: com.zakeleo877.saeedstore/.MainActivity
TotalTime: 1196
WaitTime: 1201
Complete
PASS: the application process is running with the identifier 2266
PASS: no fatal exception was logged
PASS: the scheduled Hilt worker was created and completed successfully (com.zakeleo877.saeedstore.orders.PendingOrdersWorker)
PASS: a screenshot was captured at artifacts/screenshots/com.zakeleo877.saeedstore.png
PASS: the screenshot shows a rendered screen (320x640, 256 distinct byte values)
PASS: a second screenshot after scrolling was captured
PASS: the user interface rendered on the emulator
PASS: wholesale home feature opens the products view
PASS: product tab renders the isolated sample catalogue
PASS: adding a sample item reaches the cart
PASS: checkout form opens; no order was submitted
PASS: pending tab opens the local send queue
PASS: home navigation works
PASS: no crash during isolated customer interactions
```

## Admin run
```
The adb binaries that are tried: /usr/local/lib/android/sdk/platform-tools/adb /usr/local/lib/android/sdk/platform-tools/adb /usr/local/lib/android/sdk/platform-tools/adb /usr/local/lib/android/sdk/platform-tools/adb
Using adb at /usr/local/lib/android/sdk/platform-tools/adb for the device emulator-5554
PASS: the emulator is visible to adb
PASS: the emulator finished booting
PASS: the apk was installed on the emulator
Starting: Intent { cmp=com.zakeleo877.saeedadmin/.MainActivity }
Status: ok
LaunchState: COLD
Activity: com.zakeleo877.saeedadmin/.MainActivity
TotalTime: 614
WaitTime: 620
Complete
PASS: the application process is running with the identifier 2861
PASS: no fatal exception was logged
PASS: the scheduled Hilt worker was created and completed successfully (com.zakeleo877.saeedadmin.notifications.OrdersRefreshWorker)
PASS: a screenshot was captured at artifacts/screenshots/com.zakeleo877.saeedadmin.png
PASS: the screenshot shows a rendered screen (320x640, 256 distinct byte values)
PASS: a second screenshot after scrolling was captured
PASS: the user interface rendered on the emulator
```
