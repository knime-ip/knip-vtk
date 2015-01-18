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

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;

import net.imagej.ImgPlus;
import net.imagej.axis.CalibratedAxis;
import net.imagej.axis.TypedAxis;
import net.imglib2.type.numeric.RealType;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.panels.transfunc.TransferFunctionBundle;

import vtk.vtkImageData;

/**
 * This class handles all administrative work to map between imglib2 images and vtkImages.<br>
 *
 * Basically the method returns a Viewer3DNodeVolume that is associated with some dimensional settings. Moreover it uses
 * caching to allow for quick access if one is constantly swapping around the image to view.<br>
 *
 * Note that the caching assumes that images are the same, independent of the actual ordering of the dimensions. So e.g.
 * an image with dimensions XYZ is considered to be the same as ZXY.
 *
 *
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeImageAdmin<T extends RealType<T>> {

    private LinkedHashMap<String, Viewer3DNodeVolume> m_cache;

    private Viewer3DNodeVolume m_current = null;

    private Viewer3DNodeImageToVTK<T> m_converter;

    private boolean m_caching;

    private Viewer3DNodeVolume.Mapper m_mapper;

    private Viewer3DNodeAxes m_axes;

    private final EventService m_eventService;

    /**
     * Set up a new instance to manage one image.
     *
     * @param image the image to administrate
     * @param eventService the eventService to use
     *
     * @throws Viewer3DNodeNotEnoughDimsException if there are less than 3 dims
     * @throws IllegalArgumentException if {@code eventService == null}
     *
     * @see Viewer3DNodeImageToVTK#Viewer3DNodeImageToVTK
     */
    public Viewer3DNodeImageAdmin(final ImgPlus<T> image, final EventService eventService)
                                                                                          throws Viewer3DNodeNotEnoughDimsException {

        if (eventService == null) {
            throw new IllegalArgumentException("eventService must not be null!");
        }

        m_eventService = eventService;

        m_cache = new LinkedHashMap<String, Viewer3DNodeVolume>();
        m_caching = true;
        m_mapper = Viewer3DNodeVolume.Mapper.SMART;

        // Set up the image converter
        m_converter = new Viewer3DNodeImageToVTK<T>(image, false, m_eventService);

        setUpAxes(image);
    }

    private void setUpAxes(final ImgPlus<T> image) {
        final CalibratedAxis[] axes = new CalibratedAxis[image.numDimensions()];
        image.axes(axes);

        final List<Viewer3DNodeAxis> axesList = new LinkedList<Viewer3DNodeAxis>();

        for (final TypedAxis a : axes) {
            final long extent = image.dimension(image.dimensionIndex(a.type()));
            axesList.add(new Viewer3DNodeAxis(a, (int)extent, image.dimensionIndex(a.type())));
        }

        m_axes = new Viewer3DNodeAxes(Viewer3DNodeImageToVTK.getMinDims(), axesList);
    }

    /**
     * Get the axes instance used by this admin.
     *
     * @return the axes
     */
    public final Viewer3DNodeAxes getAxes() {
        return m_axes;
    }

    /**
     * Get the volume that corresponds to the given dimensions.
     *
     * @param volume the volume to get
     * @return the corresponding volume
     */
    public final Viewer3DNodeVolume getVolume(final Viewer3DNodeAxes.Volume volume) {

        Viewer3DNodeVolume vol;

        final String key = volume.getCacheString();

        if (m_caching && m_cache.containsKey(key)) {
            vol = m_cache.get(key);
        } else {
            // create the new volume
            final vtkImageData vtkImg = m_converter.getVTKImageData(volume);

            // copy the current settings if possible
            if (m_current != null) {
                final TransferFunctionBundle gray = new TransferFunctionBundle(m_current.getBundleGray());
                final TransferFunctionBundle rgb = new TransferFunctionBundle(m_current.getBundleRGB());
                vol = new Viewer3DNodeVolume(vtkImg, volume, gray, rgb);
            } else {
                vol = new Viewer3DNodeVolume(vtkImg, volume);
            }

            if (m_caching) {
                m_cache.put(key, vol);
            }
        }

        m_current = vol;
        vol.setMapper(m_mapper);
        return vol;
    }

    /**
     * Get all currently displayed volumes as listed by the owned axes instance of this instance.
     *
     * @return a list of all volumes
     */
    public final List<Viewer3DNodeVolume> getVolumes() {
        final List<Viewer3DNodeVolume> volumes = new LinkedList<Viewer3DNodeVolume>();
        for (final Viewer3DNodeAxes.Volume v : m_axes.getDisplayedVolumes()) {
            volumes.add(getVolume(v));
        }

        return volumes;
    }

    /**
     * Set the mapper to use in the next volume.
     *
     * @param mapper the mapper to use
     */
    public final void setMapper(final Viewer3DNodeVolume.Mapper mapper) {
        m_mapper = mapper;
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
     * Gets the current volume for this instance.
     *
     * @return The current volume.
     */
    public final Viewer3DNodeVolume getCurrent() {
        return m_current;
    }

    /**
     * If this method is called, all images cached by this instance will be deleted by a call to the
     * vtkImageData.Delete() method.<br>
     *
     * This means that the memory of all images will be freed, regardless if one of the images is currently being
     * rendered!
     */
    public final void delete() {
        if (m_cache != null) {
            for (final Viewer3DNodeVolume v : m_cache.values()) {
                v.delete(false);
            }
            m_cache.clear();
        }

        m_converter = null;
        m_current = null;

        m_cache = null;
        m_mapper = null;
        m_axes = null;
    }
}
