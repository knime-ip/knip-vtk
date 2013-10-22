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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.FlatIterationOrder;
import net.imglib2.Interval;
import net.imglib2.RandomAccess;
import net.imglib2.img.Img;
import net.imglib2.meta.ImgPlus;
import net.imglib2.meta.TypedAxis;
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.ops.operation.subset.views.ImgView;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ShortType;
import net.imglib2.view.Views;

import org.knime.knip.core.ui.event.EventService;

import vtk.vtkDataArray;
import vtk.vtkImageData;
import vtk.vtkTypeInt16Array;

/**
 * This class is used to transfer data from imglib2 to the vtkImageData format.
 *
 * The class stores the created vtkImages for quick access, which can be identified given the three dimensions.
 *
 *
 * @param <T>
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeImageToVTK<T extends RealType<T>> {

    private static final int MINDIMS = 3;

    private TypedAxis[] m_axes = null;

    private ImgPlus<T> m_image = null;

    private final int m_numDimensions;

    private long[] m_dimDepth = null;

    private final Map<String, Integer> m_dimensionMap;

    private final Map<String, vtkImageData> m_createdImages;

    private final EventService m_eventService;

    private boolean m_caching;

    /**
     * Set up a new converter for the given ImgPlusValue.
     *
     * @param image the image to convert
     * @param caching wheter or not to use caching
     * @param eventService the eventService to use
     * @throws Viewer3DNodeNotEnoughDimsException when there are less than 3 dimensions in the given image
     * @throws IllegalArgumentException if {@code eventService == null}
     */
    public Viewer3DNodeImageToVTK(final ImgPlus<T> image, final boolean caching, final EventService eventService)
                                                                                                                 throws Viewer3DNodeNotEnoughDimsException {

        if (eventService == null) {
            throw new IllegalArgumentException("eventService must not be null!");
        }

        m_eventService = eventService;

        // check that there are enough dimensions
        if (image.numDimensions() < MINDIMS) {
            throw new Viewer3DNodeNotEnoughDimsException();
        }

        // set up the maps
        m_dimensionMap = new HashMap<String, Integer>();
        m_createdImages = new HashMap<String, vtkImageData>();

        m_image = image;

        setCaching(caching);

        // get some info about the dimensions
        m_numDimensions = m_image.numDimensions();
        m_dimDepth = new long[m_numDimensions];
        m_image.dimensions(m_dimDepth);

        // Set up the information about the axes
        m_axes = new TypedAxis[m_numDimensions];
        for (int d = 0; d < m_axes.length; d++) {
            m_axes[d] = m_image.axis(d);
        }

        // Store the axes -> dimensions in our map
        for (final TypedAxis a : m_axes) {
            final String label = a.type().getLabel();
            final Integer i = new Integer(m_image.dimensionIndex(a.type()));

            m_dimensionMap.put(label, i);
        }

    }

    /**
     * Get vtkImageData for some dimensions.
     *
     * @param volume the volume to extract
     *
     * @return the vtkImageData
     */
    public final vtkImageData getVTKImageData(final Viewer3DNodeAxes.Volume volume) {

        if (volume == null) {
            throw new NullPointerException();
        }

        final String store = volume.getCacheString();

        vtkImageData result;

        // check if this image has already been build once
        if (m_caching && m_createdImages.containsKey(store)) {
            // pack the current labels in there
            result = m_createdImages.get(store);
        } else {

            // set up the image

            final List<Viewer3DNodeAxis> displayed = volume.getDisplayed();
            // define the axes
            final int x = m_dimensionMap.get(displayed.get(0).getLabel()).intValue();
            final int y = m_dimensionMap.get(displayed.get(1).getLabel()).intValue();
            final int z = m_dimensionMap.get(displayed.get(2).getLabel()).intValue();

            // double spacingX = 1.0;
            // double spacingY = 1.0;
            // double spacingZ = 1.0;

            final double spacingX =
                    ((m_image.averageScale(x) <= 0.0) || Double.isNaN(m_image.averageScale(x))) ? 1.0 : m_image
                            .averageScale(x);
            final double spacingY =
                    ((m_image.averageScale(y) <= 0.0) || Double.isNaN(m_image.averageScale(y))) ? 1.0 : m_image
                            .averageScale(y);
            final double spacingZ =
                    ((m_image.averageScale(z) <= 0.0) || Double.isNaN(m_image.averageScale(z))) ? 1.0 : m_image
                            .averageScale(z);

            // Set up the vtkImageData
            final vtkImageData image = new vtkImageData();
            image.SetDimensions((int)m_dimDepth[x], (int)m_dimDepth[y], (int)m_dimDepth[z]);
            image.SetOrigin(0.0, 0.0, 0.0);
            image.SetSpacing(spacingX, spacingY, spacingZ);
            image.SetScalarTypeToShort();
            image.AllocateScalars();

            image.GetPointData().SetScalars(buildArray(volume));

            // pack the image
            result = image;

            // Store the result for caching purposes
            if (m_caching) {
                m_createdImages.put(store, result);
            }
        }

        return result;
    }

    /**
     * This method builds the array that holds the image data.
     *
     * @param volume the volume to extract
     * @return the data in form of a vtkDataArray
     */
    private vtkDataArray buildArray(final Viewer3DNodeAxes.Volume volume) {

        final List<Viewer3DNodeAxis> displayed = volume.getDisplayed();
        final List<Viewer3DNodeAxis> hidden = volume.getHidden();

        // the axes of the image
        final int xDim = m_dimensionMap.get(displayed.get(0).getLabel()).intValue();
        final int yDim = m_dimensionMap.get(displayed.get(1).getLabel()).intValue();
        final int zDim = m_dimensionMap.get(displayed.get(2).getLabel()).intValue();

        // create the array to be pushed into the vtkobject
        final long numDataPoints = m_dimDepth[xDim] * m_dimDepth[yDim] * m_dimDepth[zDim];
        final short[] data = new short[(int)numDataPoints];

        // array to set position of cursor
        final long[] pos = new long[m_numDimensions];
        for (int i = 0; i < pos.length; i++) {
            pos[i] = 0;
        }

        // set the depths of the extra dimensions
        for (int i = 0; i < hidden.size(); i++) {
            final int dim = m_dimensionMap.get(hidden.get(i).getLabel()).intValue();
            pos[dim] = volume.getDepth(hidden.get(i));
        }

        // TODO: Check weather VTK viewer also works in byte type
        // use this to convert the values to something not taking too much
        // memory
        final ShortType tmp = new ShortType();
        final Convert<T, ShortType> convert =
                new Convert<T, ShortType>(m_image.firstElement().createVariable(), tmp, TypeConversionTypes.SCALE);
        int count = 0;
        final int step = (int)(m_dimDepth[zDim] * m_dimDepth[yDim] * m_dimDepth[xDim]) / 100;

        if (m_image.iterationOrder().equals(new FlatIterationOrder(m_image))) {
            final long[] max = Arrays.copyOf(pos, pos.length);
            max[xDim] = m_dimDepth[xDim] - 1;
            max[yDim] = m_dimDepth[yDim] - 1;
            max[zDim] = m_dimDepth[zDim] - 1;

            final Interval interval = new FinalInterval(pos, max);
            final Img<T> view = new ImgView<T>(Views.interval(m_image, interval), m_image.factory());

            final Cursor<T> cursor = view.localizingCursor();
            while (cursor.hasNext()) {
                cursor.fwd();
                data[count++] = convert.compute(cursor.get(), tmp).get();
                if ((count % step) == 100) {
                    m_eventService.publish(new LoadImageEvent(count / step));
                }
            }
        } else {
            final RandomAccess<T> ra = m_image.randomAccess();
            for (int z = 0; z < (int)m_dimDepth[zDim]; z++) {
                pos[zDim] = z;
                for (int y = 0; y < (int)m_dimDepth[yDim]; y++) {
                    pos[yDim] = y;
                    for (int x = 0; x < (int)m_dimDepth[xDim]; x++) {
                        pos[xDim] = x;
                        ra.setPosition(pos);
                        data[count++] = convert.compute(ra.get(), tmp).get();

                        if ((count % step) == 100) {
                            m_eventService.publish(new LoadImageEvent(count / step));
                        }
                    }
                }
            }
        }

        final vtkTypeInt16Array array = new vtkTypeInt16Array();
        array.SetJavaArray(data);

        return array;
    }

    /**
     * Determines if this instance is caching.
     *
     * @return The caching.
     */
    public final boolean isCaching() {
        return m_caching;
    }

    /**
     * Sets whether or not this instance is caching.
     *
     * @param caching The caching.
     */
    public final void setCaching(final boolean caching) {
        m_caching = caching;
    }

    /**
     * Remove all cached Images and calls the Delete() Method on all vtkObjects that are referenced by the images in the
     * cache.
     */
    public final void emptyCache() {
        for (final vtkImageData i : m_createdImages.values()) {
            i.Delete();
        }

        m_createdImages.clear();
    }

    /**
     * Get the minimal number of dims an image needs to have so that it can be handled by this class.
     *
     * @return mindims
     */
    public static final int getMinDims() {
        return MINDIMS;
    }

}
