package com.nick.teleportlocations.dialog;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import java.util.List;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

public final class PaperDialogPresenter {
    private DialogActionHandler actionHandler = DialogActionHandler.noop();

    enum DialogKind {
        MULTI_ACTION,
        NOTICE
    }

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
        List<DialogInput> inputs = model.inputs().stream()
                .map(PaperDialogPresenter::toPaperInput)
                .toList();
        List<ActionButton> actions = model.actions().stream()
                .map(action -> ActionButton.builder(Component.text(action.label()))
                        .tooltip(Component.text(action.key()))
                        .action(DialogAction.customClick((response, audience) -> handleClick(audience, action.key(), inputValues(response)), callbackOptions()))
                        .build())
                .toList();
        return Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(Component.text(model.title()))
                        .body(body)
                        .inputs(inputs)
                        .build())
                .type(paperDialogType(dialogTypeFor(model), actions)));
    }

    private static DialogInput toPaperInput(DialogInputModel input) {
        return switch (input.kind()) {
            case NUMBER -> DialogInput.numberRange(
                            input.key(),
                            Component.text(input.label()),
                            input.min(),
                            input.max()
                    )
                    .step(input.step())
                    .initial(input.initial())
                    .labelFormat(input.labelFormat())
                    .build();
            case TEXT -> DialogInput.text(input.key(), Component.text(input.label()))
                    .initial(input.textInitial())
                    .maxLength(input.maxLength())
                    .build();
        };
    }

    static ClickCallback.Options callbackOptions() {
        return ClickCallback.Options.builder()
                .uses(ClickCallback.UNLIMITED_USES)
                .lifetime(ClickCallback.DEFAULT_LIFETIME)
                .build();
    }

    static DialogKind dialogTypeFor(DialogMenuModel model) {
        return model.actions().isEmpty() ? DialogKind.NOTICE : DialogKind.MULTI_ACTION;
    }

    private static DialogType paperDialogType(DialogKind kind, List<ActionButton> actions) {
        return switch (kind) {
            case NOTICE -> DialogType.notice();
            case MULTI_ACTION -> DialogType.multiAction(actions).build();
        };
    }

    private static DialogInputValues inputValues(DialogResponseView response) {
        return new DialogInputValues() {
            @Override
            public Float getFloat(String key) {
                return response.getFloat(key);
            }

            @Override
            public String getText(String key) {
                return response.getText(key);
            }
        };
    }

    private void handleClick(Audience audience, String actionKey, DialogInputValues inputValues) {
        if (audience instanceof Player player) {
            actionHandler.handle(player, actionKey, inputValues);
        }
    }
}
