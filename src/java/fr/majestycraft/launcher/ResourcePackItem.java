package fr.majestycraft.launcher;

import javafx.scene.image.Image;

public class ResourcePackItem {
    private String name;
    private Image icon;

    public ResourcePackItem(String name, Image icon) {
        this.name = name;
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public Image getIcon() {
        return icon;
    }
}
