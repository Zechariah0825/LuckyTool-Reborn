package com.luckyzyx.luckytool;

interface IPackageServiceController {
    void clearApplicationProfileData(String packageName);
    boolean performDexOptMode(String packageName);
}