package com.diet.config;

import org.kie.api.runtime.KieContainer;

/**
 * Holds the current KieContainer so it can be replaced when rules are reloaded
 * without changing injected beans. Services use get() to obtain the current container.
 */
public class KieContainerHolder {

    private volatile KieContainer kieContainer;

    public KieContainer get() {
        return kieContainer;
    }

    public void set(KieContainer kieContainer) {
        this.kieContainer = kieContainer;
    }
}
