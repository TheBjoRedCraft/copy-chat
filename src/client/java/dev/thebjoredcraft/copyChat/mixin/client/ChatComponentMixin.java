package dev.thebjoredcraft.copyChat.mixin.client;

import dev.thebjoredcraft.copyChat.client.selection.ChatSelection;
import net.minecraft.client.gui.Font;
//? if >=26.1 {
import net.minecraft.client.gui.GuiGraphicsExtractor;
//?} else
//import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    //? if >=26.1 {
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
    //?} else {
    /*@Inject(
        method = "render(Lnet/minecraft/client/gui/GuiGraphics;Lnet/minecraft/client/gui/Font;IIIZZ)V",
        at = @At("TAIL")
    )
    private void copychat$renderSelection(
        GuiGraphics graphics,
        Font font,
        int ticks,
        int mouseX,
        int mouseY,
        boolean focused,
        boolean changeCursorOnInsertions,
        CallbackInfo ci
    ) {
        if (focused) {
            ChatSelection.INSTANCE.render((ChatComponent) (Object) this, graphics, font);
        }
    }
    *///?}
}
