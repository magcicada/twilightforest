package twilightforest.item.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;

public class UncraftingShapedRecipe extends ShapedRecipes implements UncraftingRecipe {

    private final int cost;

    public UncraftingShapedRecipe(int width, int height, NonNullList<Ingredient> ingredients, ItemStack result, int cost) {
        super("", width, height, ingredients, result);
        this.cost = cost;
    }

    @Override
    public int getCost() {
        return this.cost;
    }
}
