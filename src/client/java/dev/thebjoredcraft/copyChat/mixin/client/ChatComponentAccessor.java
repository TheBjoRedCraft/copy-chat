package dev.thebjoredcraft.copyChat.mixin.client;

import java.util.List;
import net.minecraft.client.gui.components.ChatComponent;
//? if >=26.1 {
import net.minecraft.client.multiplayer.chat.GuiMessage;
//?} else
//import net.minecraft.client.GuiMessage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ChatComponent.class)
public interface ChatComponentAccessor {
    @Accessor("trimmedMessages")
    List<GuiMessage.Line> copychatTrimmedMessages();

    @Accessor("chatScrollbarPos")
    int copychatChatScrollbarPos();
}
