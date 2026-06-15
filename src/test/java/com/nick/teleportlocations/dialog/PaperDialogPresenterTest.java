package com.nick.teleportlocations.dialog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import net.kyori.adventure.text.event.ClickCallback;
import org.junit.jupiter.api.Test;

final class PaperDialogPresenterTest {
    @Test
    void callbackOptionsAreProvidedForCustomClickActions() {
        ClickCallback.Options options = PaperDialogPresenter.callbackOptions();

        assertThat(options).isNotNull();
        assertThat(options.uses()).isEqualTo(ClickCallback.UNLIMITED_USES);
        assertThat(options.lifetime()).isEqualTo(ClickCallback.DEFAULT_LIFETIME);
    }

    @Test
    void readOnlyMenusUseNoticeDialogType() {
        DialogMenuModel model = new DialogMenuModel("Warps", List.of("No warps are available."), List.of());

        assertThat(PaperDialogPresenter.dialogTypeFor(model)).isEqualTo(PaperDialogPresenter.DialogKind.NOTICE);
    }

    @Test
    void clickableMenusUseMultiActionDialogType() {
        DialogMenuModel model = new DialogMenuModel(
                "Admin",
                List.of("Claim Bypass: Disabled"),
                List.of(new DialogActionModel("admin-toggle-claims-bypass", "Toggle Claim Bypass"))
        );

        assertThat(PaperDialogPresenter.dialogTypeFor(model)).isEqualTo(PaperDialogPresenter.DialogKind.MULTI_ACTION);
    }
}
