package com.luckyzyx.luckytool;

interface IPowerServiceController {
    void reboot(boolean confirm, String reason, boolean wait);
    void rebootSafeMode(boolean confirm, boolean wait);
}