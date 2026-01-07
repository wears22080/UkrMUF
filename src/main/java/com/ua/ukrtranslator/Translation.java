package com.ua.ukrtranslator;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.locale.Language;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translation {

    private static final Path FOLDER = FMLPaths.CONFIGDIR.get().resolve("Ukrainizer");
    private static final Path CONFIG_FILE = FOLDER.resolve("config.json");
    private static final Map<String, String> customTranslations = new HashMap<>();
    private static Language lastLanguageInstance = null;
    private static boolean shouldShowWelcome = false;
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("%(\\d+\\$)?d");

    public static void onClientSetup(FMLClientSetupEvent event) {
        setupFiles(false);
        event.enqueueWork(Translation::prepareTranslations);
    }

    public static void setupFiles(boolean force) {
        try {
            if (!Files.exists(FOLDER)) Files.createDirectories(FOLDER);
            extractResource("/translate.json", FOLDER.resolve("translate.json"), force);
            extractResource("/achievement.json", FOLDER.resolve("achievement.json"), force);
            if (!Files.exists(CONFIG_FILE)) {
                saveConfig(true);
                shouldShowWelcome = true;
            } else {
                try (Reader r = Files.newBufferedReader(CONFIG_FILE)) {
                    shouldShowWelcome = JsonParser.parseReader(r).getAsJsonObject().get("showWelcome").getAsBoolean();
                }
            }
        } catch (Exception ignored) {}
    }

    private static void extractResource(String res, Path dest, boolean force) throws IOException {
        if (!force && Files.exists(dest)) return;
        try (InputStream is = Translation.class.getResourceAsStream(res)) {
            if (is != null) Files.copy(is, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void saveConfig(boolean val) {
        try (Writer w = Files.newBufferedWriter(CONFIG_FILE)) {
            JsonObject o = new JsonObject();
            o.addProperty("showWelcome", val);
            w.write(o.toString());
        } catch (IOException ignored) {}
    }

    public static void disableWelcomeScreen() {
        shouldShowWelcome = false;
        saveConfig(false);
    }

    public static void prepareTranslations() {
        customTranslations.clear();
        loadJsonFile(FOLDER.resolve("translate.json"));
        loadAchievementFile(FOLDER.resolve("achievement.json"));
    }

    private static void loadJsonFile(Path path) {
        if (!Files.exists(path)) return;
        try (InputStream is = Files.newInputStream(path)) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
                if (entry.getValue().isJsonObject()) {
                    for (Map.Entry<String, JsonElement> modEntry : entry.getValue().getAsJsonObject().entrySet()) {
                        customTranslations.put(modEntry.getKey(), fixPlaceholders(modEntry.getValue().getAsString()));
                    }
                } else {
                    customTranslations.put(entry.getKey(), fixPlaceholders(entry.getValue().getAsString()));
                }
            }
        } catch (Exception ignored) {}
    }

    private static void loadAchievementFile(Path path) {
        if (!Files.exists(path)) return;
        try (InputStream is = Files.newInputStream(path)) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(is, StandardCharsets.UTF_8)).getAsJsonObject();
            for (Map.Entry<String, JsonElement> modEntry : root.entrySet()) {
                if (modEntry.getValue().isJsonObject()) {
                    for (Map.Entry<String, JsonElement> fileEntry : modEntry.getValue().getAsJsonObject().entrySet()) {
                        if (fileEntry.getValue().isJsonObject()) {
                            for (Map.Entry<String, JsonElement> strEntry : fileEntry.getValue().getAsJsonObject().entrySet()) {
                                customTranslations.put(strEntry.getKey(), fixPlaceholders(strEntry.getValue().getAsString()));
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private static String fixPlaceholders(String input) {
        if (input == null) return null;
        Matcher m = PLACEHOLDER_PATTERN.matcher(input);
        return m.replaceAll("%$1s");
    }

    public static void inject() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getLanguageManager() == null || !mc.getLanguageManager().getSelected().equals("uk_ua")) return;
        if (customTranslations.isEmpty()) return;

        try {
            Language languageInstance = Language.getInstance();
            Field storageField = findMapField(languageInstance);

            if (storageField != null) {
                storageField.setAccessible(true);
                Object rawMap = storageField.get(languageInstance);

                if (rawMap instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, String> currentMap = (Map<String, String>) rawMap;

                    Map<String, String> newMap = new HashMap<>(currentMap);
                    newMap.putAll(customTranslations);

                    storageField.set(languageInstance, newMap);
                    lastLanguageInstance = languageInstance;
                }
            }
        } catch (Exception ignored) {}
    }

    private static Field findMapField(Object inst) {
        Class<?> c = inst.getClass();
        while (c != null && c != Object.class) {
            for (String n : new String[]{"storage", "f_128104_", "field_74816_c"}) {
                try { return c.getDeclaredField(n); } catch (NoSuchFieldException ignored) {}
            }
            for (Field f : c.getDeclaredFields()) {
                if (Map.class.isAssignableFrom(f.getType())) return f;
            }
            c = c.getSuperclass();
        }
        return null;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            Language cur = Language.getInstance();
            if (cur != lastLanguageInstance) inject();
        }
    }

    @SubscribeEvent
    public void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        inject();
        if (shouldShowWelcome) {
            Minecraft.getInstance().tell(() -> Minecraft.getInstance().setScreen(new Screen(customTranslations.size())));
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterClientCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("reloadua").executes(c -> {
            prepareTranslations();
            inject();
            return 1;
        }));
        d.register(Commands.literal("resetua").executes(c -> {
            setupFiles(true);
            prepareTranslations();
            inject();
            return 1;
        }));
    }
}