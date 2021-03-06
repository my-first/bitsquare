/*
 * This file is part of Bitsquare.
 *
 * Bitsquare is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bitsquare is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bitsquare. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bitsquare.gui.main.account.content.changepassword;

import io.bitsquare.gui.common.view.FxmlView;
import io.bitsquare.gui.common.view.InitializableView;
import io.bitsquare.gui.main.help.Help;
import io.bitsquare.gui.main.help.HelpId;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

import javax.inject.Inject;

@FxmlView
public class ChangePasswordView extends InitializableView<GridPane, ChangePasswordViewModel> {

    @FXML
    HBox buttonsHBox;
    @FXML
    Button saveButton, skipButton;
    @FXML
    PasswordField oldPasswordField, passwordField, repeatedPasswordField;

    @Inject
    private ChangePasswordView(ChangePasswordViewModel model) {
        super(model);
    }

    @Override
    public void initialize() {
        passwordField.textProperty().bindBidirectional(model.passwordField);
        repeatedPasswordField.textProperty().bindBidirectional(model.repeatedPasswordField);

        saveButton.disableProperty().bind(model.saveButtonDisabled);
    }

    @FXML
    private void onSaved() {
        if (!model.requestSavePassword())
            log.debug(model.getErrorMessage()); // TODO use validating TF
    }

    @FXML
    private void onOpenHelp() {
        Help.openWindow(HelpId.SETUP_PASSWORD);
    }

    @FXML
    private void onSkipped() {
    }
}

