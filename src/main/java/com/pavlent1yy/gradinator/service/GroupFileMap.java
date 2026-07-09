package com.pavlent1yy.gradinator.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GroupFileMap {

    private static final Map<String, String> GROUP_TO_FILE = Map.ofEntries(
            Map.entry("ИС1", "oit_2sem.xlsx"),
            Map.entry("СА1", "oit_2sem.xlsx"),
            Map.entry("ИБ1", "oit_2sem.xlsx"),

            Map.entry("АР1", "oar_2sem.xlsx"),
            Map.entry("ДИ1", "oar_2sem.xlsx"),
            Map.entry("РК1", "oar_2sem.xlsx"),
            Map.entry("ГД1", "oar_2sem.xlsx"),

            Map.entry("ЮР1", "oep_2sem.xlsx"),
            Map.entry("ЮС1", "oep_2sem.xlsx"),
            Map.entry("ЮР2", "oep_2sem.xlsx"),
            Map.entry("ТУ1", "oep_2sem.xlsx"),
            Map.entry("ЭК1", "oep_2sem.xlsx"),

            Map.entry("СТ1", "so_2sem.xlsx"),
            Map.entry("СД2", "so_2sem.xlsx"),
            Map.entry("МО2", "so_2sem.xlsx"),

            Map.entry("МА1", "mmo_2sem.xlsx"),
            Map.entry("ТТ1", "mmo_2sem.xlsx"),
            Map.entry("МС1", "mmo_2sem.xlsx"),
            Map.entry("УД1", "mmo_2sem.xlsx"),
            Map.entry("ЗМ1", "mmo_2sem.xlsx")
    );

    public static String getPossibleFileByGroupPrefix(String group) {
        return GROUP_TO_FILE.get(group.split("-")[0]);
    }

    public static Set<String> getAllFiles() {
        return new HashSet<>(GROUP_TO_FILE.values());
    }
}
