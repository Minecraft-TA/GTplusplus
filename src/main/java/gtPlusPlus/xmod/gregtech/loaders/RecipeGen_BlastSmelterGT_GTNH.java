package gtPlusPlus.xmod.gregtech.loaders;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.oredict.OreDictionary;

import org.apache.commons.lang3.ArrayUtils;

import gregtech.api.util.GT_Recipe;
import gtPlusPlus.api.objects.Logger;
import gtPlusPlus.api.objects.minecraft.ItemStackData;
import gtPlusPlus.core.lib.CORE;
import gtPlusPlus.core.recipe.common.CI;
import gtPlusPlus.core.util.math.MathUtils;
import gtPlusPlus.core.util.minecraft.FluidUtils;
import gtPlusPlus.core.util.minecraft.ItemUtils;

public class RecipeGen_BlastSmelterGT_GTNH {

    private static Map<String, FluidStack> mCachedIngotToFluidRegistry = new HashMap<>();
    private static Map<String, String> mCachedHotToColdRegistry = new HashMap<>();

    private static synchronized void setIngotToFluid(final ItemStackData stack, final FluidStack fluid) {
        if (stack != null && fluid != null) {
            mCachedIngotToFluidRegistry.put(stack.getUniqueDataIdentifier(), fluid);
        }
    }

    private static synchronized void setHotToCold(final ItemStackData hot, final ItemStackData cold) {
        if (hot != null && cold != null) {
            mCachedHotToColdRegistry.put(hot.getUniqueDataIdentifier(), cold.getUniqueDataIdentifier());
        }
    }

    private static synchronized FluidStack getFluidFromIngot(final ItemStackData ingot) {
        ItemStackData h = ingot;
        if (mCachedIngotToFluidRegistry.containsKey(h.getUniqueDataIdentifier())) {
            Logger.MACHINE_INFO("[ABS] mCachedIngotToFluidRegistry contains Output Ingot.");
            return mCachedIngotToFluidRegistry.get(h.getUniqueDataIdentifier());
        }
        if (mCachedHotToColdRegistry.containsKey(h.getUniqueDataIdentifier())) {
            Logger.MACHINE_INFO("[ABS] mCachedHotToColdRegistry contains Output Ingot.");
            return mCachedIngotToFluidRegistry.get(mCachedHotToColdRegistry.get(h.getUniqueDataIdentifier()));
        }
        Logger.MACHINE_INFO("[ABS] Neither Cache contains Output Ingot.");
        return null;
    }

    private static boolean isValid(final ItemStack[] inputs, final ItemStack outputs[], final FluidStack[] fluidIn,
            final FluidStack fluidOut) {
        if (inputs != null && outputs != null
                && fluidIn != null
                && fluidOut != null
                && inputs.length > 0
                && outputs.length > 0) {
            return true;
        }
        return false;
    }

    public static synchronized boolean generateGTNHBlastSmelterRecipesFromEBFList() {

        // Make a counting object
        int mSuccess = 0;

        Logger.INFO("[ABS] Starting recipe generation based on EBF recipe map.");
        Logger.INFO("[ABS] Caching Ingots and their Molten fluid..");
        // First, we make sure that we have a valid recipe map of Ingots/Dusts -> Fluids
        if (GT_Recipe.GT_Recipe_Map.sFluidExtractionRecipes.mRecipeList.size() > 0) {
            // So, let's check every recipe
            for (GT_Recipe x : GT_Recipe.GT_Recipe_Map.sFluidExtractionRecipes.mRecipeList) {
                ItemStack validInput = null;
                FluidStack validOutput = null;
                // If there aren't both non empty inputs and outputs, we skip
                if (ArrayUtils.isEmpty(x.mInputs) || ArrayUtils.isEmpty(x.mFluidOutputs)) {
                    continue;
                }

                for (int tag : OreDictionary.getOreIDs(x.mInputs[0])) {
                    String oreName = OreDictionary.getOreName(tag).toLowerCase();
                    String mType = "ingot";
                    if (oreName.startsWith(mType) && !oreName.contains("double")
                            && !oreName.contains("triple")
                            && !oreName.contains("quad")
                            && !oreName.contains("quintuple")) {
                        validInput = x.mInputs[0];
                    }
                }

                validOutput = x.mFluidOutputs[0];

                if (validInput != null) {
                    ItemStackData R = new ItemStackData(validInput);
                    setIngotToFluid(R, validOutput);
                    Logger.MACHINE_INFO(
                            "[ABS][I2F] Cached " + validInput.getDisplayName()
                                    + " to "
                                    + validOutput.getLocalizedName()
                                    + ". Stored Under ID of "
                                    + R.getUniqueDataIdentifier());
                }
            }
        }

        Logger.INFO("[ABS] Caching Ingots and their Hot form...");
        // Second, we make sure that we have a valid recipe map of Hot Ingots -> Cold Ingots
        if (GT_Recipe.GT_Recipe_Map.sVacuumRecipes.mRecipeList.size() > 0) {
            // So, let's check every recipe
            for (GT_Recipe x : GT_Recipe.GT_Recipe_Map.sVacuumRecipes.mRecipeList) {
                ItemStack validInput = null;
                ItemStack validOutput = null;
                // If we the input is an ingot and it and the output are valid, map it to cache.
                if (ArrayUtils.isNotEmpty(x.mInputs) && x.mInputs[0] != null) {
                    validInput = x.mInputs[0];
                }
                if (ArrayUtils.isNotEmpty(x.mOutputs) && x.mOutputs[0] != null) {
                    validOutput = x.mOutputs[0];
                }
                if (validInput != null && validOutput != null) {
                    ItemStackData R1 = new ItemStackData(validInput);
                    ItemStackData R2 = new ItemStackData(validOutput);
                    setHotToCold(R1, R2);
                    Logger.MACHINE_INFO(
                            "[ABS][H2C] Cached " + validInput.getDisplayName()
                                    + " to "
                                    + validOutput.getDisplayName()
                                    + ". Stored Under ID of "
                                    + R1.getUniqueDataIdentifier()
                                    + ", links to ID "
                                    + R2.getUniqueDataIdentifier());
                }
            }
        }

        Logger.INFO("[ABS] Generating recipes based on existing EBF recipes.");
        // Okay, so now lets Iterate existing EBF recipes.
        if (GT_Recipe.GT_Recipe_Map.sBlastRecipes.mRecipeList.size() > 0) {
            for (GT_Recipe x : GT_Recipe.GT_Recipe_Map.sBlastRecipes.mRecipeList) {
                if (x == null) {
                    continue;
                }
                ItemStack[] inputs, outputs;
                FluidStack[] inputsF;
                int voltage, time, special;
                boolean enabled;
                inputs = x.mInputs.clone();
                outputs = x.mOutputs.clone();
                inputsF = x.mFluidInputs.clone();
                voltage = x.mEUt;
                time = x.mDuration;
                enabled = x.mEnabled;
                special = x.mSpecialValue;

                // continue to next recipe if the Temp is too high.
                if (special > 3600) {
                    Logger.MACHINE_INFO("[ABS] Skipping ABS addition for GTNH due to temp.");
                    continue;
                } else {
                    FluidStack mMoltenStack = null;
                    int mMoltenCount = 0;
                    // If We have a valid Output, let's try use our cached data to get it's molten form.
                    if (x.mOutputs != null && x.mOutputs[0] != null) {
                        mMoltenCount = x.mOutputs[0].stackSize;
                        ItemStackData R = new ItemStackData(x.mOutputs[0]);
                        Logger.MACHINE_INFO(
                                "[ABS] Found " + x.mOutputs[0].getDisplayName()
                                        + " as valid EBF output, finding it's fluid from the cache. We will require "
                                        + (144 * mMoltenCount)
                                        + "L. Looking for ID "
                                        + R.getUniqueDataIdentifier());
                        FluidStack tempFluid = getFluidFromIngot(R);
                        if (tempFluid != null) {
                            // Logger.MACHINE_INFO("[ABS] Got Fluid from Cache.");
                            mMoltenStack = FluidUtils.getFluidStack(tempFluid, mMoltenCount * 144);
                        } else {
                            Logger.MACHINE_INFO("[ABS] Failed to get Fluid from Cache.");
                        }
                    }
                    // If this recipe is enabled and we have a valid molten fluidstack, let's try add this recipe.
                    if (enabled && isValid(inputs, outputs, inputsF, mMoltenStack)) {
                        // Boolean to decide whether or not to create a new circuit later
                        boolean circuitFound = false;

                        // Build correct input stack
                        ArrayList<ItemStack> aTempList = new ArrayList<>();
                        for (ItemStack recipeItem : inputs) {
                            if (ItemUtils.isControlCircuit(recipeItem)) {
                                circuitFound = true;
                            }
                            aTempList.add(recipeItem);
                        }

                        inputs = aTempList.toArray(new ItemStack[aTempList.size()]);
                        int inputLength = inputs.length;
                        // If no circuit was found, increase array length by 1 to add circuit at newInput[0]
                        if (!circuitFound) {
                            inputLength++;
                        }

                        ItemStack[] newInput = new ItemStack[inputLength];

                        int l = 0;
                        // If no circuit was found, add a circuit here
                        if (!circuitFound) {
                            l = 1;
                            newInput[0] = CI.getNumberedCircuit(inputs.length);
                        }

                        for (ItemStack y : inputs) {
                            newInput[l++] = y;
                        }

                        // Logger.MACHINE_INFO("[ABS] Generating ABS recipe for "+mMoltenStack.getLocalizedName()+".");
                        if (CORE.RA.addBlastSmelterRecipe(
                                newInput,
                                (inputsF.length > 0 ? inputsF[0] : null),
                                mMoltenStack,
                                100,
                                MathUtils.roundToClosestInt(time * 0.8),
                                voltage,
                                special)) {
                            // Logger.MACHINE_INFO("[ABS] Success.");
                            mSuccess++;
                        } else {
                            Logger.MACHINE_INFO("[ABS] Failure.");
                        }
                    } else {
                        if (!enabled) {
                            Logger.MACHINE_INFO("[ABS] Failure. EBF recipe was not enabled.");
                        } else {
                            Logger.MACHINE_INFO("[ABS] Failure. Invalid Inputs or Outputs.");
                            if (inputs == null) {
                                Logger.MACHINE_INFO("[ABS] Inputs were not Valid.");
                            } else {
                                Logger.MACHINE_INFO("[ABS] inputs size: " + inputs.length);
                            }
                            if (outputs == null) {
                                Logger.MACHINE_INFO("[ABS] Outputs were not Valid.");
                            } else {
                                Logger.MACHINE_INFO("[ABS] outputs size: " + outputs.length);
                            }
                            if (inputsF == null) {
                                Logger.MACHINE_INFO("[ABS] Input Fluids were not Valid.");
                            } else {
                                Logger.MACHINE_INFO("[ABS] inputsF size: " + inputsF.length);
                            }
                            if (mMoltenStack == null) {
                                Logger.MACHINE_INFO("[ABS] Output Fluid were not Valid.");
                            }
                        }
                    }
                }
            }
        } else {
            Logger.MACHINE_INFO("[ABS] Failure. Did not find any EBF recipes to iterate.");
        }

        Logger.INFO("[ABS] Processed " + mSuccess + " recipes.");
        return mSuccess > 0;
    }
}
