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

import java.awt.Dimension;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.knip.core.ui.event.EventListener;
import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.view3d.image.Viewer3DNodeAxes;
import org.knime.knip.view3d.image.Viewer3DNodeAxis;

/**
 * This class bundles the different control models for the Renderer.<br>
 * 
 * It allows the user to switch between viewing just a single image or viewing multiple images and hided the differences
 * concerning the different use models to the outside.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSliceControl extends ViewerComponent {

    private final JTabbedPane m_tab;

    private final Viewer3DNodeSliceSelectorList m_list;

    private final Viewer3DNodeSliceSelectorSlider m_slider;

    private final Viewer3DNodeSubsetSelector m_subset;

    private final String m_tab1 = "Single Image";

    private final String m_tab2 = "Multiple Images";

    private Viewer3DNodeAxes m_axes;

    private EventService m_eventService;

    private final int m_maxLengthSubset = 250;

    /**
     * Set up this control.
     * 
     * @param eventService the eventService to use
     * @param axes the axes
     * 
     */
    public Viewer3DNodeSliceControl(final EventService eventService, final Viewer3DNodeAxes axes) {
        super("foo", true);

        m_tab = new JTabbedPane();

        setEventService(eventService);

        // Set up the panels
        final EventService service = new EventService();
        service.subscribe(this);

        m_list = new Viewer3DNodeSliceSelectorList(service, null);
        m_slider = new Viewer3DNodeSliceSelectorSlider(service, null);
        m_subset = new Viewer3DNodeSubsetSelector(service, null);

        final JPanel tab1 = new JPanel();
        tab1.setLayout(new BoxLayout(tab1, BoxLayout.X_AXIS));
        tab1.add(m_slider);

        final JPanel tab2 = new JPanel();
        tab2.setLayout(new BoxLayout(tab2, BoxLayout.X_AXIS));
        tab2.add(m_list);
        tab2.add(m_subset);

        m_tab.addTab(m_tab1, tab1);
        m_tab.addTab(m_tab2, tab2);

        // put everything in the layout
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        add(m_tab);

        // call setData, if keys is to short the Exception will have happened
        // already
        if (axes != null) {
            setAxes(axes);
        }

        // add the listener for changing tabs
        m_tab.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(final ChangeEvent event) {
                final String tab = m_tab.getTitleAt(m_tab.getSelectedIndex());

                if (tab.equals(m_tab1)) {
                    // in this mode only one dimension can be displayed, so lets
                    // fix that
                    for (final Viewer3DNodeAxis a : m_axes) {
                        a.setDisplayed(new int[]{a.getManipulated()});
                    }

                    m_slider.update();

                    publishCurrentSelections(m_axes);
                }

                if (tab.equals(m_tab2)) {
                    m_list.update();
                    m_subset.update();
                }
            }
        });
    }

    /**
     * Set the axes to display.
     * 
     * @param axes the axes
     */
    public final void setAxes(final Viewer3DNodeAxes axes) {

        if (axes != null) {
            m_axes = axes;

            m_slider.setAxes(axes);
            m_subset.setAxes(axes);
            m_list.setAxes(axes);

            // Stop the subset selector from claiming all available space
            final Dimension size = new Dimension(m_maxLengthSubset, (int)m_subset.getPreferredSize().getHeight());
            m_subset.setPreferredSize(size);
        }
    }

    /**
     * Called whenever the selection of the active dims changes.
     * 
     * @param axes the axes
     */
    @EventListener
    public final void onCubeSelectionChanged(final SelectionChgEvent e) {
        m_subset.update();
        publishCurrentSelections(e.getAxes());
    }

    /**
     * Publish both events using the current settings.
     */
    private void publishCurrentSelections(final Viewer3DNodeAxes axes) {
        m_eventService.publish(new DrawChgEvent(axes));
        m_eventService.publish(new ManipulateChgEvent(axes));
    }

    /**
     * Called whenver the slider is moved or the list selection changes.
     * 
     * @param axes the axes
     * @param axis the axis
     */
    @EventListener
    public final void onCubeViewerChanged(final ViewerChgEvent e) {
        publishCurrentSelections(e.getAxes());
    }

    /**
     * Called whenever the selected dims in subset change.
     * 
     * @param axes the axes
     * @param axis the axis
     */
    @EventListener
    public final void onSubsetSelectionChanged(final SelectedDimChgEvent e) {
        m_list.update();
        publishCurrentSelections(e.getAxes());
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
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#getPosition()
     */
    @Override
    public final Position getPosition() {
        // ignore
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#saveComponentConfiguration(ObjectOutput)
     */
    @Override
    public final void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // ignore
    }

    /**
     * {@inheritDoc}
     * 
     * @see ViewerComponent#loadComponentConfiguration(ObjectInput)
     */
    @Override
    public final void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // ignore
    }

    @Override
    public final void setEnabled(final boolean val) {
        m_tab.setEnabled(val);
        m_list.setEnabled(val);
        m_slider.setEnabled(val);
        m_subset.setEnabled(val);
    }
}
