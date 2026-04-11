package com.diet.service;

import com.diet.config.KieContainerHolder;
import com.diet.entity.DietTag;
import com.diet.entity.HealthProfile;
import org.kie.api.runtime.ClassObjectFilter;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Executes Drools diet-label rules and collects generated DietTags.
 */
@Service
public class DietTagRuleService {

    private final KieContainerHolder kieContainerHolder;

    public DietTagRuleService(KieContainerHolder kieContainerHolder) {
        this.kieContainerHolder = kieContainerHolder;
    }

    /**
     * Run diet label rules for the given health profile and return generated tags.
     * Does not persist - caller should save to database.
     */
    public List<DietTag> generateDietTags(HealthProfile profile) {
        List<DietTag> result = new ArrayList<>();
        KieSession session = null;
        try {
            session = kieContainerHolder.get().newKieSession();
            FactHandle handle = session.insert(profile);
            session.fireAllRules();
            session.delete(handle);

            // Collect inserted DietTag facts from working memory
            Collection<?> objects = session.getObjects(new ClassObjectFilter(DietTag.class));
            for (Object obj : objects) {
                result.add((DietTag) obj);
            }
        } finally {
            if (session != null) {
                session.dispose();
            }
        }
        return result;
    }

}
