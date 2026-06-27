package me.leeseol.enchanting.model;

public enum EnchantOutput {
    APPLY_TO_ITEM,
    GIVE_BOOK;

    public static EnchantOutput parse(String value) {
        if (value == null) {
            return APPLY_TO_ITEM;
        }
        try {
            return EnchantOutput.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return APPLY_TO_ITEM;
        }
    }
}
