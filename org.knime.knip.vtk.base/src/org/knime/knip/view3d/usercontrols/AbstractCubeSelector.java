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

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.swing.JCheckBox;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.core.ui.imgviewer.events.ImgRedrawEvent;
import org.knime.knip.view3d.image.Viewer3DNodeAxes;
import org.knime.knip.view3d.image.Viewer3DNodeAxis;

/**
 * This class implements a way to select three dimensions out of an arbitrary number of dimensions.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public abstract class AbstractCubeSelector extends ViewerComponent {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = 3092679658956465303L;

    /**
     * The Adapter to stick into the CheckBoxEvents.
     */
    private class CheckBoxAdapter implements ActionListener {
        private final Viewer3DNodeAxis m_axis;

        /**
         * Set up a new Adapter to bind to a specific checkbox.
         * 
         * @param axis the axis
         */
        public CheckBoxAdapter(final Viewer3DNodeAxis axis) {
            m_axis = axis;
        }

        /**
         * {@inheritDoc}
         * 
         * @see ItemListener#itemStateChanged(ItemEvent)
         */
        @Override
        public void actionPerformed(final ActionEvent event) {
            if (!m_updating) {
                // display this axis and get the hidden one
                final Viewer3DNodeAxis hide = deactivate(m_axis);

                m_axes.displayAxis(m_axis, hide);
                m_eventService.publish(new SelectionChgEvent(m_axes));
                m_eventService.publish(new ImgRedrawEvent());
            }
        }
    }

    /**
     * Simple wrapper to keep all info together.
     */
    private class Wrapper {
        private JCheckBox m_box;

        private Viewer3DNodeAxis m_axis;
    }

    private final Map<Viewer3DNodeAxis, Wrapper> m_wrapper;

    private final LinkedList<Wrapper> m_active;

    protected EventService m_eventService;

    private boolean m_updating = false;

    private boolean m_enabled = true;

    /**
     * The axes instances this class uses.
     */
    protected Viewer3DNodeAxes m_axes;

    /**
     * Set up a new CubeSelector.
     * 
     * @param service the eventService to use
     */
    public AbstractCubeSelector(final EventService service) {
        super("Cube Selector", false);

        setEventService(service);

        // Set Up the Maps etc
        m_active = new LinkedList<Wrapper>();
        m_wrapper = new HashMap<Viewer3DNodeAxis, Wrapper>();
    }

    /**
     * Set the dimensions that should be displayed by this Selecotr.<br>
     * 
     * @param axes the axes to manipulate
     */
    public final void setAxes(final Viewer3DNodeAxes axes) {
        if (axes == null) {
            throw new NullPointerException();
        }

        m_updating = true;

        m_axes = axes;

        // remove the old elements, so we can this new ones
        clearElements();

        for (final Viewer3DNodeAxis a : m_axes) {
            final Wrapper w = new Wrapper();
            w.m_box = new JCheckBox(a.getLabel());
            w.m_axis = a;

            if (m_axes.isDisplayed(a)) {
                w.m_box.setEnabled(false);
                w.m_box.setSelected(true);
                m_active.add(w);
            } else {
                w.m_box.setEnabled(m_enabled);
                w.m_box.setSelected(false);
            }

            w.m_box.addActionListener(new CheckBoxAdapter(a));

            m_wrapper.put(a, w);
        }

        createLayout();

        m_updating = false;

        m_eventService.publish(new SelectionChgEvent(m_axes));
    }

    /**
     * Get the checkbox that controls this axis.<br>
     * 
     * @param axis the axis
     * 
     * @return the corresponding checkbox
     */
    protected final JCheckBox getCheckBox(final Viewer3DNodeAxis axis) {
        return m_wrapper.get(axis).m_box;
    }

    /**
     * Check if the given axis is currently active, i.e. grayed out.<br>
     * 
     * @param axis the axis to check
     * 
     * @return if the given axis is currently grayed out
     */
    protected final boolean checkIfActive(final Viewer3DNodeAxis axis) {
        for (final Wrapper w : m_active) {
            if (w.m_axis == axis) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get an array of all currently active axes.<br>
     * 
     * @return the active axes.
     */
    protected final Viewer3DNodeAxis[] getActive() {
        final Viewer3DNodeAxis[] axes = new Viewer3DNodeAxis[m_active.size()];

        for (int i = 0; i < axes.length; i++) {
            axes[i] = m_active.get(i).m_axis;
        }

        return axes;
    }

    /**
     * Get an array of all currently inactive axes.<br>
     * 
     * @return all inactive axes
     */
    protected final Viewer3DNodeAxis[] getInactive() {
        final Viewer3DNodeAxis[] axes = new Viewer3DNodeAxis[m_wrapper.size() - m_active.size()];

        int i = 0;
        for (final Wrapper w : m_wrapper.values()) {
            if (!m_active.contains(w)) {
                axes[i++] = w.m_axis;
            }
        }

        return axes;
    }

    /**
     * Callback method, overwrite this to create the layout of the implementation.
     */
    protected abstract void createLayout();

    /**
     * Deactivate (gray out) the given key.
     * 
     * @param axis the axis to gray out
     * 
     * @return the inturn activated axis
     */
    protected Viewer3DNodeAxis deactivate(final Viewer3DNodeAxis axis) {

        if (m_wrapper.containsKey(axis)) {
            // only do this if the key has not been deactivated already
            if (!m_active.contains(m_wrapper.get(axis))) {

                // pop the last active Wrapper and reactivate it
                final Wrapper old = m_active.poll();
                changeStatus(old, true);

                // Deactivate the new wrapper and put it in the queue
                final Wrapper now = m_wrapper.get(axis);
                changeStatus(now, false);

                m_active.add(now);

                return old.m_axis;
            }
        } else {
            throw new IllegalArgumentException("Axis " + axis.getLabel() + " is not part of this set of axes");
        }

        // compiler complains otherwise
        return null;
    }

    /**
     * Callback method called everytime one the checkboxes is enabled or disabled.<br>
     * 
     * @param axis the axis that was enabled / disabled
     * @param enable the new value
     */
    protected abstract void enableCallback(final Viewer3DNodeAxis axis, final boolean enable);

    /**
     * Change the status (i.e., if grayed out) of one axis.
     * 
     * @param w the wrapper
     * @param display ungray = true
     */
    private void changeStatus(final Wrapper w, final boolean enable) {
        w.m_box.setEnabled(enable);
        w.m_box.setSelected(!enable);

        enableCallback(w.m_axis, enable);
    }

    /**
     * Use this to remove everything from this controls panel.
     */
    protected void clearElements() {
        m_active.clear();
        m_wrapper.clear();
        removeAll();
    }

    /**
     * Activate the updating flag.<br>
     * 
     * This will prevent the checkboxes from being clickable.
     * 
     * @param val the value to set to
     */
    protected final void setUpdating(final boolean val) {
        m_updating = val;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.knime.knip.core.ui.event.EventServiceClient#setEventService(EventService)
     */
    @Override
    public void setEventService(final EventService eventService) {
        if (eventService != null) {
            m_eventService = eventService;
        } else {
            m_eventService = new EventService();
        }

        m_eventService.subscribe(this);
    }

    /**
     * Get the eventService of this instance.<br>
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
        return Position.SOUTH;
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

    @Override
    public void setEnabled(final boolean val) {

        m_enabled = val;

        for (final Wrapper w : m_wrapper.values()) {
            if (!m_active.contains(w)) {
                w.m_box.setEnabled(val);
                enableCallback(w.m_axis, val);
            }
        }
    }
}
