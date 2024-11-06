package twilightforest.item.recipe;

import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.util.NonNullList;

public class UncraftingShapelessRecipe extends ShapelessRecipes implements UncraftingRecipe {

    private final int cost;

    public UncraftingShapelessRecipe(ItemStack output, NonNullList<Ingredient> ingredients, int cost) {
        super("", output, ingredients);
        this.cost = cost;
    }

    @Override
    public int getCost() {
        return this.cost;
    }
}
