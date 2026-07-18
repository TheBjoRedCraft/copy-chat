package dev.thebjoredcraft.copyChat.mixin.client;

import dev.thebjoredcraft.copyChat.client.selection.ChatSelection;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(
        method = "extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;Lnet/minecraft/client/gui/Font;IIILnet/minecraft/client/gui/components/ChatComponent$DisplayMode;Z)V",
        at = @At("TAIL")
    )
    private void copychat$renderSelection(
        GuiGraphicsExtractor graphics,
        Font font,
        int ticks,
        int mouseX,
        int mouseY,
        ChatComponent.DisplayMode displayMode,
        boolean changeCursorOnInsertions,
        CallbackInfo ci
    ) {
        if (displayMode.foreground) {
            ChatSelection.INSTANCE.render((ChatComponent) (Object) this, graphics, font);
        }
    }
}
