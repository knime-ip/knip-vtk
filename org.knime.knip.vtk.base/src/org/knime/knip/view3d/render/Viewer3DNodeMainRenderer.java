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

import java.util.LinkedList;
import java.util.List;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.view3d.image.Viewer3DNodeVolume;

import vtk.vtkBoxWidget;
import vtk.vtkPlanes;
import vtk.vtkProp;

/**
 * This class contains the main render window, doing the volume rendering.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeMainRenderer extends Viewer3DNodeRenderer {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = -2630583151409224275L;

    private vtkBoxWidget m_box;

    private boolean m_first = true;

    private boolean m_boundingBox = false;

    private List<Viewer3DNodeVolume> m_volumes = new LinkedList<Viewer3DNodeVolume>();

    /**
     * {@inheritDoc}
     * 
     * @see Viewer3DNodeRenderer#Viewer3DNodeMainRenderer(EventService)
     */
    public Viewer3DNodeMainRenderer(final EventService es) {
        super(es, "Render View", true);

        m_box = new vtkBoxWidget();
        m_box.SetPlaceFactor(1.0);
        m_box.AddObserver("InteractionEvent", this, "boxInteraction");
        m_box.RotationEnabledOff();
        m_box.InsideOutOn();

        registerWidget(m_box);
    }

    /**
     * Set the volumes to be rendered.<br>
     * 
     * Currently all old volumes will be removed, and only the new volumes will be displayed.
     * 
     * @param volumes The new volumes
     */
    @Override
    protected final List<vtkProp> addViewProps(final List<Viewer3DNodeVolume> volumes) {
        m_volumes = volumes;

        final List<vtkProp> result = new LinkedList<vtkProp>();

        if (m_volumes.size() > 0) {
            // add the volumes to the renderer
            for (final Viewer3DNodeVolume v : m_volumes) {
                result.add(v.getVolume());
            }

            // add the bounding box of the first actor as well, they all have
            // the
            // same size anyway
            if (m_boundingBox) {
                result.add(m_volumes.get(0).getBoundingBoxActor());
            }

            // attach the box only to the first volume
            m_box.SetProp3D(m_volumes.get(0).getVolume());

            // use the current settings of the box to clip
            if (m_first) {
                m_box.PlaceWidget();
                m_first = false;

            } else {
                m_box.PlaceWidget();
                boxInteraction();

                // keep the box alive if it is active
                if (m_box.GetEnabled() != 0) {
                    render();
                    m_box.Off();
                    m_box.On();
                }
            }
        }

        return result;

    }

    @Override
    protected final void deleteAdditional() {
        m_box.RemoveAllObservers();
        m_box.Delete();
        m_volumes.clear();

        m_box = null;
        m_volumes = null;

    }

    /**
     * Callback method for vtk.
     */
    public final void boxInteraction() {
        final vtkPlanes planes = new vtkPlanes();
        m_box.GetPlanes(planes);
        for (final Viewer3DNodeVolume v : m_volumes) {
            v.setClippingPlanes(planes);
        }
    }

    /**
     * Use this method to reset the BoxWidget.
     */
    public final void resetBoxWidget() {
        m_box.PlaceWidget();
        boxInteraction();
        render();
    }

    /**
     * Use this method to toggle the visibility of the cropping box.
     */
    public final void toggleBox() {
        if (m_box.GetEnabled() != 0) {
            m_box.Off();
        } else {
            m_box.On();
        }
    }

    /**
     * Use this method to toggle the visibility of the bounding box.
     */
    public final void toggleBoundingBox() {
        m_boundingBox = m_boundingBox == false ? true : false;

        if (m_boundingBox) {
            addProp(m_volumes.get(0).getBoundingBoxActor());
        } else {
            removeProp(m_volumes.get(0).getBoundingBoxActor());
        }

        render();
    }

}
