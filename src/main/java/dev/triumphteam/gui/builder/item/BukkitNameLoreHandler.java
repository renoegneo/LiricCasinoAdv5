package dev.triumphteam.gui.builder.item;

import net.kyori.adventure.text.Component;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public final class BukkitNameLoreHandler implements NameLoreHandler {

    private static final BukkitNameLoreHandler INSTANCE = new BukkitNameLoreHandler();

    public static @NotNull BukkitNameLoreHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void name(final @NotNull ItemMeta itemMeta, final @NotNull Component name) {
        itemMeta.displayName(name);
    }

    @Override
    public void lore(final @NotNull ItemMeta itemMeta, final @NotNull List<Component> lore) {
        itemMeta.lore(lore.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }

    @Override
    public void lore(
            final @NotNull ItemMeta itemMeta,
            final @NotNull Consumer<List<@Nullable Component>> lore
    ) {
        final List<Component> components = itemMeta.lore() == null
                ? new ArrayList<>()
                : new ArrayList<>(itemMeta.lore());
        lore.accept(components);
        itemMeta.lore(components.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
}
