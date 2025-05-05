/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient.gui.screens.accounts;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.containers.WTable;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.systems.accounts.Accounts;
import meteordevelopment.meteorclient.systems.accounts.types.CrackedAccount;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TempAccountScreen extends AddAccountScreen {
    public TempAccountScreen(GuiTheme theme, AccountsScreen parent) {
        super(theme, "Temp", parent);
    }

    @Override
    public void initWidgets() {
        WTable t = add(theme.table()).widget();

        // Name
        t.add(theme.label("Name: "));
        WTextBox name = t.add(theme.textBox("", "seasnail8169", (text, c) ->
            // Username can't contain spaces
            c != ' '
        )).minWidth(400).expandX().widget();
        name.setFocused(true);
        t.row();

        // Add
        add = t.add(theme.button("OK")).expandX().widget();
        add.action = () -> {
            if (!name.get().isEmpty() && name.get().length() < 17) {
                CrackedAccount account = new CrackedAccount(name.get());
                MeteorExecutor.execute(() -> {
                    if (account.fetchInfo()) {
                        account.getCache().loadHead();

                        account.login();

                        this.locked = false;
                        this.close();

                        parent.reload();

                        return;
                    }

                    this.locked = false;
                });
            }
        };

        enterAction = add.action;
    }
}
