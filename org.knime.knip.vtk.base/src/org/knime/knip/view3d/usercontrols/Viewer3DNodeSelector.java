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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

/**
 * This class represents a selector to define a left or right boundary.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSelector {

    /**
     * The side a selector represents.
     */
    public enum Side {
        LEFT, RIGHT
    };

    private Color m_color = null;

    private Side m_side = null;

    private double m_pos = 0;

    private boolean m_selected = false;

    private boolean m_moved = false;

    private float m_thickness = 5.0f;

    /**
     * Set up a new selector on the given side.
     * 
     * The selector will be set at the position 0.0 if left is given, and 1.0 otherwise.
     * 
     * @param side the side
     */
    public Viewer3DNodeSelector(final Side side) {
        m_side = side;

        if (m_side == Side.LEFT) {
            m_color = Color.yellow;
            m_pos = 0;
        } else {
            m_color = Color.blue;
            m_pos = 1.0;
        }
    }

    /**
     * Set the thickness used for drawing the lines that represent the Selectors.
     * 
     * @param value the new value
     */
    public final void setThickness(final float value) {
        m_thickness = value;
    }

    /**
     * Paint this selector.
     * 
     * @param g2 the graphics object used for painting
     * @param width the width to use for painting
     * @param height the height to use for painting
     */
    public final void paintSelector(final Graphics2D g2, final int width, final int height) {

        // calculate the x position
        final int x = (int)(width * m_pos);

        // Update the position, in case a reisze has occured
        //m_x = (int) ((double) getWidth() * m_pos);

        // if selected, highlight
        if (m_selected) {
            g2.setStroke(new BasicStroke(m_thickness + 2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(Color.lightGray);
            g2.drawLine(x, 0, x, height);
        }

        g2.setStroke(new BasicStroke(m_thickness, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(m_color);
        g2.drawLine(x, 0, x, height);
    }

    /**
     * Move the selector to a new position and set the moved flag to true.
     * 
     * values smaller than 0.0 will be set to 0.0, larger than 1.0 to 1.0
     * 
     * @param x the new value
     */
    public final void move(final double x) {
        // set move flag
        m_moved = true;

        // Set the correct position
        m_pos = x;

        if (m_pos > 1.0) {
            m_pos = 1.0;
        }

        if (m_pos < 0.0) {
            m_pos = 0.0;
        }
    }

    /**
     * Checks wheter or not this selector has been moved.
     * 
     * @return true if moved, false otherwise
     */
    public final boolean moved() {
        return m_moved;
    }

    /**
     * Set wheter or not this selector has been moved.
     * 
     * @param value the new value
     */
    public final void setMoved(final boolean value) {
        m_moved = value;
    }

    /**
     * Get the current position of the selector.
     * 
     * @return the position
     */
    public final double getPos() {
        return m_pos;
    }

    /**
     * Set a flag to mark if this selector has been grabed by the user.
     * 
     * @param value the desired value
     */
    public final void setSelected(final boolean value) {
        m_selected = value;
    }

    /**
     * Check if the mouse is currently over the selector.
     * 
     * As this class never stores the actual int value of its position, but only a fractional value, the current width
     * has to be supplied.
     * 
     * @param xMouse the position of the mouse event
     * @param width the width to check against
     * @return true if the mouse is over the selector, false otherwise
     */
    public final boolean isMouseOver(final int xMouse, final int width) {

        // the current position
        final int x = (int)(m_pos * width);

        if (Math.abs(xMouse - x) < (int)m_thickness) {
            m_selected = true;
        } else {
            m_selected = false;
        }

        return m_selected;
    }

    /**
     * Check the side of this selector.
     * 
     * @return the side
     */
    public final Side getSide() {
        return m_side;
    }
}
