package com.diet.config;

import org.kie.api.KieServices;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DroolsConfig {

    @Bean
    public KieContainerHolder kieContainerHolder() {
        KieContainerHolder holder = new KieContainerHolder();
        KieServices kieServices = KieServices.Factory.get();
        holder.set(kieServices.getKieClasspathContainer());
        return holder;
    }
}
