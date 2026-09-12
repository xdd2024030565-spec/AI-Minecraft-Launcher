package com.aimc.ai_bridge;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.Recipe;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.util.collection.DefaultedList;

import java.util.*;

/**
 * 合成配方查询器
 *
 * 从 Minecraft 的 RecipeManager 查询合成配方信息。
 * 支持: 全部配方查询、按物品名称搜索。
 */
public class RecipeLookup {

    /**
     * 获取所有合成配方
     */
    @SuppressWarnings("unchecked")
    public static RecipeInfoList getAllRecipes() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return new RecipeInfoList(false, null, 0);
        }

        List<RecipeInfo> result = new ArrayList<>();
        try {
            RecipeManager manager = mc.world.getRecipeManager();
            Collection<Recipe<?>> allRecipes = manager.values();

            int count = 0;
            for (Recipe<?> recipe : allRecipes) {
                if (count >= 200) break; // 限制数量
                try {
                    RecipeInfo info = toRecipeInfo(recipe);
                    if (info != null) {
                        result.add(info);
                        count++;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] Failed to get all recipes", e);
        }

        return new RecipeInfoList(true, result, result.size());
    }

    /**
     * 根据物品名称查找配方
     *
     * @param itemName 物品名称 (如 "stone_pickaxe" 或 "minecraft:stone_pickaxe")
     */
    @SuppressWarnings("unchecked")
    public static RecipeInfoList findRecipes(String itemName) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return new RecipeInfoList(false, null, 0);
        }

        String search = itemName.toLowerCase()
                .replace("minecraft:", "")
                .replace(" ", "_")
                .trim();
        List<RecipeInfo> result = new ArrayList<>();

        try {
            RecipeManager manager = mc.world.getRecipeManager();
            Collection<Recipe<?>> allRecipes = manager.values();

            for (Recipe<?> recipe : allRecipes) {
                try {
                    ItemStack recipeResult = recipe.getResult();
                    if (recipeResult.isEmpty()) continue;

                    String resultName = recipeResult.getItem().toString()
                            .toLowerCase().replace("minecraft:", "");

                    if (resultName.contains(search)) {
                        RecipeInfo info = toRecipeInfo(recipe);
                        if (info != null) {
                            result.add(info);
                            if (result.size() >= 10) break; // 限制返回数量
                        }
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            AiBridgeMod.LOGGER.error("[AI Bridge] Failed to find recipes for: " + itemName, e);
        }

        return new RecipeInfoList(true, result, result.size());
    }

    /**
     * 将 Minecraft Recipe 对象转换为 RecipeInfo
     */
    @SuppressWarnings("unchecked")
    private static RecipeInfo toRecipeInfo(Recipe<?> recipe) {
        try {
            String id = recipe.getId().toString();
            String group = "";
            try { group = recipe.getGroup(); } catch (Exception ignored) {}

            ItemStack resultStack = recipe.getResult();
            String resultItem = resultStack.isEmpty() ? "unknown"
                    : resultStack.getItem().toString();
            int resultCount = resultStack.getCount();

            List<String> ingredients = new ArrayList<>();
            DefaultedList<Ingredient> ingredientList = recipe.getIngredients();
            for (Ingredient ingredient : ingredientList) {
                try {
                    ItemStack[] matching = ingredient.getMatchingStacks();
                    if (matching != null && matching.length > 0 && !matching[0].isEmpty()) {
                        ingredients.add(matching[0].getItem().toString());
                    } else {
                        ingredients.add("any");
                    }
                } catch (Exception e) {
                    ingredients.add("any");
                }
            }

            return new RecipeInfo(id, group, resultItem, resultCount, ingredients);
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== 数据类 ====================

    public static class RecipeInfoList {
        public boolean connected;
        public List<RecipeInfo> recipes;
        public int count;

        public RecipeInfoList(boolean connected, List<RecipeInfo> recipes, int count) {
            this.connected = connected;
            this.recipes = recipes;
            this.count = count;
        }
    }

    public static class RecipeInfo {
        public String id;
        public String group;
        public String resultItem;
        public int resultCount;
        public List<String> ingredients;

        public RecipeInfo(String id, String group, String resultItem,
                          int resultCount, List<String> ingredients) {
            this.id = id;
            this.group = group;
            this.resultItem = resultItem;
            this.resultCount = resultCount;
            this.ingredients = ingredients;
        }
    }
}
