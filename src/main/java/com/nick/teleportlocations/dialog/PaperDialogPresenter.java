package com.nick.teleportlocations.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

public final class PaperDialogPresenter {
    private DialogActionHandler actionHandler = DialogActionHandler.noop();

    public void setActionHandler(DialogActionHandler actionHandler) {
        this.actionHandler = actionHandler;
    }

    public void show(Player player, DialogMenuModel model) {
        player.showDialog(toDialog(model));
    }

    public Dialog toDialog(DialogMenuModel model) {
        List<DialogBody> body = model.lines().stream()
                .<DialogBody>map(line -> DialogBody.plainMessage(Component.text(line)))
                .toList();
        List<ActionButton> actions = model.actions().stream()
                .map(action -> ActionButton.builder(Component.text(action.label()))
                        .tooltip(Component.text(action.key()))
                        .action(DialogAction.customClick((response, audience) -> handleClick(audience, action.key()), (ClickCallback.Options) null))
                        .build())
                .toList();
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(model.title()))
                        .body(body)
                        .build())
                .type(DialogType.multiAction(actions).build()));
    }

    private void handleClick(Audience audience, String actionKey) {
        if (audience instanceof Player player) {
            actionHandler.handle(player, actionKey);
        }
    }
}
