package me.leeseol.town.structure;

public final class StructureCoreSelection {
    private StructureCoreSelection() {
    }

    public static String selectedStructureId(String persistentSelection, String itemsAdderId) {
        if (persistentSelection == null || persistentSelection.isBlank()) {
            return null;
        }
        return persistentSelection;
    }
}
