package com.diet.task;

import com.diet.service.IRecipeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PopularRecipeCacheTask {

    private static final int TOP_N = 30;
    private final IRecipeService recipeService;

    /**
     * Refresh popular recipe caches every day at 02:00.
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshPopularRecipeCache() {
        recipeService.refreshAllPopularRecipeCaches(TOP_N);
        log.info("Popular recipe cache refreshed");
    }
}
