package com.pavlent1yy.gradinator.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class GroupFileMap {

    private final Map<String, Set<String>> filePartToGroups;
    private final Map<String, String> groupToFilePart;
    private final Map<String, String> departmentToFile;

    public GroupFileMap(@Value("${api.storage-dir-path}") String storageDirPath,
                        @Value("${api.groups-config-path:./groups.cfg}") String groupsConfigPath) {
        this.filePartToGroups = loadGroups(Paths.get(groupsConfigPath));
        this.groupToFilePart = createGroupToFilePart();
        this.departmentToFile = findFiles(Paths.get(storageDirPath));
    }

    public String getPossibleFileNameByGroup(String group) {
        if (group == null || group.isBlank()) return null;

        String groupPrefix = normalizeGroup(group.split("-", 2)[0]);
        String filePart = groupToFilePart.get(groupPrefix);

        return filePart == null ? null : departmentToFile.get(filePart);
    }

    public List<String> getDepartmentsNames() {
        return new ArrayList<>(invertMap(departmentToFile).values());
    }

    public String getPossibleDepartmentByGroup(String group){
        Map<String, String> fileToDepartment = invertMap(departmentToFile);
        return  fileToDepartment.get(getPossibleFileNameByGroup(group));
    }

    public Set<String> getAllFiles() {
        return new LinkedHashSet<>(departmentToFile.values());
    }

    public Map<String, List<String>> getGroupsByFilePart(List<String> allGroups) {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String filePart : filePartToGroups.keySet()) {
            Set<String> configuredPrefixes = filePartToGroups.get(filePart);

            List<String> groups = allGroups.stream()
                    .filter(group -> {
                        String prefix = normalizeGroup(group.split("-", 2)[0]);
                        return configuredPrefixes.contains(prefix);
                    })
                    .sorted()
                    .toList();

            result.put(filePart, groups);
        }

        return result;
    }

    private Map<String, Set<String>> loadGroups(Path config) {
        if (!Files.exists(config))
            throw new RuntimeException("Файл конфигурации не найден: " + config.toAbsolutePath());

        Map<String, Set<String>> result = new LinkedHashMap<>();

        try {
            for (String line : Files.readAllLines(config)) {
                line = line.trim();

                if (line.isEmpty() || line.startsWith("#")) continue;

                String[] parts = line.split(":", 2);
                if (parts.length != 2)
                    throw new RuntimeException("Некорректная строка в groups.cfg: " + line);

                String filePart = normalizeFilePart(parts[0]);

                Set<String> groups = Stream.of(parts[1].split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(GroupFileMap::normalizeGroup)
                        .collect(Collectors.toCollection(LinkedHashSet::new));

                if (groups.isEmpty())
                    throw new RuntimeException("Для отделения '" + filePart + "' не указаны группы");

                if (result.put(filePart, groups) != null)
                    throw new RuntimeException("Отделение '" + filePart + "' указано несколько раз");
            }
        } catch (IOException e) {
            throw new RuntimeException("Ошибка чтения " + config.toAbsolutePath(), e);
        }

        if (result.isEmpty())
            throw new RuntimeException("Файл groups.cfg не содержит ни одного отделения");

        return Collections.unmodifiableMap(result);
    }

    private Map<String, String> createGroupToFilePart() {
        Map<String, String> result = new HashMap<>();

        filePartToGroups.forEach((filePart, groups) -> {
            for (String group : groups) {
                String previous = result.put(group, filePart);

                if (previous != null)
                    throw new RuntimeException("Группа '" + group + "' указана для '" + previous + "' и '" + filePart + "'");
            }
        });

        return Collections.unmodifiableMap(result);
    }

    private Map<String, String> findFiles(Path storageDir) {
        if (!Files.isDirectory(storageDir))
            throw new RuntimeException("Директория с файлами не найдена: " + storageDir.toAbsolutePath());

        Map<String, String> result = new LinkedHashMap<>();

        try (Stream<Path> files = Files.list(storageDir)) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .forEach(fileName -> {
                        String normalizedFileName = fileName.toLowerCase(Locale.ROOT);

                        for (String filePart : filePartToGroups.keySet()) {
                            if (!normalizedFileName.contains(filePart)) continue;

                            String previous = result.put(filePart, fileName);

                            if (previous != null)
                                throw new RuntimeException("Найдено несколько файлов для '" + filePart + "': "
                                        + previous + " и " + fileName);
                            break;
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при поиске файлов в " + storageDir.toAbsolutePath(), e);
        }

        for (String filePart : filePartToGroups.keySet()) {
            if (!result.containsKey(filePart))
                throw new RuntimeException("Файл, содержащий '" + filePart + "', не найден в " + storageDir.toAbsolutePath());
        }

        return Collections.unmodifiableMap(result);
    }

    private static String normalizeFilePart(String filePart) {
        return filePart.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeGroup(String group) {
        return group.trim().toUpperCase(Locale.ROOT);
    }

    private static <K, V> Map<V, K> invertMap(Map<K, V> sourceMap) {
        Map<V, K> invertedMap = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : sourceMap.entrySet()) {
            invertedMap.put(entry.getValue(), entry.getKey());
        }

        return invertedMap;
    }
}