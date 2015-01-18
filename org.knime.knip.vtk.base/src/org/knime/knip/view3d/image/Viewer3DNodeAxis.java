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
package org.knime.knip.view3d.image;

import java.util.Arrays;
import java.util.Comparator;

import net.imagej.axis.TypedAxis;


/**
 * This class contains all info about one axis of an image.
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens M�thing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeAxis implements Comparable<Viewer3DNodeAxis> {

    private class OrderByIndex implements Comparator<Viewer3DNodeAxis> {

        @Override
        public int compare(final Viewer3DNodeAxis a, final Viewer3DNodeAxis b) {
            if (a.m_index < b.m_index) {
                return -1;
            } else if (a.m_index == b.m_index) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * Get a Comparator that sorts Viewer3DNodeAxis objects by their index.<br>
     *
     * Note: this comparator imposes orderings that are inconsistent with equals
     *
     * @return a Comparator
     */
    public final Comparator<Viewer3DNodeAxis> getOrderByIndexComparator() {
        return new OrderByIndex();
    }

    private final TypedAxis m_axis;

    private final int m_index;

    private final int m_extent;

    private int[] m_displayed;

    private int m_manipulated;

    /**
     * Set up a new axis.
     *
     * @param label of this axis
     * @param extent in this dimnsion
     */
    public Viewer3DNodeAxis(final TypedAxis axis, final int extent, final int index) {
        m_axis = axis;
        m_index = index;
        m_extent = extent;
        m_displayed = new int[]{0};
        m_manipulated = 0;
    }

    /**
     * Create a deep copy of the given axis.
     *
     * @param axis the axis to copy
     */
    public Viewer3DNodeAxis(final Viewer3DNodeAxis axis) {
        m_axis = axis.m_axis;
        m_extent = axis.m_extent;
        m_index = axis.m_index;
        m_displayed = Arrays.copyOf(axis.m_displayed, axis.m_displayed.length);
        m_manipulated = axis.m_manipulated;
    }

    /**
     * Set new dims that should be rendered.
     *
     * @param val all dims to be rendered, need not be sorted, but will be
     *
     * @throws IllegalArgumentException if one of the values is negative or one of the value is larger than the extent,
     *             i.e. i is not element of [0,extent - 1]
     */
    public final void setDisplayed(final int[] val) {
        // assert that there is at least one element to be shown
        if (val.length < 1) {
            throw new IllegalArgumentException("At least one dimension must be visible!");
        }

        // assert that none of the shown values is larger than the extent
        for (final int i : val) {
            if (i >= m_extent) {
                throw new IllegalArgumentException("The value of " + Integer.toString(i)
                        + " is larger than the maximum extent " + Integer.toString(m_extent));
            }

            if (i < 0) {
                throw new IllegalArgumentException("Negative values are not allowed! " + Integer.toString(i));
            }
        }

        m_displayed = val;
        Arrays.sort(m_displayed);

        // make sure that one of the shown values is also manipulated
        boolean fits = false;
        for (final int i : m_displayed) {
            if (i == m_manipulated) {
                fits = true;
                break;
            }
        }

        if (!fits) {
            m_manipulated = m_displayed[0];
        }
    }

    /**
     * Set which dimemension should be manipulatd by the Transfer Function Viewer.
     *
     * @param val the dim to be manipulated
     *
     * @throws IllegalArgumentException if val is not one of the currently displayed dimensions
     */
    public final void setManipulated(final int val) {
        // assert that this value is one of the shown values
        boolean check = false;
        for (final int i : m_displayed) {
            if (i == val) {
                check = true;
                break;
            }
        }

        if (check) {
            m_manipulated = val;
        } else {
            String msg = "";
            for (int i = 0; i < m_displayed.length; i++) {
                msg += Integer.toString(m_displayed[i]);
                if (i != (m_displayed.length - 1)) {
                    msg += ", ";
                }
            }

            throw new IllegalArgumentException("The value " + Integer.toString(val)
                    + " is not one of the currently shown values, which are " + msg);
        }
    }

    /**
     * Get the currently manipulated dimension.
     *
     * @return manipulated dim
     */
    public final int getManipulated() {
        return m_manipulated;
    }

    /**
     * Get the currently displayed dimensions.
     *
     * @return displayed
     */
    public final int[] getDisplayed() {
        return m_displayed;
    }

    /**
     * Get the currently displayed dimensions as an array of strings.
     *
     * @return displayed
     */
    public final String[] getDisplayedAsString() {
        final String[] val = new String[m_displayed.length];

        for (int i = 0; i < m_displayed.length; i++) {
            val[i] = Integer.toString(m_displayed[i]);
        }

        return val;
    }

    /**
     * Get the extent of this axis.
     *
     * @return extent
     */
    public final int getExtent() {
        return m_extent;
    }

    /**
     * Get the label of this axis.
     *
     * @return label
     */
    public final String getLabel() {
        return m_axis.type().getLabel();
    }

    /**
     * Get the axis of this axis.
     *
     * @return axis
     */
    public final TypedAxis getAxis() {
        return m_axis;
    }

    /**
     * This only tests if the labels of the axis are the same, not if this might be a deep copy that has been changed!<br>
     *
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public final int compareTo(final Viewer3DNodeAxis axis) {
        return m_axis.type().getLabel().compareTo(axis.m_axis.type().getLabel());
    }

    /**
     * Get the index of this axis.<br>
     *
     * @return the index
     */
    public int getIndex() {
        return m_index;
    }
}
