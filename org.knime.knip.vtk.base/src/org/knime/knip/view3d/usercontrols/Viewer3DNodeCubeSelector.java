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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.view3d.image.Viewer3DNodeAxes;
import org.knime.knip.view3d.image.Viewer3DNodeAxis;

/**
 * Implementation of an abstract CubeSelector for the 3DViewer.
 * 
 * 
 * @param <T> the Type of the Element being used for actually displaying the dimension
 * @version
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public abstract class Viewer3DNodeCubeSelector<T extends JComponent> extends AbstractCubeSelector {

    /**
     * Simple wrapper to keep all info together.
     */
    protected class ViewWrapper {
        private Viewer3DNodeAxis m_axis;

        private T m_view;
    }

    /**
     * Get the current value belonging to the given view from the type of the currently used viewtype.
     * 
     * @param view the view to extract the value from
     * @return the current value
     */
    protected abstract int getCurrentValue(final T view);

    /**
     * Set this value into the given view.<br>
     * 
     * Called by the public method setCurrentValue.
     * 
     * @param view the view to put the value to
     * @param axis the axis that belongs to this view
     */
    protected abstract void setCurrentValue(final T view, final Viewer3DNodeAxis axis);

    /**
     * Overwrite this method to add the views to the cubecontrol.<br>
     * 
     * Add them using the AddElement method.
     * 
     * @param axes The axes to use
     */
    protected abstract void defineAxes(final Viewer3DNodeAxes axes);

    private final Map<Viewer3DNodeAxis, ViewWrapper> m_views = new HashMap<Viewer3DNodeAxis, ViewWrapper>();

    /**
     * Set up a new CubeSelector.
     * 
     * @param service the eventService to use
     */
    public Viewer3DNodeCubeSelector(final EventService service) {
        super(service);

        // create the layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCubeSelector#createLayout()
     */
    @Override
    protected final void createLayout() {
        // simply add all the elements, and then add a bit of spacing at the end
        defineAxes(m_axes);
        add(Box.createVerticalGlue());
    }

    /**
     * Use this method to add vieweing Element to the class.
     * 
     * The elements must provide a way to disable them using the setEnabled method. Override it if necessary
     * 
     * @param axis the axis to use for this dim
     * @param element the actual viewing element
     */
    protected final void addElement(final Viewer3DNodeAxis axis, final T element) {
        assert axis != null;
        assert element != null;

        // put everything in the layout
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
        panel.add(element);
        panel.add(super.getCheckBox(axis));

        add(panel);

        // activate the first three
        if (super.checkIfActive(axis)) {
            element.setEnabled(false);
        }

        final ViewWrapper w = new ViewWrapper();
        w.m_axis = axis;
        w.m_view = element;

        m_views.put(axis, w);
    }

    /**
     * {@inheritDoc}
     * 
     * @see AbstractCubeSelector#enableCallback(Viewer3DNodeAxis,boolean)
     */
    @Override
    protected final void enableCallback(final Viewer3DNodeAxis a, final boolean enable) {
        m_views.get(a).m_view.setEnabled(enable);
    }

    /**
     * Update the displayed values for the currently given axes.
     */
    public final void update() {
        if (m_axes == null) {
            return;
        }

        super.setUpdating(true);

        // update the displayed values
        for (final ViewWrapper w : m_views.values()) {
            setCurrentValue(w.m_view, w.m_axis);
        }

        // update the selected checkboxes
        final List<Viewer3DNodeAxis> update = new LinkedList<Viewer3DNodeAxis>();
        for (final Viewer3DNodeAxis a : m_axes.getDisplayed()) {
            if (!super.checkIfActive(a)) {
                update.add(a);
            }
        }

        for (final Viewer3DNodeAxis a : update) {
            super.deactivate(a);
        }

        super.setUpdating(false);
    }
}
