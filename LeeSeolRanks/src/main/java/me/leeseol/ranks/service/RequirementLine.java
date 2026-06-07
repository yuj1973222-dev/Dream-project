package me.leeseol.ranks.service;

public record RequirementLine(String label, String current, String required, boolean met) {
    public String format() {
        return (met ? "&a✔ " : "&c✘ ") + "&f" + label + ": &e" + current + " &7/ &e" + required;
    }
}
