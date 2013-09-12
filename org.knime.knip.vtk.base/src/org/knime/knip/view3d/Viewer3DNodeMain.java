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
package org.knime.knip.view3d;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.LayoutStyle;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import net.imglib2.type.numeric.RealType;

import org.knime.core.node.NodeLogger;
import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.NormalizationPerformedEvent;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionBundle;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionControlPanel;
import org.knime.knip.view3d.image.LoadImageEvent;
import org.knime.knip.view3d.image.Viewer3DNodeImageAdmin;
import org.knime.knip.view3d.image.Viewer3DNodeVolume;
import org.knime.knip.view3d.render.Viewer3DNodeMainRenderer;
import org.knime.knip.view3d.render.Viewer3DNodeSliceViewer;
import org.knime.knip.view3d.usercontrols.DrawChgEvent;
import org.knime.knip.view3d.usercontrols.ScreenshotTakenEvent;
import org.knime.knip.view3d.usercontrols.Viewer3DNodeScreenshot;
import org.knime.knip.view3d.usercontrols.Viewer3DNodeSliceControl;

import vtk.vtkObject;
import vtk.vtkReferenceInformation;

/**
 * This class controls the actual Rendering.
 * 
 * @param <T> the type of the image
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Muething (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeMain<T extends RealType<T>> extends ViewerComponent implements ActionListener {

    private enum Mode {
        GRAY, RGB;
    }

    /**
     * A worker class to load the images on a different thread.<br>
     * 
     * The <b>only</b> reason this class is protected and not private is because the private class would not be a "real"
     * class, rather being somehow embedded into the outer class, so that we can not register it at as a separate
     * instance on the EventService. The latter is needed to easily manipulate multiple JProgressBars at the same time.
     */
    protected final class LoadImages extends SwingWorker<Void, Integer> {

        private final JProgressBar m_progress;

        /**
         * Construct a new instance to load the currently selected images.
         */
        public LoadImages() {
            m_progress = new JProgressBar(0, 100);
            m_progress.setString("Loading Image, please wait ...");
            m_progress.setStringPainted(true);

            addPropertyChangeListener(new PropertyChangeListener() {
                @Override
                public void propertyChange(final PropertyChangeEvent e) {
                    if ("progress".equals(e.getPropertyName()) && (m_progress.getValue() < (Integer)e.getNewValue())) {
                        m_progress.setValue((Integer)e.getNewValue());
                    }
                }
            });

            m_panelNorth.add(m_progress, BorderLayout.NORTH);
            m_mainPanel.validate();
        }

        /**
         * Load the currently selected image on a background thread, so that it does not completly stop processing on
         * the EDT.<br>
         * 
         * {@inheritDoc}
         * 
         * @see SwingWorker#doInBackground()
         */
        @Override
        public Void doInBackground() {

            m_volume = m_admin.getVolume(m_admin.getAxes().getManipulatedVolume());

            // get the new volumes
            m_rendered = m_admin.getVolumes();

            if (m_mode == Mode.GRAY) {
                for (final Viewer3DNodeVolume v : m_rendered) {
                    v.setGrayMode();
                }
            } else {
                for (final Viewer3DNodeVolume v : m_rendered) {
                    v.setRGBMode();
                }
            }

            // For this to work the Events must FIRST set the images to render
            // and SECOND the image to manipulate
            if (m_transferControl.isOnlyOneFunc()) {
                applyTFToVolumes();
            }

            return null;
        }

        /**
         * Display the loaded images.<br>
         * 
         * {@inheritDoc}
         * 
         * @see SwingWorker#done()
         */
        @Override
        protected void done() {
            if (!m_deleted) {
                // render the new images
                m_renderWindow.setVolumes(m_rendered);
                m_sliceRenderer.setImages(m_rendered);

                // remove progress bar
                m_panelNorth.remove(m_progress);
                m_mainPanel.validate();

                // repaint new stuff

                // set controls to display the new settings correctly
                TransferFunctionControlPanel.Memento memento = m_volumeToMemento.get(m_volume);

                if (memento == null) {
                    final List<TransferFunctionBundle> bundles = new ArrayList<TransferFunctionBundle>();
                    bundles.add(m_volume.getBundleGray());
                    bundles.add(m_volume.getBundleRGB());

                    TransferFunctionBundle current;

                    if (m_mode == Mode.GRAY) {
                        current = m_volume.getBundleGray();
                    } else {
                        current = m_volume.getBundleRGB();
                    }

                    memento = m_transferControl.createMemento(bundles, m_volume.getHistogram(), current);
                }

                m_transferControl.setState(memento);

                m_transferControl.repaint();
                repaintImage();

                m_loading = null;
                m_sliceControl.setEnabled(true);
            }
        }

        /**
         * Called whenever another part of the images has been loaded.
         * 
         * @param e the event
         */
        @EventListener
        public void onLoadImage(final LoadImageEvent e) {
            setProgress(e.getProgress());
        }
    }

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = -6100896201596892945L;

    /* the image thread that is currently running */
    private LoadImages m_loading = null;

    private Viewer3DNodeImageAdmin<T> m_admin = null;

    private Viewer3DNodeVolume m_volume;

    private List<Viewer3DNodeVolume> m_rendered;

    private JTabbedPane m_southTabs;

    private JPanel m_mainPanel;

    private final Map<Viewer3DNodeVolume, TransferFunctionControlPanel.Memento> m_volumeToMemento =
            new HashMap<Viewer3DNodeVolume, TransferFunctionControlPanel.Memento>();

    private Viewer3DNodeSliceViewer m_sliceRenderer = null;

    private Viewer3DNodeMainRenderer m_renderWindow = null;

    private TransferFunctionControlPanel m_transferControl = null;

    private Viewer3DNodeSliceControl m_sliceControl = null;

    private Viewer3DNodeScreenshot m_screenshot = null;

    private JList m_listMapper;

    private JPanel m_transferPanel = null;

    private JPanel m_panelSettings = null;

    private JPanel m_panelNorth = null;

    private EventService m_eventService;

    private Mode m_mode;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Viewer3DNodeMain.class);

    private boolean m_initalized = false;

    // Indicates wheter the VTK components have been deleted already
    private boolean m_deleted = false;

    /**
     * Set up a new Renderer.
     */
    public Viewer3DNodeMain() {
        super("3D Rendering", true);

        // always start in this mode
        m_mode = Mode.RGB;

        // create a new eventService
        setEventService(m_eventService);

        // set up all lists
        m_rendered = new ArrayList<Viewer3DNodeVolume>();

        // Set up the control panels
        m_transferControl = new TransferFunctionControlPanel();
        m_transferControl.setAutoApply(true);
        m_transferControl.setOnlyOneFunc(false);

        // create the mainPanel
        m_mainPanel = new JPanel();
        m_mainPanel.setLayout(new BorderLayout());

        // renderwindow
        setUpMainRenderer();

        // for all controls
        m_transferPanel = new JPanel();
        m_transferPanel.setLayout(new BoxLayout(m_transferPanel, BoxLayout.X_AXIS));

        // set up the control zone
        m_sliceControl = new Viewer3DNodeSliceControl(m_eventService, null);

        // add everything to the controls panel
        m_transferPanel.add(m_transferControl);
        m_transferPanel.add(m_sliceControl);

        // screenshot control
        m_screenshot = new Viewer3DNodeScreenshot(m_eventService, m_renderWindow);
        m_renderWindow.addRenderWindowDependent(m_screenshot);

        // the general settings
        m_panelSettings = setUpGeneralSettingsPanel();

        // add everything to the tab
        m_southTabs = new JTabbedPane();
        m_southTabs.addTab("Transfer Function", m_transferPanel);
        m_southTabs.addTab("Screenshot", m_screenshot);
        m_southTabs.addTab("General Settings", m_panelSettings);

        final JPanel south = new JPanel();
        south.setLayout(new BoxLayout(south, BoxLayout.X_AXIS));
        south.add(setUpButtons());
        south.add(Box.createHorizontalStrut(8));
        south.add(m_southTabs);

        m_mainPanel.add(south, BorderLayout.SOUTH);

        setUpSliceRenderer();

        // add the northern panel
        m_panelNorth = new JPanel();
        m_panelNorth.setLayout(new BoxLayout(m_panelNorth, BoxLayout.Y_AXIS));
        m_mainPanel.add(m_panelNorth, BorderLayout.NORTH);

        setLayout(new BorderLayout());
        add(m_mainPanel, BorderLayout.CENTER);

        setMode(m_mode);

        m_transferControl.addActionListener(this);

        m_initalized = true;
    }

    @SuppressWarnings("serial")
    private Component setUpButtons() {
        final JPanel panel = new JPanel();
        final GroupLayout layout = new GroupLayout(panel);
        panel.setLayout(layout);

        final JButton camera = new JButton(new AbstractAction("Reset camera") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                m_renderWindow.resetCamera();
            }
        });

        final JButton box = new JButton(new AbstractAction("Reset Box") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                m_renderWindow.resetBoxWidget();
            }
        });

        final JButton boxShow = new JButton(new AbstractAction("Toggle Box") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                m_renderWindow.toggleBox();
            }
        });

        final JCheckBox boundingBox = new JCheckBox(new AbstractAction("Bounding Box") {
            @Override
            public void actionPerformed(final ActionEvent event) {
                m_renderWindow.toggleBoundingBox();
            }
        });

        final int width = (int)camera.getPreferredSize().getWidth();

        final Component glue = Box.createVerticalGlue();

        layout.setHorizontalGroup(layout.createParallelGroup().addComponent(camera, width, width, width)
                .addComponent(box, width, width, width).addComponent(boxShow, width, width, width)
                .addComponent(boundingBox, width, width, width).addComponent(glue));

        layout.setVerticalGroup(layout.createSequentialGroup().addComponent(camera)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(box)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(boxShow)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED).addComponent(boundingBox).addComponent(glue));

        return panel;
    }

    private void setUpSliceRenderer() {
        // remove the old one, if necessary
        if (m_sliceRenderer != null) {
            remove(m_sliceRenderer);
            m_sliceRenderer.delete();
        }

        m_sliceRenderer = new Viewer3DNodeSliceViewer(m_eventService);
        // TODO make this somehow automatic to adapt to the current size
        m_sliceRenderer.setPreferredSize(new Dimension(200, 10));
        m_mainPanel.add(m_sliceRenderer, BorderLayout.EAST);
    }

    private void setUpMainRenderer() {
        // remove the old one
        if (m_renderWindow != null) {
            remove(m_renderWindow);
            m_renderWindow.delete();
        }

        // set up a new one
        m_renderWindow = new Viewer3DNodeMainRenderer(m_eventService);
        m_mainPanel.add(m_renderWindow, BorderLayout.CENTER);
    }

    private JPanel setUpGeneralSettingsPanel() {

        final String[] mappers = {"Smart Mapper", "FixedPointMapper", "3D Texture Map"};
        m_listMapper = new JList(mappers);
        m_listMapper.setSelectedIndex(0);
        m_listMapper.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        m_listMapper.addListSelectionListener(new ListSelectionListener() {
            @Override
            public final void valueChanged(final ListSelectionEvent event) {
                final int selection = m_listMapper.getSelectedIndex();

                switch (selection) {
                    case 0:
                        setMapper(Viewer3DNodeVolume.Mapper.SMART);
                        break;
                    case 1:
                        setMapper(Viewer3DNodeVolume.Mapper.RAYFIXEDPOINT);
                        break;
                    case 2:
                        setMapper(Viewer3DNodeVolume.Mapper.TEXTURE3D);
                        break;
                }
            }

            private void setMapper(final Viewer3DNodeVolume.Mapper mapper) {
                m_admin.setMapper(mapper);

                for (final Viewer3DNodeVolume v : m_rendered) {
                    v.setMapper(mapper);
                }
            }
        });

        // Wrap a Border around the mapper selector
        final JPanel mapWrapper = new JPanel();
        mapWrapper.setBorder(BorderFactory.createTitledBorder("Mapper"));
        mapWrapper.add(m_listMapper);

        // put everything in the panel
        m_panelSettings = new JPanel();
        m_panelSettings.setLayout(new BoxLayout(m_panelSettings, BoxLayout.X_AXIS));
        m_panelSettings.add(mapWrapper);

        return m_panelSettings;
    }

    /**
     * Set the image admin to draw the images for rendering from.
     * 
     * @param admin the admin
     */
    public final void setAdmin(final Viewer3DNodeImageAdmin<T> admin) {

        if (admin != null) {

            // check if we need to remove the label
            if (m_admin == null) {
                removeAll();
                add(m_mainPanel, BorderLayout.CENTER);
            }

            m_admin = admin;

            // get the standard volumes
            setRenderVolume();

            m_sliceControl.setAxes(m_admin.getAxes());
        } else {
            m_admin = null;
            remove(m_mainPanel);

            final JLabel label =
                    new JLabel("Nothing to display. Probably there are only 1 or 2 Dimensions in the current image.");
            add(label, BorderLayout.CENTER);

        }
    }

    private synchronized void repaintImage() {
        m_renderWindow.render();
        m_sliceRenderer.render();
    }

    /**
     * This method should be called when the viewer goes out of focus.
     * 
     * It mainly tries to free all memory, espescially memory allocated by the c++ code. Moreover I hope that if fianlly
     * fixes the xcb_io.h prending deque bug, by really removing all access to the XServer, I think.
     */
    public final void delete() {
        m_deleted = true;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                m_renderWindow.delete();
                m_sliceRenderer.delete();

                m_screenshot.delete();

                // call the Garbage Collector to finally free everything
                final vtkReferenceInformation info = vtkObject.JAVA_OBJECT_MANAGER.gc(true);

                LOGGER.debug(info.toString());
                LOGGER.debug(info.listKeptReferenceToString());
                LOGGER.debug(info.listRemovedReferenceToString());

                m_volume = null;
                m_rendered.clear();
                m_rendered = null;

                if (m_admin != null) {
                    m_admin.delete();
                }

                m_eventService = null;

                m_southTabs.removeAll();
                m_southTabs = null;
                m_admin = null;
                m_renderWindow.removeAll();
                m_renderWindow = null;
                m_listMapper.removeAll();
                m_listMapper = null;
                m_mainPanel.removeAll();
                m_mainPanel = null;
                m_panelNorth.removeAll();
                m_panelNorth = null;
                m_panelSettings.removeAll();
                m_panelSettings = null;
                m_screenshot.removeAll();
                m_screenshot = null;
                m_sliceRenderer.removeAll();
                m_sliceControl.removeAll();
                m_sliceRenderer = null;
                m_sliceControl = null;
                m_transferControl.removeAll();
                m_transferControl = null;
                m_transferPanel.removeAll();
                m_transferPanel = null;
            }
        });

    }

    private void setMode(final Mode mode) {
        m_mode = mode;

        for (final Viewer3DNodeVolume v : m_rendered) {
            if (m_mode == Mode.GRAY) {
                v.setGrayMode();
            } else {
                v.setRGBMode();
            }
        }
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        switch (e.getID()) {
            case TransferFunctionControlPanel.ID_APPLY:
                apply();
                break;
            case TransferFunctionControlPanel.ID_NORMALIZE:
                break;
            case TransferFunctionControlPanel.ID_ONLYONE: /* ignore */
                break;
            default:
                throw new RuntimeException("not yet implemented");
        }
    }

    private void apply() {

        getCurrentBundle();

        applyTFToVolumes();

        // force rerender
        repaintImage();
    }

    private void getCurrentBundle() {

        final TransferFunctionBundle bundle = m_transferControl.getCurrentBundle();

        if ((m_mode == Mode.GRAY) && (bundle == m_volume.getBundleRGB())) {
            setMode(Mode.RGB);
        }

        if ((m_mode == Mode.RGB) && (bundle == m_volume.getBundleGray())) {
            setMode(Mode.GRAY);
        }
    }

    /**
     * Apply the currently active TF in m_volumes to all rendered Volumes if force TF is active. Otherwise only the
     * currently manipulated volume is updated.
     */
    private void applyTFToVolumes() {
        if (m_transferControl.isOnlyOneFunc()) {
            if (m_mode == Mode.GRAY) {
                final TransferFunctionBundle bundle = m_volume.getBundleGray();
                for (final Viewer3DNodeVolume v : m_rendered) {
                    if (v != m_volume) {
                        v.setBundleGray(bundle);
                    } else {
                        updateVolume(v);
                    }
                }
            } else {
                final TransferFunctionBundle bundle = m_volume.getBundleRGB();
                for (final Viewer3DNodeVolume v : m_rendered) {
                    if (v != m_volume) {
                        v.setBundleRGB(bundle);
                    } else {
                        updateVolume(v);
                    }
                }
            }
        } else {
            updateVolume(m_volume);
        }
    }

    /**
     * Update the transferfuntions of a single volume.
     * 
     * @param vol the volume to check.
     */
    private void updateVolume(final Viewer3DNodeVolume vol) {
        if (m_mode == Mode.GRAY) {
            vol.updateOpacityGray();
            vol.updateColorGray();
            vol.updateLookupTableGray();
        } else {
            vol.updateOpacityRGB();
            vol.updateColorRGB();
            vol.updateLookupTableRGB();
        }
    }

    private void setRenderVolume() {
        // skip if we are loading an image already
        if (m_loading == null) {
            m_sliceControl.setEnabled(false);
            m_loading = new LoadImages();
            m_eventService.subscribe(m_loading);
            m_loading.execute();
        }
    }

    /**
     * Called whenever the images to be rendered are changed.
     * 
     * @param e the event
     */
    @EventListener
    public final void onRenderImageChanged(final DrawChgEvent e) {
        setRenderVolume();

        if (m_initalized) {
            repaintImage();
            repaint();
        }
    }

    /**
     * Called whenever the user takes a screenshot to redraw in case the a magnification greater than 1 was chosen.
     * 
     * @param e the event
     */
    @EventListener
    public final void onScreenshot(final ScreenshotTakenEvent e) {
        repaintImage();
    }

    @EventListener
    public final void onNormalize(final NormalizationPerformedEvent e) {
        for (final Viewer3DNodeVolume v : m_rendered) {
            if (e.normalize()) {
                v.normalize();
            } else {
                v.useFullRangeForMapping();
            }
        }

        repaintImage();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.knime.knip.core.ui.event.EventServiceClient#setEventService(EventService)
     */
    @Override
    public final void setEventService(final EventService eventService) {
        if (eventService == null) {
            m_eventService = new EventService();
        } else {
            m_eventService = eventService;
        }
        m_eventService.subscribe(this);
    }

    /**
     * Get the eventService of this instance.
     * 
     * @return the eventService
     */
    public final EventService getEventService() {
        return m_eventService;
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
    public void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // not used
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#loadComponentConfiguration(ObjectInput)
     */
    @Override
    public void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // not used
    }
}
