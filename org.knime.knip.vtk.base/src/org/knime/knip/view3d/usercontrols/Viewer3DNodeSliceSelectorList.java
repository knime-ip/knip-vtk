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

import javax.swing.JList;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.view3d.image.Viewer3DNodeAxes;
import org.knime.knip.view3d.image.Viewer3DNodeAxis;

/**
 * This class implements a way to select three dimensions out of an arbitrary number of dimensions, using JLists.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSliceSelectorList extends Viewer3DNodeCubeSelector<Viewer3DNodeListPane> {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = -7871090868590803499L;

    /**
     * The Adapter for the JLists.
     */
    private class ListSelectionAdapter implements ListSelectionListener {

        private final Viewer3DNodeAxis m_axis;

        public ListSelectionAdapter(final Viewer3DNodeAxis axis) {
            m_axis = axis;
        }

        /**
         * {@inheritDoc}
         * 
         * @see ListSelectionListener#valueChanged(ListSelectionEvent)
         */
        @Override
        public void valueChanged(final ListSelectionEvent event) {

            if (!event.getValueIsAdjusting() && !m_settingData) {
                final JList source = (JList)event.getSource();

                if (source.getSelectedIndex() >= 0) {
                    m_axis.setManipulated(Integer.parseInt((String)source.getSelectedValue()));
                    getEventService().publish(new ViewerChgEvent(m_axes, m_axis));
                }
            }

        }
    }

    private boolean m_settingData = false;

    /**
     * Set up a new SliceSelector using Lists.
     * 
     * Note: The slider will be set up to have a range from 0 to limit, so the developer has to make sure that the range
     * will be what he expects it to be.
     * 
     * @param service the eventService to use
     * @param axes the instance of the axes to control
     */
    public Viewer3DNodeSliceSelectorList(final EventService service, final Viewer3DNodeAxes axes) {
        super(service);

        if (axes != null) {
            setAxes(axes);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeCubeSelector#defineAxes(Viewer3DNodeAxes)
     */
    @Override
    protected final void defineAxes(final Viewer3DNodeAxes axes) {

        for (final Viewer3DNodeAxis a : axes.getAxes()) {
            final Viewer3DNodeListPane pane = new Viewer3DNodeListPane(a.getDisplayedAsString());
            pane.singleSelection();

            addElement(a, pane);

            pane.addListSelectionListener(new ListSelectionAdapter(a));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeCubeSelector#getCurrentValue(Viewer3DNodeListPane)
     */
    @Override
    protected final int getCurrentValue(final Viewer3DNodeListPane pane) {
        return pane.getSelectedIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeCubeSelector#setCurrentValue(Viewer3DNodeListPane, Viewer3DNodeAxis)
     */
    @Override
    protected final void setCurrentValue(final Viewer3DNodeListPane pane, final Viewer3DNodeAxis axis) {
        m_settingData = true;
        final int selected = Integer.parseInt((String)pane.getSelectedValue());

        pane.setData(axis.getDisplayedAsString());

        // make sure the same index is still selected
        final int[] displayed = axis.getDisplayed();
        for (int i = 0; i < displayed.length; i++) {
            if (displayed[i] == selected) {
                pane.setSelectedIndex(i);
                break;
            }
        }

        m_settingData = false;
    }
}
