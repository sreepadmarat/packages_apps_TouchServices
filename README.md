# TouchServices - Touch Sampling Rate Optimization for Android

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com)
[![API](https://img.shields.io/badge/API-33%2B-brightgreen.svg)](https://android-arsenal.com/api?level=33)

TouchServices is a system application designed to manage touch sampling rate settings and optimizations for supported devices. It allows users to control low-level kernel touch node parameters dynamically on a per-app basis to maximize responsiveness during gaming, while saving battery when high sampling rates are unnecessary.

## Features

- **Global Touch Sampling Control** - Instantly toggle high touch sampling rates globally.
- **Per-App Configuration** - Automatically enable high sampling rates only for selected applications (like games).
- **Background Service Manager** - Intelligent daemon that tracks the foreground application state with minimal overhead.
- **Quick Settings Tile** - Convenient toggle to apply or monitor touch sampling states from the status bar.

## Screenshots

<p align="center">
  <img src="readme_resources/screenshot_1.jpg" width="250" />
  <img src="readme_resources/screenshot_2.jpg" width="250" />
  <img src="readme_resources/screenshot_3.jpg" width="250" />
</p>

## Requirements

- Android 13 (API 33) or higher
- System-level permissions (privileged app)
- LineageOS or AOSP-based ROM
- Kernel with exposed touch sampling sysfs node

## Supported Devices

Currently, this package is explicitly configured and tested for the following devices:
* **Poco F6** (`peridot`)
* **Poco F5** (`marble`)

## Building & Installation


### Integration into Device Tree

To integrate this package into your custom ROM or Android source tree:

1. **Clone the repository** into your source tree under `packages/apps/`:
   ```bash
   git clone https://github.com/sreepadmarat/packages_apps_TouchServices.git -b lineage-23.2 packages/apps/TouchServices
   ```

2. **Include the product makefile** in your device tree's `device.mk` or `common.mk` file:
   ```makefile
   # TouchServices
   $(call inherit-product-if-exists, packages/apps/TouchServices/touchservice.mk)
   ```

3. **Build**:
   ```bash
   # Clean build
   m clean
   m Touchservice
   
   # Or build the entire ROM
   brunch <device>
   ```

## Porting to Other Devices

If you want to use TouchServices on a device other than those explicitly supported, you must properly configure the following elements to match your hardware's specific touch driver implementation:

1. **Touch Node Paths**: The application currently writes to `/sys/devices/virtual/touch/touch_dev/bump_sample_rate`. If your device uses a different sysfs path for touch sampling, update this within the application code (`src/com/android/touchservices/TouchSamplingUtils.kt`).
2. **SELinux Policies (`sepolicy`)**: You must thoroughly update `file_contexts` and `genfs_contexts` under `sepolicy/vendor` to properly label your device's specific sysfs nodes with `u:object_r:vendor_sysfs_touch:s0`. The app *will not open* or write successfully if it is denied by SELinux. Ensure `touchservice_app.te` maintains access.
3. **Init Script (`init.touchservice.rc`)**: Adjust the device-specific `init` script permissions to run the necessary `chown system system` and `chmod 0660` commands on your exact touch node path on boot.

## Troubleshooting

### App Not Opening or Force Closing
- Check if SELinux is actively denying the UI rendering or initialization: `adb logcat | grep avc`
- Ensure `touchservice_app` domain has proper `find` permissions for baseline Android services (like `surfaceflinger_service`) which might have been missed in device-specific or outdated sepolicy definitions.

### Settings not Applying
- Ensure your kernel actually supports the target sysfs path.
- Check node permissions: `adb shell ls -la /sys/devices/virtual/touch/touch_dev/bump_sample_rate`. It must be owned by `system` and have `0660` permissions.
- Validate values: Try manually echoing to the path via root shell:
  ```bash
  su
  echo 1 > /sys/devices/virtual/touch/touch_dev/bump_sample_rate
  ```

## License

```
Copyright (C) 2026 kenway214

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Support

- **GitHub Issues**: [kenway214/packages_apps_TouchServices](https://github.com/kenway214/packages_apps_TouchServices)
- **Telegram**: [Pandemonium](https://t.me/pandemonium_haydn)

---

**Note**: This is a system application that requires privileged access. It must be built as part of your ROM and cannot be installed directly as a regular APK.
