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
package org.knime.knip.view3d.render;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.view3d.image.Viewer3DNodeVolume;

import vtk.vtkInteractorStyle;
import vtk.vtkInteractorStyleImage;
import vtk.vtkProp;

/**
 * This renderer is designed to display slices from a value.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public final class Viewer3DNodeSliceRenderer extends Viewer3DNodeRenderer {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = 3491513824665725794L;

    /**
     * The three kinds of slices we can display so far.
     */
    public enum Kind {
        /**
         * Display sagittal images.
         */
        SAGITTAL,
        /**
         * Display axial images.
         */
        AXIAL,
        /**
         * Display coronal images.
         */
        CORONAL
    };

    // The stlye for this image, use to add Observers
    private vtkInteractorStyle m_style = null;

    private Kind m_kind;

    private List<Viewer3DNodeVolume> m_volumes = null;

    private JLabel m_info = null;

    private int[] m_extent = null;

    private boolean m_rightButtonDown = false;

    /**
     * Set up a new Renderer that is bound to render one of the kinds of slices.
     * 
     * @param es the eventservice
     * @param kind the kind to render
     * @param message the message to display on the border
     */
    private Viewer3DNodeSliceRenderer(final EventService es, final Kind kind, final String message) {
        super(es, message, false);

        m_info = new JLabel();
        add(m_info, BorderLayout.SOUTH);

        m_style = new vtkInteractorStyleImage();
        setInteractorStyle(m_style);

        m_kind = kind;

        m_style.AddObserver("MouseMoveEvent", this, "mouseMoveCallback");
        m_style.AddObserver("MouseWheelForwardEvent", this, "mouseWheelForwardCallback");
        m_style.AddObserver("MouseWheelBackwardEvent", this, "mouseWheelBackwardCallback");
        m_style.AddObserver("RightButtonPressEvent", this, "rightButtonPressCallback");
        m_style.AddObserver("RightButtonReleaseEvent", this, "rightButtonReleaseCallback");
    }

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeRenderer#deleteAdditional()
     */
    @Override
    protected void deleteAdditional() {
        m_style.RemoveAllObservers();
        m_style.Delete();
        m_style = null;
        m_kind = null;
        m_info = null;
        if (m_volumes != null) {
            m_volumes.clear();
            m_volumes = null;
        }
        m_eventService = null;

    }

    /**
     * Set the images that should be displayed.<br>
     * 
     * The Renderer will automatically choose the right image to display, based on its own kind.
     * 
     * @param volumes The volumes from which to read the images
     */
    @Override
    protected final List<vtkProp> addViewProps(final List<Viewer3DNodeVolume> volumes) {
        final List<vtkProp> result = new LinkedList<vtkProp>();

        m_volumes = volumes;

        if (m_volumes.size() > 0) {
            // just take any, they are from the same orientation anyway
            final Viewer3DNodeVolume v = m_volumes.get(0);
            m_extent = v.getExtent();
            int slice = 0;
            int max = 0;

            for (final Viewer3DNodeVolume vol : m_volumes) {
                switch (m_kind) {
                    case AXIAL:
                        result.add(vol.getImageActorAxial());
                        slice = vol.getCurrentSliceAxial();
                        max = m_extent[5];
                        break;
                    case CORONAL:
                        result.add(vol.getImageActorCoronal());
                        slice = vol.getCurrentSliceCoronal();
                        max = m_extent[3];
                        break;
                    case SAGITTAL:
                        result.add(vol.getImageActorSagittal());
                        slice = vol.getCurrentSliceSagittal();
                        max = m_extent[1];
                        break;
                }
            }

            updateLabel(slice, max);
        }

        return result;
    }

    private void updateLabel(final int slice, final int max) {
        // save the current size, otherwise the Renderer will for some obscure
        // reasen need more size after chaning the text
        final Dimension dim = getPreferredSize();
        m_info.setText("Slice " + Integer.toString(slice) + "/" + Integer.toString(max));
        setPreferredSize(dim);
    }

    /**
     * Create a new Renderer to display a sagittal image.
     * 
     * @param es the eventservice
     * @return the new renderer, set to sagittal
     */
    public static Viewer3DNodeSliceRenderer newSagittalView(final EventService es) {
        final Viewer3DNodeSliceRenderer renderer = new Viewer3DNodeSliceRenderer(es, Kind.SAGITTAL, "Sagittal View");
        return renderer;
    }

    /**
     * Create a new Renderer to display a axial image.
     * 
     * @param es the eventservice
     * @return the new renderer, set to axial
     */
    public static Viewer3DNodeSliceRenderer newAxialView(final EventService es) {
        final Viewer3DNodeSliceRenderer renderer = new Viewer3DNodeSliceRenderer(es, Kind.AXIAL, "Axial View");
        return renderer;
    }

    /**
     * Create a new Renderer to display a coronal image.
     * 
     * @param es the eventservice
     * @return the new renderer, set to coronal
     */
    public static Viewer3DNodeSliceRenderer newCoronalView(final EventService es) {
        final Viewer3DNodeSliceRenderer renderer = new Viewer3DNodeSliceRenderer(es, Kind.CORONAL, "Coronal View");
        return renderer;
    }

    /**
     * Callback for the vtkInteractor.
     */
    public void mouseMoveCallback() {
        if (m_rightButtonDown) {
            final int[] last = getRenderWindowInteractor().GetLastEventPosition();
            final int[] now = getRenderWindowInteractor().GetEventPosition();

            moveImage(now[1] - last[1]);
        }
    }

    /**
     * Callback for the vtkInteractor.
     */
    public void rightButtonPressCallback() {
        m_rightButtonDown = true;
    }

    /**
     * Callback for the vtkInteractor.
     */
    public void rightButtonReleaseCallback() {
        m_rightButtonDown = false;
    }

    /**
     * Callback for the vtkInteractor.
     */
    public void mouseWheelForwardCallback() {
        moveImage(1);
    }

    /**
     * Callback for the vtkInteractor.
     */
    public void mouseWheelBackwardCallback() {
        moveImage(-1);
    }

    /**
     * Move the images this renderer displays n slices.
     * 
     * @param numSlices how many slices to move
     */
    private void moveImage(final int numSlices) {

        int slice = 0;
        int max = 0;

        for (final Viewer3DNodeVolume vol : m_volumes) {
            switch (m_kind) {
                case AXIAL:
                    vol.moveImageAxial(numSlices);
                    slice = vol.getCurrentSliceAxial();
                    max = m_extent[5];
                    break;
                case CORONAL:
                    vol.moveImageCoronal(numSlices);
                    slice = vol.getCurrentSliceCoronal();
                    max = m_extent[3];
                    break;
                case SAGITTAL:
                    vol.moveImageSagittal(numSlices);
                    slice = vol.getCurrentSliceSagittal();
                    max = m_extent[1];
                    break;
            }
        }

        updateLabel(slice, max);
        render();
    }

    /**
     * Get the kind of this renderer.
     * 
     * @return the kind
     */
    public Kind getKind() {
        return m_kind;
    }

    /**
     * Set a new kind for this renderer.<br>
     * 
     * After setting the kind, the renderer will remove all displayed images and read the correct images of the new
     * style from the same source volume as the old kind.
     * 
     * @param kind the new kind
     */
    public void setKind(final Kind kind) {
        m_kind = kind;

        setVolumes(m_volumes);
    }
}
