package dev.thebjoredcraft.copyChat.mixin.client;

import dev.thebjoredcraft.copyChat.client.selection.ChatSelection;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin extends Screen {
    protected ChatScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void copychat$mouseClicked(MouseButtonEvent event, boolean doubleClick, CallbackInfoReturnable<Boolean> cir) {
        if (ChatSelection.INSTANCE.handleMouseClicked(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void copychat$keyPressed(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (ChatSelection.INSTANCE.handleKeyPressed(event)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void copychat$removed(CallbackInfo ci) {
        ChatSelection.INSTANCE.clear();
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (ChatSelection.INSTANCE.handleMouseDragged(event)) {
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (ChatSelection.INSTANCE.handleMouseReleased(event)) {
            return true;
        }
        return super.mouseReleased(event);
    }
}
