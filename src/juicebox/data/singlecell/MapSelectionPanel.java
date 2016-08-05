/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2016 Broad Institute, Aiden Lab
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.data.singlecell;

import juicebox.HiC;
import juicebox.HiCGlobals;
import juicebox.data.Dataset;
import juicebox.gui.SuperAdapter;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by muhammadsaadshamim on 8/4/16.
 */
public class MapSelectionPanel extends JPanel {
    
    private static final long serialVersionUID = 81248921738L;
    private final List<ActionListener> actionListeners = new ArrayList<ActionListener>();

    /**
     * @param reader
     * @param controlReader
     */
    public MapSelectionPanel(SuperAdapter superAdapter, Dataset reader, Dataset controlReader) {
        super(new BorderLayout());

        Border padding = BorderFactory.createEmptyBorder(20, 20, 5, 20);
        JTabbedPane tabbedPane = new JTabbedPane();
        actionListeners.clear();

        JPanel mainMaps = generateMapActivationPanel(superAdapter, reader, "Active Hi-C Maps:");
        if (mainMaps != null) {
            mainMaps.setBorder(padding);
            tabbedPane.addTab("Main Map", null, mainMaps, "The main maps");
        }

        JPanel controlMaps = generateMapActivationPanel(superAdapter, controlReader, "Active Control Hi-C Maps:");
        if (controlMaps != null) {
            controlMaps.setBorder(padding);
            tabbedPane.addTab("Control Map", null, controlMaps, "The control maps");
        }

        add(tabbedPane, BorderLayout.CENTER);
    }

    /**
     * @param superAdapter
     * @param hic
     */
    public static void createAndShowGUI(SuperAdapter superAdapter, HiC hic) {
        Dataset reader = hic.getDataset();
        Dataset controlReader = hic.getControlDataset();
        if (reader != null) {
            JFrame frame = new JFrame("Map Selection Panel");
            MapSelectionPanel newContentPane = new MapSelectionPanel(superAdapter, reader, controlReader);
            newContentPane.setOpaque(true);
            frame.setContentPane(newContentPane);
            frame.pack();
            frame.setVisible(true);
        }
    }

    /**
     * @param superAdapter
     * @param reader
     * @param title
     * @return
     */
    private JPanel generateMapActivationPanel(final SuperAdapter superAdapter, Dataset reader, String title) {
        final JButton showItButton = new JButton("Update View");
        if (reader != null) {
            List<JCheckBox> checkBoxes = reader.getCheckBoxes(actionListeners);

            showItButton.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    for (ActionListener listener : actionListeners) {
                        listener.actionPerformed(e);
                    }
                    // because cache keys currently don't account for map activation
                    HiCGlobals.useCache = false;
                    superAdapter.refresh();
                }
            });
            return createPane(title, checkBoxes, showItButton);
        }

        return null;
    }

    /**
     * @param description
     * @param checkBoxes
     * @param showButton
     * @return
     */
    private JPanel createPane(String description, List<JCheckBox> checkBoxes, JButton showButton) {

        JPanel box = new JPanel();
        JLabel label = new JLabel(description);
        box.setLayout(new BoxLayout(box, BoxLayout.PAGE_AXIS));
        box.add(label);

        for (JCheckBox checkBox : checkBoxes) {
            box.add(checkBox);
        }

        JPanel pane = new JPanel(new BorderLayout());
        pane.add(box, BorderLayout.PAGE_START);
        pane.add(showButton, BorderLayout.PAGE_END);
        return pane;
    }
}
