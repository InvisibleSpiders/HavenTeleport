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

    @Test
    void dialogInputValuesProvideFloatAndTextValues() {
        DialogInputValues values = new DialogInputValues() {
            @Override
            public Float getFloat(String key) {
                return "amount".equals(key) ? 12.5f : null;
            }

            @Override
            public String getText(String key) {
                return "name".equals(key) ? "Market" : null;
            }
        };

        assertThat(values.getFloat("amount")).isEqualTo(12.5f);
        assertThat(values.getText("name")).isEqualTo("Market");
        assertThat(DialogInputValues.empty().getText("name")).isNull();
    }

    @Test
    void numberInputModelsKeepExistingConstructorCompatibility() {
        DialogInputModel input = new DialogInputModel("amount", "Amount", 0.0f, 100.0f, 1.0f, 10.0f, "%.0f");

        assertThat(input.kind()).isEqualTo(DialogInputModel.Kind.NUMBER);
        assertThat(input.key()).isEqualTo("amount");
        assertThat(input.label()).isEqualTo("Amount");
        assertThat(input.initial()).isEqualTo(10.0f);
        assertThat(input.labelFormat()).isEqualTo("%.0f");
    }

    @Test
    void textInputModelsExposeTextSettings() {
        DialogInputModel input = DialogInputModel.text("name", "Name", "Market", 32);

        assertThat(input.kind()).isEqualTo(DialogInputModel.Kind.TEXT);
        assertThat(input.key()).isEqualTo("name");
        assertThat(input.label()).isEqualTo("Name");
        assertThat(input.textInitial()).isEqualTo("Market");
        assertThat(input.maxLength()).isEqualTo(32);
    }
}
