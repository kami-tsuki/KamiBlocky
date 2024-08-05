package kami.lib.kamiblocky.extensions;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class WorldSmeltingRecipes {

    private static final Map<Material, Map<Double, Map<Material, Double>>> WORLD_SMELTING_RECIPES = new HashMap<>();
    private static final Map<Material, Set<Material>> MATERIAL_ALIASES = new HashMap<>();
    private static final Random RANDOM = new Random();
    private static final Logger LOGGER = Logger.getLogger("WorldSmeltingRecipes");

    public static void loadRecipes(File file) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        //interfaces:
        //  Magma:
        //    temperatures:
        //      1200.0:
        //        LAVA: 1
        //        BLACKSTONE: 15
        //        BASALT: 4
        //        MAGMA_BLOCK: 80
        //      1400.0:
        //        LAVA: 10
        //        AIR: 90
        //      1600.0:
        //        AIR: 100
        //recipes:
        //  TUFF:
        //    valid_inputs:
        //      - TUFF
        //    inherit: Magma
        //    temperatures:
        //      800.0:
        //        STONE: 90
        //        COBBLESTONE: 10

        ConfigurationSection interfacesSection = config.getConfigurationSection("interfaces");
        ConfigurationSection recipesSection = config.getConfigurationSection("recipes");
        LOGGER.log(Level.INFO, "Loading smelting recipes...");
        if (recipesSection == null) {
            LOGGER.log(Level.WARNING, "No recipes found in the configuration file.");
            return;
        }

        for (String MaterialName : recipesSection.getKeys(false)) {
            Material material = getValidMaterial(MaterialName);
            if (material == null) {
                continue;
            }
            ConfigurationSection RecipeSection = recipesSection.getConfigurationSection(MaterialName);
            if (RecipeSection == null) {
                LOGGER.log(Level.WARNING, "No recipe found for material " + MaterialName);
                continue;
            }

            Set<Material> validInputs = new HashSet<>();
            List<String> validInputNames = RecipeSection.getStringList("valid_inputs");
            for (String validInputName : validInputNames) {
                Material validInput = getValidMaterial(validInputName);
                if (validInput != null) {
                    validInputs.add(validInput);
                }
            }
            MATERIAL_ALIASES.put(material, validInputs);

            Map<Double, Map<Material, Double>> temperatureRecipes = new HashMap<>();


            String targetInterface = RecipeSection.getString("inherit");
            if (targetInterface != null && interfacesSection != null) {
                ConfigurationSection targetInterfaceSection = interfacesSection.getConfigurationSection(targetInterface);
                if (targetInterfaceSection != null) {
                    ConfigurationSection temperaturesSection = targetInterfaceSection.getConfigurationSection("temperatures");
                    if (temperaturesSection != null) {
                        List<Double> temperatures = new ArrayList<>(temperaturesSection.getKeys(false).stream()
                                .map(Double::parseDouble)
                                .sorted()
                                .toList());
                        for (Double temperature : temperatures) {
                            ConfigurationSection temperatureSection = temperaturesSection.getConfigurationSection(temperature.toString());
                            if (temperatureSection != null) {
                                Map<Material, Double> outputChances = new HashMap<>();
                                for (String outputMaterialName : temperatureSection.getKeys(false)) {
                                    Material outputMaterial = getValidMaterial(outputMaterialName);
                                    if (outputMaterial != null) {
                                        outputChances.put(outputMaterial, temperatureSection.getDouble(outputMaterialName));
                                        LOGGER.log(Level.INFO, "Loaded output material \"" + outputMaterialName + "\" for temperature " + temperature + "°C.");
                                    } else {
                                        LOGGER.log(Level.WARNING, "Invalid output material \"" + outputMaterialName + "\" for temperature " + temperature + "°C.");
                                    }
                                }
                                temperatureRecipes.put(temperature, outputChances);
                                LOGGER.log(Level.INFO, "Loaded recipe for " + MaterialName + " at " + temperature + "°C.");
                            } else {
                                LOGGER.log(Level.WARNING, "No recipe found for temperature " + temperature + "°C.");
                            }
                        }
                        WORLD_SMELTING_RECIPES.put(material, temperatureRecipes);
                        LOGGER.log(Level.INFO, "Loaded " + temperatures.size() + " temperatures for " + MaterialName + ".");
                    } else {
                        LOGGER.log(Level.WARNING, "No temperatures found for material " + MaterialName);
                        continue;
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No interface found for material " + MaterialName);
                    continue;
                }

            } else if (targetInterface != null && interfacesSection == null) {
                LOGGER.log(Level.WARNING, "No interface found for material " + MaterialName);
                continue;
            } else {
                LOGGER.log(Level.WARNING, "No interface found for material " + MaterialName);
                continue;
            }

            ConfigurationSection temperaturesSection = RecipeSection.getConfigurationSection("temperatures");
            if (temperaturesSection == null) {
                LOGGER.log(Level.WARNING, "No temperatures found for material " + MaterialName);
                continue;
            }


            List<Double> temperatures = new ArrayList<>(temperaturesSection.getKeys(false).stream()
                    .map(Double::parseDouble)
                    .sorted()
                    .toList());

            for (Double temperature : temperatures) {
                ConfigurationSection temperatureSection = temperaturesSection.getConfigurationSection(temperature.toString());
                if (temperatureSection == null) {
                    LOGGER.log(Level.WARNING, "No recipe found for temperature " + temperature + "°C.");
                    continue;
                }

                Map<Material, Double> outputChances = new HashMap<>();
                for (String outputMaterialName : temperatureSection.getKeys(false)) {
                    Material outputMaterial = getValidMaterial(outputMaterialName);
                    if (outputMaterial != null) {
                        outputChances.put(outputMaterial, temperatureSection.getDouble(outputMaterialName));
                        LOGGER.log(Level.INFO, "Loaded output material \"" + outputMaterialName + "\" for temperature " + temperature + "°C.");
                    } else {
                        LOGGER.log(Level.WARNING, "Invalid output material \"" + outputMaterialName + "\" for temperature " + temperature + "°C.");
                    }
                }
                temperatureRecipes.put(temperature, outputChances);
                LOGGER.log(Level.INFO, "Loaded recipe for " + MaterialName + " at " + temperature + "°C.");
            }
            WORLD_SMELTING_RECIPES.put(material, temperatureRecipes);
            LOGGER.log(Level.INFO, "Loaded " + temperatures.size() + " temperatures for " + MaterialName + ".");
        }
        LOGGER.log(Level.INFO, "Loaded " + WORLD_SMELTING_RECIPES.size() + " smelting recipes.");
    }

    public static Map<Double, Map<Material, Double>> getRecipes(Material input) {
        Material baseMaterial = getBaseMaterial(input);
        return WORLD_SMELTING_RECIPES.get(baseMaterial);
    }

    public static Material getOutput(Material input, double temperature) {
        Material baseMaterial = getBaseMaterial(input);
        Map<Double, Map<Material, Double>> recipes = WORLD_SMELTING_RECIPES.get(baseMaterial);

        if (recipes != null) {
            Double closestTemperature = recipes.keySet().stream()
                    .filter(temp -> temp <= temperature)
                    .max(Double::compareTo)
                    .orElse(null);
            LOGGER.log(Level.INFO, "Closest temperature for " + input.name() + " at " + temperature + "°C is " + closestTemperature + "°C.");

            if (closestTemperature != null) {
                Map<Material, Double> outputChances = recipes.get(closestTemperature);
                if (outputChances != null) {
                    double roll = RANDOM.nextDouble() * 100.0;
                    double cumulativeChance = 0.0;
                    for (Map.Entry<Material, Double> entry : outputChances.entrySet()) {
                        cumulativeChance += entry.getValue();
                        if (roll <= cumulativeChance) {
                            return entry.getKey();
                        }
                    }
                } else {
                    LOGGER.log(Level.WARNING, "No output found for " + input.name() + " at " + temperature + "°C.");
                }
            } else {
                LOGGER.log(Level.WARNING, "No recipe found for " + input.name() + " at " + temperature + "°C.");
            }
        }
        LOGGER.log(Level.WARNING, "No output found for " + input.name() + " at " + temperature + "°C.");
        return input;
    }

    private static Material getBaseMaterial(Material input) {
        return MATERIAL_ALIASES.entrySet().stream()
                .filter(entry -> entry.getValue().contains(input))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(input);
    }

    private static Material getValidMaterial(String materialName) {
        try {
            return Material.valueOf(materialName.toUpperCase());
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.WARNING, "material \"" + materialName + "\" is not a valid material.");
            return null;
        }
    }
}
