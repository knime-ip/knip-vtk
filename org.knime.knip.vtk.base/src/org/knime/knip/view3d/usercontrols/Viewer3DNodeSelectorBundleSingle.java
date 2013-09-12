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

import java.awt.Color;
import java.awt.Graphics2D;

import javax.swing.JComponent;

import org.knime.knip.core.ui.event.EventService;

/**
 * This class implements the Selector to move both Selectors independently.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSelectorBundleSingle extends Viewer3DNodeSelectorBundle {

    private final Color m_fadeOut = new Color(0.8f, 0.8f, 0.8f, 0.8f);

    /**
     * Set up a new SelectorBundleSingle, using the given EventService.
     * 
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeSelectorBundle#Viewer3DNodeSelectorBundleSingle(EventService)
     */
    public Viewer3DNodeSelectorBundleSingle(final EventService service) {
        super(service);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeSelectorBundle#moveSelector(JComponent,int)
     */
    @Override
    public final void moveSelector(final JComponent source, final int x) {

        // get the width
        final double width = source.getWidth();

        final double xFrac = x / width;

        final Viewer3DNodeSelector selected = getHighlightedSelector();
        final Viewer3DNodeSelector left = getLeftSelector();
        final Viewer3DNodeSelector right = getRightSelector();

        final int minDist = (int)(getThickness() / 2.0f);

        if (selected.getSide() == Viewer3DNodeSelector.Side.LEFT) {

            if (checkForOverlap(x, width)) {
                final double xRightFrac = (x + minDist) / width;
                right.move(xRightFrac);
            }

            left.move(xFrac);
        } else {

            if (checkForOverlap(x, width)) {
                final double xLeftFrac = (x - minDist) / width;
                left.move(xLeftFrac);
            }

            right.move(xFrac);
        }

        final double xl = left.getPos();
        final double xr = right.getPos();
        getEventService().publish(new SelectorMovedEvent(xl, xr));
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeSelectorBundle#dragDone()
     */
    @Override
    public final void dragDone() {

        final Viewer3DNodeSelector left = getLeftSelector();
        final Viewer3DNodeSelector right = getRightSelector();

        if (left.moved() || right.moved()) {
            left.setMoved(false);
            right.setMoved(false);

            final double xl = left.getPos();
            final double xr = right.getPos();
            getEventService().publish(new SelectorDragDoneEvent(xl, xr));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeSelectorBundle#paint(Graphics2D,int,int)
     */
    @Override
    public final void paint(final Graphics2D g2, final int width, final int height) {

        final Viewer3DNodeSelector left = getLeftSelector();
        final Viewer3DNodeSelector right = getRightSelector();

        // gray out the area behind the selectors
        final int posLeft = (int)(left.getPos() * width);
        final int posRight = (int)(right.getPos() * width);

        g2.setColor(m_fadeOut);
        g2.fillRect(0, 0, posLeft, height);
        g2.fillRect(posRight, 0, width, height);

        // paint the two selectors
        left.paintSelector(g2, width, height);
        right.paintSelector(g2, width, height);
    }
}
