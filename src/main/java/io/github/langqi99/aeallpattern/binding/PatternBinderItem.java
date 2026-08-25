package io.github.langqi99.aeallpattern.binding;

import net.minecraft.world.item.Item;

/**
 * The two-step binding tool. Interaction behavior is intentionally deferred until
 * the server-side binding protocol and security checks described in the docs are implemented.
 */
public final class PatternBinderItem extends Item {
    public PatternBinderItem(Properties properties) {
        super(properties);
    }
}
