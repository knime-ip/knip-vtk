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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;

import javax.swing.BoxLayout;

import org.knime.knip.core.ui.event.EventService;
import org.knime.knip.core.ui.imgviewer.ViewerComponent;
import org.knime.knip.view3d.image.Viewer3DNodeVolume;

/**
 * Show all three Views (Axial, Coronal, Sagittal) above each other.
 * 
 * @author <a href="mailto:dietzc85@googlemail.com">Christian Dietz</a>
 * @author <a href="mailto:horn_martin@gmx.de">Martin Horn</a>
 * @author <a href="mailto:michael.zinsmaier@googlemail.com">Michael Zinsmaier</a>
 * @author Clemens Müthing (clemens.muething@uni-konstanz.de)
 */
public class Viewer3DNodeSliceViewer extends ViewerComponent {

    /**
     * Eclipse generated.
     */
    private static final long serialVersionUID = 5469293057012796046L;

    private final Viewer3DNodeSliceRenderer m_rendererAxial;

    private final Viewer3DNodeSliceRenderer m_rendererCoronal;

    private final Viewer3DNodeSliceRenderer m_rendererSagittal;

    private EventService m_eventService = null;

    //    private static final NodeLogger LOGGER = Viewer3DNodeUtil.LOGGER;

    /**
     * Set up a new Viewer.
     * 
     * @param eventService the eventService to use
     */
    public Viewer3DNodeSliceViewer(final EventService eventService) {
        super("Slice View", false);

        setEventService(eventService);

        // set up the renderer
        m_rendererAxial = Viewer3DNodeSliceRenderer.newAxialView(m_eventService);
        m_rendererCoronal = Viewer3DNodeSliceRenderer.newCoronalView(m_eventService);
        m_rendererSagittal = Viewer3DNodeSliceRenderer.newSagittalView(m_eventService);

        // set up the layout
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(m_rendererAxial);
        add(m_rendererCoronal);
        add(m_rendererSagittal);
    }

    /**
     * Set the volumes to be rendered as slices.
     * 
     * @param volumes the volumes.
     */
    public final void setImages(final List<Viewer3DNodeVolume> volumes) {
        m_rendererAxial.setVolumes(volumes);
        m_rendererCoronal.setVolumes(volumes);
        m_rendererSagittal.setVolumes(volumes);
    }

    /**
     * Make all Views render.
     */
    public final void render() {
        m_rendererAxial.render();
        m_rendererCoronal.render();
        m_rendererSagittal.render();
    }

    /**
     * Free the resources of the viewers.
     */
    public final void delete() {
        m_rendererAxial.delete();
        m_rendererCoronal.delete();
        m_rendererSagittal.delete();
    }

    @Override
    public final void setEventService(final EventService eventService) {
        if (eventService != null) {
            m_eventService = eventService;
        } else {
            m_eventService = new EventService();
        }
    }

    @Override
    public final Position getPosition() {
        // not used
        return null;
    }

    @Override
    public final void saveComponentConfiguration(final ObjectOutput out) throws IOException {
        // not used
    }

    @Override
    public final void loadComponentConfiguration(final ObjectInput in) throws IOException, ClassNotFoundException {
        // not used
    }
}
