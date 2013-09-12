/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */
package org.knime.knip.view3d.usercontrols;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.core.node.NodeLogger;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.view3d.render.RenderWindowDependent;

import vtk.vtkBMPWriter;
import vtk.vtkJPEGWriter;
import vtk.vtkPNGWriter;
import vtk.vtkRenderWindow;
import vtk.vtkWindowToImageFilter;

/**
 * This class bundles everything needed to make a screenshot.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeScreenshot extends ViewerComponent implements RenderWindowDependent {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = -8789199550310785897L;

    private enum Format {
        PNG, JPEG, BMP
    };

    private final vtkWindowToImageFilter m_filter;

    private final vtkPNGWriter m_writerPNG;

    private final vtkJPEGWriter m_writerJPEG;

    private final vtkBMPWriter m_writerBMP;

    private vtkRenderWindow m_window = null;

    private int m_magnification = 1;

    private int m_imageCounter = 0;

    private Format m_format = Format.PNG;

    private String m_imageName = null;

    private String m_imageDirectory = null;

    private final Date m_date;

    private final DateFormat m_dateFormat;

    private final JList m_listMagnification;

    private final JList m_listFormat;

    private final JFileChooser m_chooser;

    private final JButton m_buttonShoot;

    private final JButton m_buttonDirectory;

    private final JLabel m_labelName;

    private final JLabel m_labelFormat;

    private final JTextField m_textDirectory;

    private final JTextField m_textName;

    private EventService m_eventService;

    private static final int MAG_LEVEL = 5;

    private static final NodeLogger LOGGER = NodeLogger.getLogger("Viewer3D");

    /**
     * Set up a new Screenshot taker.
     * 
     * @param eventService the eventService to use
     * @param panel the vtkPanel that contains the vtkRenderWindow to use for taking the screenshots
     */
    public Viewer3DNodeScreenshot(final EventService eventService, final Component panel) {
        super("Screenshot", false);

        panel.addComponentListener(new ComponentAdapter() {
            @Override
            public final void componentResized(final ComponentEvent event) {
                final Dimension size = event.getComponent().getSize();
                final int selection = m_listMagnification.getSelectedIndex();

                final String[] magnification = resolutionString(new int[]{size.width, size.height});
                m_listMagnification.setListData(magnification);
                m_listMagnification.setSelectedIndex(selection);
            }
        });

        setEventService(eventService);

        m_imageDirectory = System.getProperty("user.home");
        m_imageName = "Screenshot";

        // the main filter to create the screenshots
        m_filter = new vtkWindowToImageFilter();
        m_filter.SetMagnification(m_magnification);
        m_filter.SetInputBufferTypeToRGBA();

        // Set up the writers
        m_writerPNG = new vtkPNGWriter();
        m_writerJPEG = new vtkJPEGWriter();
        m_writerBMP = new vtkBMPWriter();

        // Set up the date info to append to the files
        m_date = new Date();
        m_dateFormat = new SimpleDateFormat("dd.MMMM.yy '-' HH:mm:ss");

        // Set up the directory chooser
        m_chooser = new JFileChooser();
        m_chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        // The two buttons
        // Icon camera = new
        // ImageIcon(getClass().getResource("org.knime.knip.view3d.res.camera.png"));
        // TODO add the camera here
        m_buttonShoot = new JButton("camera");
        m_buttonShoot.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent event) {
                takeShot();
            }
        });

        m_buttonDirectory = new JButton("Directory");
        m_buttonDirectory.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent event) {
                final int result = m_chooser.showOpenDialog(m_buttonDirectory);

                if (result == JFileChooser.APPROVE_OPTION) {
                    final File dir = m_chooser.getSelectedFile();
                    m_imageDirectory = dir.getAbsolutePath();
                    m_textDirectory.setText(dir.getAbsolutePath());
                }
            }
        });

        // The lists
        final String[] format = {"PNG", "JPEG", "BMP"};
        m_listFormat = new JList(format);
        m_listFormat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_listFormat.setLayoutOrientation(JList.HORIZONTAL_WRAP);
        m_listFormat.setVisibleRowCount(1);
        m_listFormat.setSelectedIndex(0);
        m_listFormat.addListSelectionListener(new ListSelectionListener() {
            @Override
            public final void valueChanged(final ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    switch (m_listFormat.getSelectedIndex()) {
                        case 0:
                            m_format = Format.PNG;
                            break;
                        case 1:
                            m_format = Format.JPEG;
                            break;
                        case 2:
                            m_format = Format.BMP;
                            break;
                    }
                }
            }
        });

        final String[] magnification = resolutionString(new int[]{10, 10});
        m_listMagnification = new JList(magnification);
        m_listMagnification.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_listMagnification.setSelectedIndex(0);
        m_listMagnification.addListSelectionListener(new ListSelectionListener() {
            @Override
            public final void valueChanged(final ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    m_magnification = m_listMagnification.getSelectedIndex() + 1;
                }
            }
        });

        // wrap the magnification list in a panel to add a border
        final JPanel magWrapper = new JPanel();
        magWrapper.setBorder(BorderFactory.createTitledBorder("Magnification Level"));
        // set a min size so that the complete Border Text is shown
        magWrapper.setMinimumSize(new Dimension(160, 10));
        magWrapper.add(m_listMagnification);

        // The labels
        m_labelName = new JLabel("Filename");
        m_labelFormat = new JLabel("Image Format");

        // the text fields
        m_textDirectory = new JTextField(m_imageDirectory);
        m_textDirectory.addFocusListener(new FocusListener() {
            @Override
            public final void focusLost(final FocusEvent event) {
                directoryChanged();
            }

            @Override
            public final void focusGained(final FocusEvent event) {
                // do nothing
            }
        });
        m_textDirectory.addActionListener(new ActionListener() {
            @Override
            public final void actionPerformed(final ActionEvent event) {
                directoryChanged();
            }
        });

        m_textName = new JTextField(m_imageName);
        m_textName.addFocusListener(new FocusListener() {
            @Override
            public final void focusLost(final FocusEvent event) {
                m_imageName = m_textName.getText();
            }

            @Override
            public final void focusGained(final FocusEvent event) {
                // do nothing
            }
        });

        // Set up the layout
        final GroupLayout layout = new GroupLayout(this);
        setLayout(layout);

        final JPanel fileSettings = new JPanel();
        fileSettings.setBorder(BorderFactory.createTitledBorder("File settings"));
        final GroupLayout fileSettingsLayout = new GroupLayout(fileSettings);
        fileSettings.setLayout(fileSettingsLayout);

        final GroupLayout.ParallelGroup fsh0 =
                fileSettingsLayout.createParallelGroup().addComponent(m_buttonDirectory).addComponent(m_labelName)
                        .addComponent(m_labelFormat);

        final GroupLayout.ParallelGroup fsh1 =
                fileSettingsLayout.createParallelGroup().addComponent(m_textDirectory).addComponent(m_textName)
                        .addComponent(m_listFormat);

        // do not resize vertically
        final GroupLayout.ParallelGroup fsv0 =
                fileSettingsLayout
                        .createParallelGroup()
                        .addComponent(m_buttonDirectory, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE)
                        .addComponent(m_textDirectory, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE);

        final GroupLayout.ParallelGroup fsv1 =
                fileSettingsLayout
                        .createParallelGroup()
                        .addComponent(m_labelName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE)
                        .addComponent(m_textName, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE);

        final GroupLayout.ParallelGroup fsv2 =
                fileSettingsLayout
                        .createParallelGroup()
                        .addComponent(m_labelFormat, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE)
                        .addComponent(m_listFormat, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
                                      GroupLayout.PREFERRED_SIZE);

        fileSettingsLayout.setHorizontalGroup(fileSettingsLayout.createSequentialGroup().addGroup(fsh0)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(fsh1));

        fileSettingsLayout.setVerticalGroup(fileSettingsLayout.createSequentialGroup().addGroup(fsv0)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(fsv1)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED).addGroup(fsv2));

        final Component glueVertical = Box.createVerticalGlue();
        final Component glueHorizontal = Box.createHorizontalGlue();

        final GroupLayout.ParallelGroup v0 =
                layout.createParallelGroup().addComponent(m_buttonShoot).addComponent(fileSettings)
                        .addComponent(magWrapper).addComponent(glueHorizontal);

        final GroupLayout.ParallelGroup h1 =
                layout.createParallelGroup().addComponent(fileSettings).addComponent(glueVertical);

        final GroupLayout.SequentialGroup horizontal =
                layout.createSequentialGroup().addComponent(m_buttonShoot)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addGroup(h1)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(magWrapper)
                        .addComponent(glueHorizontal);
        final GroupLayout.SequentialGroup vertical =
                layout.createSequentialGroup().addGroup(v0).addComponent(glueVertical);

        layout.setHorizontalGroup(horizontal);
        layout.setVerticalGroup(vertical);
    }

    private void directoryChanged() {
        final String temp = m_textDirectory.getText();

        final File dir = new File(temp);
        if (dir.exists()) {
            m_imageDirectory = temp;
            m_chooser.setCurrentDirectory(dir);
        } else {
            LOGGER.error("No such directory: " + temp);
        }
    }

    /**
     * Used to calculate the current reoslutions depending on the actual size of the RenderWindow.
     * 
     * @param size the size of the window
     * @return the Strings
     */
    private String[] resolutionString(final int[] size) {
        final String[] result = new String[MAG_LEVEL];

        for (int i = 1; i <= MAG_LEVEL; i++) {
            final String w = Integer.toString(size[0] * i);
            final String h = Integer.toString(size[1] * i);

            result[i - 1] = Integer.toString(i) + " - " + w + "x" + h;
        }

        return result;
    }

    /**
     * Set the RenderWindow to use.
     * 
     * @param window the renderwindow to use
     */
    public final void setWindow(final vtkRenderWindow window) {
        if (window != null) {
            m_window = window;

            m_filter.SetInput(m_window);

            m_writerPNG.SetInputConnection(m_filter.GetOutputPort());
            m_writerJPEG.SetInputConnection(m_filter.GetOutputPort());
            m_writerBMP.SetInputConnection(m_filter.GetOutputPort());
        }
    }

    /**
     * Take a screenshot using the current settings.
     */
    public final void takeShot() {

        if (m_window != null) {
            m_filter.SetMagnification(m_magnification);
            m_filter.Modified();

            final String name =
                    m_imageDirectory + System.getProperty("file.separator") + m_imageName + " "
                            + Integer.toString(m_imageCounter++) + " " + m_dateFormat.format(m_date);
            LOGGER.info("Writing file " + name);

            switch (m_format) {
                case PNG:
                    m_writerPNG.SetFileName(name + ".png");
                    m_writerPNG.Write();
                    break;
                case JPEG:
                    m_writerJPEG.SetFileName(name + ".jpg");
                    m_writerJPEG.Write();
                    break;
                case BMP:
                    m_writerBMP.SetFileName(name + ".bmp");
                    m_writerBMP.Write();
                    break;
            }

            // issue event so the window may be repainted
            m_eventService.publish(new ScreenshotTakenEvent());
        } else {
            LOGGER.error("Cannot take screenshot, no RenderWindow is set");
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.knime.knip.core.ui.event.EventServiceClient#setEventService(EventService)
     */
    @Override
    public final void setEventService(final EventService eventService) {
        if (eventService != null) {
            m_eventService = eventService;
        } else {
            m_eventService = new EventService();
        }
    }

    /**
     * Call this method to delete all references to the vtkObjects.
     */
    public final void delete() {
        m_filter.Delete();
        m_writerPNG.Delete();
        m_writerJPEG.Delete();
        m_writerBMP.Delete();

        // do not delete the m_window, we still need that!

        // do not call the garbage collector here, thats done on the closing of
        // the view
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#getPosition()
     */
    @Override
    public final Position getPosition() {
        // not used
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#saveComponentConfiguration(ObjectOutput)
     */
    @Override
    public final void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // not used
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#loadComponentConfiguration(ObjectInput)
     */
    @Override
    public final void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // not used
    }

    /**
     * {@inheritDoc}
     * 
     * @see RenderWindowDependent#renderWindowChanged(vtkRenderWindow)
     */
    @Override
    public void renderWindowChanged(final vtkRenderWindow window) {
        setWindow(window);
    }
}
