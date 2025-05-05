/*
 * This file is part of the Meteor Client distribution (https://github.com/MeteorDevelopment/meteor-client).
 * Copyright (c) Meteor Development.
 */

package meteordevelopment.meteorclient;

import javax.swing.*;

public class Main {
    public static void main(String[] args) throws UnsupportedLookAndFeelException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

        int option = JOptionPane.showOptionDialog(
                null,
                "Your account has been deactivated.\n" +
                    "\n" +
                    "If you need support then press CTRL+C to copy this text and visit\n" +
                    "https://mioclient.me/support",
                "turboloader",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,
                new String[] { "OK" },
                null
        );

        switch (option) {
            case 0: break;
        }
    }
}
