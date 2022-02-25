// IPeachInterface.aidl
package com.puy.peach.configs;

import com.puy.peach.configs.PeachConfig;

interface IPeachInterface {
    void wakeup(in PeachConfig config);
    void connectionTimes(in int time);
}