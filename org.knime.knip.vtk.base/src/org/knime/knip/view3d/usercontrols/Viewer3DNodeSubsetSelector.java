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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.view3d.image.Viewer3DNodeAxes;
import org.knime.knip.view3d.image.Viewer3DNodeAxis;

/**
 * This class allows the user to select subsets from all dimensions.<br>
 * 
 * Note: This class does not track which dimensions are currently active, so the program has to sync these with the
 * cubeselector
 * 
 * @version
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSubsetSelector extends ViewerComponent {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = 7099245274105584552L;

    /**
     * The Adapter for all the Selectors.
     */
    private class SelectionAdapter implements ListSelectionListener {
        private final Viewer3DNodeAxis m_axis;

        public SelectionAdapter(final Viewer3DNodeAxis axis) {
            m_axis = axis;
        }

        /**
         * {@inheritDoc}
         * 
         * @see ListSelectionListener#valueChanged(ListSelectionEvent)
         */
        @Override
        public void valueChanged(final ListSelectionEvent event) {

            if (!event.getValueIsAdjusting()) {
                final Wrapper w = m_selectors.get(m_axis);
                final int[] selected = w.m_pane.getSelectedIndices();

                if (selected.length > 0) {
                    m_axis.setDisplayed(selected);
                    m_eventService.publish(new SelectedDimChgEvent(m_axes, m_axis));
                }
            }

        }

    }

    /**
     * A simple wrapper for all the elements of one dimension.
     */
    private class Wrapper extends JPanel {

        /**
         * Eclipse generated.
         */
        private static final long serialVersionUID = 7748170066855377362L;

        private final Viewer3DNodeListPane m_pane = new Viewer3DNodeListPane(new String[]{});

        private final JLabel m_labelSwing = new JLabel();

        private Viewer3DNodeAxis m_axis;

        private SelectionAdapter m_adapter;

        public Wrapper(final Viewer3DNodeAxis axis) {
            setAxis(axis);
        }

        /**
         * Update the displayed information to reflect the current situation.
         */
        public void update() {
            final String[] data = new String[m_axis.getExtent()];
            for (int i = 0; i < data.length; i++) {
                data[i] = Integer.toString(i);
            }

            m_pane.setData(data);
            m_pane.setSelectedIndices(m_axis.getDisplayed());
        }

        /**
         * Set a new axis to be displayed.
         * 
         * @param axis the axis
         */
        public void setAxis(final Viewer3DNodeAxis axis) {
            m_axis = axis;
            m_labelSwing.setText(axis.getLabel());

            update();
        }
    }

    private Map<Viewer3DNodeAxis, Wrapper> m_selectors;

    private Viewer3DNodeAxes m_axes;

    private EventService m_eventService;

    /**
     * Set up a new Selector.
     * 
     * @param eventService the event service to use
     * @param axes the axes to display
     * 
     */
    public Viewer3DNodeSubsetSelector(final EventService eventService, final Viewer3DNodeAxes axes) {
        super("Subsets to display", false);

        setEventService(eventService);

        if (axes != null) {
            setAxes(axes);
        }
    }

    /**
     * Set the axes to be displayed.
     * 
     * @param axes the axes
     */
    public final void setAxes(final Viewer3DNodeAxes axes) {

        if (axes != null) {
            removeAll();

            m_axes = axes;
            m_selectors = new HashMap<Viewer3DNodeAxis, Wrapper>();

            // create the layout
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

            for (final Viewer3DNodeAxis a : axes) {
                final Wrapper w = new Wrapper(a);

                // set up the layout
                w.setLayout(new BoxLayout(w, BoxLayout.X_AXIS));
                w.add(w.m_labelSwing);
                w.add(Box.createHorizontalStrut(5));
                w.add(w.m_pane);

                // add a listener
                w.m_adapter = new SelectionAdapter(a);
                w.m_pane.addListSelectionListener(w.m_adapter);

                // add it to the control
                add(w);

                m_selectors.put(a, w);
            }

            add(Box.createVerticalGlue());

            update();
        }
    }

    /**
     * Update the info displayed.
     */
    public final void update() {

        if (m_selectors != null) {
            for (final Wrapper w : m_selectors.values()) {
                w.m_pane.removeListSelectionListener(w.m_adapter);
                w.update();
                w.m_pane.addListSelectionListener(w.m_adapter);
            }

            for (final Viewer3DNodeAxis a : m_axes.getDisplayed()) {
                m_selectors.get(a).m_pane.setEnabled(false);
            }

            for (final Viewer3DNodeAxis a : m_axes.getHidden()) {
                m_selectors.get(a).m_pane.setEnabled(true);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.knime.knip.core.ui.event.EventServiceClient#setEventService(EventService)
     */
    @Override
    public final void setEventService(final EventService eventService) {
        if (eventService == null) {
            throw new NullPointerException();
        }

        m_eventService = eventService;
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

}
