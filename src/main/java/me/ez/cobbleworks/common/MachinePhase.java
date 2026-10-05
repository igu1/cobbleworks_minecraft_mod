package me.ez.cobbleworks.common;

import net.minecraft.util.StringRepresentable;

public enum MachinePhase implements StringRepresentable {
    WAITING("waiting"), RUNNING("running"), PAUSED("paused"), FULL("full");

    private final String name;
    MachinePhase(String name) { this.name = name; }
    @Override public String getSerializedName() { return name; }
}
