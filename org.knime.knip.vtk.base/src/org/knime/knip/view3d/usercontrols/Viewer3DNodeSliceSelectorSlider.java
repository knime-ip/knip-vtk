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

import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import javax.swing.JScrollBar;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.view3d.image.Viewer3DNodeAxes;
import org.knime.knip.view3d.image.Viewer3DNodeAxis;

/**
 * This class implements a way to select three dimensions out of an arbitrary number of dimensions.
 * 
 * Moreover the user may sweep over the non active dimensions.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSliceSelectorSlider extends Viewer3DNodeCubeSelector<JScrollBar> {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = 1188656286843158061L;

    /**
     * The Adapter to plug into the ScrollBars.
     */
    private class ScrollBarAdapter implements AdjustmentListener {

        private final Viewer3DNodeAxis m_axis;

        private int m_lastValue;

        /**
         * Set up a new Adapter to bind to a specific ScrollBar.
         * 
         * @param axis the axis for this wrapper
         * @param value the current value of the ScrollBar
         */
        public ScrollBarAdapter(final Viewer3DNodeAxis axis, final int value) {
            m_lastValue = value;
            m_axis = axis;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AdjustmentListener#adjustmentValueChanged(AdjustmentEvent)
         */
        @Override
        public void adjustmentValueChanged(final AdjustmentEvent event) {

            // only issue event when value realy changed
            if ((event.getValue() != m_lastValue) && !m_settingData) {
                m_lastValue = event.getValue();
                m_axis.setDisplayed(new int[]{m_lastValue});
                m_axis.setManipulated(m_lastValue);
                getEventService().publish(new ViewerChgEvent(m_axes, m_axis));
            }
        }
    }

    private boolean m_settingData = false;

    /**
     * Set up a new CubeSelector.
     * 
     * Note: The slider will be set up to have a range from 0 to limit, so the developer has to make sure that the range
     * will be what he expects it to be.
     * 
     * @param service the eventService to use
     * @param axes the axes to use for this control
     */
    public Viewer3DNodeSliceSelectorSlider(final EventService service, final Viewer3DNodeAxes axes) {
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
            final JScrollBar bar = new JScrollBar(Adjustable.HORIZONTAL);
            bar.setMinimum(0);
            bar.setMaximum(a.getExtent());
            bar.setVisibleAmount(1);

            bar.addAdjustmentListener(new ScrollBarAdapter(a, bar.getValue()));

            addElement(a, bar);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeCubeSelector#getCurrentValue(JScrollBar)
     */
    @Override
    protected final int getCurrentValue(final JScrollBar bar) {
        return bar.getValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeCubeSelector#setCurrentValue(JScrollBar,Viewer3DNodeAxis)
     */
    @Override
    protected final void setCurrentValue(final JScrollBar view, final Viewer3DNodeAxis axis) {
        m_settingData = true;
        view.setValue(axis.getManipulated());
        m_settingData = false;
    }
}
