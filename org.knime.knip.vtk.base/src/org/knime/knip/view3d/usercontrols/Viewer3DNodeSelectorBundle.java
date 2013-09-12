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

import java.awt.Graphics2D;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.EventObject;

import javax.swing.JComponent;

import org.knime.knip.core.ui.event.EventService;

/**
 * This class bundles to selectors.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public abstract class Viewer3DNodeSelectorBundle {

    /**
     * Bundles all methods used to mess around with the selectors.
     */
    private class SelectorMouseAdapter extends MouseAdapter {

        /**
         * Check if the mouse exited the panel, and if yes unset any selections.
         * 
         * {@inheritDoc}
         * 
         * @see MouseListener#mouseExited(MouseEvent)
         */
        @Override
        public final void mouseExited(final MouseEvent event) {
            // if we are leaving and not dragging
            if (!m_dragging) {
                resetSelection();
            }
        }

        /**
         * This method is used to check if a dragging operation has ended, and if so to publish an event about the new
         * position.
         * 
         * {@inheritDoc}
         * 
         * @see MouseListener#mouseReleased(MouseEvent)
         */
        @Override
        public final void mouseReleased(final MouseEvent event) {
            m_dragging = false;
            dragDone();
        }

        /**
         * This method is used to move the Selectors.
         * 
         * Note: the event source MUST be an instance of JComponent, otherwise nothing will happen.
         * 
         * {@inheritDoc}
         * 
         * @see MouseMotionListener#mouseDragged(MouseEvent)
         */
        @Override
        public final void mouseDragged(final MouseEvent event) {

            // Only change anything if something is selected
            if (m_selected != null) {

                final JComponent source = getEventSource(event);

                // If source is invalid just abort
                if (source != null) {
                    m_dragging = true;
                    moveSelector(source, event.getX());
                }
            }
        }

        /**
         * This method is used to find the currently hightlighted element.
         * 
         * Note: the event source MUST be an instance of JComponent, otherwise nothing will happen
         * 
         * {@inheritDoc}
         * 
         * @see MouseMotionListener#mouseMoved(MouseEvent)
         */
        @Override
        public final void mouseMoved(final MouseEvent event) {

            final JComponent source = getEventSource(event);

            if (source != null) {
                final int width = source.getWidth();

                // above left selector
                if (m_selectorLeft.isMouseOver(event.getX(), width)) {

                    if (m_selected == null) {
                        m_selected = m_selectorLeft;
                        m_eventService.publish(new SelectorHilitedEvent());
                    }
                } else {
                    // above right selector
                    if (m_selectorRight.isMouseOver(event.getX(), width)) {

                        if (m_selected == null) {
                            m_selected = m_selectorRight;
                            m_eventService.publish(new SelectorHilitedEvent());
                        }
                    } else { // and all other case

                        if (m_selected != null) {
                            resetSelection();
                        }
                    }
                }
            }
        }

        /**
         * Use this method to check if the source of the event was a JComponent and therefore has a getWidth() and
         * getHeight() Method.
         * 
         * @param event the event to check
         * @return the JComponent from which this event originates, otherwise null
         */
        private JComponent getEventSource(final EventObject event) {
            // Get the source
            JComponent source;

            if (event.getSource() instanceof JComponent) {
                source = (JComponent)event.getSource();
            } else {
                source = null;
            }

            return source;
        }
    }

    private Viewer3DNodeSelector m_selectorLeft = null;

    private Viewer3DNodeSelector m_selectorRight = null;

    private Viewer3DNodeSelector m_selected = null;

    private boolean m_dragging = false;

    private final SelectorMouseAdapter m_adapter;

    private EventService m_eventService = null;

    private final float m_thickness = 5.0f;

    /**
     * Set up a new SelectorBundle in SINGLE Mode.
     * 
     * @param eventService the eventService used to publish
     */
    public Viewer3DNodeSelectorBundle(final EventService eventService) {
        setEventService(eventService);

        // Set up the two selectors
        m_selectorLeft = new Viewer3DNodeSelector(Viewer3DNodeSelector.Side.LEFT);
        m_selectorRight = new Viewer3DNodeSelector(Viewer3DNodeSelector.Side.RIGHT);

        // Set the size to use for drawing
        m_selectorLeft.setThickness(m_thickness);
        m_selectorRight.setThickness(m_thickness);

        // and set up the adapter
        m_adapter = new SelectorMouseAdapter();
    }

    /**
     * Set a new eventService used for publishing.
     * 
     * @param eventService the eventService.
     */
    public final void setEventService(final EventService eventService) {
        m_eventService = eventService;
    }

    /**
     * Perform the painting operation.
     * 
     * @param g2 the Graphics2D object to use
     * @param width the width to use for painting
     * @param height the height to use for painting
     */
    public abstract void paint(final Graphics2D g2, final int width, final int height);

    /**
     * Convenience method to reset all selections at once.
     */
    private void resetSelection() {
        m_selected = null;
        m_selectorLeft.setSelected(false);
        m_selectorRight.setSelected(false);

        m_eventService.publish(new SelectorHiliteResetEvent());
    }

    /**
     * Gets the MouseAdapter for this instance.
     * 
     * @return The adapter.
     */
    public final SelectorMouseAdapter getMouseAdapter() {
        return m_adapter;
    }

    /**
     * Called when the MouseAdapter detectes that one of the Selectors should be moved.
     * 
     * @param source the souce of the event
     * @param x the x coordiante of the event, relative to the source
     */
    public abstract void moveSelector(final JComponent source, final int x);

    /**
     * Called when the MouseAdapter detectes that a drag ends.
     */
    public void dragDone() {
    };

    /**
     * Get the left selector.
     * 
     * @return the left selector
     */
    protected final Viewer3DNodeSelector getLeftSelector() {
        return m_selectorLeft;
    }

    /**
     * Get the right selector.
     * 
     * @return the right selector
     */
    protected final Viewer3DNodeSelector getRightSelector() {
        return m_selectorRight;
    }

    /**
     * Get the currently highlighted selector.
     * 
     * @return the highlightes selector
     */
    protected final Viewer3DNodeSelector getHighlightedSelector() {
        return m_selected;
    }

    /**
     * Get the EventService.
     * 
     * @return The event service
     */
    protected final EventService getEventService() {
        return m_eventService;
    }

    /**
     * Convenience method to quickly check if the two selectors will overlap.
     * 
     * @param x the x position of the selector after the move
     * @param width the width to use for calculating
     * @return true if an overlap will occur, false otherwise
     */
    protected final boolean checkForOverlap(final int x, final double width) {

        final int minDist = (int)(m_thickness / 2.0f);
        final double xFrac = x / width;

        boolean overlap = false;

        // check if selectors will overlap
        final int xRight = (int)(m_selectorRight.getPos() * width);
        final int xLeft = (int)(m_selectorLeft.getPos() * width);

        if (m_selected.getSide() == Viewer3DNodeSelector.Side.LEFT) {
            if ((Math.abs(xRight - x) <= minDist) || (m_selectorRight.getPos() < xFrac)) {
                overlap = true;
            }
        } else {
            if ((Math.abs(xLeft - x) <= minDist) || (m_selectorLeft.getPos() > xFrac)) {
                overlap = true;
            }
        }

        return overlap;
    }

    /**
     * Get the thickness used to draw the Selectors.
     * 
     * @return the thickness
     */
    public final float getThickness() {
        return m_thickness;
    }
}
