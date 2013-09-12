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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Represents all axis information of one image.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeAxes implements Iterable<Viewer3DNodeAxis> {

    /**
     * This class represents all axial about one specific volume.
     */
    public final class Volume {
        private final List<Viewer3DNodeAxis> m_displayed;

        private final List<Viewer3DNodeAxis> m_hidden;

        private final Map<Viewer3DNodeAxis, Integer> m_map;

        private Volume(final List<Viewer3DNodeAxis> displayed, final List<Viewer3DNodeAxis> hidden, final int[] depths) {
            m_displayed = displayed;
            m_hidden = hidden;

            m_map = new HashMap<Viewer3DNodeAxis, Integer>();

            for (final Viewer3DNodeAxis a : m_displayed) {
                m_map.put(a, depths[a.getIndex()]);
            }

            for (final Viewer3DNodeAxis a : m_hidden) {
                m_map.put(a, depths[a.getIndex()]);
            }
        }

        /**
         * Make a deep copy.
         * 
         * @param vol the volume to copy.
         */
        private Volume(final Volume vol) {
            m_displayed = copyList(vol.m_displayed);
            m_hidden = copyList(vol.m_hidden);

            m_map = new HashMap<Viewer3DNodeAxis, Integer>();

            // copy the map
            for (int i = 0; i < m_displayed.size(); i++) {
                final Integer val = vol.m_map.get(vol.m_displayed.get(i));
                m_map.put(m_displayed.get(i), new Integer(val));
            }

            for (int i = 0; i < m_hidden.size(); i++) {
                final Integer val = vol.m_map.get(vol.m_hidden.get(i));
                m_map.put(m_hidden.get(i), new Integer(val));
            }
        }

        private List<Viewer3DNodeAxis> copyList(final List<Viewer3DNodeAxis> list) {
            final List<Viewer3DNodeAxis> copy = new LinkedList<Viewer3DNodeAxis>();

            for (final Viewer3DNodeAxis a : list) {
                copy.add(new Viewer3DNodeAxis(a));
            }

            return copy;
        }

        /**
         * Get the axis that are visible for this volume.
         * 
         * @return the visible axis
         */
        public List<Viewer3DNodeAxis> getDisplayed() {
            return m_displayed;
        }

        /**
         * Make a deep copy of this object.
         * 
         * @return a deep copy
         */
        public Volume deepCopy() {
            return new Volume(this);
        }

        /**
         * Get the axis that are hidden for this volume.
         * 
         * @return the hidden axis
         */
        public List<Viewer3DNodeAxis> getHidden() {
            return m_hidden;
        }

        /**
         * Get the depth info associated with one axis.
         * 
         * @param axis the axis
         * @return the dimensional depth
         */
        public int getDepth(final Viewer3DNodeAxis axis) {
            return m_map.get(axis).intValue();
        }

        /**
         * Get the depths of all axis of this volume.<br>
         * 
         * The corresponding values are at the axis.getIndex() position in the returned array. This method also adds
         * values for all the displayed axes, which is alway axis.getDisplayed()[0].
         * 
         * @return depths
         */
        public int[] getDepths() {
            final int[] depths = new int[m_map.size()];

            for (final Viewer3DNodeAxis a : m_hidden) {
                depths[a.getIndex()] = m_map.get(a);
            }

            for (final Viewer3DNodeAxis a : m_displayed) {
                depths[a.getIndex()] = a.getDisplayed()[0];
            }

            return depths;
        }

        /**
         * Get the depths of all axis of this volume.<br>
         * 
         * The corresponding values are at the axis.getIndex() position in the returned array. This method also adds
         * values for all the displayed axes, which is alway axis.getDisplayed()[0].
         * 
         * @return depths
         */
        public long[] getDepthsLong() {
            final int[] d = getDepths();
            final long[] depths = new long[d.length];

            for (int i = 0; i < d.length; i++) {
                depths[i] = d[i];
            }

            return depths;
        }

        /**
         * Get the actual string for caching for this Volume.<br>
         * 
         * @return the string for caching
         */
        public String getCacheString() {

            final StringBuilder sb = new StringBuilder();

            // first append the labels
            for (final Viewer3DNodeAxis a : m_displayed) {
                sb.append(a.getLabel());
            }

            for (final Viewer3DNodeAxis a : m_hidden) {
                sb.append(a.getLabel());
                sb.append(m_map.get(a));
            }

            return sb.toString();
        }
    }

    private static final int MINDIM = 1;

    private final List<Viewer3DNodeAxis> m_axes;

    private final List<Viewer3DNodeAxis> m_displayed;

    private final List<Viewer3DNodeAxis> m_hidden;

    private final int m_noDisplayed;

    /**
     * Get a new instance and order the axis in the standard way (xyzct).
     * 
     * @param noDisplayed no to display
     * @param axis the axis
     * @return a new Viewer3DNodeAxes instance with ordered axis
     * @see Viewer3DNodeAxes#Viewer3DNodeAxes(int, List)
     * 
     * @deprecated names of the axis are read as whatever they are
     */
    @Deprecated
    public static final Viewer3DNodeAxes newOrderedAxes(final int noDisplayed, final List<Viewer3DNodeAxis> axis) {
        // order the axis
        final List<Viewer3DNodeAxis> order = new LinkedList<Viewer3DNodeAxis>();

        nextOrder(order, axis, "x");
        nextOrder(order, axis, "y");
        nextOrder(order, axis, "z");
        nextOrder(order, axis, "channel");
        nextOrder(order, axis, "time");

        return new Viewer3DNodeAxes(noDisplayed, order);
    }

    /**
     * Append the next ordering string.
     * 
     * @param order the new axis order
     * @param axis the old axis
     * @param str search for this string
     * 
     * @deprecated names of the axis are no longer clearly defined, so there is more a "standard" ordering
     */
    @Deprecated
    private static void nextOrder(final List<Viewer3DNodeAxis> order, final List<Viewer3DNodeAxis> axis,
                                  final String str) {
        for (final Viewer3DNodeAxis a : axis) {
            if (a.getLabel().toLowerCase().equals(str)) {
                order.add(a);
            }
        }
    }

    /**
     * Create a new instance and define the ordering and number of displayed axes.<br>
     * 
     * There must be at least on axis to be displayed, otherwise either the value of noDisplayed will be reset (if
     * negative or zero) or if the list is empty.<br>
     * 
     * Moreover, the user must make sure that the noDisplayed axes that should be displayed initally are the noDisplayed
     * first elements of the passed list. All excess axes will be hidden initally.
     * 
     * @param noDisplayed the number of dimensions that should be displayed.
     * @param axes a list of all axes
     * 
     * @throws IllegalArgumentException if there are not enough axes as indicated by the noDisplayed
     * 
     */
    public Viewer3DNodeAxes(final int noDisplayed, final List<Viewer3DNodeAxis> axes) {
        // fit noMin into the bounds
        m_noDisplayed = noDisplayed >= MINDIM ? noDisplayed : MINDIM;

        // assert there are at least enough dimensions
        if (axes.size() < m_noDisplayed) {
            throw new IllegalArgumentException("Only " + Integer.toString(axes.size())
                    + " axes were passed of required " + Integer.toString(m_noDisplayed));
        }

        m_axes = axes;

        m_displayed = new ArrayList<Viewer3DNodeAxis>();
        m_hidden = new ArrayList<Viewer3DNodeAxis>();

        // add the first three axis to be displayed, while the rest stays hidden
        int count = 0;
        for (final Viewer3DNodeAxis a : m_axes) {
            if (count < m_noDisplayed) {
                m_displayed.add(a);
            } else {
                m_hidden.add(a);
            }
            count++;
        }
    }

    /**
     * Set a new axis to be displayed.<br>
     * 
     * As we can not change the amount of dimensions that should be visible for a given image, we must also specify
     * which other dimension should now be hidden.
     * 
     * @param display the new axis that should be displayed
     * @param hide one of the old axis that should now be hidden
     * 
     * @throws NullPointerException if either parameter is null
     * @throws IllegalArgumentException if display is currently visible or if hide is currently hidden, also if either
     *             is not part of the set of axis anyway
     */
    public final void displayAxis(final Viewer3DNodeAxis display, final Viewer3DNodeAxis hide) {

        // assert not null
        if ((display == null) || (hide == null)) {
            throw new NullPointerException();
        }

        if (!m_axes.contains(display) || !m_axes.contains(hide)) {
            throw new IllegalArgumentException("Either " + display.getLabel() + " or " + hide.getLabel()
                    + " is not part of this set of axis!");
        }

        // assert that both axis are in the correct lists
        if (!m_displayed.contains(hide)) {
            throw new IllegalArgumentException("The axis " + hide.getLabel() + " is currently not visible!");
        }
        if (!m_hidden.contains(display)) {
            throw new IllegalArgumentException("The axis " + display.getLabel() + " is currently not visible!");
        }

        // remove the axis
        m_displayed.remove(hide);
        m_hidden.remove(display);

        // add them to the new list
        m_displayed.add(display);
        m_hidden.add(hide);
    }

    /**
     * Get the volume that represents the currently manipulated volume.<br>
     * 
     * @return the manipulated volume
     */
    public final Volume getManipulatedVolume() {
        final int[] depths = new int[m_axes.size()];

        for (int i = 0; i < m_displayed.size(); i++) {
            depths[i] = 0;
        }

        for (int i = 0; i < m_hidden.size(); i++) {
            depths[i + m_displayed.size()] = m_hidden.get(i).getManipulated();
        }

        return new Volume(m_displayed, m_hidden, depths);
    }

    /**
     * Get a list of all currently displayed volumes.<br>
     * 
     * This is mainly thought to use the string as a hashvalue for caching the volumes.
     * 
     * @return a list of all volumes, coded in string form
     */
    public final List<String> getCacheStrings() {

        final List<String> result = new LinkedList<String>();

        final List<Volume> volumes = getDisplayedVolumes();

        for (final Volume v : volumes) {
            result.add(v.getCacheString());
        }

        return result;
    }

    /**
     * Get a list of all currently displayed volumes.
     * 
     * @return all visible volumes
     */
    public final List<Volume> getDisplayedVolumes() {
        final int[] depths = new int[m_axes.size()];

        // build all combinations
        final List<Volume> volumes = new LinkedList<Volume>();
        appendNextDim(0, depths, volumes);

        return volumes;
    }

    /**
     * Use this method to recursively append all hidden dimensions to the cache strings.
     * 
     * @param i the start position, i.e. should be 0
     * @param depths the int array for the dims, should be of same size as hidden.size() indicates
     * @param result the list to store the strings in
     */
    private void appendNextDim(final int i, final int[] depths, final List<Volume> result) {

        if (m_hidden.size() > 0) {
            final int[] displayed = m_hidden.get(i).getDisplayed();
            for (int j = 0; j < displayed.length; j++) {
                depths[m_hidden.get(i).getIndex()] = displayed[j];

                // check if there are more hidden dims
                if (i < (m_hidden.size() - 1)) {
                    appendNextDim(i + 1, depths, result);
                }

                // if this is the last dim, create the string
                if (i == (m_hidden.size() - 1)) {
                    result.add(new Volume(m_displayed, m_hidden, depths));
                }
            }
        } else {
            result.add(new Volume(m_displayed, m_hidden, depths));
        }
    }

    /**
     * Check wheter a given axis is currently displayed.<br>
     * 
     * @param axis the axis to check
     * @return true or false
     */
    public final boolean isDisplayed(final Viewer3DNodeAxis axis) {
        if (m_displayed.contains(axis)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Check wheter a given axis is currently hidden.<br>
     * 
     * @param axis the axis to check
     * @return true or false
     */
    public final boolean isHidden(final Viewer3DNodeAxis axis) {
        if (m_hidden.contains(axis)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the number of displayed dimensions.
     * 
     * @return number of dims
     */
    public final int getNoDisplayed() {
        return m_noDisplayed;
    }

    /**
     * Get a list of all displayed Axes.
     * 
     * @return displayed axes
     */
    public final List<Viewer3DNodeAxis> getDisplayed() {
        return Collections.unmodifiableList(m_displayed);
    }

    /**
     * Get a list of all hidden Axes.
     * 
     * @return hidden axes
     */
    public final List<Viewer3DNodeAxis> getHidden() {
        return Collections.unmodifiableList(m_hidden);
    }

    /**
     * Get the number of axes.
     * 
     * @return no axes
     */
    public final int getNoAxes() {
        return m_axes.size();
    }

    /**
     * Get all axis.
     * 
     * @return the axis
     */
    public final List<Viewer3DNodeAxis> getAxes() {
        return Collections.unmodifiableList(m_axes);
    }

    /**
     * {@inheritDoc}
     * 
     * @see Iterable#iterator()
     */
    @Override
    public final Iterator<Viewer3DNodeAxis> iterator() {
        return m_axes.iterator();
    }
}
