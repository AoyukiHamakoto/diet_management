package com.diet.service;

import com.diet.config.KieContainerHolder;
import com.diet.dto.RuleBackupItemDTO;
import com.diet.dto.RuleContentDTO;
import com.diet.dto.RuleFileDTO;
import com.diet.dto.RuleValidateDTO;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.runtime.KieContainer;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages Drools rule files in a writable directory: list, get, save with backup,
 * validate, list backups, restore. On save success optionally reloads KieContainer.
 */
@Service
public class RuleFileService implements InitializingBean {

    private static final String KMODULE_PATH = "src/main/resources/META-INF/kmodule.xml";
    private static final String RULES_KFS_PREFIX = "src/main/resources/rules/";
    private static final ReleaseId RELEASE_ID = KieServices.Factory.get()
            .newReleaseId("com.diet", "rules", "1.0.0");

    /** Logical name -> relative path under rules dir (for file I/O and KFS path) */
    private static final Map<String, String> NAME_TO_REL_PATH = Map.of(
            "diet-label-rules.drl", "diet-label-rules.drl",
            "meal-plan-rules.drl", "mealplan/meal-plan-rules.drl"
    );

    @Value("${rules.file-path:${user.dir}/rules}")
    private String rulesFilePath;

    @Value("${rules.auto-reload:true}")
    private boolean autoReload;

    private final KieContainerHolder kieContainerHolder;

    public RuleFileService(KieContainerHolder kieContainerHolder) {
        this.kieContainerHolder = kieContainerHolder;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        Path base = Path.of(rulesFilePath);
        Files.createDirectories(base);
        Path mealplan = base.resolve("mealplan");
        Files.createDirectories(mealplan);
        for (Map.Entry<String, String> e : NAME_TO_REL_PATH.entrySet()) {
            Path target = base.resolve(e.getValue());
            if (!Files.isRegularFile(target)) {
                copyFromClasspath(e.getKey(), target);
            }
        }
    }

    /** Copy one rule file from classpath (rules/xxx.drl) to target path. */
    private void copyFromClasspath(String ruleFileName, Path target) throws IOException {
        String classpathPath = "rules/" + ruleFileName;
        try (InputStream in = new ClassPathResource(classpathPath).getInputStream()) {
            Files.createDirectories(target.getParent());
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public List<RuleFileDTO> list() {
        return NAME_TO_REL_PATH.entrySet().stream()
                .map(e -> new RuleFileDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public RuleContentDTO getContent(String name) throws IOException {
        String rel = NAME_TO_REL_PATH.get(name);
        if (rel == null) {
            throw new IllegalArgumentException("Unknown rule name: " + name);
        }
        Path file = Path.of(rulesFilePath).resolve(rel);
        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Rule file not found: " + name);
        }
        String content = Files.readString(file, StandardCharsets.UTF_8);
        return new RuleContentDTO(name, content);
    }

    /**
     * Save content for the given rule file: backup, write, build. On build failure restores backup and throws.
     * On success and auto-reload, rebuilds and updates KieContainer.
     */
    public void save(String name, String content) throws IOException {
        String rel = NAME_TO_REL_PATH.get(name);
        if (rel == null) {
            throw new IllegalArgumentException("Unknown rule name: " + name);
        }
        Path file = Path.of(rulesFilePath).resolve(rel);
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").withZone(ZoneId.systemDefault()).format(Instant.now());
        Path backup = file.getParent().resolve(file.getFileName() + ".bak." + ts);
        if (Files.isRegularFile(file)) {
            Files.copy(file, backup, StandardCopyOption.REPLACE_EXISTING);
        }
        try {
            Files.writeString(file, content != null ? content : "", StandardCharsets.UTF_8);
            buildAndReload();
        } catch (Exception e) {
            if (Files.isRegularFile(backup)) {
                Files.copy(backup, file, StandardCopyOption.REPLACE_EXISTING);
            }
            throw new IllegalStateException("规则编译失败，已恢复备份: " + e.getMessage(), e);
        }
    }

    /** Validate content for the given rule (other rules read from disk). Returns valid and error messages. */
    public RuleValidateDTO validate(String name, String content) {
        if (!NAME_TO_REL_PATH.containsKey(name)) {
            return new RuleValidateDTO(false, List.of("未知规则文件: " + name));
        }
        Map<String, String> overrides = Map.of(name, content != null ? content : "");
        BuildResult result = buildFromDirWithOverrides(overrides);
        return new RuleValidateDTO(result.errors.isEmpty(), result.errors);
    }

    public List<RuleBackupItemDTO> listBackups(String name) throws IOException {
        String rel = NAME_TO_REL_PATH.get(name);
        if (rel == null) {
            return List.of();
        }
        Path file = Path.of(rulesFilePath).resolve(rel);
        Path dir = file.getParent();
        String prefix = file.getFileName().toString() + ".bak.";
        List<RuleBackupItemDTO> list = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(p -> p.getFileName().toString().startsWith(prefix))
                    .map(Path::getFileName)
                    .map(Path::toString)
                    .forEach(fn -> {
                        String ts = fn.substring(prefix.length());
                        list.add(new RuleBackupItemDTO(fn, ts));
                    });
        }
        list.sort(Comparator.comparing(RuleBackupItemDTO::getTimestamp).reversed());
        return list;
    }

    /** Restore from backup file (e.g. diet-label-rules.drl.bak.20250101120000), then build and reload. */
    public void restore(String name, String backupFilename) throws IOException {
        String rel = NAME_TO_REL_PATH.get(name);
        if (rel == null) {
            throw new IllegalArgumentException("Unknown rule name: " + name);
        }
        Path file = Path.of(rulesFilePath).resolve(rel);
        Path backup = file.getParent().resolve(backupFilename);
        if (!Files.isRegularFile(backup) || !backupFilename.startsWith(file.getFileName() + ".bak.")) {
            throw new IllegalArgumentException("Invalid backup: " + backupFilename);
        }
        Files.copy(backup, file, StandardCopyOption.REPLACE_EXISTING);
        buildAndReload();
    }

    private void buildAndReload() throws IOException {
        BuildResult result = buildFromDirWithOverrides(Collections.emptyMap());
        if (!result.errors.isEmpty()) {
            throw new IllegalStateException("编译错误: " + String.join("; ", result.errors));
        }
        if (autoReload && result.container != null) {
            kieContainerHolder.set(result.container);
        }
    }

    private BuildResult buildFromDirWithOverrides(Map<String, String> contentOverrides) {
        KieServices ks = KieServices.Factory.get();
        KieFileSystem kfs = ks.newKieFileSystem();
        try {
            String kmodule = StreamUtils.copyToString(
                    new ClassPathResource("META-INF/kmodule.xml").getInputStream(), StandardCharsets.UTF_8);
            kfs.write(KMODULE_PATH, kmodule);
        } catch (IOException e) {
            return new BuildResult(null, List.of("无法读取 kmodule.xml: " + e.getMessage()));
        }
        kfs.generateAndWritePomXML(RELEASE_ID);

        Path base = Path.of(rulesFilePath);
        for (Map.Entry<String, String> e : NAME_TO_REL_PATH.entrySet()) {
            String name = e.getKey();
            String rel = e.getValue();
            String kfsPath = RULES_KFS_PREFIX + rel;
            String content;
            if (contentOverrides.containsKey(name)) {
                content = contentOverrides.get(name);
            } else {
                try {
                    content = Files.readString(base.resolve(rel), StandardCharsets.UTF_8);
                } catch (IOException ex) {
                    return new BuildResult(null, List.of("读取规则文件失败 " + rel + ": " + ex.getMessage()));
                }
            }
            kfs.write(kfsPath, content);
        }

        KieBuilder kb = ks.newKieBuilder(kfs).buildAll();
        if (kb.getResults().hasMessages(Message.Level.ERROR)) {
            List<String> errors = kb.getResults().getMessages(Message.Level.ERROR).stream()
                    .map(m -> m.getPath() + ": " + m.getText())
                    .collect(Collectors.toList());
            return new BuildResult(null, errors);
        }
        KieContainer container = ks.newKieContainer(RELEASE_ID);
        return new BuildResult(container, List.of());
    }

    private static class BuildResult {
        final KieContainer container;
        final List<String> errors;

        BuildResult(KieContainer container, List<String> errors) {
            this.container = container;
            this.errors = errors;
        }
    }
}
